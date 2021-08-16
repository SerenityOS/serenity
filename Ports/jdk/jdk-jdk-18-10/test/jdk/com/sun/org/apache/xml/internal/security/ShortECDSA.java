/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 8259535
 * @summary ECDSA SignatureValue do not always have the specified length
 * @modules java.xml.crypto/com.sun.org.apache.xml.internal.security
 *          java.xml.crypto/com.sun.org.apache.xml.internal.security.signature
 */

import com.sun.org.apache.xml.internal.security.Init;
import com.sun.org.apache.xml.internal.security.signature.XMLSignature;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

public class ShortECDSA {

    public static final String XML = "<testXmlFile>\n"
            + "\t<element>Value</element>\n"
            + "</testXmlFile>";

    private static final String PRIVATE_KEY_BASE_64 =
              "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgP6yNCRsISznuzY4D"
            + "0cwkBjgV8uu2lQ2tCPxdam7Fx9OhRANCAAS33BazN06tOnXsLYatPvmkrEVDyRWj"
            + "yzxlCU+en8PPJ4ETUGRhz8h1fAELEkKS0Cky5M61oQVyiSxHaXhunH29";

    private static final XMLSignatureFactory FAC =
            XMLSignatureFactory.getInstance("DOM");

    public static void main(String[] args) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().
                parse(new InputSource(new StringReader(XML)));

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(PRIVATE_KEY_BASE_64));
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        // Remove SunEC so neither its ASN.1 or P1363 format ECDSA will be used
        Security.removeProvider("SunEC");
        Security.addProvider(new MyProvider());

        Document signedDocument = XmlSigningUtils.signDocument(document, privateKey);
        NodeList nodeList = signedDocument.getElementsByTagName("SignatureValue");
        byte[] sig = Base64.getMimeDecoder().decode(
                nodeList.item(0).getFirstChild().getNodeValue());
        if (sig.length != 64) {
            System.out.println("Length: " + sig.length);
            System.out.println(HexFormat.ofDelimiter(":").formatHex(sig));
            throw new RuntimeException("Failed");
        }

        // Internal way
        Init.init();
        XMLSignature signature = new XMLSignature(document, null,
                SignatureMethod.ECDSA_SHA256, CanonicalizationMethod.INCLUSIVE);
        signature.sign(privateKey);
        sig = signature.getSignatureValue();
        if (sig.length != 64) {
            System.out.println("Length: " + sig.length);
            System.out.println(HexFormat.ofDelimiter(":").formatHex(sig));
            throw new RuntimeException("Failed");
        }
    }

    public static class XmlSigningUtils {

        public static Document signDocument(Document document, PrivateKey privateKey)
                throws Exception {
            DOMResult result = new DOMResult();
            TransformerFactory.newInstance().newTransformer()
                    .transform(new DOMSource(document), result);
            Document newDocument = (Document) result.getNode();
            FAC.newXMLSignature(buildSignedInfo(), buildKeyInfo())
                    .sign(new DOMSignContext(privateKey, newDocument.getDocumentElement()));
            return newDocument;
        }

        private static SignedInfo buildSignedInfo()
                throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
            return FAC.newSignedInfo(
                    FAC.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    FAC.newSignatureMethod(SignatureMethod.ECDSA_SHA256, null),
                    List.of(FAC.newReference(
                            "",
                            FAC.newDigestMethod(DigestMethod.SHA256, null),
                            List.of(FAC.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null)));
        }

        private static KeyInfo buildKeyInfo() {
            KeyInfoFactory keyInfoFactory = FAC.getKeyInfoFactory();
            X509IssuerSerial x509IssuerSerial = keyInfoFactory
                    .newX509IssuerSerial("CN=Me", BigInteger.ONE);
            X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(x509IssuerSerial));
            return keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));
        }
    }

    // Only provide SHA256withECDSA, no P1363 format. This triggers the convertASN1toXMLDSIG translation.
    public static class MyProvider extends Provider {
        MyProvider() {
            super("MyProvider", "1", "My provider");
            put("Signature.SHA256withECDSA", MyECDSA.class.getName());
        }
    }

    public static class MyECDSA extends SignatureSpi {

        // Hardcoded signature with short r and s
        public static byte[] hardcoded;

        static {
            hardcoded = new byte[68];
            // Fill in with a number <128 so it will look like a
            // correct ASN.1 encoding for positive numbers.
            Arrays.fill(hardcoded, (byte) 0x55);
            hardcoded[0] = 0x30; // SEQUENCE OF
            hardcoded[1] = 66;   // 2 [tag,len,31] components
            hardcoded[2] = hardcoded[35] = 2;   // Each being an INTEGER...
            hardcoded[3] = hardcoded[36] = 31;  // ... of 31 bytes long
        }

        protected void engineInitVerify(PublicKey publicKey) {
        }

        protected void engineInitSign(PrivateKey privateKey) {
        }

        protected void engineUpdate(byte b) {
        }

        protected void engineUpdate(byte[] b, int off, int len) {
        }

        protected byte[] engineSign() {
            return hardcoded;
        }

        protected boolean engineVerify(byte[] sigBytes) {
            return false;
        }

        protected void engineSetParameter(String param, Object value) {
        }

        protected Object engineGetParameter(String param) {
            throw new UnsupportedOperationException();
        }

        protected void engineSetParameter(AlgorithmParameterSpec params) {
        }

        protected AlgorithmParameters engineGetParameters() {
            throw new UnsupportedOperationException();
        }

        protected int engineSign(byte[] outbuf, int offset, int len) {
            System.arraycopy(hardcoded, 0, outbuf, offset, hardcoded.length);
            return hardcoded.length;
        }
    }
}
