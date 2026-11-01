package com.example.bag.service

import java.util.UUID

/**
 * 记录每名玩家上一次同步时的“解锁行数”。
 *
 * 这是判断“是否发生降级”的唯一状态来源：
 * - 上一次行数大于当前行数 → 需要丢弃新锁定行的物品；
 * - 没有记录（首次加入）→ 不把当前状态当成降级，避免误丢物品。
 *
 * 只保存运行期状态，不持久化；玩家退出和插件停用时清理。
 * 所有访问都在服务端主线程。
 */
class CapacityTracker {
    private val rowsByPlayer = HashMap<UUID, Int>()

    /** 返回上次记录；首次见到的玩家返回 `null`。 */
    fun get(playerId: UUID): Int? = rowsByPlayer[playerId]

    /** 更新玩家当前行数。 */
    fun set(
        playerId: UUID,
        rows: Int,
    ) {
        rowsByPlayer[playerId] = rows
    }

    /** 玩家退出/死亡时移除，避免下次使用过期容量。 */
    fun remove(playerId: UUID) {
        rowsByPlayer.remove(playerId)
    }

    /** 插件停用时清空。 */
    fun clear() {
        rowsByPlayer.clear()
    }
}
