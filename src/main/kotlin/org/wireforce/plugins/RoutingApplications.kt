package org.wireforce.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.wireforce.dto.KtorTaskValue
import org.wireforce.interfaces.SnippetRouterAdapter
import org.wireforce.snippets.Prism1
import org.wireforce.snippets.YTCC

/**
 * Configures routing for the Ktor application, defining endpoints for handling snippets.
 *
 * This function sets up routes for retrieving a list of available snippets and executing a specific snippet.
 * Snippet information is stored in the 'snippets' map, where each snippet is associated with a unique identifier ('snippedId').
 *
 * @param Application The Ktor application to which routing configurations are applied.
 */
fun Application.configureRoutingApplications() {
	// Define snippets with associated parameters and execution logic
	val snippets = mapOf(
		"ytcc" to object : SnippetRouterAdapter<Map<String, Any?>> {
			override val description: String
				get() = "YTCC - a system for translating YouTube videos into its text" +
						"description and basic classification"

			override suspend fun call(call: ApplicationCall): KtorTaskValue<Map<String, Any?>> {
				val body = call.receive<Map<String, Any>>()
				val videoId = body["videoId"].toString()

				// Execute YTCC snippet with the provided 'id' parameter
				return YTCC(videoId).execute(call)
			}
		},

		"prism1" to object : SnippetRouterAdapter<Map<String, Any?>> {
			override val description: String
				get() = "Prism1 is a snippet for splitting a user message into the necessary characteristics for its complex classification." +
						"The name itself - Prism1 itself declares the possibility of the existence of other presets of the Prism version," +
						"such as Prism2, Prism3, etc. They are independent and can even be created by different authors"

			override suspend fun call(call: ApplicationCall): KtorTaskValue<Map<String, Any?>> {
				val body = call.receive<Map<String, String>>()
				val message = body.getOrDefault("message", "")

				return Prism1(message).execute(call)
			}
		}
	)

	// Configure routing for the application
	routing {
		// Endpoint for retrieving a list of available snippets
		get("/snippets") {
			call.respond(
				HttpStatusCode.OK,
				KtorTaskValue(
					snippets.keys,
					kotlin.time.Duration.ZERO
				)
			)
		}

		// Endpoint for executing a specific snippet
		post("/snippet/{snippedId}") {
			// Extract 'snippedId' from the path parameters
			val appId = requireNotNull(this.call.parameters["snippedId"]) {
				"snippedId not passed"
			}

			// Check if the specified snippet exists
			if (!snippets.containsKey(appId)) {
				return@post call.respond(
					HttpStatusCode.NotFound,
					KtorTaskValue("Snippet not found", kotlin.time.Duration.ZERO)
				)
			}

			// Invoke the execution logic of the specified snippet
			val applicationExecution = snippets[appId]?.call(call)

			// Respond with the result of the snippet execution
			call.respond(
				HttpStatusCode.OK,
				applicationExecution ?: KtorTaskValue<Any>(null, kotlin.time.Duration.ZERO)
			)
		}
	}
}