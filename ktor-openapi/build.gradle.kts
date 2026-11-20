plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(libs.ktor.server.routing.openapi)
    implementation(libs.ktor.openapi.schema.reflect)
    implementation(libs.ktor.server.swagger)

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(platform(libs.jackson3.bom))
    testFixturesImplementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(platform(libs.jackson3.bom))
    testImplementation("tools.jackson.module:jackson-module-kotlin")
}
