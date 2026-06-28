package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlin.random.Random

// Representa uma partícula de confete animada
data class Confetti(
    val initialX: Float,
    val initialY: Float,
    val targetX: Float,
    val targetY: Float,
    val size: Float,
    val color: Color,
    val rotationStart: Float,
    val rotationEnd: Float,
    val isCircle: Boolean
)

@Composable
fun LevelUpCelebration(
    level: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")
    
    // Progresso de 0f a 1f para animar as posições do confete
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Progress"
    )

    // Ângulo de rotação contínuo para o brilho do fundo
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlowRotation"
    )

    // Animação de entrada do conteúdo central
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    // Geração determinística mas aleatória de confetes (gerados uma única vez)
    val confetes = remember {
        val list = mutableListOf<Confetti>()
        val colors = listOf(
            Color(0xFFFFC107), // Amber
            Color(0xFF26A69A), // Mint
            Color(0xFF3F51B5), // Indigo
            Color(0xFFE91E63), // Pink
            Color(0xFF00BCD4), // Cyan
            Color(0xFF9C27B0)  // Purple
        )
        for (i in 0..59) {
            val startX = Random.nextFloat() * 1000f - 100f
            val startY = -50f
            val endX = startX + (Random.nextFloat() * 200f - 100f)
            val endY = 1200f + Random.nextFloat() * 300f
            list.add(
                Confetti(
                    initialX = startX,
                    initialY = startY,
                    targetX = endX,
                    targetY = endY,
                    size = Random.nextFloat() * 14f + 8f,
                    color = colors.random(),
                    rotationStart = Random.nextFloat() * 360f,
                    rotationEnd = Random.nextFloat() * 720f + 360f,
                    isCircle = Random.nextBoolean()
                )
            )
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .testTag("level_up_celebration_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // --- Camada de Confete Animado no Fundo ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                confetes.forEach { confetti ->
                    // Interpolação de coordenadas com base no progresso
                    val currentX = confetti.initialX + (confetti.targetX - confetti.initialX) * animationProgress
                    val currentY = confetti.initialY + (confetti.targetY - confetti.initialY) * animationProgress
                    val currentRotation = confetti.rotationStart + (confetti.rotationEnd - confetti.rotationStart) * animationProgress

                    rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                        if (confetti.isCircle) {
                            drawCircle(
                                color = confetti.color,
                                radius = confetti.size / 2f,
                                center = Offset(currentX, currentY)
                            )
                        } else {
                            drawRect(
                                color = confetti.color,
                                size = androidx.compose.ui.geometry.Size(confetti.size, confetti.size / 1.5f),
                                topLeft = Offset(currentX - confetti.size / 2f, currentY - confetti.size / 3f)
                            )
                        }
                    }
                }
            }

            // --- Conteúdo da Comemoração com Entrada Animada ---
            AnimatedVisibility(
                visible = animateIn,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 350.dp)
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = PrimaryIndigo),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(PrimaryIndigo, TertiaryMint)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- Efeito de Raio de Sol / Glória Rotativa ---
                        Box(
                            modifier = Modifier
                                .size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Círculo de brilho de fundo
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .rotate(glowRotation)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color.Transparent,
                                                PrimaryIndigo,
                                                TertiaryMint,
                                                SecondaryAmber,
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )

                            // Medalha / Ícone com escala pulsante
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "MedalPulse"
                            )

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                PrimaryIndigo,
                                                PrimaryIndigo.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                                    .shadow(8.dp, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Estrela de Conquista",
                                    tint = SecondaryAmber,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Textos de Títulos e Elogios ---
                        Text(
                            text = "¡PARABÉNS!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TertiaryMint,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Novo Nível Alcançado!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = LightText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Indicador do Nível Grande e Imponente ---
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NÍVEL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedText,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = level.toString(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SecondaryAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "A sua dedicação aos estudos está a dar frutos! Você acumulou horas de foco valiosas para sua jornada acadêmica em Moçambique.",
                            fontSize = 13.sp,
                            color = MutedText,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Botão de Continuação para Retomar com Força ---
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigo
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("level_up_continue_button"),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Continuar Focado",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
