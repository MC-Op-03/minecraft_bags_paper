# bag 接口与参数说明

本文件面向后续开发者，说明：

- 代码中的扩展点与调用约定；
- `config.yml` 参数的定义、默认值和修改方式；
- CraftEngine pack 的物品、模型、图标与配方接口；
- 修改容量、移速、消息和物品时还需要同步修改什么。

运行时权威依据仍然是源码、`config.yml`、CraftEngine pack 和实际构建结果。本文档与代码冲突时，以代码为准。

## 1. 运行环境

| 项 | 值 |
|---|---|
| 平台 | Paper 26.3 |
| Java | 25 |
| Kotlin | 2.4.10 |
| Gradle Wrapper | 9.8.1 |
| 文本 | Adventure / MiniMessage |
| 可选内容框架 | CraftEngine 26.9.2 Community Edition |

CraftEngine 是可选依赖。没有 CE 时，插件回退原版背包物品；容量、占位槽、掉落和移速逻辑仍然工作。

## 2. 代码接口

### 2.1 `BagItemProvider`

路径：`src/main/kotlin/com/example/bag/bag/BagItemProvider.kt`

```kotlin
interface BagItemProvider {
    fun createBag(tier: BagTier): ItemStack?
    fun tierOf(item: ItemStack?): BagTier?
}
```

约定：

- 所有方法在主线程调用。
- `createBag` 返回 `null` 表示当前 provider 无法创建该等级；调用方会回退到原版 provider。
- `tierOf` 必须是只读操作，不能修改物品，也不能产生副作用。
- `tierOf` 返回 `null` 表示该物品不是本 provider 的背包，或者物品无效。

### 2.2 `BagItemService`

路径：`src/main/kotlin/com/example/bag/bag/BagItemService.kt`

职责：

- 优先调用 CraftEngine provider；
- CE 物品不存在、构建失败或读取失败时回退原版 provider；
- 保存当前可选的 CE provider 引用，`/bag reload` 后可以替换。

公开方法：

| 方法 | 说明 |
|---|---|
| `createBag(tier)` | 优先 CE，失败回退原版 |
| `tierOf(item)` | 优先 CE 自定义物品 ID，未命中再读原版 `bag:tier` PDC |
| `setCraftEngine(provider)` | 由 `BagPlugin` 在启用/重载时调用 |
| `usingCraftEngine` | 当前是否存在 CE provider |

扩展新内容来源时，实现 `BagItemProvider`，在 `BagPlugin` 的 provider 装配处加入，不要让业务服务直接依赖具体插件 API。

### 2.3 `BagTier`

路径：`src/main/kotlin/com/example/bag/bag/BagTier.kt`

```kotlin
enum class BagTier(val id: String, val rows: Int) {
    SMALL("small", 1),
    MEDIUM("medium", 2),
    LARGE("large", 3),
}
```

- `id` 是稳定标识，会写入物品 PDC `bag:tier`，也会用于 `config.yml` 的 `tiers` 和 `craftengine.items` 键。
- `rows` 是解锁的存储行数，必须与 `StorageLayout` 的公式一致。
- 已发布后不要随意修改 `id`，否则旧物品会无法识别。
- 当前设计固定 3 个等级、最多 3 行；新增等级需要同时修改 `StorageLayout.MAX_ROWS`、测试、配置和 CE pack。

### 2.4 `StorageLayout`

路径：`src/main/kotlin/com/example/bag/bag/StorageLayout.kt`

槽位约定：

| 区域 | 槽位 |
|---|---|
| 快捷栏 | `0..8`，始终可用 |
| 存储区 | `9..35`，共 3 行 |
| 第 1 行（最上面） | `27..35` |
| 第 2 行 | `18..26` |
| 第 3 行（最下面） | `9..17` |

当前解锁方式是**从上往下**：

| 背包 | rows | 可用存储槽 | 锁定槽 |
|---|---:|---|---|
| 无 | 0 | 无 | `9..35` |
| 小 | 1 | `27..35` | `9..26` |
| 中 | 2 | `18..35` | `9..17` |
| 大 | 3 | `9..35` | 无 |

公开方法：

| 方法 | 返回值 | 说明 |
|---|---|---|
| `lockedSlots(rows)` | `IntRange` | 当前等级锁定的存储槽；无锁定时为 `IntRange.EMPTY` |
| `allowedStorageSlots(rows)` | `IntRange` | 当前等级可用的存储区 |
| `allowedSlots(rows)` | `List<Int>` | 快捷栏 + 可用存储区，用于放置/拾取模拟 |
| `isLocked(slot, rows)` | `Boolean` | 槽位是否锁定 |
| `newlyLockedSlots(oldRows, newRows)` | `IntRange` | 降级时“新变成锁定”的槽位范围 |

修改解锁方向时，必须同时修改这三个公式及 `StorageLayoutTest`、`DropRangeTest`、README、实现计划和 CE 文档。不要在监听器中硬编码槽位范围。

### 2.5 `BagCapacityService`

路径：`src/main/kotlin/com/example/bag/service/BagCapacityService.kt`

职责：

- 读取胸甲栏 tier；
- 计算新旧行数；
- 降级/卸下时丢弃新锁定槽位物品；
- 修复锁定槽位中的异常物品；
- 放置占位玻璃板；
- 更新移速修饰符。

公开方法：

| 方法 | 说明 |
|---|---|
| `rowsOf(player)` | 只读返回当前解锁行数 |
| `syncInitial(player)` | 初始化 tracker 后执行一次同步，不因“未知旧容量”误丢物品 |
| `sync(player)` | 完整同步容量、占位、掉落和移速 |
| `clearPlaceholders(player)` | 清理玩家物品栏中的占位物品 |
| `give(player, item)` | 只向可用槽位放置物品；放不下则掉落在脚下 |

线程：所有方法必须在主线程调用。

不变量：

- 锁定存储槽要么为空，要么放有带 `bag:placeholder` 的占位物品。
- 占位物品不进入死亡掉落、不会保存到玩家数据。
- 降级导致的物品必须掉落，不自动搬回可用槽位。
- 外部异常写入锁定槽的物品优先移动到可用槽位，放不下才掉落。

### 2.6 `InventoryFitService`

路径：`src/main/kotlin/com/example/bag/service/InventoryFitService.kt`

用于在事件触发前模拟“物品是否能放入允许槽位”：

- `wouldFit(inventory, item, rows)`：只考虑快捷栏和已解锁存储区，先尝试堆叠，再尝试空格。
- `addToAllowed(inventory, item, rows)`：实际写入允许槽位，返回未放下的剩余物品（`null` 表示全部放下）。

这个服务不会考虑锁定槽位的占位物品；调用方负责保证锁定槽不参与放置。

### 2.7 `SpeedModifierService`

路径：`src/main/kotlin/com/example/bag/service/SpeedModifierService.kt`

- Attribute：`Attribute.MOVEMENT_SPEED`
- Key：`bag:movement_speed`
- Operation：`AttributeModifier.Operation.ADD_SCALAR`
- 使用 `addTransientModifier`，不写入玩家持久化数据。
- 中背包 `-0.15`，大背包 `-0.30`，小背包/无背包为 `0.0`。
- `update(player, tier)` 会移除同 key 旧 modifier 后按当前 tier 重新添加。
- `clear(player)` 在死亡、断线、停用或切回无惩罚时调用。

注意：`ADD_SCALAR -0.15` 是基础速度的加法倍率；在没有其他速度修正时等价于最终移速 `-15%`。

### 2.8 配置模型

路径：`src/main/kotlin/com/example/bag/config/BagConfig.kt`

```kotlin
BagConfig(
    tiers: Map<BagTier, BagTierAppearance>,
    placeholder: PlaceholderAppearance,
    messages: Messages,
    craftEngine: CraftEngineConfig,
    debuffs: DebuffConfig,
)
```

`DebuffConfig.speedFor(tier)` 是移速值的唯一映射入口；不要在监听器中重复写 `-0.15/-0.30`。

`ConfigLoader` 负责：

- 三个 tier 必须存在；
- 材质必须是有效物品；
- `custom-model-data` 必须非负；
- `craftengine.items` 必须符合 `namespace:path` 格式；
- `debuffs` 必须是 `-1.0..0.0` 的有限数；
- MiniMessage 语法错误会拒绝加载。

## 3. `config.yml` 参数

### 3.1 `tiers`

```yaml
tiers:
  small:
    material: LEATHER_CHESTPLATE
    custom-model-data: 1001
    name: "<gray>小背包"
    lore:
      - "<dark_gray>容量：1 行"
  medium: ...
  large: ...
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `material` | 是 | 无 CE 回退物品的 Bukkit 材质；必须可作为物品 |
| `custom-model-data` | 否 | 无 CE 回退物品的 CustomModelData；非负整数 |
| `name` | 否 | MiniMessage 显示名 |
| `lore` | 否 | MiniMessage Lore 列表 |

三个 tier 必须全部存在。修改名称/Lore 后执行 `/bag reload` 即可，不需要重启；已有的原版回退物品不会自动改写，需要重新 `/bag give`。

### 3.2 `placeholder`

```yaml
placeholder:
  material: GRAY_STAINED_GLASS_PANE
  name: "<dark_gray>未解锁"
  lore:
    - "<gray>装备更大的背包以解锁"
```

锁定槽位的视觉占位物品。必须是有物品材质的 Material；占位识别依赖 PDC `bag:placeholder`，不依赖材质。修改后 `/bag reload`，在线玩家的占位会重建。

### 3.3 `craftengine`

```yaml
craftengine:
  enabled: true
  items:
    small: bag:small_bag
    medium: bag:medium_bag
    large: bag:large_bag
```

| 字段 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 是否尝试加载 CE provider；CE 未安装时自动回退 |
| `items.<tier>` | `bag:<tier>_bag` | CE 自定义物品 ID；必须与 CE pack 一致 |

修改 CE 物品 ID 时，必须同时修改：

1. `config.yml` 的 `craftengine.items`；
2. `craftengine/bag/configuration/items.yml` 的物品键；
3. CE pack 的物品模型/装备资源引用；
4. 配方结果或中心材料（如果改了 ID）。

`/bag reload` 会重新读取 ID；CE 配置本身需要 `/ce reload all`。

### 3.4 `debuffs`

```yaml
debuffs:
  medium-speed: -0.15
  large-speed: -0.30
```

| 字段 | 默认 | 允许范围 | 说明 |
|---|---:|---:|---|
| `medium-speed` | `-0.15` | `-1.0..0.0` | 中背包 `MOVEMENT_SPEED` 的 `ADD_SCALAR` 值 |
| `large-speed` | `-0.30` | `-1.0..0.0` | 大背包 `MOVEMENT_SPEED` 的 `ADD_SCALAR` 值 |

修改后执行 `/bag reload`。同时建议更新 CE 物品 Lore（`craftengine/bag/configuration/items.yml`）中的百分比说明，避免显示与实际值不一致。

### 3.5 `messages`

```yaml
messages:
  locked-slot: "<red>该物品栏尚未解锁。"
  capacity-shrunk: "<yellow>背包容量变小，超出的物品已掉落在脚下。"
  no-space: "<red>背包空间不足。"
```

全部使用 MiniMessage。修改后 `/bag reload`。MiniMessage 语法错误会导致重载失败并保留旧配置。

## 4. CraftEngine pack 接口

路径：`projects/bag/craftengine/bag/`

### 4.1 物品 ID

| 等级 | CE 物品 ID | 3D 装备资源 | 2D fallback |
|---|---|---|---|
| 小 | `bag:small_bag` | `bag:small_bag_chest_piece` | `bag:small_bag_chest_piece_2d` |
| 中 | `bag:medium_bag` | `bag:medium_bag_chest_piece` | `bag:medium_bag_chest_piece_2d` |
| 大 | `bag:large_bag` | `bag:large_bag_chest_piece` | `bag:large_bag_chest_piece_2d` |

物品定义在 `configuration/items.yml`：

- `material: leather_chestplate`
- `data.item_model: bag:<tier>_bag`：16×16 正面图标模型
- `data.equippable.asset_id`：CE 3D armor 资源
- `unbreakable: true`
- `components.minecraft:max_stack_size: 1`
- `remove_components: minecraft:attribute_modifiers`：不让原版胸甲提供护甲属性

移速惩罚由插件管理，**不要**在 CE 的 `data.attribute_modifiers` 或 `settings.attribute_modifiers` 中重复添加，否则可能双倍生效。

### 4.2 图标

- 路径：`craftengine/bag/resourcepack/assets/bag/textures/item/<tier>_bag_icon.png`
- 当前为 16×16 透明 PNG。
- 模型定义：`resourcepack/assets/bag/models/item/<tier>_bag.json`
- item model definition：`resourcepack/assets/bag/items/<tier>_bag.json`

替换图标时保持文件名和路径不变即可；如果修改尺寸，确认模型 JSON 仍能正确采样。

### 4.3 3D 模型

源文件：`craftengine/bag/blueprint/bag/<tier>_bag.bbmodel`

生成资源：

- 3D：`resourcepack/assets/bag/equipment/<tier>_bag_chest_piece.json`
- 2D：`resourcepack/assets/bag/equipment/<tier>_bag_chest_piece_2d.json`
- 贴图：`resourcepack/assets/bag/textures/entity/equipment/humanoid/*.png`
- shader overlays：`resourcepack/armor3d_shader_overlay_*`

模型需要保留 CE 3D armor 转换所需的 `humanoid` 根组和 body part 结构。模型源文件由用户提供，插件代码不读取 `.bbmodel`；它是 CE 资源生成输入。

修改模型后：

1. 更新 `blueprint/bag/*.bbmodel`；
2. 用 CE 3D armor 转换流程重新生成 equipment 资源；
3. 清理旧 equipment/humanoid 贴图，避免残留；
4. `/ce reload all`；
5. 玩家重新接受资源包。

### 4.4 配方

`configuration/recipes.yml` 使用 CE `shaped` 配方：

- 小：8 皮革 + 箱子
- 中：8 皮革 + 小背包
- 大：8 皮革 + 中背包

配方结果 ID 必须与 `items.yml` 的物品 ID 一致。修改配方后执行 `/ce reload all` 或 `/ce reload recipe`。

## 5. PDC 与识别协议

| Key | 类型 | 用途 |
|---|---|---|
| `bag:tier` | String | 原版回退背包等级：`small` / `medium` / `large` |
| `bag:placeholder` | Byte | 标记锁定槽占位物品 |
| `bag:movement_speed` | Attribute key | 插件管理的移速 modifier |

- 不要改用显示名称、Lore、材质或模型来判断等级。
- CE 物品优先通过 `CraftEngineItems.getCustomItemId` 识别；原版回退物品通过 `bag:tier`。
- 占位物品识别只读 PDC，不依赖材质。

## 6. 命令与权限

```text
/bag give <player> <small|medium|large>
/bag info [player]
/bag reload
```

- 权限：`bag.admin`，默认仅 OP。
- `give` 使用 `BagItemService`，有 CE 时给 CE 物品，无 CE 时给原版回退物品。
- `info` 显示 tier、行数、可用/锁定槽和移速修正。
- `reload` 重载配置并重建在线玩家的占位与移速；CE pack 使用 `/ce reload all`。

## 7. 修改指南

### 修改容量方向或行数

1. 修改 `BagTier.rows`（若只是等级行数变化）。
2. 修改 `StorageLayout` 中的解锁/锁定公式；`MAX_ROWS` 仅在所有等级总数变化时调整。
3. 更新 `StorageLayoutTest`、`DropRangeTest`。
4. 更新 README、实现计划、CE 物品 Lore。
5. 旧玩家登录时，新的锁定槽会优先移动物品到可用槽，放不下才掉落。

### 修改移速

1. 改 `config.yml` 的 `debuffs`。
2. 改 `craftengine/bag/configuration/items.yml` 的 Lore 百分比。
3. `/bag reload`；必要时 `/ce reload all`。
4. 验证中/大/小/无背包四种状态，避免残留 modifier。

### 替换物品材质或图标

1. 改 `tiers` 的 `material` / `custom-model-data`（仅无 CE 回退）。
2. 改 CE pack 的 `items.yml` 和图标 PNG。
3. CE 环境下执行 `/ce reload all`。
4. 已存在的物品不会被改写；需要重新发放。

### 新增等级

当前 `StorageLayout.MAX_ROWS = 3` 与 3 行存储区绑定。新增第 4 个等级需要：

- 修改 `BagTier`、`StorageLayout` 和全部槽位测试；
- 确认原生物品栏是否仍有空间（当前 9–35 已全部使用）；
- 新增 CE 物品、equipment、图标、配方和 Lore；
- 更新所有文档。

不要只增加 `BagTier` 枚举而不更新布局和测试。

### 修改包名

1. 替换 `group`、源码包路径、`plugin.yml` 的 `main`；
2. 更新所有 `package` 与 import；
3. 更新 Gradle 构建；
4. 重新构建、测试、部署；
5. 旧包名插件不能与当前插件同时加载。

## 8. 线程与生命周期

- 所有 Bukkit/Paper 物品栏、玩家属性、掉落和占位操作必须在主线程。
- 异步路径不得直接修改 `PlayerInventory` 或 `Player` 属性。
- 占位物品必须在断线、死亡、插件停用时清理；加入、重生、启动时重建。
- 移速 modifier 是 transient，不写入玩家数据；断线/死亡/停用必须清理。
- 每 20 tick 的兜底任务用于处理其他插件绕过事件写入的物品。

## 9. 构建与验证

```text
gradlew.bat clean build
gradlew.bat test
gradlew.bat dist
tools\dev\lint.ps1 -ProjectName bag
```

构建产物：

- `build/libs/bag-0.2.0.jar`：开发 JAR
- `dist/bag-0.2.0.jar`：轻量发布 JAR（`libraries`）
- `dist/bag.jar`：自包含发布 JAR（内置 Kotlin stdlib）
- `dist/SHA256SUMS.txt`

部署到本地测试服：

```text
tools\dev\dev.ps1 -ProjectName bag
```

测试服要求：Paper 26.3、Java 25、可选 CraftEngine 26.9.2。CE pack 需要放入 `servers/demo-26.3/plugins/CraftEngine/resources/bag` 并执行 `/ce reload all`。

## 10. 已知约束

- 当前模型由用户提供，保持原样；中/大模型没有身前肩带。
- 小包的斜挎带有零厚度平面，CE 转换会排除部分零面积面。
- 占位物品是真实物品，其他插件读取玩家物品栏时会看到它们。
- 其他插件绕过事件直接写入锁定槽时，最多约有 1 秒兜底延迟。
- 测试服为离线模式，白名单不能防冒名；不得暴露到公网。
