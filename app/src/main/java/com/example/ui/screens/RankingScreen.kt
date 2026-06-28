package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentRank
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.components.SchoolWeeklyLeaderboard

enum class RankingFilter {
    GLOBAL, PROVINCE, SCHOOL, AREA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(viewModel: FocusViewModel) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val studentName by viewModel.studentName.collectAsState()
    val studentProvince by viewModel.studentProvince.collectAsState()
    val studentSchool by viewModel.studentSchool.collectAsState()
    val studentFocusArea by viewModel.studentFocusArea.collectAsState()
    var selectedFilter by remember { mutableStateOf(RankingFilter.GLOBAL) }

    // Filtrar e reordenar a lista dinamicamente
    val filteredList = remember(leaderboard, selectedFilter, studentProvince, studentSchool, studentFocusArea) {
        val filtered = when (selectedFilter) {
            RankingFilter.GLOBAL -> leaderboard
            RankingFilter.PROVINCE -> leaderboard.filter { it.province.equals(studentProvince, ignoreCase = true) }
            RankingFilter.SCHOOL -> leaderboard.filter { it.school.contains(studentSchool, ignoreCase = true) || studentSchool.contains(it.school, ignoreCase = true) }
            RankingFilter.AREA -> leaderboard.filter { it.focusArea.equals(studentFocusArea, ignoreCase = true) }
        }
        filtered.sortedByDescending { it.totalXp }
    }

    // Dividir em Top 3 e Restante para o Podium
    val top1 = filteredList.getOrNull(0)
    val top2 = filteredList.getOrNull(1)
    val top3 = filteredList.getOrNull(2)
    val remainingStudents = if (filteredList.size > 3) filteredList.subList(3, filteredList.size) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // --- CABEÇALHO DO LEADERBOARD ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Arena de Líderes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Text(
                    text = "Desempenho nacional e regional em tempo real",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }

            IconButton(
                onClick = { viewModel.syncWithFirestore() },
                enabled = !isSyncing,
                colors = IconButtonDefaults.iconButtonColors(contentColor = PrimaryIndigo)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryIndigo)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- FILTROS DE CRESCIMENTO (TAB SELECTOR) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RankingFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                val title = when (filter) {
                    RankingFilter.GLOBAL -> "Geral"
                    RankingFilter.PROVINCE -> "Província"
                    RankingFilter.SCHOOL -> "Escola"
                    RankingFilter.AREA -> "Minha Área"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                        .clickable { selectedFilter = filter }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFilter == RankingFilter.SCHOOL) {
            val studyHistory by viewModel.studyHistory.collectAsState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SchoolWeeklyLeaderboard(
                        studentSchool = studentSchool,
                        studentName = studentName,
                        studyHistory = studyHistory
                    )
                }
                
                // Banner Técnico de Paginação & Cloud Architecture (Young CEO Vision)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Arquitetura Pronta para Escala (CEO Vision)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Para suportar mais de 10.000 alunos ativos em Moçambique, os rankings são processados no lado do servidor via Firebase Cloud Functions e agregados em blocos de 20 registros por paginação otimizada, prevenindo fraudes de alteração de XP no aparelho e economizando dados móveis.",
                                fontSize = 10.sp,
                                color = MutedText,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // --- LISTAGEM DE TODO O RANKING (Usamos LazyColumn para englobar Podium e Itens com excelente performance) ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Podium de destaque
            item {
                if (top1 != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Top 2 (Esquerda)
                        if (top2 != null) {
                            PodiumMember(
                                student = top2,
                                rank = 2,
                                height = 90.dp,
                                avatarColor = Color(0xFFC0C0C0), // Prata
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Top 1 (Centro) - Maior, dourado brilhante
                        PodiumMember(
                            student = top1,
                            rank = 1,
                            height = 120.dp,
                            avatarColor = SecondaryAmber, // Ouro
                            modifier = Modifier.weight(1.2f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Top 3 (Direita)
                        if (top3 != null) {
                            PodiumMember(
                                student = top3,
                                rank = 3,
                                height = 75.dp,
                                avatarColor = Color(0xFFCD7F32), // Bronze
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Carregando classificação...", color = MutedText, fontSize = 14.sp)
                    }
                }
            }

            // Separador Visual para o resto da lista
            if (remainingStudents.isNotEmpty()) {
                item {
                    Text(
                        text = "Outros Competidores",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Alunos abaixo do Top 3
            itemsIndexed(remainingStudents) { index, student ->
                val displayRank = index + 4
                val isSelf = student.name == studentName
                val cardBorder = if (isSelf) BorderStroke(1.dp, PrimaryIndigo) else BorderStroke(1.dp, GlassBorder)
                val cardBg = if (isSelf) SlateSurface.copy(alpha = 0.9f) else SlateSurface

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge de Posição
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GlassBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayRank.toString(),
                                fontWeight = FontWeight.Bold,
                                color = LightText,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Nome, província e escola
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = student.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelf) PrimaryIndigo else LightText
                                )
                                if (isSelf) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Você)",
                                        fontSize = 11.sp,
                                        color = PrimaryIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${student.school} • ${student.province}",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                            if (student.focusArea.isNotEmpty()) {
                                Text(
                                    text = "Foco: ${student.focusArea}",
                                    fontSize = 10.sp,
                                    color = TertiaryMint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Pontos de XP
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${student.totalXp} XP",
                                fontWeight = FontWeight.Black,
                                color = TertiaryMint,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Nível ${student.level}",
                                fontSize = 10.sp,
                                color = MutedText
                            )
                        }
                    }
                }
            }

            // Banner Técnico de Paginação & Cloud Architecture (Young CEO Vision)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Arquitetura Pronta para Escala (CEO Vision)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Para suportar mais de 10.000 alunos ativos em Moçambique, os rankings são processados no lado do servidor via Firebase Cloud Functions e agregados em blocos de 20 registros por paginação otimizada, prevenindo fraudes de alteração de XP no aparelho e economizando dados móveis.",
                            fontSize = 10.sp,
                            color = MutedText,
                            lineHeight = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }
}

@Composable
fun PodiumMember(
    student: StudentRank,
    rank: Int,
    height: Dp,
    avatarColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (rank == 1) 76.dp else 60.dp)
                .clip(CircleShape)
                .background(SlateSurface)
                .border(
                    width = if (rank == 1) 3.dp else 1.5.dp,
                    color = avatarColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (rank == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(avatarColor.copy(alpha = 0.15f))
                )
            }
            Text(
                text = student.name.firstOrNull()?.toString()?.uppercase() ?: "",
                fontWeight = FontWeight.Black,
                fontSize = if (rank == 1) 24.sp else 18.sp,
                color = avatarColor
            )
            // Medalha/Número na base do avatar
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = student.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = "${student.totalXp} XP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TertiaryMint,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Coluna sólida do Podium
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SlateSurface, SlateSurface.copy(alpha = 0.3f))
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (rank == 1) avatarColor.copy(alpha = 0.4f) else GlassBorder.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (rank) {
                    1 -> "👑"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> ""
                },
                fontSize = 20.sp
            )
        }
    }
}
