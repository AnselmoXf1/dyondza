package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSession
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class WeeklySchoolMockStudent(
    val name: String,
    val weeklyHours: Double,
    val isSelf: Boolean = false,
    val initial: String
)

@Composable
fun SchoolWeeklyLeaderboard(
    studentSchool: String,
    studentName: String,
    studyHistory: List<FocusSession>
) {
    val schoolDisplayName = studentSchool.ifEmpty { "Escola Secundária Josina Machel" }

    // Calcular as horas de estudo reais do usuário na semana atual (últimos 7 dias)
    val userWeeklyHours = remember(studyHistory) {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val seconds = studyHistory.filter { it.startTime >= oneWeekAgo }.sumOf { it.durationSeconds }
        seconds.toDouble() / 3600.0
    }

    // Gerar lista de estudantes da escola para a semana
    val leaderboardList = remember(userWeeklyHours, studentName) {
        val baseMocks = listOf(
            WeeklySchoolMockStudent("Elena Mondlane", 14.8, initial = "E"),
            WeeklySchoolMockStudent("Mateus Chongo", 12.5, initial = "M"),
            WeeklySchoolMockStudent("Sérgio Langa", 9.4, initial = "S"),
            WeeklySchoolMockStudent("Lucília Tembe", 7.2, initial = "L"),
            WeeklySchoolMockStudent("Anselmo Macuácua", 4.5, initial = "A")
        )

        // Verificar se o nome do usuário colide com algum mock, ou adicioná-lo de forma limpa
        val cleanName = studentName.ifEmpty { "Amina Muthemba" }
        val hasUserInMocks = baseMocks.any { it.name.equals(cleanName, ignoreCase = true) }

        val combined = if (hasUserInMocks) {
            baseMocks.map {
                if (it.name.equals(cleanName, ignoreCase = true)) {
                    it.copy(weeklyHours = userWeeklyHours, isSelf = true)
                } else {
                    it
                }
            }
        } else {
            baseMocks + WeeklySchoolMockStudent(
                name = cleanName,
                weeklyHours = userWeeklyHours,
                isSelf = true,
                initial = cleanName.firstOrNull()?.toString()?.uppercase() ?: "U"
            )
        }

        combined.sortedByDescending { it.weeklyHours }
    }

    // Gerar string com intervalo de datas da semana atual
    val weekIntervalString = remember {
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Ajustar para segunda-feira da semana corrente
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startStr = sdf.format(cal.time)
        // Ajustar para domingo
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endStr = sdf.format(cal.time)
        "Semana de $startStr a $endStr"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("school_leaderboard_component")
    ) {
        // --- Header Informativo da Escola ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("school_leaderboard_header_card"),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = schoolDisplayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = weekIntervalString,
                        fontSize = 11.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Compita saudavelmente com seus colegas de escola acumulando horas reais de estudo concentrado esta semana. O ranking é atualizado ao concluir sessões de foco.",
                    fontSize = 11.sp,
                    color = MutedText,
                    lineHeight = 15.sp
                )
            }
        }

        // --- Lista de Alunos ---
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("school_leaderboard_list")
        ) {
            leaderboardList.forEachIndexed { index, student ->
                val rank = index + 1
                val isSelf = student.isSelf
                val itemBg = if (isSelf) SlateSurface.copy(alpha = 0.95f) else SlateSurface
                val itemBorder = if (isSelf) BorderStroke(1.2.dp, PrimaryIndigo) else BorderStroke(1.dp, GlassBorder)

                // Destaques de cores para o Top 3
                val rankBadgeColor = when (rank) {
                    1 -> Color(0xFFD4AF37) // Dourado
                    2 -> Color(0xFFC0C0C0) // Prata
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> GlassBorder
                }

                val rankTextColor = when (rank) {
                    1, 2, 3 -> Color.White
                    else -> LightText
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("school_leaderboard_item_$rank"),
                    colors = CardDefaults.cardColors(containerColor = itemBg),
                    border = itemBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge da posição
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(rankBadgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rank.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = rankTextColor
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Avatar Circular Simples
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo.copy(alpha = 0.1f))
                                .border(1.dp, PrimaryIndigo.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = student.initial,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Nome do Aluno
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = student.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelf) PrimaryIndigo else LightText
                                )
                                if (isSelf) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Você)",
                                        fontSize = 10.sp,
                                        color = PrimaryIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Moçambique • Escola Ativa",
                                fontSize = 10.sp,
                                color = MutedText
                            )
                        }

                        // Horas Semanais de Estudo Acumuladas
                        val hoursFormatted = String.format("%.1f", student.weeklyHours)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = TertiaryMint.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$hoursFormatted h",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TertiaryMint
                            )
                        }
                    }
                }
            }
        }
    }
}
