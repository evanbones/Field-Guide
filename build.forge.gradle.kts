plugins {
    id("net.neoforged.moddev.legacyforge")
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

val modId = property("mod.id") as String
val refmap = "$modId.refmap.json"
val mixinConfigs = listOf("$modId.mixins.json", "$modId.forge.mixins.json")

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + prop("deps.minecraft")
        this["minecraft_version_range"] = prop("mod.mc_dep_forgelike")
        this["mod_id"] = prop("mod.id")
        this["mod_name"] = prop("mod.name")
        this["description"] = prop("mod.description")
        this["mod_author"] = prop("mod.author")
        this["credits"] = prop("mod.credits")
        this["license"] = prop("mod.license")
        this["forge_loader_version_range"] = prop("deps.forge_loader_version_range")
        this["forge_version"] = prop("deps.forge")
        this["java_version"] = prop("deps.java_version")
        this["yacl_version"] = prop("deps.yacl").substringBefore('+')
    }
    inputs.properties(props)

    filesMatching(listOf("pack.mcmeta", "META-INF/mods.toml", "*.mixins.json")) {
        expand(props)
    }

    val refmapEntry = "\"refmap\": \"$refmap\",\n  \"package\":"
    filesMatching(mixinConfigs) {
        filter { line -> line.replace("\"package\":", refmapEntry) }
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
base.archivesName = modId

repositories {
    fieldGuideRepositories()
}

mixin {
    add(sourceSets["main"], refmap)
    mixinConfigs.forEach(::config)
}

legacyForge {
    version = "${dep("minecraft")}-${dep("forge")}"
    validateAccessTransformers = true

    val at = rootProject.file("src/main/overlays/1.20.1/META-INF/accesstransformer.cfg")
    if (at.exists()) accessTransformers.from(at.absolutePath)

    parchment {
        minecraftVersion = dep("minecraft")
        mappingsVersion = dep("parchment")
    }

    runs {
        register("client") {
            gameDirectory = file("run/client")
            client()
        }
        register("server") {
            gameDirectory = file("run/server")
            server()
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.accesswidener", "**/neoforge.mods.toml", "**/*.fabric.mixins.json", "**/*.neoforge.mixins.json")
        exclude("META-INF/services/dev.latvian.mods.kubejs.plugin.KubeJSPlugin")
        if (optDep("kubejs") == null) exclude("kubejs.plugins.txt")
        legacyDataLayout()
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    jar {
        finalizedBy("reobfJar")
        manifest.attributes("MixinConfigs" to mixinConfigs.joinToString(","))
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(named<Jar>("reobfJar").map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("org.jetbrains:annotations:26.0.2-1")

    // YACL
    modCompileOnly("dev.isxander:yet-another-config-lib:${dep("yacl")}")
    modRuntimeOnly("dev.isxander:yet-another-config-lib:${dep("yacl")}")

    // EMI
    optDep("emi")?.let {
        modCompileOnly("dev.emi:emi-forge:$it:api")
        modRuntimeOnly("dev.emi:emi-forge:$it")
    }

    // Item Descriptions
    modCompileOnly("maven.modrinth:item-descriptions:${dep("item_descriptions")}")
    modRuntimeOnly("maven.modrinth:item-descriptions:${dep("item_descriptions")}")

    // Immersive Overlays
    modCompileOnly("maven.modrinth:immersive-overlays:${dep("immersive_overlays")}")
    modRuntimeOnly("maven.modrinth:immersive-overlays:${dep("immersive_overlays")}")

    // ETF & EMF
    modCompileOnly("maven.modrinth:entitytexturefeatures:${dep("etf")}")
    modCompileOnly("maven.modrinth:entity-model-features:${dep("emf")}")

    // Reliable Remover
    modCompileOnly("maven.modrinth:reliable-remover:${dep("reliable_remover")}")

    // Scholar
    optDep("scholar")?.let { modCompileOnly("maven.modrinth:scholar:$it") }

    // Exposure
    optDep("exposure")?.let { modCompileOnly("maven.modrinth:exposure:$it") }

    // Cobblemon
    optDep("cobblemon")?.let { modCompileOnly("com.cobblemon:forge:$it") }

    // KubeJS
    optDep("kubejs")?.let {
        modCompileOnly("maven.modrinth:kubejs:$it")
        modCompileOnly("dev.latvian.mods:rhino-forge:${dep("rhino")}")
    }

    // Serene Seasons
    optDep("serene_seasons")?.let { modCompileOnly("maven.modrinth:serene-seasons:$it") }

    // Ecliptic Seasons
    optDep("ecliptic_seasons")?.let { modCompileOnly("maven.modrinth:ecliptic-seasons:$it") }

    // Dawn Era
    optDep("dawn_era")?.let {
        modCompileOnly("maven.modrinth:the-dawn-era:$it")
        modCompileOnly("maven.modrinth:astemirlib:${dep("astemirlib")}")
    }

    // Mixin Constraints
    compileOnly("com.moulberry:mixinconstraints:${dep("mixin_constraints")}")
    val mixinConstraints = implementation("com.moulberry:mixinconstraints") {
        version {
            strictly("[${dep("mixin_constraints")},)")
            prefer(dep("mixin_constraints"))
        }
    }
    "jarJar"(mixinConstraints!!)
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
            artifactId = "$modId-forge"
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
    file = tasks.named<Jar>("reobfJar").flatMap { it.archiveFile }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} Forge $mcVersion - ${property("mod.version")}"
    version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("forge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.environmentVariable("MODRINTH_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("item-descriptions")
        requires("immersive-overlays")
        optional("yacl")
        if (optDep("emi") != null) optional("emi")
        if (optDep("kubejs") != null) optional("kubejs")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.environmentVariable("CURSEFORGE_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("item-descriptions")
        requires("immersive-overlays")
        optional("yacl")
        if (optDep("emi") != null) optional("emi")
        if (optDep("kubejs") != null) optional("kubejs")
        client = true
        server = true
    }
}
