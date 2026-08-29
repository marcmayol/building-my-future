import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Firma de release: se lee de keystore.properties (fuera de git). Si no existe,
// se compila sin firma configurada (útil en CI/otros equipos) sin romper el build.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.marc.gymplan100"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.marc.gymplan100"
        minSdk = 26
        targetSdk = 35
        // Fuente única en gradle.properties: la comparten el reloj y el manifiesto
        // de actualizaciones, así que no puede haber dos numeraciones desalineadas.
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersionName").get()
        vectorDrawables { useSupportLibrary = true }
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

    // Dos formas de repartir la MISMA app:
    //
    //  - `play`    va a Google Play, que prohibe que una app se actualice por fuera de la
    //              tienda: aqui no entra el modulo :actualizador ni sus permisos.
    //  - `directo` es la de siempre (DracApps y el movil de casa), con su auto-actualizacion.
    //              Lleva sufijo en el applicationId para poder tener las dos a la vez: una
    //              como app de verdad y otra como banco de pruebas.
    flavorDimensions += "distribucion"
    productFlavors {
        create("play") {
            dimension = "distribucion"
            // Identidad nueva y definitiva para la tienda, sobre el dominio propio: el
            // applicationId de una app publicada en Play NO se puede cambiar nunca, y
            // "gymplan100" era el nombre de trabajo de hace cuatro versiones, no el de la app.
            applicationId = "com.marcmayol.buildingmyfuture"
        }
        create("directo") {
            dimension = "distribucion"
            // Se queda con el applicationId de siempre a proposito: es la que ya esta
            // instalada en los moviles de casa, y cambiarselo la convertiria en otra app
            // distinta que habria que instalar a mano perdiendo el progreso.
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
        // El módulo :actualizador compara contra BuildConfig.VERSION_CODE.
        buildConfig = true
    }
    lint {
        // Falso positivo: registramos el contrato de permisos de Health Connect sobre una
        // ComponentActivity (no Fragment), pero el check exige Fragment 1.3.0+. No aplica.
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

dependencies {
    // Solo en la variante que se reparte fuera de Play.
    "directoImplementation"(project(":actualizador"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.health.connect.client)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
