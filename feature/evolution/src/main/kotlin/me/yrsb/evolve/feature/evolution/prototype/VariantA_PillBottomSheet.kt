package me.yrsb.evolve.feature.evolution.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * 变体 A：顶部紧凑药丸条 (Tonal Pill Strip) + 单击唤起 ModalBottomSheet
 * 强调拇指友好的移动端交互：顶部高密度横滑药丸胶囊，点击查看详细公式，底部固定合成操作区。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantA_PillBottomSheet(
    engine: EvolveEngine,
    gameState: GameState,
    resources: Map<ResourceType, ResourcePool>,
    evolution: EvolutionState,
    modifier: Modifier = Modifier
) {
    val rna = resources[ResourceType.RNA] ?: ResourcePool()
    val dna = resources[ResourceType.DNA] ?: ResourcePool()

    var selectedResourceForSheet by remember { mutableStateOf<ResourceType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cytoplasmCount = evolution.organelles[OrganelleType.CYTOPLASM]?.count ?: 0
    val mitochondriaCount = evolution.organelles[OrganelleType.MITOCHONDRIA]?.count ?: 0
    val membraneCount = evolution.organelles[OrganelleType.MEMBRANE]?.count ?: 0

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部粘性紧凑胶囊条 (Sticky Tonal Pill Strip)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // RNA Pill
                        ResourcePill(
                            name = "RNA",
                            amount = rna.amount,
                            capacity = rna.capacity,
                            rate = rna.rate,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { selectedResourceForSheet = ResourceType.RNA }
                        )

                        // DNA Pill
                        ResourcePill(
                            name = "DNA",
                            amount = dna.amount,
                            capacity = dna.capacity,
                            rate = dna.rate,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { selectedResourceForSheet = ResourceType.DNA }
                        )

                        // Locked Resource Stub
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔒 氨基酸 (原初汤解锁)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "💡 轻触资源胶囊展开属性公式与生产源明细 (ModalBottomSheet)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. 主操作与内容滚动区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 140.dp), // 为底部操作区与悬浮切换器留出视口余量
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "细胞器阶梯 (Organelles)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tick #${gameState.tick}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Organelle List Cards
                OrganelleType.entries.forEach { type ->
                    val state = evolution.organelles[type]
                    val count = state?.count ?: 0
                    val cost = state?.cost(type) ?: type.baseCostRna
                    val canAfford = rna.amount >= cost

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                                    text = "${type.displayName}  ×$count",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val effect = buildString {
                                    if (type.rnaRateBonus > 0) append("+${type.rnaRateBonus}/s RNA  ")
                                    if (type.rnaCapBonus > 0) append("+${type.rnaCapBonus.toInt()} 上限")
                                }
                                Text(
                                    text = effect,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            OutlinedButton(
                                onClick = { engine.dispatch(GameCommand.UpgradeOrganelle(type)) },
                                enabled = canAfford,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("合成 (${cost.toInt()} RNA)")
                            }
                        }
                    }
                }
            }
        }

        // 3. 底部固定高频主操作区 (Fixed Bottom Action Bar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 72.dp), // 位于悬浮 Switcher 上方
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { engine.dispatch(GameCommand.ManualSynthesizeRna) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = rna.amount < rna.capacity,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (rna.amount < rna.capacity) "⚡ 手相催化 RNA (+1.0)" else "⚠ 达到存储上限",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(52.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "累计触击", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${evolution.manualClicks}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 4. ModalBottomSheet 属性详情抽屉
        if (selectedResourceForSheet != null) {
            val resourceType = selectedResourceForSheet!!
            val pool = resources[resourceType] ?: ResourcePool()

            ModalBottomSheet(
                onDismissRequest = { selectedResourceForSheet = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${resourceType.displayName} 属性全量拆解",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "实时游戏核心仿真状态",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Progress bar
                    val progress = (pool.amount / pool.capacity.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "当前储量: ${"%.1f".format(pool.amount)} / ${"%.0f".format(pool.capacity)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Net rate breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📊 产出速率源分解",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("基础自然合成", style = MaterialTheme.typography.bodyMedium)
                                Text("+0.00 / 秒", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (resourceType == ResourceType.RNA) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("细胞质 (${cytoplasmCount}个 × 0.25)", style = MaterialTheme.typography.bodyMedium)
                                    Text("+${"%.2f".format(cytoplasmCount * 0.25)} / 秒", color = MaterialTheme.colorScheme.secondary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("线粒体 (${mitochondriaCount}个 × 1.00)", style = MaterialTheme.typography.bodyMedium)
                                    Text("+${"%.2f".format(mitochondriaCount * 1.00)} / 秒", color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("净速率合计", fontWeight = FontWeight.Bold)
                                Text("+${"%.2f".format(pool.rate)} / 秒", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Capacity & ETA breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🛡 存储上限与预估",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("初始基准上限", style = MaterialTheme.typography.bodyMedium)
                                Text("50.0", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (resourceType == ResourceType.RNA) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("细胞膜扩容 (${membraneCount}个 × 50)", style = MaterialTheme.typography.bodyMedium)
                                    Text("+${membraneCount * 50}", color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("达到满仓预估", fontWeight = FontWeight.Bold)
                                val etaText = when {
                                    pool.amount >= pool.capacity -> "已达上限"
                                    pool.rate > 0 -> "${((pool.capacity - pool.amount) / pool.rate).toInt()} 秒后"
                                    else -> "无自动累积"
                                }
                                Text(etaText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Button(
                        onClick = { selectedResourceForSheet = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("关闭详情")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcePill(
    name: String,
    amount: Double,
    capacity: Double,
    rate: Double,
    accentColor: Color,
    onClick: () -> Unit
) {
    val progress = (amount / capacity.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .height(44.dp)
            .clickable(onClick = onClick)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Column(verticalArrangement = Arrangement.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "${"%.1f".format(amount)}/${capacity.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "+${"%.2f".format(rate)}/s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
