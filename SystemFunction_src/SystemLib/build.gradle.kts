import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.library)
    `maven-publish`
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.android.systemlib"

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

    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":Android12"))
    implementation(project(":Android13"))
    implementation(project(":Android14"))
    implementation(project(":Android15"))
    implementation(project(":Android16"))
    compileOnly(files("libs/classes.jar")) // Android10
}

afterEvaluate {
    publishing {
        publications {
            val today = SimpleDateFormat("yyyyMMdd").format(Date())
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.android.systemlib"
                artifactId = "systemlib"
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