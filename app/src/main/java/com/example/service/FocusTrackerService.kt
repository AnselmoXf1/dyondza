package com.example.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Serviço de Primeiro Plano (Foreground Service) que valida e monitora a sessão de foco do aluno.
 * Utiliza a UsageStatsManager API para detectar desvios de atenção e implementar o
 * "Anti-Distraction Shield" em tempo real.
 */
class FocusTrackerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var trackingJob: Job? = null

    // Dependências de persistência
    private lateinit var repository: FocusRepository

    // Lista de pacotes considerados "proibidos" durante a sessão de foco
    private val prohibitedPackages = setOf(
        "com.whatsapp",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.google.android.youtube",
        "com.tiktok.android",
        "com.zhiliaoapp.musically",
        "com.netflix.mediaclient",
        "com.valvesoftware.android.steam.community"
    )

    override fun onCreate() {
        super.onCreate()
        val dao = AppDatabase.getDatabase(applicationContext).focusDao()
        repository = FocusRepository(dao)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val topic = intent.getStringExtra(EXTRA_TOPIC) ?: "Estudos"
                val countdownDuration = intent.getLongExtra(EXTRA_COUNTDOWN_DURATION, 0L)
                startFocusSession(topic, countdownDuration)
            }
            ACTION_STOP -> {
                stopFocusSession(completed = true)
            }
            ACTION_CANCEL -> {
                stopFocusSession(completed = false)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Inicia o cronômetro e o monitoramento em primeiro plano da UsageStatsManager API.
     */
    private fun startFocusSession(topic: String, countdownDuration: Long = 0L) {
        if (_sessionState.value.isActive) return

        val isCountdown = countdownDuration > 0L
        val initialState = FocusSessionState(
            isActive = true,
            topic = topic,
            secondsElapsed = 0L,
            secondsRemaining = if (isCountdown) countdownDuration else 0L,
            isCountdown = isCountdown,
            initialDurationSeconds = countdownDuration,
            warningsCount = 0,
            lastForegroundApp = "Dyondza",
            statusMessage = "Foco ativado! Escudo protetor ligado."
        )
        _sessionState.value = initialState

        // Inicia a notificação persistente obrigatória para Foreground Services
        startForeground(NOTIFICATION_ID, buildNotification(initialState))

        // Inicializa a Coroutine periódica de monitoramento
        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(1000) // Monitoramento a cada 1 segundo para responsividade máxima
                
                val currentState = _sessionState.value
                var updatedSeconds = currentState.secondsElapsed + 1
                var updatedRemaining = if (currentState.isCountdown) {
                    (currentState.secondsRemaining - 1).coerceAtLeast(0L)
                } else {
                    0L
                }

                // 2. Consulta qual aplicativo está no topo (Foreground)
                val currentApp = getForegroundAppPackage()
                var warnings = currentState.warningsCount
                var status = "Sessão protegida pelo Escudo Anti-Distração"
                var resetSession = false
                var autoStopSession = false

                if (currentState.isCountdown && updatedRemaining == 0L) {
                    autoStopSession = true
                    status = "Tempo esgotado! Sessão de foco concluída com sucesso."
                }

                // 3. Valida distração contra a lista negra
                if (!autoStopSession && currentApp != null && currentApp != packageName && prohibitedPackages.contains(currentApp)) {
                    // Evita disparar múltiplos avisos seguidos no mesmo segundo
                    if (currentState.lastForegroundApp != currentApp) {
                        warnings++
                        Log.w("FocusTrackerService", "Distração detectada: $currentApp! Avisos: $warnings/3")
                        
                        // Zera o tempo acumulado na sessão atual
                        updatedSeconds = 0L
                        if (currentState.isCountdown) {
                            updatedRemaining = currentState.initialDurationSeconds
                        }

                        // Alerta sonoro imediato de infração
                        try {
                            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
                        } catch (e: Exception) {
                            Log.e("FocusTrackerService", "Erro ao emitir sinal sonoro de distração", e)
                        }

                        // Resposta tátil forte de vibração
                        try {
                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                            if (vibrator != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(600, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(600)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FocusTrackerService", "Erro ao acionar vibração", e)
                        }

                        val appFriendlyName = when {
                            currentApp.contains("whatsapp") -> "WhatsApp"
                            currentApp.contains("instagram") -> "Instagram"
                            currentApp.contains("facebook") -> "Facebook"
                            currentApp.contains("twitter") -> "X/Twitter"
                            currentApp.contains("youtube") -> "YouTube"
                            currentApp.contains("tiktok") -> "TikTok"
                            currentApp.contains("netflix") -> "Netflix"
                            else -> currentApp.substringAfterLast(".")
                        }

                        if (warnings >= 3) {
                            // Se atingir 3 avisos, a sessão é cancelada automaticamente por excesso de tentativas
                            resetSession = true
                            status = "Sessão cancelada! Você abriu apps proibidos 3 vezes."
                        } else {
                            status = "Foco zerado! Você abriu o app proibido: $appFriendlyName. (Aviso $warnings/3)"
                        }
                    }
                }

                if (resetSession) {
                    _sessionState.value = currentState.copy(
                        secondsElapsed = 0L,
                        secondsRemaining = 0L,
                        warningsCount = warnings,
                        lastForegroundApp = currentApp ?: "Desconhecido",
                        statusMessage = status
                    )
                    // Para o serviço imediatamente informando falha por trapaça
                    stopFocusSession(completed = false, warningsExceeded = true)
                    break
                } else if (autoStopSession) {
                    _sessionState.value = currentState.copy(
                        secondsElapsed = updatedSeconds,
                        secondsRemaining = 0L,
                        warningsCount = warnings,
                        statusMessage = status
                    )
                    stopFocusSession(completed = true)
                    break
                } else {
                    val nextState = currentState.copy(
                        secondsElapsed = updatedSeconds,
                        secondsRemaining = updatedRemaining,
                        warningsCount = warnings,
                        lastForegroundApp = currentApp ?: currentState.lastForegroundApp,
                        statusMessage = status
                    )
                    _sessionState.value = nextState
                    
                    // Atualiza a notificação de primeiro plano em tempo real
                    updateNotification(nextState)
                }
            }
        }
    }

    /**
     * Encerra a sessão de foco, salvando ou descartando dependendo das regras do negócio.
     */
    private fun stopFocusSession(completed: Boolean, warningsExceeded: Boolean = false) {
        trackingJob?.cancel()
        trackingJob = null

        val finalState = _sessionState.value
        serviceScope.launch {
            if (completed && finalState.secondsElapsed > 5) { // Só salva se estudou pelo menos 5s
                repository.saveSession(
                    topic = finalState.topic,
                    durationSeconds = finalState.secondsElapsed,
                    distractions = finalState.warningsCount,
                    warningsExceeded = false
                )
                Log.i("FocusTrackerService", "Sessão salva com sucesso!")
            } else if (warningsExceeded) {
                // Salva registro de sessão cancelada/zerada com infração
                repository.saveSession(
                    topic = finalState.topic,
                    durationSeconds = 0L,
                    distractions = finalState.warningsCount,
                    warningsExceeded = true
                )
            }

            _sessionState.value = FocusSessionState(
                isActive = false,
                topic = "",
                secondsElapsed = 0L,
                warningsCount = if (warningsExceeded) 3 else 0,
                statusMessage = if (warningsExceeded) "Sessão zerada por excesso de distração." else "Sessão concluída!",
                lastCompletedDuration = if (completed) finalState.secondsElapsed else 0L,
                justCompleted = completed
            )

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Utiliza a UsageStatsManager API para obter o pacote do app ativo no topo da tela.
     * Requer permissão de acesso ao uso do sistema.
     */
    private fun getForegroundAppPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        
        // Consultar eventos dos últimos 10 segundos
        val events = usageStatsManager.queryEvents(time - 10000, time)
        val event = android.app.usage.UsageEvents.Event()
        var lastForegroundApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundApp = event.packageName
            }
        }
        return lastForegroundApp
    }

    /**
     * Constrói e formata a notificação em primeiro plano.
     */
    private fun buildNotification(state: FocusSessionState): Notification {
        val displaySeconds = if (state.isCountdown) state.secondsRemaining else state.secondsElapsed
        val minutes = displaySeconds / 60
        val seconds = displaySeconds % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dyondza Shield: Focando em '${state.topic}'")
            .setContentText("Tempo: $timeStr | Avisos: ${state.warningsCount}/3")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // Usando ícone do sistema para compatibilidade imediata
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(state: FocusSessionState) {
        val notification = buildNotification(state)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Dyondza Monitor de Foco",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente enquanto o cronômetro do Dyondza está rodando"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Classe que representa o estado reativo da sessão de foco ativa.
     */
    data class FocusSessionState(
        val isActive: Boolean = false,
        val topic: String = "",
        val secondsElapsed: Long = 0L,
        val secondsRemaining: Long = 0L,
        val isCountdown: Boolean = false,
        val initialDurationSeconds: Long = 0L,
        val warningsCount: Int = 0,
        val lastForegroundApp: String = "",
        val statusMessage: String = "",
        val lastCompletedDuration: Long = 0L,
        val justCompleted: Boolean = false
    )

    companion object {
        const val CHANNEL_ID = "com.example.dyondza.FOCUS_CHANNEL"
        const val NOTIFICATION_ID = 89903

        const val ACTION_START = "com.example.dyondza.action.START"
        const val ACTION_STOP = "com.example.dyondza.action.STOP"
        const val ACTION_CANCEL = "com.example.dyondza.action.CANCEL"

        const val EXTRA_TOPIC = "com.example.dyondza.extra.TOPIC"
        const val EXTRA_COUNTDOWN_DURATION = "com.example.dyondza.extra.COUNTDOWN_DURATION"

        // Estado do fluxo reativo compartilhado globalmente com a UI (Thread-safe)
        private val _sessionState = MutableStateFlow(FocusSessionState())
        val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

        /**
         * Verifica se o usuário concedeu permissão de acesso ao UsageStatsManager.
         */
        fun isUsageAccessGranted(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        /**
         * Método auxiliar para simular uma detecção de abertura de app proibido na UI
         * (ótimo para testes e apresentações em ambiente de emulador).
         */
        fun triggerSimulatedWarning(context: Context, appPackage: String) {
            val currentState = _sessionState.value
            if (!currentState.isActive) return

            val updatedWarnings = currentState.warningsCount + 1
            if (updatedWarnings >= 3) {
                // Cancela por infração
                val finalState = currentState.copy(
                    secondsElapsed = 0L,
                    warningsCount = updatedWarnings,
                    lastForegroundApp = appPackage,
                    statusMessage = "Cancelado: Distração por $appPackage excedeu o limite!"
                )
                _sessionState.value = finalState
                
                // Manda fechar
                val stopIntent = Intent(context, FocusTrackerService::class.java).apply {
                    action = ACTION_CANCEL
                }
                context.startService(stopIntent)
            } else {
                _sessionState.value = currentState.copy(
                    warningsCount = updatedWarnings,
                    lastForegroundApp = appPackage,
                    statusMessage = "Aviso $updatedWarnings/3: Evite abrir o $appPackage!"
                )
            }
        }
    }
}
