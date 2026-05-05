# 自动打卡 Attendance Tracker

一个基于 Android 原生开发的自动打卡 App，可根据公司位置和围栏半径自动记录上下班时间。

## 项目简介

用户设置公司位置和围栏半径后，App 通过高德定位前台服务在后台定期获取当前位置。当检测到进入公司范围时自动记录上班时间，离开公司范围时自动记录下班时间。每天第一次进入作为上班时间，最后一次离开作为下班时间。非工作日进入公司会生成待确认记录。

> 此项目为个人学习/开发阶段作品，不是企业级考勤系统。

## 核心功能

- [x] 公司位置设置（手动输入 / 高德地图选点 / POI 搜索）
- [x] 高德地图选点（点击选点 + Marker）
- [x] POI 搜索（关键字搜索 + 候选列表）
- [x] 逆地理编码（点击地图自动解析地址）
- [x] 围栏半径设置（默认 150 米）
- [x] 前台定位服务（高德定位 SDK）
- [x] 自动进入 / 离开检测（基于距离判断）
- [x] 每日考勤记录汇总（第一条进入 / 最后一条离开）
- [x] 原始进出事件记录（LocationEvent 流水）
- [x] 非工作日待确认（PENDING → 用户确认 → CONFIRMED）
- [x] 工作日判断（周一到周五工作日，周六周日非工作日）
- [x] 首页卡片式 UI
- [x] 设置页卡片式 UI
- [x] 记录页卡片式 UI
- [x] DataStore 本地状态保存
- [x] Room 本地数据库

## App 页面

### 首页

- 顶部显示日期、工作日/休息日、自动检测开关状态
- 今日考勤状态卡片：未到达 / 已到公司 / 已离开 / 待确认（不同颜色）
- 今日时间线卡片：上班时间、下班时间、今日工时
- 自动检测卡片：最近检测时间、距离公司距离、当前是否在公司范围内，开启/关闭按钮
- 公司位置卡片：地址、围栏半径
- 导航按钮：设置、打卡记录
- Debug 构建下显示开发者测试按钮

### 设置页

- 公司位置卡片：当前地址和经纬度
- 地图选点卡片：跳转到地图选点页面（含 POI 搜索、点击选点）
- 手动输入卡片：纬度、经度、地址输入框
- 围栏半径卡片：半径输入框（支持 10-5000 米）
- 提醒设置：周末提醒打卡开关
- 保存按钮（保存后自动返回首页）
- 清除按钮

### 记录页

- 顶部标题 + 返回按钮
- 按日期展示打卡记录卡片列表
- 每张卡片显示：日期、状态 Chip（正常/待确认/未完整）、上班时间、下班时间、工时
- 无记录时显示友好空状态

## App 截图

> 后续可在此处补充首页、设置页、记录页截图。

| 首页 | 设置页 | 记录页 |
|---|---|---|
| 待补充 | 待补充 | 待补充 |

## 每日考勤汇总规则

每天只保留一条 `AttendanceRecord`，表示当天的考勤汇总。

| 事件 | 行为 |
|------|------|
| 当天第一次 ENTER | 创建记录，设置 arriveTime |
| 当天后续 ENTER | **不覆盖** arriveTime，仅插入 `LocationEvent` |
| 当天 EXIT | 更新 leaveTime = 当前时间 |
| 当天后续 EXIT | **始终更新** leaveTime，确保是最后一次离开时间 |
| `LocationEvent` | 保留所有 ENTER/EXIT 原始事件流水 |

**首页状态判断**：
- 无记录 / 无 arriveTime → 未到公司
- status = PENDING → 优先显示"非工作日到达，等待确认"
- 最新 LocationEvent 是 ENTER → 已到公司
- 最新 LocationEvent 是 EXIT → 已离开公司

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.24 |
| UI | Jetpack Compose + Material Design 3 (BOM 2024.05.00) |
| 导航 | Navigation Compose 2.8.1 |
| 本地存储 | DataStore Preferences 1.1.1 |
| 数据库 | Room 2.6.1 + KSP 1.9.24-1.0.20 |
| 地图 | 高德 3DMap 7.4.0 + 定位 5.2.0 + 搜索 7.1.0 |
| 架构 | MVVM (ViewModel + Repository + Room + DataStore) |

## 项目结构

```
app/src/main/java/com/example/attendance/
├── MainActivity.kt
├── data/
│   ├── local/             # Room 数据库
│   │   ├── AppDatabase.kt
│   │   ├── AttendanceDao.kt
│   │   └── entities/      # AttendanceRecord, LocationEvent
│   ├── repository/        # AttendanceRepository
│   └── settings/          # SettingsDataStore
├── domain/                # 业务逻辑
│   ├── AttendanceRules.kt
│   └── WorkdayChecker.kt
├── location/              # 定位 & 地图
│   ├── AMapLocationClientWrapper.kt
│   ├── AttendanceLocationService.kt    # 前台定位服务
│   ├── PoiSearchManager.kt
│   └── ReverseGeocoder.kt
├── notification/          # 通知
│   ├── AttendanceActionReceiver.kt
│   └── NotificationHelper.kt
├── ui/
│   ├── components/        # 通用组件
│   ├── home/              # 首页
│   ├── map/               # 地图组件
│   ├── navigation/        # 导航图
│   ├── records/           # 记录页
│   ├── settings/          # 设置页 + 地图选点页
│   └── theme/             # Material 3 主题
└── util/
    ├── DateTimeUtils.kt
    └── PermissionUtils.kt
```

## 本地构建

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK API 34
- JDK 17

### 步骤

1. 克隆项目
   ```bash
   git clone https://github.com/yourusername/attendance-tracker.git
   ```
2. 用 Android Studio 打开项目根目录
3. 配置高德 Key（见下方说明）
4. 等待 Gradle 同步完成
5. 点击 Run 运行

### 高德 Key 配置

1. 前往 [高德开放平台](https://lbs.amap.com/) 注册账号
2. 创建应用，获取 API Key
3. 打开 `local.properties`，添加：
   ```
   AMAP_API_KEY=你的高德Key
   ```
4. 重新同步 Gradle 后运行

**注意**：`local.properties` 已配置在 `.gitignore` 中，不会提交到 Git 仓库。不要将真实 Key 写入代码或提交到仓库。

### 构建命令

```bash
# 调试构建
./gradlew assembleDebug

# 发布构建
./gradlew assembleRelease
```

## Android 权限说明

| 权限 | 用途 | 必需 |
|------|------|------|
| `ACCESS_FINE_LOCATION` | 前台精确定位 | 地图选点、距离检测 |
| `ACCESS_COARSE_LOCATION` | 前台粗略定位 | 距离检测降级 |
| `ACCESS_BACKGROUND_LOCATION` | 后台定位 | 后台自动检测 |
| `POST_NOTIFICATIONS` | 发送通知 (Android 13+) | 前台服务通知 |
| `FOREGROUND_SERVICE` | 前台服务 | 后台定位检测 |
| `FOREGROUND_SERVICE_LOCATION` | 前台定位服务 (Android 14+) | 后台定位检测 |

详细权限说明请参考 [docs/permission-notes.md](docs/permission-notes.md)。

## 常见问题

### Q: 编译时提示高德 Key 未配置？
A: 确保 `local.properties` 中已添加 `AMAP_API_KEY=你的Key`。

### Q: 开启自动检测后没有反应？
A: 检查是否授予了后台定位权限。Android 10+ 需要在系统设置中手动开启「始终允许」定位权限。开启后等待 1-2 分钟，首页自动检测卡片会显示最近检测时间。

### Q: 非工作日到达没有通知？
A: Android 13+ 需要授予通知权限。首次打开应用时会提示授权。

## 后续计划

- [ ] 完善单元测试和 UI 测试
- [ ] CI/CD 配置
- [ ] 国际化支持
- [ ] 团队/多设备适配

## License

MIT License - 详见 [LICENSE](LICENSE) 文件。
