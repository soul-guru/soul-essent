package org.wireforce.interfaces

import io.ktor.server.application.*
import org.wireforce.dto.KtorTaskValue

interface SnippetRouterAdapter <T> {
	val description: String
	suspend fun call(call: ApplicationCall): KtorTaskValue<T>
}