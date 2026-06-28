package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade para armazenar as escolas registradas localmente.
 * Evita redundâncias limpando o nome e garante uma lista de sugestões limpa para outros estudantes.
 */
@Entity(tableName = "registered_schools")
data class School(
    @PrimaryKey val name: String,
    val dateRegistered: Long = System.currentTimeMillis()
)
