package org.wireforce.dto

import com.google.gson.annotations.SerializedName

/**
 * Represents the output structure of a Ktor model task, containing information about the model's execution.
 *
 * @property classificationTime The time taken for classification by the model.
 * @property initTime The initialization time of the model.
 * @property model The identifier or name of the model.
 * @property output The result or output produced by the model.
 * @property task The type or category of the model task.
 * @param T The type of the output produced by the model.
 */
data class KtorModelOutput<T>(
    @SerializedName("classification_time")
    val classificationTime: Double,
    @SerializedName("init_time")
    val initTime: Double,
    val model: String,
    val output: T,
    val task: String
)