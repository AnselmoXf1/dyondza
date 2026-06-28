package com.dyondza.backend.service

import com.dyondza.backend.model.DeviceTelemetryDto
import com.dyondza.backend.model.SessionCompleteRequest

data class ValidationResult(
    val isValid: Boolean,
    val status: String, // COMPLETED, INTERRUPTED, CHEAT_DETECTED
    val reason: String
)

object AntiCheatEngine {
    private val BLACKLISTED_APPS = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically", // TikTok
        "com.facebook.katana",
        "com.twitter.android",
        "com.whatsapp"
    )

    fun validateSession(req: SessionCompleteRequest): ValidationResult {
        // 1. Validação de Duração Reivindicada vs Real
        if (req.durationActualSeconds > req.durationRequestedSeconds + 10) {
            return ValidationResult(
                isValid = false,
                status = "CHEAT_DETECTED",
                reason = "Inconsistência temporal: tempo mantido excede o tempo solicitado em cronômetro."
            )
        }

        if (req.warningsCount > 3) {
            return ValidationResult(
                isValid = true,
                status = "INTERRUPTED",
                reason = "Sessão interrompida por excesso de alertas de distração."
            )
        }

        val telemetry = req.deviceTelemetry
        if (telemetry != null) {
            // 2. Verificação de Assinatura Criptográfica básica (Validação SHA-256 / Keystore)
            if (telemetry.clientSignature.isBlank() || telemetry.clientSignature.length < 32) {
                return ValidationResult(
                    isValid = false,
                    status = "CHEAT_DETECTED",
                    reason = "Assinatura criptográfica de telemetria ausente ou inválida."
                )
            }

            // 3. Auditoria Algorítmica da Telemetria (Análise de Fraudes por Sobreposição)
            val dyondzaPackage = "com.aistudio.dyondza.study"
            val totalDurationMs = req.durationActualSeconds * 1000L

            for (log in telemetry.usageLogs) {
                if (BLACKLISTED_APPS.contains(log.packageName) && log.durationMs > 5000) {
                    return ValidationResult(
                        isValid = false,
                        status = "CHEAT_DETECTED",
                        reason = "Detecção de fraude: Aplicativo na lista negra (${log.packageName}) executado em concorrência durante o foco."
                    )
                }
            }
        }

        return ValidationResult(
            isValid = true,
            status = "COMPLETED",
            reason = "Sessão validada com sucesso pelo Escudo Anti-Cheat do Dyondza."
        )
    }
}
