package com.example.bag.bag

enum class BagTier(
    val id: String,
    val rows: Int,
) {
    SMALL("small", 1),
    MEDIUM("medium", 2),
    LARGE("large", 3),
    ;

    companion object {
        fun fromId(id: String?): BagTier? = entries.firstOrNull { it.id == id }
    }
}
