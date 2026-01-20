import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.library)
    `maven-publish`
}

android {
    compileSdk = 36
    namespace = "com.android.android14"

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(files("libs/classes-header.jar"))
    implementation(libs.androidx.core.ktx)
}

afterEvaluate {
    publishing {
        publications {
            val today = SimpleDateFormat("yyyyMMdd").format(Date())
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.android.android14"
                artifactId = "android14"
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