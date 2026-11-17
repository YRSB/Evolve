package me.yrsb.evolve.feature.evolution.prototype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PrototypeVariant(val id: String, val title: String, val summary: String) {
    VARIANT_A(
        id = "A",
        title = "Variant A: 紧凑胶囊条 + 底部半屏抽屉",
        summary = "顶部药丸条 · 单击唤起 ModalBottomSheet · 底部固定快速操作栏"
    ),
    VARIANT_B(
        id = "B",
        title = "Variant B: 网格仪表 HUD + 锚定浮窗",
        summary = "2x2 网格平铺 · 长按唤起 DropdownMenu 浮动气泡 · 分段选项卡"
    ),
    VARIANT_C(
        id = "C",
        title = "Variant C: 折叠伸缩顶栏 + 内联卡片",
        summary = "动态折叠压缩顶栏 · 单击内联展开明细 · 沉浸单流快操"
    )
}

/**
 * 原型悬浮切换栏（遵循 prototype 规范）：
 * 固定于屏幕底部中央，允许用户在不同 UI 架构变体之间即时无缝切换对比。
 */
@Composable
fun PrototypeSwitcher(
    current: PrototypeVariant,
    onSelectVariant: (PrototypeVariant) -> Unit,
    modifier: Modifier = Modifier
) {
    val variants = PrototypeVariant.entries
    val currentIndex = variants.indexOf(current)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val prevIndex = if (currentIndex - 1 < 0) variants.size - 1 else currentIndex - 1
                    onSelectVariant(variants[prevIndex])
                }
            ) {
                Text(
                    text = "◀",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = current.summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }

            IconButton(
                onClick = {
                    val nextIndex = (currentIndex + 1) % variants.size
                    onSelectVariant(variants[nextIndex])
                }
            ) {
                Text(
                    text = "▶",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
