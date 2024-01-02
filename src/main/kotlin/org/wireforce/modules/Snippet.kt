package org.wireforce.modules

import io.ktor.server.application.*
import org.wireforce.dto.KtorTaskValue
import org.wireforce.tensor

/**
 * A base class for implementing snippets in an application, providing a common structure for snippet execution.
 *
 * @param O The type parameter representing the output type of the snippet.
 * @property snippetName The name associated with the snippet.
 */
open class Snippet<O>(protected val snippetName: String) {
	/**
	 * Lazy-initialized tensor transforms used by the snippet.
	 */
	protected val tensorTransforms by lazy {
		tensor // Assuming tensor is defined elsewhere
	}

	/**
	 * Executes the snippet logic asynchronously and returns the result as a KtorTaskValue.
	 *
	 * @param call The Ktor ApplicationCall object providing information about the HTTP request.
	 * @return A KtorTaskValue containing the result of the snippet execution.
	 * @throws IllegalCallerException if the execute function is called on the base Snippet class (override required).
	 */
	open suspend fun execute(call: ApplicationCall): KtorTaskValue<O> {
		throw IllegalCallerException()
	}
}