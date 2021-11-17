plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
    id("com.diffplug.spotless") version "6.0.0"
}

group = "org.quiltmc"
version = "3.1.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

spotless {
    java {
        // Use comma separator for openjdk like license headers
        licenseHeaderFile(file("HEADER")).yearSeparator(", ")
    }
}

dependencies {
    implementation("net.fabricmc:fabric-loom:0.10-SNAPSHOT")
    implementation("net.fabricmc:mapping-io:0.2.1")
    implementation("net.fabricmc:tiny-mappings-parser:0.2.2.14")
}

gradlePlugin {
    plugins {
        create("quiltMappingsLoom") {
            id = "org.quiltmc.quilt-mappings-on-loom"
            implementationClass = "org.quiltmc.quiltmappings.loom.QuiltMappingsOnLoomPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.jar)
        }
    }

    repositories {
        mavenLocal()
        if (System.getenv("MAVEN_URL") != null) {
            maven {
                setUrl(System.getenv("MAVEN_URL"))
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
                name = "Maven"
            }
        }
    }
}