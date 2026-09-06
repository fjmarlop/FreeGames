# FreeGames

Monorepo de juegos pequeños para Android: **offline, sin anuncios, sin tracking, sin
dependencias de Google Play**. Se distribuyen como APK firmada y descargable directamente,
enlazada desde la sección [`/juegos` de fjmarlop.es](https://fjmarlop.es). Cada release
publica el **SHA256** del APK para que cualquiera pueda verificar que el binario que instala
es exactamente el que salió de este repositorio.

La garantía técnica de "no envía datos a ningún sitio": **ningún módulo declara
`android.permission.INTERNET`**, y el build lo verifica automáticamente (ver
[El guard de permisos](#el-guard-de-permisos)).

---

## Estructura

```
FreeGames/
├── build-logic/                 # composite build con los convention plugins
│   └── convention/
│       └── src/main/kotlin/
│           ├── SdkConfig.kt                      # compileSdk / minSdk / targetSdk — fuente única
│           ├── AndroidGameConventionPlugin.kt    # id("freegames.android.game")
│           └── VerifyNoDangerousPermissionsTask.kt
├── gradle/
│   └── libs.versions.toml        # catálogo de versiones compartido
├── games/
│   ├── _template/                # plantilla para juegos nuevos (NO registrada en settings)
│   └── sudoku/                   # primer juego
├── .github/workflows/release.yml # build + release por tag
├── settings.gradle.kts
└── README.md
```

Todo lo que debe ser idéntico entre juegos vive en el convention plugin
`freegames.android.game`. Un módulo de juego solo declara lo suyo: `namespace`,
`applicationId`, dependencias y, si acaso, ajustes de `lint`/`testOptions`.

### Qué fija el convention plugin

| | |
|---|---|
| SDK | `compileSdk` / `minSdk` / `targetSdk` desde `SdkConfig` |
| Lenguaje | Java 17, Kotlin 17, `-Xannotation-default-target=param-property` |
| Plugins | Android application, Compose compiler, kotlinx.serialization, KSP, Hilt |
| Compose | `buildFeatures.compose = true` |
| Release | `isMinifyEnabled` + `isShrinkResources` + ProGuard |
| Firma | `signingConfigs.release` desde entorno / `local.properties` |
| Versión | `versionName` desde `-PversionName`, `versionCode` derivado de la semver |
| Seguridad | tarea `verify<Variant>Permissions` enganchada a `assemble` / `bundle` / `check` |

---

## Requisitos

- **JDK 17** (Temurin recomendado; es lo que usa CI).
- **Android SDK** con la plataforma indicada en `SdkConfig.COMPILE_SDK`.
  `local.properties` debe apuntar al SDK: `sdk.dir=/ruta/al/Android/Sdk`.

## Compilar y testear

```bash
./gradlew :games:sudoku:testDebugUnitTest    # tests unitarios JVM
./gradlew :games:sudoku:lintDebug            # lint
./gradlew :games:sudoku:assembleDebug        # APK debug
./gradlew :games:sudoku:assembleRelease      # APK release (sin firmar si no hay credenciales)
./gradlew :games:sudoku:check                # todo lo anterior + el guard de permisos
```

Sustituye `sudoku` por el módulo que corresponda.

---

## Añadir un juego nuevo

Ejemplo: añadir `tetris`.

1. **Copiar la plantilla:**

   ```bash
   cp -r games/_template games/tetris
   ```

2. **Renombrar el paquete.** Mueve
   `games/tetris/src/main/java/es/fjmarlop/freegames/template/` a
   `.../freegames/tetris/` y cambia `package es.fjmarlop.freegames.template` por
   `...tetris` en los dos `.kt`.

3. **Editar `games/tetris/build.gradle.kts`:**

   ```kotlin
   android {
       namespace = "es.fjmarlop.freegames.tetris"
       defaultConfig {
           applicationId = "es.fjmarlop.freegames.tetris"
       }
   }
   ```

4. **Registrar el módulo** en `settings.gradle.kts`:

   ```kotlin
   include(":games:sudoku")
   include(":games:tetris")
   ```

5. **Cambiar `app_name`** en `games/tetris/src/main/res/values/strings.xml` y, si
   quieres, el icono (`android:icon` en el manifest).

6. Comprobar:

   ```bash
   ./gradlew :games:tetris:check
   ```

No hay que tocar versiones de SDK, firma, ni el workflow: todo lo hereda del convention
plugin. El workflow de release ya funciona para cualquier módulo bajo `games/`.

> `games/_template` **no está** en `settings.gradle.kts` a propósito: es material para
> copiar, no un módulo que se compile.

---

## Firma release

El convention plugin resuelve las credenciales en este orden por clave: **variable de
entorno → `local.properties`** (que está en `.gitignore`). Claves:

| Clave | Contenido |
|---|---|
| `KEYSTORE_BASE64` | el `.jks` codificado en base64 |
| `KEYSTORE_PATH` | *alternativa local*: ruta a un `.jks` ya en disco |
| `KEYSTORE_PASSWORD` | contraseña del keystore |
| `KEY_ALIAS` | alias de la clave |
| `KEY_PASSWORD` | contraseña de la clave |

- Si **todas** resuelven → el APK release se firma.
- Si **falta alguna** → `assembleRelease` sigue funcionando pero produce un APK **sin
  firmar** (útil para `check` en CI sin secretos).

### Compilar release firmada en local

Añade a `local.properties` (nunca se commitea):

```properties
KEYSTORE_PATH=/ruta/absoluta/al/freegames-release.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

y ejecuta `./gradlew :games:sudoku:assembleRelease`.

### Generar el keystore (una sola vez, para toda la familia de juegos)

```bash
keytool -genkeypair -v -keystore freegames-release.jks \
  -alias freegames -keyalg RSA -keysize 4096 -validity 10000
```

Guarda el `.jks` y las contraseñas en un gestor de secretos. **No entran en el repo.**

### Secrets en GitHub

En *Settings → Secrets and variables → Actions* del repo, crea:

```
KEYSTORE_BASE64      # base64 -w0 freegames-release.jks  (y pega el resultado)
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

---

## Proceso de release por tag

Publicar `sudoku` versión `1.0.0`:

```bash
git tag sudoku-v1.0.0
git push origin sudoku-v1.0.0
```

El workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) se dispara con
cualquier tag `<juego>-vX.Y.Z` y:

1. Extrae del tag el **juego** (todo antes de `-v`) y la **versión** (todo después).
2. Configura JDK 17 (Temurin) + Gradle con cache.
3. Decodifica `KEYSTORE_BASE64` a un `.jks` temporal.
4. Si el módulo tiene la tarea `securityCheck` (plugin propio OWASP Mobile Top 10), la
   ejecuta y **aborta el release si falla**. Si no está, deja un `::warning::` y continúa.
5. `./gradlew :games:<juego>:assembleRelease -PversionName=<version>`.
6. Calcula el **SHA256** del APK.
7. Crea un **GitHub Release** con el tag, adjuntando `<juego>-v<version>.apk` y
   `<juego>-v<version>.apk.sha256`, con este cuerpo:

   > APK firmada, sin permisos de red, código abierto. Verifica el hash antes de instalar.

8. Borra el keystore temporal (siempre, incluso si el job falla).

`versionCode` se deriva de la versión: `MAJOR*10000 + MINOR*100 + PATCH`
(`1.0.0` → `10000`, `1.4.2` → `10402`).

---

## Verificar una APK descargada

```bash
# con el fichero .sha256 al lado del .apk:
sha256sum -c sudoku-v1.0.0.apk.sha256

# o a mano:
sha256sum sudoku-v1.0.0.apk   # debe coincidir con el hash del Release
```

Comprobar que no puede acceder a la red:

```bash
aapt dump permissions sudoku-v1.0.0.apk    # no debe listar android.permission.INTERNET
```

---

## El guard de permisos

[`VerifyNoDangerousPermissionsTask`](build-logic/convention/src/main/kotlin/VerifyNoDangerousPermissionsTask.kt)
lee el **manifest fusionado** de cada variante (no el manifest fuente — así detecta permisos
que pudiera inyectar una dependencia) y **falla el build** si aparece cualquiera de la lista
prohibida: `INTERNET` (la que importa), estado de red/wifi, ubicación, cámara, micrófono,
contactos, SMS, registro de llamadas, almacenamiento/media, cuentas y Bluetooth.

Se ejecuta automáticamente en `assemble<Variant>`, `bundle<Variant>` y `check` de todos los
juegos. Para verlo en acción:

```bash
./gradlew :games:sudoku:verifyReleasePermissions
```
