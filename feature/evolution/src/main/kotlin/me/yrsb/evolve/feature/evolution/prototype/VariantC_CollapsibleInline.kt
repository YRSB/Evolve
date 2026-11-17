package me.yrsb.evolve.feature.evolution.prototype

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.yrsb.evolve.core.engine.EvolveEngine
import me.yrsb.evolve.core.model.EvolutionState
import me.yrsb.evolve.core.model.GameCommand
import me.yrsb.evolve.core.model.GameState
import me.yrsb.evolve.core.model.OrganelleType
import me.yrsb.evolve.core.model.ResourcePool
import me.yrsb.evolve.core.model.ResourceType

/**
 * 变体 C：折叠伸缩顶栏 (Collapsible Sticky Header) + 内联非模态卡片 (Inline Breakdown Card)
 * 强调纯单手沉浸流：无需模态弹窗打断视线，点击仪表内联平滑展开公式明细，顶栏可压缩为 36dp 跑马灯状态条。
 */
@Composable
fun VariantC_CollapsibleInline(
    engine: EvolveEngine,
    gameState: GameState,
    resources: Map<ResourceType, ResourcePool>,
    evolution: EvolutionState,
    modifier: Modifier = Modifier
) {
    val rna = resources[ResourceType.RNA] ?: ResourcePool()
    val dna = resources[ResourceType.DNA] ?: ResourcePool()

    var isCompactMode by remember { mutableStateOf(false) }
    var isDetailExpanded by remember { mutableStateOf(false) }

    val cytoplasmCount = evolution.organelles[OrganelleType.CYTOPLASM]?.count ?: 0
    val mitochondriaCount = evolution.organelles[OrganelleType.MITOCHONDRIA]?.count ?: 0
    val membraneCount = evolution.organelles[OrganelleType.MEMBRANE]?.count ?: 0

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 可伸缩折叠粘性顶栏 (Collapsible Sticky Header)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCompactMode) {
                        // 紧凑微型条模式 (36dp 高度极简状态流)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clickable { isDetailExpanded = !isDetailExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RNA ${"%.1f".format(rna.amount)}/${rna.capacity.toInt()} (+${"%.2f".format(rna.rate)}/s)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "DNA ${dna.amount.toInt()}/${dna.capacity.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = if (isDetailExpanded) "收起 ▲" else "明细 ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { isCompactMode = false },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("展开", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    } else {
                        // 标准展开顶栏模式
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Evolve: 原始演化时代",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tick #${gameState.tick} · 4Hz 仿真步进",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { isDetailExpanded = !isDetailExpanded },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isDetailExpanded) "收起公式 ▲" else "查看公式 ▼")
                                }
                                IconButton(onClick = { isCompactMode = true }) {
                                    Text("收缩", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        // 资源快速指标卡
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailExpanded = !isDetailExpanded },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("RNA 储量", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${"%.1f".format(rna.amount)} / ${rna.capacity.toInt()}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "+${"%.2f".format(rna.rate)} / 秒",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("DNA 觉醒", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${dna.amount.toInt()} / ${dna.capacity.toInt()}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "手动触击: ${evolution.manualClicks}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // 内联无模态公式展开卡片 (Inline Animated Detail Card)
                    AnimatedVisibility(
                        visible = isDetailExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔬 内联属性解析 (零模态打断)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• 产出速率: 细胞质 (+${"%.2f".format(cytoplasmCount * 0.25)}/s) + 线粒体 (+${"%.2f".format(mitochondriaCount * 1.00)}/s) = +${"%.2f".format(rna.rate)}/s",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "• 存储上限: 初始 50 + 细胞膜 (+${membraneCount * 50}) = ${rna.capacity.toInt()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val eta = if (rna.amount >= rna.capacity) "已满" else if (rna.rate > 0) "${((rna.capacity - rna.amount) / rna.rate).toInt()}s" else "暂停"
                                Text(
                                    text = "• 满仓倒计时: $eta",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. 连续单流滚动内容 (含顶置触媒按钮，无底部遮挡)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 90.dp), // 留出悬浮 switcher 的边距
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 顶置巨型合成按钮（单流操作）
                Button(
                    onClick = { engine.dispatch(GameCommand.ManualSynthesizeRna) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = rna.amount < rna.capacity
                ) {
                    Text(
                        text = if (rna.amount < rna.capacity) "⚡ 手相催化合成 RNA (+1.0)" else "⚠ 达到存储上限",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "细胞器演化进阶",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OrganelleType.entries.forEach { type ->
                    val state = evolution.organelles[type]
                    val count = state?.count ?: 0
                    val cost = state?.cost(type) ?: type.baseCostRna
                    val canAfford = rna.amount >= cost

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    text = "${type.displayName}  (等级: $count)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val desc = buildString {
                                    if (type.rnaRateBonus > 0) append("+${type.rnaRateBonus}/s RNA  ")
                                    if (type.rnaCapBonus > 0) append("+${type.rnaCapBonus.toInt()} 容量")
                                }
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            OutlinedButton(
                                onClick = { engine.dispatch(GameCommand.UpgradeOrganelle(type)) },
                                enabled = canAfford,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("${cost.toInt()} RNA")
                            }
                        }
                    }
                }
            }
        }
    }
}
