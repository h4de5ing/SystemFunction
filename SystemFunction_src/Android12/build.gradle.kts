import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.library)
    `maven-publish`
}

android {
    compileSdk = 36
    namespace = "com.android.android12"

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