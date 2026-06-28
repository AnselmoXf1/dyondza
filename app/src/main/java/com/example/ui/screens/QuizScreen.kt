package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.QuizUiState

@Composable
fun QuizScreen(
    viewModel: FocusViewModel,
    topic: String,
    onBackToDashboard: () -> Unit
) {
    val quizState by viewModel.quizUiState.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()

    var currentQuestionIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = quizState) {
            is QuizUiState.Idle -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Quiz Instantâneo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valide seu aprendizado em '$topic' com 3 perguntas geradas em tempo real pela IA.",
                        fontSize = 14.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.generateQuiz(topic) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text("Gerar Perguntas com a IA")
                    }
                }
            }

            is QuizUiState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "A IA do Gemini está estruturando seu quiz...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Estamos validando as conexões cognitivas para o tópico '$topic'.",
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            is QuizUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Erro", tint = WarningRed, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ocorreu um erro",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.generateQuiz(topic) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text("Tentar Novamente")
                    }
                }
            }

            is QuizUiState.Success -> {
                val quiz = state.quiz
                val currentQuestion = quiz.questions.getOrNull(currentQuestionIndex)

                if (currentQuestion != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Cabeçalho de Progresso
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pergunta ${currentQuestionIndex + 1} de ${quiz.questions.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                            Text(
                                text = topic,
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex + 1) / quiz.questions.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryIndigo,
                            trackColor = GlassBorder
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Pergunta
                        Text(
                            text = currentQuestion.question,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Alternativas
                        currentQuestion.options.forEachIndexed { optIndex, option ->
                            val isSelected = selectedAnswers[currentQuestionIndex] == optIndex
                            val optionBorder = if (isSelected) BorderStroke(1.5.dp, PrimaryIndigo) else BorderStroke(1.dp, GlassBorder)
                            val optionBg = if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else SlateSurface

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { viewModel.selectAnswer(currentQuestionIndex, optIndex) },
                                shape = RoundedCornerShape(12.dp),
                                border = optionBorder,
                                colors = CardDefaults.cardColors(containerColor = optionBg)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectAnswer(currentQuestionIndex, optIndex) },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo, unselectedColor = MutedText)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        color = LightText,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botão Próxima ou Enviar
                        val hasSelected = selectedAnswers.containsKey(currentQuestionIndex)
                        val isLastQuestion = currentQuestionIndex == quiz.questions.size - 1

                        Button(
                            onClick = {
                                if (isLastQuestion) {
                                    viewModel.submitQuiz(quiz)
                                } else {
                                    currentQuestionIndex++
                                }
                            },
                            enabled = hasSelected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastQuestion) TertiaryMint else PrimaryIndigo
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isLastQuestion) "Submeter Avaliação" else "Próxima Pergunta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            is QuizUiState.Completed -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (state.score >= 2) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = "Sucesso",
                        tint = if (state.score >= 2) TertiaryMint else SecondaryAmber,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (state.score == 3) "Desempenho Impecável!" else if (state.score == 2) "Bom Trabalho!" else "Continue Estudando!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Você acertou ${state.score} de 3 perguntas sobre '$topic'.",
                        fontSize = 14.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Recompensas Card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "RECOMPENSAS ACADÊMICAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryAmber,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "XP", tint = TertiaryMint, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+${state.xpEarned} XP do Escudo Dyondza",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = LightText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            currentQuestionIndex = 0
                            onBackToDashboard()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Voltar ao Painel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
