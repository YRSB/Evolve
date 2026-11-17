package me.yrsb.evolve.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ResourceType(val displayName: String) {
    RNA("RNA"),
    DNA("DNA")
}

@Serializable
data class ResourcePool(
    val amount: Double = 0.0,
    val capacity: Double = 50.0,
    val rate: Double = 0.0
)
