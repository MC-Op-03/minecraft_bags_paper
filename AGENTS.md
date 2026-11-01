# AGENTS.md — bag

本文件细化工作区根目录 `AGENTS.md` 的通用规则。若与根规则冲突，先指出冲突再确认。

## 项目目标

实现一个 Paper 服务端背包插件：背包作为胸甲栏装备，按等级限制原生物品栏的存储行，并在降级或卸下时把超出容量的物品掉落到地上。

## 版本与需求边界

- 稳定基线版本：`v0.1.0`，由 Git 标签固定；当前开发版本为 `v0.2.0`。
- `additional/` 保存后续需求、设计草案和验收说明；每个需求一个文件，建议 `YYYY-MM-DD-主题.md`。
- 后续需求不得静默改写 `v0.1.0` 的历史行为说明；涉及兼容性变化时，在需求文档中写明迁移与回滚方式。

## 当前业务规则

- 快捷栏 0–8 始终可用。
- 存储行 9–35 按背包等级解锁：小背包 1 行（27–35）、中背包 2 行（18–35）、大背包 3 行（9–35）。
- 存储行从上往下解锁。
- 未装备背包时，9–35 全部锁定。
- 锁定槽位放置带 `bag:placeholder` 标记的灰色玻璃板作为视觉占位，并拦截所有取放操作。
- 从大背包换成小背包或卸下时，新锁定行中的真实物品掉落在玩家脚下；快捷栏物品不受影响。
- 死亡、断线、跨维度沿用原版物品栏持久化；服务器不开放下界与末地。
- 中背包提供 `-15%` 移速、大背包 `-30%` 移速；小背包无惩罚。惩罚只依据胸甲栏背包等级。
- 新模型与 16×16 图标由用户提供；模型保持原样，不再调整背带。
- 背包通过 CraftEngine 物品/3D armor 渲染；无 CE 时回退原版物品。

## 技术栈

- 语言：Kotlin 2.4.10
- 平台：Paper 26.3（`paper-api` 作为 `compileOnly` 依赖）
- Java：25（Gradle toolchain 固定）
- 构建：Gradle Kotlin DSL + Gradle Wrapper 9.8.1
- 测试：JUnit 5（Jupiter）
- 文本：Adventure API / MiniMessage（Paper 运行时提供 MiniMessage 5.2.0）

## 包与结构

```text
src/main/kotlin/com/example/bag/
├── BagPlugin.kt              插件入口、任务与重载
├── bag/                      等级、槽位布局、物品生成与识别
├── config/                   配置模型与校验
├── service/                  容量同步、物品迁移、拾取模拟
├── listener/                 装备、物品栏拦截、生命周期
└── command/                  /bag 命令
src/main/resources/           plugin.yml、config.yml
src/release/resources/        自包含发布 JAR 使用的 plugin.yml（无 libraries）
src/test/kotlin/              纯逻辑单元测试
additional/                   后续需求与设计草案
docs/                         设计与计划
```

- 包名 `com.example.bag` 为占位值，正式发布前替换。
- 不引入数据库、GUI 框架、ProtocolLib 或 MockBukkit。

## 关键实现约束

- 所有物品栏修改必须在主线程执行。
- 占位物品的 PDC 标记是唯一识别依据，不得依赖材质或显示名称。
- 占位物品不得进入玩家可保存的物品栏状态：断线、死亡、插件停用时必须清理；启动、加入、重生时重新同步。
- 降级/卸下导致的溢出必须掉落；其他异常残留在锁定槽位的物品优先移动到可用槽位，放不下才掉落。
- 移速惩罚使用独立的 transient `AttributeModifier` key；同一玩家只允许一个，死亡、断线、停用必须清理。
- 不在 `onDisable` 中执行会丢失玩家物品的操作。
- 修改容量规则时必须同步更新 `StorageLayout`、测试、README 与设计文档。

## 代码接口与参数文档

- 修改公开接口、配置字段、CE 物品 ID、模型/图标路径或移速数值时，必须同步更新 `INTERFACES.md`。
- 源码为后续开发者保留类/方法级 KDoc 和关键路径注释；新增代码应延续该风格，注释解释原因和约束，而不是逐行复述。

## 命令、权限与配置

- `/bag give <player> <small|medium|large>`：给予背包。
- `/bag info [player]`：查看当前容量。
- `/bag reload`：重载外观与消息配置，不改变已有物品等级。
- 权限：`bag.admin`，默认仅 OP。
- 配置中的三个等级必须存在；行数固定在代码中（1/2/3），不从配置读取。
- `debuffs.medium-speed` / `debuffs.large-speed` 默认 `-0.15` / `-0.30`，必须位于 `-1.0..0.0`。

## 构建与验证

在项目根目录执行：

```text
gradlew.bat clean build
gradlew.bat test
gradlew.bat dist
tools\dev\lint.ps1 -ProjectName bag
```

完成后确认：

- `build/libs/bag-<version>.jar` 内含 `plugin.yml` 与 `config.yml`。
- `dist/bag-<version>.jar` 是轻量开发发布包，`plugin.yml` 含 `libraries`。
- `dist/bag.jar` 是自包含发布包，`plugin.yml` 不含 `libraries`，并包含 Kotlin stdlib 类。
- `dist/SHA256SUMS.txt` 与 JAR 实际哈希一致。
- `dist/` 不提交 Git。

## 部署与联调

- 构建、部署、重启测试服：`tools\dev\dev.ps1 -ProjectName bag`
- 只部署不重启：`tools\dev\dev.ps1 -ProjectName bag -NoRestart`
- 测试服：`F:\minecraft\servers\demo-26.3`
- 不手工删除或覆盖服务器世界、配置与运行数据。
- 重启测试服前确认用户已授权；联调后检查日志中的异常与物品复制/丢失。

## 跨 Session 上下文协议

### Session 开始

1. 读取根目录 `README.md`、`AGENTS.md`、`context/CURRENT.md`。
2. 读取本项目 `AGENTS.md` 与 `README.md`。
3. 按需读取 `TOOLS.md`、`context/DECISIONS.md`、最新交接记录。
4. 先核验实际状态，再修改。

### Session 结束

1. 更新 `F:\minecraft\context\CURRENT.md`。
2. 在 `F:\minecraft\context\sessions\` 新增交接记录。
3. 只记录必要结果、验证日期、路径与未解决问题。
4. 不自动提交或推送；不记录 RCON 密码、令牌或代理凭据。
