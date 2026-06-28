package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FocusTrackerService
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.QuizUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FocusViewModel,
    onNavigateToQuiz: (String) -> Unit
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsState()
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
    val sessionSummary by viewModel.sessionSummary.collectAsState()
    val coachAdvice by viewModel.coachAdvice.collectAsState()
    val quizUiState by viewModel.quizUiState.collectAsState()
    val studentName by viewModel.studentName.collectAsState()
    val history by viewModel.studyHistory.collectAsState()
    
    val totalHours by viewModel.totalStudyHours.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val progressToNextLevel by viewModel.progressToNextLevel.collectAsState()

    val totalMinutes = history.sumOf { it.durationSeconds } / 60
    val totalXp = (totalMinutes * 10) + (history.size * 30)

    var studyTopic by remember { mutableStateOf("Desenvolvimento de Software") }
    var selectedDurationPreset by remember { mutableLongStateOf(0L) }
    var isPermissionGranted by remember { mutableStateOf(FocusTrackerService.isUsageAccessGranted(context)) }

    // Efeito para re-checar permissão quando a tela ganha foco
    LaunchedEffect(Unit) {
        while (true) {
            isPermissionGranted = FocusTrackerService.isUsageAccessGranted(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    if (sessionSummary != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSessionSummary() },
            containerColor = SlateSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sucesso",
                        tint = TertiaryMint,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Sessão Concluída! 🎉",
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Parabéns por manter o foco! O seu cérebro agradece pelo esforço.",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tópico:", color = MutedText, fontSize = 13.sp)
                                Text(sessionSummary!!.topic, color = LightText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            
                            val completedMins = sessionSummary!!.durationSeconds / 60
                            val completedSecs = sessionSummary!!.durationSeconds % 60
                            val durationFormatted = String.format("%02d:%02d", completedMins, completedSecs)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tempo de Foco:", color = MutedText, fontSize = 13.sp)
                                Text(durationFormatted, color = TertiaryMint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Avisos evitados:", color = MutedText, fontSize = 13.sp)
                                Text(
                                    text = "${sessionSummary!!.warningsCount}/3",
                                    color = if (sessionSummary!!.warningsCount > 0) SecondaryAmber else TertiaryMint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            
                            HorizontalDivider(color = GlassBorder)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("XP Adquirido:", color = MutedText, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = SecondaryAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+${sessionSummary!!.xpEarned} XP", color = SecondaryAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSessionSummary() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABEÇALHO PERFIL DE ALTA PERFORMANCE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bem-vinda, $studentName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Text(
                    text = "Nível $currentLevel de Estudo • $totalXp XP",
                    fontSize = 13.sp,
                    color = MutedText
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = studentName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // --- INDICADOR INTEGRADO DE HORAS E PROGRESSO DE NÍVEL ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("dashboard_hours_progress_card"),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Ícone de Progresso",
                            tint = SecondaryAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nível $currentLevel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                    val hoursFormatted = String.format("%.2f", totalHours)
                    Text(
                        text = "$hoursFormatted h acumuladas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TertiaryMint
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar baseada nas horas de estudo acumuladas
                LinearProgressIndicator(
                    progress = { progressToNextLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("dashboard_level_progress_bar"),
                    color = TertiaryMint,
                    trackColor = GlassBorder
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Milestone Atual",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                    Text(
                        text = "Próximo: Nível ${currentLevel + 1}",
                        fontSize = 11.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- ALERTA DE PERMISSÃO DE ACESSO AO USO ---
        if (!isPermissionGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = SecondaryAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escudo de Distração Desativado",
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Para que o Dyondza verifique se você está focado e bloqueie redes sociais, precisamos da permissão de Acesso ao Uso.",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Conceder Acesso nas Configurações", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- SEÇÃO DO CRONÔMETRO DE FOCO (GLOWING TIMER UI) ---
        val infiniteTransition = rememberInfiniteTransition(label = "GlowTransition")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowScale"
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .shadow(
                    elevation = if (sessionState.isActive) 24.dp else 12.dp,
                    shape = CircleShape,
                    spotColor = if (sessionState.isActive) {
                        if (sessionState.warningsCount > 0) SecondaryAmber else PrimaryIndigo
                    } else Color.Transparent
                )
                .border(
                    BorderStroke(
                        4.dp,
                        if (sessionState.isActive) {
                            if (sessionState.warningsCount > 0) SecondaryAmber else PrimaryIndigo
                        } else GlassBorder
                    ),
                    CircleShape
                )
                .background(SlateSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val animatedColor by animateColorAsState(
                    targetValue = if (sessionState.isActive) {
                        if (sessionState.warningsCount > 0) SecondaryAmber else PrimaryIndigo
                    } else MutedText,
                    label = "TimerColor"
                )

                Text(
                    text = if (sessionState.isActive) {
                        if (sessionState.isCountdown) "ESCUDO ATIVO" else "FOCO ATIVO"
                    } else "MODO ESTUDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedColor,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Formatação do tempo
                val displaySeconds = if (sessionState.isCountdown) sessionState.secondsRemaining else sessionState.secondsElapsed
                val mins = displaySeconds / 60
                val secs = displaySeconds % 60
                val timeFormatted = String.format("%02d:%02d", mins, secs)

                Text(
                    text = timeFormatted,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = LightText,
                    letterSpacing = (-1).sp
                )

                if (sessionState.isActive) {
                    Text(
                        text = studyTopic,
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CONFIGURAÇÃO DO ESCUDO DE FOCO (DURAÇÃO E NOTIFICAÇÕES) ---
        if (!sessionState.isActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configurar Escudo Anti-Distração",
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Duração do Foco:",
                        color = MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val durationPresets = listOf(
                        Pair("Livre", 0L),
                        Pair("10s (Test)", 10L),
                        Pair("25m", 1500L),
                        Pair("45m", 2700L),
                        Pair("60m", 3600L)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationPresets.forEach { (label, duration) ->
                            val isSelected = selectedDurationPreset == duration
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDurationPreset = duration },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryIndigo,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkBg,
                                    labelColor = MutedText
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GlassBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isNotificationEnabled) TertiaryMint else MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bloquear Notificações",
                                    fontWeight = FontWeight.Bold,
                                    color = LightText,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Silencia popups de distrações durante a sessão de foco.",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { viewModel.toggleNotificationBlocking(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TertiaryMint,
                                checkedTrackColor = TertiaryMint.copy(alpha = 0.4f),
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = GlassBorder
                            )
                        )
                    }
                }
            }
        }

        // --- INPUT DO TÓPICO DE ESTUDO ---
        if (!sessionState.isActive) {
            OutlinedTextField(
                value = studyTopic,
                onValueChange = { studyTopic = it },
                label = { Text("O que você vai estudar agora?", color = MutedText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LightText,
                    unfocusedTextColor = LightText,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = SlateSurface,
                    unfocusedContainerColor = SlateSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- CONTROLES DA SESSÃO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!sessionState.isActive) {
                Button(
                    onClick = { viewModel.startSession(context, studyTopic, selectedDurationPreset) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar Sessão", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.stopSession(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryMint),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Concluir")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Concluir e Salvar", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.cancelSession(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Desistir")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- PAINEL DE ESCUDO ANTI-DISTRAÇÃO ---
        AnimatedVisibility(visible = sessionState.isActive) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            text = "Escudo Anti-Distração",
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (sessionState.warningsCount > 0) SecondaryAmber else TertiaryMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Avisos: ${sessionState.warningsCount}/3",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sessionState.warningsCount > 0) SecondaryAmber else TertiaryMint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Status: ${sessionState.statusMessage}",
                        fontSize = 12.sp,
                        color = if (sessionState.warningsCount >= 2) WarningRed else MutedText
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = GlassBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Painel de Testes/Simulação do Escudo (Padrão Startup de Alta Maturidade)
                    Text(
                        text = "Simulador de Distração (Fácil para Emulador):",
                        fontSize = 11.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateDistraction(context, "com.whatsapp") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1BD741)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Abrir WhatsApp", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.simulateDistraction(context, "com.instagram.android") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Abrir Instagram", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- COACH DE ESTUDO GEMINI AI ADVICE ---
        if (coachAdvice != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Coach IA",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coach de Estudos Inteligente (Gemini API)",
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = coachAdvice!!,
                        fontSize = 13.sp,
                        color = LightText,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.generateQuiz(studyTopic)
                            onNavigateToQuiz(studyTopic)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryMint),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Quiz de Retenção", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- BOTÃO DE RE-QUIZ RÁPIDO SE NÃO TIVER ATIVO ---
        if (!sessionState.isActive && coachAdvice == null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    viewModel.generateQuiz(studyTopic)
                    onNavigateToQuiz(studyTopic)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TertiaryMint),
                border = BorderStroke(1.dp, TertiaryMint),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gerar Quiz Instantâneo com IA", fontWeight = FontWeight.Bold)
            }
        }
    }
}
