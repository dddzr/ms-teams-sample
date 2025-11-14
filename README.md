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
- ✅ Graph API 호출

## 🚀 시작하기

### 1. Azure AD 앱 등록
Microsoft Teams API를 사용하려면 먼저 Azure AD에 앱을 등록해야 합니다.

#### 단계:

1-1. **Azure Portal 접속**
   - https://portal.azure.com 접속
   - Microsoft Entra ID(이전 Azure Active Directory)로 이동

1-2. **앱 등록**
   - "앱 등록" > "새 등록" 클릭
   - 이름: 원하는 앱 이름 입력 (예: "Teams API Test")
   - 지원되는 계정 유형: "이 조직 디렉터리만의 계정" 선택
   - 리디렉션 URI: 
     - 플랫폼: Web
     - URI: `http://localhost:8080/callback`
   - "등록" 클릭

1-3. **API 권한 설정**
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

1-4. **클라이언트 시크릿 생성**
   - "인증서 및 비밀" 메뉴로 이동
   - "새 클라이언트 암호" 클릭
   - 설명 입력 (예: "Test Secret")
   - 만료 기간 선택
   - "추가" 클릭
   - ⚠️ **중요**: 생성된 "값"을 즉시 복사! (다시 볼 수 없음)

1-5. **앱 정보 확인**
   - "개요" 메뉴에서 다음 정보 확인:
     - 애플리케이션(클라이언트) ID
     - 디렉터리(테넌트) ID

### 2. 프로젝트 설정

#### 2-1. application.properties 수정

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

#### 2-2. SMAL 인증서 생성
우리 포탈이 IDP로 사용될 때 필요. ( ms에서 지원 안 함. 실제동작x )
1: 수동 생성: JAVA 코드 직접 실행
```
mvn compile exec:java -Dexec.mainClass="com.example.teams.util.CertificateGeneratorMain"
```bash
2. openSSL 이용

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
2. Microsoft 계정으로 로그인
3. Teams API 테스트!

## 📁 프로젝트 구조

```
src/main/java/com/example/teams/
├── auth/                            # 인증 관련
│   ├── config/                      # 인증 설정
│   │   ├── AppSamlConfig.java       # SAML 2.0 설정 (IdP: App)
│   │   ├── AppSamlInitializer.java  # SAML 초기화 (IdP: App)
│   │   └── AzureOAuthConfig.java    # OAuth 2.0 설정 (IdP: Azure)
│   ├── controller/                  # 인증 컨트롤러
│   │   ├── AppAuthController.java    # 앱 자체 로그인
│   │   ├── AppSamlController.java    # SAML 2.0 인증 (IdP: App)
│   │   ├── AzureAuthController.java  # Azure(Microsoft) 단독 로그인
│   │   ├── AzureOAuthController.java # OAuth 2.0 인증 (IdP: Azure)
│   │   └── CommonAuthController.java # 공통 인증
│   └── service/                     # 인증 서비스
│       ├── AppSamlService.java      # SAML 서비스 (IdP: App)
│       └── AzureOAuthService.java   # OAuth 서비스 (IdP: Azure)
├── controller/                      # 메인 컨트롤러
│   └── MainController.java          # 메인 페이지
├── ms/                              # Microsoft Graph API 관련
│   ├── controller/                  # MS API 컨트롤러
│   │   ├── TeamsController.java     # Teams API
│   │   ├── ChatController.java      # Chat API
│   │   ├── CalendarController.java  # Calendar API
│   │   ├── MeetingController.java   # Meeting API
│   │   └── AdminController.java     # Admin API
│   ├── service/                     # MS API 서비스
│   │   ├── TeamsService.java        # Teams 서비스
│   │   ├── ChatService.java         # Chat 서비스
│   │   ├── CalendarService.java     # Calendar 서비스
│   │   ├── MeetingService.java     # Meeting 서비스
│   │   └── GraphClientService.java  # Graph Client 서비스
│   ├── dto/                         # MS API DTO
│   │   ├── TeamDto.java             # Team 데이터 모델
│   │   ├── ChannelDto.java          # Channel 데이터 모델
│   │   ├── ChatDto.java             # Chat 데이터 모델
│   │   ├── MessageDto.java          # Message 데이터 모델
│   │   ├── EventDto.java            # Event 데이터 모델
│   │   ├── MeetingDto.java          # Meeting 데이터 모델
│   │   └── UserDto.java             # User 데이터 모델
│   ├── exception/                   # MS API 예외
│   │   └── GraphApiException.java   # Graph API 예외
│   └── util/                        # MS API 유틸리티
│       └── GraphApiErrorHandler.java # Graph API 에러 핸들러
├── user/                            # 사용자 관리
│   ├── controller/                  # 사용자 컨트롤러
│   │   └── UserController.java      # 사용자 관리
│   ├── service/                     # 사용자 서비스
│   │   └── UserService.java         # 사용자 서비스
│   ├── repository/                  # 사용자 리포지토리
│   │   └── UserRepository.java      # 사용자 리포지토리
│   ├── entity/                      # 사용자 엔티티
│   │   └── User.java                # 사용자 엔티티
│   └── dto/                         # 사용자 DTO
│       ├── LoginRequest.java         # 로그인 요청
│       └── RegisterRequest.java     # 회원가입 요청
├── shared/                          # 공유 유틸리티
│   ├── exception/                   # 공통 예외
│   │   ├── GlobalExceptionHandler.java  # 전역 예외 핸들러
│   │   ├── UnauthorizedException.java   # 인증 예외
│   │   └── ForbiddenException.java      # 권한 예외
│   ├── port/                        # 포트 인터페이스
│   │   └── GraphClientPort.java     # Graph Client 포트
│   └── util/                        # 공통 유틸리티
│       ├── AuthUtil.java             # 인증 유틸리티
│       ├── ApiResponse.java         # API 응답 유틸리티
│       └── CertificateGenerator.java # 인증서 생성기
├── TeamsApplication.java            # 메인 애플리케이션
└── ServletInitializer.java          # 서블릿 초기화

src/main/resources/
├── templates/                       # Thymeleaf 템플릿
│   ├── index.html                   # 로그인 페이지
│   ├── home.html                    # 홈 페이지
│   ├── teams.html                   # Teams 페이지
│   ├── chats.html                   # Chats 페이지
│   ├── calendar.html                # Calendar 페이지
│   ├── meetings.html                # Meetings 페이지
│   ├── profile.html                 # Profile 페이지
│   ├── admin.html                   # Admin 페이지
│   ├── auth/                        # 인증 관련 템플릿
│   │   ├── app/                     # 앱 로그인 템플릿
│   │   └── saml/                    # SAML 로그인 템플릿
│   └── fragments/                   # 공통 프래그먼트
├── saml/                            # SAML 인증서
│   ├── certificate.pem              # 인증서
│   └── private-key.pem              # 개인키
├── static/                          # 정적 리소스
└── application.properties           # 설정 파일
```

## 🔧 API 엔드포인트

### 페이지
- `GET /` - 메인 페이지 (인증 시나리오 테스트)
- `GET /home` - 홈 페이지
- `GET /home/profile` - 프로필 페이지
- `GET /home/teams` - Teams 페이지
- `GET /home/chats` - Chats 페이지
- `GET /home/calendar` - Calendar 페이지
- `GET /home/meetings` - Meetings 페이지
- `GET /home/admin` - 관리자 페이지

### 인증
#### MS 단독 로그인
- `POST /auth/microsoft/callback` - MS 단독 로그인 콜백
- `GET /auth/microsoft/login/select-account` - 계정 선택 창 강제 표시
- `GET /auth/microsoft/login/force` - 로그인 창 강제 표시

#### OAuth 2.0 연동
- `GET /auth/oauth/login` - OAuth 로그인 시작
- `POST /auth/oauth/callback` - OAuth 콜백
- `GET /auth/oauth/login/select-account` - 계정 선택 창 강제 표시
- `GET /auth/oauth/login/force` - 로그인 창 강제 표시
- `GET /auth/oauth/link` - 기존 앱 계정에 OAuth 연동

#### 앱 자체 로그인
- `GET /auth/app/login` - 앱 로그인 페이지
- `POST /auth/app/login` - 앱 로그인 처리
- `GET /auth/app/register` - 회원가입 페이지
- `POST /auth/app/register` - 회원가입 처리
- `GET /auth/app/oauth/link` - OAuth 연동

#### SAML 2.0
- `GET /auth/saml/sso` - SAML SSO 시작
- `GET /auth/saml/login` - SAML 로그인 페이지
- `POST /auth/saml/login` - SAML 로그인 처리
- `GET /auth/saml/assert` - SAML Assertion 생성 및 전송
- `GET /auth/saml/metadata` - IdP 메타데이터 (XML)

#### 공통
- `GET /auth/logout` - 로그아웃

### Microsoft Graph API
#### 사용자 정보
- `GET /api/me` - 현재 사용자 정보 (MS Graph)
- `PUT /api/me` - 사용자 정보 수정

#### Teams & Channels
- `GET /api/teams` - 소속 Teams 목록
- `GET /api/teams/{teamId}/channels` - Team의 채널 목록
- `POST /api/teams/{teamId}/channels` - 채널 생성
- `GET /api/teams/{teamId}/channels/{channelId}/messages` - 채널 메시지 목록

#### Chats
- `GET /api/chats` - 채팅 목록
- `POST /api/chats` - 채팅 생성
- `GET /api/chats/{chatId}/messages` - 채팅 메시지 목록
- `POST /api/chats/{chatId}/messages` - 메시지 전송
- `GET /api/chats/{chatId}/members` - 채팅 멤버 목록

#### Calendar
- `GET /api/me/events` - 캘린더 이벤트 목록
- `POST /api/me/events` - 이벤트 생성

#### Meetings
- `GET /api/me/onlineMeetings` - 온라인 미팅 목록
- `POST /api/me/onlineMeetings` - 온라인 미팅 생성

### 앱 사용자 API
- `GET /api/app/me` - 앱(DB) 사용자 정보 조회

### 관리자 API
- `GET /api/admin/check` - 관리자 권한 확인
- `GET /api/admin/token-info` - 토큰 정보 조회
- `GET /api/admin/users` - 사용자 목록 조회

## 🎯 Teams API 사용 방법

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
- [Azure AD OAuth 문서](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow)
- [Microsoft Graph SDK for Java](https://github.com/microsoftgraph/msgraph-sdk-java)

## 🐛 트러블슈팅

### 인증 실패
- Azure Portal에서 리디렉션 URI가 정확히 `http://localhost:8080/callback`로 설정되어 있는지 확인
- Client ID, Client Secret, Tenant ID가 올바른지 확인

### API 호출 실패
- API 권한이 올바르게 설정되어 있는지 확인
- 라이선스 확인 (관리자가 라이선스 구매, User에게 라이선스를 할당해야합니다.)
- 관리자 동의가 완료되었는지 확인
- 로그를 확인하여 구체적인 오류 메시지 확인 (`logging.level.com.example.teams=DEBUG`)

## 📝 라이선스
이 프로젝트는 학습 및 테스트 목적으로 만들어졌습니다.

## 👨‍💻 개발자
dddzr