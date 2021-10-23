plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.oroarmor"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation("net.fabricmc:fabric-loom:0.10-SNAPSHOT")
    implementation("net.fabricmc:mapping-io:0.2.1")
    implementation("net.fabricmc:lorenz-tiny:4.0.2")
    implementation("net.fabricmc:tiny-mappings-parser:0.2.2.14")
    implementation("net.fabricmc:stitch:0.6.1")
}

gradlePlugin {
    plugins {
        create("quiltMappingsLoom") {
            id = "quilt-mappings-on-loom"
            implementationClass = "com.oroarmor.quiltmappings.loom.QuiltMappingsOnLoomPlugin"
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
                name = "OroArmorMaven"
            }
        }
    }
}