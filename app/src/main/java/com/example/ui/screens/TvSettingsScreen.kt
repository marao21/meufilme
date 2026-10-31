package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentPink
import com.example.ui.theme.TvCardBg
import com.example.ui.theme.TvCardHover
import com.example.ui.theme.TvDeepSurface
import com.example.ui.theme.TvFocusHighlight
import com.example.ui.theme.TvPrimaryPurple
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.tvFocusable

@Composable
fun TvSettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onClearDetectedHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Surface(
                color = TvDeepSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Configurações e Preferências TV",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ajuste os filtros de captura de streams e as opções do aplicativo para TV Box.",
                        fontSize = 13.sp,
                        color = TvTextSecondary
                    )
                }
            }
        }

        // Duration Filter
        item {
            val cardShape = RoundedCornerShape(14.dp)
            Surface(
                color = TvCardBg,
                shape = cardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Duração Mínima do Vídeo (Descartar Anúncios)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ignora trechos ou comerciais curtos. Vídeos abaixo deste tempo serão desconsiderados na detecção automática.",
                        fontSize = 12.sp,
                        color = TvTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(0 to "Sem limite", 5 to "5 min", 15 to "15 min (Padrão)", 30 to "30 min").forEach { (mins, label) ->
                            val isSelected = settings.minDurationMinutes == mins
                            val pillShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(if (isSelected) TvPrimaryPurple else TvCardHover)
                                    .tvFocusable(shape = pillShape) {
                                        onUpdateSettings(settings.copy(minDurationMinutes = mins))
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TvTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggles: Unknown duration & Protected Mode
        item {
            val cardShape = RoundedCornerShape(14.dp)
            Surface(
                color = TvCardBg,
                shape = cardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Unknown duration toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mostrar vídeos de duração desconhecida",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvTextPrimary
                            )
                            Text(
                                text = "Útil para streams HLS ao vivo ou servidores que não informam o tempo total no cabeçalho.",
                                fontSize = 12.sp,
                                color = TvTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.showUnknownDuration,
                            onCheckedChange = { onUpdateSettings(settings.copy(showUnknownDuration = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TvPrimaryPurple
                            )
                        )
                    }

                    // Protected Mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Modo Protegido (Bloqueador de Redirecionamento)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TvTextPrimary
                                )
                            }
                            Text(
                                text = "Impede que pop-ups abusivos de sites de doramas troquem sua tela na TV Box.",
                                fontSize = 12.sp,
                                color = TvTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.protectedMode,
                            onCheckedChange = { onUpdateSettings(settings.copy(protectedMode = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TvPrimaryPurple
                            )
                        )
                    }
                }
            }
        }

        // Management Actions: Clear history & Reset Home
        item {
            val cardShape = RoundedCornerShape(14.dp)
            Surface(
                color = TvCardBg,
                shape = cardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ações do Sistema",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val btnShape = RoundedCornerShape(8.dp)

                        // Clear detected history
                        Box(
                            modifier = Modifier
                                .clip(btnShape)
                                .background(TvCardHover)
                                .tvFocusable(shape = btnShape) {
                                    onClearDetectedHistory()
                                    Toast.makeText(context, "Histórico de vídeos detectados limpo!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = TvAccentPink, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Limpar Vídeos Detectados", color = TvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reset to official Vercel home
                        Box(
                            modifier = Modifier
                                .clip(btnShape)
                                .background(TvCardHover)
                                .tvFocusable(shape = btnShape) {
                                    onUpdateSettings(settings.copy(homeUrl = "https://baixar-video-drama-web-uy33.vercel.app/"))
                                    Toast.makeText(context, "URL inicial restaurada para Vercel!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar Início Vercel", color = TvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // About Box
        item {
            val cardShape = RoundedCornerShape(14.dp)
            Surface(
                color = TvDeepSurface,
                shape = cardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Baixar Vídeo Drama TV — Versão 1.0", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                        Text(
                            "Construído especialmente para TV Box Android e Smart TVs. Conectado ao sistema https://baixar-video-drama-web-uy33.vercel.app/ com suporte a controle remoto D-Pad, sniffing nativo de streams e reprodutor com download.",
                            fontSize = 12.sp,
                            color = TvTextSecondary
                        )
                    }
                }
            }
        }
    }
}
