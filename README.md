# Bag — Paper 26.3 背包限制插件

把背包装备在胸甲栏，用原生物品栏作为存储空间：不同等级的背包解锁不同的存储行，降级或卸下时超出容量的物品掉落到地上。

Paper 26.3 · Java 25 · Kotlin 2.4.10 · GPL-3.0

> 当前开发版本：`0.2.0-dev`（`feature/craftengine-models`）
>
> 稳定基线：`v0.1.0`

## 功能特性

- 三档背包：小背包 1 行、中背包 2 行、大背包 3 行存储空间。
- 快捷栏 `0–8` 始终可用；存储行 `9–35` 从上往下解锁。
- 锁定槽位显示占位玻璃板，并拦截点击、拖动、拾取和 Shift 放入。
- 换成更小背包或卸下时，新锁定行中的物品掉落在玩家脚下；快捷栏物品不受影响。
- 背包可作为胸甲装备，占用胸甲栏，不能同时穿戴胸甲。
- 中背包提供 `-15%` 移速，大背包提供 `-30%` 移速；小背包和无背包无惩罚。
- 可选 CraftEngine 集成：3D armor 模型、16×16 图标和工作台配方。
- 未安装 CraftEngine 时自动回退原版胸甲物品，容量、占位、掉落和移速逻辑仍然工作。
- 不使用数据库；死亡、断线和跨维度沿用原版玩家物品栏持久化。

## 容量规则

| 装备 | 解锁存储行 | 可用存储槽 | 锁定槽 |
|---|---:|---|---|
| 无背包 | 0 | 无 | `9–35` |
| 小背包 | 1 | `27–35` | `9–26` |
| 中背包 | 2 | `18–35` | `9–17` |
| 大背包 | 3 | `9–35` | 无 |

行数固定在代码中（1/2/3），不从配置文件读取。

## 环境要求

| 项 | 要求 |
|---|---|
| 服务端 | Paper 26.3 |
| Java | 25 |
| Kotlin | 2.4.10 |
| 构建 | Gradle Wrapper 9.8.1 |
| 文本 | Adventure API / MiniMessage |
| 可选内容框架 | CraftEngine 26.9.2 Community Edition |

## 安装

### 从源码构建

需要 JDK 25 和 Git；不需要单独安装 Gradle。

```bash
git clone https://github.com/St0Rm-Leon/minecraft_bags_paper.git
cd minecraft_bags_paper
git switch feature/craftengine-models
```

Windows：

```text
gradlew.bat clean build dist
```

Linux / macOS：

```bash
./gradlew clean build dist
```

`build` 会执行单元测试。构建产物：

| 文件 | 说明 |
|---|---|
| `build/libs/bag-0.2.0.jar` | 开发 JAR |
| `dist/bag-0.2.0.jar` | 轻量发布 JAR，`plugin.yml` 保留 `libraries`，首次加载需要联网下载 Kotlin stdlib |
| `dist/bag.jar` | 自包含发布 JAR，内置 Kotlin stdlib，适合直接分发 |
| `dist/SHA256SUMS.txt` | 两个发布 JAR 的 SHA256 |

`dist/` 是生成目录，不提交 Git；需要发布时在本地构建。

### 安装插件

1. 将 `dist/bag.jar` 复制到 Paper 服务端的 `plugins/` 目录。
2. 启动服务器，确认日志中没有 `Bag` 相关异常。
3. 执行 `/bag give <player> <small|medium|large>` 获取背包。
4. 将背包放入胸甲栏，检查存储区是否按等级解锁。

### 安装 CraftEngine 资源包（可选）

如果服务器安装了 CraftEngine 26.9.2，可将本仓库的 `craftengine/bag` 复制到：

```text
<server>/plugins/CraftEngine/resources/bag
```

然后执行 `/ce reload all`，并让客户端接受 CraftEngine 生成的资源包。这样会启用 3D armor 模型、16×16 图标和背包配方。

没有 CraftEngine 时可以跳过这一步；插件会使用原版物品，不影响容量逻辑。

## 快速开始

```text
/bag give <player> small
/bag info
```

拿到背包后放入胸甲栏：

- 小背包只解锁最上面一行存储槽。
- 中背包解锁上面两行。
- 大背包解锁全部三行。
- 在高层槽位放入物品后换成更小的背包，超出容量的物品会掉落在脚下。

## 命令与权限

```text
/bag give <player> <small|medium|large>
/bag info [player]
/bag reload
```

| 命令 | 说明 |
|---|---|
| `/bag give` | 给予指定等级的背包；物品栏满时掉落在脚下 |
| `/bag info` | 查看当前背包等级、可用槽位、锁定槽位和移速修正 |
| `/bag reload` | 重载外观与消息配置，不改变已有背包的等级 |

权限：`bag.admin`，默认仅 OP。

## 配置

主要配置位于 `src/main/resources/config.yml`：

| 配置段 | 用途 |
|---|---|
| `tiers` | 三个等级的材质、CustomModelData、名称和 Lore |
| `placeholder` | 锁定槽位占位物的材质、名称和 Lore |
| `craftengine` | CraftEngine 开关和三个 CE 物品 ID |
| `debuffs` | 中/大背包的移速修正，默认 `-0.15` / `-0.30` |
| `messages` | 锁定槽位、容量缩小和空间不足提示，使用 MiniMessage |

三个等级必须全部存在；`debuffs` 的值必须在 `-1.0..0.0` 范围内。修改配置后执行 `/bag reload`；CraftEngine 配置本身需要 `/ce reload all`。

完整的配置字段、代码接口和修改步骤见 [INTERFACES.md](INTERFACES.md)。

## 工作原理

1. 插件读取玩家胸甲栏物品，通过 CraftEngine 物品 ID 或原版 PDC `bag:tier` 判断背包等级。
2. `StorageLayout` 根据等级计算允许使用的快捷栏和存储槽位。
3. `BagCapacityService` 在装备变化、加入、重生、切世界和交互后同步容量。
4. 锁定槽位放置带 `bag:placeholder` 标记的玻璃板；容量减少时先处理真实物品，再补充占位。
5. `SpeedModifierService` 使用 `Attribute.MOVEMENT_SPEED` 和 transient `bag:movement_speed` modifier 管理移速惩罚。
6. 每 20 tick 执行一次兜底校正，处理其他插件绕过事件写入锁定槽位的情况。

所有物品栏修改都在主线程执行。占位物品不会保存到玩家数据，断线、死亡和插件停用时会被清理。

## CraftEngine 集成

CraftEngine 是可选依赖。启用后提供三个独立物品：

| 等级 | CE 物品 ID |
|---|---|
| 小 | `bag:small_bag` |
| 中 | `bag:medium_bag` |
| 大 | `bag:large_bag` |

CE pack 包含：

- `items.yml`：物品定义、图标模型和 3D armor 资源引用。
- `recipes.yml`：8 皮革 + 箱子/小包/中包的工作台配方。
- `blueprint/bag/*.bbmodel`：模型源文件。
- `resourcepack/`：图标、装备贴图、模型 JSON 和 CE shader overlay。

移速惩罚由插件管理，不要同时在 CE 的 `attribute_modifiers` 中重复添加，否则可能双倍生效。

模型、图标和配方细节见 [INTERFACES.md](INTERFACES.md) 第 4 节。

## 项目结构

```text
src/main/kotlin/com/example/bag/
├── BagPlugin.kt              插件入口、任务与重载
├── bag/                      等级、槽位布局、物品生成与识别
├── config/                   配置模型与校验
├── service/                  容量同步、物品迁移、拾取模拟、移速
├── listener/                 装备、物品栏拦截、生命周期
└── command/                  /bag 命令
src/main/resources/           plugin.yml、config.yml
src/release/resources/        自包含发布 JAR 使用的 plugin.yml
src/test/kotlin/              纯逻辑单元测试
craftengine/bag/              CraftEngine 物品、配方、模型与资源
docs/                         实现方案与验收记录
additional/                   后续需求与设计草案
```

## 开发与测试

```text
gradlew.bat clean build
gradlew.bat test
gradlew.bat dist
```

当前测试覆盖容量布局、掉落范围、配置校验和移速修正。测试不依赖真实 Minecraft 服务端。

实现方案和阶段记录见 [docs/implementation-plan.md](docs/implementation-plan.md)。

## 已知限制

- 背包占用胸甲栏，不能同时穿戴胸甲。
- 锁定槽位使用真实物品占位；其他插件读取玩家物品栏时会看到占位玻璃板。
- 其他插件或命令绕过事件直接写入锁定槽位时，依赖兜底同步，最多约 1 秒延迟。
- 当前中/大背包模型不含身前肩带；模型按作者交付版本原样使用。
- 小背包斜挎带由零厚度平面组成，CE 转换时部分零面积面会被排除，实际效果以游戏内为准。
- 下界与末地的跨维度行为尚未验证；测试服只在主世界范围内验证。
- 包名 `com.example.bag` 仍为占位值，正式发布前可能调整。
- 当前没有 GitHub Release 附件；请从源码构建。

## 后续计划

- 完成 `v0.2.0` 游戏内验收并打标签。
- 增加 GitHub Actions 构建与测试。
- 正式发布前替换占位包名。
- 明确第二代模型和图标的再分发许可。

后续需求先写入 [additional/](additional/)，确认后再修改实现。不要静默改写 `v0.1.0` 的历史行为说明。

## 贡献与问题反馈

- 问题反馈：<https://github.com/St0Rm-Leon/minecraft_bags_paper/issues>
- 当前开发分支：`feature/craftengine-models`
- 提交前请至少运行 `gradlew.bat clean build`，并确认测试通过。

## 许可证

- Kotlin 源码、Gradle 构建脚本、配置文件和项目文档：GPL-3.0，见 [LICENSE](LICENSE)。
- 第一代模型：可按 CC BY-NC-SA 再分发；再分发时需保留署名、非商业使用、相同方式共享。具体版本与署名信息待作者补充。
- 第二代模型与当前仓库中的图标/贴图：许可待定，作者保留权利；在明确授权前不授予额外再分发许可。
- `game_bag.png` 是需求参考图，未单独声明许可，不属于 GPL-3.0 范围。
- 第三方组件和资源说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 致谢

- [Paper](https://papermc.io/) 与 [Adventure](https://docs.advntr.dev/) 提供插件平台和文本 API。
- [CraftEngine](https://github.com/Xiao-MoMi/craft-engine) 提供可选的自定义物品和 3D armor 支持。
- 背包模型与 16×16 图标由项目模型作者提供。
