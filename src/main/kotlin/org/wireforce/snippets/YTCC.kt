package org.wireforce.snippets

import io.ktor.server.application.*
import org.wireforce.dto.KtorTaskValue
import org.wireforce.dto.KtorValue
import org.wireforce.modules.NLP
import org.wireforce.modules.Snippet
import org.wireforce.tensor
import kotlin.time.measureTimedValue

/**
 * YouTube Caption Classifier (YTCC) snippet implementation.
 * This class extends the {@link Snippet} class and processes YouTube captions using Tensor and NLP functionalities.
 *
 * @param id The identifier associated with the YouTube video.
 */
class YTCC(private val id: String) : Snippet<Map<String, Any?>>("YTCC") {

	/**
	 * Executes the YTCC snippet to process YouTube captions and perform summarization with NLP analysis.
	 *
	 * This function performs the following steps:
	 * 1. Retrieves captions for the specified YouTube video using a raw GET call to the Tensor Python daemon.
	 * 2. Concatenates the captions into a single raw text.
	 * 3. Calls the Tensor Python daemon to perform text summarization on the concatenated captions.
	 * 4. Analyzes the summarized text using NLP to extract intents and primitive emotions.
	 * 5. Calculates the average emotions of the summary and original captions.
	 *
	 * @param call The {@link ApplicationCall} associated with the execution context.
	 * @return A {@link KtorTaskValue} containing the results of the YTCC snippet, including Tensor and NLP analysis results.
	 */
	override suspend fun execute(call: ApplicationCall): KtorTaskValue<Map<String, Any?>> {

		// Step 1: Retrieve captions for the specified YouTube video
		val captions = tensor.rawGetCall<List<Map<String, String>>>("/app/youtube/cc/$id")

		// Step 2: Concatenate captions into a single raw text
		val rawCaptions = captions?.mapNotNull { it.getOrDefault("text", null) }?.joinToString(" ")

		// Step 3: Perform text summarization on the concatenated captions using Tensor
		val results = measureTimedValue {
			tensor.callTextualTaskInModel<List<Map<String, String>>>(
				"summarization",
				"Falconsai/text_summarization",
				rawCaptions ?: ""
			)
		}

		// Step 4: Analyze the summarized text using NLP to extract intents and primitive emotions
		val nlp = results.value?.output?.firstOrNull()?.let { resultRaw ->
			val input = resultRaw.getOrDefault("summary_text", null) ?: return@let null

			val emotionsOfSummary = NLP.exportPrimitiveEmotions(KtorValue(input))
			val emotionsOfOriginal = NLP.exportPrimitiveEmotions(KtorValue(rawCaptions!!))

			// Step 5: Calculate the average emotions of the summary and original captions
			mapOf(
				"intents" to NLP.exportIntents(KtorValue(input)),
				"emotionsOfSummary" to emotionsOfSummary,
				"emotionsOfOriginal" to emotionsOfOriginal,
				"avgEmotionsOfSummary" to emotionsOfSummary.value.values.average().coerceIn(-2.0, 2.0),
				"avgEmotionsOfOriginal" to emotionsOfOriginal.value.values.average().coerceIn(-2.0, 2.0),
			)
		}

		// Return the final results as a KtorTaskValue
		return KtorTaskValue(
			mapOf(
				"tensor" to results.value,
				"nlp" to nlp,
			),
			results.duration
		)
	}
}
