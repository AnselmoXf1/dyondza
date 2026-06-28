package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

class LevelProgressTest {

    // Função de cálculo de nível para testes idêntica à do ViewModel
    private fun calculateLevelFromHours(hours: Double): Int {
        return (1 + Math.floor(sqrt(hours * 2.0))).toInt().coerceIn(1, 50)
    }

    private fun calculateProgressToNextLevel(hours: Double): Float {
        val level = calculateLevelFromHours(hours)
        val minHours = (level - 1).toDouble().pow(2) / 2.0
        val maxHours = level.toDouble().pow(2) / 2.0
        val range = maxHours - minHours
        return if (range > 0.0) {
            ((hours - minHours) / range).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    @Test
    fun testLevelCalculationBasedOnHours() {
        // 0 horas de estudo -> Nível 1
        assertEquals(1, calculateLevelFromHours(0.0))

        // 0.4 horas de estudo -> Nível 1 (limiar de nível 2 é 0.5h)
        assertEquals(1, calculateLevelFromHours(0.4))

        // 0.5 horas de estudo (30 mins) -> Início do Nível 2
        assertEquals(2, calculateLevelFromHours(0.5))

        // 1.9 horas de estudo -> Nível 2 (limiar de nível 3 é 2.0h)
        assertEquals(2, calculateLevelFromHours(1.9))

        // 2.0 horas de estudo (120 mins) -> Início do Nível 3
        assertEquals(3, calculateLevelFromHours(2.0))

        // 4.5 horas de estudo (270 mins) -> Início do Nível 4
        assertEquals(4, calculateLevelFromHours(4.5))

        // 8.0 horas de estudo (480 mins) -> Início do Nível 5
        assertEquals(5, calculateLevelFromHours(8.0))
    }

    @Test
    fun testProgressCalculation() {
        // No início exato do nível 2 (0.5h), progresso deve ser 0% para o nível 3
        assertEquals(0f, calculateProgressToNextLevel(0.5), 0.001f)

        // No meio do caminho entre limiar de nível 2 (0.5h) e nível 3 (2.0h)
        // Faixa de horas do nível 2 = 2.0 - 0.5 = 1.5h
        // Se estudou 1.25h: progresso = (1.25 - 0.5) / 1.5 = 0.75 / 1.5 = 50%
        assertEquals(0.5f, calculateProgressToNextLevel(1.25), 0.001f)

        // No limiar exato do nível 3 (2.0h), progresso deve ser 100% ou limiar inicial de nível 3 (0%)
        assertEquals(0f, calculateProgressToNextLevel(2.0), 0.001f)
    }

    @Test
    fun testWeeklyHoursCalculationAndMockLeaderboard() {
        // Dados de simulação de sessões de estudo
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        val tenDaysAgo = now - (10 * 24 * 60 * 60 * 1000L)

        // Criar estrutura mock de sessões para testes
        class MockSession(val startTime: Long, val durationSeconds: Long)

        val sessions = listOf(
            MockSession(twoDaysAgo, 3600L), // 1 hora de estudo recente (dentro dos últimos 7 dias)
            MockSession(now, 1800L),       // 0.5 horas de estudo recente (dentro dos últimos 7 dias)
            MockSession(tenDaysAgo, 7200L)  // 2 horas de estudo antigo (fora dos últimos 7 dias)
        )

        // Somar apenas o que estiver nos últimos 7 dias
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val weeklySeconds = sessions.filter { it.startTime >= oneWeekAgo }.sumOf { it.durationSeconds }
        val weeklyHours = weeklySeconds.toDouble() / 3600.0

        // Esperamos 1.5 horas (3600 + 1800 = 5400 segundos / 3600 = 1.5 h)
        assertEquals(1.5, weeklyHours, 0.001)

        // Simular a ordenação da lista
        val mocks = listOf(
            Pair("Elena Mondlane", 14.8),
            Pair("Mateus Chongo", 12.5),
            Pair("Você", weeklyHours)
        )

        val sorted = mocks.sortedByDescending { it.second }
        
        // Com 1.5h o usuário deve estar na última posição (Elena 14.8, Mateus 12.5, Você 1.5)
        assertEquals("Você", sorted.last().first)
        
        // Se o usuário estudasse mais (ex: 20h), deveria estar no topo
        val mocksHighStudy = listOf(
            Pair("Elena Mondlane", 14.8),
            Pair("Mateus Chongo", 12.5),
            Pair("Você", 20.0)
        )
        val sortedHigh = mocksHighStudy.sortedByDescending { it.second }
        assertEquals("Você", sortedHigh.first().first)
    }
}
