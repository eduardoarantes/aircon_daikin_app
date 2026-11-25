package com.example.airconapp.domain

import com.example.airconapp.data.Zone
import kotlin.math.pow

/** How often a zone has been toggled, as a score that halves every [ZoneUsage.HALF_LIFE_MILLIS]. */
data class ZoneUsage(val score: Double, val updatedAtMillis: Long) {

    fun scoreAt(nowMillis: Long): Double {
        val elapsed = (nowMillis - updatedAtMillis).coerceAtLeast(0)
        return score * 0.5.pow(elapsed.toDouble() / HALF_LIFE_MILLIS)
    }

    companion object {
        /** Two weeks: a zone used a lot last month gives way to one used this week. */
        const val HALF_LIFE_MILLIS = 14L * 24 * 60 * 60 * 1000

        /** Adds one toggle of [zoneName] at [nowMillis]. */
        fun record(usage: Map<String, ZoneUsage>, zoneName: String, nowMillis: Long): Map<String, ZoneUsage> {
            val previous = usage[zoneName]?.scoreAt(nowMillis) ?: 0.0
            return usage + (zoneName to ZoneUsage(previous + 1, nowMillis))
        }

        /**
         * Indices of the [count] most used zones, most used first. Zones never used keep the
         * device's order, so with no history this is simply the first [count] zones.
         */
        fun mostUsed(zones: List<Zone>, usage: Map<String, ZoneUsage>, nowMillis: Long, count: Int = 2): List<Int> =
            zones.indices
                .sortedByDescending { usage[zones[it].name]?.scoreAt(nowMillis) ?: 0.0 }
                .take(count)

        /** One zone per line: name, score and time, tab separated. Zone names never hold tabs. */
        fun encode(usage: Map<String, ZoneUsage>): String =
            usage.entries.joinToString("\n") { (name, u) -> "$name\t${u.score}\t${u.updatedAtMillis}" }

        fun decode(text: String?): Map<String, ZoneUsage> =
            text.orEmpty().lineSequence().mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 3) return@mapNotNull null
                val score = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val time = parts[2].toLongOrNull() ?: return@mapNotNull null
                parts[0] to ZoneUsage(score, time)
            }.toMap()
    }
}
