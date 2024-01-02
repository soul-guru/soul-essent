package org.wireforce.modules

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import org.wireforce.dto.KtorValue
import java.util.*
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

/**
 * Natural Language Processing (NLP) utility class for processing text and extracting information.
 *
 * This class provides functionality for exporting intents and primitive emotions from textual input
 * using Stanford CoreNLP library.
 */
open class NLP {
	companion object {
		// Configuration properties for Stanford CoreNLP
		protected val stanfordProps = Properties().apply {
			setProperty("annotators", "tokenize, ssplit, parse, sentiment")
		}

		// Lazy-initialized instance of StanfordCoreNLP for NLP processing
		protected val pipelineStanfordCoreNLP by lazy {
			StanfordCoreNLP(stanfordProps)
		}

		/**
		 * Export intents from the given textual input.
		 *
		 * @param value The KtorValue containing the text to be processed.
		 * @return TimedValue containing a list of extracted intents.
		 */
		fun exportIntents(value: KtorValue): TimedValue<List<String>> {
			val time = measureTimedValue {
				val intents = mutableListOf<String>()

				// Process the input text with Stanford CoreNLP
				val annotation = pipelineStanfordCoreNLP.process(value.value)

				// Extract intents based on the dependency parse
				for (sentence in annotation.get(CoreAnnotations.SentencesAnnotation::class.java)) {
					val sg = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation::class.java)
					for (edge in sg.edgeIterable()) {
						if (edge.relation.longName === "direct object") {
							val tverb = edge.governor.originalText()
							var dobj = edge.dependent.originalText()
							dobj = dobj.substring(0, 1).uppercase(Locale.getDefault()) + dobj.substring(1).lowercase(Locale.getDefault())
							intents.add(tverb + dobj)
						}
					}
				}

				intents.toList()
			}

			return time
		}

		/**
		 * Export primitive emotions from the given textual input.
		 *
		 * @param value The KtorValue containing the text to be processed.
		 * @return TimedValue containing a map of segmented text and corresponding primitive emotions.
		 */
		fun exportPrimitiveEmotions(value: KtorValue): TimedValue<MutableMap<String, Int>> {
			return measureTimedValue {
				val annotation = pipelineStanfordCoreNLP.process(value.value)
				val segments = mutableMapOf<String, Int>()

				// Extract sentiment and map it to primitive emotions
				for (sentence in annotation.get(CoreAnnotations.SentencesAnnotation::class.java)) {
					val tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree::class.java)
					val sentimentInt = RNNCoreAnnotations.getPredictedClass(tree)
					segments[sentence.toString()] = sentimentInt - 2
				}

				segments
			}
		}
	}
}