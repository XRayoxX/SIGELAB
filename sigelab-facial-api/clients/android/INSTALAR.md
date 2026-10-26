# SIGELAB móvil (Android) – reconocimiento facial

Copie a su proyecto de Android Studio:

| Archivo | Destino en su app |
|---|---|
| `app/src/main/java/sigelab/facial/*.java` | su carpeta `java/` (puede cambiar el `package` por el suyo) |
| `app/src/main/res/layout/activity_captura_facial.xml` | `res/layout/` |
| `app/src/main/res/values/facial_api.xml` | `res/values/` → ponga la URL y la API key |

Si su proyecto es Kotlin, estas clases Java funcionan igual (Android Studio mezcla ambos lenguajes).

## 1. Dependencias (`app/build.gradle` o `build.gradle.kts`)

```gradle
dependencies {
    def camerax = "1.3.4"
    implementation "androidx.camera:camera-core:$camerax"
    implementation "androidx.camera:camera-camera2:$camerax"
    implementation "androidx.camera:camera-lifecycle:$camerax"
    implementation "androidx.camera:camera-view:$camerax"
    implementation "com.squareup.okhttp3:okhttp:4.12.0"
}
```
(`minSdk` 21 o superior y Java 8+ en `compileOptions`.)

## 2. AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.front" android:required="false" />

<application ...>
    <activity android:name="sigelab.facial.CapturaFacialActivity"
              android:screenOrientation="portrait"
              android:exported="false" />
</application>
```

## 3. Usarla

Vea `EjemploLoginFacialActivity.java`: abre `CapturaFacialActivity` con
`MODO_VERIFICAR` (después del login), `MODO_REGISTRAR` o `MODO_IDENTIFICAR` (marcar ingreso),
y recibe el resultado (`r.coincide()`, `r.mensaje()`, `username`, `userId`).

## 4. Primera prueba en su teléfono (izquierda/derecha)

Registre su rostro y luego haga una verificación. Si siempre responde
*"Se detectó el giro hacia el lado contrario"*, abra `CapturaFacialActivity.java`
y cambie `FOTOS_EN_ESPEJO = false` por `true`. Algunos teléfonos entregan la foto
frontal en espejo.

## Seguridad

La API key queda dentro del APK y se puede extraer. Para el proyecto académico es aceptable.
Lo más seguro sería que el teléfono envíe las fotos a su API actual (que ya autentica al usuario)
y que esa API llame a la Facial API con la key guardada en el servidor. Puede usar una key distinta
para el celular: `API_KEYS=clave-escritorio,clave-movil`.
