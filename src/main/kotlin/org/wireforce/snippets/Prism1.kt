package org.wireforce.snippets

import io.ktor.server.application.*
import org.wireforce.dto.KtorTaskValue
import org.wireforce.dto.KtorValue
import org.wireforce.modules.NLP
import org.wireforce.modules.Snippet
import kotlin.time.measureTimedValue

class Prism1(private val message: String) :  Snippet<Map<String, Any?>>("prism1") {
	override suspend fun execute(call: ApplicationCall): KtorTaskValue<Map<String, Any?>> {
		val result = measureTimedValue {
			val intents = NLP.exportIntents(KtorValue(message))
			val emotions = NLP.exportPrimitiveEmotions(KtorValue(message))

			mapOf(
				"intents" to intents,
				"emotions" to emotions
			)
		}

		return KtorTaskValue(
			result.value,
			result.duration
		)
	}
}