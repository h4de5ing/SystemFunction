plugins {
    alias(libs.plugins.application)
}

android {
    namespace = "com.android.t"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.t"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("sign") {
            storeFile = file("D:\\Android12SignerGUI\\SignFiles\\NewPublic\\platform.jks")
            storePassword = "android"
            keyAlias = "android"
            keyPassword = "android"
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sign")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sign")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":SystemLib"))
}
