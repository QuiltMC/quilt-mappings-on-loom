pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		maven {
			url = uri("https://maven.fabricmc.net/")
		}
		maven { url = uri("https://maven.quiltmc.org/repository/release") }
	}
}

rootProject.name = "quilt-mappings-on-loom-test"