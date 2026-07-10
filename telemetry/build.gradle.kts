plugins {
    id("mihon.library")
    kotlin("android")
}

android {
    namespace = "mihon.telemetry"

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/noop/kotlin")
            manifest.srcFile("src/noop/AndroidManifext.xml")
        }
    }
}

dependencies {
    // Better logging (EH)
    implementation(sylibs.xlog)
}
