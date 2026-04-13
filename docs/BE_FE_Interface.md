# Smart Warehouse Dashboard — BE/FE 인터페이스 명세서

> **프로젝트**: 스마트 창고 출고 지연 예측 대시보드
> **작성 기준**: Backend Spring Boot 3.5 / Frontend Vue 3 (Vite)
> **Base URL**: `http://localhost:8080/api/v1`
> **Swagger**: `http://localhost:8080/swagger-ui.html`

---

## 목차

1. [시스템 구조 개요](#1-시스템-구조-개요)
2. [공통 규칙](#2-공통-규칙)
3. [화면 흐름 및 API 호출 순서](#3-화면-흐름-및-api-호출-순서)
4. [API 명세](#4-api-명세)
   - [4.1 공장 목록 조회](#41-공장-목록-조회)
   - [4.2 공장 기본 정보 조회](#42-공장-기본-정보-조회)
   - [4.3 시나리오 목록 조회](#43-시나리오-목록-조회)
   - [4.4 예측 출고 지연 조회](#44-예측-출고-지연-조회)
   - [4.5 주문 유입량 조회](#45-주문-유입량-조회)
   - [4.6 로봇 운영 현황 조회](#46-로봇-운영-현황-조회)
   - [4.7 혼잡도 조회](#47-혼잡도-조회)
5. [데이터 타입 정의](#5-데이터-타입-정의)
6. [프론트엔드 구현 가이드](#6-프론트엔드-구현-가이드)

---

## 1. 시스템 구조 개요

```
┌─────────────────────────────────────────────────────────┐
│                      Frontend (Vue 3)                   │
│                                                         │
│  [ 공장 드롭다운 ] → [ 시나리오 드롭다운 ] → [ 대시보드 ] │
└────────────────────────────┬────────────────────────────┘
                             │ HTTP REST (JSON)
                             ▼
┌─────────────────────────────────────────────────────────┐
│              Backend (Spring Boot 3.5 / MyBatis)        │
│                                                         │
│  WarehouseController → WarehouseService → Mapper → DB   │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    MySQL (warehouse_db)                  │
│                                                         │
│  warehouse_layout   warehouse_snapshot                  │
│  (창고 정적 정보)    (15분 단위 스냅샷 데이터)            │
└─────────────────────────────────────────────────────────┘
```

### 핵심 데이터 구조

| 테이블 | 역할 | 주요 컬럼 |
|---|---|---|
| `warehouse_layout` | 창고 레이아웃 정적 정보 | layout_id, layout_type, floor_area_sqm, robot_total |
| `warehouse_snapshot` | 15분 단위 운영 스냅샷 | layout_id, scenario_id, snapshot_time, 각종 지표 |

> `snapshot_time`은 시나리오별로 `2024-01-01 00:00:00`부터 **15분 간격**으로 증가하는 임의 시간값입니다.

---

## 2. 공통 규칙

| 항목 | 내용 |
|---|---|
| 프로토콜 | HTTP |
| 데이터 형식 | JSON |
| 인코딩 | UTF-8 |
| 요청 메서드 | GET |
| 응답 Content-Type | `application/json` |
| 날짜 형식 | `yyyy-MM-dd'T'HH:mm:ss` (LocalDateTime, ISO 8601) |
| Null 처리 | 데이터 없는 필드는 `null` 반환 — FE에서 방어 처리 필요 |

---

## 3. 화면 흐름 및 API 호출 순서

```
[ 페이지 진입 ]
      │
      ▼
 GET /factories                     → 공장 드롭다운 렌더링
      │
      │ 사용자가 공장 선택 (layoutId)
      ▼
 GET /factories/{factoryId}         → 상단 공장 정보 카드 렌더링
 GET /factories/{factoryId}/scenarios → 시나리오 드롭다운 렌더링
      │
      │ 사용자가 시나리오 선택 (scenarioId)
      ▼
 GET .../dashboard/shipping-delay   → 출고 지연 시계열 차트
 GET .../dashboard/order-inflow     → 주문 유입량 시계열 차트
 GET .../dashboard/robot-summary    → 로봇 현황 차트
 GET .../dashboard/congestion       → 혼잡도 차트
```

> 대시보드 4개 API는 동시에 병렬 호출해도 됩니다.
> X축(시간축)은 모든 차트에서 공통으로 `snapshotTime`을 사용합니다.

---

## 4. API 명세

---

### 4.1 공장 목록 조회

**사용 화면**: 첫 화면 공장 선택 드롭다운

```
GET /api/v1/factories
```

#### Response (Array)

```json
[
  {
    "layoutId": "WH_136",
    "layoutType": "Type_A",
    "floorAreaSqm": 5000,
    "robotTotal": 30
  },
  {
    "layoutId": "WH_204",
    "layoutType": "Type_B",
    "floorAreaSqm": 3200,
    "robotTotal": 20
  }
]
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID (드롭다운 value로 사용) |
| layoutType | String | Y | 레이아웃 타입 (드롭다운 label로 사용 가능) |
| floorAreaSqm | Integer | Y | 창고 면적 (㎡) |
| robotTotal | Integer | Y | 전체 로봇 수 |

---

### 4.2 공장 기본 정보 조회

**사용 화면**: 공장 선택 후 상단 정보 카드

```
GET /api/v1/factories/{factoryId}
```

#### Path Parameter

| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | `WH_136` | 공장 목록에서 선택한 layoutId |

#### Response (Object)

```json
{
  "layoutId": "WH_136",
  "layoutType": "Type_A",
  "floorAreaSqm": 5000,
  "robotTotal": 30
}
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID |
| layoutType | String | Y | 레이아웃 타입 |
| floorAreaSqm | Integer | Y | 창고 면적 (㎡) |
| robotTotal | Integer | Y | 전체 로봇 수 |

---

### 4.3 시나리오 목록 조회

**사용 화면**: 공장 선택 후 시나리오 드롭다운

```
GET /api/v1/factories/{factoryId}/scenarios
```

#### Path Parameter

| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | `WH_136` | 선택한 공장 ID |

#### Response (Array)

```json
[
  { "layoutId": "WH_136", "scenarioId": "SC_07598" },
  { "layoutId": "WH_136", "scenarioId": "SC_07604" },
  { "layoutId": "WH_136", "scenarioId": "SC_07597" }
]
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID |
| scenarioId | String | N | 시나리오 ID (드롭다운 value로 사용) |

---

### 4.4 예측 출고 지연 조회

**사용 화면**: 대시보드 — 출고 지연 시계열 차트
**차트 권장**: Line Chart (X축: snapshotTime, Y축: avgDelayMinutesNext30m)

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/shipping-delay
```

#### Path Parameter

| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | `WH_136` | 선택한 공장 ID |
| scenarioId | String | `SC_07598` | 선택한 시나리오 ID |

#### Response (Array)

```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "snapshotTime": "2024-01-01T00:00:00",
    "avgDelayMinutesNext30m": 5.55
  },
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "snapshotTime": "2024-01-01T00:15:00",
    "avgDelayMinutesNext30m": 5.04
  }
]
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID |
| scenarioId | String | N | 시나리오 ID |
| snapshotTime | LocalDateTime | Y | 스냅샷 시각 (15분 간격) — 차트 X축 |
| avgDelayMinutesNext30m | Double | Y | 향후 30분 예측 출고 지연 (분) — 차트 Y축 |

---

### 4.5 주문 유입량 조회

**사용 화면**: 대시보드 — 주문 유입량 시계열 차트
**차트 권장**: Bar Chart 또는 Line Chart (X축: snapshotTime, Y축: orderInflow15m)

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/order-inflow
```

#### Path Parameter

| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | `WH_136` | 선택한 공장 ID |
| scenarioId | String | `SC_07598` | 선택한 시나리오 ID |

#### Response (Array)

```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "snapshotTime": "2024-01-01T00:00:00",
    "orderInflow15m": 51.0
  },
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "snapshotTime": "2024-01-01T00:15:00",
    "orderInflow15m": null
  }
]
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID |
| scenarioId | String | N | 시나리오 ID |
| snapshotTime | LocalDateTime | Y | 스냅샷 시각 (15분 간격) — 차트 X축 |
| orderInflow15m | Double | Y | 15분 단위 주문 유입량 — 차트 Y축 |

---

### 4.6 로봇 운영 현황 조회

**사용 화면**: 대시보드 — 로봇 현황 차트
**차트 권장**: Stacked Bar Chart (가동/유휴/충전 비율) 또는 Line Chart

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/robot-summary
```

#### Path Parameter

| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | `WH_136` | 선택한 공장 ID |
| scenarioId | String | `SC_07598` | 선택한 시나리오 ID |

#### Response (Array)

```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "snapshotTime": "2024-01-01T00:00:00",
    "robotActive": 9,
    "robotIdle": 21,
    "robotCharging": 0,
    "avgIdleDurationMin": 5.15
  }
]
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID |
| scenarioId | String | N | 시나리오 ID |
| snapshotTime | LocalDateTime | Y | 스냅샷 시각 (15분 간격) — 차트 X축 |
| robotActive | Integer | Y | 가동 중인 로봇 수 |
| robotIdle | Integer | Y | 유휴 로봇 수 |
| robotCharging | Integer | Y | 충전 중인 로봇 수 |
| avgIdleDurationMin | Double | Y | 평균 유휴 시간 (분) |

---

### 4.7 혼잡도 조회

**사용 화면**: 대시보드 — 혼잡도 차트
**차트 권장**: Line Chart 또는 Gauge (X축: snapshotTime, Y축: congestionScore)

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/congestion
```

#### Path Parameter

| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | `WH_136` | 선택한 공장 ID |
| scenarioId | String | `SC_07598` | 선택한 시나리오 ID |

#### Response (Array)

```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "snapshotTime": "2024-01-01T00:00:00",
    "congestionScore": 0.0
  }
]
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| layoutId | String | N | 창고 ID |
| scenarioId | String | N | 시나리오 ID |
| snapshotTime | LocalDateTime | Y | 스냅샷 시각 (15분 간격) — 차트 X축 |
| congestionScore | Double | Y | 혼잡도 점수 (0.0 ~ 1.0, 높을수록 혼잡) |

---

## 5. 데이터 타입 정의

| Java 타입 | JSON 타입 | 예시 | 비고 |
|---|---|---|---|
| String | string | `"WH_136"` | |
| Integer | number | `30` | 정수 |
| Double | number | `5.55` | 소수점 포함, null 가능 |
| LocalDateTime | string | `"2024-01-01T00:00:00"` | ISO 8601 형식 |

---

## 6. 프론트엔드 구현 가이드

### 6.1 API 호출 예시 (Vue 3 + fetch)

```javascript
const BASE_URL = 'http://localhost:8080/api/v1'

// 공장 목록
const getFactories = () =>
  fetch(`${BASE_URL}/factories`).then(r => r.json())

// 시나리오 목록
const getScenarios = (factoryId) =>
  fetch(`${BASE_URL}/factories/${factoryId}/scenarios`).then(r => r.json())

// 대시보드 (병렬 호출)
const getDashboard = (factoryId, scenarioId) => {
  const base = `${BASE_URL}/factories/${factoryId}/scenarios/${scenarioId}/dashboard`
  return Promise.all([
    fetch(`${base}/shipping-delay`).then(r => r.json()),
    fetch(`${base}/order-inflow`).then(r => r.json()),
    fetch(`${base}/robot-summary`).then(r => r.json()),
    fetch(`${base}/congestion`).then(r => r.json()),
  ])
}
```

### 6.2 snapshotTime 파싱

```javascript
// "2024-01-01T00:00:00" → Date 객체로 변환
const parseTime = (str) => new Date(str)

// 차트 라벨용 포맷 (예: "00:00", "00:15")
const formatTime = (str) =>
  new Date(str).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
```

### 6.3 Null 방어 처리

```javascript
// Double 필드는 null일 수 있으므로 반드시 방어 처리
const value = item.avgDelayMinutesNext30m ?? 0
```

### 6.4 CORS 설정

Backend에서 CORS가 설정되어 있지 않다면 프론트엔드에서 호출 시 오류가 발생합니다.
BE 담당자에게 아래 Origin 허용 요청이 필요합니다:

```
http://localhost:5173   (Vite 기본 포트)
```

---

*본 문서는 BE 구현 기준으로 작성되었으며, API 변경 시 함께 업데이트해야 합니다.*
