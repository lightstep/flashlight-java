import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("me.qoomon.git-versioning") version "6.4.2"

    id("java")
}

group = "com.lightstep.flashlight"
version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
    refs {
        branch(".+") {
            version = "\${ref}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }
    rev {
        version = "\${commit}"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    // Needed for https://github.com/FoxSamu/ASM-Descriptor
    maven {
        url = uri("https://maven.shadew.net/")
    }
}

dependencies {
    implementation("info.picocli:picocli:4.7.2")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-util:9.4")

    // https://github.com/FoxSamu/ASM-Descriptor
    implementation("net.shadew:descriptor:1.0") {
        exclude("org.ow2.asm", "asm-commons")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks {
    getByName<Test>("test") {
        useJUnitPlatform()
    }

    val shadowJar by existing(ShadowJar::class) {
        archiveClassifier.set("")
        archiveVersion.set("")

        manifest {
            attributes(jar.get().manifest.attributes)
            attributes("Main-Class" to "com.lightstep.flashlight.FlashlightMain",
                    "Implementation-Title" to "Flashlight",
                    "Implementation-Version" to project.version)
        }
        minimize()
    }

    named("build") {
        dependsOn(shadowJar)
    }

    named("jar") {
        enabled = false
    }
}
