import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

android {
    namespace = "io.github.information89.motion.lab"
    compileSdk = libs.versions.sdk.compile.get().toInt()
    buildToolsVersion = libs.versions.build.tools.get()

    defaultConfig {
        applicationId = "io.github.information89.motion.lab"
        minSdk = libs.versions.sdk.min.get().toInt()
        targetSdk = libs.versions.sdk.compile.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

spotless {
    kotlin {
        target("**/*.kt", "**/*.kts")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation(project(":motion"))
    implementation(project(":motion-ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.test.manifest)
}
