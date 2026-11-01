# 第三方组件与资源许可说明

本文件说明本仓库中不由 GPL-3.0 覆盖的内容，以及第三方组件和资源的来源与许可。它是项目维护说明，不构成法律意见。

## 1. 本仓库的许可范围

- **GPL-3.0**：Kotlin 源码、Gradle 构建脚本、`config.yml`/`plugin.yml` 等配置文件、项目文档。完整文本见 [LICENSE](LICENSE)。
- **不自动适用 GPL-3.0**：模型、贴图、图标、预览图和其他美术资源，按下文单独说明。
- 如果你再分发本仓库或构建产物，请保留本文件、`LICENSE` 以及相关资源的署名信息。

## 2. 模型与图标

### 第一代模型

- 来源：项目模型作者提供。
- 许可：CC BY-NC-SA（Creative Commons Attribution-NonCommercial-ShareAlike）。
- 再分发条件：保留作者署名、不得用于商业用途、修改后的作品需以相同许可共享。
- 具体许可版本和署名信息尚待作者补充；在补充前，请以作者书面说明为准。

### 第二代模型与当前图标/贴图

- 范围包括当前仓库中的：
  - `craftengine/bag/blueprint/bag/*.bbmodel`
  - `craftengine/bag/resourcepack/assets/bag/textures/`
  - `craftengine/bag/resourcepack/assets/bag/models/`
  - `craftengine/bag/resourcepack/assets/bag/equipment/`
- 来源：项目模型作者提供。
- 许可：**待定**。在作者明确授权前，作者保留所有权利，不授予复制、修改、再分发或商业使用许可。
- 这不影响你在本地构建和运行插件；但不要把上述资源单独提取、重新打包或用于其他项目。

### 参考图

- `game_bag.png` 是需求沟通用的参考图，未单独声明许可，不属于 GPL-3.0 范围。
- 不要把该图片作为插件宣传素材或再分发，除非获得权利人许可。

## 3. 运行时与构建依赖

| 组件 | 用途 | 许可 | 是否随发布 JAR 分发 |
|---|---|---|---|
| [Paper API](https://papermc.io/) 26.3 | 编译期服务端 API | GPL-3.0 | 否，`compileOnly` |
| [Kotlin stdlib](https://kotlinlang.org/) 2.4.10 | Kotlin 运行时 | Apache-2.0 | 是，`dist/bag.jar` 内置 |
| [Adventure / MiniMessage](https://docs.advntr.dev/) 5.2.0 | 文本与消息 | MIT | 否，Paper 运行时提供 |
| [CraftEngine](https://github.com/Xiao-MoMi/craft-engine) 26.9.2 | 可选自定义物品/3D armor | GPL-3.0 | 否，软依赖 |
| [Gradle Wrapper](https://gradle.org/) 9.8.1 | 构建工具 | Apache-2.0 | 仓库包含 Wrapper |
| [JUnit 5](https://junit.org/junit5/) 5.14.4 | 单元测试 | Eclipse Public License 2.0 | 否，仅测试 |

## 4. CraftEngine 生成资源

`craftengine/bag/resourcepack/` 中包含通过 CraftEngine 3D armor 流程生成的装备模型、贴图和 shader overlay。这些资源的使用和再分发应同时遵守 CraftEngine 的 GPL-3.0 许可及上游项目要求。

CraftEngine 是可选依赖；未安装时插件回退原版物品，不会加载上述资源。

## 5. 发布检查清单

发布 GitHub Release、整合包或服务器资源包前，请确认：

1. 第一代模型的署名和 CC BY-NC-SA 版本已补充。
2. 第二代模型和图标已获得作者明确授权，或从发布包中排除。
3. `dist/bag.jar` 中保留了 Kotlin stdlib 的 Apache-2.0 许可信息。
4. 发布说明链接了 `LICENSE` 和本文件。
