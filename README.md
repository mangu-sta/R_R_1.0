# 🎮 ROGUE & RELOAD

<div align="center">

### 실시간 멀티플레이 전투 기반 웹 게임

프론트엔드와 백엔드를 분리하여 구성한  
**실시간 멀티플레이 전투 중심 웹 게임 프로젝트**입니다.

로그인과 캐릭터 생성부터 로비/파티,  
실시간 전투, 스테이지 진행, 저장 및 랭킹까지의 흐름을 구현했습니다.

</div>

---

## 프로젝트 정보

- **프로젝트명**: ROGUE & RELOAD
- **프로젝트 유형**: 팀 프로젝트 (Frontend / Backend 협업)
- **배포 여부**: 미배포
- **한 줄 설명**: 실시간 멀티플레이 전투 기반 웹 게임
- **프로젝트 구조**: 프론트엔드(React + Phaser)와 백엔드(Spring Boot)가 분리된 실시간 멀티플레이 게임 구조
- **전체 흐름**: 로그인/캐릭터 생성 → 로비/파티 → 실시간 게임 → 전투/스테이지 진행 → 저장/랭킹

---

<a id="table-of-contents"></a>
## 목차

- [프로젝트 소개](#project-overview)
- [협업 방식](#collaboration)
- [주요 기능](#main-features)
- [시스템 아키텍처](#system-architecture)
- [기술 스택](#tech-stack)
- [프로젝트 구조](#project-structure)
- [API](#api)
- [개발자](#developer)

---

<a id="project-overview"></a>
## 프로젝트 소개

**ROGUE & RELOAD**는 실시간 멀티플레이 전투를 중심으로 설계한 웹 게임 프로젝트입니다.

여러 플레이어가 함께 로비와 파티를 구성하고, 게임 내에서 이동, 조준, 발사, 채팅, 상태 동기화를 수행할 수 있도록 구현했습니다.  
프로젝트는 로그인, 캐릭터 생성, 로비, 친구/알림, 실시간 전투, 스테이지 진행, 저장 및 랭킹 흐름으로 구성되어 있습니다.

프론트엔드는 **React + Phaser** 기반으로 구성되어 있으며, 백엔드는 **Spring Boot** 기반으로 분리하여 구현했습니다.  
본 프로젝트는 Frontend와 Backend를 분리하여 2인 팀으로 협업하여 개발했습니다.
실시간 통신은 **WebSocket(STOMP)** 와 **SSE**를 사용하고, 상태 관리와 동기화를 위해 **Redis**를 활용했습니다.

---

<a id="collaboration"></a>
## 🤝 협업 방식

- Frontend / Backend 역할 분리 개발
- REST API 및 WebSocket 인터페이스 설계 후 연동
- 실시간 게임 동기화를 위한 데이터 구조 협의

---

<a id="main-features"></a>
## 주요 기능

### 1. 회원 기능
- 회원가입
- 로그인
- 로그아웃
- 토큰 검증
- 토큰 재발급
- 사용자 삭제
- 키 설정 저장

### 2. 캐릭터 기능
- 직업 기반 캐릭터 생성
- 내 캐릭터 조회
- 다른 유저 캐릭터 조회

### 3. 로비 기능
- 개인 로비/파티 조회
- 파티 참가
- 초대 수락
- 파티 나가기
- 강퇴
- 게임 시작 요청/응답

### 4. 친구 기능
- 친구 목록 조회
- 친구 요청 보내기
- 받은 요청 조회
- 보낸 요청 조회
- 친구 요청 수락/거절/취소
- 친구 삭제
- 차단/차단 해제

### 5. 알림 기능
- SSE 기반 실시간 알림 구독
- 친구 요청/수락/거절/취소 알림
- 로비 초대 알림

### 6. 실시간 게임 기능
- WebSocket(STOMP) 기반 입장
- 이동
- 회전
- 조준
- 발사
- 채팅
- 상태 동기화

### 7. 전투 기능
- 몬스터 피격 처리
- 플레이어/몬스터 HP 갱신
- 레벨업 후 스탯 선택

### 8. 스테이지 기능
- 튜토리얼/메인 스테이지 전환
- 스테이지 이동 요청/응답

### 9. 저장/랭킹 기능
- 게임 저장
- 보스 클리어 랭킹 조회

### 10. 게임 시스템
- 몬스터 AI
- 몬스터 스폰
- 경로 탐색(A*)
- 시야 처리
- 스태미나 시스템
- 파티 플레이어 동기화

---

<a id="system-architecture"></a>
## 시스템 아키텍처

## 시스템 아키텍처

본 프로젝트는 React + Phaser 기반 프론트엔드와  
Spring Boot 백엔드를 분리한 구조로 설계되었으며,  
실시간 처리를 위해 WebSocket(STOMP)과 Redis를 활용했습니다.

![시스템 아키텍처](./AT2.png)
<!-- architecture image placeholder -->

### 구조 설명
- **Frontend**는 React 기반 UI와 Phaser 기반 게임 씬으로 구성됩니다.
- **Backend**는 인증, 로비, 전투, 게임 상태 처리 등 도메인 로직을 담당합니다.
- **MariaDB**는 영속 데이터 저장을 담당하고, **Redis**는 실시간 상태 및 세션성 데이터를 처리합니다.
- **WebSocket(STOMP)** 는 실시간 게임 플레이와 상태 브로드캐스트에 사용되며, **SSE**는 알림 전달에 사용됩니다.

---

<a id="tech-stack"></a>
## 기술 스택

### Frontend

![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?style=flat-square&logo=vite&logoColor=white)
![Phaser](https://img.shields.io/badge/Phaser-3-2B2E3A?style=flat-square&logo=phaser&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router-7-CA4245?style=flat-square&logo=reactrouter&logoColor=white)

![Axios](https://img.shields.io/badge/Axios-1-5A29E4?style=flat-square&logo=axios&logoColor=white)
![SockJS](https://img.shields.io/badge/SockJS-Client-000000?style=flat-square)
![STOMP](https://img.shields.io/badge/STOMP-WebSocket-010101?style=flat-square)
![Zustand](https://img.shields.io/badge/Zustand-Store-443E38?style=flat-square)

![React Toastify](https://img.shields.io/badge/React_Toastify-11-FF6C37?style=flat-square)
![SweetAlert2](https://img.shields.io/badge/SweetAlert2-11-FF5A5F?style=flat-square)
![Konva](https://img.shields.io/badge/Konva-Canvas-0D83CD?style=flat-square)
![React Konva](https://img.shields.io/badge/React_Konva-19-0D83CD?style=flat-square)

### Backend

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring WebSocket](https://img.shields.io/badge/Spring_WebSocket-WebSocket-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)

![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring JDBC](https://img.shields.io/badge/Spring_JDBC-JDBC-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Data Redis](https://img.shields.io/badge/Spring_Data_Redis-Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-jjwt-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-Annotation-BC4521?style=flat-square)

### Database / Infra

![MariaDB](https://img.shields.io/badge/MariaDB-11.4-003545?style=flat-square&logo=mariadb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Latest-DC382D?style=flat-square&logo=redis&logoColor=white)

---

<a id="project-structure"></a>
## 프로젝트 구조

### 루트 구조

```text
ROGUE & RELOAD
├─ Front_demo_demo/        # React + Phaser 프론트엔드
├─ backend/RR/             # Spring Boot 백엔드
├─ docker/                 # MariaDB / Redis 실행용 Docker 설정
├─ schema.sql              # DB 스키마 관련 SQL
├─ DB_B_RR.sql             # DB 데이터 관련 SQL
├─ architecture.md         # 시스템 구조 문서
└─ erd.md                  # ERD 문서
```

### 프론트 핵심 구조

```text
Front_demo_demo/
├─ src/
│  ├─ components/          # 로그인, 회원가입, 캐릭터 생성, 로비, 친구창, 옵션, 튜토리얼, 게임 화면
│  ├─ game/
│  │  ├─ scenes/           # 게임 씬
│  │  ├─ systems/          # 이동, 전투, 몬스터, 카메라, 시야 등 시스템
│  │  ├─ network/          # WebSocket 연결 및 메시지 처리
│  │  └─ utils/            # 사운드, 키맵핑 등 유틸리티
│  ├─ css/                 # 화면별 스타일
│  └─ assets/              # 프론트 내부 에셋
├─ public/assets/          # 이미지, 맵 JSON, 정적 리소스
└─ package.json
```

- `src/components/`는 화면 단위 UI를 구성합니다.
- `src/game/`는 Phaser 기반 실시간 게임 로직을 담당합니다.
- `public/assets/`, `src/assets/`에는 이미지, 맵 데이터, 사운드 리소스가 포함됩니다.

### 백엔드 핵심 구조

```text
backend/RR/
├─ src/main/java/com/release/rr/
│  ├─ domain/
│  │  ├─ user/             # 인증, 사용자 관련
│  │  ├─ characters/       # 캐릭터 관련
│  │  ├─ lobby/            # 로비, 파티 관련
│  │  ├─ friends/          # 친구 관련
│  │  ├─ friend_requests/  # 친구 요청 관련
│  │  ├─ notifications/    # SSE 알림 관련
│  │  ├─ map/              # 맵, 이동, 스테이지 관련
│  │  ├─ combat/           # 전투 처리 관련
│  │  ├─ monster/          # 몬스터 AI, 스폰 관련
│  │  ├─ game/             # 게임 진행 및 저장 관련
│  │  └─ rank/             # 랭킹 관련
│  └─ global/
│     ├─ security/         # 보안, JWT 처리
│     ├─ websocket/        # WebSocket 설정
│     ├─ redis/            # Redis DAO 및 DTO
│     ├─ exception/        # 공통 예외 처리
│     └─ response/         # 공통 응답 구조
└─ src/main/resources/
   ├─ map/                 # 장애물 및 맵 JSON
   └─ static/              # 테스트용 HTML
```

- `domain/`은 비즈니스 도메인 기준으로 기능을 분리한 영역입니다.
- `global/`은 인증, 실시간 통신 설정, Redis 처리, 예외 처리 등 공통 인프라를 담당합니다.
- `resources/map/`은 게임 맵 데이터, `resources/static/`은 테스트용 정적 파일을 포함합니다.

### 대표 진입점

```text
Frontend Routing : src/main.jsx
Frontend App     : src/App.jsx
Backend App      : backend/RR/src/main/java/com/release/rr/RrApplication.java
```

---

<a id="api"></a>
## API

### REST API

#### 인증 / 사용자
- `POST /api/signup`
- `POST /api/signin`
- `POST /api/auth/refresh`
- `POST /api/signout`
- `POST /api/auth/verify`
- `DELETE /api/user/{userId}`
- `POST /api/key-config`

#### 캐릭터
- `POST /api/character/create`
- `POST /api/character`

#### 로비 / 파티
- `POST /api/lobby/me`
- `POST /api/lobby/join`
- `POST /api/lobby/invite`
- `POST /api/lobby/invite/accept`
- `POST /api/lobby/leave`
- `POST /api/lobby/{mapNanoId}/start-request`
- `POST /api/lobby/{nanoId}/start-response`

#### 친구
- `POST /api/friends`
- `POST /api/friends/delete`
- `POST /api/friends/block`
- `POST /api/friends/unblock`

#### 친구 요청
- `POST /api/friend-request/send`
- `POST /api/friend-request/received`
- `POST /api/friend-request/sents`
- `POST /api/friend-request/accept`
- `POST /api/friend-request/reject`
- `POST /api/friend-request/cancel`

#### 알림
- `GET /api/notifications/subscribe`
- `GET /api/notifications`
- `DELETE /api/notifications`

#### 게임
- `POST /api/game/save`
- `POST /api/game/{nanoId}/stage-advance/request`
- `POST /api/game/{nanoId}/stage-advance/response`

#### 랭킹
- `GET /api/rank/boss`

#### 테스트성 API
- `GET /api/me`
- `GET /api/token`
- `DELETE /character/{id}`
- `GET /redis-test/set`
- `GET /redis-test/get`

### WebSocket / STOMP

#### 엔드포인트
- `WS /ws/lobby + SockJS`

#### 클라이언트 송신 (`/app`)
- `/app/lobby/{roomId}/chat`
- `/app/game/{nanoId}/join`
- `/app/game/{nanoId}/move`
- `/app/game/angle`
- `/app/game/{nanoId}/rotate`
- `/app/game/{nanoId}/fire`
- `/app/game/{nanoId}/monster-hit`
- `/app/game/{nanoId}/stat-select`

#### 서버 브로드캐스트 (`/topic`)
- `/topic/lobby/{roomId}`
- `/topic/game/{nanoId}`

---

<a id="developer"></a>
## 👨‍💻 개발자

| 이름 | 역할 | 담당 영역 |
|---|---|---|
| 최명기 | Backend | API, WebSocket, 게임 로직, Redis |
| 정광성 | Frontend | UI, Phaser 게임 구현, 상태 관리 | 
