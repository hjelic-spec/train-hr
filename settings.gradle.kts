pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.10.1"
        id("org.jetbrains.kotlin.android") version "2.2.20"
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
        id("com.google.devtools.ksp") version "2.2.20-2.0.3"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "train-hr"

include(":core")
// Android modul se preskače s -PcoreOnly (npr. u okruženju bez Android SDK-a).
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":app")
}
