# API Interface Document
> Smart Warehouse Outbound Delay Prediction
> Base URL: `http://localhost:8080/api/v1`

---

## 공통 사항

| 항목 | 내용 |
|---|---|
| 프로토콜 | HTTP |
| 데이터 형식 | JSON |
| 인코딩 | UTF-8 |
| Content-Type | `application/json` |

---

## 1. 공장 목록 조회

**FE 사용 시점**: 첫 화면 진입 시 드롭다운 1 렌더링

```
GET /api/v1/factories
```

### Response
```json
[
  {
    "layoutId": "WH_136",
    "layoutType": "string",
    "floorAreaSqm": 5000,
    "robotTotal": 30
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID (PK) |
| layoutType | String | 레이아웃 타입 |
| floorAreaSqm | Integer | 창고 면적 (㎡) |
| robotTotal | Integer | 전체 로봇 수 |

---

## 2. 공장 기본 정보 조회

**FE 사용 시점**: 공장 선택 후 상단 정보 카드 렌더링

```
GET /api/v1/factories/{factoryId}
```

### Path Parameter
| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | WH_136 | 창고 ID |

### Response
```json
{
  "layoutId": "WH_136",
  "layoutType": "string",
  "floorAreaSqm": 5000,
  "robotTotal": 30
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID |
| layoutType | String | 레이아웃 타입 |
| floorAreaSqm | Integer | 창고 면적 (㎡) |
| robotTotal | Integer | 전체 로봇 수 |

---

## 3. 시나리오 목록 조회

**FE 사용 시점**: 공장 선택 후 드롭다운 2 렌더링

```
GET /api/v1/factories/{factoryId}/scenarios
```

### Path Parameter
| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | WH_136 | 창고 ID |

### Response
```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598"
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID |
| scenarioId | String | 시나리오 ID |

---

## 4. 예측 출고 지연 조회

**FE 사용 시점**: 대시보드 - 출고 지연 차트

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/shipping-delay
```

### Path Parameter
| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | WH_136 | 창고 ID |
| scenarioId | String | SC_07598 | 시나리오 ID |

### Response
```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "avgDelayMinutesNext30m": 5.55
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID |
| scenarioId | String | 시나리오 ID |
| avgDelayMinutesNext30m | Double | 향후 30분 예측 출고 지연 (분) |

---

## 5. 주문 유입량 조회

**FE 사용 시점**: 대시보드 - 주문 유입량 차트

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/order-inflow
```

### Path Parameter
| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | WH_136 | 창고 ID |
| scenarioId | String | SC_07598 | 시나리오 ID |

### Response
```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "orderInflow15m": 51.0
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID |
| scenarioId | String | 시나리오 ID |
| orderInflow15m | Double | 15분 단위 주문 유입량 |

---

## 6. 로봇 운영 현황 조회

**FE 사용 시점**: 대시보드 - 로봇 현황 카드/차트

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/robot-summary
```

### Path Parameter
| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | WH_136 | 창고 ID |
| scenarioId | String | SC_07598 | 시나리오 ID |

### Response
```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "robotActive": 9,
    "robotIdle": 21,
    "robotCharging": 0,
    "avgIdleDurationMin": 5.15
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID |
| scenarioId | String | 시나리오 ID |
| robotActive | Integer | 가동 중인 로봇 수 |
| robotIdle | Integer | 유휴 로봇 수 |
| robotCharging | Integer | 충전 중인 로봇 수 |
| avgIdleDurationMin | Double | 평균 유휴 시간 (분) |

---

## 7. 혼잡도 조회

**FE 사용 시점**: 대시보드 - 혼잡도 게이지/차트

```
GET /api/v1/factories/{factoryId}/scenarios/{scenarioId}/dashboard/congestion
```

### Path Parameter
| 파라미터 | 타입 | 예시 | 설명 |
|---|---|---|---|
| factoryId | String | WH_136 | 창고 ID |
| scenarioId | String | SC_07598 | 시나리오 ID |

### Response
```json
[
  {
    "layoutId": "WH_136",
    "scenarioId": "SC_07598",
    "congestionScore": 0.0
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| layoutId | String | 창고 ID |
| scenarioId | String | 시나리오 ID |
| congestionScore | Double | 혼잡도 점수 (0.0 ~ 1.0) |

---

## FE 호출 순서 (플로우)

```
1. GET /factories
        ↓ layoutId 선택
2. GET /factories/{factoryId}          ← 상단 정보 카드
   GET /factories/{factoryId}/scenarios ← 시나리오 드롭다운
        ↓ scenarioId 선택
3. GET .../dashboard/shipping-delay    ← 출고 지연 차트
   GET .../dashboard/order-inflow      ← 주문 유입량 차트
   GET .../dashboard/robot-summary     ← 로봇 현황 카드
   GET .../dashboard/congestion        ← 혼잡도 차트
```

---

## Swagger UI

전체 API 테스트: `http://localhost:8080/swagger-ui.html`
