plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(libs.hikaricp)
    api(libs.flyway.database.postgresql)
    implementation(libs.tbd.libs.naisful.postgres)
    runtimeOnly(libs.postgresql)

    testFixturesApi(libs.hikaricp)
    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesImplementation(libs.flyway.database.postgresql)
    testFixturesRuntimeOnly(libs.postgresql)

    testImplementation(kotlin("test"))
}
