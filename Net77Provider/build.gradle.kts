plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.lagradost.cloudstream3.gradle")
}

android {
    namespace = "com.horis.net77"
    defaultConfig {
        minSdk = 21
        compileSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
}

dependencies {
    implementation("com.github.recloudstream.cloudstream:library:-SNAPSHOT")
    implementation("com.github.Blatzar:NiceHttp:0.4.11")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("androidx.core:core:1.13.1")
    implementation("org.jsoup:jsoup:1.17.2")
}

cloudstream {
    setRepo("https://github.com/manurocky143a-del/Net77-CloudStream")
    description  = "Watch Netflix, Hotstar & Prime Video via net77.cc (NetMirror mirror)"
    iconUrl      = "https://www.google.com/s2/favicons?domain=net77.cc&sz=64"
    language     = "hi"
    version      = 1
    status       = 1   // 1 = Working
    tvTypes      = listOf("Movie", "TvSeries")
    authors      = listOf("manurocky143a-del")
}
