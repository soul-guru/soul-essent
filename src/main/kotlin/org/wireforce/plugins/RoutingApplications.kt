package org.wireforce.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.wireforce.dto.KtorTaskValue
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
		"ytcc" to object {
			val parameters = listOf("id")

			val call = { call: ApplicationCall ->
				suspend {
					// Execute YTCC snippet with the provided 'id' parameter
					YTCC(call.parameters["id"]!!).execute(call)
				}
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
			val applicationExecution = snippets[appId]?.call?.invoke(call)?.invoke()

			// Respond with the result of the snippet execution
			call.respond(
				HttpStatusCode.OK,
				applicationExecution ?: KtorTaskValue<Any>(null, kotlin.time.Duration.ZERO)
			)
		}
	}
}