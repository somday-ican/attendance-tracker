# Attendance Tracker 架构设计

## 一、整体架构

采用 MVVM (Model-View-ViewModel) 架构模式，分层清晰，职责分明。

```
┌──────────────────────────────────────────────┐
│ UI Layer (Compose Screens)                   │
│  HomeScreen / SettingsScreen /               │
│  CompanyLocationPickerScreen / RecordsScreen │
├──────────────────────────────────────────────┤
│ ViewModel Layer                              │
│  HomeViewModel / SettingsViewModel /         │
│  CompanyLocationPickerViewModel /            │
│  RecordsViewModel                            │
├──────────────────────────────────────────────┤
│ Domain Layer                                 │
│  WorkdayChecker / AttendanceRules            │
├──────────────────────────────────────────────┤
│ Data Layer                                   │
│  AttendanceRepository                        │
│  ├── Room (AppDatabase / AttendanceDao)      │
│  ├── DataStore (SettingsDataStore)           │
│  └── WorkdayChecker                          │
├──────────────────────────────────────────────┤
│ Infrastructure Layer                         │
│  ├── Location (AMapLocationClientWrapper /   │
│  │    ReverseGeocoder / PoiSearchManager)    │
│  ├── Geofence (GeofenceManager /             │
│  │    GeofenceBroadcastReceiver)             │
│  └── Notification (NotificationHelper /      │
│       AttendanceActionReceiver)              │
└──────────────────────────────────────────────┘
```

## 二、数据流说明

### 公司位置设置数据流

```
用户输入/地图选点 → SettingsViewModel → SettingsDataStore (DataStore)
                                            ↓
AppNavGraph (LaunchedEffect 监听 DataStore Flow)
                                            ↓
GeofenceManager.registerGeofence() → Google Play Services GeofencingClient
```

### 打卡记录数据流

```
Geofence ENTER/EXIT → GeofenceBroadcastReceiver
                           ↓
                    AppDatabase (Room)
                           ↓
                    AttendanceDao
                           ↓
                    AttendanceRepository.allRecords (Flow)
                           ↓
                    RecordsViewModel.records (StateFlow)
                           ↓
                    RecordsScreen (LazyColumn)
```

## 三、地图选点流程

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
WorkdayChecker.isWorkday() → 工作日 or 非工作日
           ↓
插入 LocationEvent(type=ENTER)
           ↓
工作日   → 插入/更新 AttendanceRecord(status=CONFIRMED)
非工作日 → 插入/更新 AttendanceRecord(status=PENDING) + 发送通知
```

### 离开围栏 (EXIT)

```
GeofenceBroadcastReceiver.onReceive()
           ↓
插入 LocationEvent(type=EXIT)
           ↓
更新 AttendanceRecord.leaveTime
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
