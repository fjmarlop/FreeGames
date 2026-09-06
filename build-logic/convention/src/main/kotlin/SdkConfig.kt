/**
 * Single source of truth for the Android SDK levels shared by every game module.
 * Bump here once and every game in the monorepo picks it up.
 */
object SdkConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 24
    const val TARGET_SDK = 36
}
