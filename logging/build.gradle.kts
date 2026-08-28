plugins {
    id("no.nav.helse.sas.sas-kotlin")
}

dependencies {
    implementation(libs.kotlinx.coroutines.slf4j)
    api(libs.slf4jApi)
    api(libs.logback.classic)
    api(libs.logstash.logback.encoder)

    testImplementation(kotlin("test"))
}
