@echo off
REM SAML 인증서 생성 스크립트 (Windows)
REM OpenSSL이 설치되어 있어야 합니다.

echo ========================================
echo SAML 인증서 생성
echo ========================================
echo.

REM 현재 디렉토리 확인
cd /d %~dp0

REM OpenSSL 확인
where openssl >nul 2>&1
if %errorlevel% neq 0 (
    echo [오류] OpenSSL이 설치되어 있지 않습니다.
    echo.
    echo OpenSSL 설치 방법:
    echo 1. Git Bash 사용: Git for Windows 설치 시 포함됨
    echo 2. WSL 사용: Windows Subsystem for Linux 설치
    echo 3. 직접 설치: https://slproweb.com/products/Win32OpenSSL.html
    echo.
    echo 또는 Git Bash에서 다음 명령 실행:
    echo   openssl genrsa -out private-key.pem 2048
    echo   openssl req -new -x509 -key private-key.pem -out certificate.pem -days 365 -subj "/CN=localhost"
    pause
    exit /b 1
)

echo [1/2] 개인키 생성 중...
openssl genrsa -out private-key.pem 2048
if %errorlevel% neq 0 (
    echo [오류] 개인키 생성 실패
    pause
    exit /b 1
)

echo [2/2] 인증서 생성 중...
openssl req -new -x509 -key private-key.pem -out certificate.pem -days 365 -subj "/CN=localhost/O=Test Organization/C=KR"
if %errorlevel% neq 0 (
    echo [오류] 인증서 생성 실패
    pause
    exit /b 1
)

echo.
echo ========================================
echo 인증서 생성 완료!
echo ========================================
echo.
echo 생성된 파일:
echo   - certificate.pem (Azure Portal에 입력할 인증서)
echo   - private-key.pem (서버에서만 사용, 공유하지 말 것)
echo.
echo 다음 단계:
echo   1. certificate.pem 파일을 열어서 내용 복사
echo   2. Azure Portal의 Certificate 필드에 붙여넣기
echo.
pause

