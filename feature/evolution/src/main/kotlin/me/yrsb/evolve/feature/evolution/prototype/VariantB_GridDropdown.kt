package me.yrsb.evolve.feature.evolution.prototype

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * 变体 B：2x2 网格平铺仪表 HUD (Grid Matrix HUD) + 长按/触控锚定气泡浮窗 (DropdownMenu)
 * 强调高信息密度瞬时一览：网格仪表展示多维指标，长按锚定唤起紧凑气泡菜单，避免大面积遮挡。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VariantB_GridDropdown(
    engine: EvolveEngine,
    gameState: GameState,
    resources: Map<ResourceType, ResourcePool>,
    evolution: EvolutionState,
    modifier: Modifier = Modifier
) {
    val rna = resources[ResourceType.RNA] ?: ResourcePool()
    val dna = resources[ResourceType.DNA] ?: ResourcePool()
    val rnaProg = (rna.amount / rna.capacity.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val dnaProg = (dna.amount / dna.capacity.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    var showRnaMenu by remember { mutableStateOf(false) }
    var showDnaMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val cytoplasmCount = evolution.organelles[OrganelleType.CYTOPLASM]?.count ?: 0
    val mitochondriaCount = evolution.organelles[OrganelleType.MITOCHONDRIA]?.count ?: 0
    val membraneCount = evolution.organelles[OrganelleType.MEMBRANE]?.count ?: 0

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部粘性 2x2 网格仪表盘 (Sticky Grid Matrix HUD)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 第一行：RNA 与 DNA 网格卡片
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // RNA HUD Card with Long-press Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { showRnaMenu = true },
                                        onLongClick = { showRnaMenu = true }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RNA",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "+${"%.2f".format(rna.rate)}/s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = "${"%.1f".format(rna.amount)} / ${rna.capacity.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { rnaProg },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }

                            // 锚定于该卡片的 DropdownMenu 气泡
                            DropdownMenu(
                                expanded = showRnaMenu,
                                onDismissRequest = { showRnaMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("📊 RNA 储量: ${"%.1f".format(rna.amount)} / ${rna.capacity.toInt()} (${(rnaProg * 100).toInt()}%)", fontWeight = FontWeight.Bold)
                                    },
                                    onClick = { showRnaMenu = false }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("⚡ 产出速率: +${"%.2f".format(rna.rate)} / 秒") },
                                    onClick = { showRnaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔬 细胞质贡献: +${"%.2f".format(cytoplasmCount * 0.25)}/s (${cytoplasmCount}个)") },
                                    onClick = { showRnaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔋 线粒体贡献: +${"%.2f".format(mitochondriaCount * 1.00)}/s (${mitochondriaCount}个)") },
                                    onClick = { showRnaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("🛡 细胞膜容量: +${membraneCount * 50} (${membraneCount}个)") },
                                    onClick = { showRnaMenu = false }
                                )
                            }
                        }

                        // DNA HUD Card with Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { showDnaMenu = true },
                                        onLongClick = { showDnaMenu = true }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DNA",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            text = "触击 ${evolution.manualClicks}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Text(
                                        text = "${"%.1f".format(dna.amount)} / ${dna.capacity.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { dnaProg },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDnaMenu,
                                onDismissRequest = { showDnaMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🧬 DNA: ${"%.1f".format(dna.amount)} / ${dna.capacity.toInt()}", fontWeight = FontWeight.Bold) },
                                    onClick = { showDnaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("👆 累计手相手动点击: ${evolution.manualClicks} 次") },
                                    onClick = { showDnaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("🚀 文明觉醒阈值: 20 DNA (Sentience)") },
                                    onClick = { showDnaMenu = false }
                                )
                            }
                        }
                    }

                    // 第二行：状态微标与提示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 长按或轻点仪表格唤起 DropdownMenu 锚定浮动卡片",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tick #${gameState.tick} · 4Hz",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 分段选项卡
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("细胞器演化") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("原初汤环境") }
                    )
                }
            }

            // 2. 页面主体内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
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
                                        text = "${type.displayName} (已构筑: $count)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val desc = buildString {
                                        if (type.rnaRateBonus > 0) append("+${type.rnaRateBonus}/s ")
                                        if (type.rnaCapBonus > 0) append("+${type.rnaCapBonus.toInt()} Cap")
                                    }
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Button(
                                    onClick = { engine.dispatch(GameCommand.UpgradeOrganelle(type)) },
                                    enabled = canAfford,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("构筑 ${cost.toInt()} RNA")
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("原初汤地质环境 (Primordial Soup)", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "地壳温度: 42°C | 氨氮浓度: 14.8% | 紫外线辐射: 极强\n正在孕育核苷酸分子与早期原生质团。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // 3. 底部主操作条
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 72.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { engine.dispatch(GameCommand.ManualSynthesizeRna) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = rna.amount < rna.capacity
                ) {
                    Text(
                        text = "⚡ 触媒合成 RNA (+1.0)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
