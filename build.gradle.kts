import java.net.URL
import java.io.FileOutputStream

plugins {
    `java-library`
    alias(libs.plugins.vanilla.gradle) apply false

    `maven-publish`
    signing
    alias(libs.plugins.nexuspublish)
}

group = "net.minestom"
version = System.getenv("TAG_VERSION") ?: "${libs.versions.minecraft.get()}-dev"
description = "Generator for Minecraft game data values"

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.register("generateData") {
    logger.warn("Mojang requires all source-code and mappings used to be governed by the Minecraft EULA.")
    logger.warn("Please read the Minecraft EULA located at https://account.mojang.com/documents/minecraft_eula.")
    logger.warn("In order to agree to the EULA you must create a file called eula.txt with the text 'eula=true'.")
    val eulaTxt = File("${rootProject.projectDir}/eula.txt")
    logger.warn("The file must be located at '${eulaTxt.absolutePath}'.")
    if ((eulaTxt.exists() && eulaTxt.readText(Charsets.UTF_8).equals("eula=true", true))
            || project.properties["eula"].toString().toBoolean()
            || System.getenv("EULA")?.toBoolean() == true
    ) {
        logger.warn("")
        logger.warn("The EULA has been accepted and signed.")
        logger.warn("")
    } else {
        throw GradleException("Data generation has been halted as the EULA has not been signed.")
    }
    logger.warn("It is unclear if the data from the data generator also adhere to the Minecraft EULA.")
    logger.warn("Please consult your own legal team!")
    logger.warn("All data is given independently without warranty, guarantee or liability of any kind.")
    logger.warn("The data may or may not be the intellectual property of Mojang Studios.")
    logger.warn("")

    // Simplified by Sponge's VanillaGradle
    dependsOn(project(":DataGenerator").tasks.getByName<JavaExec>("run") {
        args = arrayListOf(rootDir.resolve("src/main/resources").absolutePath)
    })
}

tasks.processResources.get().dependsOn("generateData")

abstract class DownloadFilesTask : DefaultTask() {

    @get:Input
    abstract val filesToDownload: MapProperty<String, String>

    @get:OutputFiles
    val outputFiles: FileCollection = project.layout.files(
            filesToDownload.map { it.keys.map(project::file) }
    )

    @TaskAction
    fun downloadFiles() {
        filesToDownload.get().forEach { (destPath, url) ->
            val destFile = project.file(destPath).apply { parentFile.mkdirs() }

            logger.lifecycle("Downloading $url")
            URL(url).openStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            logger.lifecycle("Saved to $destFile")
        }
    }
}

val downloadMappings = tasks.register<DownloadFilesTask>("downloadMappings") {
    filesToDownload.set(mapOf(
            "mappings/sounds.json" to "https://raw.githubusercontent.com/Draglis/GeyserMCMappings/refs/heads/master/sounds.json"
    ))
}

nexusPublishing {
    this.packageGroup.set("net.minestom")

    repositories.sonatype {
        nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
        snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

        if (System.getenv("SONATYPE_USERNAME") != null) {
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

publishing.publications.create<MavenPublication>("maven") {
    groupId = "net.minestom"
    artifactId = "data"
    version = project.version.toString()

    from(project.components["java"])

    pom {
        name.set("data")
        description.set("Minecraft game data values")
        url.set("https://github.com/minestom/MinestomDataGenerator")

        licenses {
            license {
                name.set("Apache 2.0")
                url.set("https://github.com/minestom/MinestomDataGenerator/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("mworzala")
                name.set("Matt Worzala")
                email.set("matt@hollowcube.dev")
            }
            developer {
                id.set("TheMode")
            }
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/minestom/MinestomDataGenerator/issues")
        }

        scm {
            connection.set("scm:git:git://github.com/minestom/MinestomDataGenerator.git")
            developerConnection.set("scm:git:git@github.com:minestom/MinestomDataGenerator.git")
            url.set("https://github.com/minestom/MinestomDataGenerator")
            tag.set("HEAD")
        }

        ciManagement {
            system.set("Github Actions")
            url.set("https://github.com/minestom/MinestomDataGenerator/actions")
        }
    }
}

signing {
    isRequired = System.getenv("CI") != null

    val privateKey = System.getenv("GPG_PRIVATE_KEY")
    val keyPassphrase = System.getenv()["GPG_PASSPHRASE"]
    useInMemoryPgpKeys(privateKey, keyPassphrase)

    sign(publishing.publications)
}
