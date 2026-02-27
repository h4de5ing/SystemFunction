pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://github.com/h4de5ing/SystemFunction/raw/master/SystemLib_repository") }
        maven { url = uri("https://gitee.com/lex1992/repository/raw/master/repository") }
    }
}

rootProject.name = "SystemFunction"

include(":SystemLib")
include(":Android12")
include(":Android13")
include(":Android14")
include(":Android15")
include(":Android16")
include(":t")