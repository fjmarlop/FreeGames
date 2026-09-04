# FreeSudoku — Plan 1: Scaffold + Domain Engine

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Stand up the Android project and build the entire pure-Kotlin Sudoku engine — grid model, full-grid generator, logical solver with human techniques, difficulty rater, puzzle carver, campaign curve, and the pure game engine (moves/undo/redo/hints/validation).

**Architecture:** Single Gradle module `:app`, packages by layer. This plan only touches `domain/` (plus scaffold). Everything here is plain Kotlin with **no Android or coroutine dependencies** so it runs as fast JVM unit tests. Data layer (Room/DataStore) is Plan 2; UI is Plan 3.

**Tech Stack:** Kotlin, Gradle Kotlin DSL + version catalog, JUnit4, Google Truth, Hilt (scaffold only — no domain use of it yet). Compose/Room/DataStore are wired in scaffold but exercised in later plans.

**Platform:** Android

---

## Spec reference

Implements §2 (scaffold, package structure), §3 (entire `domain` layer), and §8.1 test items that concern the domain. Deferred to later plans: §4 (data), §5–6 (UI + data flow), §7 items that need repos, §8.2/§8.3.

## Calibration constants (starting values — tests in Tasks 8, 10, 11 tune them)

| Constant | Location | Starting value |
|----------|----------|----------------|
| Technique costs | `Technique.cost` | NakedSingle 10, HiddenSingle 15, LockedCandidates 25, NakedPair 35, NakedTriple 45, HiddenPair 42, HiddenTriple 48, XWing 65 |
| `RATER_W_HARDEST` | `DifficultyRater` | 0.60 |
| `RATER_W_FREQ` | `DifficultyRater` | 0.10 |
| `RATER_W_CLUES` | `DifficultyRater` | 0.80 |
| `RATER_CLUE_PIVOT` | `DifficultyRater` | 32 |
| Band ranges (score) | `DifficultyBand` | PRINCIPIANTE `0..<12`, FACIL `12..<25`, MEDIO `25..<40`, DIFICIL `40..<58`, EXPERTO `58..<78`, MAESTRO `78..∞` |
| `CURVE_BASE` | `CampaignCurve` | 4.0 |
| `CURVE_GROWTH` | `CampaignCurve` | 12.0 |
| `CURVE_MAX` | `CampaignCurve` | 88.0 |
| `CURVE_NOISE` | `CampaignCurve` | 3.0 |
| `CARVE_MAX_ATTEMPTS` | `PuzzleFactory` | 40 |
| `CARVE_TOLERANCE_BASE` | `CampaignCurve.toleranceWindow` | 6.0 |
| `CARVE_TOLERANCE_STEP` | `CampaignCurve.toleranceWindow` | 1.5 |
| `CARVE_TOLERANCE_MAX` | `CampaignCurve.toleranceWindow` | 20.0 |

---

## File map (created by this plan)

```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/freesudoku/app/FreeSudokuApplication.kt
app/src/main/java/com/freesudoku/app/MainActivity.kt
app/src/main/java/com/freesudoku/app/ui/theme/Theme.kt
app/src/main/java/com/freesudoku/app/ui/theme/Color.kt
app/src/main/java/com/freesudoku/app/ui/theme/Type.kt
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml

app/src/main/java/com/freesudoku/app/domain/model/Grid.kt
app/src/main/java/com/freesudoku/app/domain/model/GridCodec.kt
app/src/main/java/com/freesudoku/app/domain/model/DifficultyBand.kt
app/src/main/java/com/freesudoku/app/domain/model/Puzzle.kt
app/src/main/java/com/freesudoku/app/domain/model/Board.kt
app/src/main/java/com/freesudoku/app/domain/model/Move.kt
app/src/main/java/com/freesudoku/app/domain/model/GameSnapshot.kt

app/src/main/java/com/freesudoku/app/domain/generator/FullGridGenerator.kt
app/src/main/java/com/freesudoku/app/domain/generator/PuzzleCarver.kt
app/src/main/java/com/freesudoku/app/domain/generator/PuzzleFactory.kt

app/src/main/java/com/freesudoku/app/domain/solver/SolverState.kt
app/src/main/java/com/freesudoku/app/domain/solver/SolutionCounter.kt
app/src/main/java/com/freesudoku/app/domain/solver/Technique.kt
app/src/main/java/com/freesudoku/app/domain/solver/techniques/NakedSingle.kt
app/src/main/java/com/freesudoku/app/domain/solver/techniques/HiddenSingle.kt
app/src/main/java/com/freesudoku/app/domain/solver/techniques/LockedCandidates.kt
app/src/main/java/com/freesudoku/app/domain/solver/techniques/NakedSubset.kt
app/src/main/java/com/freesudoku/app/domain/solver/techniques/HiddenSubset.kt
app/src/main/java/com/freesudoku/app/domain/solver/techniques/XWing.kt
app/src/main/java/com/freesudoku/app/domain/solver/LogicalSolver.kt
app/src/main/java/com/freesudoku/app/domain/solver/DifficultyRater.kt

app/src/main/java/com/freesudoku/app/domain/campaign/CampaignCurve.kt
app/src/main/java/com/freesudoku/app/domain/game/GameEngine.kt

app/src/test/java/com/freesudoku/app/domain/... (mirrors, per task)
app/src/test/resources/fixtures/*.txt
```

---

## Task 1: Project scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/freesudoku/app/FreeSudokuApplication.kt`, `app/src/main/java/com/freesudoku/app/MainActivity.kt`, theme files, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Generate the Gradle wrapper**

Run (from repo root):
```bash
gradle wrapper --gradle-version 8.13
```
If `gradle` is not installed, create `gradle/wrapper/gradle-wrapper.properties` manually with:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```
and fetch `gradle-wrapper.jar`, `gradlew`, `gradlew.bat` from the Gradle 8.13 distribution.

- [ ] **Step 2: Write `gradle/libs.versions.toml`**

> Executor: bump each version to the latest stable at implementation time; these are known-good floors.

```toml
[versions]
agp = "8.13.0"
kotlin = "2.2.20"
ksp = "2.2.20-2.0.2"
coreKtx = "1.15.0"
lifecycle = "2.9.0"
activityCompose = "1.10.0"
composeBom = "2025.09.00"
navigationCompose = "2.9.0"
hilt = "2.57"
hiltNavigationCompose = "1.2.0"
room = "2.8.0"
datastore = "1.1.7"
kotlinxSerialization = "1.7.3"
coroutines = "1.10.2"
junit = "4.13.2"
truth = "1.4.4"
turbine = "1.2.0"
androidxTestExt = "1.2.1"
androidxTestRunner = "1.6.2"
espresso = "3.6.1"
robolectric = "4.14.1"
mockk = "1.13.13"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExt" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 3: Write root `settings.gradle.kts` and `build.gradle.kts`**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FreeSudoku"
include(":app")
```

`build.gradle.kts` (root):
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
kotlin.code.style=official
ksp.useKSP2=true
```

- [ ] **Step 4: Write `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.freesudoku.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.freesudoku.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.freesudoku.app.HiltTestRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android)
    kspAndroidTest(libs.hilt.compiler)
}
```

`app/proguard-rules.pro`:
```proguard
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.freesudoku.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

- [ ] **Step 5: Write manifest, Application, MainActivity, theme, resources**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".FreeSudokuApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FreeSudoku">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.FreeSudoku">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`FreeSudokuApplication.kt`:
```kotlin
package com.freesudoku.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FreeSudokuApplication : Application()
```

`MainActivity.kt`:
```kotlin
package com.freesudoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.freesudoku.app.ui.theme.FreeSudokuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreeSudokuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    Placeholder(Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Text("FreeSudoku", modifier = modifier)
}
```

`ui/theme/Color.kt`:
```kotlin
package com.freesudoku.app.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF2E6C8E)
val Secondary = Color(0xFF4E7D95)
val Tertiary = Color(0xFF7E5260)
```

`ui/theme/Type.kt`:
```kotlin
package com.freesudoku.app.ui.theme

import androidx.compose.material3.Typography

val Typography = Typography()
```

`ui/theme/Theme.kt`:
```kotlin
package com.freesudoku.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(primary = Primary, secondary = Secondary, tertiary = Tertiary)
private val DarkColors = darkColorScheme(primary = Primary, secondary = Secondary, tertiary = Tertiary)

@Composable
fun FreeSudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

`res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">FreeSudoku</string>
</resources>
```

`res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.FreeSudoku" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

Add launcher icons: run Android Studio's default, or copy `mipmap` placeholders. Minimum: create `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` adaptive icons referencing a solid color drawable, plus `res/drawable/ic_launcher_background.xml` and `res/drawable/ic_launcher_foreground.xml`. (Executor may use `Android Studio > New > Image Asset`.)

- [ ] **Step 6: Add a HiltTestRunner for later plans**

`app/src/androidTest/java/com/freesudoku/app/HiltTestRunner.kt`:
```kotlin
package com.freesudoku.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
```

- [ ] **Step 7: Build**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "chore: scaffold FreeSudoku Android project"
```

---

## Task 2: Grid model + codec

**Files:**
- Create: `domain/model/Grid.kt`, `domain/model/GridCodec.kt`
- Test: `app/src/test/java/com/freesudoku/app/domain/model/GridTest.kt`, `.../GridCodecTest.kt`

- [ ] **Step 1: Write failing tests**

`GridTest.kt`:
```kotlin
package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GridTest {
    @Test fun `empty grid has 81 zero cells`() {
        val g = Grid.empty()
        assertThat(g.cells.size).isEqualTo(81)
        assertThat(g.cells.all { it == 0 }).isTrue()
        assertThat(g.isComplete).isFalse()
    }

    @Test fun `value at row col round trips`() {
        val g = Grid.empty().withValue(row = 3, col = 5, value = 7)
        assertThat(g.valueAt(3, 5)).isEqualTo(7)
        assertThat(g.valueAt(0, 0)).isEqualTo(0)
    }

    @Test fun `boxIndex maps 3x3 blocks`() {
        assertThat(Grid.boxIndex(0, 0)).isEqualTo(0)
        assertThat(Grid.boxIndex(4, 4)).isEqualTo(4)
        assertThat(Grid.boxIndex(8, 8)).isEqualTo(8)
        assertThat(Grid.boxIndex(2, 5)).isEqualTo(1)
    }

    @Test fun `isValidPlacement rejects row column and box conflicts`() {
        val g = Grid.empty().withValue(0, 0, 5)
        assertThat(g.isValidPlacement(0, 8, 5)).isFalse() // same row
        assertThat(g.isValidPlacement(8, 0, 5)).isFalse() // same col
        assertThat(g.isValidPlacement(1, 1, 5)).isFalse() // same box
        assertThat(g.isValidPlacement(4, 4, 5)).isTrue()
    }

    @Test fun `equality is structural`() {
        assertThat(Grid.empty().withValue(1, 1, 9))
            .isEqualTo(Grid.empty().withValue(1, 1, 9))
    }

    @Test fun `isComplete true only when full and consistent`() {
        val solved = GridCodec.decode(SOLVED_81)
        assertThat(solved.isComplete).isTrue()
    }

    private companion object {
        const val SOLVED_81 =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}
```

`GridCodecTest.kt`:
```kotlin
package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.Assert.assertThrows

class GridCodecTest {
    private val puzzle81 =
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

    @Test fun `decode then encode is identity`() {
        assertThat(GridCodec.encode(GridCodec.decode(puzzle81))).isEqualTo(puzzle81)
    }

    @Test fun `decode accepts dots as empty`() {
        val withDots = puzzle81.replace('0', '.')
        assertThat(GridCodec.decode(withDots)).isEqualTo(GridCodec.decode(puzzle81))
    }

    @Test fun `decode rejects wrong length`() {
        assertThrows(IllegalArgumentException::class.java) { GridCodec.decode("123") }
    }
}
```

- [ ] **Step 2: Run — expect FAIL** (`Grid` unresolved)

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.model.*"
```

- [ ] **Step 3: Implement `Grid.kt`**

```kotlin
package com.freesudoku.app.domain.model

/** Immutable 9x9 Sudoku grid. 0 = empty, 1..9 = value. Row-major, index = row*9+col. */
class Grid private constructor(val cells: IntArray) {

    init { require(cells.size == 81) { "Grid needs 81 cells, got ${cells.size}" } }

    fun valueAt(row: Int, col: Int): Int = cells[row * 9 + col]

    fun withValue(row: Int, col: Int, value: Int): Grid {
        require(value in 0..9) { "value must be 0..9" }
        val copy = cells.copyOf()
        copy[row * 9 + col] = value
        return Grid(copy)
    }

    val filledCount: Int get() = cells.count { it != 0 }

    val isFull: Boolean get() = cells.none { it == 0 }

    val isComplete: Boolean
        get() {
            if (!isFull) return false
            for (i in 0 until 9) {
                if (rowDigits(i) != FULL_SET) return false
                if (colDigits(i) != FULL_SET) return false
                if (boxDigits(i) != FULL_SET) return false
            }
            return true
        }

    fun isValidPlacement(row: Int, col: Int, value: Int): Boolean {
        if (value == 0) return true
        for (c in 0 until 9) if (c != col && valueAt(row, c) == value) return false
        for (r in 0 until 9) if (r != row && valueAt(r, col) == value) return false
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if ((r != row || c != col) && valueAt(r, c) == value) return false
        }
        return true
    }

    private fun rowDigits(row: Int): Set<Int> = (0 until 9).map { valueAt(row, it) }.toSet()
    private fun colDigits(col: Int): Set<Int> = (0 until 9).map { valueAt(it, col) }.toSet()
    private fun boxDigits(box: Int): Set<Int> {
        val br = (box / 3) * 3; val bc = (box % 3) * 3
        val out = HashSet<Int>()
        for (r in br until br + 3) for (c in bc until bc + 3) out += valueAt(r, c)
        return out
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Grid && cells.contentEquals(other.cells))
    override fun hashCode(): Int = cells.contentHashCode()

    companion object {
        private val FULL_SET = (1..9).toSet()
        fun empty(): Grid = Grid(IntArray(81))
        fun of(cells: IntArray): Grid = Grid(cells.copyOf())
        fun boxIndex(row: Int, col: Int): Int = (row / 3) * 3 + (col / 3)
    }
}
```

- [ ] **Step 4: Implement `GridCodec.kt`**

```kotlin
package com.freesudoku.app.domain.model

object GridCodec {
    fun decode(text: String): Grid {
        require(text.length == 81) { "Grid string must be 81 chars, got ${text.length}" }
        val cells = IntArray(81)
        for (i in text.indices) {
            val ch = text[i]
            cells[i] = when {
                ch == '.' || ch == '0' -> 0
                ch in '1'..'9' -> ch - '0'
                else -> throw IllegalArgumentException("Invalid char '$ch' at $i")
            }
        }
        return Grid.of(cells)
    }

    fun encode(grid: Grid): String = buildString(81) {
        grid.cells.forEach { append(if (it == 0) '0' else ('0' + it)) }
    }
}
```

- [ ] **Step 5: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.model.*"
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/model app/src/test/java/com/freesudoku/app/domain/model
git commit -m "feat(domain): add Grid model and codec"
```

---

## Task 3: Difficulty band, Puzzle, Board, Move, GameSnapshot

**Files:**
- Create: `domain/model/DifficultyBand.kt`, `Puzzle.kt`, `Board.kt`, `Move.kt`, `GameSnapshot.kt`
- Test: `.../model/DifficultyBandTest.kt`, `.../model/BoardTest.kt`

- [ ] **Step 1: Failing tests**

`DifficultyBandTest.kt`:
```kotlin
package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DifficultyBandTest {
    @Test fun `fromScore maps ranges`() {
        assertThat(DifficultyBand.fromScore(0.0)).isEqualTo(DifficultyBand.PRINCIPIANTE)
        assertThat(DifficultyBand.fromScore(11.9)).isEqualTo(DifficultyBand.PRINCIPIANTE)
        assertThat(DifficultyBand.fromScore(12.0)).isEqualTo(DifficultyBand.FACIL)
        assertThat(DifficultyBand.fromScore(39.9)).isEqualTo(DifficultyBand.MEDIO)
        assertThat(DifficultyBand.fromScore(58.0)).isEqualTo(DifficultyBand.EXPERTO)
        assertThat(DifficultyBand.fromScore(1000.0)).isEqualTo(DifficultyBand.MAESTRO)
    }

    @Test fun `bands are ordered by lowerBound`() {
        val bounds = DifficultyBand.entries.map { it.lowerBound }
        assertThat(bounds).isInOrder()
    }
}
```

`BoardTest.kt`:
```kotlin
package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BoardTest {
    private val givens = GridCodec.decode(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    )
    private val solution = GridCodec.decode(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    )

    @Test fun `board starts from givens with given flags`() {
        val b = Board.fromGivens(givens)
        assertThat(b.cell(0, 0).value).isEqualTo(5)
        assertThat(b.cell(0, 0).isGiven).isTrue()
        assertThat(b.cell(0, 2).value).isEqualTo(0)
        assertThat(b.cell(0, 2).isGiven).isFalse()
    }

    @Test fun `setValue on given cell is rejected`() {
        val b = Board.fromGivens(givens)
        val result = b.withValue(0, 0, 1)
        assertThat(result).isSameInstanceAs(b)
    }

    @Test fun `setValue then clear on non-given cell`() {
        val b = Board.fromGivens(givens).withValue(0, 2, 4)
        assertThat(b.cell(0, 2).value).isEqualTo(4)
        val cleared = b.withValue(0, 2, 0)
        assertThat(cleared.cell(0, 2).value).isEqualTo(0)
    }

    @Test fun `notes toggle`() {
        val b = Board.fromGivens(givens).withNoteToggled(0, 2, 4).withNoteToggled(0, 2, 7)
        assertThat(b.cell(0, 2).notes).containsExactly(4, 7)
        val b2 = b.withNoteToggled(0, 2, 4)
        assertThat(b2.cell(0, 2).notes).containsExactly(7)
    }

    @Test fun `matchesSolution true when board equals solution`() {
        val b = Board.fromGivens(givens)
        assertThat(b.toGrid()).isNotEqualTo(solution)
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.model.*"
```

- [ ] **Step 3: Implement models**

`DifficultyBand.kt`:
```kotlin
package com.freesudoku.app.domain.model

enum class DifficultyBand(val lowerBound: Double) {
    PRINCIPIANTE(0.0),
    FACIL(12.0),
    MEDIO(25.0),
    DIFICIL(40.0),
    EXPERTO(58.0),
    MAESTRO(78.0);

    companion object {
        fun fromScore(score: Double): DifficultyBand =
            entries.last { score >= it.lowerBound }
    }
}
```

`Puzzle.kt`:
```kotlin
package com.freesudoku.app.domain.model

/**
 * A generated puzzle. [number] is the campaign index once assigned (null while in the buffer).
 */
data class Puzzle(
    val id: String,
    val number: Int?,
    val givens: Grid,
    val solution: Grid,
    val difficultyScore: Double,
    val band: DifficultyBand,
) {
    init {
        require(solution.isComplete) { "solution must be a complete valid grid" }
        require(givens.filledCount in 17..80) { "givens out of sane range" }
    }
}
```

`Move.kt`:
```kotlin
package com.freesudoku.app.domain.model

/** A reversible edit to the board. */
sealed interface Move {
    val row: Int
    val col: Int

    data class SetValue(
        override val row: Int,
        override val col: Int,
        val value: Int,
        val previousValue: Int,
        val previousNotes: Set<Int>,
    ) : Move

    data class ToggleNote(
        override val row: Int,
        override val col: Int,
        val digit: Int,
        val added: Boolean,
    ) : Move

    data class ClearCell(
        override val row: Int,
        override val col: Int,
        val previousValue: Int,
        val previousNotes: Set<Int>,
    ) : Move
}
```

`Board.kt`:
```kotlin
package com.freesudoku.app.domain.model

data class BoardCell(
    val value: Int,
    val isGiven: Boolean,
    val notes: Set<Int> = emptySet(),
)

class Board private constructor(private val grid: List<BoardCell>) {

    fun cell(row: Int, col: Int): BoardCell = grid[row * 9 + col]

    fun withValue(row: Int, col: Int, value: Int): Board {
        val idx = row * 9 + col
        if (grid[idx].isGiven) return this
        val next = grid.toMutableList()
        next[idx] = next[idx].copy(value = value, notes = emptySet())
        return Board(next)
    }

    fun withNotes(row: Int, col: Int, notes: Set<Int>): Board {
        val idx = row * 9 + col
        if (grid[idx].isGiven || grid[idx].value != 0) return this
        val next = grid.toMutableList()
        next[idx] = next[idx].copy(notes = notes.toSortedSet())
        return Board(next)
    }

    fun withNoteToggled(row: Int, col: Int, digit: Int): Board {
        val current = cell(row, col).notes
        val updated = if (digit in current) current - digit else current + digit
        return withNotes(row, col, updated)
    }

    fun toGrid(): Grid {
        val cells = IntArray(81) { grid[it].value }
        return Grid.of(cells)
    }

    val isComplete: Boolean get() = toGrid().isComplete

    fun emptyCells(): List<Pair<Int, Int>> = buildList {
        for (r in 0 until 9) for (c in 0 until 9) if (cell(r, c).value == 0) add(r to c)
    }

    companion object {
        fun fromGivens(givens: Grid): Board = Board(
            List(81) { i ->
                val v = givens.cells[i]
                BoardCell(value = v, isGiven = v != 0)
            }
        )

        fun restore(cells: List<BoardCell>): Board {
            require(cells.size == 81)
            return Board(cells)
        }
    }

    fun cells(): List<BoardCell> = grid
}
```

`GameSnapshot.kt`:
```kotlin
package com.freesudoku.app.domain.model

enum class GameStatus { IN_PROGRESS, COMPLETED, FAILED }

/** The single source of truth for an in-progress game. Pure data; persisted by Plan 2. */
data class GameSnapshot(
    val puzzle: Puzzle,
    val board: Board,
    val undoStack: List<Move>,
    val redoStack: List<Move>,
    val elapsedMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val status: GameStatus,
    val mistakeLimitEnabled: Boolean,
) {
    companion object {
        const val MISTAKE_LIMIT = 3
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.model.*"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/model app/src/test/java/com/freesudoku/app/domain/model
git commit -m "feat(domain): add band, puzzle, board, move, snapshot models"
```

---

## Task 4: FullGridGenerator

**Files:**
- Create: `domain/generator/FullGridGenerator.kt`
- Test: `.../generator/FullGridGeneratorTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.model.Grid
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class FullGridGeneratorTest {

    @Test fun `generates a complete valid grid`() {
        val grid = FullGridGenerator(Random(1)).generate()
        assertThat(grid.isComplete).isTrue()
    }

    @Test fun `is deterministic for a fixed seed`() {
        val a = FullGridGenerator(Random(42)).generate()
        val b = FullGridGenerator(Random(42)).generate()
        assertThat(a).isEqualTo(b)
    }

    @Test fun `different seeds usually differ`() {
        val a = FullGridGenerator(Random(1)).generate()
        val b = FullGridGenerator(Random(2)).generate()
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun `every row column and box is a permutation of 1_9`() {
        val g = FullGridGenerator(Random(7)).generate()
        for (i in 0 until 9) {
            assertThat((0 until 9).map { g.valueAt(i, it) }.toSet()).isEqualTo((1..9).toSet())
            assertThat((0 until 9).map { g.valueAt(it, i) }.toSet()).isEqualTo((1..9).toSet())
        }
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.generator.FullGridGeneratorTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.model.Grid
import kotlin.random.Random

/** Produces a complete, valid 9x9 solution grid via randomized backtracking. */
class FullGridGenerator(private val random: Random = Random.Default) {

    fun generate(): Grid {
        val cells = IntArray(81)
        check(fill(cells, 0)) { "backtracking failed to fill a full grid" }
        return Grid.of(cells)
    }

    private fun fill(cells: IntArray, index: Int): Boolean {
        if (index == 81) return true
        val row = index / 9
        val col = index % 9
        val candidates = (1..9).shuffled(random)
        for (v in candidates) {
            if (canPlace(cells, row, col, v)) {
                cells[index] = v
                if (fill(cells, index + 1)) return true
                cells[index] = 0
            }
        }
        return false
    }

    private fun canPlace(cells: IntArray, row: Int, col: Int, v: Int): Boolean {
        for (c in 0 until 9) if (cells[row * 9 + c] == v) return false
        for (r in 0 until 9) if (cells[r * 9 + col] == v) return false
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if (cells[r * 9 + c] == v) return false
        }
        return true
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.generator.FullGridGeneratorTest"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/generator/FullGridGenerator.kt app/src/test/java/com/freesudoku/app/domain/generator/FullGridGeneratorTest.kt
git commit -m "feat(domain): add full grid generator"
```

---

## Task 5: SolverState + SolutionCounter

**Files:**
- Create: `domain/solver/SolverState.kt`, `domain/solver/SolutionCounter.kt`
- Test: `.../solver/SolutionCounterTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.GridCodec
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SolutionCounterTest {
    @Test fun `unique puzzle counts exactly one`() {
        val unique = GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        )
        assertThat(SolutionCounter().countUpTo(unique, limit = 2)).isEqualTo(1)
    }

    @Test fun `empty grid has many solutions and stops at limit`() {
        val empty = GridCodec.decode("0".repeat(81))
        assertThat(SolutionCounter().countUpTo(empty, limit = 2)).isEqualTo(2)
    }

    @Test fun `puzzle with two solutions counts two`() {
        // Known 2-solution grid (last clue of a unique puzzle removed).
        val ambiguous = GridCodec.decode(
            "000070000600195000098000060800060003400803001700020006060000280000419005000080079"
        )
        assertThat(SolutionCounter().countUpTo(ambiguous, limit = 2)).isEqualTo(2)
    }

    @Test fun `solve returns the unique completion`() {
        val unique = GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        )
        val solved = SolutionCounter().solve(unique)!!
        assertThat(solved.isComplete).isTrue()
        assertThat(solved.valueAt(0, 2)).isEqualTo(4)
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.SolutionCounterTest"
```

- [ ] **Step 3: Implement `SolverState.kt`**

```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.Grid

/**
 * Mutable candidate model for solving. [values] holds fixed digits (0 = empty).
 * [candidates] is a bitmask per cell; bit (d-1) set means digit d is possible.
 */
class SolverState private constructor(
    val values: IntArray,
    val candidates: IntArray,
) {
    fun copy(): SolverState = SolverState(values.copyOf(), candidates.copyOf())

    fun isSolved(): Boolean = values.none { it == 0 }

    fun candidateList(index: Int): List<Int> =
        (1..9).filter { candidates[index] and (1 shl (it - 1)) != 0 }

    fun candidateCount(index: Int): Int = Integer.bitCount(candidates[index])

    /** Assigns [digit] to [index], propagating elimination to peers. Returns false on contradiction. */
    fun assign(index: Int, digit: Int): Boolean {
        val others = candidateList(index).filter { it != digit }
        for (d in others) if (!eliminate(index, d)) return false
        return true
    }

    /** Removes [digit] from [index]'s candidates; if it collapses to one, assigns it. */
    fun eliminate(index: Int, digit: Int): Boolean {
        val bit = 1 shl (digit - 1)
        if (candidates[index] and bit == 0) return true
        candidates[index] = candidates[index] and bit.inv()
        return when (candidateCount(index)) {
            0 -> false
            1 -> {
                val last = candidateList(index).first()
                place(index, last)
                peers(index).all { eliminate(it, last) }
            }
            else -> true
        }
    }

    private fun place(index: Int, digit: Int) {
        values[index] = digit
        candidates[index] = 1 shl (digit - 1)
    }

    fun toGrid(): Grid = Grid.of(values)

    companion object {
        private val PEERS: Array<IntArray> = Array(81) { idx ->
            val row = idx / 9
            val col = idx % 9
            val set = LinkedHashSet<Int>()
            for (c in 0 until 9) if (c != col) set += row * 9 + c
            for (r in 0 until 9) if (r != row) set += r * 9 + col
            val br = (row / 3) * 3
            val bc = (col / 3) * 3
            for (r in br until br + 3) for (c in bc until bc + 3) {
                val p = r * 9 + c
                if (p != idx) set += p
            }
            set.toIntArray()
        }

        val UNITS: Array<IntArray> = buildList {
            for (r in 0 until 9) add(IntArray(9) { r * 9 + it })
            for (c in 0 until 9) add(IntArray(9) { it * 9 + c })
            for (b in 0 until 9) {
                val br = (b / 3) * 3; val bc = (b % 3) * 3
                add(IntArray(9) { k -> (br + k / 3) * 9 + (bc + k % 3) })
            }
        }.toTypedArray()

        fun peers(index: Int): IntArray = PEERS[index]

        fun from(grid: Grid): SolverState? {
            val state = SolverState(IntArray(81), IntArray(81) { 0x1FF })
            for (i in 0 until 81) {
                val v = grid.cells[i]
                if (v != 0) {
                    if (state.candidates[i] and (1 shl (v - 1)) == 0) return null
                    state.place(i, v)
                }
            }
            for (i in 0 until 81) {
                val v = state.values[i]
                if (v != 0 && !SolverState.peers(i).all { state.eliminate(it, v) }) return null
            }
            return state
        }
    }
}

private fun SolverState.peers(index: Int) = SolverState.peers(index)
```

- [ ] **Step 4: Implement `SolutionCounter.kt`**

```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.Grid

/** Counts solutions (bounded) and can return the unique completion. */
class SolutionCounter {

    fun countUpTo(grid: Grid, limit: Int): Int {
        val state = SolverState.from(grid) ?: return 0
        return search(state, limit, IntArray(1))
    }

    fun solve(grid: Grid): Grid? {
        val state = SolverState.from(grid) ?: return null
        val holder = arrayOfNulls<Grid>(1)
        search(state, limit = 1, count = IntArray(1), capture = holder)
        return holder[0]
    }

    private fun search(state: SolverState, limit: Int, count: IntArray, capture: Array<Grid?>? = null): Int {
        if (state.isSolved()) {
            count[0]++
            if (capture != null && capture[0] == null) capture[0] = state.toGrid()
            return count[0]
        }
        // pick the empty cell with the fewest candidates (MRV)
        var best = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (state.values[i] == 0) {
                val c = state.candidateCount(i)
                if (c < bestCount) { bestCount = c; best = i; if (c == 2) break }
            }
        }
        if (best == -1 || bestCount == 0) return count[0]
        for (d in state.candidateList(best)) {
            val branch = state.copy()
            if (branch.assign(best, d)) {
                search(branch, limit, count, capture)
                if (count[0] >= limit) return count[0]
            }
        }
        return count[0]
    }
}
```

- [ ] **Step 5: Run — expect PASS.** If the `ambiguous` fixture in the test does not actually have 2 solutions, replace it: take the `unique` string, and for each non-`0` position, blank it, run `countUpTo(.., 2)`, and pick the first that yields 2. Hard-code that string into the test.

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.SolutionCounterTest"
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/solver app/src/test/java/com/freesudoku/app/domain/solver
git commit -m "feat(domain): add solver state and bounded solution counter"
```

---

## Task 6: Technique interface + NakedSingle + HiddenSingle

**Files:**
- Create: `domain/solver/Technique.kt`, `domain/solver/techniques/NakedSingle.kt`, `domain/solver/techniques/HiddenSingle.kt`
- Test: `.../solver/techniques/SinglesTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SinglesTest {
    private fun state(p: String) = SolverState.from(GridCodec.decode(p))!!

    @Test fun `naked single finds the only candidate in a cell`() {
        val s = state("530070000600195000098000060800060003400803001700020006060000280000419005000080079")
        val step = NakedSingle().apply(s)
        assertThat(step).isNotNull()
        assertThat(step!!.placements).isNotEmpty()
        val (idx, digit) = step.placements.first()
        assertThat(s.candidateList(idx)).containsExactly(digit)
    }

    @Test fun `hidden single finds digit that fits one cell in a unit`() {
        val s = state("000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        // craft: put 1..8 around so digit 9 is a hidden single in r0c0's box
        val crafted = state("020000000000000000000000000900000000000900000000000000000000000000000000000000009")
        val step = HiddenSingle().apply(crafted)
        assertThat(step).isNotNull()
    }

    @Test fun `technique costs are ordered`() {
        assertThat(NakedSingle().cost).isLessThan(HiddenSingle().cost)
    }

    @Test fun `apply returns null when technique does not fire`() {
        val solved = state("534678912672195348198342567859761423426853791713924856961537284287419635345286179")
        assertThat(NakedSingle().apply(solved)).isNull()
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.techniques.SinglesTest"
```

- [ ] **Step 3: Implement `Technique.kt`**

```kotlin
package com.freesudoku.app.domain.solver

/** A single deduction step: which digits get placed, which candidates get removed. */
data class TechniqueStep(
    val technique: TechniqueId,
    val placements: List<Pair<Int, Int>> = emptyList(),   // (cellIndex, digit)
    val eliminations: List<Pair<Int, Int>> = emptyList(),  // (cellIndex, digit)
) {
    val isProgress: Boolean get() = placements.isNotEmpty() || eliminations.isNotEmpty()
}

enum class TechniqueId { NAKED_SINGLE, HIDDEN_SINGLE, LOCKED_CANDIDATES, NAKED_SUBSET, HIDDEN_SUBSET, X_WING }

interface Technique {
    val id: TechniqueId
    val cost: Int
    /** Applies the technique to [state] in place, returning the step taken or null if it did not fire. */
    fun apply(state: SolverState): TechniqueStep?
}
```

- [ ] **Step 4: Implement `NakedSingle.kt`**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

class NakedSingle : Technique {
    override val id = TechniqueId.NAKED_SINGLE
    override val cost = 10

    override fun apply(state: SolverState): TechniqueStep? {
        for (i in 0 until 81) {
            if (state.values[i] == 0 && state.candidateCount(i) == 1) {
                val d = state.candidateList(i).first()
                if (!state.assign(i, d)) return null
                return TechniqueStep(id, placements = listOf(i to d))
            }
        }
        return null
    }
}
```

- [ ] **Step 5: Implement `HiddenSingle.kt`**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

class HiddenSingle : Technique {
    override val id = TechniqueId.HIDDEN_SINGLE
    override val cost = 15

    override fun apply(state: SolverState): TechniqueStep? {
        for (unit in SolverState.UNITS) {
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val slots = unit.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                if (slots.size == 1 && state.values[slots[0]] == 0) {
                    val idx = slots[0]
                    if (!state.assign(idx, d)) return null
                    return TechniqueStep(id, placements = listOf(idx to d))
                }
            }
        }
        return null
    }
}
```

- [ ] **Step 6: Run — expect PASS.** If the crafted `HiddenSingle` fixture is fiddly, replace it with this deterministic construction in the test: start from the solved grid string, blank the whole first box except keep enough peers of `r0c0` in row 0 / col 0 so exactly one digit remains for `r0c0`. Concretely use `state("534678912600000000100000000800000000400000000700000000900000000200000000300000000")` and assert a placement at index 0.

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.techniques.SinglesTest"
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/solver app/src/test/java/com/freesudoku/app/domain/solver
git commit -m "feat(domain): add Technique interface and singles"
```

- [ ] **Step 8: Build checkpoint**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

---

## Task 7: Advanced techniques — LockedCandidates, NakedSubset, HiddenSubset, XWing

**Files:**
- Create: `domain/solver/techniques/LockedCandidates.kt`, `NakedSubset.kt`, `HiddenSubset.kt`, `XWing.kt`
- Test: `.../solver/techniques/AdvancedTechniquesTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.solver.SolverState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdvancedTechniquesTest {
    private fun state(p: String) = SolverState.from(GridCodec.decode(p))!!

    @Test fun `locked candidates eliminates along a line`() {
        // Fixture where digit is confined to one row within a box -> eliminated elsewhere in that row.
        val s = state(FIXTURE_LOCKED)
        val step = LockedCandidates().apply(s)
        assertThat(step).isNotNull()
        assertThat(step!!.eliminations).isNotEmpty()
    }

    @Test fun `naked pair removes candidates from the rest of the unit`() {
        val s = state(FIXTURE_NAKED_PAIR)
        val step = NakedSubset().apply(s)
        assertThat(step).isNotNull()
        assertThat(step!!.eliminations).isNotEmpty()
    }

    @Test fun `hidden pair collapses two cells to two candidates`() {
        val s = state(FIXTURE_HIDDEN_PAIR)
        val step = HiddenSubset().apply(s)
        assertThat(step).isNotNull()
    }

    @Test fun `x-wing eliminates candidates in columns`() {
        val s = state(FIXTURE_XWING)
        val step = XWing().apply(s)
        assertThat(step).isNotNull()
        assertThat(step!!.eliminations).isNotEmpty()
    }

    private companion object {
        // Executor: fill these from https://www.sudokuwiki.org strategy example grids
        // (81-char strings). Each must be a valid partial with the target pattern present.
        const val FIXTURE_LOCKED = "984000000000500000000000000000000000000000000000000000000000000000000000000000000"
        const val FIXTURE_NAKED_PAIR = "000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        const val FIXTURE_HIDDEN_PAIR = "000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        const val FIXTURE_XWING = "000000000000000000000000000000000000000000000000000000000000000000000000000000000"
    }
}
```

> **Executor note:** the placeholder fixture strings above MUST be replaced with real strategy-example grids before the test is meaningful. Source them from sudokuwiki.org's solver examples (Pointing Pairs, Naked Pairs, Hidden Pairs, X-Wing pages each show an 81-char grid). Verify each fixture: load it, run `LogicalSolver` up to but excluding the target technique, then assert the target technique fires. Do not mark this task done with placeholder fixtures.

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.techniques.AdvancedTechniquesTest"
```

- [ ] **Step 3: Implement `LockedCandidates.kt`**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/** Pointing (box -> line) and claiming (line -> box). */
class LockedCandidates : Technique {
    override val id = TechniqueId.LOCKED_CANDIDATES
    override val cost = 25

    override fun apply(state: SolverState): TechniqueStep? {
        val boxes = (0 until 9).map { b ->
            val br = (b / 3) * 3; val bc = (b % 3) * 3
            IntArray(9) { k -> (br + k / 3) * 9 + (bc + k % 3) }
        }
        for (b in 0 until 9) {
            val box = boxes[b]
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val slots = box.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                if (slots.size < 2) continue
                val rows = slots.map { it / 9 }.toSet()
                val cols = slots.map { it % 9 }.toSet()
                if (rows.size == 1) {
                    val row = rows.first()
                    val elim = (0 until 9).map { row * 9 + it }
                        .filter { it !in slots && state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return applyElim(state, elim, d)
                }
                if (cols.size == 1) {
                    val col = cols.first()
                    val elim = (0 until 9).map { it * 9 + col }
                        .filter { it !in slots && state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return applyElim(state, elim, d)
                }
            }
        }
        // claiming: line -> box
        for (line in SolverState.UNITS.take(18)) {
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val slots = line.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                if (slots.size < 2) continue
                val boxesOf = slots.map { (it / 9 / 3) * 3 + (it % 9 / 3) }.toSet()
                if (boxesOf.size == 1) {
                    val boxIdx = boxesOf.first()
                    val elim = boxes[boxIdx]
                        .filter { it !in slots && state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return applyElim(state, elim, d)
                }
            }
        }
        return null
    }

    private fun applyElim(state: SolverState, cells: List<Int>, d: Int): TechniqueStep? {
        for (c in cells) if (!state.eliminate(c, d)) return null
        return TechniqueStep(id, eliminations = cells.map { it to d })
    }
}
```

- [ ] **Step 4: Implement `NakedSubset.kt`** (pairs + triples)

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

class NakedSubset : Technique {
    override val id = TechniqueId.NAKED_SUBSET
    override val cost = 40

    override fun apply(state: SolverState): TechniqueStep? {
        for (size in 2..3) {
            for (unit in SolverState.UNITS) {
                val open = unit.filter { state.values[it] == 0 }
                val combos = combinations(open, size)
                for (combo in combos) {
                    val union = combo.fold(0) { acc, i -> acc or state.candidates[i] }
                    if (Integer.bitCount(union) == size) {
                        val targets = unit.filter {
                            it !in combo && state.values[it] == 0 && state.candidates[it] and union != 0
                        }
                        val elims = buildList {
                            for (t in targets) for (d in 1..9) {
                                val bit = 1 shl (d - 1)
                                if (union and bit != 0 && state.candidates[t] and bit != 0) add(t to d)
                            }
                        }
                        if (elims.isNotEmpty()) {
                            for ((c, d) in elims) if (!state.eliminate(c, d)) return null
                            return TechniqueStep(id, eliminations = elims)
                        }
                    }
                }
            }
        }
        return null
    }
}

internal fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())
    if (items.size < k) return emptyList()
    val head = items.first()
    val tail = items.drop(1)
    return combinations(tail, k - 1).map { listOf(head) + it } + combinations(tail, k)
}
```

- [ ] **Step 5: Implement `HiddenSubset.kt`**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

class HiddenSubset : Technique {
    override val id = TechniqueId.HIDDEN_SUBSET
    override val cost = 48

    override fun apply(state: SolverState): TechniqueStep? {
        for (size in 2..3) {
            for (unit in SolverState.UNITS) {
                val digitSlots = (1..9).associateWith { d ->
                    val bit = 1 shl (d - 1)
                    unit.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                }.filterValues { it.isNotEmpty() }
                val digits = digitSlots.keys.toList()
                for (combo in combinations(digits, size)) {
                    val cells = combo.flatMap { digitSlots.getValue(it) }.toSet()
                    if (cells.size == size && combo.all { digitSlots.getValue(it).all { c -> c in cells } }) {
                        val keepMask = combo.fold(0) { acc, d -> acc or (1 shl (d - 1)) }
                        val elims = buildList {
                            for (c in cells) for (d in 1..9) {
                                val bit = 1 shl (d - 1)
                                if (keepMask and bit == 0 && state.candidates[c] and bit != 0) add(c to d)
                            }
                        }
                        if (elims.isNotEmpty()) {
                            for ((c, d) in elims) if (!state.eliminate(c, d)) return null
                            return TechniqueStep(id, eliminations = elims)
                        }
                    }
                }
            }
        }
        return null
    }
}
```

- [ ] **Step 6: Implement `XWing.kt`**

```kotlin
package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

class XWing : Technique {
    override val id = TechniqueId.X_WING
    override val cost = 65

    override fun apply(state: SolverState): TechniqueStep? {
        for (d in 1..9) {
            val bit = 1 shl (d - 1)
            // row-based
            val rowSlots = (0 until 9).map { r ->
                (0 until 9).filter { c -> state.values[r * 9 + c] == 0 && state.candidates[r * 9 + c] and bit != 0 }
            }
            for (r1 in 0 until 9) for (r2 in r1 + 1 until 9) {
                if (rowSlots[r1].size == 2 && rowSlots[r1] == rowSlots[r2]) {
                    val (c1, c2) = rowSlots[r1]
                    val elim = (0 until 9)
                        .filter { it != r1 && it != r2 }
                        .flatMap { r -> listOf(r * 9 + c1, r * 9 + c2) }
                        .filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) {
                        for (c in elim) if (!state.eliminate(c, d)) return null
                        return TechniqueStep(id, eliminations = elim.map { it to d })
                    }
                }
            }
            // column-based
            val colSlots = (0 until 9).map { c ->
                (0 until 9).filter { r -> state.values[r * 9 + c] == 0 && state.candidates[r * 9 + c] and bit != 0 }
            }
            for (c1 in 0 until 9) for (c2 in c1 + 1 until 9) {
                if (colSlots[c1].size == 2 && colSlots[c1] == colSlots[c2]) {
                    val (r1, r2) = colSlots[c1]
                    val elim = (0 until 9)
                        .filter { it != c1 && it != c2 }
                        .flatMap { c -> listOf(r1 * 9 + c, r2 * 9 + c) }
                        .filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) {
                        for (c in elim) if (!state.eliminate(c, d)) return null
                        return TechniqueStep(id, eliminations = elim.map { it to d })
                    }
                }
            }
        }
        return null
    }
}
```

- [ ] **Step 7: Run — expect PASS** (after real fixtures are in)

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.techniques.AdvancedTechniquesTest"
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/solver app/src/test/java/com/freesudoku/app/domain/solver
git commit -m "feat(domain): add locked candidates, subsets, x-wing"
```

---

## Task 8: LogicalSolver + DifficultyRater

**Files:**
- Create: `domain/solver/LogicalSolver.kt`, `domain/solver/DifficultyRater.kt`
- Test: `.../solver/LogicalSolverTest.kt`, `.../solver/DifficultyRaterTest.kt`
- Test resources: `app/src/test/resources/fixtures/rated_puzzles.csv`

- [ ] **Step 1: Add fixture file**

`app/src/test/resources/fixtures/rated_puzzles.csv` — CSV `givens81,expectedBand`. Executor populates with ~18 puzzles (3 per band) from a known source (e.g. exported from an existing rated set, or hand-verified). Example rows:
```
530070000600195000098000060800060003400803001700020006060000280000419005000080079,FACIL
...
```

- [ ] **Step 2: Failing tests**

`LogicalSolverTest.kt`:
```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.GridCodec
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogicalSolverTest {
    @Test fun `solves an easy puzzle with singles only`() {
        val grid = GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        )
        val result = LogicalSolver().solve(grid)
        assertThat(result.solved).isTrue()
        assertThat(result.finalGrid.isComplete).isTrue()
        assertThat(result.steps.map { it.technique }.toSet())
            .containsAnyOf(TechniqueId.NAKED_SINGLE, TechniqueId.HIDDEN_SINGLE)
    }

    @Test fun `reports unsolved when logic runs out`() {
        val hard = GridCodec.decode(
            "000000010400000000020000000000050407008000300001090000300400200050100000000806000"
        )
        val result = LogicalSolver().solve(hard)
        // may or may not solve with MVP technique set; must not crash and must be consistent
        assertThat(result.finalGrid.filledCount).isAtLeast(hard.filledCount)
    }

    @Test fun `hardest technique is recorded`() {
        val grid = GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        )
        val result = LogicalSolver().solve(grid)
        assertThat(result.hardestTechnique).isNotNull()
    }
}
```

`DifficultyRaterTest.kt`:
```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GridCodec
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DifficultyRaterTest {
    private val rater = DifficultyRater(LogicalSolver())

    @Test fun `score is non-negative and finite`() {
        val r = rater.rate(GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        ))
        assertThat(r.score).isAtLeast(0.0)
        assertThat(r.score.isFinite()).isTrue()
    }

    @Test fun `harder puzzle scores higher than easier one`() {
        val easy = rater.rate(GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        ))
        val hard = rater.rate(GridCodec.decode(
            "000000010400000000020000000000050407008000300001090000300400200050100000000806000"
        ))
        assertThat(hard.score).isGreaterThan(easy.score)
    }

    @Test fun `band matches score`() {
        val r = rater.rate(GridCodec.decode(
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        ))
        assertThat(r.band).isEqualTo(DifficultyBand.fromScore(r.score))
    }

    @Test fun `rated fixtures land in the expected band or an adjacent one`() {
        val lines = javaClass.getResourceAsStream("/fixtures/rated_puzzles.csv")!!
            .bufferedReader().readLines().filter { it.isNotBlank() }
        var withinOne = 0
        for (line in lines) {
            val (givens, expected) = line.split(",")
            val got = rater.rate(GridCodec.decode(givens)).band
            val gap = kotlin.math.abs(got.ordinal - DifficultyBand.valueOf(expected).ordinal)
            if (gap <= 1) withinOne++
        }
        assertThat(withinOne.toDouble() / lines.size).isAtLeast(0.8)
    }
}
```

- [ ] **Step 3: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.LogicalSolverTest" --tests "com.freesudoku.app.domain.solver.DifficultyRaterTest"
```

- [ ] **Step 4: Implement `LogicalSolver.kt`**

```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.Grid
import com.freesudoku.app.domain.solver.techniques.HiddenSingle
import com.freesudoku.app.domain.solver.techniques.HiddenSubset
import com.freesudoku.app.domain.solver.techniques.LockedCandidates
import com.freesudoku.app.domain.solver.techniques.NakedSingle
import com.freesudoku.app.domain.solver.techniques.NakedSubset
import com.freesudoku.app.domain.solver.techniques.XWing

data class SolveResult(
    val solved: Boolean,
    val finalGrid: Grid,
    val steps: List<TechniqueStep>,
) {
    val techniqueCounts: Map<TechniqueId, Int> =
        steps.groupingBy { it.technique }.eachCount()
    val hardestTechnique: TechniqueId? =
        steps.maxByOrNull { TECHNIQUE_COST.getValue(it.technique) }?.technique
}

val TECHNIQUE_COST: Map<TechniqueId, Int> = mapOf(
    TechniqueId.NAKED_SINGLE to 10,
    TechniqueId.HIDDEN_SINGLE to 15,
    TechniqueId.LOCKED_CANDIDATES to 25,
    TechniqueId.NAKED_SUBSET to 40,
    TechniqueId.HIDDEN_SUBSET to 48,
    TechniqueId.X_WING to 65,
)

class LogicalSolver(
    private val techniques: List<Technique> = listOf(
        NakedSingle(), HiddenSingle(), LockedCandidates(), NakedSubset(), HiddenSubset(), XWing(),
    ).sortedBy { it.cost },
) {
    fun solve(grid: Grid): SolveResult {
        val state = SolverState.from(grid)
            ?: return SolveResult(false, grid, emptyList())
        val steps = mutableListOf<TechniqueStep>()
        var guard = 0
        while (!state.isSolved() && guard++ < 500) {
            val step = techniques.firstNotNullOfOrNull { it.apply(state) }
            if (step == null || !step.isProgress) break
            steps += step
        }
        return SolveResult(state.isSolved(), state.toGrid(), steps)
    }
}
```

- [ ] **Step 5: Implement `DifficultyRater.kt`**

```kotlin
package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.Grid
import kotlin.math.max

data class Rating(
    val score: Double,
    val band: DifficultyBand,
    val solvedByLogic: Boolean,
    val hardestTechnique: TechniqueId?,
)

class DifficultyRater(private val solver: LogicalSolver = LogicalSolver()) {

    fun rate(givens: Grid): Rating {
        val result = solver.solve(givens)
        val hardestCost = result.hardestTechnique?.let { TECHNIQUE_COST.getValue(it) } ?: 0
        val freqComponent = result.techniqueCounts.entries.sumOf { (id, n) ->
            n * (TECHNIQUE_COST.getValue(id) / 100.0)
        }
        val clueComponent = max(0.0, (RATER_CLUE_PIVOT - givens.filledCount).toDouble())
        var score = RATER_W_HARDEST * hardestCost +
            RATER_W_FREQ * freqComponent +
            RATER_W_CLUES * clueComponent
        if (!result.solved) score += UNSOLVED_PENALTY
        return Rating(
            score = score,
            band = DifficultyBand.fromScore(score),
            solvedByLogic = result.solved,
            hardestTechnique = result.hardestTechnique,
        )
    }

    companion object {
        const val RATER_W_HARDEST = 0.60
        const val RATER_W_FREQ = 0.10
        const val RATER_W_CLUES = 0.80
        const val RATER_CLUE_PIVOT = 32
        const val UNSOLVED_PENALTY = 25.0
    }
}
```

- [ ] **Step 6: Run — expect PASS.** If the fixture accuracy assertion fails, tune the `RATER_W_*` constants and band bounds (documented in the Calibration table) until ≥ 80% land within one band. Record the final values back into the spec's §10.

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.*"
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/solver app/src/test/java/com/freesudoku/app/domain/solver app/src/test/resources/fixtures
git commit -m "feat(domain): add logical solver and difficulty rater"
```

---

## Task 9: PuzzleCarver

**Files:**
- Create: `domain/generator/PuzzleCarver.kt`
- Test: `.../generator/PuzzleCarverTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.solver.SolutionCounter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class PuzzleCarverTest {

    @Test fun `carved puzzle keeps a unique solution`() {
        val full = FullGridGenerator(Random(3)).generate()
        val carved = PuzzleCarver(Random(3)).carve(full)
        assertThat(SolutionCounter().countUpTo(carved.givens, limit = 2)).isEqualTo(1)
        assertThat(carved.solution).isEqualTo(full)
    }

    @Test fun `carved puzzle has fewer clues than a full grid`() {
        val full = FullGridGenerator(Random(5)).generate()
        val carved = PuzzleCarver(Random(5)).carve(full)
        assertThat(carved.givens.filledCount).isLessThan(81)
        assertThat(carved.givens.filledCount).isAtLeast(17)
    }

    @Test fun `deterministic for a fixed seed`() {
        val full = FullGridGenerator(Random(9)).generate()
        val a = PuzzleCarver(Random(11)).carve(full)
        val b = PuzzleCarver(Random(11)).carve(full)
        assertThat(a.givens).isEqualTo(b.givens)
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.generator.PuzzleCarverTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.model.Grid
import com.freesudoku.app.domain.solver.SolutionCounter
import kotlin.random.Random

data class CarvedPuzzle(val givens: Grid, val solution: Grid)

/** Removes clues from a complete grid, greedily, keeping the solution unique. */
class PuzzleCarver(
    private val random: Random = Random.Default,
    private val counter: SolutionCounter = SolutionCounter(),
) {
    fun carve(solution: Grid): CarvedPuzzle {
        require(solution.isComplete) { "carve() needs a complete grid" }
        var current = solution
        val order = (0 until 81).shuffled(random)
        for (idx in order) {
            val row = idx / 9
            val col = idx % 9
            if (current.valueAt(row, col) == 0) continue
            val candidate = current.withValue(row, col, 0)
            if (counter.countUpTo(candidate, limit = 2) == 1) {
                current = candidate
            }
        }
        return CarvedPuzzle(givens = current, solution = solution)
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.generator.PuzzleCarverTest"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/generator/PuzzleCarver.kt app/src/test/java/com/freesudoku/app/domain/generator/PuzzleCarverTest.kt
git commit -m "feat(domain): add puzzle carver"
```

- [ ] **Step 6: Build checkpoint**

```bash
./gradlew :app:assembleDebug
```

---

## Task 10: CampaignCurve

**Files:**
- Create: `domain/campaign/CampaignCurve.kt`
- Test: `.../campaign/CampaignCurveTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.campaign

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CampaignCurveTest {
    private val curve = CampaignCurve()

    @Test fun `target score is non-decreasing across the campaign`() {
        val scores = (1..300).map { curve.targetScore(it) }
        val smoothed = scores.windowed(10, 10) { it.average() }
        assertThat(smoothed).isInOrder()
    }

    @Test fun `first puzzle is in beginner range`() {
        assertThat(curve.targetScore(1)).isLessThan(15.0)
    }

    @Test fun `score saturates below the max`() {
        assertThat(curve.targetScore(5000)).isAtMost(CampaignCurve.CURVE_MAX)
    }

    @Test fun `tolerance window widens with attempts and is capped`() {
        assertThat(curve.toleranceWindow(0)).isLessThan(curve.toleranceWindow(5))
        assertThat(curve.toleranceWindow(999)).isAtMost(CampaignCurve.CARVE_TOLERANCE_MAX)
    }

    @Test fun `same puzzle number always yields the same target`() {
        assertThat(curve.targetScore(37)).isEqualTo(curve.targetScore(37))
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.campaign.CampaignCurveTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.freesudoku.app.domain.campaign

import kotlin.math.ln
import kotlin.math.min

/** Maps a campaign puzzle number to a target difficulty score, monotonically rising and saturating. */
class CampaignCurve {

    fun targetScore(puzzleNumber: Int): Double {
        require(puzzleNumber >= 1)
        val base = CURVE_BASE + CURVE_GROWTH * ln(1.0 + puzzleNumber)
        val noisy = base + deterministicNoise(puzzleNumber)
        return noisy.coerceIn(0.0, CURVE_MAX)
    }

    fun toleranceWindow(attempt: Int): Double =
        min(CARVE_TOLERANCE_MAX, CARVE_TOLERANCE_BASE + CARVE_TOLERANCE_STEP * attempt)

    private fun deterministicNoise(n: Int): Double {
        // hash the number into [-CURVE_NOISE, +CURVE_NOISE]
        var h = n * 2654435761L
        h = h xor (h ushr 13)
        val unit = ((h and 0xFFFF).toDouble() / 0xFFFF) * 2.0 - 1.0
        return unit * CURVE_NOISE
    }

    companion object {
        const val CURVE_BASE = 4.0
        const val CURVE_GROWTH = 12.0
        const val CURVE_MAX = 88.0
        const val CURVE_NOISE = 3.0
        const val CARVE_TOLERANCE_BASE = 6.0
        const val CARVE_TOLERANCE_STEP = 1.5
        const val CARVE_TOLERANCE_MAX = 20.0
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.campaign.CampaignCurveTest"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/campaign app/src/test/java/com/freesudoku/app/domain/campaign
git commit -m "feat(domain): add campaign difficulty curve"
```

---

## Task 11: PuzzleFactory

**Files:**
- Create: `domain/generator/PuzzleFactory.kt`
- Test: `.../generator/PuzzleFactoryTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.solver.SolutionCounter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class PuzzleFactoryTest {
    private fun factory(seed: Long) = PuzzleFactory(
        random = Random(seed),
        curve = CampaignCurve(),
    )

    @Test fun `produces a puzzle with a unique solution`() {
        val puzzle = factory(1).generateForTarget(targetScore = 20.0)
        assertThat(SolutionCounter().countUpTo(puzzle.givens, limit = 2)).isEqualTo(1)
    }

    @Test fun `puzzle band reflects its score`() {
        val puzzle = factory(2).generateForTarget(targetScore = 20.0)
        assertThat(puzzle.band).isEqualTo(DifficultyBand.fromScore(puzzle.difficultyScore))
    }

    @Test fun `higher target yields higher average score`() {
        val easy = (1..4).map { factory(it.toLong()).generateForTarget(10.0).difficultyScore }.average()
        val hard = (1..4).map { factory(it.toLong()).generateForTarget(60.0).difficultyScore }.average()
        assertThat(hard).isGreaterThan(easy)
    }

    @Test fun `generateForCampaign assigns the puzzle number`() {
        val puzzle = factory(3).generateForCampaign(number = 12)
        assertThat(puzzle.number).isEqualTo(12)
    }

    @Test fun `id is stable and unique-ish`() {
        val p = factory(4).generateForTarget(15.0)
        assertThat(p.id).isNotEmpty()
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.generator.PuzzleFactoryTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.freesudoku.app.domain.solver.DifficultyRater
import kotlin.math.abs
import kotlin.random.Random

class PuzzleFactory(
    private val random: Random = Random.Default,
    private val curve: CampaignCurve = CampaignCurve(),
    private val fullGridGenerator: FullGridGenerator = FullGridGenerator(random),
    private val carver: PuzzleCarver = PuzzleCarver(random),
    private val rater: DifficultyRater = DifficultyRater(),
    private val maxAttempts: Int = CARVE_MAX_ATTEMPTS,
) {
    fun generateForCampaign(number: Int): Puzzle =
        generateForTarget(curve.targetScore(number)).copy(number = number)

    fun generateForTarget(targetScore: Double): Puzzle {
        var best: Puzzle? = null
        var bestGap = Double.MAX_VALUE
        for (attempt in 0 until maxAttempts) {
            val solution = fullGridGenerator.generate()
            val carved = carver.carve(solution)
            val rating = rater.rate(carved.givens)
            val gap = abs(rating.score - targetScore)
            if (gap < bestGap) {
                bestGap = gap
                best = Puzzle(
                    id = newId(),
                    number = null,
                    givens = carved.givens,
                    solution = carved.solution,
                    difficultyScore = rating.score,
                    band = DifficultyBand.fromScore(rating.score),
                )
            }
            if (gap <= curve.toleranceWindow(attempt)) return best!!
        }
        return best!!
    }

    private fun newId(): String =
        buildString { repeat(16) { append(ID_ALPHABET[random.nextInt(ID_ALPHABET.length)]) } }

    companion object {
        const val CARVE_MAX_ATTEMPTS = 40
        private const val ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.generator.PuzzleFactoryTest"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/generator/PuzzleFactory.kt app/src/test/java/com/freesudoku/app/domain/generator/PuzzleFactoryTest.kt
git commit -m "feat(domain): add puzzle factory targeting campaign difficulty"
```

---

## Task 12: GameEngine (pure game rules)

**Files:**
- Create: `domain/game/GameEngine.kt`
- Test: `.../game/GameEngineTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.freesudoku.app.domain.game

import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameEngineTest {
    private val engine = GameEngine()
    private val puzzle = Puzzle(
        id = "t", number = 1,
        givens = GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        solution = GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        difficultyScore = 15.0, band = DifficultyBand.FACIL,
    )
    private fun fresh(limit: Boolean = true) = GameSnapshot(
        puzzle = puzzle, board = Board.fromGivens(puzzle.givens),
        undoStack = emptyList(), redoStack = emptyList(),
        elapsedMs = 0, mistakes = 0, hintsUsed = 0,
        status = GameStatus.IN_PROGRESS, mistakeLimitEnabled = limit,
    )

    @Test fun `correct value is placed and is not a mistake`() {
        val s = engine.setValue(fresh(), 0, 2, 4)
        assertThat(s.board.cell(0, 2).value).isEqualTo(4)
        assertThat(s.mistakes).isEqualTo(0)
        assertThat(s.undoStack).hasSize(1)
    }

    @Test fun `wrong value increments mistakes`() {
        val s = engine.setValue(fresh(), 0, 2, 9)
        assertThat(s.mistakes).isEqualTo(1)
    }

    @Test fun `three mistakes fail the game when limit enabled`() {
        var s = fresh()
        s = engine.setValue(s, 0, 2, 9)
        s = engine.setValue(s, 0, 3, 9)
        s = engine.setValue(s, 0, 5, 9)
        assertThat(s.status).isEqualTo(GameStatus.FAILED)
    }

    @Test fun `mistakes do not fail the game in relaxed mode`() {
        var s = fresh(limit = false)
        repeat(5) { s = engine.setValue(s, 0, 2, 9) }
        assertThat(s.status).isEqualTo(GameStatus.IN_PROGRESS)
    }

    @Test fun `given cells cannot be edited`() {
        val s = engine.setValue(fresh(), 0, 0, 1)
        assertThat(s.board.cell(0, 0).value).isEqualTo(5)
        assertThat(s.undoStack).isEmpty()
    }

    @Test fun `undo then redo restores state`() {
        val s1 = engine.setValue(fresh(), 0, 2, 4)
        val s2 = engine.undo(s1)
        assertThat(s2.board.cell(0, 2).value).isEqualTo(0)
        val s3 = engine.redo(s2)
        assertThat(s3.board.cell(0, 2).value).isEqualTo(4)
    }

    @Test fun `toggle note adds and removes`() {
        var s = engine.toggleNote(fresh(), 0, 2, 4)
        assertThat(s.board.cell(0, 2).notes).containsExactly(4)
        s = engine.toggleNote(s, 0, 2, 4)
        assertThat(s.board.cell(0, 2).notes).isEmpty()
    }

    @Test fun `hint reveals the correct value and counts`() {
        val s = engine.hint(fresh(), 0, 2)
        assertThat(s.board.cell(0, 2).value).isEqualTo(4)
        assertThat(s.hintsUsed).isEqualTo(1)
    }

    @Test fun `completing the board sets COMPLETED`() {
        var s = fresh()
        val sol = puzzle.solution
        for (r in 0 until 9) for (c in 0 until 9) {
            if (s.board.cell(r, c).value == 0) s = engine.setValue(s, r, c, sol.valueAt(r, c))
        }
        assertThat(s.status).isEqualTo(GameStatus.COMPLETED)
    }

    @Test fun `auto-removes notes of the same digit from peers`() {
        var s = engine.toggleNote(fresh(), 0, 3, 4)   // note 4 in same row
        s = engine.setValue(s, 0, 2, 4)               // place 4 nearby
        assertThat(s.board.cell(0, 3).notes).doesNotContain(4)
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.game.GameEngineTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.freesudoku.app.domain.game

import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.Move

/** Pure Sudoku game rules over a [GameSnapshot]. No Android, no coroutines, no persistence. */
class GameEngine(private val autoRemoveNotes: Boolean = true) {

    fun setValue(snapshot: GameSnapshot, row: Int, col: Int, value: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || cell.value == value) return snapshot

        var board = snapshot.board.withValue(row, col, value)
        if (autoRemoveNotes && value != 0) board = clearPeerNotes(board, row, col, value)

        val correct = value != 0 && snapshot.puzzle.solution.valueAt(row, col) == value
        val isMistake = value != 0 && !correct
        val mistakes = snapshot.mistakes + if (isMistake) 1 else 0

        val move = Move.SetValue(row, col, value, cell.value, cell.notes)
        val status = when {
            snapshot.mistakeLimitEnabled && mistakes >= GameSnapshot.MISTAKE_LIMIT -> GameStatus.FAILED
            board.isComplete && board.toGrid() == snapshot.puzzle.solution -> GameStatus.COMPLETED
            else -> GameStatus.IN_PROGRESS
        }
        return snapshot.copy(
            board = board,
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
            mistakes = mistakes,
            status = status,
        )
    }

    fun clear(snapshot: GameSnapshot, row: Int, col: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || (cell.value == 0 && cell.notes.isEmpty())) return snapshot
        val move = Move.ClearCell(row, col, cell.value, cell.notes)
        return snapshot.copy(
            board = snapshot.board.withValue(row, col, 0).withNotes(row, col, emptySet()),
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
        )
    }

    fun toggleNote(snapshot: GameSnapshot, row: Int, col: Int, digit: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || cell.value != 0) return snapshot
        val added = digit !in cell.notes
        val move = Move.ToggleNote(row, col, digit, added)
        return snapshot.copy(
            board = snapshot.board.withNoteToggled(row, col, digit),
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
        )
    }

    fun hint(snapshot: GameSnapshot, row: Int, col: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || cell.value != 0) return snapshot
        val answer = snapshot.puzzle.solution.valueAt(row, col)
        var board = snapshot.board.withValue(row, col, answer)
        if (autoRemoveNotes) board = clearPeerNotes(board, row, col, answer)
        val move = Move.SetValue(row, col, answer, cell.value, cell.notes)
        val status = if (board.isComplete && board.toGrid() == snapshot.puzzle.solution)
            GameStatus.COMPLETED else snapshot.status
        return snapshot.copy(
            board = board,
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
            hintsUsed = snapshot.hintsUsed + 1,
            status = status,
        )
    }

    /** Reveals the "easiest" next cell (fewest legal candidates) when the user requests a hint with no selection. */
    fun autoHint(snapshot: GameSnapshot): GameSnapshot {
        val target = snapshot.board.emptyCells().minByOrNull { (r, c) ->
            (1..9).count { snapshot.board.toGrid().isValidPlacement(r, c, it) }
        } ?: return snapshot
        return hint(snapshot, target.first, target.second)
    }

    fun undo(snapshot: GameSnapshot): GameSnapshot {
        val move = snapshot.undoStack.lastOrNull() ?: return snapshot
        val board = revert(snapshot.board, move)
        return snapshot.copy(
            board = board,
            undoStack = snapshot.undoStack.dropLast(1),
            redoStack = snapshot.redoStack + move,
            status = if (snapshot.status == GameStatus.COMPLETED) GameStatus.IN_PROGRESS else snapshot.status,
        )
    }

    fun redo(snapshot: GameSnapshot): GameSnapshot {
        val move = snapshot.redoStack.lastOrNull() ?: return snapshot
        val board = reapply(snapshot.board, move)
        return snapshot.copy(
            board = board,
            redoStack = snapshot.redoStack.dropLast(1),
            undoStack = snapshot.undoStack + move,
        )
    }

    fun tick(snapshot: GameSnapshot, deltaMs: Long): GameSnapshot =
        if (snapshot.status == GameStatus.IN_PROGRESS)
            snapshot.copy(elapsedMs = snapshot.elapsedMs + deltaMs) else snapshot

    private fun revert(board: Board, move: Move): Board = when (move) {
        is Move.SetValue -> board.withValue(move.row, move.col, move.previousValue)
            .withNotes(move.row, move.col, move.previousNotes)
        is Move.ClearCell -> board.withValue(move.row, move.col, move.previousValue)
            .withNotes(move.row, move.col, move.previousNotes)
        is Move.ToggleNote -> board.withNoteToggled(move.row, move.col, move.digit)
    }

    private fun reapply(board: Board, move: Move): Board = when (move) {
        is Move.SetValue -> board.withValue(move.row, move.col, move.value)
        is Move.ClearCell -> board.withValue(move.row, move.col, 0).withNotes(move.row, move.col, emptySet())
        is Move.ToggleNote -> board.withNoteToggled(move.row, move.col, move.digit)
    }

    private fun clearPeerNotes(board: Board, row: Int, col: Int, digit: Int): Board {
        var b = board
        for (c in 0 until 9) if (digit in b.cell(row, c).notes) b = b.withNoteToggled(row, c, digit)
        for (r in 0 until 9) if (digit in b.cell(r, col).notes) b = b.withNoteToggled(r, col, digit)
        val br = (row / 3) * 3; val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if (digit in b.cell(r, c).notes) b = b.withNoteToggled(r, c, digit)
        }
        return b
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.game.GameEngineTest"
```

- [ ] **Step 5: Full domain test sweep + build**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/game app/src/test/java/com/freesudoku/app/domain/game
git commit -m "feat(domain): add pure game engine (moves, undo/redo, hints, validation)"
```

---

## Plan 1 self-review

**Spec coverage (domain slice of §3):**
- §3.1 models — Tasks 2, 3 ✅
- §3.2 generator (GridBuilder / ClueRemover / UniquenessChecker / PuzzleFactory) — Tasks 4, 5, 9, 11 ✅ (`GridBuilder`→`FullGridGenerator`, `ClueRemover`→`PuzzleCarver`, `UniquenessChecker`→`SolutionCounter`)
- §3.3 solver + rater — Tasks 6, 7, 8 ✅
- §3.4 campaign curve — Task 10 ✅
- §3.5 use cases — **partial**: pure game rules (`ApplyMove`, undo/redo, hint, completion) land as `GameEngine` in Task 12. Repo-backed use cases (`GetNextCampaignPuzzle`, `StartGame`, `ResumeGame`, `CompletePuzzle`, `AbandonGame`, `GetProgress`, `GetStats`) are Plan 2 — they need Room. This is a deliberate plan boundary.
- §8.1 domain tests — every task is TDD ✅

**Placeholder scan:** The only intentional placeholders are the strategy-example fixture strings in Task 7 and the rated CSV in Task 8, each with an explicit executor note and an acceptance gate ("do not mark done with placeholder fixtures"). All implementation code is complete.

**Type consistency:** `Grid` uses `valueAt(row,col)` + `cells: IntArray` everywhere. `SolverState` exposes `values`/`candidates` as `IntArray` and `UNITS`/`peers` as `companion`. `TechniqueStep`/`TechniqueId` shared across all techniques and `LogicalSolver`. `Puzzle` is a `data class` (Task 11 uses `.copy(number=...)`). `GameSnapshot` fields match `GameEngine` usage and the Task 12 test.

**Open calibration (carried to spec §10):** rater weights, band bounds, curve constants — Tasks 8 & 10 tune them against fixtures and the final values get written back into the spec.

---

## After Plan 1

Plan 2 (`data`) and Plan 3 (`ui`) will be written as their own documents once Plan 1 lands, because each is an independently testable subsystem and Plan 2's DAO/entity shapes depend on the final domain types from this plan. Rough contents:

- **Plan 2 — Data layer:** Room entities + DAOs (`PuzzleBufferEntity`, `CurrentGameEntity`, `CompletedPuzzleEntity`, `CampaignProgressEntity`), `TypeConverter`s, DB + migration v1, DataStore settings, repositories (`PuzzleRepository` with background refill, `GameRepository` with debounced save, `StatsRepository`, `SettingsRepository`, `CampaignRepository`), entity↔domain mappers, the repo-backed use cases from §3.5, Hilt modules. Instrumented DAO + migration tests.
- **Plan 3 — UI layer:** theme finalization, Navigation Compose graph, Home / Game / Stats / Settings screens + ViewModels, reusable components (`SudokuBoard`, `NumberPad`, `GameToolbar`, `MistakeCounter`, `TimerText`, `ResultSheet`), lifecycle/timer handling with `SavedStateHandle`, on-demand generation loading state, accessibility pass, edge-to-edge. Compose UI tests + ViewModel tests.
