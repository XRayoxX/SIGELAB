# =============================================================================
#  SIGELAB - Genera la aplicación ejecutable para Windows (no necesita Java instalado)
#
#  Requisitos (solo en la PC del desarrollador):
#    1) En IntelliJ: Build > Rebuild Project (arma las ventanas .form en target\classes)
#    2) JDK 21 con jpackage y las dependencias descargadas por Maven (~\.m2)
#
#  Uso (desde la carpeta del proyecto):
#    powershell -ExecutionPolicy Bypass -File empaquetar.ps1
#    powershell -ExecutionPolicy Bypass -File empaquetar.ps1 -AccesoDirecto   (además crea el icono en el Escritorio)
#
#  Resultado:
#    dist\SIGELAB\SIGELAB.exe        -> aplicación lista (incluye su propio Java)
#    dist\SIGELAB-portable.zip       -> para copiar a otra PC: descomprimir y abrir SIGELAB.exe
# =============================================================================
param(
    [switch]$AccesoDirecto
)

$ErrorActionPreference = 'Stop'
$proyecto = $PSScriptRoot
$clases = Join-Path $proyecto 'target\classes'
$fuentes = Join-Path $proyecto 'src\main\java'
$dist = Join-Path $proyecto 'dist'
$temporal = Join-Path $env:TEMP 'sigelab-empaquetado'
$m2 = Join-Path $env:USERPROFILE '.m2\repository'

# Dependencias que la aplicación necesita en tiempo de ejecución
$dependencias = @(
    'com\microsoft\sqlserver\mssql-jdbc\13.4.0.jre11\mssql-jdbc-13.4.0.jre11.jar',
    'com\intellij\forms_rt\7.0.3\forms_rt-7.0.3.jar',
    'com\google\zxing\core\3.5.3\core-3.5.3.jar',
    'com\google\zxing\javase\3.5.3\javase-3.5.3.jar',
    # Webcam para el reconocimiento facial (webcam-capture y sus dependencias)
    'com\github\sarxos\webcam-capture\0.3.12\webcam-capture-0.3.12.jar',
    'com\nativelibs4java\bridj\0.7.0\bridj-0.7.0.jar',
    'org\slf4j\slf4j-api\1.7.2\slf4j-api-1.7.2.jar',
    # Correo (Jakarta Mail / Angus) para las notificaciones
    'org\eclipse\angus\angus-mail\2.0.3\angus-mail-2.0.3.jar',
    'org\eclipse\angus\angus-activation\2.0.2\angus-activation-2.0.2.jar',
    'jakarta\mail\jakarta.mail-api\2.1.3\jakarta.mail-api-2.1.3.jar',
    'jakarta\activation\jakarta.activation-api\2.1.3\jakarta.activation-api-2.1.3.jar'
) | ForEach-Object { Join-Path $m2 $_ }

# Si Maven descargó otra versión de alguna dependencia, se usa la que esté en ~\.m2
$dependencias = $dependencias | ForEach-Object {
    if (Test-Path $_) { $_ } else {
        $carpetaArtefacto = Split-Path (Split-Path $_ -Parent) -Parent
        $alternativa = Get-ChildItem $carpetaArtefacto -Recurse -Filter *.jar -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object FullName | Select-Object -Last 1
        if ($alternativa) { $alternativa.FullName } else { $_ }
    }
}

function Fallar($mensaje) {
    Write-Host ""
    Write-Host "ERROR: $mensaje" -ForegroundColor Red
    exit 1
}

Write-Host "== 1/5 Verificando que IntelliJ compiló las ventanas (.form)"
# Solo las clases que tienen .form necesitan que IntelliJ las arme
foreach ($java in Get-ChildItem (Join-Path $fuentes 'SIGELAB\view') -Filter *.java | Where-Object { Test-Path ([IO.Path]::ChangeExtension($_.FullName, '.form')) }) {
    $nombre = $java.BaseName
    $clase = Join-Path $clases "SIGELAB\view\$nombre.class"
    $form = [IO.Path]::ChangeExtension($java.FullName, '.form')
    if (-not (Test-Path $clase)) {
        Fallar "Falta $nombre.class. Abra IntelliJ y haga Build > Rebuild Project."
    }
    $modificado = (Get-Item $java.FullName).LastWriteTime
    if ((Test-Path $form) -and (Get-Item $form).LastWriteTime -gt $modificado) { $modificado = (Get-Item $form).LastWriteTime }
    if ($modificado -gt (Get-Item $clase).LastWriteTime) {
        Fallar "La ventana $nombre cambió después de la última compilación. En IntelliJ: Build > Rebuild Project."
    }
    # Las ventanas del GUI Designer deben tener el método generado por IntelliJ
    if (-not (Select-String -Path $clase -Pattern 'setupUI' -SimpleMatch -Quiet)) {
        Fallar "$nombre.class no tiene las ventanas armadas. En IntelliJ: Build > Rebuild Project."
    }
}
foreach ($jar in $dependencias) {
    if (-not (Test-Path $jar)) { Fallar "No se encontró $jar. En IntelliJ recargue Maven (icono de Maven > Reload)." }
}

Write-Host "== 2/5 Preparando clases"
Remove-Item -Recurse -Force $temporal -ErrorAction SilentlyContinue
$clasesApp = Join-Path $temporal 'clases'
$entrada = Join-Path $temporal 'entrada'
New-Item -ItemType Directory -Force $clasesApp, $entrada | Out-Null
# Las ventanas se toman de IntelliJ (ya armadas). Archivos con contraseñas NO entran al .jar.
Copy-Item -Recurse "$clases\*" $clasesApp
Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $clasesApp 'config.properties'), (Join-Path $clasesApp 'db.properties'), (Join-Path $clasesApp '.gitkeep')
# El resto (model, service, repository y ventanas sin .form) se recompila desde el código fuente
$noVistas = Get-ChildItem -Recurse $fuentes -Filter *.java | Where-Object { -not (Test-Path ([IO.Path]::ChangeExtension($_.FullName, '.form'))) } | ForEach-Object FullName
$classpath = ($clasesApp + ';' + ($dependencias -join ';'))
& javac -encoding UTF-8 -nowarn -cp $classpath -d $clasesApp $noVistas
if ($LASTEXITCODE -ne 0) { Fallar "El código no compila. Revise los errores de arriba." }

Write-Host "== 3/5 Creando sigelab.jar"
& jar --create --file (Join-Path $entrada 'sigelab.jar') --main-class Main -C $clasesApp .
if ($LASTEXITCODE -ne 0) { Fallar "No se pudo crear sigelab.jar." }
$dependencias | ForEach-Object { Copy-Item $_ $entrada }

Write-Host "== 4/5 Creando SIGELAB.exe con su propio Java (puede tardar un minuto)"
Remove-Item -Recurse -Force (Join-Path $dist 'SIGELAB') -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $dist | Out-Null
& jpackage --type app-image --name SIGELAB --app-version 1.0.0 --vendor 'UMG - Programacion II' `
    --input $entrada --main-jar sigelab.jar --main-class Main --dest $dist `
    --java-options '-Dfile.encoding=UTF-8' `
    --jlink-options '--strip-debug --no-header-files --no-man-pages'
if ($LASTEXITCODE -ne 0) { Fallar "jpackage no pudo crear la aplicación." }

# La configuración de la base va AFUERA del .jar, junto a él: se puede editar sin recompilar
$carpetaApp = Join-Path $dist 'SIGELAB\app'
$config = Join-Path $proyecto 'src\main\resources\config.properties'
if (Test-Path $config) {
    Copy-Item $config $carpetaApp
} else {
    Copy-Item (Join-Path $proyecto 'src\main\resources\config.properties.example') (Join-Path $carpetaApp 'config.properties')
    Write-Host "   AVISO: no existe config.properties; edite dist\SIGELAB\app\config.properties con los datos de la base." -ForegroundColor Yellow
}

Write-Host "== 5/5 Creando dist\SIGELAB-portable.zip"
$zip = Join-Path $dist 'SIGELAB-portable.zip'
Remove-Item -Force $zip -ErrorAction SilentlyContinue
Compress-Archive -Path (Join-Path $dist 'SIGELAB') -DestinationPath $zip

if ($AccesoDirecto) {
    $escritorio = [Environment]::GetFolderPath('Desktop')
    $shell = New-Object -ComObject WScript.Shell
    $atajo = $shell.CreateShortcut((Join-Path $escritorio 'SIGELAB.lnk'))
    $atajo.TargetPath = Join-Path $dist 'SIGELAB\SIGELAB.exe'
    $atajo.WorkingDirectory = Join-Path $dist 'SIGELAB'
    $atajo.Description = 'Sistema de Gestion y Control de Laboratorios Universitarios'
    $atajo.Save()
    Write-Host "   Acceso directo creado en el Escritorio: SIGELAB"
}

Remove-Item -Recurse -Force $temporal -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "LISTO" -ForegroundColor Green
Write-Host "  Ejecutable : $(Join-Path $dist 'SIGELAB\SIGELAB.exe')"
Write-Host "  Para otra PC: copie $zip, descomprímalo y abra SIGELAB\SIGELAB.exe"
