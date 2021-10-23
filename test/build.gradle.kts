plugins {
    java
    id("fabric-loom") version "0.10-SNAPSHOT"
    id("quilt-mappings-on-loom") version "1.0.0"
}

group = "com.oroarmor"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

var minecraft_version = "21w42a"
var quilt_mappings = "21w42a+build.1"
var loader_version = "0.12.2"

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        addLayer(quiltmappings.mappings("org.quiltmc:quilt-mappings:${quilt_mappings}:v2", true))
    })
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
}
