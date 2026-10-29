plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    api(libs.ktor.server.auth.jwt)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mock.oauth2.server)
}
