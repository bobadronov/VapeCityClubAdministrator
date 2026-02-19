@file:OptIn(ExperimentalWasmDsl::class)

import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.time.LocalDateTime
import java.util.Properties


// ./gradlew jsBrowserProductionWebpack
// python -m http.server 8000
// composeApp/build/dist/js/productionExecutable/
// composeApp:hotRunJvm --autoReload

private val isDebugBuild = libs.versions.isDebugBuild.get().toBoolean()
private val major = libs.versions.major.get().toInt()
private val minor = libs.versions.minor.get().toInt()
private val patch = libs.versions.patch.get().toInt()
private val subpath = libs.versions.subpath.get().toInt()
private val desktopBuild = patch * 100 + subpath              // 0..65535
private val projectVersionName = "$major.$minor.$desktopBuild"        // те, що показуєш користувачу
private val desktopVersion = "$major.$minor.$desktopBuild"        // MAJOR.MINOR.BUILD
private val androidVersionCode: Int =
    libs.versions.major.get().toInt() * 1000 + libs.versions.minor.get().toInt() * 100 + libs.versions.patch.get().toInt() * 10 + libs.versions.subpath.get().toInt()
private val currentYear: Int = LocalDateTime.now().toLocalDate().year

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    applyDefaultHierarchyTemplate()

    jvmToolchain(17)

    androidTarget()

//    iosX64()
    iosArm64()
//    iosSimulatorArm64()

    jvm()

//    js {
//        browser()
//        binaries.executable()
//    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.bundles.ui)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.koin)
            implementation(libs.bundles.ktor)
            implementation(libs.bundles.navigation3)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.bundles.supabase)
            implementation(libs.bundles.filekit)
            implementation(libs.bundles.connectivity)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.napier)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.openai.client)
//            implementation("com.dshatz.pdfmp:pdfmp-compose:1.0.9") # https://github.com/dshatz/pdfmp/
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.bundles.androidOnly)
        }

        wasmJsMain.dependencies {
            implementation(libs.bundles.webOnly)
            implementation(npm("fflate", "0.8.2"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.bundles.jvmOnly)
        }

        iosMain.dependencies {
            implementation(libs.bundles.iosOnly)
        }

    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "SharedUI"
                    isStatic = true
                }
            }
        }

}

extensions.configure<ApplicationExtension> {

    namespace = "org.bigblackowl.vccadmin"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.bigblackowl.vccadmin"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = androidVersionCode
        versionName = projectVersionName
        manifestPlaceholders["appName"] = libs.versions.appName.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            val ksPath = System.getenv("ANDROID_KEYSTORE_PATH") // D:\Мій диск\Работа\SlideShow\key\slide_show_key.jks
            if (!ksPath.isNullOrBlank()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

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

}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

compose.desktop {
    application {

        mainClass = "org.bigblackowl.vccadmin.MainKt"

        nativeDistributions {

            outputBaseDir.set(project.layout.projectDirectory.dir("release_desktop"))

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = libs.versions.appName.get()

            packageVersion = desktopVersion

            version = desktopVersion

            description = "${libs.versions.appName.get()} — corporate tool for administering a chain of stores: users, access rights, and content."

            copyright = "© 2022–${currentYear} BigBlackOwl. All trademarks are property of their respective owners."

            vendor = "BigBlackOwl"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
                packageVersion = desktopVersion
            }

            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
                shortcut = true
                console = false
                dirChooser = false
                msiPackageVersion = desktopVersion
            }

            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "org.bigblackowl.vccadmin.desktopApp"
                dmgPackageBuildVersion = desktopVersion
                dmgPackageVersion = desktopVersion
            }
        }

        buildTypes.release.proguard {
            version = "7.8.0"
            isEnabled = false  // false to disable proguard
            optimize = true
            obfuscate = true
//            configurationFiles.from(file("proguard-rules.pro"))
        }
    }
}

val envProps = Properties().apply {
    load(rootProject.file(".env").inputStream())
}

buildConfig {
    packageName("org.bigblackowl.vccadmin")
    buildConfigField("APP_NAME", libs.versions.appName.get())
    buildConfigField("IS_DEBUG_BUILD", isDebugBuild)
    buildConfigField("APP_VERSION", projectVersionName)
    buildConfigField("SUPABASE_URL", "${envProps["SUPABASE_URL"] ?: ""}")
    buildConfigField("SUPABASE_KEY", "${envProps["SUPABASE_KEY"] ?: ""}")
    buildConfigField("OPEN_AI_KEY", "${envProps["OPEN_AI_KEY"] ?: ""}")
}
