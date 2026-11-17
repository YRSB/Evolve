package me.yrsb.evolve.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class OrganelleType(
    val displayName: String,
    val baseCostRna: Double,
    val rnaRateBonus: Double,
    val rnaCapBonus: Double
) {
    MEMBRANE("细胞膜", 10.0, 0.0, 50.0),
    CYTOPLASM("细胞质", 15.0, 0.25, 0.0),
    MITOCHONDRIA("线粒体", 50.0, 1.0, 0.0)
}

@Serializable
data class OrganelleState(
    val count: Int = 0
) {
    fun cost(type: OrganelleType): Double {
        return type.baseCostRna * Math.pow(1.15, count.toDouble())
    }
}
