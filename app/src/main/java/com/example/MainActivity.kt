package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.RankingScreen
import com.example.ui.screens.AuthNavigator
import com.example.ui.screens.SettingsScreen
import com.example.ui.components.LevelUpCelebration
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FocusViewModel

enum class Screen {
    DASHBOARD, MISSIONS, RANKING, HISTORY, PROFILE, QUIZ, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    val context = LocalContext.current
    val viewModel: FocusViewModel = viewModel(factory = FocusViewModel.Factory(context))

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var activeQuizTopic by remember { mutableStateOf("Geral") }

    // Solicitar Permissão de Notificações para Android 13+ (Obrigatório para Foreground Services)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val levelUpCelebration by viewModel.levelUpCelebration.collectAsState()

    if (!isLoggedIn) {
        AuthNavigator(
            viewModel = viewModel,
            onAuthSuccess = {
                currentScreen = Screen.DASHBOARD
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Só mostra o bottom navigation se não estiver respondendo o quiz ativo ou nas configurações
            if (currentScreen != Screen.QUIZ && currentScreen != Screen.SETTINGS) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.DASHBOARD,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Foco") },
                        label = { Text("Foco", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.MISSIONS,
                        onClick = { currentScreen = Screen.MISSIONS },
                        icon = { Icon(Icons.Default.Check, contentDescription = "Missões") },
                        label = { Text("Missões", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.RANKING,
                        onClick = { currentScreen = Screen.RANKING },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Ranking") },
                        label = { Text("Leaderboard", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.HISTORY,
                        onClick = { currentScreen = Screen.HISTORY },
                        icon = { Icon(Icons.Default.List, contentDescription = "Histórico") },
                        label = { Text("Métricas", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.PROFILE,
                        onClick = { currentScreen = Screen.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToQuiz = { topic ->
                        activeQuizTopic = topic
                        currentScreen = Screen.QUIZ
                    }
                )
                Screen.MISSIONS -> MissionsScreen(
                    viewModel = viewModel
                )
                Screen.RANKING -> RankingScreen(
                    viewModel = viewModel
                )
                Screen.HISTORY -> HistoryScreen(
                    viewModel = viewModel
                )
                Screen.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                )
                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onBackClicked = { currentScreen = Screen.PROFILE },
                    onLogoutClicked = {
                        viewModel.logout()
                        currentScreen = Screen.DASHBOARD
                    }
                )
                Screen.QUIZ -> QuizScreen(
                    viewModel = viewModel,
                    topic = activeQuizTopic,
                    onBackToDashboard = { currentScreen = Screen.DASHBOARD }
                )
            }

            // Global Level Up Celebration Overlay
            levelUpCelebration?.let { level ->
                LevelUpCelebration(
                    level = level,
                    onDismiss = { viewModel.dismissLevelUpCelebration() }
                )
            }
        }
    }
}

