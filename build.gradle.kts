plugins {
    alias(libs.plugins.sas.root)
    alias(libs.plugins.sas.kotlin) apply false
}

subprojects {
    // Alle modulene i dette prosjektet er biblioteker som publiseres til GitHub Package Registry.
    apply(plugin = "maven-publish")

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            withSourcesJar()
        }
        configure<PublishingExtension> {
            publications.register<MavenPublication>("maven") {
                from(components["java"])
            }
            repositories.maven("https://maven.pkg.github.com/navikt/sykepenger-libs") {
                name = "githubPackages"
                // Leser ORG_GRADLE_PROJECT_githubPackagesUsername / ORG_GRADLE_PROJECT_githubPackagesPassword.
                credentials(PasswordCredentials::class)
            }
        }
    }
}
