# bag 实现方案与脚手架计划

- 日期：2026-10-24
- 状态：`v0.1.0` 已实施并经用户验收，作为当前基线冻结；后续需求写入 `additional/`
- 目标环境：Paper 26.3 / Java 25 / Kotlin 2.4.10 / Gradle Wrapper 9.8.1
- 参考：`projects/bag/README.md`、`game_bag.png`（原生物品栏被扩展的示意）

## 1. 需求解读

背包不是独立容器，而是“限制原生玩家物品栏可用槽位”的装备：

- 背包物品装备在胸甲栏，装备/卸下方式与原版盔甲一致。
- 原生物品栏的 3 行存储区（27 格）按背包等级解锁。
- 快捷栏 9 格、盔甲栏、副手栏不受背包限制。
- 玩家死亡、断线、跨维度时沿用原版玩家物品栏持久化，不另建数据库。
- 死亡时背包物品与其中物品按原版规则掉落。
- 后续背包模型由 CraftEngine 提供，容量逻辑不依赖模型实现。

`game_bag.png` 展示的是被扩展到 5 行以上的原生物品栏；本插件的目标是把可用的存储行限制为 0/1/2/3 行。

## 2. 容量模型（核心不变量）

| 背包 | 等级 | 可用存储行 | 可用存储槽位 | 锁定槽位 |
|---|---|---|---|---|
| 无 | 0 | 0x9 | 无 | 9–35 |
| 小背包 | 1 | 1x9 | 9–17 | 18–35 |
| 中背包 | 2 | 2x9 | 9–26 | 27–35 |
| 大背包 | 3 | 3x9 | 9–35 | 无 |

- 快捷栏 `0–8` 永远可用。
- 存储行从下往上解锁（靠近快捷栏的一行先解锁）。
- 背包等级由胸甲栏物品决定；不是“打开某个界面”才生效，限制始终存在。
- 容量变化只影响存储区，不移动快捷栏物品。

## 3. 物品与识别

### 3.1 三个独立物品

小/中/大背包是三个独立 `ItemStack`，不是同一物品的 NBT 等级变体。每个物品带固定的持久化数据：

```text
PDC: bag:tier = "small" | "medium" | "large"
```

容量等级由 `bag:tier` 映射，不读取显示名称、Lore 或模型，避免玩家改名绕过限制。

### 3.2 第一版外观

第一版不依赖 CraftEngine：

- 物品材质与模型由 `config.yml` 定义，默认使用三种原版胸甲材质（如皮革/锁链/铁）并设置不同 `custom-model-data`，便于测试时区分。
- 物品设置为不可破坏、最大堆叠 1；是否清除护甲属性在实施时按实际效果决定并记录。
- 显示名称与 Lore 使用 MiniMessage。

CraftEngine 接入时只替换“识别/生成”层：

```text
BagItemResolver
├── PersistentDataBagResolver   // 第一版：读取 bag:tier
└── CraftEngineBagResolver      // 后续：读取 CraftEngine 物品 ID 并映射等级
```

容量服务只接收 `BagTier?`，不感知物品来自原版还是 CraftEngine。

### 3.3 配置结构（草案）

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

messages:
  locked-slot: "<red>该物品栏尚未解锁"
  capacity-shrunk: "<yellow>背包容量变小，超出的物品已掉落在脚下"
  no-space: "<red>背包空间不足"
```

- 三个等级固定存在，缺失或非法配置在启动时明确报错。
- 行数由代码固定（1/2/3），不放进配置，避免配置失误改变业务规则。
- 重载配置只更新外观与消息，不改变已有物品的等级映射。

## 4. 运行期逻辑

### 4.1 容量计算

```text
rows(chestplate) = PDC bag:tier -> 1 | 2 | 3，否则 0
locked = 9 + rows*9 .. 35
allowed = 0..8 + 9 .. 9 + rows*9 - 1
```

每次事件都实时读取胸甲物品，不依赖可能过期的缓存。

### 4.2 装备变化

主监听：`EntityEquipmentChangedEvent`

- 只处理 `Player` 且变化包含 `EquipmentSlot.CHEST`。
- 事件触发时装备已经变更，读取新胸甲计算容量。
- 使用每名玩家的“上次容量”跟踪器：

```text
oldRows = tracker[player] ?: newRows
if (newRows < oldRows)
    丢弃 9 + newRows*9 .. 9 + oldRows*9 - 1 中的物品
tracker[player] = newRows
```

效果：

- 换成更大背包：解锁，不移动物品。
- 换成更小背包：只丢弃“新锁定行”的物品。
- 卸下背包/换成普通胸甲：丢弃全部存储区物品，快捷栏保留。
- 从普通胸甲换成背包：不丢任何物品。

补充兜底：

- 加入、重生（延迟 1 tick）、切换世界（延迟 1 tick）时重新同步。
- 每 20 tick 对所有在线玩家做一次全量校正；交互事件后再延迟 1 tick 校正一次。
- 插件启动时同步已在线玩家；插件关闭时不丢弃任何物品。

### 4.3 交互拦截

| 事件 | 处理 |
|---|---|
| `InventoryClickEvent` 点击锁定槽位 | 取消 |
| `InventoryClickEvent` 数字键/副手交换/丢弃等作用于锁定槽位 | 取消（点击槽位锁定即可覆盖） |
| `InventoryClickEvent` 从容器 Shift 点击进玩家物品栏 | 模拟可用槽位，放不下则取消；放得下则允许，随后 1 tick 校正 |
| `InventoryDragEvent` 任一目标槽位为锁定槽位 | 取消整个拖动 |
| `PlayerAttemptPickupItemEvent` | 模拟可用槽位，放不下则取消拾取 |
| 其他插件/命令直接写入锁定槽位 | 兜底校正：优先移动到可用槽位，无空间才掉落 |

判定“玩家物品栏槽位”时使用 `event.clickedInventory === player.inventory` + `event.slot`，避免容器界面的 raw slot 偏移问题；拖动使用 `InventoryView.getInventory(raw)` / `convertSlot(raw)` 并在联调时验证。

点击锁定槽位时发送一次带冷却的提示，避免刷屏。

### 4.4 视觉占位与掉落策略

锁定槽位第一版使用带 `bag:placeholder` PDC 标记的灰色玻璃板占位：

- 占位物品最大堆叠 1，所有点击、拖动、拾取、Shift 放入与丢弃操作拦截。
- 占位物品阻止原版把物品放入锁定槽位，作为事件拦截之外的第二层保护。
- 容量增加时移除新解锁槽位的占位；容量减少时先掉落真实物品，再补占位。
- 断线、死亡、插件停用时清理占位；加入、重生、启动时重新同步。
- 死亡掉落列表中的占位物品会被移除；意外掉落的占位物品在生成时被取消。
- 降级/卸下导致的溢出：必须掉落，不自动搬回可用槽位（符合需求）。
- 外部写入/异常残留在锁定槽位的物品：先尝试放入可用槽位；仍放不下时再掉落，避免无必要损失。
- 掉落位置：玩家脚下，使用 `world.dropItemNaturally`。
- 丢弃前先清空槽位，避免复制；掉落失败时记录 warning 并保留在槽位，等待下次兜底。
- 所有物品栏操作只在主线程执行。

### 4.5 生命周期

- `onEnable`：加载/校验配置、注册监听器与命令、启动兜底任务、同步在线玩家。
- `onDisable`：取消任务、清空跟踪器；不执行掉落、不修改玩家物品栏。
- 断线/跨维度：不拦截原版持久化；仅重新同步容量。
- 死亡：不额外处理，沿用原版掉落；若服务器 `keepInventory` 开启，则按原版保留，行为差异写入文档。

## 5. 命令与权限（用于测试和运维）

```text
/bag give <player> <small|medium|large>   给予一个对应背包
/bag info [player]                        查看当前容量与锁定行
/bag reload                               重载外观与消息配置
```

- 权限：`bag.admin`，默认仅 OP。
- 参数校验：玩家在线、等级合法、配置存在；物品栏满时掉落在玩家脚下。
- 不提供“打开背包”命令，因为界面始终是原生物品栏。

## 6. 包与文件结构

```text
projects/bag/
├── AGENTS.md
├── README.md                    保留现有需求，补充实现/构建/验证说明
├── .gitignore
├── .editorconfig
├── settings.gradle.kts
├── build.gradle.kts
├── gradlew / gradlew.bat
├── gradle/wrapper/
├── additional/                  后续需求入口
├── dist/                        发布产物（不提交 Git）
├── docs/
│   └── implementation-plan.md
└── src/
    ├── main/kotlin/com/example/bag/
    │   ├── BagPlugin.kt
    │   ├── bag/
    │   │   ├── BagTier.kt
    │   │   ├── BagItemFactory.kt
    │   │   └── BagItemResolver.kt
    │   ├── config/
    │   │   ├── BagConfig.kt
    │   │   └── ConfigLoader.kt
    │   ├── service/
    │   │   ├── BagCapacityService.kt     // 容量、槽位范围、降级丢弃
    │   │   ├── InventoryFitService.kt    // 拾取/Shift 点击可放置模拟
    │   │   └── CapacityTracker.kt
    │   ├── listener/
    │   │   ├── EquipmentListener.kt
    │   │   ├── InventoryGuardListener.kt
    │   │   └── LifecycleListener.kt
    │   └── command/
    │       └── BagCommand.kt
    ├── main/resources/
    │   ├── plugin.yml
    │   └── config.yml
    ├── release/resources/
    │   └── plugin.yml            自包含发布 JAR 使用（无 libraries）
    └── test/kotlin/com/example/bag/
        ├── BagTierTest.kt
        ├── StorageLayoutTest.kt
        └── DropRangeTest.kt
```

说明：

- 第一版不引入数据库、GUI 框架、ProtocolLib、MockBukkit 或其他重量级依赖。
- MiniMessage 以 `compileOnly` 引入并依赖 Paper 运行时自带版本，首次联调时验证；若不可用，退回 Adventure `Component`/`LegacyComponentSerializer`。
- 包名 `com.example.bag` 为占位值，正式发布前需替换。

## 7. 脚手架步骤

1. 从 `projects/demo-plugin` 复制 Gradle Wrapper（`gradlew`、`gradlew.bat`、`gradle/wrapper/`）。
2. 新建 `settings.gradle.kts`、`build.gradle.kts`（Paper API、JUnit 5、MiniMessage compileOnly、Java 25 toolchain）。
3. 新建 `.gitignore`、`.editorconfig`、项目 `AGENTS.md`。
4. 保留并扩展现有 `README.md` 的需求段落，补充构建、安装、配置、验证和已知限制。
5. 实现纯逻辑层（等级、槽位范围、掉落范围）并先写单元测试。
6. 实现配置加载、物品生成/识别、容量服务、监听器、命令。
7. `plugin.yml` 声明主类、`api-version: '26.3'`、`libraries` 中的 `kotlin-stdlib` 与权限。
8. 执行 `gradlew.bat clean build`、`gradlew.bat test`、`tools\dev\lint.ps1`。
9. 检查 JAR 内含 `plugin.yml` 与 `config.yml`。
10. 为 `tools/dev/dev.ps1` 增加项目参数（默认仍为 `demo-plugin`），使其可构建并部署 `bag`；或新增 `tools/dev/bag-dev.ps1`，避免复制逻辑。
11. 仅在用户授权后重启测试服进行联调；不修改世界与运行数据。
12. 是否 `git init`、是否创建远程仓库，单独确认。

## 8. 测试与验收

### 8.1 自动化

- 单元测试：等级→行数、可用/锁定槽位边界、降级时受影响槽位范围、配置非法值。
- 构建：`gradlew.bat clean build`。
- 格式：`tools\dev\lint.ps1`。
- JAR 内容检查。

### 8.2 服务器集成清单（需授权重启）

1. `/bag give` 获取三个等级，确认物品可装备到胸甲栏。
2. 无背包：存储区 9–35 无法点击、拖动、Shift 放入、拾取进入。
3. 小背包：9–17 可用，18–35 锁定。
4. 中/大背包：逐级解锁。
5. 大→中→小→卸下：高层物品按预期掉落在脚下，低层与快捷栏保留。
6. 从普通胸甲换背包：不丢物品。
7. 背包满时拾取/从箱子 Shift 点击：不进入锁定槽位。
8. 重新登录、死亡、重生：物品与容量符合原版行为。
9. 配置重载：已有背包仍按原等级工作。
10. 观察日志无异常、无物品复制/丢失。

跨维度测试暂不执行：服务器不开放下界/末地。

## 9. 风险与限制

- 第一版用真实物品占位，其他插件读取玩家物品栏时会看到占位玻璃板；占位物品在断线、死亡、停用时清理，但服务器崩溃后可能残留，加入时会自动校正。
- 其他插件或命令可绕过事件直接写入物品栏；仅靠兜底任务校正，极端情况下会有最多约 1 秒的短暂残留。
- `EntityEquipmentChangedEvent` 对 CraftEngine 自定义物品的覆盖需在接入 CE 后验证；兜底同步必须保留。
- 死亡行为受服务器 `keepInventory` 设置影响，需要按服务器实际配置验收。
- 背包占用胸甲栏，不能同时穿胸甲；这是需求本身的结果。
- 第一版外观不是最终模型；CraftEngine 模型、装备动画与音效不在本阶段范围。
- 锁定槽位中的物品被兜底移动/掉落属于异常路径，需在日志中记录，便于排查。

## 10. 待确认项

1. 快捷栏 9 格是否始终可用（本方案假设是）。
2. 存储行是否从下往上解锁（本方案假设是）。
3. 锁定槽位第一版不做视觉占位（本方案假设是）。
4. 第一版背包用三种原版胸甲材质 + CustomModelData 是否可接受。
5. 包名是否先用 `com.example.bag` 占位。
6. 是否允许改造 `tools/dev/dev.ps1` 以支持 `bag` 项目。
7. 是否初始化 `projects/bag` 的本地 Git 仓库。
8. 联调时是否允许重启测试服。

### 10.1 用户的确认

1. 0-8号快捷栏始终可用。
2. 存储行从下向上解锁。
3. 锁定槽位第一版制作一个视觉占位方法。
4. 可以先使用原版材质等待后续做成CraftEngine物品。
5. 包名先使用你说的方式进行占位。
6. 等待插件制作完成后放置到本地测试服务器。
7. 初始化仓库，等后续再commit。
8. 允许重启测试服务器。

## 11. 实施结果

1. 第 10 节已确认；视觉占位方案已实现。
2. 脚手架、纯逻辑、配置、物品、容量、监听与命令已完成。
3. 构建、13 项单元测试与 ktlint 已通过。
4. 已部署到本地测试服，`Bag v0.1.0` 正常加载；用户确认满足当前需求。
5. 详细游戏内验收清单未逐项留档，最终以用户验收结论为准。
6. 测试服已关闭；`v0.1.0` 由 Git 提交与标签固定。
7. 后续需求进入 `additional/`。
