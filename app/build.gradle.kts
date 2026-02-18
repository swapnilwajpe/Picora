plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.swappy.picora"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.swappy.picora"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes.addAll(listOf(
            "META-INF/services/javax.xml.stream.XMLInputFactory",
            "META-INF/services/javax.xml.stream.XMLOutputFactory",
            "META-INF/services/javax.xml.stream.XMLEventFactory",
            "META-INF/DEPENDENCIES"
        ))
        resources.pickFirsts.add("org/jetbrains/annotations/**")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")
    implementation(libs.androidx.material.icons.extended)
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.commons:commons-compress:1.26.1")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-io:commons-io:2.16.1")
    implementation("com.github.virtuald:curvesapi:1.08")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
