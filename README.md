# 🎮 Log & Reload

<div align="center">

### 실시간 멀티플레이 전투 기반 웹 게임

React + Phaser 기반 프론트엔드와 Spring Boot 백엔드를 분리하여  
로비/파티 시스템부터 실시간 전투, 스테이지 진행, 저장 및 랭킹까지 구현한 개인 프로젝트입니다.

</div>

---

## 📌 프로젝트 정보

| 항목 | 내용 |
|---|---|
| 프로젝트명 | Log & Reload |
| 프로젝트 유형 | 개인 프로젝트 |
| 한 줄 설명 | 실시간 멀티플레이 전투 기반 웹 게임 |
| 구조 | React + Phaser + Spring Boot 분리형 아키텍처 |
| 배포 여부 | 미배포 |

---

## 🔎 목차

- [프로젝트 소개](#intro)
- [주요 기능](#features)
- [시스템 아키텍처](#architecture)
- [기술 스택](#skills)
- [프로젝트 구조](#structure)
- [API](#api)
- [개발자](#developer)

---

<a name="intro"></a>

## 📝 프로젝트 소개

Log & Reload는 실시간 멀티플레이 환경에서  
여러 플레이어가 함께 전투를 진행할 수 있도록 설계된 웹 게임입니다.

기존 웹 기반 게임에서 부족했던  
👉 실시간 동기화  
👉 파티 기반 플레이  
👉 상태 공유  

를 개선하기 위해 제작되었습니다.

전체 흐름은 다음과 같습니다:

👉 로그인 / 캐릭터 생성  
→ 로비 / 파티 구성  
→ 실시간 게임 입장  
→ 전투 및 스테이지 진행  
→ 저장 및 랭킹 반영

---

<a name="features"></a>

## ✨ 주요 기능

### 🔐 회원 기능
- 회원가입 / 로그인 / 로그아웃
- JWT 기반 인증 및 토큰 재발급
- 사용자 삭제 및 키 설정 저장

---

### 🧑 캐릭터 시스템
- 직업 기반 캐릭터 생성
- 내 캐릭터 / 다른 유저 캐릭터 조회

---

### 🏠 로비 & 파티 시스템
- 개인 로비 및 파티 조회
- 파티 초대 / 수락 / 거절
- 파티 나가기 / 강퇴
- 게임 시작 요청 및 응답

---

### 🤝 친구 시스템
- 친구 목록 조회
- 친구 요청 / 수락 / 거절 / 취소
- 친구 삭제 및 차단 / 해제

---

### 🔔 실시간 알림
- SSE 기반 알림 구독
- 친구 요청 / 수락 / 로비 초대 알림

---

### 🎮 실시간 게임
- WebSocket(STOMP) 기반 동기화
- 이동 / 회전 / 조준 / 발사
- 채팅 및 상태 공유

---

### ⚔ 전투 시스템
- 몬스터 피격 처리
- 플레이어 및 몬스터 HP 동기화
- 레벨업 및 스탯 선택

---

### 🗺 스테이지 시스템
- 튜토리얼 / 메인 스테이지 전환
- 스테이지 이동 요청 및 응답

---

### 🏆 저장 & 랭킹
- 게임 진행 데이터 저장
- 보스 클리어 랭킹 조회

---

### 🤖 게임 시스템
- 몬스터 AI
- 스폰 및 경로 탐색 (A*)
- 시야 및 스태미나 관리
- 파티 플레이어 상태 동기화

---

<a name="architecture"></a>

## 🏗 시스템 아키텍처

```text
[ React + Phaser Frontend ]
            ↓
[ Spring Boot Backend ]
            ↓
[ MariaDB / Redis ]
            ↓
[ WebSocket(STOMP) / SSE ]
<!-- 추후 아키텍처 이미지 추가 예정 -->

<a name="skills"></a>

🛠 기술 스택
🎨 Frontend
<p> <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black"/> <img src="https://img.shields.io/badge/Vite-7-646CFF?style=for-the-badge&logo=vite&logoColor=white"/> <img src="https://img.shields.io/badge/Phaser-3-0A0A0A?style=for-the-badge"/> <img src="https://img.shields.io/badge/React_Router-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white"/> <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge"/> <img src="https://img.shields.io/badge/SockJS-STOMP-000000?style=for-the-badge"/> <img src="https://img.shields.io/badge/Zustand-State-000000?style=for-the-badge"/> <img src="https://img.shields.io/badge/React_Toastify-FF6F61?style=for-the-badge"/> <img src="https://img.shields.io/badge/SweetAlert2-FF69B4?style=for-the-badge"/> <img src="https://img.shields.io/badge/Konva-Canvas-333333?style=for-the-badge"/> </p>
⚙ Backend
<p> <img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/WebSocket-STOMP-6DB33F?style=for-the-badge"/> <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge"/> <img src="https://img.shields.io/badge/JPA-6DB33F?style=for-the-badge"/> <img src="https://img.shields.io/badge/JDBC-6DB33F?style=for-the-badge"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"/> <img src="https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge"/> <img src="https://img.shields.io/badge/Lombok-BC4521?style=for-the-badge"/> </p>
🗄 Database / Infra
<p> <img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"/> <img src="https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white"/> </p>

<a name="structure"></a>

📂 프로젝트 구조
Root
├─ Front_demo_demo/        # React + Phaser 프론트엔드
│  ├─ components/          # UI 컴포넌트
│  ├─ game/                # 게임 로직 (Phaser)
│  │  ├─ scenes/
│  │  ├─ systems/
│  │  ├─ network/
│  │  └─ utils/
│  ├─ assets/              # 이미지, 맵, 사운드
│
├─ backend/RR/             # Spring Boot 백엔드
│  ├─ domain/              # 도메인 (user, lobby, combat 등)
│  ├─ global/              # 공통 설정 (보안, JWT, Redis 등)
│  └─ resources/
│
├─ docker/                 # DB/Redis 실행
├─ schema.sql              # DB 구조
├─ DB_B_RR.sql             # 데이터

<a name="api"></a>

🔌 API
인증/사용자

POST /api/signup

POST /api/signin

POST /api/auth/refresh

POST /api/signout

캐릭터

POST /api/character/create

POST /api/character

로비/파티

POST /api/lobby/join

POST /api/lobby/invite

POST /api/lobby/leave

친구

POST /api/friends

POST /api/friends/delete

알림

GET /api/notifications/subscribe

게임

POST /api/game/save

POST /api/game/{nanoId}/stage-advance/request

WebSocket

/ws/lobby

/app/game/{nanoId}/move

/topic/game/{nanoId}

<a name="developer"></a>

👨‍💻 개발자
이름	역할
망구	Full Stack / 개인 프로젝트
