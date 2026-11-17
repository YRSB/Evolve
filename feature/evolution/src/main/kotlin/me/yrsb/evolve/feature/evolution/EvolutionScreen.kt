package me.yrsb.evolve.feature.evolution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.yrsb.evolve.core.engine.EvolveEngine
import me.yrsb.evolve.core.model.GameCommand
import me.yrsb.evolve.core.model.OrganelleType
import me.yrsb.evolve.core.model.ResourceType

@Composable
fun EvolutionScreen(
    engine: EvolveEngine,
    modifier: Modifier = Modifier
) {
    val resources by engine.resourceState.collectAsStateWithLifecycle()
    val evolution by engine.evolutionState.collectAsStateWithLifecycle()
    val gameState by engine.gameState.collectAsStateWithLifecycle()

    val rna = resources[ResourceType.RNA]
    val rnaAmount = rna?.amount ?: 0.0
    val rnaCap = rna?.capacity ?: 50.0
    val rnaRate = rna?.rate ?: 0.0

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Era Header
            Column {
                Text(
                    text = "Evolve: 原始演化时代",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Tick: #${gameState.tick} | 4Hz 纯函数步进仿真",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Resource Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "资源仪表板",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "RNA: ${"%.1f".format(rnaAmount)} / ${"%.0f".format(rnaCap)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "速率: +${"%.2f".format(rnaRate)}/秒",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val dna = resources[ResourceType.DNA]
                            Text(
                                text = "DNA: ${"%.1f".format(dna?.amount ?: 0.0)} / ${"%.0f".format(dna?.capacity ?: 20.0)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "点击数: ${evolution.manualClicks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Manual Synthesis Action
            Button(
                onClick = { engine.dispatch(GameCommand.ManualSynthesizeRna) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = rnaAmount < rnaCap
            ) {
                Text(
                    text = if (rnaAmount < rnaCap) "⚡ 手相触媒合成 RNA (+1)" else "⚠ RNA 存储已满",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Organelle Upgrades
            Text(
                text = "细胞器演化阶梯 (Organelles)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OrganelleType.entries.forEach { organelleType ->
                val organelleState = evolution.organelles[organelleType]
                val count = organelleState?.count ?: 0
                val cost = organelleState?.cost(organelleType) ?: organelleType.baseCostRna
                val canAfford = rnaAmount >= cost

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${organelleType.displayName} (数量: $count)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val desc = buildString {
                                if (organelleType.rnaRateBonus > 0) append("+${organelleType.rnaRateBonus}/s RNA ")
                                if (organelleType.rnaCapBonus > 0) append("+${organelleType.rnaCapBonus.toInt()} 上限 ")
                            }
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        OutlinedButton(
                            onClick = { engine.dispatch(GameCommand.UpgradeOrganelle(organelleType)) },
                            enabled = canAfford,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("购买 (${"%.1f".format(cost)} RNA)")
                        }
                    }
                }
            }
        }
    }
}
