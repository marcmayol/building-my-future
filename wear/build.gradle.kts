import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Misma firma que el móvil: el APK del reloj se publica como asset de la Release para
// poder instalarlo por adb sin builds de depuración.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.marc.gymplan100.wear"
    compileSdk = 35

    defaultConfig {
        // Mismo applicationId que el móvil: así Wear OS las reconoce como la misma app
        // y la instalación del reloj se asocia al teléfono.
        applicationId = "com.marc.gymplan100"
        minSdk = 30
        targetSdk = 35
        // Misma versión que el móvil (gradle.properties): el APK del reloj viaja como
        // asset de la misma Release y así se sabe de un vistazo cuál llevas puesta.
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersionName").get()
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    // Los mismos dos sabores que el movil, y por un motivo que no es cosmetico: la capa de
    // datos de Wear OS empareja el reloj con el telefono por applicationId. Un reloj
    // `com.marc.gymplan100` no le hablaria a la app de Play, que es
    // `com.marcmayol.buildingmyfuture`, y el mando dejaria de funcionar.
    flavorDimensions += "distribucion"
    productFlavors {
        create("play") {
            dimension = "distribucion"
            applicationId = "com.marcmayol.buildingmyfuture"
            // Movil y reloj van en la MISMA ficha de Play, y Play exige que cada artefacto
            // tenga su propio versionCode: con los dos en 28 rechaza el segundo que subas.
            // El offset deja las dos series separadas para siempre y se lee de un vistazo
            // (10028 = reloj de la 28) en vez de tener que acordarse de sumar uno a mano.
            versionCode = providers.gradleProperty("appVersionCode").get().toInt() + 10_000
        }
        create("directo") {
            dimension = "distribucion"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear)
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.play.services.wearable)
    debugImplementation(libs.androidx.ui.tooling)
}
