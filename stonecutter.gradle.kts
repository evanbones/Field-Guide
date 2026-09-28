plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.18.2" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
    id("org.moddedmc.wiki.toolkit") version "0.4.1"
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge")
    filters.include("**/*.fsh", "**/*.vsh")
    fun rename(from: String, to: String) = listOf("\\b$from\\b", to, "\\b$to\\b", from)
    replacements {
        regex(eval(node.metadata.version, ">=26.1")) {
            for ((a, b) in listOf(
                "ResourceLocation" to "Identifier",
                "ResourceLocationArgument" to "IdentifierArgument",
                "GuiGraphics" to "GuiGraphicsExtractor",
                "readResourceLocation" to "readIdentifier",
                "writeResourceLocation" to "writeIdentifier",
            )) {
                val (p1, r1, p2, r2) = rename(a, b)
                replace(p1, r1, p2, r2)
            }
            replace(
                "(?<=\\b(?:guiGraphics|graphics))\\.drawString\\(", ".text(",
                "(?<=\\b(?:guiGraphics|graphics))\\.text\\(", ".drawString("
            )
            replace(
                "(?<=\\b(?:guiGraphics|graphics))\\.renderTooltip\\(", ".setTooltipForNextFrame(",
                "(?<=\\b(?:guiGraphics|graphics))\\.setTooltipForNextFrame\\(", ".renderTooltip("
            )
            replace(
                "net\\.minecraft\\.advancements\\.critereon\\b", "net.minecraft.advancements.criterion",
                "net\\.minecraft\\.advancements\\.criterion\\b", "net.minecraft.advancements.critereon"
            )
        }
        regex(eval(node.metadata.version, ">=26.2")) {
            replace("(?<!\\.gui)\\.setScreen\\(", ".gui.setScreen(", "\\.gui\\.setScreen\\(", ".setScreen(")
            replace(
                "(?<=\\b(?:minecraft|mc|client\\(\\)|getInstance\\(\\)))\\.screen\\b(?!\\()", ".gui.screen()",
                "\\.gui\\.screen\\(\\)", ".screen"
            )
        }
        regex(eval(node.metadata.version, ">=26.3")) {
            replace("InputConstants\\.Type\\.KEYSYM\\b", "InputConstants.Type.KEYBOARD", "InputConstants\\.Type\\.KEYBOARD\\b", "InputConstants.Type.KEYSYM")
            replace("\\.mulPose\\(Axis\\.", ".rotate(Axis.", "\\.rotate\\(Axis\\.", ".mulPose(Axis.")
        }
    }
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}

wiki {
    wikiAccessToken = System.getenv("WIKI_ACCESS_TOKEN")
    docs.create("field-guide") {
        root = file("docs/")
    }
}
