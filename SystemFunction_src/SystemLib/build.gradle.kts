import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.kotlin)
    `maven-publish`
}

android {
    compileSdk = 36
    namespace = "com.android.systemlib"
    
    defaultConfig {
        minSdk = 21
        targetSdk = 36
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