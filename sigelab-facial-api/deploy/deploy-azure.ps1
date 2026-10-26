<#
.SYNOPSIS
  Despliega SIGELAB Facial API en Azure Container Apps (con Azure for Students).

.DESCRIPTION
  1. Inicia sesion en Azure (si hace falta) y registra los proveedores necesarios.
  2. Busca su servidor Azure SQL (sigelabumg) y usa su misma region.
  3. Permite que los servicios de Azure se conecten al servidor SQL.
  4. Crea un Azure Container Registry, construye la imagen Docker y la sube.
  5. Crea el entorno y la Container App con los secretos (clave de BD, API key, clave de cifrado).
  6. Prueba /health y la conexion a la base de datos.

  Los secretos generados se guardan en deploy\.secrets.local.json (NO lo suba a GitHub).
  La contrasena de SQL no se guarda en ese archivo.

.EXAMPLE
  # Primer despliegue (pregunta usuario y contrasena de SQL):
  .\deploy\deploy-azure.ps1

.EXAMPLE
  # Despues de cambiar el codigo Python, solo reconstruye y actualiza la imagen:
  .\deploy\deploy-azure.ps1 -SoloActualizarCodigo

.EXAMPLE
  # Dejar 1 replica siempre encendida (por ejemplo el dia de la presentacion):
  .\deploy\deploy-azure.ps1 -SoloEscalar -MinReplicas 1
#>
[CmdletBinding()]
param(
    [string]$ResourceGroup = "rg-sigelab-facial",
    [string]$Location = "",
    [string]$AppName = "sigelab-facial-api",
    [string]$EnvironmentName = "sigelab-env",
    [string]$AcrName = "",
    [string]$SqlServer = "sigelabumg",
    [string]$SqlDatabase = "sigelabadministrador",
    [string]$SqlUser = "",
    [string]$UsersTable = "Usuario",
    [string]$UserIdColumn = "IdUsuario",
    [string]$UserNameColumn = "NombreUsuario",
    [string]$UserRoleColumn = "IdRol",
    [string]$UserActiveColumn = "Estado",
    # IdRol que puede eliminar perfiles faciales (varios separados por coma).
    [string]$AdminRoles = "1",
    [ValidateSet("auto", "local", "acr")]
    [string]$BuildMode = "auto",
    [ValidateRange(0, 3)]
    [int]$MinReplicas = 0,
    [switch]$SoloActualizarCodigo,
    [switch]$SoloEscalar
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $PSScriptRoot
$SecretsFile = Join-Path $PSScriptRoot ".secrets.local.json"
$ImageName = "sigelab-facial-api"

function Write-Step([string]$Text) { Write-Host "`n==> $Text" -ForegroundColor Cyan }

function Invoke-Az {
    # Ejecuta az y detiene el script si falla.
    $output = & az @args
    if ($LASTEXITCODE -ne 0) { throw "Fallo el comando: az $($args -join ' ')" }
    return $output
}

function Test-Az {
    # Devuelve $true si el comando az termina bien (sin detener el script).
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $null = & az @args -o none 2>$null
    $ok = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = $previous
    return $ok
}

function New-RandomBase64([int]$Bytes, [switch]$UrlSafe) {
    $buffer = New-Object byte[] $Bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    $text = [Convert]::ToBase64String($buffer)
    if ($UrlSafe) { $text = $text.TrimEnd("=").Replace("+", "-").Replace("/", "_") }
    return $text
}

function Save-Secrets($Data) {
    $json = $Data | ConvertTo-Json -Depth 5
    [System.IO.File]::WriteAllText($SecretsFile, $json, (New-Object System.Text.UTF8Encoding $false))
}

function Wait-Health([string]$Url) {
    Write-Host "Esperando a que la API responda (el primer arranque puede tardar 1-2 minutos)..."
    for ($i = 1; $i -le 30; $i++) {
        try {
            $health = Invoke-RestMethod -Uri "$Url/health" -TimeoutSec 20
            if ($health.modelLoaded) { return $health }
        } catch { }
        Start-Sleep -Seconds 6
    }
    throw "La API no respondio en $Url/health. Revise los logs: az containerapp logs show -n $AppName -g $ResourceGroup --follow"
}

# ------------------------------------------------------------------ inicio
if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "No se encontro Azure CLI. Instalelo con: winget install -e --id Microsoft.AzureCLI  (y abra una nueva terminal)"
}

$secrets = @{}
if (Test-Path $SecretsFile) {
    (Get-Content $SecretsFile -Raw | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $secrets[$_.Name] = $_.Value }
}
if (-not $AcrName) { $AcrName = $secrets["acrName"] }
if (-not $AcrName) { $AcrName = "sigelabacr" + (Get-Random -Minimum 10000 -Maximum 99999) }
$secrets["acrName"] = $AcrName
$secrets["resourceGroup"] = $ResourceGroup
$secrets["appName"] = $AppName

Write-Step "Verificando sesion de Azure"
if (-not (Test-Az account show)) { Invoke-Az login | Out-Null }
$account = Invoke-Az account show --query "{name:name, user:user.name}" -o json | ConvertFrom-Json
Write-Host "Suscripcion: $($account.name)  Usuario: $($account.user)"

# ------------------------------------------------------- solo escalar
if ($SoloEscalar) {
    Write-Step "Cambiando replicas minimas a $MinReplicas"
    Invoke-Az containerapp update -n $AppName -g $ResourceGroup --min-replicas $MinReplicas --max-replicas ([Math]::Max(1, $MinReplicas)) -o none
    Write-Host "Listo. Con MinReplicas=0 la API se apaga sola cuando no se usa (gratis, pero el primer uso tarda)."
    return
}

Write-Step "Preparando extensiones y proveedores (solo tarda la primera vez)"
Invoke-Az extension add --name containerapp --upgrade --only-show-errors -o none
foreach ($provider in "Microsoft.App", "Microsoft.OperationalInsights", "Microsoft.ContainerRegistry") {
    Invoke-Az provider register --namespace $provider --wait -o none
}

Write-Step "Buscando el servidor Azure SQL '$SqlServer'"
$sqlInfo = Invoke-Az sql server list --query "[?name=='$SqlServer'] | [0].{rg:resourceGroup, loc:location}" -o json | ConvertFrom-Json
if (-not $sqlInfo) { throw "No encontre el servidor SQL '$SqlServer' en esta suscripcion. Use -SqlServer <nombre>." }
if (-not $Location) { $Location = $sqlInfo.loc }
Write-Host "Servidor SQL en grupo '$($sqlInfo.rg)', region '$($sqlInfo.loc)'. Region de la API: $Location"

if (-not $SoloActualizarCodigo) {
    Write-Step "Creando grupo de recursos y registro de contenedores ($AcrName)"
    Invoke-Az group create -n $ResourceGroup -l $Location -o none
    if (-not (Test-Az acr show -n $AcrName -g $ResourceGroup)) {
        Invoke-Az acr create -n $AcrName -g $ResourceGroup -l $Location --sku Basic --admin-enabled true -o none
    }

    Write-Step "Permitiendo que los servicios de Azure se conecten a $SqlServer"
    if (-not (Test-Az sql server firewall-rule show -g $sqlInfo.rg -s $SqlServer -n AllowAllWindowsAzureIps)) {
        Invoke-Az sql server firewall-rule create -g $sqlInfo.rg -s $SqlServer -n AllowAllWindowsAzureIps `
            --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 -o none
    }
}
Save-Secrets $secrets

# ---------------------------------------------------- construir imagen
$tag = Get-Date -Format "yyyyMMdd-HHmmss"
$loginServer = "$AcrName.azurecr.io"
$image = "$loginServer/${ImageName}:$tag"

if ($BuildMode -eq "auto") {
    $BuildMode = "acr"
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $previous = $ErrorActionPreference; $ErrorActionPreference = "Continue"
        $null = & docker info 2>$null
        if ($LASTEXITCODE -eq 0) { $BuildMode = "local" }
        $ErrorActionPreference = $previous
    }
}

Write-Step "Construyendo la imagen ($BuildMode): $image"
if ($BuildMode -eq "local") {
    & docker build --platform linux/amd64 -t $image $ProjectDir
    if ($LASTEXITCODE -ne 0) { throw "docker build fallo." }
    Invoke-Az acr login -n $AcrName | Out-Null
    & docker push $image
    if ($LASTEXITCODE -ne 0) { throw "docker push fallo." }
} else {
    Write-Host "No se encontro Docker Desktop encendido: se construye en Azure (ACR Tasks)."
    Write-Host "Si falla con 'TasksOperationsNotAllowed', instale/abra Docker Desktop y use -BuildMode local."
    Invoke-Az acr build -r $AcrName -t "${ImageName}:$tag" --platform linux/amd64 $ProjectDir
}

# ------------------------------------------- solo actualizar el codigo
if ($SoloActualizarCodigo) {
    Write-Step "Actualizando la Container App con la nueva imagen"
    Invoke-Az containerapp update -n $AppName -g $ResourceGroup --image $image -o none
    $fqdn = Invoke-Az containerapp show -n $AppName -g $ResourceGroup --query properties.configuration.ingress.fqdn -o tsv
    $health = Wait-Health "https://$fqdn"
    Write-Host "`nListo: https://$fqdn  (modelo: $($health.model))" -ForegroundColor Green
    return
}

# --------------------------------------------------- secretos y config
if (-not $secrets["encryptionKey"]) { $secrets["encryptionKey"] = New-RandomBase64 32 }
if (-not $secrets["apiKey"]) { $secrets["apiKey"] = New-RandomBase64 32 -UrlSafe }
if (-not $SqlUser) { $SqlUser = $secrets["sqlUser"] }
if (-not $SqlUser) { $SqlUser = Read-Host "Usuario administrador de Azure SQL ($SqlServer)" }
$secrets["sqlUser"] = $SqlUser
Save-Secrets $secrets

$securePassword = Read-Host "Contrasena de Azure SQL para '$SqlUser'" -AsSecureString
$sqlPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword))

Write-Step "Creando el entorno de Container Apps ($EnvironmentName)"
if (-not (Test-Az containerapp env show -n $EnvironmentName -g $ResourceGroup)) {
    Invoke-Az containerapp env create -n $EnvironmentName -g $ResourceGroup -l $Location -o none
}
$environmentId = Invoke-Az containerapp env show -n $EnvironmentName -g $ResourceGroup --query id -o tsv
$acrUser = Invoke-Az acr credential show -n $AcrName --query username -o tsv
$acrPassword = Invoke-Az acr credential show -n $AcrName --query "passwords[0].value" -o tsv

function EnvVar([string]$Name, [string]$Value) { return @{ name = $Name; value = $Value } }
function EnvSecret([string]$Name, [string]$Secret) { return @{ name = $Name; secretRef = $Secret } }

$spec = @{
    location   = $Location
    name       = $AppName
    type       = "Microsoft.App/containerApps"
    properties = @{
        managedEnvironmentId = $environmentId
        configuration        = @{
            activeRevisionsMode = "Single"
            ingress             = @{ external = $true; targetPort = 8000; transport = "auto"; allowInsecure = $false }
            registries          = @(@{ server = $loginServer; username = $acrUser; passwordSecretRef = "registry-password" })
            secrets             = @(
                @{ name = "registry-password"; value = $acrPassword },
                @{ name = "db-password"; value = $sqlPassword },
                @{ name = "encryption-key"; value = $secrets["encryptionKey"] },
                @{ name = "api-keys"; value = $secrets["apiKey"] }
            )
        }
        template             = @{
            containers = @(@{
                name      = "facial-api"
                image     = $image
                resources = @{ cpu = 1.0; memory = "2Gi" }
                env       = @(
                    (EnvVar "DB_HOST" "$SqlServer.database.windows.net"),
                    (EnvVar "DB_NAME" $SqlDatabase),
                    (EnvVar "DB_USER" $SqlUser),
                    (EnvSecret "DB_PASSWORD" "db-password"),
                    (EnvSecret "FACIAL_ENCRYPTION_KEY" "encryption-key"),
                    (EnvSecret "API_KEYS" "api-keys"),
                    (EnvVar "USERS_TABLE" $UsersTable),
                    (EnvVar "USER_ID_COLUMN" $UserIdColumn),
                    (EnvVar "USER_NAME_COLUMN" $UserNameColumn),
                    (EnvVar "USER_ROLE_COLUMN" $UserRoleColumn),
                    (EnvVar "USER_ACTIVE_COLUMN" $UserActiveColumn),
                    (EnvVar "ADMIN_ROLES" $AdminRoles),
                    (EnvVar "AUTO_MIGRATE" "true"),
                    (EnvVar "REQUIRE_LIVENESS" "true"),
                    (EnvVar "INFERENCE_CONCURRENCY" "1")
                )
                probes    = @(
                    @{ type = "Startup"; httpGet = @{ path = "/health"; port = 8000 }; initialDelaySeconds = 5; periodSeconds = 10; failureThreshold = 10; timeoutSeconds = 5 },
                    @{ type = "Liveness"; httpGet = @{ path = "/health"; port = 8000 }; periodSeconds = 30; failureThreshold = 3; timeoutSeconds = 5 }
                )
            })
            scale      = @{
                minReplicas = $MinReplicas
                maxReplicas = [Math]::Max(1, $MinReplicas)
                rules       = @(@{ name = "http"; http = @{ metadata = @{ concurrentRequests = "10" } } })
            }
        }
    }
}

# JSON es YAML valido; se escribe a un archivo temporal para no pasar secretos por la linea de comandos.
$specFile = Join-Path ([IO.Path]::GetTempPath()) ("sigelab-facial-" + [Guid]::NewGuid().ToString("N") + ".yaml")
try {
    [System.IO.File]::WriteAllText($specFile, ($spec | ConvertTo-Json -Depth 20), (New-Object System.Text.UTF8Encoding $false))
    if (Test-Az containerapp show -n $AppName -g $ResourceGroup) {
        Write-Step "Actualizando la Container App $AppName"
        Invoke-Az containerapp update -n $AppName -g $ResourceGroup --yaml $specFile -o none
    } else {
        Write-Step "Creando la Container App $AppName"
        Invoke-Az containerapp create -n $AppName -g $ResourceGroup --yaml $specFile -o none
    }
} finally {
    Remove-Item $specFile -Force -ErrorAction SilentlyContinue
    $sqlPassword = $null
}

$fqdn = Invoke-Az containerapp show -n $AppName -g $ResourceGroup --query properties.configuration.ingress.fqdn -o tsv
$apiUrl = "https://$fqdn"
$secrets["apiUrl"] = $apiUrl
Save-Secrets $secrets

$health = Wait-Health $apiUrl
Write-Step "Probando la conexion a Azure SQL desde la API"
try {
    $db = Invoke-RestMethod -Uri "$apiUrl/api/health/db" -Headers @{ "X-API-Key" = $secrets["apiKey"] } -TimeoutSec 90
    Write-Host "Base de datos: $($db.database)" -ForegroundColor Green
} catch {
    Write-Warning "La API no pudo conectarse a la base de datos. Revise usuario/contrasena y el firewall de $SqlServer."
}

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host " SIGELAB Facial API desplegada" -ForegroundColor Green
Write-Host "  URL:      $apiUrl"
Write-Host "  API key:  $($secrets['apiKey'])"
Write-Host "  Docs:     $apiUrl/docs"
Write-Host "  Secretos guardados en: $SecretsFile"
Write-Host "  IMPORTANTE: respalde 'encryptionKey' de ese archivo. Si se pierde,"
Write-Host "  los rostros registrados ya no se podran verificar."
Write-Host "==============================================================" -ForegroundColor Green
