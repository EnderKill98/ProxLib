/*
 * AI SLOP WARNING!
 * Do not use this as a reference. While it might "work", AI (namely Junie with Opus 4.6) has been set
 * loose on this file in particular in a loop of "still broke fix" prompts. So this is the opposite of anything clean!
 *
 * Reason: I wanted to get the 26.1 release done, but didn't wanna become a Gradle or Stonecutter dev in order to figure
 * out how to do this properly. Had other priorities. Sorry not sorry in this case.
 */
class ModData {
    val id = property("mod.id").toString()
    val name = property("mod.name").toString()
    val version = property("mod.version").toString()
    val group = property("mod.group").toString()
    val java = property("mod.java").toString()
}

class ModDependencies {
    operator fun get(name: String) = property("deps.$name").toString()
    fun has(name: String) = hasProperty("deps.$name")
}

val mod = ModData()
val deps = ModDependencies()
val mcVersion = stonecutter.current.version
val mcDep = property("mod.mc_dep").toString()

plugins {
    `maven-publish`
    id("fabric-loom") version "1.15.5"
    //id("dev.kikugie.j52j")
    //id("me.modmuss50.mod-publish-plugin")
}


version = "${mod.version}+$mcVersion"
group = mod.group
base { archivesName.set(mod.id) }

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    fun fapi(vararg modules: String) = modules.forEach {
        if (deps.has("fabric_api")) {
            modImplementation(fabricApi.module(it, deps["fabric_api"]))
        }
    }

    minecraft("com.mojang:minecraft:$mcVersion")
    if (!mcVersion.startsWith("26.")) {
        mappings(loom.officialMojangMappings())
    } else {
        mappings(loom.layered {
            files(rootProject.file("versions/26.1/identity-named.jar"))  // official → named
            // No intermediary() call — intermediaryUrl in loom{} handles that step
        })
    }
    modImplementation("net.fabricmc:fabric-loader:${deps["fabric_loader"]}")
    fapi(
        // Add modules from https://github.com/FabricMC/fabric
        "fabric-lifecycle-events-v1",
    )
}

loom {
    if (mcVersion.startsWith("26.")) {
        @Suppress("UnstableApiUsage")
        intermediaryUrl.set(
            "file://${rootProject.file("versions/26.1/identity-intermediary.jar").absolutePath}"
        )
        // NO noIntermediateMappings() — the full chain must be preserved for mod jars
        enableTransitiveAccessWideners.set(false)
    }

    decompilers {
        get("vineflower").apply { // Adds names to lambdas - useful for mixins
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    val java = if (mcVersion.startsWith("26."))
        JavaVersion.VERSION_25
    else if (stonecutter.eval(mcVersion, ">=1.20.6"))
        JavaVersion.VERSION_21
    else
        JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

tasks.processResources {
    inputs.property("id", mod.id)
    inputs.property("name", mod.name)
    inputs.property("version", mod.version)
    inputs.property("mcdep", mcDep)
    inputs.property("java", mod.java)

    val map = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "mcdep" to mcDep,
        "java" to mod.java
    )

    filesMatching("fabric.mod.json") { expand(map) }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
    dependsOn("build")
}

/*
publishMods {
    file = tasks.remapJar.get().archiveFile
    additionalFiles.from(tasks.remapSourcesJar.get().archiveFile)
    displayName = "${mod.name} ${mod.version} for $mcVersion"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN")
        .getOrNull() == null || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }
}
*/

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${mod.id}"
            artifactId = mod.version
            version = mcVersion

            from(components["java"])
        }
    }
}
