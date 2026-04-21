# MyAppDev 작업 내역 정리

## 프로젝트 개요
스터디 플랫폼 Android 앱(MyAppDev)
최근 작업은 앱 브랜딩 정리, 스터디 참여 안정화, 마이페이지 최신화, 게시판 UX/시각 개선 중심으로 진행됨.

  ---

## 최근 주요 변경 사항

### 1. 앱 이름(런처 라벨) 변경
- 앱 아이콘 아래 이름을 `스터디 허브`로 통일
- 수정 파일
    - `app/src/main/res/values/strings.xml`
    - `app/src/main/res/values-ko/strings.xml`

### 2. 일정 추가 다이얼로그 UI/입력 개선
- `스터디/일시` 섹션 정렬 우측 이동 및 레이아웃 기준선 정리
- 하드코딩 문자열 리소스화
- 저장 중 중복 클릭 방지
- 과거 시각 등록 방지(신규 등록)
- 인증 누락/실패 안내 메시지 보강
- 접근성 개선(FAB contentDescription)
- 관련 파일
    - `app/src/main/res/layout/dialog_schedule_editor.xml`
    - `app/src/main/res/layout/fragment_calendar.xml`
    - `app/src/main/java/kr/ac/mjc/myappdev/calendar/CalendarFragment.java`
    - `app/src/main/res/values/strings.xml`
    - `app/src/main/res/values-ko/strings.xml`
    - `app/src/main/res/values/dimens.xml`

### 3. 마이페이지 프로필 수정 버튼 스타일 정리
- 흰색 배경 버튼을 화면 톤과 맞는 색상으로 변경
- 수정 파일
    - `app/src/main/res/layout/fragment_my_page.xml`

### 4. 스터디 참여 실패 이슈 해결
- 증상: 다른 계정 참여 시 실패
- 원인:
    - 사용자 문서(`users/{uid}`) 없을 때 `update` 실패
    - Firestore 트랜잭션 내 read/write 순서 위반 가능성
- 해결:
    - 사용자 문서 없는 경우 `set(..., merge)`로 안전 생성/갱신
    - 트랜잭션 전체를 **읽기 먼저, 쓰기 나중** 순서로 재구성
- 수정 파일
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyDetailActivity.java`

### 5. 참여자 목록 UID 노출 제거
- 참여자 메타에서 UID(영문 긴 문자열) 제거
- 닉네임 없을 때 `이름 미설정` 표시
- 수정 파일
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyMemberAdapter.java`
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyDetailActivity.java`

### 6. 마이페이지 참여 중 최신화 개선
- 증상: 상단 참여 수/하단 참여 목록 최신화 불안정
- 해결:
    - `users/{uid}.joinedStudyIds` 실시간 리스너 기반 동기화
    - 요청 토큰으로 오래된 응답 덮어쓰기 방지
    - 실제 `memberUids` 교차검증으로 데이터 정합성 보완
- 수정 파일
    - `app/src/main/java/kr/ac/mjc/myappdev/mypage/MyPageFragment.java`

### 7. 마이페이지 리스트 표시/스크롤 개선
- 증상: 하단 참여 목록이 1개만 보이는 문제
- 해결:
    - `ScrollView` → `NestedScrollView`
    - 내부 `RecyclerView` 비스크롤 확장 레이아웃매니저 적용
- 수정 파일
    - `app/src/main/res/layout/fragment_my_page.xml`
    - `app/src/main/java/kr/ac/mjc/myappdev/mypage/MyPageFragment.java`

### 8. 스터디 게시판 스크롤/체감 개선
- `StudyAdapter`를 `ListAdapter + DiffUtil`로 전환
- RecyclerView 스크롤 튜닝 (`hasFixedSize`, cache, change animation 최소화)
- 카드 진입 애니메이션(stagger + fade/translate) 추가
- 수정 파일
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyAdapter.java`
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyBoardFragment.java`
    - `app/src/main/res/layout/fragment_study_board.xml`
    - `app/src/main/res/anim/study_board_item_enter.xml`
    - `app/src/main/res/anim/study_board_layout_animation.xml`

### 9. 게시판 카테고리 색상 구분 강화
- 모집 상태/스터디 유형/지역을 색상으로 구분
- 빠른 필터 칩(`모집 중`, `온라인`, `코딩`)에 고유 팔레트 적용
- 수정 파일
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyAdapter.java`
    - `app/src/main/java/kr/ac/mjc/myappdev/board/StudyBoardFragment.java`
    - `app/src/main/res/values/colors.xml`

  ---

## 빌드/설치 확인
- `./gradlew :app:assembleDebug` 성공
- `./gradlew installDebug` 성공 (에뮬레이터 설치 확인)

  ---

## 다음 개선 후보
1. 문자열 하드코딩 전면 리소스화(국제화 대비)
2. Firestore write 실패 사유별 사용자 메시지 분리
3. UI 계층별 스타일 가이드 정리(색상/간격/타이포)