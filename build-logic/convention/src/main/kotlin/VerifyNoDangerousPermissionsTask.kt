import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Fails the build if the *merged* manifest of a variant declares any forbidden permission.
 *
 * Reading the merged manifest (not the source one) is deliberate: it is the only place a
 * transitive dependency could sneak in `android.permission.INTERNET`. A green run of this task
 * is the verifiable proof that a FreeGames game cannot open a socket.
 */
abstract class VerifyNoDangerousPermissionsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val forbiddenPermissions: ListProperty<String>

    @TaskAction
    fun verify() {
        val manifest = mergedManifest.get().asFile
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)

        val androidNs = "http://schemas.android.com/apk/res/android"
        val declared = buildList {
            for (tag in listOf("uses-permission", "uses-permission-sdk-23")) {
                val nodes = doc.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    add((nodes.item(i) as Element).getAttributeNS(androidNs, "name"))
                }
            }
        }

        val forbidden = forbiddenPermissions.get().toSet()
        val violations = declared.filter { it in forbidden }.distinct()

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("El manifest fusionado declara permisos prohibidos para un juego FreeGames:")
                    violations.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Manifest: $manifest")
                    appendLine(
                        "Estos juegos deben poder demostrar que no envían datos a ningún sitio. " +
                            "Si una dependencia introduce el permiso, elimínala o neutralízalo en el " +
                            "manifest con tools:node=\"remove\".",
                    )
                },
            )
        }

        logger.lifecycle(
            "verifyNoDangerousPermissions: OK — ${declared.size} permiso(s) en el manifest, ninguno prohibido.",
        )
    }
}
