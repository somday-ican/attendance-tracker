# Attendance Tracker 架构设计

## 一、整体架构

采用 MVVM (Model-View-ViewModel) 架构模式，分层清晰，职责分明。

```
└──────────────────────────────────────────────┘

## 二、数据表设计

### AttendanceRecord — 每日考勤汇总

每条记录代表一天的考勤汇总，而不是每次进出事件。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| date | String | 日期，格式 yyyy-MM-dd |
| arriveTime | Long? | 当天**第一次**进入公司时间 |
| leaveTime | Long? | 当天**最后一次**离开公司时间 |
| isWorkday | Boolean | 是否为工作日 |
| source | String | AUTO / MANUAL |
| status | String | CONFIRMED / PENDING |
| address | String? | 公司地址 |
| note | String? | 备注 |

### LocationEvent — 原始进出事件流水

每条记录代表一次原始的 ENTER 或 EXIT 事件，用于调试和状态判断。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| type | String | ENTER / EXIT |
| timestamp | Long | 事件时间戳 |
| latitude | Double? | 事件发生时纬度 |
| longitude | Double? | 事件发生时经度 |
| accuracy | Float? | 定位精度 |
| handled | Boolean | 是否已处理 |

### 职责区别

- **AttendanceRecord** = 每日考勤汇总。每天只一条，记录上班/下班时间。
- **LocationEvent** = 原始进出事件流水。每次进入/离开都会记录，用于判断最新状态是 EINTER 还是 EXIT。

## 三、数据流说明

### 3.1 公司位置设置数据流

```
用户输入/地图选点 → SettingsViewModel → SettingsDataStore (DataStore)
                                            ↓
AppNavGraph (LaunchedEffect 监听 DataStore Flow)
                                            ↓
GeofenceManager.registerGeofence() → Google Play Services GeofencingClient
```

### 3.2 打卡记录数据流

两条数据表并行写入：

```
Geofence ENTER/EXIT → GeofenceBroadcastReceiver
                           ↓
                    AttendanceRepository.handleEnter/handleExit
                           ↓
                    ┌─────────────────┬──────────────────┐
                    │ AttendanceRecord │  LocationEvent   │
                    │ (每日考勤汇总)   │ (原始事件流水)    │
                    │                 │                  │
                    │ date=今天       │ type=ENTER/EXIT  │
                    │ arriveTime=首次 │ timestamp=当前   │
                    │ leaveTime=末次  │                  │
                    └────────┬────────┴────────┬─────────┘
                             │                 │
                             ▼                 ▼
                    RecordsViewModel      (调试用, 未在前台展示)
                             │
                             ▼
                    RecordsScreen (LazyColumn)
```

- **首次 ENTER**: 创建 AttendanceRecord（设置 arriveTime）+ 插入 LocationEvent(ENTER)
- **后续 ENTER**: 仅插入 LocationEvent(ENTER)，**不覆盖** arriveTime
- **EXIT**: 更新 AttendanceRecord.leaveTime + 插入 LocationEvent(EXIT)
- **后续 EXIT**: 始终更新 leaveTime 为最新时间

## 四、地图选点流程

```
用户打开 CompanyLocationPickerScreen
           ↓
显示高德 MapView (AndroidView 包装)
           ↓
点击地图 → aMap.setOnMapClickListener
           ↓
更新 MapPickerState (selectedLatitude, selectedLongitude, hasMarker)
           ↓
清除旧 Marker → 添加新 Marker → 移动相机
           ↓
触发 onMapClicked 回调
           ↓
ReverseGeocoder.resolve() → 获取地址
           ↓
更新 selectedAddress
           ↓
用户点击确认保存
           ↓
CompanyLocationPickerViewModel → SettingsDataStore
```

## 四、POI 搜索流程

```
用户在 PoiSearchBox 中输入关键词（≥2字）
           ↓
PoiSearchManager.search() → PoiSearch.searchPOIAsyn()
           ↓
搜索结果 → PoiSearchBox 显示候选列表
           ↓
用户点击 POI 结果
           ↓
更新 MapPickerState (经纬度、地址、hasMarker)
           ↓
state.moveToTrigger += 1 (触发相机移动)
           ↓
AMapView LaunchedEffect → aMap.moveCamera() → 添加 Marker
```

## 五、逆地理编码流程

```
地图点击 → onMapClicked(lat, lng)
           ↓
ReverseGeocoder.resolve() → GeocodeSearch.getFromLocationAsyn()
           ↓
onRegeocodeSearched 回调
           ↓
成功 → result.regeocodeAddress.formatAddress → 更新 selectedAddress
失败 → selectedAddress = "地址解析失败"
```

## 六、Geofence 自动打卡流程

### 注册围栏

```
AppNavGraph LaunchedEffect 观察 DataStore
           ↓
companyLocation 变化 → GeofenceManager.registerGeofence()
           ↓
GeofencingClient.addGeofences() → PendingIntent → GeofenceBroadcastReceiver
```

### 进入围栏 (ENTER)

```
GeofenceBroadcastReceiver.onReceive()
           ↓
repository.handleEnter("AUTO")
           ↓
插入 LocationEvent(type=ENTER)
           ↓
查询今天是否已有 AttendanceRecord
           ↓
无记录 → 创建新记录 (arriveTime=now, status 根据工作日判断)
有记录 → 不操作记录 (不覆盖 arriveTime)
           ↓
WorkdayChecker.isWorkday()
           ↓
工作日   → status=CONFIRMED
非工作日 → status=PENDING + 发送通知
```

### 离开围栏 (EXIT)

```
GeofenceBroadcastReceiver.onReceive()
           ↓
repository.handleExit()
           ↓
插入 LocationEvent(type=EXIT)
           ↓
查询今天 AttendanceRecord
           ↓
有记录 → 更新 leaveTime=now (始终最新)
无记录 → 只保留 LocationEvent，不创建异常记录
```

### 首页状态判断

```
读取今日 AttendanceRecord + 最新 LocationEvent
           ↓
┌─ 无记录/无 arriveTime → "未到公司"
├─ status = PENDING      → "非工作日到达，等待确认" (优先)
├─ leaveTime != null     → ┌─ 最新 Event=ENTER → "已到公司"
│                          └─ 最新 Event=EXIT  → "已离开公司"
└─ else                  → "已到公司"
```

### 总上班时长

```
有 arriveTime + leaveTime → formatDuration(arriveTime, leaveTime)
                            <1小时: "xx 分钟"
                            ≥1小时: "x 小时 xx 分钟"
有 arriveTime 无 leaveTime → "进行中"
无 arriveTime              → (不显示)
```

## 七、非工作日确认流程

```
非工作日 ENTER → 生成 PENDING 记录 + 发送通知
           ↓
通知标题："非工作日到达公司"
通知内容："今天是非工作日，是否确认记录本次到公司？"
通知按钮："确认记录"
           ↓
用户点击 "确认记录"
           ↓
AttendanceActionReceiver.onReceive()
           ↓
查询当天记录 → 更新 status = CONFIRMED
           ↓
用户不点击 → 记录保持 PENDING
```
