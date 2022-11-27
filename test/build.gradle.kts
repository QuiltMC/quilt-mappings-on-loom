plugins {
    java
    id("fabric-loom") version "1.0.+"
    id("org.quiltmc.quilt-mappings-on-loom")
}

group = "org.quiltmc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

var minecraft_version = "1.18.2"
// Test QMoL crashing with post-Intermediary publication builds by swtichting to 1.18.2+build.26
var quilt_mappings = "1.18.2+build.24"
var loader_version = "0.14.10"

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:${quilt_mappings}:v2"))
    })
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
}
