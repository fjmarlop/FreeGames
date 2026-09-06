plugins {
    id("freegames.android.game")
}

android {
    // Al copiar esta carpeta: cambia `template` por el nombre del juego en las dos líneas
    // siguientes y en la ruta de paquete src/main/java/es/fjmarlop/freegames/<juego>/.
    namespace = "es.fjmarlop.freegames.template"

    defaultConfig {
        applicationId = "es.fjmarlop.freegames.template"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // versionName / versionCode los pone el convention plugin
        // (por defecto 0.1.0; el workflow de release lo sobrescribe con -PversionName=<tag>).
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
