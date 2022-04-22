pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		maven {
			url = uri("https://maven.fabricmc.net/")
		}
		maven { url = uri("https://maven.quiltmc.org/repository/release") }
	}
	includeBuild("..")
}

rootProject.name = "quilt-mappings-on-loom-test"