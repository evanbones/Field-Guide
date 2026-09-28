@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
    id("maven-publish")
}

val minecraft = stonecutter.current.version
val mcVersion = stonecutter.current.project.substringBeforeLast('-')
fun dep(name: String) = property("deps.$name") as String
fun optDep(name: String) = findProperty("deps.$name") as String?
fun since(predicate: String) = stonecutter.eval(minecraft, predicate)

for ((dir, predicate) in RESOURCE_OVERLAYS) {
    if (since(predicate)) sourceSets.main { resources.srcDir(rootProject.file("src/main/overlays/$dir")) }
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + prop("deps.minecraft")
        this["minecraft"] = prop("mod.mc_dep_fabric")
        this["mod_id"] = prop("mod.id")
        this["mod_name"] = prop("mod.name")
        this["description"] = prop("mod.description")
        this["mod_author"] = prop("mod.author")
        this["credits"] = prop("mod.credits")
        this["license"] = prop("mod.license")
        this["fabric_loader_version"] = prop("deps.fabric_loader")
        this["java_version"] = prop("deps.java_version")
        this["yacl_version"] = prop("deps.yacl").substringBefore('+')
    }
    inputs.properties(props)

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(props)
    }
}

tasks.named("processResources") {
    dependsOn(":${stonecutter.current.project}:stonecutterGenerate")
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

repositories {
    fieldGuideRepositories()
}

dependencies {
    minecraft("com.mojang:minecraft:${dep("minecraft")}")
    if (!since(">=26.1")) {
        val loom = project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()
        "mappings"(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${dep("minecraft")}:${dep("parchment")}@zip")
        })
    }
    modImplementation("net.fabricmc:fabric-loader:${dep("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${dep("fabric_api")}")

    // YACL
    val yacl = "dev.isxander:yet-another-config-lib:${dep("yacl")}"
    modCompileOnly(yacl)
    modLocalRuntime(yacl)
    modImplementation("com.terraformersmc:modmenu:${dep("modmenu")}")

    // EMI/RRV
    optDep("emi")?.let {
        modCompileOnly("dev.emi:emi-fabric:$it:api")
        modLocalRuntime("dev.emi:emi-fabric:$it")
    }
    optDep("rrv")?.let {
        modCompileOnly("cc.cassian.rrv:reliable-recipe-viewer-fabric:$it")
        modLocalRuntime("cc.cassian.rrv:reliable-recipe-viewer-fabric:$it") { isTransitive = false }
    }

    // Item Descriptions
    modCompileOnly("maven.modrinth:item-descriptions:${dep("item_descriptions")}")
    modLocalRuntime("maven.modrinth:item-descriptions:${dep("item_descriptions")}")
    optDep("kaleido_config")?.let { modLocalRuntime("folk.sisby:kaleido-config:$it") }

    // Immersive Overlays
    optDep("immersive_overlays")?.let {
        modLocalRuntime("maven.modrinth:immersive-overlays:$it")
        modLocalRuntime("maven.modrinth:mru:${dep("mru")}")
    }

    // Spyglass Improvements
    optDep("spyglass_improvements")?.let { modLocalRuntime("maven.modrinth:spyglass-improvements:$it") }

    // ETF & EMF
    modCompileOnly("maven.modrinth:entitytexturefeatures:${dep("etf")}")
    modCompileOnly("maven.modrinth:entity-model-features:${dep("emf")}")

    // Reliable Remover
    modCompileOnly("maven.modrinth:reliable-remover:${dep("reliable_remover")}")

    // Scholar
    optDep("scholar")?.let { modCompileOnly("maven.modrinth:scholar:$it") }

    // Exposure
    optDep("exposure")?.let {
        modCompileOnly("maven.modrinth:exposure:$it")
        modLocalRuntime("maven.modrinth:exposure:$it")
        modLocalRuntime("maven.modrinth:forge-config-api-port:${dep("fcapi")}")
        localRuntime("com.electronwill.night-config:core:${dep("night_config")}")
        localRuntime("com.electronwill.night-config:toml:${dep("night_config")}")
    }

    // No Man's Land
    optDep("no_mans_land")?.let { compileOnly("maven.modrinth:no-mans-land:$it") }

    // Cobblemon
    optDep("cobblemon")?.let { modCompileOnly("com.cobblemon:fabric:$it") }

    // Serene Seasons
    optDep("serene_seasons")?.let { modCompileOnly("maven.modrinth:serene-seasons:$it") }

    // Fabric Seasons
    optDep("fabric_seasons")?.let {
        modCompileOnly("maven.modrinth:fabric-seasons:$it")
        modLocalRuntime("maven.modrinth:fabric-seasons:$it")
    }

    // Mixin Constraints
    include(implementation("com.moulberry:mixinconstraints:${dep("mixin_constraints")}")!!)
}

val accessWidener = rootProject.file("src/main/overlays/${if (since(">=26.1")) "26.x" else "1.21.1"}/fieldguide.accesswidener")
if (accessWidener.exists()) {
    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
        accessWidenerPath.set(accessWidener)
    }
}

tasks {
    processResources {
        exclude(
            "**/neoforge.mods.toml", "**/mods.toml", "**/*.neoforge.mixins.json", "**/accesstransformer.cfg",
            "kubejs.plugins.txt", "META-INF/services/dev.latvian.mods.kubejs.plugin.KubeJSPlugin"
        )
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(loomx.modJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

val javaVer = (property("deps.java_version") as String).toInt()
java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVer)
    sourceCompatibility = JavaVersion.toVersion(javaVer)
    targetCompatibility = JavaVersion.toVersion(javaVer)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVer
}

val modName = property("mod.name") as String
tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename("LICENSE", "LICENSE_$modName")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = property("mod.group") as String
            artifactId = "${property("mod.id")}-fabric"
            version = "${property("mod.version")}+${property("deps.minecraft")}"

            from(components["java"])
        }
    }
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = loomx.modJar.map { it.archiveFile.get() }
    additionalFiles.from(loomx.modSourcesJar.map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} Fabric $mcVersion - ${property("mod.version")}"
    version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.environmentVariable("MODRINTH_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api")
        requires("item-descriptions")
        optional("yacl")
        if (optDep("emi") != null) optional("emi")
        if (optDep("rrv") != null) optional("rrv")
        optional("modmenu")
        if (optDep("immersive_overlays") != null) requires("immersive-overlays")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.environmentVariable("CURSEFORGE_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api")
        requires("item-descriptions")
        optional("yacl")
        if (optDep("emi") != null) optional("emi")
        if (optDep("rrv") != null) optional("rrv")
        optional("modmenu")
        if (optDep("immersive_overlays") != null) requires("immersive-overlays")
        client = true
        server = true
    }
}
