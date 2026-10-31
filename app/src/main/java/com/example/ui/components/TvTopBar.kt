package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvNavTab
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentPink
import com.example.ui.theme.TvCardBg
import com.example.ui.theme.TvDeepSurface
import com.example.ui.theme.TvFocusHighlight
import com.example.ui.theme.TvPrimaryPurple
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.tvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvTopBar(
    currentTab: TvNavTab,
    detectedCount: Int,
    downloadsCount: Int,
    onTabSelected: (TvNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(10000)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TvDeepSurface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvPrimaryPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vídeo Drama",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvAccentPink)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TV BOX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Navigation Tabs with TV Focus states
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvTabItem(
                    title = "Modo Web Vercel",
                    icon = Icons.Default.Language,
                    isSelected = currentTab == TvNavTab.WEB,
                    badgeCount = detectedCount,
                    badgeColor = TvAccentCyan,
                    onClick = { onTabSelected(TvNavTab.WEB) }
                )

                TvTabItem(
                    title = "Extrator Nativo",
                    icon = Icons.Default.Search,
                    isSelected = currentTab == TvNavTab.EXTRACTOR,
                    onClick = { onTabSelected(TvNavTab.EXTRACTOR) }
                )

                TvTabItem(
                    title = "Meus Downloads",
                    icon = Icons.Default.Download,
                    isSelected = currentTab == TvNavTab.DOWNLOADS,
                    badgeCount = downloadsCount,
                    badgeColor = TvPrimaryPurple,
                    onClick = { onTabSelected(TvNavTab.DOWNLOADS) }
                )

                TvTabItem(
                    title = "Opções",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == TvNavTab.SETTINGS,
                    onClick = { onTabSelected(TvNavTab.SETTINGS) }
                )
            }

            // TV Clock & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentTime,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TvTextSecondary
                )
            }
        }
    }
}

@Composable
private fun TvTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int = 0,
    badgeColor: Color = TvAccentCyan,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (isSelected) TvPrimaryPurple else TvCardBg

    BadgedBox(
        badge = {
            if (badgeCount > 0) {
                Badge(
                    containerColor = badgeColor,
                    contentColor = Color.Black
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(shape)
                .background(bgColor)
                .tvFocusable(shape = shape, scaleFocused = 1.05f, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else TvTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = if (isSelected) Color.White else TvTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
