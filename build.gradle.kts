import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayExtension.*
import groovy.util.Node
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.builtins.isNumberedFunctionClassFqName
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.2.10"
    id("com.jfrog.bintray") version "1.7.3"
    jacoco
    `maven-publish`
}

group = "ru.gildor.coroutines"
version = "0.9.0"
description = "Provides Kotlin Coroutines suspendable await() extensions for Retrofit Call"

repositories {
    jcenter()
}

java {
    targetCompatibility = JavaVersion.VERSION_1_6
    sourceCompatibility = JavaVersion.VERSION_1_6
}

dependencies {
    val kotlinVersion = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion
    compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.20")
    compile("com.squareup.retrofit2:retrofit:2.3.0")
    testCompile("junit:junit:4.12")
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

tasks {
    val jacocoTestReport by getting(JacocoReport::class) {
        reports.xml.isEnabled = true
    }
    this["test"].finalizedBy(jacocoTestReport)
}


/* Publishing */

val githubId = "gildor/kotlin-coroutines-retrofit"
val repoWeb = "https://github.com/$githubId"
val repoVcs = "$repoWeb.git"
val tags = listOf("retrofit", "kotlin", "coroutines")
val licenseId = "Apache-2.0"
val licenseName = "The Apache Software License, Version 2.0"
val licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
val releaseTag = "v${project.version}"

val sourcesJar by tasks.creating(Jar::class) {
    //}, dependsOn: classes) {
    dependsOn("classes")
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    val javadoc by tasks.getting(Javadoc::class)
    dependsOn(javadoc)
    classifier = "javadoc"
    from(javadoc.destinationDir)
}

publishing {
    publications {
        create("MavenJava", MavenPublication::class.java) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom.withXml {
                asNode().apply {
                    appendNode("name", project.name)
                    appendNode("description", project.description)
                    appendNode("url", repoWeb)
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("name", "Andrey Mischenko")
                        appendNode("email", "git@gildor.ru")
                        appendNode("organizationUrl", "https://github.com/gildor")
                    }
                    appendNode("issueManagement").apply {
                        appendNode("system", "GitHub Issues")
                        appendNode("url", "$repoWeb/issues")
                    }
                    appendNode("scm").apply {
                        appendNode("url", repoWeb)
                        appendNode("connection", "scm:git:$repoVcs")
                        appendNode("developerConnection", "scm:git:$repoVcs")
                        appendNode("tag", releaseTag)
                    }
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", licenseName)
                        appendNode("url", licenseUrl)
                    }
                }
            }
        }
    }
}

bintray {
    user = project.properties["bintray.user"]?.toString()
    key = project.properties["bintray.key"]?.toString()
    setPublications("MavenJava")
    publish = true
    pkg(delegateClosureOf<PackageConfig> {
        repo = project.properties["bintray.repo"]?.toString() ?: "maven"
        name = project.name
        desc = description
        githubRepo = githubId
        githubReleaseNotesFile = "CHANGELOG.md"
        websiteUrl = repoWeb
        issueTrackerUrl = "$repoWeb/issues"
        vcsUrl = repoVcs
        setLicenses(licenseId)
        setLabels(*tags.toTypedArray())
        version(delegateClosureOf<VersionConfig> {
            name = project.version.toString()
            vcsTag = releaseTag
            mavenCentralSync(delegateClosureOf<MavenCentralSyncConfig> {
                sync = project.properties["sonatype.user"] != null
                user = project.properties["sonatype.user"]?.toString()
                password = project.properties["sonatype.password"]?.toString()
                close = "true"
            })
        })
    })
}
