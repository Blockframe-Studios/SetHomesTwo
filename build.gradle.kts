// Gradle is the end-to-end test runner and nothing else. Maven builds the jar;
// useExternalPluginsOnly stops Plugwright looking for a Gradle jar task.
plugins {
    id("io.github.drownek.plugwright") version "2.0.3"
}

// Newest first, matching the rule Spigot/start.bat already uses. The shade
// plugin also leaves original-SetHomesTwo-*.jar in target/, which this skips,
// and stale jars from earlier versions can linger until mvn clean.
fun newestPluginJar(): File {
    val jars = file("target")
        .listFiles { f -> f.isFile && f.name.startsWith("SetHomesTwo-") && f.name.endsWith(".jar") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()
    if (jars.isEmpty()) {
        throw GradleException("No SetHomesTwo-*.jar found in target/. Run: mvn package")
    }
    return jars.first()
}

plugwright {
    minecraftVersion.set("1.21.11")
    acceptEula.set(true)
    testsDir.set(file("src/test/e2e"))
    useExternalPluginsOnly.set(true)

    // Never report to the public bStats dashboard from a server we run.
    jvmArgs.set(listOf("-Xmx2G", "-Dsethomestwo.metrics.disabled=true"))

    // CI installs Node itself; a workstation may have none.
    downloadNode.set(System.getenv("CI") != "true")

    writeFiles {
        file(
            "plugins/SetHomesTwo/config.yml",
            """
            inventoryTitle: "Your homes"
            maxHomeEnabled: false
            delay: 0
            cancelOnMove: false
            teleportSafety: false
            checkForUpdates: false
            debugLevel: "info"
            permissions:
              sh2.player: op
            """.trimIndent()
        )
    }
}

// Set lazily so an unbuilt target/ fails when the suite runs, with a message
// naming the fix, rather than at configuration time on every Gradle command.
tasks.named<me.drownek.plugwright.PlugwrightTestTask>("plugwrightTest") {
    pluginJar.set(providers.provider { newestPluginJar() })
    dependsOn("plugwrightClean")
}

tasks.named<me.drownek.plugwright.PlugwrightRunTask>("plugwrightRunServer") {
    pluginJar.set(providers.provider { newestPluginJar() })
}
