# Android 权限说明

## 权限列表

| 权限 | 最低 API | 用途 | 是否必需 |
|------|----------|------|----------|
| `ACCESS_FINE_LOCATION` | API 1 | 前台精确定位，用于地图选点和 Geofence 注册 | 是 |
| `ACCESS_COARSE_LOCATION` | API 1 | 前台粗略定位，作为精确定位的降级方案 | 是 |
| `ACCESS_BACKGROUND_LOCATION` | API 29 (Android 10) | 后台定位，用于 Geofence 自动进出检测 | Geofence 功能必需 |
| `POST_NOTIFICATIONS` | API 33 (Android 13) | 发送非工作日确认通知 | 非工作日确认必需 |

## 为什么需要前台定位 (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)

- 打开地图选点页面时，尝试自动定位到用户当前位置
- 注册 Geofence 时需要精确定位权限

## 为什么需要后台定位 (`ACCESS_BACKGROUND_LOCATION`)

- 应用在后台时，Geofence 才能检测到用户进入/离开公司区域
- 如果不授予此权限，Geofence 仅在前台工作时生效

## 为什么需要通知权限 (`POST_NOTIFICATIONS`)

- 非工作日进入公司时，弹出通知让用户确认是否记录
- Android 13+ 要求运行时授权通知权限

## Android 不同版本权限差异

### Android 10 (API 29) 及以上

- `ACCESS_BACKGROUND_LOCATION` 需要单独授权
- 无法在首次定位权限弹窗时同时申请，需要额外步骤

### Android 11 (API 30) 及以上

- 后台定位权限申请流程变更，需要在系统设置中手动开启

### Android 13 (API 33) 及以上

- `POST_NOTIFICATIONS` 作为运行时权限，需要在代码中申请
- 新增通知运行时权限弹窗

## 用户如何开启后台定位

### 方法一：首次授权时选择「始终允许」

1. 首次打开应用时，定位权限弹窗选择「始终允许」
2. 系统会自动授予前后台定位权限

### 方法二：在系统设置中开启

1. 打开系统「设置」→「应用」→「Attendance Tracker」
2. 点击「权限」→「位置信息」
3. 选择「始终允许」
4. 如果 Android 11+，还需要确保「精确位置」已开启

## 用户如何开启通知权限

### Android 13+

1. 首次打开应用时，通知权限弹窗点击「允许」
2. 如果拒绝后需要重新开启：
   - 系统「设置」→「应用」→「Attendance Tracker」
   - 点击「通知」→ 开启「允许通知」

### Android 12 及以下

- 通知权限默认开启，无需用户操作
