package org.wireforce.dto

import kotlin.time.Duration

/**
 * Represents the result of a Ktor task, including the calculated value and the time taken for the task.
 *
 * @property value The calculated value of the task, or `null` if the task did not produce a result.
 * @property calculateTime The duration of time taken to perform the task.
 * @param T The type of the calculated value.
 */
data class KtorTaskValue<T>(
	val value: T?,
	val calculateTime: Duration
)
