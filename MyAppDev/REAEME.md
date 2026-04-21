# Study Hub (스터디 허브)

스터디 모집, 참여, 채팅, 일정 관리를 한 번에 제공하는 Android 앱입니다.
Firebase 기반 실시간 데이터 동기화와 직관적인 UI로 스터디 운영/참여 흐름을 단순화했습니다.

## 프로젝트 개요

- 앱 이름: 스터디 허브 (Study Hub)
- 플랫폼: Android (Java)
- 아키텍처: Fragment 기반 탭 구조 (게시판 / 채팅 / 캘린더 / 마이페이지)
- 백엔드: Firebase (Auth, Firestore, Storage)

## 주요 기능

- 회원가입 / 로그인 / 프로필 설정
- 스터디 게시판
- 스터디 생성 / 수정 / 삭제
- 스터디 참여 / 참여자 목록 확인
- 스터디 상세 보기 및 상태(모집중/모집완료) 표시
- 실시간 채팅방 목록 및 채팅 메시지
- 캘린더 일정 추가 / 수정 / 삭제
- 날짜별 일정 조회 및 월별 일정 날짜 표시
- 마이페이지
- 내가 만든 스터디 목록
- 참여 중인 스터디 목록
- 상단 통계(만든 스터디/참여 중) 실시간 반영
- 로그아웃 / 프로필 수정

## UI/UX 개선 사항

- Warm Scholar 디자인 컨셉 적용
- 오프화이트 베이스 + 딥 네이비 포인트 컬러
- 테두리 기반 카드 스타일(그림자 최소화)
- 하단 내비게이션 가독성 개선
- 게시판/캘린더/마이페이지 스크롤 자연스러움 개선
- 카테고리/상태별 색상 구분 강화

## 기술 스택

- Language: Java
- UI: Material Components, RecyclerView, ViewBinding
- Backend: Firebase Authentication, Cloud Firestore, Firebase Storage
- Build: Gradle (Android Studio)

## 프로젝트 구조

- `app/src/main/java/kr/ac/mjc/myappdev`
- `auth` 로그인/회원가입/프로필 설정
- `board` 스터디 게시판/상세/생성/참여 로직
- `chat` 채팅방 목록/채팅 화면
- `calendar` 일정 관리
- `mypage` 마이페이지/프로필 수정
- `model` 데이터 모델
- `util` Firebase 공통 유틸
- `app/src/main/res/layout` 화면 XML
- `app/src/main/res/drawable` 배경/컴포넌트 스타일
- `app/src/main/res/values` 컬러/테마/문자열 리소스

## Firestore 데이터 구조 (요약)

- `studyPosts`
- 스터디 기본 정보(제목, 설명, 분야, 지역, 모집상태, 정원 등)
- 멤버 UID 배열(`memberUids`)
- `studyPosts/{postId}/schedules`
- 스터디 일정(제목, 설명, 시간, 생성자 등)
- `chatRooms`, `messages`(구성에 따라)
- 채팅방/메시지 저장

## 실행 방법

1. Android Studio에서 프로젝트 열기
2. Firebase 프로젝트 연결 (`google-services.json` 설정)
3. Gradle Sync
4. 에뮬레이터 또는 실기기 실행

## 트러블슈팅 기록

- Firestore Transaction 오류
- 이슈: `Firestore transactions require all reads to be executed before all writes`
- 해결: 트랜잭션 내 Read/Write 순서 정리 (모든 조회 후 쓰기 실행)

- 캘린더 탭 진입 시 앱 종료
- 원인: 라이브러리 호환성 이슈
- 해결: 안정적인 캘린더 구성으로 교체 및 구조 재정비

- 마이페이지 참여 중 정보 최신화 지연
- 원인: 참여 데이터 반영 시점/조회 로직 분리
- 해결: 참여 직후 재조회 및 UI 갱신 흐름 보강

## 향후 개선 아이디어

- 푸시 알림(Firebase Cloud Messaging)
- 일정 리마인더 알림
- 검색/필터 고도화(복합 조건, 정렬 옵션)
- 이미지 업로드/미리보기 UX 개선
- 테스트 코드 및 CI 파이프라인 추가

## 라이선스

개인/학습용 프로젝트.