import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import java.io.File
import java.util.Base64
import java.util.Properties

/**
 * Convention plugin applied by every game module in the monorepo (`id("freegames.android.game")`).
 *
 * Centralises everything that must be identical across games so that adding the next game is just
 * a namespace + applicationId + dependencies list:
 *  - SDK levels ([SdkConfig])
 *  - Java / Kotlin 17
 *  - Compose, the release build type (minify + resource shrink + proguard) and packaging excludes
 *  - the shared Gradle plugin set (Android, Compose compiler, kotlinx.serialization, KSP, Hilt)
 *  - release signing read from the environment / local.properties (never hard-coded, never committed)
 *  - the `verify<Variant>Permissions` guard that fails the build on any forbidden permission
 */
class AndroidGameConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("org.jetbrains.kotlin.plugin.serialization")
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        val signing = resolveReleaseSigning()

        // Version comes from `-PversionName=X.Y.Z` (the release workflow passes it from the git tag);
        // falls back to a dev default. versionCode is derived from the semver so it stays monotonic.
        val versionNameValue = providers.gradleProperty("versionName").orNull?.trim()?.ifBlank { null }
            ?: DEFAULT_VERSION_NAME

        extensions.configure<ApplicationExtension> {
            compileSdk = SdkConfig.COMPILE_SDK

            defaultConfig {
                minSdk = SdkConfig.MIN_SDK
                targetSdk = SdkConfig.TARGET_SDK
                versionName = versionNameValue
                versionCode = versionNameValue.toSemverVersionCode()
                vectorDrawables.useSupportLibrary = true
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            buildFeatures {
                compose = true
            }

            if (signing != null) {
                signingConfigs.create("release") {
                    storeFile = signing.storeFile
                    storePassword = signing.storePassword
                    keyAlias = signing.keyAlias
                    keyPassword = signing.keyPassword
                }
            } else {
                // No credentials resolved (e.g. CI `check` without secrets, or a fresh checkout):
                // assembleRelease still works, it just produces an unsigned APK.
                logger.warn(
                    "FreeGames: sin credenciales de firma release — el APK release saldrá SIN FIRMAR. " +
                        "Define KEYSTORE_BASE64/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD (entorno o local.properties).",
                )
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                    if (signing != null) {
                        signingConfig = signingConfigs.getByName("release")
                    }
                }
            }

            packaging {
                resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                // Kotlin 2.2: keep annotations on the parameter + property (Hilt/Room friendly).
                freeCompilerArgs.add("-Xannotation-default-target=param-property")
            }
        }

        registerPermissionGuard()
    }

    /** Wires a `verify<Variant>Permissions` task per variant and makes `assemble`/`check` depend on it. */
    private fun Project.registerPermissionGuard() {
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val capName = variant.name.replaceFirstChar { it.uppercase() }
                val guard = tasks.register<VerifyNoDangerousPermissionsTask>("verify${capName}Permissions") {
                    group = "verification"
                    description = "Falla si el manifest fusionado de ${variant.name} declara un permiso prohibido."
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    forbiddenPermissions.set(FORBIDDEN_PERMISSIONS)
                }
                // The variant's `assemble<Variant>` / `bundle<Variant>` tasks are created by AGP
                // after `onVariants`, so wire lazily via a live matching collection.
                tasks.matching { it.name == "assemble$capName" || it.name == "bundle$capName" }
                    .configureEach { dependsOn(guard) }
            }
        }
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(tasks.withType<VerifyNoDangerousPermissionsTask>())
        }
    }

    /**
     * Resolves release signing credentials, environment variables first, then `local.properties`
     * (git-ignored). Returns `null` when anything is missing so the release build stays unsigned
     * instead of failing.
     */
    private fun Project.resolveReleaseSigning(): ReleaseSigning? {
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use(::load)
        }
        fun value(key: String): String? =
            (providers.environmentVariable(key).orNull ?: localProps.getProperty(key))
                ?.trim()
                ?.ifBlank { null }

        val storePassword = value("KEYSTORE_PASSWORD") ?: return null
        val keyAlias = value("KEY_ALIAS") ?: return null
        val keyPassword = value("KEY_PASSWORD") ?: return null

        val storeFile: File = when {
            value("KEYSTORE_BASE64") != null -> {
                val decoded = Base64.getMimeDecoder().decode(value("KEYSTORE_BASE64"))
                layout.buildDirectory.file("signing/release.jks").get().asFile.apply {
                    parentFile.mkdirs()
                    if (!exists() || !readBytes().contentEquals(decoded)) writeBytes(decoded)
                }
            }

            value("KEYSTORE_PATH") != null -> file(value("KEYSTORE_PATH")!!)
            else -> return null
        }

        if (!storeFile.exists()) {
            logger.warn("FreeGames: keystore no encontrado ($storeFile) — release quedará sin firmar.")
            return null
        }
        return ReleaseSigning(storeFile, storePassword, keyAlias, keyPassword)
    }

    private data class ReleaseSigning(
        val storeFile: File,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String,
    )

    private companion object {
        const val DEFAULT_VERSION_NAME = "0.1.0"

        /**
         * `MAJOR.MINOR.PATCH` -> `MAJOR*10000 + MINOR*100 + PATCH`. Any pre-release / build suffix
         * (`1.2.0-rc1`) is ignored. Monotonic for MINOR/PATCH < 100, which is plenty for these games.
         */
        fun String.toSemverVersionCode(): Int {
            val (major, minor, patch) = (removePrefix("v").split('.', '-', '+') + listOf("0", "0", "0"))
                .take(3)
                .map { it.toIntOrNull() ?: 0 }
            return major * 10_000 + minor * 100 + patch
        }

        /**
         * Permissions a FreeGames game must never carry. `INTERNET` is the load-bearing one — its
         * absence is the proof the app can't phone home — the rest are the dangerous-level
         * permissions no offline puzzle game has any business requesting.
         */
        val FORBIDDEN_PERMISSIONS = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.GET_ACCOUNTS",
            "android.permission.BLUETOOTH_CONNECT",
        )
    }
}
