package com.example.bag.service

import java.util.UUID

class CapacityTracker {
    private val rowsByPlayer = HashMap<UUID, Int>()

    fun get(playerId: UUID): Int? = rowsByPlayer[playerId]

    fun set(
        playerId: UUID,
        rows: Int,
    ) {
        rowsByPlayer[playerId] = rows
    }

    fun remove(playerId: UUID) {
        rowsByPlayer.remove(playerId)
    }

    fun clear() {
        rowsByPlayer.clear()
    }
}
