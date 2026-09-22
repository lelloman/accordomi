import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val signingProperties = Properties().apply {
    val signingPropertiesFile = rootProject.file("signing.properties")
    if (signingPropertiesFile.exists()) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.lelloman.accordomi"
    compileSdk = 37
    ndkVersion = "27.0.12077973"
    externalNativeBuild {
        cmake {
            path = rootProject.file("native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = "com.lelloman.accordomi"
        minSdk = 29
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake { arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON" }
        }
    }

    signingConfigs {
        if (signingProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.lelloman:lellodesign-compose:0.2.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// JVM regression tests load a host build of the same JNI/C sources used by Android.
val hostNativeDirectory = rootProject.layout.buildDirectory.dir("native-jvm")
val configureHostNative = tasks.register<Exec>("configureHostNative") {
    inputs.files(rootProject.fileTree("native") { exclude("README.md") })
    outputs.file(hostNativeDirectory.map { it.file("CMakeCache.txt") })
    commandLine("cmake", "-S", rootProject.file("native").absolutePath,
        "-B", hostNativeDirectory.get().asFile.absolutePath,
        "-DAC_BUILD_JNI=ON", "-DCMAKE_BUILD_TYPE=Release")
}
val buildHostNative = tasks.register<Exec>("buildHostNative") {
    dependsOn(configureHostNative)
    inputs.files(rootProject.fileTree("native") { exclude("README.md") })
    outputs.file(hostNativeDirectory.map { it.file(System.mapLibraryName("accordomi_jni")) })
    commandLine("cmake", "--build", hostNativeDirectory.get().asFile.absolutePath,
        "--target", "accordomi_jni", "--parallel", "2")
}
tasks.withType<Test>().configureEach {
    dependsOn(buildHostNative)
    inputs.file(hostNativeDirectory.map { it.file(System.mapLibraryName("accordomi_jni")) })
    systemProperty("java.library.path", hostNativeDirectory.get().asFile.absolutePath)
}
