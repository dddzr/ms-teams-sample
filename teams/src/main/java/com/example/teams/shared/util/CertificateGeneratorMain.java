package com.example.teams.shared.util;

import java.nio.file.Paths;

/**
 * SAML 인증서 생성 메인 클래스
 * 
 * 실행 방법:
 * 1. IDE에서 이 클래스를 실행하거나
 * 2. Maven으로 실행: mvn exec:java -Dexec.mainClass="com.example.teams.util.CertificateGeneratorMain"
 */
public class CertificateGeneratorMain {
    
    public static void main(String[] args) {
        try {
            // 인증서 파일 경로
            String basePath = "src/main/resources/saml";
            String certPath = Paths.get(basePath, "certificate.pem").toString();
            String keyPath = Paths.get(basePath, "private-key.pem").toString();
            
            System.out.println("========================================");
            System.out.println("SAML 인증서 생성");
            System.out.println("========================================");
            System.out.println();
            
            // 인증서 생성
            CertificateGenerator.generateCertificate(certPath, keyPath);
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("인증서 생성 완료!");
            System.out.println("========================================");
            System.out.println();
            System.out.println("생성된 파일:");
            System.out.println("  - " + certPath + " (Azure Portal에 입력할 인증서)");
            System.out.println("  - " + keyPath + " (서버에서만 사용, 공유하지 말 것)");
            System.out.println();
            System.out.println("다음 단계:");
            System.out.println("  1. certificate.pem 파일을 열어서 내용 복사");
            System.out.println("  2. Azure Portal의 Certificate 필드에 붙여넣기");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("인증서 생성 실패: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

