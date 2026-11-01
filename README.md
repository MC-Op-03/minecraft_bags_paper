# 一个Minecraft服务器插件项目, 用以创建背包系统

## 用户对这个插件应实现内容的叙述

这个插件应该实现以下内容:

设计一个背包类，这个背包类应包括以下属性和方法:

- 背包的实体模型(后续的实体模型兼容CraftEngine，并像鞘翅一样装备在胸甲栏位上)；
- 对物品栏的限制逻辑；

## 用户对这个插件的一个对象的描述

用户不想让服务器中游玩的玩家获得过大的背包物品栏空间(如`game_bag.png`中所示)，需要一个背包功能限制，限制如下:

- 不装备背包时，背包内没有物品栏(0x9)。
- 装备小背包时，背包内保留一行物品栏(1x9)。
- 装备中背包时，背包内保留两行物品栏(2x9)。
- 装备中背包时，背包内物品栏全部解锁(3x9)。

此外，在用户卸下背包或更换背包时的逻辑为：

- 如果更换为更大背包时，解锁栏位；
- 如果更换为更小背包或卸下背包时，背包内物品被丢弃到地上；

## 实现状态

稳定基线版本：`v0.1.0`（Git 标签）。当前开发版本为 `v0.2.0`，已实现：

- 快捷栏 0–8 始终可用；存储区 9–35 按背包等级从上往下解锁：小背包 27–35，中背包 18–35，大背包 9–35。
- 锁定槽位使用带 PDC 标记的灰色玻璃板作为视觉占位，并拦截点击、拖动、拾取与 Shift 放入。
- 胸甲栏装备变化时立即同步；另有每 20 tick 的兜底校正。
- 降级或卸下时，新锁定行中的物品掉落在玩家脚下。
- 三个等级是三个独立 CE 物品；无 CE 时回退原版胸甲物品。
- 使用用户提供的 3D 模型与 16×16 正面图标，通过 CraftEngine 原生 3D armor 渲染。
- 中背包移速 `-15%`、大背包 `-30%`，小背包/无背包无惩罚；使用插件管理的临时属性修饰符，卸下、死亡、断线和停用都会清理。
- 不使用数据库，死亡、断线与跨维度沿用原版玩家物品栏持久化。

后续需求请写入 `additional/`，不要直接改写本版本的历史说明。

## 目标环境

- Minecraft / Paper：26.3
- Java：25（Eclipse Temurin）
- Kotlin：2.4.10
- Gradle：9.8.1（项目自带 Wrapper）
- 测试：JUnit 5（Jupiter 5.14.4）

## 构建与测试

在项目根目录 `projects/bag` 下执行：

```text
gradlew.bat clean build
gradlew.bat test
gradlew.bat dist
tools\dev\lint.ps1 -ProjectName bag
```

构建产物：

- `build/libs/bag-<version>.jar`：普通开发 JAR。
- `dist/bag-<version>.jar`：轻量发布 JAR，保留 `plugin.yml` 的 `libraries`，首次加载需要联网下载 Kotlin stdlib。
- `dist/bag.jar`：自包含发布 JAR，内置 Kotlin stdlib，`plugin.yml` 不含 `libraries`，适合直接分发。
- `dist/SHA256SUMS.txt`：两个 JAR 的 SHA256。

`dist/` 是构建产物目录，不提交 Git。

## 安装与验证

1. 将 `dist/bag.jar` 复制到 Paper 26.3 服务端的 `plugins/` 目录。
2. 启动服务器，确认日志中没有 `Bag` 相关异常。
3. 使用 `/bag give <player> <small|medium|large>` 获取背包。
4. 将背包放入胸甲栏，确认存储区按等级解锁，锁定槽位显示占位玻璃板。
5. 在高层槽位放入物品后换成更小背包或卸下，确认超出物品掉落在脚下。
6. 重新登录，确认背包与物品栏内容按原版持久化保留。

一键构建、部署并重启本地测试服：

```text
tools\dev\dev.ps1 -ProjectName bag
```

## 配置

`config.yml` 主要配置项：

- `tiers.small|medium|large`：三个等级的材质、CustomModelData、名称与 Lore。三个等级必须存在。
- `placeholder`：锁定槽位占位物品的材质、名称与 Lore。
- `craftengine`：CE 集成开关与三个 CE 物品 ID。
- `debuffs.medium-speed` / `debuffs.large-speed`：移速属性修正，默认 `-0.15` / `-0.30`，允许范围 `-1.0..0.0`。
- `messages`：锁定槽位、容量缩小、空间不足提示，使用 MiniMessage。

行数固定在代码中（1/2/3），不从配置读取。`/bag reload` 只重载外观与消息，不改变已有背包的等级。

## 命令与权限

```text
/bag give <player> <small|medium|large>
/bag info [player]
/bag reload
```

权限：`bag.admin`，默认仅 OP。

## 已知限制

- 锁定槽位使用真实物品占位；其他插件查看玩家物品栏时会看到占位玻璃板。
- 其他插件或命令绕过事件直接写入锁定槽位时，依靠兜底同步移动或掉落，最多约 1 秒延迟。
- 背包占用胸甲栏，不能同时穿戴胸甲。
- 新中/大模型不含身前肩带；按用户确认保留模型原样。
- 小包斜挎带由零厚度平面组成，CE 转换时部分零面积面会被排除，实际效果以游戏内为准。
- 服务器不开放下界与末地，跨维度只在主世界范围内验证。
- 包名 `com.example.bag` 为占位值。

## 后续需求

`additional/` 用于保存 `v0.1.0` 之后的追加需求、设计草案和验收说明。命名建议 `YYYY-MM-DD-主题.md`，并保留需求原文与验收条件。

## 设计文档

- 实现方案与验收清单：`docs/implementation-plan.md`
- 代码接口、配置参数与修改方法：`INTERFACES.md`
- 版本检查点：Git 标签 `v0.1.0`
