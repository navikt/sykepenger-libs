plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    api(libs.sykepenger.libs.leader.election)
    api(libs.tbd.libs.rapids.and.rivers.api)
    implementation(libs.sykepenger.libs.logging)

    testImplementation(kotlin("test"))
    testImplementation(testFixtures(libs.sykepenger.libs.postgres))
    testImplementation(libs.sykepenger.libs.testing)
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
}
