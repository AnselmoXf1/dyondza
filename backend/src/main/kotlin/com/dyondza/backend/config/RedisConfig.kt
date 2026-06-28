package com.dyondza.backend.config

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisConfig {
    private var jedisPool: JedisPool? = null
    private val inMemoryZSet = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, Double>>()
    private val inMemoryKV = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun init() {
        val host = System.getenv("REDIS_HOST") ?: "localhost"
        val port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379
        try {
            val poolConfig = JedisPoolConfig().apply {
                maxTotal = 20
                maxIdle = 10
                minIdle = 2
            }
            val pool = JedisPool(poolConfig, host, port, 2000)
            // Test connection
            pool.resource.use { it.ping() }
            jedisPool = pool
            println("Conectado ao Redis em $host:$port com sucesso.")
        } catch (e: Exception) {
            println("Aviso: Não foi possível conectar ao Redis em $host:$port (${e.message}). Ativando fallback em memória ZSET/KV para desenvolvimento.")
            jedisPool = null
        }
    }

    fun zadd(key: String, score: Double, member: String) {
        val pool = jedisPool
        if (pool != null) {
            try {
                pool.resource.use { it.zadd(key, score, member) }
                return
            } catch (e: Exception) {
                // fallback
            }
        }
        val set = inMemoryZSet.computeIfAbsent(key) { java.util.concurrent.ConcurrentHashMap() }
        set[member] = score
    }

    fun zrevrangeWithScores(key: String, start: Long, stop: Long): List<Pair<String, Double>> {
        val pool = jedisPool
        if (pool != null) {
            try {
                pool.resource.use { jedis ->
                    val res = jedis.zrevrangeWithScores(key, start, stop)
                    return res.map { Pair(it.element, it.score) }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        val set = inMemoryZSet[key] ?: return emptyList()
        return set.entries
            .sortedByDescending { it.value }
            .drop(start.toInt())
            .take((stop - start + 1).toInt())
            .map { Pair(it.key, it.value) }
    }

    fun setex(key: String, seconds: Long, value: String) {
        val pool = jedisPool
        if (pool != null) {
            try {
                pool.resource.use { it.setex(key, seconds, value) }
                return
            } catch (e: Exception) {
                // fallback
            }
        }
        inMemoryKV[key] = value
    }

    fun get(key: String): String? {
        val pool = jedisPool
        if (pool != null) {
            try {
                pool.resource.use { return it.get(key) }
            } catch (e: Exception) {
                // fallback
            }
        }
        return inMemoryKV[key]
    }
}
