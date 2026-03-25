# Napolme Backend

> 아이온2 캐릭터 정보 포털 [napolme.com](https://napolme.com)의 백엔드 API 서버

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Database | MySQL 8.0, Redis |
| Security | Spring Security, IP Auto-Block, Rate Limiting |
| Infra | AWS EC2 + ECR, Docker, GitHub Actions CI/CD |
| Build | Gradle |

## 주요 기능

- **캐릭터 조회** — PlayNC 공식 API 연동으로 캐릭터 스탯, 장비, 다에바니온 정보 수집
- **전투력 계산** — 스탯 기반 전투력 점수 산출 및 다중 캐릭터 비교
- **랭킹 시스템** — 일일 인기 검색 랭킹, 전투력 종족별 TOP 5
- **공지 게시판** — PlayNC 공식 패치노트 연동 + Napolme 자체 업데이트 관리
- **닉네임 생성기** — 게임 스타일 랜덤 닉네임 생성
- **스트리밍 연동** — Chzzk/트위치 아이온2 방송 조회
- **보안** — 토큰 버킷 Rate Limiting, IP 자동 차단, 요청 로깅 및 핑거프린팅

## 프로젝트 구조

```
src/main/java/com/dev/napolme/
├── controller/         # REST API 엔드포인트
│   ├── character/      # 캐릭터 검색/조회
│   ├── combat/         # 전투력 계산
│   ├── board/          # 게시판
│   ├── stat/           # 통계/랭킹
│   └── nickname/       # 닉네임 생성
├── service/            # 비즈니스 로직
├── domain/             # JPA 엔티티
├── repository/         # 데이터 접근 계층
├── infra/plaync/       # PlayNC 외부 API 클라이언트
├── security/           # Spring Security, IP 자동 차단
├── rateLimit/          # Redis 기반 Rate Limiting
├── logging/            # 요청 로깅 필터
└── cache/              # 캐릭터 캐싱 레이어
```

## API 엔드포인트

### 캐릭터

```
GET  /api/character/search                    # 이름/서버 검색
GET  /api/character/info                      # 캐릭터 스탯 조회
GET  /api/character/equipment                 # 장비 조회
GET  /api/character/equipment/bundle          # 장비 + 아이템 상세
GET  /api/character/daevanion/bundle          # 다에바니온 상세
POST /api/characters/fetch                    # 캐릭터 저장
POST /api/characters/fetch-by-ref             # 레퍼런스로 저장 (60s 쿨다운)
POST /api/characters/{id}/refresh             # 캐릭터 갱신
```

### 전투력

```
GET  /api/combat-score                        # 캐릭터 전투력 조회
POST /api/combat-score                        # 스탯으로 전투력 계산
POST /api/combat-score/compare                # 다중 전투력 비교
```

### 통계 / 랭킹

```
GET  /api/stat/daily-search-ranking           # 오늘의 인기 캐릭터 TOP 10
GET  /api/stat/napolme-ranking                # 종족별 전투력 랭킹 TOP 5
GET  /api/stat/chzzk-lives                    # 아이온2 생방송 TOP 6
```

### 게시판

```
GET  /api/board/updates                       # 공식 패치노트
GET  /api/board/napolme-updates               # Napolme 공지
POST /api/board/napolme-updates               # 공지 작성 (허용 IP만)
```

## 보안

- **Rate Limiting** — Redis 토큰 버킷 (기본: 10 req/s, burst 20)
- **IP 자동 차단** — 고장률/패턴 반복/비정상 UA 감지 시 30분 차단
- **요청 로깅** — UUID 요청 ID, 클라이언트 IP 추출 (Cloudflare CF-Connecting-IP 지원), 익명 쿠키 핑거프린팅
- **CORS** — `napolme.com`, `www.napolme.com`, `localhost:5173` 허용


### 로컬 개발

```bash
# 빌드 (테스트 제외)
./gradlew build -x test

# 실행
./gradlew bootRun
```

### Docker

```bash
docker build -t napolme-backend .
docker run -p 8080:8080 --env-file .env napolme-backend
```

## CI/CD

`main` 브랜치에 push 시 자동 배포:

1. GitHub Actions 트리거
2. Docker 이미지 빌드
3. AWS ECR 푸시
4. EC2 SSH 접속 → 기존 컨테이너 교체 → 새 컨테이너 실행

## DB 스키마 (주요 테이블)

| 테이블 | 설명 |
|--------|------|
| `saved_character` | 저장된 캐릭터 프로필 (서버ID+캐릭터ID 유니크) |
| `napolme_updates` | Napolme 자체 공지사항 |
| `request_logs` | 전체 API 요청 감사 로그 (IP, UA, 응답시간 등) |

라이선스
This project is private and proprietary. All rights reserved.

Copyright © 2026 Napolme. Unauthorized copying, distribution, or modification is strictly prohibited.