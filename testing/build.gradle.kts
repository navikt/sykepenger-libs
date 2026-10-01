plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    api(kotlin("test"))
    api(platform(libs.jackson3.bom))
    api("tools.jackson.module:jackson-module-kotlin")
}
