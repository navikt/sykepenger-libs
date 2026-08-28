plugins {
    id("no.nav.helse.sas.sas-kotlin")
}

dependencies {
    api(kotlin("test"))
    api(platform(libs.jackson3.bom))
    api("tools.jackson.module:jackson-module-kotlin")
}
