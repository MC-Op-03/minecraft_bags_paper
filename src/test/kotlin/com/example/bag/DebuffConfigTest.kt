package com.example.bag

import com.example.bag.bag.BagTier
import com.example.bag.config.DebuffConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 验证“等级 -> 移速修正值”的纯映射。
 *
 * 实际 AttributeModifier 的添加/移除属于 Bukkit 运行期行为，在服务器联调中验证；
 * 这里保证配置文件解析后的数值映射不会改变小包规则。
 */
class DebuffConfigTest {
    private val debuffs = DebuffConfig(mediumSpeed = -0.15, largeSpeed = -0.30)

    @Test
    fun `small and no bag have no speed penalty`() {
        assertEquals(0.0, debuffs.speedFor(BagTier.SMALL))
        assertEquals(0.0, debuffs.speedFor(null))
    }

    @Test
    fun `medium and large use their configured penalties`() {
        assertEquals(-0.15, debuffs.speedFor(BagTier.MEDIUM))
        assertEquals(-0.30, debuffs.speedFor(BagTier.LARGE))
    }
}
