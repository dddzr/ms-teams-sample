# MS Teams API 테스트 프로젝트
Microsoft Teams Graph API를 학습하고 테스트하기 위한 Spring Boot 프로젝트입니다.

## 📋 프로젝트 구성

### 기술 스택
- **Backend**: Spring Boot 3.5.7, Java 17
- **Frontend**: Thymeleaf, HTML/CSS/JavaScript
- **API**: Microsoft Graph API, Azure AD OAuth 2.0
- **Libraries**: 
  - Microsoft Graph SDK 6.54.0
  - Azure Identity 1.18.1
  - OkHttp 4.12.0
  - Lombok

### 주요 기능
- ✅ Azure AD OAuth 2.0 인증
- ✅ 사용자 정보 조회
- ✅ 소속 Teams 목록 조회
- ✅ Team 채널 목록 조회
- ✅ 채널 메시지 조회

## 🚀 시작하기

### 1. Azure AD 앱 등록
Microsoft Teams API를 사용하려면 먼저 Azure AD에 앱을 등록해야 합니다.

#### 단계:

1. **Azure Portal 접속**
   - https://portal.azure.com 접속
   - Microsoft Entra ID(이전 Azure Active Directory)로 이동

2. **앱 등록**
   - "앱 등록" > "새 등록" 클릭
   - 이름: 원하는 앱 이름 입력 (예: "Teams API Test")
   - 지원되는 계정 유형: "이 조직 디렉터리만의 계정" 선택
   - 리디렉션 URI: 
     - 플랫폼: Web
     - URI: `http://localhost:8080/callback`
   - "등록" 클릭

3. **API 권한 설정**
   - "API 권한" 메뉴로 이동
   - "권한 추가" 클릭
   - "Microsoft Graph" 선택
   - "위임된 권한" 선택
   - 다음 권한들을 추가:
     - `User.Read` - 사용자 프로필 읽기
     - `Team.ReadBasic.All` - Teams 기본 정보 읽기
     - `Channel.ReadBasic.All` - 채널 기본 정보 읽기
     - `ChannelMessage.Read.All` - 채널 메시지 읽기
     - `Chat.Read` - 채팅 읽기
   - "권한 추가" 클릭
   - **중요**: "관리자 동의 허용" 버튼 클릭 (관리자 권한 필요)

4. **클라이언트 시크릿 생성**
   - "인증서 및 비밀" 메뉴로 이동
   - "새 클라이언트 암호" 클릭
   - 설명 입력 (예: "Test Secret")
   - 만료 기간 선택
   - "추가" 클릭
   - ⚠️ **중요**: 생성된 "값"을 즉시 복사! (다시 볼 수 없음)

5. **앱 정보 확인**
   - "개요" 메뉴에서 다음 정보 확인:
     - 애플리케이션(클라이언트) ID
     - 디렉터리(테넌트) ID

### 2. 프로젝트 설정

#### application.properties 수정

`src/main/resources/application.properties` 파일을 열어 Azure AD 정보를 입력:

```properties
# Azure AD Configuration
azure.client-id=YOUR_CLIENT_ID          # 애플리케이션(클라이언트) ID
azure.client-secret=YOUR_CLIENT_SECRET  # 클라이언트 시크릿 값
azure.tenant-id=YOUR_TENANT_ID          # 디렉터리(테넌트) ID
azure.redirect-uri=http://localhost:8080/callback
# azure.scope=User.Read,Team.ReadBasic.All,Channel.ReadBasic.All,ChannelMessage.Read.All,Chat.Read
azure.scope=https://graph.microsoft.com/.default offline_access
```

### 3. 프로젝트 실행

#### Maven을 사용한 실행

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

또는 IDE에서 `TeamsApplication.java`를 실행.

### 4. 애플리케이션 사용

1. 브라우저에서 `http://localhost:8080` 접속
2. "Microsoft 계정으로 로그인" 버튼 클릭
3. Microsoft 계정으로 로그인
4. 권한 동의 화면에서 "동의" 클릭
5. 대시보드에서 Teams API 테스트!

## 📁 프로젝트 구조

```
src/main/java/com/example/teams/
├── config/
│   └── AzureAdConfig.java           # Azure AD 설정
├── controller/
│   ├── HomeController.java          # 메인 페이지
│   ├── AuthController.java          # OAuth 인증 처리
│   ├── DashboardController.java     # 대시보드
│   └── TeamsController.java         # Teams API 엔드포인트
├── dto/
│   ├── TeamDto.java                 # Team 데이터 모델
│   ├── ChannelDto.java              # Channel 데이터 모델
│   ├── UserDto.java                 # User 데이터 모델
│   └── MessageDto.java              # Message 데이터 모델
├── service/
│   ├── AuthService.java             # 인증 서비스
│   └── TeamsService.java            # Teams API 서비스
└── TeamsApplication.java            # 메인 애플리케이션

src/main/resources/
├── templates/
│   ├── index.html                   # 로그인 페이지
│   └── dashboard.html               # 대시보드 페이지
└── application.properties           # 설정 파일
```

## 🔧 API 엔드포인트

### 인증
- `GET /` - 메인 페이지 (로그인)
- `GET /callback` - OAuth 콜백
- `GET /logout` - 로그아웃

### Teams API
- `GET /api/me` - 현재 사용자 정보
- `GET /api/teams` - 소속 Teams 목록
- `GET /api/teams/{teamId}/channels` - Team의 채널 목록
- `GET /api/teams/{teamId}/channels/{channelId}/messages` - 채널 메시지 목록

### 대시보드
- `GET /dashboard` - API 테스트 대시보드

## 🎯 사용 방법

### 1. 사용자 정보 조회
대시보드에서 "사용자 정보 조회" 버튼을 클릭하면 현재 로그인한 사용자의 정보를 확인할 수 있습니다.

### 2. Teams 목록 조회
"Teams 목록 조회" 버튼을 클릭하면 사용자가 속한 모든 Teams를 확인할 수 있습니다. 
각 Team 항목을 클릭하면 Team ID가 자동으로 입력됩니다.

### 3. 채널 목록 조회
Team ID를 입력하고 "채널 조회" 버튼을 클릭하면 해당 Team의 채널 목록을 확인할 수 있습니다.

### 4. 메시지 조회
Team ID와 Channel ID를 입력하고 "메시지 조회" 버튼을 클릭하면 해당 채널의 최근 메시지 20개를 확인할 수 있습니다.

## ⚠️ 주의사항

1. **권한 문제**
   - Teams 메시지를 읽으려면 조직의 관리자가 API 권한을 승인해야 합니다
   - 일부 기능은 관리자 권한이 필요할 수 있습니다

2. **Rate Limiting**
   - Microsoft Graph API에는 사용량 제한이 있습니다
   - 과도한 요청은 제한될 수 있습니다

3. **보안**
   - `application.properties`의 시크릿 정보를 Git에 커밋하지 마세요
   - 프로덕션 환경에서는 환경변수나 보안 저장소를 사용하세요

4. **세션 관리**
   - 현재 Access Token은 세션에 저장됩니다
   - 프로덕션 환경에서는 Refresh Token을 사용한 자동 갱신을 구현하세요

## 📚 참고 자료

- [Microsoft Graph API 문서](https://docs.microsoft.com/graph/)
- [Microsoft Teams API 문서](https://docs.microsoft.com/graph/api/resources/teams-api-overview)
- [Azure AD 인증 문서](https://docs.microsoft.com/azure/active-directory/develop/)
- [Microsoft Graph SDK for Java](https://github.com/microsoftgraph/msgraph-sdk-java)

## 🐛 트러블슈팅

### 인증 실패
- Azure Portal에서 리디렉션 URI가 정확히 `http://localhost:8080/callback`로 설정되어 있는지 확인
- Client ID, Client Secret, Tenant ID가 올바른지 확인

### API 호출 실패
- API 권한이 올바르게 설정되어 있는지 확인
- 관리자 동의가 완료되었는지 확인
- 로그를 확인하여 구체적인 오류 메시지 확인 (`logging.level.com.example.teams=DEBUG`)

### Teams 목록이 비어있음
- 사용자가 실제로 Teams에 속해있는지 확인
- Microsoft Teams 앱에서 확인 후 다시 시도

## 📝 라이선스
이 프로젝트는 학습 및 테스트 목적으로 만들어졌습니다.

## 👨‍💻 개발자
dddzr