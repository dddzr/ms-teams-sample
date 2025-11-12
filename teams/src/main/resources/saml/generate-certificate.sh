#!/bin/bash
# SAML 인증서 생성 스크립트 (Linux/Mac/Git Bash)

echo "========================================"
echo "SAML 인증서 생성"
echo "========================================"
echo ""

# 현재 디렉토리로 이동
cd "$(dirname "$0")"

# OpenSSL 확인
if ! command -v openssl &> /dev/null; then
    echo "[오류] OpenSSL이 설치되어 있지 않습니다."
    echo ""
    echo "OpenSSL 설치 방법:"
    echo "  - Ubuntu/Debian: sudo apt-get install openssl"
    echo "  - Mac: brew install openssl"
    echo "  - Windows: Git Bash 사용 또는 WSL 설치"
    exit 1
fi

echo "[1/2] 개인키 생성 중..."
openssl genrsa -out private-key.pem 2048
if [ $? -ne 0 ]; then
    echo "[오류] 개인키 생성 실패"
    exit 1
fi

echo "[2/2] 인증서 생성 중..."
openssl req -new -x509 -key private-key.pem -out certificate.pem -days 365 \
  -subj "/CN=localhost/O=Test Organization/C=KR"
if [ $? -ne 0 ]; then
    echo "[오류] 인증서 생성 실패"
    exit 1
fi

echo ""
echo "========================================"
echo "인증서 생성 완료!"
echo "========================================"
echo ""
echo "생성된 파일:"
echo "  - certificate.pem (Azure Portal에 입력할 인증서)"
echo "  - private-key.pem (서버에서만 사용, 공유하지 말 것)"
echo ""
echo "다음 단계:"
echo "  1. certificate.pem 파일을 열어서 내용 복사"
echo "  2. Azure Portal의 Certificate 필드에 붙여넣기"
echo ""

