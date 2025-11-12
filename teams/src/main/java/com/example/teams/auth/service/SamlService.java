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
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import com.example.teams.auth.config.SamlConfig;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
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
            
            // Assertion 생성
            Assertion assertion = createAssertion(userId, email, name, inResponseTo);
            response.getAssertions().add(assertion);
            
            // XML로 마샬링
            Element element = XMLObjectProviderRegistrySupport.getMarshallerFactory()
                .getMarshaller(response)
                .marshall(response);
            
            // Base64 인코딩
            String xmlString = elementToString(element);
            String base64Response = java.util.Base64.getEncoder()
                .encodeToString(xmlString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            log.info("SAML Response 생성 완료: ResponseID={}", response.getID());
            return base64Response;
            
        } catch (Exception e) {
            log.error("SAML Response 생성 실패", e);
            throw new RuntimeException("SAML Response 생성 실패", e);
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
        org.opensaml.saml.saml2.core.AttributeValue nameValue = (AttributeValue) new AttributeBuilder().buildObject();
        // setTextContent is not available for AttributeValue, so use XSString to set the value
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

