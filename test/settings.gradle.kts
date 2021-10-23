pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		maven {
			url = uri("https://maven.fabricmc.net/")
		}
		maven { url = uri("https://maven.oroarmor.com") }
	}
}

rootProject.name = "quilt-mappings-on-loom-test"