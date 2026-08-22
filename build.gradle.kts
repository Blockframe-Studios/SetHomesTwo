// Gradle is the end-to-end test runner and nothing else. Maven builds the jar;
// useExternalPluginsOnly stops Plugwright looking for a Gradle jar task.
plugins {
    id("io.github.drownek.plugwright") version "2.0.3"
}

// The shade plugin also leaves original-SetHomesTwo-*.jar in target/, and jars
// from earlier versions linger until mvn clean, so take the newest match.
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

    // sh2.player is granted to everyone and the bypass nodes stay op-only, so the
    // delay, cancel-on-move and the max-homes cap bind ordinary players while an op
    // keeps the instant, uncapped path.
    writeFiles {
        file(
            "plugins/SetHomesTwo/config.yml",
            """
            inventoryTitle: "E2E homes"
            maxHomeEnabled: true
            maxHomesType: "singular"
            maxHomes: 3
            delay: 2
            cancelOnMove: true
            teleportSafety: true
            checkForUpdates: false
            debugLevel: "info"
            permissions:
              sh2.player: true
              sh2.move-home: op
            """.trimIndent()
        )
    }
}

// Lazy, so an unbuilt target/ fails when the suite runs rather than on every
// Gradle command.
tasks.named<me.drownek.plugwright.PlugwrightTestTask>("plugwrightTest") {
    pluginJar.set(providers.provider { newestPluginJar() })
    dependsOn("plugwrightClean")
}

tasks.named<me.drownek.plugwright.PlugwrightRunTask>("plugwrightRunServer") {
    pluginJar.set(providers.provider { newestPluginJar() })
}
