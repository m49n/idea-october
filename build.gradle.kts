import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "dev.idea.october"
version = "0.1.34"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        phpstorm(providers.gradleProperty("platformVersion")) {
            useInstaller = false
        }
        bundledPlugin("com.jetbrains.php")
        bundledPlugin("com.jetbrains.twig")
        bundledPlugin("org.jetbrains.plugins.yaml")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.3")
}

intellijPlatform {
    projectName.set("October CMS Support")
    buildSearchableOptions = false

    pluginConfiguration {
        id.set("dev.idea.october")
        name.set("October CMS Support")
        version.set(project.version.toString())
        description.set("Base PhpStorm plugin scaffold for October CMS support.")
        changeNotes.set("Add scoped Twig variables, October filters and page property completion with automatic popups.")

        ideaVersion {
            sinceBuild.set("253")
        }

        vendor {
            name.set("Idea October")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
