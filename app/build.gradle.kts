plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "cn.parentchess"
    compileSdk = 35
    defaultConfig { applicationId = "cn.parentchess"; minSdk = 26; targetSdk = 35; versionCode = 11; versionName = "1.5.2" }
    signingConfigs {
        create("family") {
            storeFile = rootProject.file(".signing/family.jks")
            storePassword = System.getenv("CHESS_KEY_PASSWORD") ?: ""
            keyAlias = "family"
            keyPassword = System.getenv("CHESS_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes { getByName("release") { signingConfig = signingConfigs.getByName("family"); isMinifyEnabled = false } }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation(project(":chesslib"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
}
