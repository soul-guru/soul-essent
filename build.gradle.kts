import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.copyToRecursively

val ktor_version: String by project
val kotlin_version: String by project

plugins {
	kotlin("jvm") version "1.9.22"
	id("com.github.johnrengelman.shadow") version "8.1.1"

	id("io.ktor.plugin") version "2.3.7"
}

group = "org.wireforce"
version = "0.0.1"

ktor {
	docker {
		portMappings.set(listOf(
			io.ktor.plugin.features.DockerPortMapping(
				80,
				8080,
				io.ktor.plugin.features.DockerPortMappingProtocol.TCP
			)
		))
	}
}

tasks {
	named<ShadowJar>("shadowJar") {
		exclude {
			it.path.contains("python/tfc")
		}

		include {
			!it.path.contains("python/tfc")
		}

		doFirst {
			@OptIn(ExperimentalPathApi::class)
			Path("$projectDir/src/main/resources/").copyToRecursively(
				Path("$projectDir/build/libs/resources"),
				overwrite = true,
				followLinks = true
			)
		}
	}
}

application {
	mainClass.set("org.wireforce.ApplicationKt")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("io.ktor:ktor-server-core-jvm:2.2.4")
	implementation("io.ktor:ktor-server-cio-jvm:2.2.4")
	implementation("io.ktor:ktor-client-core:2.2.4")
	implementation("io.ktor:ktor-client-cio:2.2.4")
	implementation("io.ktor:ktor-client-apache:2.2.4")
	implementation("io.ktor:ktor-server-content-negotiation:2.2.4")
	implementation("io.ktor:ktor-serialization-gson:2.2.4")
	implementation("io.ktor:ktor-network-tls-certificates:2.2.4")
	implementation("edu.stanford.nlp:stanford-corenlp:4.5.5")
	implementation("edu.stanford.nlp:stanford-corenlp:4.5.5:models")
	implementation("edu.stanford.nlp:stanford-corenlp:4.5.5:models-english")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("io.github.ludovicianul:pretty-logger:1.16")
	implementation("org.fusesource.jansi:jansi:2.4.0")
	implementation("com.jcabi:jcabi-log:0.24.1")
	implementation("org.tuxdude.logback.extensions:logback-colorizer:1.0.1")
	implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-2.0:1.32.0-alpha")
	implementation("io.ktor:ktor-server-metrics-micrometer:2.2.4")
	implementation("io.micrometer:micrometer-registry-prometheus:1.10.5")
	implementation("io.dropwizard.metrics:metrics-jmx:4.2.17")
	implementation("io.ktor:ktor-server-call-logging:2.2.4")
	implementation("edu.stanford.nlp:stanford-corenlp:4.5.5:models-english-kbp")
//	implementation("io.reflectoring:descriptive-logger:1.0")
	implementation("ch.qos.logback:logback-classic:1.4.12")
	implementation("org.apache.opennlp:opennlp-tools:2.3.1")
	implementation("org.apache.opennlp:opennlp-docs:2.3.1")
	implementation("org.apache.calcite.avatica:avatica-metrics-dropwizardmetrics:1.23.0")
}
