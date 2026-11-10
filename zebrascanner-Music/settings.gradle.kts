pluginManagement {
    repositories {
        maven {
            url = uri("https://repo.itextsupport.com/android")


        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        maven(url = "https://jitpack.io")
        maven(url = "https://repository.apache.org/snapshots/")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = uri("https://maven.pkg.github.com/OWNER/REPO")
        }
        maven {
            url = uri("https://repo.itextsupport.com/android")


        }

    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://repo.itextsupport.com/android")


        }
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://repository.apache.org/snapshots/")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = uri("https://maven.pkg.github.com/OWNER/REPO")
        }

    }
}

rootProject.name = "zebrascanner"
include(":app")
 