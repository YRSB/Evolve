package me.yrsb.evolve.core.model

sealed interface GameCommand {
    data object ManualSynthesizeRna : GameCommand
    data class UpgradeOrganelle(val type: OrganelleType) : GameCommand
    data object ResetGame : GameCommand
}
