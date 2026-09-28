import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.expand
import org.gradle.kotlin.dsl.maven
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.*

val Project.mod: ModData get() = ModData(this)
fun Project.prop(key: String): String? = findProperty(key)?.toString()
fun String.upperCaseFirst() = replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

fun RepositoryHandler.strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
    forRepository { maven(url) { name = alias } }
    filter { groups.forEach(::includeGroup) }
}

fun ProcessResources.properties(files: Iterable<String>, vararg properties: Pair<String, Any>) {
    for ((name, value) in properties) inputs.property(name, value)
    filesMatching(files) {
        expand(properties.toMap())
    }
}

val RESOURCE_OVERLAYS: Map<String, String> = linkedMapOf(
    "1.21.1" to "<26.1",
    "26.x" to ">=26.1",
    "26.1" to ">=26.1 <26.2",
    "26.2" to ">=26.2 <26.3",
    "pre-26.3" to "<26.3",
    "26.3+" to ">=26.3",
)

fun RepositoryHandler.fieldGuideRepositories() {
    mavenLocal()
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com/releases/") {
        name = "Terraformers (Mod Menu, EMI)"
        content {
            includeGroupAndSubgroups("com.terraformersmc")
            includeGroupAndSubgroups("dev.emi")
        }
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven (YACL)"
        content {
            includeGroupAndSubgroups("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "Quilt Maven"
        content { includeGroupAndSubgroups("org.quiltmc.parsers") }
    }
    maven("https://maven.cassian.cc/") {
        name = "Cassian's Maven"
        content { includeGroupAndSubgroups("cc.cassian") }
    }
    maven("https://maven.impactdev.net/repository/development/") {
        name = "ImpactDev (Cobblemon)"
        content { includeGroupAndSubgroups("com.cobblemon") }
    }
    maven("https://maven.latvian.dev/releases") {
        name = "Latvian Maven"
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.latvian.apps")
        }
    }
    exclusiveContent {
        forRepository { maven("https://repo.sleeping.town/") { name = "Sleeping Town" } }
        filter { includeGroup("folk.sisby") }
    }
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
        content { includeGroupAndSubgroups("org.parchmentmc") }
    }
}

@JvmInline
value class ModData(private val project: Project) {
    val id: String get() = requireNotNull(project.prop("mod.id")) { "Missing 'mod.id'" }
    val name: String get() = requireNotNull(project.prop("mod.name")) { "Missing 'mod.name'" }
    val version: String get() = requireNotNull(project.prop("mod.version")) { "Missing 'mod.version'" }
    val group: String get() = requireNotNull(project.prop("mod.group")) { "Missing 'mod.group'" }

    fun prop(key: String) = requireNotNull(project.prop("mod.$key")) { "Missing 'mod.$key'" }
    fun dep(key: String) = requireNotNull(project.prop("deps.$key")) { "Missing 'deps.$key'" }
}
