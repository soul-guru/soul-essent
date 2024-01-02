package org.wireforce.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.wireforce.dto.KtorTaskValue
import org.wireforce.dto.KtorValue
import org.wireforce.modules.NLP
import org.wireforce.tensor
import kotlin.time.measureTimedValue

/**
 * Configures routing for the Ktor application, defining endpoints for various NLP and Tensor tasks.
 * This function sets up routes for intent extraction, primitive emotion analysis, and general textual tasks.
 *
 * Endpoints:
 * - POST "/nlp/intent": Extracts intents from the provided text using NLP.
 * - POST "/nlp/emotion": Analyzes primitive emotions in the provided text using NLP.
 * - POST "/task/{task}": Performs a specified textual task using the Tensor Python daemon.
 * - GET "/": Responds with a simple "Hello World!" message.
 *
 * @receiver The Ktor application to which routing is being configured.
 */
fun Application.configureRouting() {
	routing {
		route("/nlp") {
			// Endpoint: POST "/nlp/intent"
			post("/intent") {
				val value = call.receive<KtorValue>()
				val out = NLP.exportIntents(value)

				call.respond(
					HttpStatusCode.OK,
					KtorTaskValue(
						out.value,
						out.duration
					)
				)
			}

			// Endpoint: POST "/nlp/emotion"
			post("/emotion") {
				val value = call.receive<KtorValue>()
				val out = NLP.exportPrimitiveEmotions(value)

				call.respond(
					HttpStatusCode.OK,
					KtorTaskValue(
						out.value,
						out.duration
					)
				)
			}
		}

		// Endpoint: POST "/task/{task}"
		post("/task/{task}") {
			val value = call.receive<KtorValue>()
			val model = requireNotNull(call.request.queryParameters["model"]) {
				// Handle missing query parameter error
				call.respond(HttpStatusCode.BadRequest, "Missing 'model' query parameter.")
			}

			val task = requireNotNull(call.parameters["task"]) {
				// Handle missing path parameter error
				call.respond(HttpStatusCode.BadRequest, "Missing 'task' path parameter.")
			}

			val results = measureTimedValue {
				tensor.callTextualTaskInModel<String>(
					task,
					model,
					value.value
				)
			}

			call.respond(
				HttpStatusCode.OK,
				KtorTaskValue(
					results.value,
					results.duration
				)
			)
		}

		// Endpoint: GET "/"
		get("/") {
			call.respondText("Hello World!")
		}
	}
}
