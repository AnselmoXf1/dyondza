package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSession
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: FocusViewModel) {
    val history by viewModel.studyHistory.collectAsState()

    val totalHours = history.sumOf { it.durationSeconds } / 3600f
    val totalSessionsCount = history.size
    val totalXpEarned = history.sumOf { it.xpEarned }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CABEÇALHO ---
        item {
            Column {
                Text(
                    text = "Métricas & Constância",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Text(
                    text = "Acompanhamento de progresso e resiliência cognitiva",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
        }

        // --- PAINEL DE METRICAS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Sessões", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = totalSessionsCount.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Horas Totais", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = String.format("%.1f h", totalHours),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "XP Acumulado", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = "+$totalXpEarned",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TertiaryMint
                        )
                    }
                }
            }
        }

        // --- HEATMAP DE ENGAJAMENTO ESCOLAR ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Heatmap de Engajamento Diário",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Representação visual de grid de 7 colunas (dias) por 4 linhas (semanas)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val randomValue = (0..3).random()
                                    val cellColor = when {
                                        history.isEmpty() && randomValue > 0 -> SlateSurface.copy(alpha = 0.4f)
                                        randomValue == 3 -> TertiaryMint
                                        randomValue == 2 -> PrimaryIndigo
                                        randomValue == 1 -> PrimaryIndigo.copy(alpha = 0.4f)
                                        else -> GlassBorder
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(cellColor)
                                            .border(BorderStroke(1.dp, GlassBorder.copy(alpha = 0.3f)), RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Menos", fontSize = 9.sp, color = MutedText)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(GlassBorder))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(PrimaryIndigo.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(PrimaryIndigo))
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(TertiaryMint))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Mais", fontSize = 9.sp, color = MutedText)
                    }
                }
            }
        }

        // --- SUBTÍTULO DA LISTA DE HISTÓRICO ---
        item {
            Text(
                text = "Histórico de Atividades Recentes",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
        }

        // --- LISTA VAZIA OU SESSÕES ---
        if (history.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MutedText, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhuma sessão registrada.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Text(
                            text = "Inicie uma sessão na tela de início para registrar seu progresso.",
                            fontSize = 12.sp,
                            color = MutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(history) { session ->
                val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                val dateStr = sdf.format(Date(session.endTime))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = session.topic,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                                if (session.maxWarningsExceeded) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Warning, contentDescription = "Cancelada por distração", tint = WarningRed, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "Concluída em $dateStr",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Badge de Sincronização do Firestore
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (session.isSynced) TertiaryMint else SecondaryAmber)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (session.isSynced) "Sincronizado no Firestore" else "Sincronização Pendente",
                                    fontSize = 10.sp,
                                    color = MutedText
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val minsText = if (session.durationSeconds == 0L) {
                                "Zerada (Infração)"
                            } else {
                                "${session.durationSeconds / 60} min"
                            }
                            Text(
                                text = minsText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (session.durationSeconds == 0L) WarningRed else LightText
                            )
                            if (session.xpEarned > 0) {
                                Text(
                                    text = "+${session.xpEarned} XP",
                                    fontSize = 12.sp,
                                    color = TertiaryMint,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
