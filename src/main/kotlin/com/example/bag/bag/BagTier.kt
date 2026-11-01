package com.example.bag.bag

/**
 * 背包等级及其解锁的存储行数。
 *
 * [id] 是写入物品 PDC `bag:tier` 的稳定标识，也是 `config.yml` 与
 * CraftEngine pack 使用的键；发布后不要随意修改，否则旧物品会失去识别能力。
 *
 * [rows] 表示该等级解锁的存储行数。槽位范围的计算不在本枚举中完成，
 * 而是集中在 [StorageLayout]，避免多个地方各自维护一套槽位规则。
 */
enum class BagTier(
    val id: String,
    val rows: Int,
) {
    SMALL("small", 1),
    MEDIUM("medium", 2),
    LARGE("large", 3),
    ;

    companion object {
        /**
         * 把 PDC / 配置中的字符串转换为等级。
         *
         * @param id 小写等级 ID，例如 `small`
         * @return 匹配的等级；`null`、空字符串或未知 ID 返回 `null`
         */
        fun fromId(id: String?): BagTier? = entries.firstOrNull { it.id == id }
    }
}
