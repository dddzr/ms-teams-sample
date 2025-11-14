package com.example.teams.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml.saml2.core.impl.ResponseBuilder;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.x509.X509Credential;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import com.example.teams.auth.config.SamlConfig;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;

/**
 * SAML 2.0 서비스
 * 
 * 시나리오: 우리 포털이 IdP (Identity Provider)
 * - 우리 포털이 사용자 인증을 담당
 * - Microsoft Entra ID는 Relying Party (RP)로 동작
 * - 사용자가 우리 포털에서 로그인하면 Microsoft Entra ID가 우리 포털의 인증 정보를 받음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SamlService {
    
    private final SamlConfig samlConfig;
    private final ResourceLoader resourceLoader;
    
    /**
     * SAML 2.0 AuthnRequest 파싱
     * Microsoft Entra ID로부터 받은 인증 요청을 처리
     */
    public AuthnRequest parseAuthnRequest(String samlRequest) {
        try {
            // Base64 디코딩 및 압축 해제
            byte[] decoded = java.util.Base64.getDecoder().decode(samlRequest);
            // TODO: 실제로는 압축 해제가 필요할 수 있음
            
            // XML 파싱
            // TODO: OpenSAML을 사용한 실제 파싱 구현 필요
            log.info("SAML AuthnRequest 수신: {}", samlRequest);
            
            // 임시로 null 반환 (실제 구현 필요)
            return null;
        } catch (Exception e) {
            log.error("SAML AuthnRequest 파싱 실패", e);
            throw new RuntimeException("SAML AuthnRequest 파싱 실패", e);
        }
    }
    
    /**
     * SAML 2.0 Response 생성
     * 인증 성공 후 Microsoft Entra ID로 전송할 응답 생성
     * 
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param name 사용자 이름
     * @param inResponseTo 원본 AuthnRequest의 ID
     * @return Base64 인코딩된 SAML Response
     */
    public String createSamlResponse(Long userId, String email, String name, String inResponseTo) {
        try {
            // Response 생성
            Response response = new ResponseBuilder().buildObject();
            response.setID("_" + UUID.randomUUID().toString());
            response.setVersion(SAMLVersion.VERSION_20);
            response.setIssueInstant(DateTime.now(DateTimeZone.UTC));
            response.setDestination(samlConfig.getAcsUrl());
            response.setInResponseTo(inResponseTo);
            response.setIssuer(createIssuer());
            
            // Status 생성 (필수)
            Status status = new org.opensaml.saml.saml2.core.impl.StatusBuilder().buildObject();
            StatusCode statusCode = new org.opensaml.saml.saml2.core.impl.StatusCodeBuilder().buildObject();
            statusCode.setValue("urn:oasis:names:tc:SAML:2.0:status:Success");
            status.setStatusCode(statusCode);
            response.setStatus(status);
            
            // Assertion 생성
            Assertion assertion = createAssertion(userId, email, name, inResponseTo);
            
            // Assertion에 서명 추가 (마샬링 전에 서명 설정)
            try {
                signAssertion(assertion);
                log.info("SAML Assertion 서명 완료");
            } catch (Exception e) {
                log.warn("SAML Assertion 서명 실패: {}. 서명 없이 진행합니다.", e.getMessage());
                // 서명 실패해도 계속 진행 (테스트용)
            }
            
            response.getAssertions().add(assertion);
            
            // XML로 마샬링
            var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            if (marshallerFactory == null) {
                throw new RuntimeException("MarshallerFactory가 null입니다. OpenSAML이 제대로 초기화되지 않았을 수 있습니다.");
            }
            
            var marshaller = marshallerFactory.getMarshaller(response);
            if (marshaller == null) {
                throw new RuntimeException("Response에 대한 Marshaller를 찾을 수 없습니다. Response 타입: " + response.getClass().getName());
            }
            
            Element element = marshaller.marshall(response);
            if (element == null) {
                throw new RuntimeException("Marshalling 결과가 null입니다.");
            }
            
            // Base64 인코딩
            String xmlString = elementToString(element);
            
            // XML이 비어있지 않은지 확인
            if (xmlString == null || xmlString.trim().isEmpty()) {
                throw new RuntimeException("생성된 SAML XML이 비어있습니다");
            }
            
            log.info("생성된 SAML XML 길이: {} bytes", xmlString.length());
            log.debug("생성된 SAML XML: {}", xmlString);
            
            String base64Response = java.util.Base64.getEncoder()
                .encodeToString(xmlString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            log.info("SAML Response 생성 완료: ResponseID={}, Base64 길이={}", 
                response.getID(), base64Response.length());
            return base64Response;
            
        } catch (Exception e) {
            log.error("SAML Response 생성 실패: {}", e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("SAML Response 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * Assertion 생성
     */
    private Assertion createAssertion(Long userId, String email, String name, String inResponseTo) {
        Assertion assertion = new org.opensaml.saml.saml2.core.impl.AssertionBuilder().buildObject();
        assertion.setID("_" + UUID.randomUUID().toString());
        assertion.setIssueInstant(DateTime.now(DateTimeZone.UTC));
        assertion.setIssuer(createIssuer());
        
        // Subject 생성
        Subject subject = new org.opensaml.saml.saml2.core.impl.SubjectBuilder().buildObject();
        NameID nameID = new org.opensaml.saml.saml2.core.impl.NameIDBuilder().buildObject();
        nameID.setValue(email);
        nameID.setFormat(samlConfig.getNameIdFormat());
        subject.setNameID(nameID);
        
        // SubjectConfirmation 생성
        SubjectConfirmation subjectConfirmation = new org.opensaml.saml.saml2.core.impl.SubjectConfirmationBuilder().buildObject();
        subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
        SubjectConfirmationData subjectConfirmationData = new org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder().buildObject();
        subjectConfirmationData.setRecipient(samlConfig.getAcsUrl());
        subjectConfirmationData.setNotOnOrAfter(DateTime.now(DateTimeZone.UTC).plusSeconds(samlConfig.getAssertionValiditySeconds()));
        subjectConfirmationData.setInResponseTo(inResponseTo);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        
        assertion.setSubject(subject);
        
        // AuthnStatement 생성
        AuthnStatement authnStatement = new org.opensaml.saml.saml2.core.impl.AuthnStatementBuilder().buildObject();
        authnStatement.setAuthnInstant(DateTime.now(DateTimeZone.UTC));
        AuthnContext authnContext = new org.opensaml.saml.saml2.core.impl.AuthnContextBuilder().buildObject();
        AuthnContextClassRef authnContextClassRef = new org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder().buildObject();
        authnContextClassRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        assertion.getAuthnStatements().add(authnStatement);
        
        // AttributeStatement 생성
        AttributeStatement attributeStatement = new org.opensaml.saml.saml2.core.impl.AttributeStatementBuilder().buildObject();
        
        // Email 속성
        Attribute emailAttr = new AttributeBuilder().buildObject();
        emailAttr.setName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        emailAttr.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        // Use XSString to represent string attribute values correctly in SAML 2.0
        XSString emailXSString = 
            (XSString) XMLObjectProviderRegistrySupport
                .getBuilderFactory()
                .<XSString>getBuilder(XSString.TYPE_NAME)
                .buildObject(XSString.TYPE_NAME);
        emailXSString.setValue(email);
        emailAttr.getAttributeValues().add(emailXSString);
        attributeStatement.getAttributes().add(emailAttr);
        
        // Name 속성
        Attribute nameAttr = new AttributeBuilder().buildObject();
        nameAttr.setName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name");
        nameAttr.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        // Use XSString to represent string attribute values correctly in SAML 2.0
        XSString nameXSString = 
            (XSString) XMLObjectProviderRegistrySupport
                .getBuilderFactory()
                .<XSString>getBuilder(XSString.TYPE_NAME)
                .buildObject(XSString.TYPE_NAME);
        nameXSString.setValue(name);
        nameAttr.getAttributeValues().add(nameXSString);
        attributeStatement.getAttributes().add(nameAttr);

        assertion.getAttributeStatements().add(attributeStatement);
        
        return assertion;
    }
    
    /**
     * Issuer 생성
     */
    private Issuer createIssuer() {
        Issuer issuer = new org.opensaml.saml.saml2.core.impl.IssuerBuilder().buildObject();
        issuer.setValue(samlConfig.getEntityId());
        return issuer;
    }
    
    /**
     * Element를 String으로 변환
     */
    private String elementToString(Element element) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }
    
    /**
     * Assertion에 서명 추가
     */
    private void signAssertion(Assertion assertion) throws Exception {
        try {
            // 인증서와 개인 키 로드
            Credential credential = loadCredential();
            if (credential == null) {
                throw new RuntimeException("인증서 또는 개인 키를 로드할 수 없습니다.");
            }
            
            // Signature 생성
            Signature signature = (Signature) XMLObjectProviderRegistrySupport
                .getBuilderFactory()
                .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                .buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            // 서명 설정
            signature.setSigningCredential(credential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            
            // Assertion에 Signature 추가 (마샬링 전에 추가)
            assertion.setSignature(signature);
            
            // Assertion을 마샬링 (Signature가 포함된 상태로 마샬링하여 XMLSignature 인스턴스 생성)
            var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            var assertionMarshaller = marshallerFactory.getMarshaller(assertion);
            if (assertionMarshaller == null) {
                throw new RuntimeException("Assertion에 대한 Marshaller를 찾을 수 없습니다.");
            }
            assertionMarshaller.marshall(assertion);
            
            // 서명 실행 (마샬링 후 XMLSignature 인스턴스가 생성된 상태에서 서명)
            Signer.signObject(signature);
            
            log.info("SAML Assertion 서명 완료");
            
        } catch (Exception e) {
            log.error("SAML Assertion 서명 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 인증서와 개인 키 로드
     */
    private Credential loadCredential() throws Exception {
        try {
            // 인증서 로드
            Resource certResource = resourceLoader.getResource(samlConfig.getCertificatePath());
            if (!certResource.exists()) {
                log.warn("인증서 파일을 찾을 수 없습니다: {}", samlConfig.getCertificatePath());
                return null;
            }
            
            X509Certificate certificate = loadCertificate(certResource.getInputStream());
            
            // 개인 키 로드
            Resource keyResource = resourceLoader.getResource(samlConfig.getPrivateKeyPath());
            if (!keyResource.exists()) {
                log.warn("개인 키 파일을 찾을 수 없습니다: {}", samlConfig.getPrivateKeyPath());
                return null;
            }
            
            PrivateKey privateKey = loadPrivateKey(keyResource.getInputStream());
            
            // Credential 생성 (OpenSAML 3.x 방식)
            X509Credential credential = CredentialSupport.getSimpleCredential(certificate, privateKey);
            
            log.info("인증서와 개인 키 로드 완료");
            return credential;
            
        } catch (Exception e) {
            log.error("인증서 또는 개인 키 로드 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * PEM 형식 인증서 로드
     */
    private X509Certificate loadCertificate(InputStream inputStream) throws Exception {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            StringBuilder pemContent = new StringBuilder();
            boolean inCertificate = false;
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.contains("BEGIN CERTIFICATE")) {
                    inCertificate = true;
                    continue;
                }
                if (line.contains("END CERTIFICATE")) {
                    break;
                }
                if (inCertificate && !line.isEmpty()) {
                    pemContent.append(line);
                }
            }
            
            // Base64 디코딩 (공백 제거)
            String base64Content = pemContent.toString().replaceAll("\\s+", "");
            if (base64Content == null || base64Content.isEmpty()) {
                throw new RuntimeException("인증서 내용이 비어있습니다. PEM 파일 형식을 확인하세요.");
            }
            
            byte[] certBytes = Base64.getDecoder().decode(base64Content);
            if (certBytes == null || certBytes.length == 0) {
                throw new RuntimeException("인증서 디코딩 실패: Base64 디코딩 결과가 비어있습니다.");
            }
            
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(
                new java.io.ByteArrayInputStream(certBytes)
            );
        }
    }
    
    /**
     * PEM 형식 개인 키 로드
     */
    private PrivateKey loadPrivateKey(InputStream inputStream) throws Exception {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            StringBuilder pemContent = new StringBuilder();
            boolean inPrivateKey = false;
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.contains("BEGIN PRIVATE KEY") || line.contains("BEGIN RSA PRIVATE KEY")) {
                    inPrivateKey = true;
                    continue;
                }
                if (line.contains("END PRIVATE KEY") || line.contains("END RSA PRIVATE KEY")) {
                    break;
                }
                if (inPrivateKey && !line.isEmpty()) {
                    pemContent.append(line);
                }
            }
            
            // Base64 디코딩 (공백 제거)
            String base64Content = pemContent.toString().replaceAll("\\s+", "");
            if (base64Content == null || base64Content.isEmpty()) {
                throw new RuntimeException("개인 키 내용이 비어있습니다. PEM 파일 형식을 확인하세요.");
            }
            
            byte[] keyBytes = Base64.getDecoder().decode(base64Content);
            if (keyBytes == null || keyBytes.length == 0) {
                throw new RuntimeException("개인 키 디코딩 실패: Base64 디코딩 결과가 비어있습니다.");
            }
            
            // PKCS#8 형식인지 확인하고 적절한 KeySpec 사용
            try {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e) {
                // PKCS#8 형식이 아닐 경우, RSA PRIVATE KEY 형식일 수 있음
                // BouncyCastle을 사용하거나 다른 방법으로 처리
                log.warn("PKCS#8 형식으로 로드 실패, 다른 방법 시도: {}", e.getMessage());
                throw new RuntimeException("개인 키 로드 실패: 지원하지 않는 형식일 수 있습니다.", e);
            }
        }
    }
    
    /**
     * SAML 메타데이터 생성
     * Microsoft Entra ID에 등록할 우리 포털의 메타데이터
     */
    public String generateMetadata() {
        // TODO: 실제 메타데이터 생성 구현 필요
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + samlConfig.getEntityId() + "\">\n" +
               "  <IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" +
               "    <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"" + samlConfig.getSsoUrl() + "\"/>\n" +
               "  </IDPSSODescriptor>\n" +
               "</EntityDescriptor>";
    }
}

