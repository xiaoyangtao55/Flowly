# Flowly · 流量视界

一个用 Kotlin + Jetpack Compose 编写的 Android 流量统计应用，采用 **Material 3 Expressive** 设计语言，在本地读取系统流量数据，展示每日用量、套餐进度与应用排行。

所有数据均来自系统 `NetworkStatsManager`，不上传、不联网、不采集任何个人信息。

## 功能

- **套餐总览**：本计费周期移动流量使用量 / 剩余量 / 套餐进度环，支持自定义套餐额度与账单起始日
- **今日速览**：今日移动数据与 Wi-Fi 用量，并与昨日对比给出涨跌百分比
- **用量趋势**：近 7 天 / 近 30 天按日柱状图，可切换移动数据与 Wi-Fi
- **应用排行**：按 UID 汇总各应用在今日 / 本周期内的流量消耗，带用量条形对比
- **热点共享**：读取系统记录的 tethering（UID `-5`）用量；机型不上报时显示“—”而非 0
- **应用详情**：点击任意应用查看其今日与本周期分别的移动、Wi-Fi 用量
- **权限引导**：未授予「使用情况访问权限」时展示引导页，一键跳转系统设置
- **本地存储**：套餐配置保存在本机 SharedPreferences

## 设计：Material 3 Expressive

- `MaterialExpressiveTheme` + `MotionScheme.expressive` 弹性动效
- 自定义薄荷绿 / 深青配色，浅色与深色双主题
- 34dp 超大圆角卡片、Emphasized 大字号数字排版
- `CircularWavyProgressIndicator` 波动进度环、`LoadingIndicator` 加载态
- `ToggleButton` 传输方式切换、`MaterialShapes.Flower` 装饰图形
- 柱状图基于 Canvas 手绘，带 spring 入场动画与渐变填充

## 权限

| 权限 | 用途 |
| --- | --- |
| `android.permission.PACKAGE_USAGE_STATS` | 通过 `NetworkStatsManager` 读取各应用流量统计，需在系统「使用情况访问权限」中手动开启 |
| `android.permission.ACCESS_NETWORK_STATE` | 判断网络状态 |
| `android.permission.QUERY_ALL_PACKAGES` | 获取应用名称与图标用于排行展示 |

首次启动如未授权，应用会展示引导页，点击「去授权」跳转至系统设置，开启后返回即自动刷新。

## 构建

环境要求：JDK 17、Android SDK Platform 37、Build-Tools 37。

```bash
git clone https://github.com/xiaoyangtao55/Flowly.git
cd Flowly
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 技术栈

- Kotlin 2.4.21，AGP 9.4.1，Gradle 9.8.2
- Jetpack Compose（BOM 2026.09.00）
- Material 3 `1.5.0-beta02`（Expressive API）
- `minSdk 26` / `targetSdk 37`
- 架构：单 Activity + Compose，`UsageViewModel` + `UsageRepository` 单向数据流

## 项目结构

```
app/src/main/java/com/flowly/usage/
├── MainActivity.kt          # 入口，edge-to-edge
├── FlowlyApp.kt             # Scaffold + 底部导航 + 权限门控
├── data/
│   ├── Models.kt            # Transport / Period / PlanSettings / UsageSnapshot
│   ├── PlanStore.kt         # 套餐配置持久化
│   └── UsageRepository.kt   # NetworkStatsManager 查询与聚合
└── ui/
    ├── UsageViewModel.kt    # UiState 与刷新逻辑
    ├── Format.kt            # 字节、百分比、日期格式化
    ├── theme/Theme.kt       # 配色、形状、字体
    ├── components/          # 卡片、排行行、柱状图等复用组件
    └── screens/             # 概览 / 应用 / 设置 / 应用详情
```

## 已知限制

- 流量数据依赖系统统计，部分 ROM 的统计口径与运营商账单存在差异
- 热点用量依赖机型是否上报 UID `-5`，不上报的机型无法给出数值
- 首次查询 30 天历史数据在低端设备上可能耗时较长
- 目前仅为调试签名包，未提供正式签名与商店发布配置

## 许可

本项目尚未附加开源协议。如需使用、分发或二次开发，请先联系作者。
