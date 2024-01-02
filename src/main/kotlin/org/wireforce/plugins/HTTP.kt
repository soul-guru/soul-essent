package org.wireforce.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v2_0.server.KtorServerTracing
import org.slf4j.event.Level
import java.io.File
import java.text.DateFormat
import java.time.Duration

/**
 * Configures HTTP features for the Ktor application, including content negotiation, logging, and metrics.
 * This function installs various Ktor features such as ContentNegotiation, KtorServerTracing, CallLogging,
 * and MicrometerMetrics to enhance the HTTP capabilities of the application.
 *
 * Features Installed:
 * - ContentNegotiation: Configures Gson as the default serializer for content negotiation.
 * - KtorServerTracing: Sets up OpenTelemetry for server-side tracing (currently set to a no-op configuration).
 * - CallLogging: Configures request/response logging with the specified logging level.
 * - MicrometerMetrics: Enables metrics collection using Micrometer, including distribution statistics and various meter binders.
 *
 * Additional Features (commented out):
 * - DropwizardMetrics: Alternative metrics collection using DropwizardMetrics.
 * - JmxReporter: JMX reporting for metrics visualization.
 *
 * @receiver The Ktor application to which HTTP features are being configured.
 */
fun Application.configureHttp() {
	install(ContentNegotiation) {
		gson {
			setDateFormat(DateFormat.LONG)
			setPrettyPrinting()
		}
	}

	install(KtorServerTracing) {
		setOpenTelemetry(OpenTelemetry.noop())
	}

	// DropwizardMetrics installation (commented out)
	// install(DropwizardMetrics)
	// install(DropwizardMetrics) {
	//     baseName = "my.prefix"
	// }

	install(CallLogging) {
		level = Level.INFO
	}

	install(MicrometerMetrics) {
		distributionStatisticConfig = DistributionStatisticConfig.Builder()
			.percentilesHistogram(true)
			.maximumExpectedValue(Duration.ofSeconds(20).toNanos().toDouble())
			.serviceLevelObjectives(
				Duration.ofMillis(100).toNanos().toDouble(),
				Duration.ofMillis(500).toNanos().toDouble()
			)
			.build()

		// JmxReporter installation (commented out)
		// JmxReporter.forRegistry()
		//     .convertRatesTo(TimeUnit.SECONDS)
		//     .convertDurationsTo(TimeUnit.MILLISECONDS)
		//     .build()
		//     .start()

		meterBinders = listOf(
			JvmMemoryMetrics(),
			ClassLoaderMetrics(),
			JvmGcMetrics(),
			ProcessorMetrics(),
			UptimeMetrics(),
			DiskSpaceMetrics(File("./"))
		)
	}
}
