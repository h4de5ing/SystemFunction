import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.library)
    `maven-publish`
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.android.android12"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
        targetCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(files("libs/framework.jar"))
    implementation(libs.androidx.core.ktx)
}

afterEvaluate {
    publishing {
        publications {
            val today = SimpleDateFormat("yyyyMMdd").format(Date())
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.android.android12"
                artifactId = "android12"
                version = "1.0-$today"
            }
        }
        repositories {
            mavenLocal()
            maven {
                url = uri("../../SystemLib_repository")
            }
        }
    }
}