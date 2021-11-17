plugins {
    java
    id("fabric-loom") version "0.10-SNAPSHOT"
    id("quilt-mappings-on-loom") version "3.0.0"
}

group = "org.quiltmc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

var minecraft_version = "1.18-pre2"
var quilt_mappings = "1.18-pre2+build.3"
var loader_version = "0.12.2"

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:${quilt_mappings}:v2"))
    })
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
}
