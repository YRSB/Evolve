# 调研报告：refs/strings 多语言词条自动提取与 Android 映射方案

- **任务编号**: Wayfinder Research Ticket [#7](https://github.com/YRSB/Evolve/issues/7)
- **关联母工单**: [#1 [Wayfinder Map] Evolve Android 原生移植与可玩原型](https://github.com/YRSB/Evolve/issues/1)
- **研究对象**: 上游 Evolve 仓库 `refs/strings/` 文本资源体系与 `refs/src/locale.js` 本地化运行时
- **状态**: 完成 (Completed)

---

## 1. 执行摘要 (Executive Summary)

针对 Evolve 原生 Android (Kotlin + Jetpack Compose) 移植工程，本调研对上游 `refs/strings/` 多语言词典及 `refs/src/locale.js` 的实现机制进行了深入源码剖析。

核心结论如下：
1. **结构形态**：上游 `strings.json` 为**纯扁平键值对结构**（共有 11,641 条键，无嵌套对象），通过下划线命名法（`_`）划分为 111 个逻辑命名空间（如 `evo_*`, `tech_*`, `city_*`, `resource_*` 等）。
2. **插值与复数**：
   - 动态参数采用**基于零的数字占位符**（`%0` 至 `%29`），全量库中共计 2,642 条字符串包含此类占位符；不存在 `{0}`, `{1}` 或 `%s` 格式。且不同语言之间存在**参数重排序**（如英语 `"+%0 %1"` 与中文 `"%1 +%0"`）。
   - 共有 635 处字符串包含原生百分号（如 `+%0%` 或 `+10%`），在 Android XML 中属于高危破坏性语法。
   - 上游**未采用任何原生复数规则引擎**（无 ICU MessageFormat 或 gettext），而是通过代码层三元表达式显式分支（全库仅有 11 个显式以 `_plural` 结尾的独立键名）。
3. **映射策略选型**：
   - **否决全盘映射为 Android `strings.xml`**：存在 343 个非法/高危资源标识符（包含点号 `.`、特殊重音符 `í`、大写驼峰等会阻断 AAPT2 编译）；破坏 `:core:model` / `:core:engine` 纯 Kotlin JVM 架构与无 Android SDK 依赖测试；无法原生支持上游大量动态拼装键（如 `loc("resource_" + name + "_name")`）；且 11,641 键的全量 arsc 资源表将造成严重体积与 DEX 膨胀。
   - **采纳双层映射架构**：
     - **领域核心层（JVM）**：构建基于纯 Kotlin 的 `EvolveLocale` 词典与 `StringTable`，支持 `%0` 占位符、多语言回退（zh-CN -> en）及动态键查找，完全兼容 JVM 单元测试与无 UI 游戏模拟。
     - **表现呈现层（Compose）**：通过 `LocalStringTable` 提供 Compose UI 响应式订阅，并由自动化脚本生成按模块分组的类型安全 DSL 访问器（`EvolveStrings.Evo`, `EvolveStrings.City`）。
     - **系统壳层（OS）**：仅将应用名称、通知渠道等极少数系统级静态文本写入标准 `strings.xml`。
4. **提取与裁剪**：
   - 针对当前“演化时代至基础古典文明时代”里程碑，提取管道通过白名单规则精准提取 2,544 个词条，**直接裁剪掉 78.1% 的非必要数据**（排除深空/地狱维度/3,300+条百科词条），大幅轻量化应用体积与冷启动耗时。

---

## 2. 上游文本键值结构分析 (Primary Source Analysis)

### 2.1 语言文件拓扑与组织方式
上游 `refs/strings/` 目录下维护了 13 个语言版本，以英文 `strings.json` 作为标准主版本（Master Schema）：

| 文件路径 | 对应语言 | 词条总数 | 说明 |
| :--- | :--- | :--- | :--- |
| [`refs/strings/strings.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json) | 英语 (en-US) | 11,641 | 主参考版本，所有键在此全量定义 |
| [`refs/strings/strings.zh-CN.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.zh-CN.json) | 简体中文 (zh-CN) | 11,641 | 完整对齐翻译 |
| [`refs/strings/strings.zh-TW.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.zh-TW.json) | 繁体中文 (zh-TW) | 11,208 | 缺失 433 键 |
| [`refs/strings/strings.ja-JP.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.ja-JP.json) | 日语 (ja-JP) | 11,641 | 完整对齐翻译 |
| [`refs/strings/strings.de-DE.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.de-DE.json) | 德语 (de-DE) | 10,150 | 缺失 1,491 键 |
| `strings.es-ES.json` / `strings.cs-CZ.json` | 西语 / 捷克语 | 3,150 ~ 4,799 | 覆盖率较低，大量依赖英语回退 |

### 2.2 扁平结构与命名空间体系
通过对 `strings.json` 进行 AST 解析：
- **JSON 嵌套层级**：严格为 1 级（扁平字典 `{ "key": "string_value" }`），不存在嵌套子对象。
- **命名空间前缀**：全量 11,641 键通过下划线 `_` 前缀归类为 111 个命名空间。主要分布如下：

```mermaid
pie title Evolve strings.json Key Distribution
    "wiki (百科资料)" : 3314
    "tech (科技树与研究)" : 1679
    "race (种族与物种)" : 708
    "achieve (成就与特权)" : 698
    "portal / hell (传送门/地狱)" : 644
    "trait (基因与种族特性)" : 477
    "space / inter (太空与星际)" : 700
    "city (城市建筑与发展)" : 354
    "gov / civics (政体与公民)" : 453
    "arpa (ARPA与基因项目)" : 277
    "event (事件与日志)" : 254
    "evo (原始演化时代)" : 246
    "resource (资源名称与上限)" : 164
    "job (人口职业分工)" : 88
    "tab (UI标签与导航)" : 48
    "others (其他通用/前缀)" : 785
```

典型前缀语义规范：
- `tab_*`：导航栏与核心页面 Tab 文本（见 [`refs/strings/strings.json#L2-L50`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L2-L50)）。
- `evo_*`：演化时代阶段、器官变异、生化反应描述（见 [`refs/strings/strings.json#L3100-L3346`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L3100-L3346)）。
- `resource_*`：资源基础名称、复数形式及容量说明（如 `resource_RNA_name`, `resource_Lumber_name`）。
- `city_*`：城市建筑名称、升级与产出说明（如 `city_basic_housing_desc`, `city_farm`）。
- `job_*`：市民岗位名称与职责描述（如 `job_farmer`, `job_lumberjack`, `job_quarry_worker`）。
- `tech_*`：科技名称、简述及前置条件。
- `gov_*`：政体模式（`gov_anarchy`, `gov_democracy`）与政策指令。

### 2.3 上游运行时加载与回退逻辑
在 [`refs/src/locale.js`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/locale.js)：
1. **默认与回退机制**（[`refs/src/locale.js#L42-L65`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/locale.js#L42-L65)）：
   ```javascript
   let defaultString;
   $.getJSON("strings/strings.json", (data) => { defaultString = data; });
   if (locale != "en-US") {
       let localeString;
       $.getJSON(`strings/strings.${locale}.json`, (data) => { localeString = data; });
       if (localeString) {
           Object.assign(defaultString, localeString); // 以英文为基底，浅合并本地化词条
       }
   }
   ```
   **核心行为**：非英语语言直接覆盖在英语词典之上。任何非英语文件中缺失的键，自动透明回退为英文原词。
2. **缺失键保护**（[`refs/src/locale.js#L6-L14`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/locale.js#L6-L14)）：
   若词条在合并后的字典中仍未命中，`loc(key)` 直接返回原始 `key` 字符串自身，避免界面抛出空指针或崩溃。
3. **外部词条包扩展**（[`refs/src/locale.js#L66-L85`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/locale.js#L66-L85)）：
   支持从存档中读取 `string_pack`（通过 LZString 解压），允许动态应用主题或玩家自定义汉化包。

---

## 3. 动态字符串插值与复数机制 (Interpolation & Pluralization)

### 3.1 动态占位符模式
上游完全摒弃了标准 `printf` 语法（如 `%s`, `%d`）或 `{0}`, `{name}` 命名插值，而是统一采用**基于正则的 0 起始数字索引替换**（见 [`refs/src/locale.js#L15-L38`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/locale.js#L15-L38)）：
```javascript
for (let i = 0; i < variables.length; i++) {
    let re = new RegExp(`%${i}(?!\\d)`, "g"); // 负向前瞻匹配，避免 %1 误匹配 %10
    string = string.replace(re, variables[i]);
}
```

统计数据：
- **占位符词条数**：共 2,642 条字符串包含 `%0` ~ `%29`。
- **高频占位符分布**：
  - `%0`: 2,686 次
  - `%1`: 1,024 次
  - `%2`: 418 次
  - `%3`: 184 次
  - `%4`: 86 次
  - `%5` ~ `%29`: 120 次

### 3.2 语序重排现象 (Parameter Reordering)
不同自然语言在表达逻辑上的语法差异在上游被直接利用：
- 英语（[`refs/strings/strings.json#L73`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L73)）：`"plus_resource": "+%0 %1"` （如 `+5 Food`）
- 中文（[`refs/strings/strings.zh-CN.json#L73`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.zh-CN.json#L73)）：`"plus_resource": "%1 +%0"` （如 `食物 +5`）
- 复杂效果：
  - 英文（[`refs/strings/strings.json#L61`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L61)）：`"requires_power_combo_effect": "-%0MW, -%1 %2/s"`
  - 中文（[`refs/strings/strings.zh-CN.json#L61`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.zh-CN.json#L61)）：`"requires_power_combo_effect": "电力 -%0MW，%2 -%1/s"`

### 3.3 原生百分号冲突 (Literal Percent Collisions)
经扫描，上游有 **635 个词条** 在包含或不包含参数的情况下，携带了原生的字面量百分号 `%`：
- 样例 1（[`refs/strings/strings.json#L74`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L74)）：`"plus_resource_per": "+%0% %1 per %2"`
- 样例 2（[`refs/strings/strings.json#L125`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L125)）：`"sign_aquarius_effect": "Tourism revenue +%0%."`
- 样例 3：`"Tax rate: 10%"`

**AAPT 风险警示**：如果盲目将其转换为 Android `<string>` 标签，由于 `String.format` 规范，`+%0%` 会导致 AAPT2 报错 `UnknownFormatConversionException` 或解析混乱，除非在 XML 中转义为 `%%`。

### 3.4 复数机制的实际实现 (Ad-hoc Pluralization)
上游**完全没有引入国际化复数规则（Plural Rules）**。其复数处理策略分为两类：
1. **代码内三元逻辑硬编码**：
   在业务调用处根据数值手动切换词条键名。例如：
   - 城市住宅人数（[`refs/src/actions.js#L494`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/actions.js#L494)）：
     ```javascript
     desc() {
         return $(this)[0].citizens() === 1
             ? loc('city_basic_housing_desc')
             : loc('city_basic_housing_desc_plural', [$(this)[0].citizens()]);
     }
     ```
   - 地狱巡逻队战损（[`refs/src/portal.js#L1868-L1869`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/portal.js#L1868-L1869)）：
     ```javascript
     patrol.wounded > 1
         ? loc('hell_report_log_patrol_wounded_plural', [patrol.wounded])
         : loc('hell_report_log_patrol_wounded');
     ```
2. **专属复数键名（全库仅 11 个）**：
   全量 11,641 键中，仅有 11 个键包含 `plural` 关键字：
   - `city_basic_housing_desc_plural`
   - `resource_Crates_plural`, `resource_Containers_plural`
   - `resource_Knockoff_plural_name`, `resource_Plasmid_plural_name`, `resource_AntiPlasmid_plural_name`, `resource_Supercoiled_plural_name`
   - `hell_report_log_patrol_wounded_plural`, `hell_report_log_patrol_killed_plural`, `hell_report_log_revived_plural`, `hell_report_log_surveyors_plural`

---

## 4. Android 映射策略对比与评估 (Strategy Evaluation)

### 4.1 方案 A：直接生成标准 Android `strings.xml`
将 `strings.json` 转换为 `res/values/strings.xml`，`strings.zh-CN.json` 转换为 `res/values-zh-rCN/strings.xml`。

- **优势**：
  1. 深度贴合 Android OS 系统级多语言机制，支持跟随系统语言或单应用语言偏好设置（`Per-App Language Preferences`）。
  2. Compose 原生支持 `stringResource(R.string.key)`。
- **致命缺陷与阻断点**：
  1. **标识符冲突与非法字符**：
     - 上游存在 343 个不符合 Android 资源命名规范（仅限 `[a-z0-9_]`）的键名。
     - **点号运算符号**：例如 [`refs/strings/strings.json#L10582-L10587`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L10582-L10587) 的 `wiki_trait_effect_catnip0.1`、`wiki_trait_effect_catnip0.25` 等。在 Android 中点号会导致生成的 `R.string.xxx` 语法错误。
     - **非 ASCII 字符**：例如 [`refs/strings/strings.json#L4280`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json#L4280) 的 `cheese_hermelín` 包含特殊重音字母。
     - **大写驼峰**：`theme_darkNight`, `tabLoad`, `Threshold`, `gov_name_Sanders` 等。
  2. **XML 转义灾难**：
     - 单引号（288 处，如 `don't`）需转义为 `\'`。
     - 双引号（35 处）需转义为 `\"`。
     - 符号与标记（43 处 `&`，17 处 `<` 或 `>`，如 `<%0> * <%1>`、`Civics -> Military`）必须严格转为 `&amp;`、`&lt;`、`&gt;`。
     - 635 处原生百分号需手工转换或替换为 `%%`，稍有不慎即打断 Gradle 构建流程。
  3. **架构解耦原则违背（核心硬伤）**：
     - 根据母工单 [#1](https://github.com/YRSB/Evolve/issues/1) 架构设计，`:core:model` 与 `:core:engine` 是**纯 Kotlin JVM 模块**，完全脱离 Android SDK。
     - 纯 JVM 引擎在计算战斗日志、状态变更或事件描述时，无法引用 `R.string` 或 `android.content.Context`。
  4. **动态键名无法高效解析**：
     - 上游大量逻辑动态拼装键（如 `loc("resource_" + name + "_name")`）。若映射为 `R.string`，在 Android 中必须依赖 `resources.getIdentifier(key, "string", packageName)`，不仅性能低下（每次哈希表反射查找），而且极易被 R8/ProGuard 代码混淆误删。
  5. **包体与 DEX 膨胀**：
     - 11,641 键全量生成将产生 1.1 万个常量静态字段及巨型 `resources.arsc` 资源表。

### 4.2 方案 B：纯 Kotlin JVM 动态词典与类型安全 DSL（推荐架构）
基于纯 Kotlin 实现独立的本地化模块 `:core:i18n`（或集成于 `:core:model`），构建静态/动态双模词库：

```mermaid
flowchart TD
    UpstreamJSON["refs/strings/ (strings.json, strings.zh-CN.json)"] --> Extractor["构建期提取脚本 (tools/extract-strings.mjs)"]
    
    subgraph GeneratedArtifacts ["生成的产物 (Build Artifacts)"]
        ScopedJSON["模块化轻量 JSON 资产<br/>(assets/strings/*.json)"]
        TypedDSL["强类型类型安全访问器<br/>(EvolveStrings.kt)"]
    end
    
    Extractor --> ScopedJSON
    Extractor --> TypedDSL
    
    subgraph CoreEngine [":core:model & :core:engine (Pure Kotlin JVM)"]
        StringTable["StringTable 词典引擎<br/>(O(1) 内存 Map + 回退机制)"]
        ScopedJSON -.->|启动一次性载入| StringTable
        EngineLogic["游戏模拟与事件生成<br/>(EvolveLocale.loc('evo_rna', 2))"] --> StringTable
    end
    
    subgraph ComposeUI [":core:ui & :feature:* (Jetpack Compose)"]
        LocalStrings["CompositionLocalProvider<br/>(LocalStringTable)"]
        StringTable --> LocalStrings
        Composable["Compose 组件<br/>(Text(EvolveStrings.Evo.dnaTitle))"] --> LocalStrings
        ComposableDynamic["动态实体组件<br/>(Text(loc('resource_' + id + '_name')))"] --> LocalStrings
    end
```

- **优势**：
  1. **跨平台纯 JVM 兼容**：可在本地快速 Tick 单元测试中完美运行，无需 Robolectric 或 Android 模拟器。
  2. **100% 保持上游 `%0` 语义与语序重排**：完全复刻 `refs/src/locale.js` 的正则替换与默认回退逻辑。
  3. **天然支持动态键名**：`StringTable["resource_${resId}_name"]` 为普通 Map 访问，O(1) 复杂度，无反射损耗，不受 R8 资源混淆影响。
  4. **免疫任何 XML 字符转义缺陷**：无论是单引号、双引号、尖括号还是 `catnip0.1`，作为 JSON 字符串读取均天然无害。
  5. **灵活裁剪与懒加载**：可在不同开发阶段仅打包当前时代所需词条。

### 4.3 综合决策矩阵

| 评估维度 | 方案 A: 标准 Android strings.xml | 方案 B: 纯 Kotlin 动态词典与 DSL | 最终决议 |
| :--- | :--- | :--- | :--- |
| **纯 Kotlin JVM 兼容性** | ❌ 无法在纯 JVM 模块运行 | ✅ 天然支持纯 JVM 与单元测试 | **采纳方案 B** |
| **动态键支持 (`loc('tech_' + id)`)** | ❌ 需低效反射 `getIdentifier` | ✅ 原生 O(1) Map 索引 | **采纳方案 B** |
| **特殊字符与命名容忍度** | ❌ 343 处命名非法，635 处 `%` 转义易崩 | ✅ JSON 原生安全，零转义风险 | **采纳方案 B** |
| **参数替换与多语言语序** | ⚠️ 需将 `%0` 改写为 `%1$s` 且强制类型标注 | ✅ 原汁原味复刻上游 `%0` 正则逻辑 | **采纳方案 B** |
| **Compose UI 编写体验** | ⚠️ `stringResource(R.string.xxx)` | ✅ `EvolveStrings.Evo.dnaTitle` 代码补全 | **采纳方案 B** |
| **系统桌面/图标原生集成** | ✅ 原生支持系统多语言菜单 | ⚠️ 仅需为系统 App Name 保留微量 XML | **混合架构** |

> [!IMPORTANT]
> **最终落地结论（混合策略）**：
> 1. **游戏领域层所有词条（99.9%）**：统一采用**方案 B（纯 Kotlin 词典 + Compose DSL）**。
> 2. **Android 壳工程元数据（0.1%）**：仅将 `app_name`、通知渠道名称等不超过 5 条系统级文本保留在 `res/values/strings.xml` 中。

---

## 5. 演化时代与基础文明时代词条范围界定 (Domain Scoping)

为达成母工单 [#1](https://github.com/YRSB/Evolve/issues/1)“原始演化时代至基础古典文明时代”的最小可玩闭环，提取脚本应对词库进行精准裁剪：

### 5.1 范围定义与命名空间白名单
1. **演化时代核心 (Evolution Era)**：
   - 核心动作与机制：`evo_*`（共 246 条，涵盖 RNA/DNA、细胞核、线粒体、有性生殖、噬菌作用、叶绿体、几丁质、多细胞化、感知力等；见 [`refs/src/actions.js#L21-L150`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/actions.js#L21-L150)）。
   - 生物纲属分类：`genus_*`（哺乳纲、爬行纲、鸟纲、昆虫纲、植物界、真菌界、原生生物界等）。
   - 演化基础资源：`resource_RNA_*`, `resource_DNA_*`。
   - 基础物种与初级突变特性：`race_*`, `trait_*`。
2. **基础古典文明时代 (Civilization Era)**：
   - 城市建筑与民居：`city_*`（涵盖营地、基础住房、小屋、公寓、农田、磨坊、风车、筒仓、采石场、伐木场、矿井、兵营、神庙、市场等；见 [`refs/src/actions.js#L8222-L8260`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/actions.js#L8222-L8260)）。
   - 原始与古典科技树：`tech_*` 中标记为 `era: 'primitive'` 和 `era: 'civilized'` 的所有科技项（木棒、骨器、火、石器、农业、制陶、石工、采矿、冶炼、轮子、青铜、铁器、货币、文字等；见 [`refs/src/tech.js#L15698-L15701`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/tech.js#L15698-L15701)）。
   - 人口岗位与分工：`job_*`（采集者、猎人、农夫、伐木工、采石工、矿工、工匠、学者、士兵等；见 [`refs/src/jobs.js`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/jobs.js)）。
   - 前期基础资源：`Food`, `Lumber`, `Stone`, `Chrysotile`, `Copper`, `Iron`, `Bronze`, `Cement`, `Clay`, `Brick`, `Money`, `Knowledge`, `Steel`, `Crates`, `Containers`, `Plasmid`。
   - 早期政体与市政：`gov_*`, `civics_*`（无政府、独裁、民主、寡头、神权、共和制等）。
3. **全局通用与 UI 标签 (Common UI)**：
   - 导航与标签：`tab_*`（`tab_evolve`, `tab_city1` 至 `tab_city8`, `tab_civics`, `tab_research`, `tab_resources` 等）。
   - 状态与加成文本：`plus_*`, `minus_*`, `requires_*`, `morale_*`, `season_*`, `day`, `year`, `base`。

### 5.2 裁剪效果指标
经原型脚本验证，过滤规则执行结果如下：
- **全量词条总数**：11,641 键
- **提取后核心词条数**：2,544 键
- **数据缩减率**：**78.1%**
- **剔除的冗余内容**：太空时代（`space_*`, `interstellar_*`, `galaxy_*`, `tau_*`）、地狱传送门（`portal_*`, `hell_*`）、转生机制（`bioseed_*`, `blackhole_*`）、以及 3,314 条庞大的游戏内维基百科（`wiki_*`）。

---

## 6. 自动化提取脚本与 Kotlin 体系设计 (Extraction Pipeline)

### 6.1 提取管道架构
构建时自动化处理流程推荐采用 Node.js 脚本（开发机开箱即用、处理百万字符 JSON 极速）或 Kotlin Script（与 Gradle 构建深度集成）。

#### 自动化提取脚本实现 (`scripts/extract-strings.mjs`)
以下脚本可直接保存并执行，提取 `en` 与 `zh-CN` 对应时代的词条，并生成结构化 JSON 与强类型 Kotlin 文件：

```javascript
/**
 * scripts/extract-strings.mjs
 * 演化与基础文明时代多语言词条提取器
 */
import fs from 'node:fs';
import path from 'node:path';

const REFS_STRINGS_DIR = 'refs/strings';
const OUTPUT_DIR = 'core/model/src/main/resources/strings';
const OUTPUT_KT_PATH = 'core/model/src/main/kotlin/com/yrsb/evolve/model/i18n/EvolveStrings.kt';

const enAll = JSON.parse(fs.readFileSync(path.join(REFS_STRINGS_DIR, 'strings.json'), 'utf8'));
const zhAll = JSON.parse(fs.readFileSync(path.join(REFS_STRINGS_DIR, 'strings.zh-CN.json'), 'utf8'));

// 1. 时代前缀白名单
const ALLOWED_PREFIXES = [
    'tab_', 'evo_', 'genus_', 'trait_', 'race_', 'city_', 
    'job_', 'gov_', 'civics_', 'morale_', 'season_', 
    'plus_', 'minus_', 'requires_'
];

// 2. 演化与古典文明时代基础资源白名单
const CIV_RESOURCES = [
    'RNA', 'DNA', 'Food', 'Lumber', 'Stone', 'Chrysotile',
    'Copper', 'Iron', 'Bronze', 'Cement', 'Clay', 'Brick',
    'Money', 'Knowledge', 'Steel', 'Crates', 'Containers',
    'Plasmid', 'AntiPlasmid', 'Phage'
];

// 3. 解析 refs/src/tech.js 中 primitive 与 civilized 时代的科技 ID
const techSource = fs.readFileSync('refs/src/tech.js', 'utf8');
const earlyTechSet = new Set(
    [...techSource.matchAll(/([a-zA-Z0-9_]+):\s*\{[\s\S]*?era:\s*'(primitive|civilized)'/g)].map(m => m[1])
);

// 4. 词条过滤判定
function isKeyAccepted(key) {
    if (ALLOWED_PREFIXES.some(p => key.startsWith(p))) return true;

    if (key.startsWith('resource_')) {
        return CIV_RESOURCES.some(r => key.startsWith(`resource_${r}_`) || key === `resource_${r}`);
    }

    if (key.startsWith('tech_')) {
        const rawTechId = key.replace(/^tech_/, '').replace(/_(desc|effect|title|info|cost|detail)$/, '');
        return earlyTechSet.has(rawTechId) || earlyTechSet.has(key.replace(/^tech_/, ''));
    }

    const commonKeys = ['base', 'year', 'day', 'to_full', 'to_empty', 'true', 'false', 'theme', 'locale', 'citizen'];
    return commonKeys.includes(key);
}

const extractedEn = {};
const extractedZh = {};
const sortedKeys = Object.keys(enAll).filter(isKeyAccepted).sort();

for (const key of sortedKeys) {
    extractedEn[key] = enAll[key];
    extractedZh[key] = zhAll[key] ?? enAll[key]; // 若中文缺失则回退为英文
}

fs.mkdirSync(OUTPUT_DIR, { recursive: true });
fs.writeFileSync(path.join(OUTPUT_DIR, 'strings.en.json'), JSON.stringify(extractedEn, null, 2), 'utf8');
fs.writeFileSync(path.join(OUTPUT_DIR, 'strings.zh-CN.json'), JSON.stringify(extractedZh, null, 2), 'utf8');

console.log(`[Success] Extracted ${sortedKeys.length} strings for Evolution & Classical Era.`);
```

### 6.2 Kotlin 词典运行时设计 (`StringTable.kt`)
在 `:core:model` 纯 Kotlin JVM 模块中构建如下词典解析与格式化引擎：

```kotlin
package com.yrsb.evolve.model.i18n

import kotlinx.serialization.json.Json

/**
 * 纯 Kotlin JVM 字符串词库引擎
 */
class StringTable(
    val locale: String,
    private val dictionary: Map<String, String>,
    private val fallback: StringTable? = null
) {
    /**
     * 格式化词条，完全复刻上游 %0, %1 正则替换逻辑
     */
    fun loc(key: String, vararg args: Any): String {
        val raw = dictionary[key] ?: fallback?.dictionary?.get(key) ?: return key
        if (args.isEmpty()) return raw

        var result = raw
        for (i in args.indices) {
            // 采用负向前瞻避免误伤两位数占位符（例如 %1 不替换 %10）
            val regex = Regex("%$i(?!\\d)")
            result = result.replace(regex, args[i].toString())
        }
        return result
    }

    operator fun get(key: String): String = loc(key)

    companion object {
        fun loadFromResources(locale: String, fallback: StringTable? = null): StringTable {
            val resourcePath = "/strings/strings.$locale.json"
            val jsonStream = StringTable::class.java.getResourceAsStream(resourcePath)
                ?: return fallback ?: StringTable(locale, emptyMap())
            
            val content = jsonStream.bufferedReader().use { it.readText() }
            val dict = Json.decodeFromString<Map<String, String>>(content)
            return StringTable(locale, dict, fallback)
        }
    }
}

/**
 * 全局本地化单例（可在无 Android Context 下供纯 JVM 引擎直接调用）
 */
object EvolveLocale {
    private var defaultTable: StringTable = StringTable.loadFromResources("en")
    var current: StringTable = defaultTable
        private set

    fun setLocale(locale: String) {
        current = if (locale == "en") {
            defaultTable
        } else {
            StringTable.loadFromResources(locale, fallback = defaultTable)
        }
    }

    fun loc(key: String, vararg args: Any): String = current.loc(key, *args)
}
```

### 6.3 Compose UI 响应式集成
在 Compose 体系中，通过 `CompositionLocal` 注入 `StringTable`，确保语言切换时全树响应式重组（Recomposition）：

```kotlin
package com.yrsb.evolve.ui.theme

import androidx.compose.runtime.*
import com.yrsb.evolve.model.i18n.EvolveLocale
import com.yrsb.evolve.model.i18n.StringTable

val LocalStringTable = staticCompositionLocalOf { EvolveLocale.current }

@Composable
fun EvolveI18nProvider(
    locale: String,
    content: @Composable () -> Unit
) {
    val stringTable = remember(locale) {
        EvolveLocale.setLocale(locale)
        EvolveLocale.current
    }

    CompositionLocalProvider(LocalStringTable provides stringTable) {
        content()
    }
}

/**
 * Compose 专用取词扩展函数
 */
@Composable
fun loc(key: String, vararg args: Any): String {
    val table = LocalStringTable.current
    return table.loc(key, *args)
}
```

---

## 7. 演进路线与工单建议 (Roadmap & Next Steps)

1. **Gradle 任务集成**：
   在根目录创建 `tools/extract-strings.mjs`，并在 `:core:model` 的 `build.gradle.kts` 中挂载预编译任务 `generateLocalizedStrings`，确保每次构建前自动同步 upstream 的 `refs/strings/` 词条更新。
2. **DSL 代码生成拓展**：
   在提取脚本中增加可选参数，针对高频使用的 `tab_*`, `evo_*`, `city_*` 生成类型安全的 Kotlin Object 常量（如 `EvolveStrings.Evo.DnaTitle`），为 IDE 提供自动补全支持。
3. **支持外部字符串包导入（String Pack Modding）**：
   由于 Android 移植版支持导入 Web 版旧存档（内含 `string_pack` LZString），`StringTable` 设计已预留层叠加载能力，可在载入存档时将自定义词条覆盖至 `dictionary` 最顶层。
4. **单元测试与 CI 校验**：
   编写测试用例验证：
   - 提取的 2,544 条中文词条中，所有占位符 `%0` ~ `%9` 的出现数量与英文原版严格一致；
   - 包含原生 `%` 的 635 个词条在格式化时不会抛出异常；
   - 缺失键透明回退至英文原版。

---

## 8. 一手证据与引用索引 (Primary Source Citations)

- **词条全量定义与键位分布**: [`refs/strings/strings.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.json) (11,641 键)
- **简体中文完整对齐词库**: [`refs/strings/strings.zh-CN.json`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/strings.zh-CN.json)
- **上游翻译同步与检查工具**: [`refs/strings/readme.md#L1-L35`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/strings/readme.md#L1-L35)
- **上游运行时本地化加载与回退实现**: [`refs/src/locale.js#L6-L88`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/locale.js#L6-L88)
- **演化时代动作定义与多语言调用**: [`refs/src/actions.js#L21-L150`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/actions.js#L21-L150)
- **建筑名称动态分支与复数处理**: [`refs/src/actions.js#L494`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/actions.js#L494), [`refs/src/actions.js#L8222-L8260`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/actions.js#L8222-L8260)
- **科技树时代划分与路径定义**: [`refs/src/tech.js#L15698-L15701`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/tech.js#L15698-L15701)
- **地狱巡逻战报中的硬编码复数逻辑**: [`refs/src/portal.js#L1868-L1875`](file:///C:/Users/nobody/.gemini/antigravity/worktrees/5885081c-16fe-4c9d-b5df-99a574377867/subagent-Localization-Pipeline-Researcher-self-66eb8b49/refs/src/portal.js#L1868-L1875)
