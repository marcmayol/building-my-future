plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

// Lo que comparten el móvil Android y el iPhone: primero la lógica de data/ y, después,
// las pantallas. Lo que solo existe en una plataforma (Health Connect, el reloj, las
// notificaciones, la valoración de Play) se queda en :app o va a iosMain.
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
            api(libs.jb.navigation.compose)
            api(libs.jb.lifecycle.viewmodel.compose)
            api(libs.jb.lifecycle.runtime.compose)
            implementation(libs.compose.backhandler)
            api(compose.components.resources)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            api(libs.coroutines.core)
            api(libs.androidx.datastore.preferences.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.marc.gymplan100.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    // Lo usan también :app (lógica que devuelve frases) y el iPhone.
    publicResClass = true
    packageOfResClass = "com.marc.gymplan100.shared"
    generateResClass = always
}
