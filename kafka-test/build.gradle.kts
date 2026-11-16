plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    api(libs.testcontainers.kafka)
    implementation(libs.kafka.clients)

    testImplementation(kotlin("test"))
}
