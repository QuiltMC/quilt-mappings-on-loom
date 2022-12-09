plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
    id("com.diffplug.spotless") version "6.0.0"
}

group = "org.quiltmc"
// Don't forget to update README.MD
version = "4.2.3"

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
    implementation("net.fabricmc:fabric-loom:0.11-SNAPSHOT")
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

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

publishing {
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
