# Quilt Mappings on Loom

## **The time has finally arrived! Quilt Mappings are now usable in Loom!**

Ever wanted to use mappings other than Yarn or MojMap? 

[Quilt Mappings](https://github.com/QuiltMC/quilt-mappings) is a Yarn derivative that allows inspiration from MojMap, which allows Quilt Mappings to map the obscure constants in Minecraft code and more. 
This gradle plugin allows Quilt Mappings to be used in Loom and Fabric environments. 
Currently, only 1.17.1+ has support, with no plans of backporting to earlier versions.

## **Benefits of Quilt Mappings:**
1. Faster turn around. 
   - For the past couple weeks, Quilt Mappings has been out before intermediary. While this doesn't help too much in loom, it is something to look forward to in the future.
2. Consistent names. 
   - Quilt Mapping names are never mismatched, meaning you won't find issues where names swap between versions.
3. Familiarity of Yarn with the completeness of MojMap. 
   - While not complete, Quilt Mappings has the goal of 100% mapping coverage for Minecraft. Any help toward this goal would be appreciated as well!

## **How to use Quilt Mappings:**

`settings.gradle`:
```groovy
pluginManagement {
    repositories {
        maven { url = "https://maven.quiltmc.org/repository/release" }
    }
}
```
`build.gradle`:
```groovy
plugins {
  // ...
  id "org.quiltmc.quilt-mappings-on-loom" version "QMoL_VERSION"
}

// ...

dependencies {
   mappings(loom.layered {
      addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:${minecraft_version}+build.${project.quilt_mappings}:v2"))
   })
}
```
### QMoL Versions
Replace `QMoL_VERSION` in `build.gradle` with the version corresponding to the version of Loom being used:
| Loom Version | QMoL Version |
| - | - |
| 0.10 | 3.1.2 |
| 0.11.31- | 4.0.0 |
| 0.11.32+ | 4.2.3 |

Note: Version 4.2.3 deprecates this plugin. No further versions will be released.
