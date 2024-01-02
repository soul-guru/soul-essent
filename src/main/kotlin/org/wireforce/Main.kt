package org.wireforce

import kotlinx.coroutines.runBlocking

fun main() {
	TensorTransforms().apply {
		runBlocking {
//			daemon()

			val output = callTextualTaskInModel<String>(
				"text-classification",
				"mohameddhiab/humor-no-humor",
				"I am a banana"
			)

			println(output)
		}
	}
}