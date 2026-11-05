plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    api(project(":leader-election"))
    api(libs.tbd.libs.rapids.and.rivers.api)
    implementation(project(":logging"))

    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":postgres")))
    testImplementation(project(":testing"))
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
}
