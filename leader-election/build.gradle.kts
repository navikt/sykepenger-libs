plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    implementation(libs.sykepenger.libs.logging)
    implementation(platform(libs.jackson3.bom))
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation(kotlin("test"))
}
