# Attendance Tracker

基于 Android Jetpack Compose 的考勤追踪应用。通过高德地图选点、Geofence 地理围栏自动检测进出公司，支持工作日/非工作日区分和通知确认。

## 功能列表

- **公司位置设置**：手动输入经纬度 / 高德地图点击选点 / POI 搜索选点
- **逆地理编码**：点击地图后自动解析地址
- **Geofence 自动打卡**：进入/离开公司围栏时自动记录
- **工作日判断**：周一到周五为工作日，周六周日为非工作日
- **非工作日通知确认**：非工作日到达时弹出通知，用户确认后记录
- **打卡记录查看**：历史打卡记录列表，包含总上班时长
- **模拟测试按钮**：HomeScreen 提供模拟进入/离开按钮，方便测试

## 每日考勤汇总规则

每天只保留一条 `AttendanceRecord`，表示当天的考勤汇总。

| 事件 | 行为 |
|------|------|
| 当天第一次 ENTER | 创建记录，设置 arriveTime |
| 当天后续 ENTER | **不覆盖** arriveTime，仅插入 `LocationEvent` |
| 当天 EXIT | 更新 leaveTime = 当前时间 |
| 当天后续 EXIT | **始终更新** leaveTime，确保是最后一次离开时间 |
| `LocationEvent` | 保留所有 ENTER/EXIT 原始事件流水，用于调试 |

**首页状态判断**：
- 无记录 / 无 arriveTime → 未到公司
- status = PENDING → 优先显示"非工作日到达，等待确认"
- 最新 LocationEvent 是 ENTER → 已到公司
- 最新 LocationEvent 是 EXIT → 已离开公司

## 总上班时长统计

当 `arriveTime` 和 `leaveTime` 都存在时：
- `总时长 = leaveTime - arriveTime`
- 显示格式：小于 1 小时显示"xx 分钟"，大于等于 1 小时显示"x 小时 xx 分钟"
- 如果还没有 `leaveTime`，显示"进行中"

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.05.00) |
| 导航 | Navigation Compose 2.8.1 |
| 本地存储 | DataStore Preferences 1.1.1 |
| 数据库 | Room 2.6.1 + KSP 1.9.24-1.0.20 |
| 地图 | 高德 3DMap 7.4.0 + 定位 5.2.0 |
| 地理围栏 | Google Play Services Location 21.0.1 |
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
├── location/              # 定位 & 地理围栏
│   ├── AMapLocationClientWrapper.kt
│   ├── GeofenceBroadcastReceiver.kt
│   ├── GeofenceManager.kt
│   ├── PoiSearchManager.kt
│   └── ReverseGeocoder.kt
├── notification/          # 通知
│   ├── AttendanceActionReceiver.kt
│   └── NotificationHelper.kt
├── ui/
│   ├── components/        # PermissionCard
│   ├── home/              # HomeScreen + HomeViewModel
│   ├── map/               # AMapView, MapPickerState, PoiSearchBox
│   ├── navigation/        # AppNavGraph
│   ├── records/           # RecordsScreen + RecordsViewModel
│   ├── settings/          # SettingsScreen, SettingsViewModel, CompanyLocationPickerScreen, CompanyLocationPickerViewModel
│   └── theme/             # Color, Theme, Type
└── util/
    ├── DateTimeUtils.kt
    └── PermissionUtils.kt
```

## 如何运行

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK API 34
- JDK 17

### 步骤

1. 克隆项目
2. 用 Android Studio 打开项目根目录
3. 配置高德 Key（见下方说明）
4. 等待 Gradle 同步完成
5. 点击 Run 运行

## 高德 Key 配置方式

1. 前往 [高德开放平台](https://lbs.amap.com/) 注册账号
2. 创建应用，获取 API Key
3. 打开 `local.properties`，添加：
   ```
   AMAP_API_KEY=你的高德Key
   ```
4. 重新同步 Gradle 后运行

**注意**：`local.properties` 已配置在 `.gitignore` 中，不会提交到 Git 仓库。

## Android 权限说明

| 权限 | 用途 | 必需 |
|------|------|------|
| `ACCESS_FINE_LOCATION` | 前台精确定位 | 地图选点、Geofence |
| `ACCESS_COARSE_LOCATION` | 前台粗略定位 | 地图选点、Geofence |
| `ACCESS_BACKGROUND_LOCATION` | 后台定位 | Geofence 自动进出检测 |
| `POST_NOTIFICATIONS` | 发送通知 (Android 13+) | 非工作日确认通知 |

详细权限说明请参考 [docs/permission-notes.md](docs/permission-notes.md)。

## 当前已实现功能

- [x] 项目骨架搭建 (Gradle + Compose + MD3)
- [x] Navigation Compose 页面导航
- [x] DataStore 公司位置存储
- [x] Room 打卡记录数据库
- [x] 工作日判断逻辑
- [x] 高德地图选点 (点击选点 + POI 搜索)
- [x] 逆地理编码 (点击自动解析地址)
- [x] Geofence 自动进出检测
- [x] 非工作日通知确认

## 后续 TODO

- [ ] 完善单元测试和 UI 测试
- [ ] CI/CD 配置
- [ ] 国际化支持

## 常见问题

### Q: 编译时提示高德 Key 未配置？
A: 确保 `local.properties` 中已添加 `AMAP_API_KEY=你的Key`。

### Q: Geofence 不生效？
A: 需要授予后台定位权限。Android 10+ 需要在系统设置中手动开启「始终允许」定位权限。

### Q: 非工作日没有收到通知？
A: Android 13+ 需要授予通知权限。首次打开应用时会提示授权。

## License

MIT License - 详见 [LICENSE](LICENSE) 文件。
