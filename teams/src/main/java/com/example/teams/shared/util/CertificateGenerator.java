package com.example.teams.shared.util;

import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * SAML 인증서 생성 유틸리티
 * 테스트용 자체 서명 인증서를 생성합니다.
 */
@Slf4j
public class CertificateGenerator {
    
    /**
     * SAML 인증서와 개인키를 생성합니다.
     * 
     * @param certPath 인증서 파일 경로
     * @param keyPath 개인키 파일 경로
     * @throws Exception 인증서 생성 실패 시
     */
    public static void generateCertificate(String certPath, String keyPath) throws Exception {
        // 키 페어 생성
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        // 인증서 정보
        X500Name issuer = new X500Name("CN=localhost, O=Test Organization, C=KR");
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 365L * 24 * 60 * 60 * 1000); // 1년
        
        // 인증서 빌더
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            issuer,
            keyPair.getPublic()
        );
        
        // 서명
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
        
        // PEM 형식으로 저장
        writePemFile(certPath, "CERTIFICATE", cert.getEncoded());
        writePemFile(keyPath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        
        log.info("인증서 생성 완료: {}", certPath);
        log.info("개인키 생성 완료: {}", keyPath);
    }
    
    private static void writePemFile(String path, String type, byte[] content) throws IOException {
        try (PemWriter writer = new PemWriter(new FileWriter(path))) {
            writer.writeObject(new PemObject(type, content));
        }
    }
}

