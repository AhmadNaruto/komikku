plugins {
    id("mihon.library")
    alias(libs.plugins.androidKotlinMultiplatform)
    alias(libs.plugins.moko)
    id("com.github.ben-manes.versions")
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.moko.core)
            }
        }
    }
}

android {
    namespace = "tachiyomi.i18n.kmk"

    sourceSets {
        named("main") {
            res.srcDir("src/commonMain/resources")
        }
    }

    lint {
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation"))
    }
}

multiplatformResources {
    resourcesClassName.set("KMR")
    resourcesPackage.set("tachiyomi.i18n.kmk")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.freeCompilerArgs.addAll(
        "-Xexpect-actual-classes",
    )
}
