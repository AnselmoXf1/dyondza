package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel

data class Badge(
    val title: String,
    val description: String,
    val levelRequired: Int,
    val emoji: String,
    val color: Color
)

data class ThemeColorOption(
    val name: String,
    val levelRequired: Int,
    val primaryColor: Color,
    val backgroundColor: Color
)

@Composable
fun ProfileScreen(viewModel: FocusViewModel, onNavigateToSettings: () -> Unit) {
    val history by viewModel.studyHistory.collectAsState()
    val studentName by viewModel.studentName.collectAsState()

    val totalHours by viewModel.totalStudyHours.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val progressToNextLevel by viewModel.progressToNextLevel.collectAsState()

    // Cálculo da economia com base no histórico real: 1 min = 10 XP
    // Mais as missões completadas para inflar o XP de forma divertida e gamificada!
    val totalMinutes = history.sumOf { it.durationSeconds } / 60
    val xpFromStudy = totalMinutes * 10

    // Vamos simular XP extra vindo de missões concluídas no histórico
    val totalXp = xpFromStudy + (history.size * 30) // +30 XP por sessão concluída para bônus de consistência

    val userTitle = when {
        currentLevel >= 40 -> "Sábio da Montanha"
        currentLevel >= 30 -> "Polímata Supremo"
        currentLevel >= 20 -> "Estudante Destemido"
        currentLevel >= 10 -> "Pesquisador Dedicado"
        else -> "Aprendiz Zen"
    }

    val badges = listOf(
        Badge("Escudo Dyondza", "Desbloqueado ao ingressar na academia", 1, "🛡️", PrimaryIndigo),
        Badge("Foco do Alvorecer", "Estude de madrugada (antes das 7h)", 5, "🌅", SecondaryAmber),
        Badge("Maratonista", "Conclua 3 sessões em um único dia", 10, "🏃", TertiaryMint),
        Badge("Sábio Digital", "Mantenha o bloqueador ativo por 5h", 20, "🧠", Color(0xFF673AB7)),
        Badge("Inabalável", "Consiga 1.000 XP acadêmicos", 30, "💎", Color(0xFFE91E63)),
        Badge("Mestre da Leitura", "Atinja o Nível 50 supremo", 50, "👑", Color(0xFFFFD700))
    )

    val themes = listOf(
        ThemeColorOption("Classic Zen Sage", 1, PrimaryIndigo, DarkBg),
        ThemeColorOption("Midnight Space", 5, Color(0xFF3F51B5), Color(0xFF121212)),
        ThemeColorOption("Cyberpunk Spark", 12, Color(0xFFE91E63), Color(0xFF0F0E17)),
        ThemeColorOption("Classic Paper", 18, Color(0xFF795548), Color(0xFFF4ECD8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SEÇÃO DO PERFIL DO ALUNO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("profile_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações",
                    tint = LightText
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryIndigo.copy(alpha = 0.15f))
                .border(2.dp, PrimaryIndigo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = studentName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryIndigo
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = studentName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LightText
        )

        Text(
            text = "$userTitle • Nível $currentLevel",
            fontSize = 13.sp,
            color = MutedText,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- BARRA DE NÍVEL & XP ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_hours_progress_card"),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progresso de Nível",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                    val hoursFormatted = String.format("%.2f", totalHours)
                    Text(
                        text = "$hoursFormatted h de Estudo",
                        fontSize = 11.sp,
                        color = TertiaryMint,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressToNextLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("profile_level_progress_bar"),
                    color = PrimaryIndigo,
                    trackColor = GlassBorder
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "XP Total", fontSize = 10.sp, color = MutedText)
                        Text(
                            text = "$totalXp XP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryIndigo
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Fichas de IA (Gemini)", fontSize = 10.sp, color = MutedText)
                        Text(
                            text = "${5 + (totalXp / 50).toInt()} Fichas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TertiaryMint
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- MEDALHAS COLECIONÁVEIS ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Medalhas de Conquista",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Text(
                text = "Suba de nível para desbloquear novos selos de honra",
                fontSize = 11.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Grid customizada de Medalhas
            badges.chunked(3).forEach { badgeRow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    badgeRow.forEach { badge ->
                        val isUnlocked = currentLevel >= badge.levelRequired
                        val cardBg = if (isUnlocked) SlateSurface else SlateSurface.copy(alpha = 0.5f)
                        val cardBorder = if (isUnlocked) BorderStroke(1.dp, badge.color.copy(alpha = 0.5f)) else BorderStroke(1.dp, GlassBorder)

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = cardBorder
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (isUnlocked) {
                                    Text(text = badge.emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = badge.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = badge.description,
                                        fontSize = 8.sp,
                                        color = MutedText,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 10.sp,
                                        maxLines = 2
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Bloqueado",
                                        tint = MutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Nível ${badge.levelRequired}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedText
                                    )
                                }
                            }
                        }
                    }
                    // Se a linha não tiver 3 elementos, preencher com espacadores invisíveis
                    if (badgeRow.size < 3) {
                        for (i in 0 until (3 - badgeRow.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TEMAS DO APLICATIVO ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Personalização de Estilo",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Text(
                text = "Desbloqueie novas paletas de foco conforme evolui",
                fontSize = 11.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(12.dp))

            themes.forEach { theme ->
                val isUnlocked = currentLevel >= theme.levelRequired
                val themeBorder = if (isUnlocked) BorderStroke(1.dp, GlassBorder) else BorderStroke(1.dp, GlassBorder.copy(alpha = 0.5f))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = themeBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor)
                                    .border(1.dp, GlassBorder, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = theme.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) LightText else MutedText
                                )
                                Text(
                                    text = if (isUnlocked) "Tema Disponível" else "Desbloqueia no Nível ${theme.levelRequired}",
                                    fontSize = 10.sp,
                                    color = MutedText
                                )
                            }
                        }

                        if (isUnlocked) {
                            TextButton(onClick = { /* Tema selecionado esteticamente */ }) {
                                Text("Ativar", color = PrimaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Bloqueado",
                                tint = MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
