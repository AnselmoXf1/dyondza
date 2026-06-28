package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float, // 0.0f to 1.0f
    val progressText: String,
    val rewardXp: Int,
    val isCompleted: Boolean,
    val type: String // "Diária" ou "Semanal"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(viewModel: FocusViewModel) {
    val history by viewModel.studyHistory.collectAsState()

    // Cálculo dinâmico das missões com base no histórico real do Room
    val missions = remember(history) {
        val today = java.util.Calendar.getInstance()
        val sessionsToday = history.filter {
            val sessionCal = java.util.Calendar.getInstance().apply { timeInMillis = it.endTime }
            sessionCal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                    sessionCal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        }

        // 1. Leitor Noturno: 60 minutos após as 20h
        val nightStudySeconds = history.filter {
            val sessionCal = java.util.Calendar.getInstance().apply { timeInMillis = it.endTime }
            sessionCal.get(java.util.Calendar.HOUR_OF_DAY) >= 20 || sessionCal.get(java.util.Calendar.HOUR_OF_DAY) < 7
        }.sumOf { it.durationSeconds }
        val nightProgress = (nightStudySeconds / 3600f).coerceIn(0f, 1f)
        val nightMinutes = (nightStudySeconds / 60).coerceAtMost(60)

        // 2. Maratonista de Tópicos: 3 sessões da mesma disciplina hoje
        val topicGroupCount = sessionsToday.groupBy { it.topic }.values.map { it.size }.maxOrNull() ?: 0
        val maratonistaProgress = (topicGroupCount / 3f).coerceIn(0f, 1f)

        // 3. Polímata: 3 disciplinas diferentes hoje
        val uniqueTopicsToday = sessionsToday.distinctBy { it.topic }.size
        val polimataProgress = (uniqueTopicsToday / 3f).coerceIn(0f, 1f)

        // 4. Sexta-feira de Foco: Estudar na Sexta
        val isFriday = today.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        val studiedOnFriday = history.any {
            val sessionCal = java.util.Calendar.getInstance().apply { timeInMillis = it.endTime }
            sessionCal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        }
        val fridayProgress = if (studiedOnFriday) 1f else if (isFriday) 0f else 0f

        listOf(
            Mission(
                id = "m1",
                title = "Leitor Noturno (Sábio)",
                description = "Estude 60 minutos antes das 7h ou após as 20h.",
                progress = nightProgress,
                progressText = "$nightMinutes/60 min",
                rewardXp = 150,
                isCompleted = nightProgress >= 1f,
                type = "Diária"
            ),
            Mission(
                id = "m2",
                title = "Maratonista de Tópicos",
                description = "Conclua 3 sessões de foco no mesmo assunto hoje.",
                progress = maratonistaProgress,
                progressText = "$topicGroupCount/3 sessões",
                rewardXp = 100,
                isCompleted = maratonistaProgress >= 1f,
                type = "Diária"
            ),
            Mission(
                id = "m3",
                title = "Polímata Acadêmico",
                description = "Estude 3 disciplinas/tópicos diferentes hoje.",
                progress = polimataProgress,
                progressText = "$uniqueTopicsToday/3 tópicos",
                rewardXp = 120,
                isCompleted = polimataProgress >= 1f,
                type = "Semanal"
            ),
            Mission(
                id = "m4",
                title = "Sexta-Feira de Foco",
                description = "Garanta a consistência estudando na sexta-feira.",
                progress = fridayProgress,
                progressText = if (studiedOnFriday) "1/1 concluído" else "0/1 pendente",
                rewardXp = 80,
                isCompleted = studiedOnFriday,
                type = "Semanal"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // --- CABEÇALHO ---
        Text(
            text = "Missões Diárias & Semanais",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LightText
        )
        Text(
            text = "Conclua tarefas acadêmicas para obter recompensas e IA avançada",
            fontSize = 12.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- PROGRESSO GERAL ---
        val completedCount = missions.count { it.isCompleted }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Progresso das Missões",
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedCount de ${missions.size} missões cumpridas",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { completedCount / missions.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryIndigo,
                        trackColor = GlassBorder
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(PrimaryIndigo.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryIndigo)
                        Text(
                            text = "+${missions.filter { it.isCompleted }.sumOf { it.rewardXp }}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- LISTA VERTICAL DE MISSÕES ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(missions) { mission ->
                val progressAnimated by animateFloatAsState(targetValue = mission.progress, label = "Progress")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mission.isCompleted) SlateSurface else SlateSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (mission.isCompleted) PrimaryIndigo.copy(alpha = 0.5f) else GlassBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (mission.type == "Diária") SecondaryAmber.copy(alpha = 0.1f)
                                            else PrimaryIndigo.copy(alpha = 0.1f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = mission.type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mission.type == "Diária") SecondaryAmber else PrimaryIndigo
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mission.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }

                            if (mission.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Concluída",
                                    tint = TertiaryMint,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = mission.progressText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = mission.description,
                            fontSize = 12.sp,
                            color = MutedText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LinearProgressIndicator(
                                progress = { progressAnimated },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (mission.isCompleted) TertiaryMint else PrimaryIndigo,
                                trackColor = GlassBorder
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "+${mission.rewardXp} XP",
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
