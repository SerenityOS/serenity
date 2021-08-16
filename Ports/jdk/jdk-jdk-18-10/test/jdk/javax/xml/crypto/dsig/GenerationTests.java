/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4635230 6283345 6303830 6824440 6867348 7094155 8038184 8038349 8046949
 *      8046724 8079693 8177334 8205507 8210736 8217878 8241306
 * @summary Basic unit tests for generating XML Signatures with JSR 105
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 *          java.xml.crypto/org.jcp.xml.dsig.internal.dom
 *          jdk.httpserver/com.sun.net.httpserver
 * @library /test/lib
 * @compile -XDignore.symbol.file KeySelectors.java SignatureValidator.java
 *     X509KeySelector.java GenerationTests.java
 * @run main/othervm/timeout=300 -Dsun.net.httpserver.nodelay=true GenerationTests
 * @author Sean Mullan
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.spec.KeySpec;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECField;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.stream.Stream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.crypto.Data;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dom.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import jdk.test.lib.security.SecurityUtils;

/**
 * Test that recreates merlin-xmldsig-twenty-three test vectors (and more)
 * but with different keys and X.509 data.
 */
public class GenerationTests {

    private static XMLSignatureFactory fac;
    private static KeyInfoFactory kifac;
    private static DocumentBuilder db;
    private static CanonicalizationMethod withoutComments;
    private static SignatureMethod dsaSha1, dsaSha256,
            rsaSha1, rsaSha224, rsaSha256, rsaSha384, rsaSha512,
            ecdsaSha1, ecdsaSha224, ecdsaSha256, ecdsaSha384, ecdsaSha512,
            hmacSha1, hmacSha224, hmacSha256, hmacSha384, hmacSha512,
            rsaSha1mgf1, rsaSha224mgf1, rsaSha256mgf1, rsaSha384mgf1, rsaSha512mgf1, rsaShaPSS;
    private static DigestMethod sha1, sha224, sha256, sha384, sha512,
                                sha3_224, sha3_256, sha3_384, sha3_512;
    private static KeyInfo dsa1024, dsa2048, rsa, rsa1024, rsa2048,
                           p256ki, p384ki, p521ki;
    private static KeySelector kvks = new KeySelectors.KeyValueKeySelector();
    private static KeySelector sks;
    private static Key signingKey;
    private static PublicKey validatingKey;
    private static Certificate signingCert;
    private static KeyStore ks;
    private final static String DIR = System.getProperty("test.src", ".");
//    private final static String DIR = ".";
    private final static String DATA_DIR =
        DIR + System.getProperty("file.separator") + "data";
    private final static String KEYSTORE =
        DATA_DIR + System.getProperty("file.separator") + "certs" +
        System.getProperty("file.separator") + "test.jks";
    private final static String CRL =
        DATA_DIR + System.getProperty("file.separator") + "certs" +
        System.getProperty("file.separator") + "crl";
    // XML Document with a DOCTYPE declaration
    private final static String ENVELOPE =
        DATA_DIR + System.getProperty("file.separator") + "envelope.xml";
    // XML Document without a DOCTYPE declaration
    private final static String ENVELOPE2 =
        DATA_DIR + System.getProperty("file.separator") + "envelope2.xml";
    private static URIDereferencer httpUd = null;
    private final static String STYLESHEET =
        "http://www.w3.org/TR/xml-stylesheet";
    private final static String STYLESHEET_B64 =
        "http://www.w3.org/Signature/2002/04/xml-stylesheet.b64";
    private final static String DSA_SHA256 =
        "http://www.w3.org/2009/xmldsig11#dsa-sha256";

    private static final String BOGUS = "bogus";

    private static final  String xslt = ""
          + "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'\n"
          + "            xmlns='http://www.w3.org/TR/xhtml1/strict' \n"
          + "            exclude-result-prefixes='foo' \n"
          + "            version='1.0'>\n"
          + "  <xsl:output encoding='UTF-8' \n"
          + "           indent='no' \n"
          + "           method='xml' />\n"
          + "  <xsl:template match='/'>\n"
          + "    <html>\n"
          + "   <head>\n"
          + "    <title>Notaries</title>\n"
          + "   </head>\n"
          + "   <body>\n"
          + "    <table>\n"
          + "      <xsl:for-each select='Notaries/Notary'>\n"
          + "           <tr>\n"
          + "           <th>\n"
          + "            <xsl:value-of select='@name' />\n"
          + "           </th>\n"
          + "           </tr>\n"
          + "      </xsl:for-each>\n"
          + "    </table>\n"
          + "   </body>\n"
          + "    </html>\n"
          + "  </xsl:template>\n"
          + "</xsl:stylesheet>\n";

    private static final String[] canonicalizationMethods = new String[] {
        CanonicalizationMethod.EXCLUSIVE,
        CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
        CanonicalizationMethod.INCLUSIVE,
        CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS
    };

    private static final String[] xml_transforms = new String[] {
        Transform.XSLT,
        Transform.XPATH,
        Transform.XPATH2,
        CanonicalizationMethod.EXCLUSIVE,
        CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
        CanonicalizationMethod.INCLUSIVE,
        CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
    };

    private static final String[] non_xml_transforms = new String[] {
        null, Transform.BASE64
    };

    // It will be too time consuming to test all combinations of
    // all digest methods and signature methods. So we pick some
    // majors one and only test a combination when a major method
    // (either digest or signature) is included.
    //
    //              *  *  *
    //              *  *  *
    //              *  *  *
    //     *  *  *  *  *  *  *  *  *
    //     *  *  *  *  *  *  *  *  *
    //     *  *  *  *  *  *  *  *  *
    //              *  *  *
    //              *  *  *
    //              *  *  *

    private static List<String> majorSignatureMethods = List.of(
            SignatureMethod.DSA_SHA256,
            SignatureMethod.RSA_SHA256,
            SignatureMethod.ECDSA_SHA256,
            SignatureMethod.HMAC_SHA256,
            SignatureMethod.SHA256_RSA_MGF1,
            SignatureMethod.RSA_PSS);

    private static final String[] allSignatureMethods
            = Stream.of(SignatureMethod.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .map(f -> {
                    try {
                        return (String)f.get(null);
                    } catch (Exception e) {
                        throw new Error("should not happen");
                    }
                })
                .toArray(String[]::new);

    private static final List<String> majorDigestMethods = List.of(
            DigestMethod.SHA1,
            DigestMethod.SHA256,
            DigestMethod.SHA3_256);

    private static final String[] allDigestMethods
            = Stream.of(DigestMethod.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers())
                                && !f.getName().equals("RIPEMD160"))
                .map(f -> {
                    try {
                        return (String)f.get(null);
                    } catch (Exception e) {
                        throw new Error("should not happen");
                    }
                })
                .toArray(String[]::new);

    // As of JDK 17, the number of defined algorithms are...
    static {
        if (allSignatureMethods.length != 23
                || allDigestMethods.length != 9) {
            System.out.println(Arrays.toString(allSignatureMethods));
            System.out.println(Arrays.toString(allDigestMethods));
            throw new AssertionError("Not all methods are counted");
        }
    }

    private static enum Content {
        Xml, Text, Base64, NotExisitng
    }

    private static enum KeyInfoType {
        KeyValue, x509data, KeyName
    }

    // cached keys (for performance) used by test_create_detached_signature().
    private static HashMap<String,Key[]> cachedKeys = new HashMap<>();

    // Load cachedKeys persisted in a file to reproduce a failure.
    // The keys are always saved to "cached-keys" but you can rename
    // it to a different file name and load it here. Note: The keys will
    // always be persisted so renaming is a good idea although the
    // content might not change.
    static {
        String cacheFile = System.getProperty("use.cached.keys");
        if (cacheFile != null) {
            try (FileInputStream fis = new FileInputStream(cacheFile);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                cachedKeys = (HashMap<String,Key[]>) ois.readObject();
            } catch (Exception e) {
                throw new AssertionError("Cannot read " + cacheFile, e);
            }
        }
    }

    private static boolean result = true;

    public static void main(String args[]) throws Exception {
        // Re-enable sha1 algs
        SecurityUtils.removeAlgsFromDSigPolicy("sha1");

        setup();
        test_create_signature_enveloped_dsa(1024);
        test_create_signature_enveloped_dsa(2048);
        test_create_signature_enveloping_b64_dsa();
        test_create_signature_enveloping_dsa();
        test_create_signature_enveloping_hmac_sha1_40();
        test_create_signature_enveloping_hmac_sha256();
        test_create_signature_enveloping_hmac_sha224();
        test_create_signature_enveloping_hmac_sha384();
        test_create_signature_enveloping_hmac_sha512();
        test_create_signature_enveloping_rsa();
        test_create_signature_enveloping_p256_sha1();
        test_create_signature_enveloping_p256_sha224();
        test_create_signature_enveloping_p256_sha256();
        test_create_signature_enveloping_p256_sha384();
        test_create_signature_enveloping_p256_sha512();
        test_create_signature_enveloping_p384_sha1();
        test_create_signature_enveloping_p521_sha1();
        test_create_signature_external_b64_dsa();
        test_create_signature_external_dsa();
        test_create_signature_keyname();
        test_create_signature_retrievalmethod_rawx509crt();
        test_create_signature_x509_crt_crl();
        test_create_signature_x509_crt();
        test_create_signature_x509_is();
        test_create_signature_x509_ski();
        test_create_signature_x509_sn();
        test_create_signature();
        test_create_exc_signature();
        test_create_sign_spec();
        test_create_signature_enveloping_sha256_dsa();
        test_create_signature_enveloping_sha384_rsa_sha256();
        test_create_signature_enveloping_sha224_rsa_sha256();
        test_create_signature_enveloping_sha3_224_rsa_sha256();
        test_create_signature_enveloping_sha3_256_rsa_sha256();
        test_create_signature_enveloping_sha3_384_rsa_sha256();
        test_create_signature_enveloping_sha3_512_rsa_sha256();
        test_create_signature_enveloping_sha512_rsa_sha384();
        test_create_signature_enveloping_sha512_rsa_sha224();
        test_create_signature_enveloping_sha512_rsa_sha512();
        test_create_signature_enveloping_sha512_rsa_sha1_mgf1();
        test_create_signature_enveloping_sha512_rsa_sha224_mgf1();
        test_create_signature_enveloping_sha512_rsa_sha256_mgf1();
        test_create_signature_enveloping_sha512_rsa_sha384_mgf1();
        test_create_signature_enveloping_sha512_rsa_sha512_mgf1();
        test_create_signature_enveloping_sha512_rsa_pss();
        test_create_signature_reference_dependency();
        test_create_signature_with_attr_in_no_namespace();
        test_create_signature_with_empty_id();
        test_create_signature_enveloping_over_doc(ENVELOPE, true);
        test_create_signature_enveloping_over_doc(ENVELOPE2, true);
        test_create_signature_enveloping_over_doc(ENVELOPE, false);
        test_create_signature_enveloping_dom_level1();

        // run tests for detached signatures with local http server
        try (Http server = Http.startServer()) {
            server.start();

            System.out.println("\ntests for XML documents");
            Arrays.stream(canonicalizationMethods).forEach(c ->
                Arrays.stream(allSignatureMethods).forEach(s ->
                    Arrays.stream(allDigestMethods).forEach(d ->
                        Arrays.stream(xml_transforms).forEach(t ->
                            Arrays.stream(KeyInfoType.values()).forEach(k -> {
                                if (isMajor(s, d)) {
                                    test_create_detached_signature(c, s, d, t, k,
                                            Content.Xml, server.getPort(), false, null);
                                }
                        })))));

            System.out.println("\ntests for text data with no transform");
            Arrays.stream(canonicalizationMethods).forEach(c ->
                Arrays.stream(allSignatureMethods).forEach(s ->
                    Arrays.stream(allDigestMethods).forEach(d ->
                        Arrays.stream(KeyInfoType.values()).forEach(k -> {
                            if (isMajor(s, d)) {
                                test_create_detached_signature(c, s, d, null, k,
                                        Content.Text, server.getPort(), false, null);
                            }
                        }))));

            System.out.println("\ntests for base64 data");
            Arrays.stream(canonicalizationMethods).forEach(c ->
                Arrays.stream(allSignatureMethods).forEach(s ->
                    Arrays.stream(allDigestMethods).forEach(d ->
                        Arrays.stream(non_xml_transforms).forEach(t ->
                            Arrays.stream(KeyInfoType.values()).forEach(k -> {
                                if (isMajor(s, d)) {
                                    test_create_detached_signature(c, s, d, t, k,
                                            Content.Base64, server.getPort(),
                                            false, null);
                                }
                        })))));

            // negative tests

            System.out.println("\nunknown CanonicalizationMethod");
            test_create_detached_signature(
                    CanonicalizationMethod.EXCLUSIVE + BOGUS,
                    SignatureMethod.DSA_SHA1,
                    DigestMethod.SHA1,
                    CanonicalizationMethod.INCLUSIVE,
                    KeyInfoType.KeyName,
                    Content.Xml,
                    server.getPort(),
                    true,
                    NoSuchAlgorithmException.class);

            System.out.println("\nunknown SignatureMethod");
            test_create_detached_signature(
                    CanonicalizationMethod.EXCLUSIVE,
                    SignatureMethod.DSA_SHA1 + BOGUS,
                    DigestMethod.SHA1,
                    CanonicalizationMethod.INCLUSIVE,
                    KeyInfoType.KeyName, Content.Xml,
                    server.getPort(),
                    true,
                    NoSuchAlgorithmException.class);

            System.out.println("\nunknown DigestMethod");
            test_create_detached_signature(
                    CanonicalizationMethod.EXCLUSIVE,
                    SignatureMethod.DSA_SHA1,
                    DigestMethod.SHA1 + BOGUS,
                    CanonicalizationMethod.INCLUSIVE,
                    KeyInfoType.KeyName, Content.Xml,
                    server.getPort(),
                    true,
                    NoSuchAlgorithmException.class);

            System.out.println("\nunknown Transform");
            test_create_detached_signature(
                    CanonicalizationMethod.EXCLUSIVE,
                    SignatureMethod.DSA_SHA1,
                    DigestMethod.SHA1,
                    CanonicalizationMethod.INCLUSIVE + BOGUS,
                    KeyInfoType.KeyName, Content.Xml,
                    server.getPort(),
                    true,
                    NoSuchAlgorithmException.class);

            System.out.println("\nno source document");
            test_create_detached_signature(
                    CanonicalizationMethod.EXCLUSIVE,
                    SignatureMethod.DSA_SHA1,
                    DigestMethod.SHA1,
                    CanonicalizationMethod.INCLUSIVE,
                    KeyInfoType.KeyName,
                    Content.NotExisitng,
                    server.getPort(),
                    true,
                    XMLSignatureException.class);

            System.out.println("\nwrong transform for text data");
            test_create_detached_signature(
                    CanonicalizationMethod.EXCLUSIVE,
                    SignatureMethod.DSA_SHA1,
                    DigestMethod.SHA1,
                    CanonicalizationMethod.INCLUSIVE,
                    KeyInfoType.KeyName,
                    Content.Text,
                    server.getPort(),
                    true,
                    XMLSignatureException.class);
        }

        // persist cached keys to a file.
        try (FileOutputStream fos = new FileOutputStream("cached-keys", true);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(cachedKeys);
        }

        if (!result) {
            throw new RuntimeException("At least one test case failed");
        }
    }

    // Do not test on all combinations.
    private static boolean isMajor(String signatureMethod, String digestMethod) {
        return majorDigestMethods.contains(digestMethod)
                || majorSignatureMethods.contains(signatureMethod);
    }

    private static void setup() throws Exception {
        fac = XMLSignatureFactory.getInstance();
        kifac = fac.getKeyInfoFactory();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        db = dbf.newDocumentBuilder();

        // get key & self-signed certificate from keystore
        FileInputStream fis = new FileInputStream(KEYSTORE);
        ks = KeyStore.getInstance("JKS");
        ks.load(fis, "changeit".toCharArray());
        signingKey = ks.getKey("user", "changeit".toCharArray());
        signingCert = ks.getCertificate("user");
        validatingKey = signingCert.getPublicKey();

        // create common objects
        withoutComments = fac.newCanonicalizationMethod
            (CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec)null);
        dsaSha1 = fac.newSignatureMethod(SignatureMethod.DSA_SHA1, null);
        dsaSha256 = fac.newSignatureMethod(DSA_SHA256, null);

        sha1 = fac.newDigestMethod(DigestMethod.SHA1, null);
        sha224 = fac.newDigestMethod(DigestMethod.SHA224, null);
        sha256 = fac.newDigestMethod(DigestMethod.SHA256, null);
        sha384 = fac.newDigestMethod(DigestMethod.SHA384, null);
        sha512 = fac.newDigestMethod(DigestMethod.SHA512, null);
        sha3_224 = fac.newDigestMethod(DigestMethod.SHA3_224, null);
        sha3_256 = fac.newDigestMethod(DigestMethod.SHA3_256, null);
        sha3_384 = fac.newDigestMethod(DigestMethod.SHA3_384, null);
        sha3_512 = fac.newDigestMethod(DigestMethod.SHA3_512, null);

        dsa1024 = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(validatingKey)));
        dsa2048 = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(getPublicKey("DSA", 2048))));
        rsa = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(getPublicKey("RSA", 512))));
        rsa1024 = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(getPublicKey("RSA", 1024))));
        rsa2048 = kifac.newKeyInfo(Collections.singletonList
                (kifac.newKeyValue(getPublicKey("RSA", 2048))));
        p256ki = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(getECPublicKey("P256"))));
        p384ki = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(getECPublicKey("P384"))));
        p521ki = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyValue(getECPublicKey("P521"))));

        rsaSha1 = fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
        rsaSha224 = fac.newSignatureMethod(SignatureMethod.RSA_SHA224, null);
        rsaSha256 = fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null);
        rsaSha384 = fac.newSignatureMethod(SignatureMethod.RSA_SHA384, null);
        rsaSha512 = fac.newSignatureMethod(SignatureMethod.RSA_SHA512, null);

        rsaSha1mgf1 = fac.newSignatureMethod(SignatureMethod.SHA1_RSA_MGF1, null);
        rsaSha224mgf1 = fac.newSignatureMethod(SignatureMethod.SHA224_RSA_MGF1, null);
        rsaSha256mgf1 = fac.newSignatureMethod(SignatureMethod.SHA256_RSA_MGF1, null);
        rsaSha384mgf1 = fac.newSignatureMethod(SignatureMethod.SHA384_RSA_MGF1, null);
        rsaSha512mgf1 = fac.newSignatureMethod(SignatureMethod.SHA512_RSA_MGF1, null);
        rsaShaPSS = fac.newSignatureMethod(SignatureMethod. RSA_PSS, null);

        ecdsaSha1 = fac.newSignatureMethod(SignatureMethod.ECDSA_SHA1, null);
        ecdsaSha224 = fac.newSignatureMethod(SignatureMethod.ECDSA_SHA224, null);
        ecdsaSha256 = fac.newSignatureMethod(SignatureMethod.ECDSA_SHA256, null);
        ecdsaSha384 = fac.newSignatureMethod(SignatureMethod.ECDSA_SHA384, null);
        ecdsaSha512 = fac.newSignatureMethod(SignatureMethod.ECDSA_SHA512, null);

        hmacSha1 = fac.newSignatureMethod(SignatureMethod.HMAC_SHA1, null);
        hmacSha224 = fac.newSignatureMethod(SignatureMethod.HMAC_SHA224, null);
        hmacSha256 = fac.newSignatureMethod(SignatureMethod.HMAC_SHA256, null);
        hmacSha384 = fac.newSignatureMethod(SignatureMethod.HMAC_SHA384, null);
        hmacSha512 = fac.newSignatureMethod(SignatureMethod.HMAC_SHA512, null);

        sks = new KeySelectors.SecretKeySelector("secret".getBytes("ASCII"));

        httpUd = new HttpURIDereferencer();
    }

    static void test_create_signature_enveloped_dsa(int size) throws Exception {
        System.out.println("* Generating signature-enveloped-dsa-"
                           + size + ".xml");
        SignatureMethod sm = null;
        KeyInfo ki = null;
        Key privKey;
        if (size == 1024) {
            sm = dsaSha1;
            ki = dsa1024;
            privKey = signingKey;
        } else if (size == 2048) {
            sm = dsaSha256;
            ki = dsa2048;
            privKey = getPrivateKey("DSA", 2048);
        } else throw new RuntimeException("unsupported keysize:" + size);

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo
            (withoutComments, sm, Collections.singletonList
                (fac.newReference
                    ("", sha1, Collections.singletonList
                        (fac.newTransform(Transform.ENVELOPED,
                            (TransformParameterSpec) null)),
                 null, null)));

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature(si, ki);

        Document doc = db.newDocument();
        Element envelope = doc.createElementNS
            ("http://example.org/envelope", "Envelope");
        envelope.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            "xmlns", "http://example.org/envelope");
        doc.appendChild(envelope);

        DOMSignContext dsc = new DOMSignContext(privKey, envelope);

        sig.sign(dsc);
//        StringWriter sw = new StringWriter();
//        dumpDocument(doc, sw);
//        System.out.println(sw.toString());

        DOMValidateContext dvc = new DOMValidateContext
            (kvks, envelope.getFirstChild());
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }

        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }
        System.out.println();
    }

    static void test_create_signature_enveloping_b64_dsa() throws Exception {
        System.out.println("* Generating signature-enveloping-b64-dsa.xml");
        test_create_signature_enveloping
            (sha1, dsaSha1, dsa1024, signingKey, kvks, true, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_dsa() throws Exception {
        System.out.println("* Generating signature-enveloping-dsa.xml");
        test_create_signature_enveloping
            (sha1, dsaSha1, dsa1024, signingKey, kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha256_dsa() throws Exception {
        System.out.println("* Generating signature-enveloping-sha256-dsa.xml");
        test_create_signature_enveloping
            (sha256, dsaSha1, dsa1024, signingKey, kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_hmac_sha1_40()
        throws Exception {
        System.out.println("* Generating signature-enveloping-hmac-sha1-40.xml");
        try {
            test_create_signature_enveloping(sha1, hmacSha1, null,
                getSecretKey("secret".getBytes("ASCII")), sks, false, true);
        } catch (Exception e) {
            if (!(e instanceof XMLSignatureException)) {
                throw e;
            }
        }
        System.out.println();
    }

    static void test_create_signature_enveloping_hmac_sha256()
        throws Exception {
        System.out.println("* Generating signature-enveloping-hmac-sha256.xml");
        test_create_signature_enveloping(sha1, hmacSha256, null,
            getSecretKey("secret".getBytes("ASCII")), sks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_hmac_sha224()
            throws Exception {
        System.out.println("* Generating signature-enveloping-hmac-sha224.xml");
        test_create_signature_enveloping(sha1, hmacSha224, null,
                getSecretKey("secret".getBytes("ASCII")), sks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_hmac_sha384()
        throws Exception {
        System.out.println("* Generating signature-enveloping-hmac-sha384.xml");
        test_create_signature_enveloping(sha1, hmacSha384, null,
            getSecretKey("secret".getBytes("ASCII")), sks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_hmac_sha512()
        throws Exception {
        System.out.println("* Generating signature-enveloping-hmac-sha512.xml");
        test_create_signature_enveloping(sha1, hmacSha512, null,
            getSecretKey("secret".getBytes("ASCII")), sks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_rsa() throws Exception {
        System.out.println("* Generating signature-enveloping-rsa.xml");
        test_create_signature_enveloping(sha1, rsaSha1, rsa,
            getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha384_rsa_sha256()
        throws Exception {
        System.out.println("* Generating signature-enveloping-sha384-rsa_sha256.xml");
        test_create_signature_enveloping(sha384, rsaSha256, rsa,
            getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha224_rsa_sha256()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha224-rsa_sha256.xml");
        test_create_signature_enveloping(sha224, rsaSha256, rsa,
                getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha3_224_rsa_sha256()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha3_224-rsa_sha256.xml");
        test_create_signature_enveloping(sha3_224, rsaSha256, rsa,
                getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha3_256_rsa_sha256()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha3_256-rsa_sha256.xml");
        test_create_signature_enveloping(sha3_256, rsaSha256, rsa,
                getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha3_384_rsa_sha256()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha3_384-rsa_sha256.xml");
        test_create_signature_enveloping(sha3_384, rsaSha256, rsa,
                getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha3_512_rsa_sha256()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha3_512-rsa_sha256.xml");
        test_create_signature_enveloping(sha3_512, rsaSha256, rsa,
                getPrivateKey("RSA", 512), kvks, false, false);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha384()
        throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha384.xml");
        test_create_signature_enveloping(sha512, rsaSha384, rsa1024,
            getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha224()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha224.xml");
        test_create_signature_enveloping(sha512, rsaSha224, rsa1024,
                getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha512()
        throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha512.xml");
        test_create_signature_enveloping(sha512, rsaSha512, rsa1024,
            getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha1_mgf1()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha1_mgf1.xml");
        test_create_signature_enveloping(sha512, rsaSha1mgf1, rsa1024,
                getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha224_mgf1()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha224_mgf1.xml");
        test_create_signature_enveloping(sha512, rsaSha224mgf1, rsa1024,
                getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha256_mgf1()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha256_mgf1.xml");
        test_create_signature_enveloping(sha512, rsaSha256mgf1, rsa1024,
                getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha384_mgf1()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha384_mgf1.xml");
        test_create_signature_enveloping(sha512, rsaSha384mgf1, rsa1024,
                getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_sha512_mgf1()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512-rsa_sha512_mgf1.xml");
        test_create_signature_enveloping(sha512, rsaSha512mgf1, rsa2048,
                getPrivateKey("RSA", 2048), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_sha512_rsa_pss()
            throws Exception {
        System.out.println("* Generating signature-enveloping-sha512_rsa_pss.xml");
        test_create_signature_enveloping(sha512, rsaShaPSS, rsa1024,
                getPrivateKey("RSA", 1024), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p256_sha1() throws Exception {
        System.out.println("* Generating signature-enveloping-p256-sha1.xml");
        test_create_signature_enveloping(sha1, ecdsaSha1, p256ki,
            getECPrivateKey("P256"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p256_sha224() throws Exception {
        System.out.println("* Generating signature-enveloping-p256-sha224.xml");
        test_create_signature_enveloping(sha1, ecdsaSha224, p256ki,
                getECPrivateKey("P256"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p256_sha256() throws Exception {
        System.out.println("* Generating signature-enveloping-p256-sha256.xml");
        test_create_signature_enveloping(sha1, ecdsaSha256, p256ki,
                getECPrivateKey("P256"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p256_sha384() throws Exception {
        System.out.println("* Generating signature-enveloping-p256-sha384.xml");
        test_create_signature_enveloping(sha1, ecdsaSha384, p256ki,
                getECPrivateKey("P256"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p256_sha512() throws Exception {
        System.out.println("* Generating signature-enveloping-p256-sha512.xml");
        test_create_signature_enveloping(sha1, ecdsaSha512, p256ki,
                getECPrivateKey("P256"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p384_sha1() throws Exception {
        System.out.println("* Generating signature-enveloping-p384-sha1.xml");
        test_create_signature_enveloping(sha1, ecdsaSha1, p384ki,
            getECPrivateKey("P384"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_enveloping_p521_sha1() throws Exception {
        System.out.println("* Generating signature-enveloping-p521-sha1.xml");
        test_create_signature_enveloping(sha1, ecdsaSha1, p521ki,
            getECPrivateKey("P521"), kvks, false, true);
        System.out.println();
    }

    static void test_create_signature_external_b64_dsa() throws Exception {
        System.out.println("* Generating signature-external-b64-dsa.xml");
        test_create_signature_external(dsaSha1, dsa1024, signingKey, kvks, true);
        System.out.println();
    }

    static void test_create_signature_external_dsa() throws Exception {
        System.out.println("* Generating signature-external-dsa.xml");
        test_create_signature_external(dsaSha1, dsa1024, signingKey, kvks, false);
        System.out.println();
    }

    static void test_create_signature_keyname() throws Exception {
        System.out.println("* Generating signature-keyname.xml");
        KeyInfo kn = kifac.newKeyInfo(Collections.singletonList
            (kifac.newKeyName("user")));
        test_create_signature_external(dsaSha1, kn, signingKey,
            new X509KeySelector(ks), false);
        System.out.println();
    }

    static void test_create_signature_retrievalmethod_rawx509crt()
        throws Exception {
        System.out.println(
            "* Generating signature-retrievalmethod-rawx509crt.xml");
        KeyInfo rm = kifac.newKeyInfo(Collections.singletonList
            (kifac.newRetrievalMethod
            ("certs/user.crt", X509Data.RAW_X509_CERTIFICATE_TYPE, null)));
        test_create_signature_external(dsaSha1, rm, signingKey,
            new X509KeySelector(ks), false);
        System.out.println();
    }

    static void test_create_signature_x509_crt_crl() throws Exception {
        System.out.println("* Generating signature-x509-crt-crl.xml");
        List<Object> xds = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        xds.add(signingCert);
        FileInputStream fis = new FileInputStream(CRL);
        X509CRL crl = (X509CRL) cf.generateCRL(fis);
        fis.close();
        xds.add(crl);
        KeyInfo crt_crl = kifac.newKeyInfo(Collections.singletonList
            (kifac.newX509Data(xds)));

        test_create_signature_external(dsaSha1, crt_crl, signingKey,
            new X509KeySelector(ks), false);
        System.out.println();
    }

    static void test_create_signature_x509_crt() throws Exception {
        System.out.println("* Generating signature-x509-crt.xml");
        KeyInfo crt = kifac.newKeyInfo(Collections.singletonList
            (kifac.newX509Data(Collections.singletonList(signingCert))));

        test_create_signature_external(dsaSha1, crt, signingKey,
            new X509KeySelector(ks), false);
        System.out.println();
    }

    static void test_create_signature_x509_is() throws Exception {
        System.out.println("* Generating signature-x509-is.xml");
        KeyInfo is = kifac.newKeyInfo(Collections.singletonList
            (kifac.newX509Data(Collections.singletonList
            (kifac.newX509IssuerSerial
            ("CN=User", new BigInteger("45ef2729", 16))))));
        test_create_signature_external(dsaSha1, is, signingKey,
            new X509KeySelector(ks), false);
        System.out.println();
    }

    static void test_create_signature_x509_ski() throws Exception {
        System.out.println("* Generating signature-x509-ski.xml");
        KeyInfo ski = kifac.newKeyInfo(Collections.singletonList
            (kifac.newX509Data(Collections.singletonList
            ("keyid".getBytes("ASCII")))));

        test_create_signature_external(dsaSha1, ski, signingKey,
            KeySelector.singletonKeySelector(validatingKey), false);
        System.out.println();
    }

    static void test_create_signature_x509_sn() throws Exception {
        System.out.println("* Generating signature-x509-sn.xml");
        KeyInfo sn = kifac.newKeyInfo(Collections.singletonList
            (kifac.newX509Data(Collections.singletonList("CN=User"))));

        test_create_signature_external(dsaSha1, sn, signingKey,
            new X509KeySelector(ks), false);
        System.out.println();
    }

    static void test_create_signature_reference_dependency() throws Exception {
        System.out.println("* Generating signature-reference-dependency.xml");
        // create references
        List<Reference> refs = Collections.singletonList
            (fac.newReference("#object-1", sha1));

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, rsaSha1, refs);

        // create objects
        List<XMLObject> objs = new ArrayList<>();

        // Object 1
        List<Reference> manRefs = Collections.singletonList
            (fac.newReference("#object-2", sha1));
        objs.add(fac.newXMLObject(Collections.singletonList
            (fac.newManifest(manRefs, "manifest-1")), "object-1", null, null));

        // Object 2
        Document doc = db.newDocument();
        Element nc = doc.createElementNS(null, "NonCommentandus");
        nc.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "");
        nc.appendChild(doc.createComment(" Commentandum "));
        objs.add(fac.newXMLObject(Collections.singletonList
            (new DOMStructure(nc)), "object-2", null, null));

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature(si, rsa, objs, "signature", null);
        DOMSignContext dsc = new DOMSignContext(getPrivateKey("RSA", 512), doc);

        sig.sign(dsc);

//      dumpDocument(doc, new PrintWriter(System.out));

        DOMValidateContext dvc = new DOMValidateContext
            (kvks, doc.getDocumentElement());
        dvc.setProperty("org.jcp.xml.dsig.secureValidation", false);
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }

        System.out.println();
    }

    static void test_create_signature_with_attr_in_no_namespace()
        throws Exception
    {
        System.out.println
            ("* Generating signature-with-attr-in-no-namespace.xml");

        // create references
        List<Reference> refs = Collections.singletonList
            (fac.newReference("#unknown", sha1));

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, rsaSha1, refs);

        // create object-1
        Document doc = db.newDocument();
        Element nc = doc.createElementNS(null, "NonCommentandus");
        // add attribute with no namespace
        nc.setAttribute("Id", "unknown");
        XMLObject obj = fac.newXMLObject(Collections.singletonList
            (new DOMStructure(nc)), "object-1", null, null);

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature(si, rsa,
                                               Collections.singletonList(obj),
                                               "signature", null);
        DOMSignContext dsc = new DOMSignContext(getPrivateKey("RSA", 512), doc);
        dsc.setIdAttributeNS(nc, null, "Id");

        sig.sign(dsc);

//      dumpDocument(doc, new PrintWriter(System.out));

        DOMValidateContext dvc = new DOMValidateContext
            (kvks, doc.getDocumentElement());
        dvc.setProperty("org.jcp.xml.dsig.secureValidation", false);
        dvc.setIdAttributeNS(nc, null, "Id");
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }

        System.out.println();
    }

    static void test_create_signature_with_empty_id() throws Exception {
        System.out.println("* Generating signature-with-empty-id.xml");

        // create references
        List<Reference> refs = Collections.singletonList
            (fac.newReference("#", sha1));

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, rsaSha1, refs);

        // create object with empty id
        Document doc = db.newDocument();
        XMLObject obj = fac.newXMLObject(Collections.singletonList
            (new DOMStructure(doc.createTextNode("I am the text."))),
            "", "text/plain", null);

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature(si, rsa,
                                               Collections.singletonList(obj),
                                               "signature", null);
        DOMSignContext dsc = new DOMSignContext(getPrivateKey("RSA", 512), doc);
        sig.sign(dsc);

        System.out.println();
    }

    static void test_create_signature_enveloping_over_doc(String filename,
        boolean pass) throws Exception
    {
        System.out.println("* Generating signature-enveloping-over-doc.xml");

        // create reference
        Reference ref = fac.newReference("#object", sha256);

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, rsaSha256,
            Collections.singletonList(ref));

        // create object
        Document doc = null;
        try (FileInputStream fis = new FileInputStream(filename)) {
            doc = db.parse(fis);
        }
        DOMStructure ds = pass ? new DOMStructure(doc.getDocumentElement())
                               : new DOMStructure(doc);
        XMLObject obj = fac.newXMLObject(Collections.singletonList(ds),
            "object", null, "UTF-8");

        // This creates an enveloping signature over the entire XML Document
        XMLSignature sig = fac.newXMLSignature(si, rsa,
                                               Collections.singletonList(obj),
                                               "signature", null);
        DOMSignContext dsc = new DOMSignContext(getPrivateKey("RSA", 1024), doc);
        try {
            sig.sign(dsc);
            if (!pass) {
                // A Document node can only exist at the root of the doc so this
                // should fail
                throw new Exception("Test unexpectedly passed");
            }
        } catch (Exception e) {
            if (!pass) {
                System.out.println("Test failed as expected: " + e);
            } else {
                throw e;
            }
        }

        if (pass) {
            DOMValidateContext dvc = new DOMValidateContext
                (getPublicKey("RSA", 1024), doc.getDocumentElement());
            XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

            if (sig.equals(sig2) == false) {
                throw new Exception
                    ("Unmarshalled signature is not equal to generated signature");
            }
            if (sig2.validate(dvc) == false) {
                throw new Exception("Validation of generated signature failed");
            }
        }

        System.out.println();
    }

    static void test_create_signature_enveloping_dom_level1() throws Exception {
        System.out.println("* Generating signature-enveloping-dom-level1.xml");

        // create reference
        Reference ref = fac.newReference("#object", sha256);

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, rsaSha256,
            Collections.singletonList(ref));

        // create object using DOM Level 1 methods
        Document doc = db.newDocument();
        Element child = doc.createElement("Child");
        child.setAttribute("Version", "1.0");
        child.setAttribute("Id", "child");
        child.setIdAttribute("Id", true);
        child.appendChild(doc.createComment("Comment"));
        XMLObject obj = fac.newXMLObject(
            Collections.singletonList(new DOMStructure(child)),
            "object", null, "UTF-8");

        XMLSignature sig = fac.newXMLSignature(si, rsa,
                                               Collections.singletonList(obj),
                                               "signature", null);
        DOMSignContext dsc = new DOMSignContext(getPrivateKey("RSA", 1024), doc);
        sig.sign(dsc);

        DOMValidateContext dvc = new DOMValidateContext
            (getPublicKey("RSA", 1024), doc.getDocumentElement());
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }

        System.out.println();
    }

    static void test_create_signature() throws Exception {
        System.out.println("* Generating signature.xml");

        // create references
        List<Reference> refs = new ArrayList<>();

        // Reference 1
        refs.add(fac.newReference(STYLESHEET, sha1));

        // Reference 2
        refs.add(fac.newReference
            (STYLESHEET_B64,
            sha1, Collections.singletonList
            (fac.newTransform(Transform.BASE64,
                (TransformParameterSpec) null)), null, null));

        // Reference 3
        refs.add(fac.newReference("#object-1", sha1, Collections.singletonList
            (fac.newTransform(Transform.XPATH,
            new XPathFilterParameterSpec("self::text()"))),
            XMLObject.TYPE, null));

        // Reference 4
        String expr = "\n"
          + " ancestor-or-self::dsig:SignedInfo                  " + "\n"
          + "  and                                               " + "\n"
          + " count(ancestor-or-self::dsig:Reference |           " + "\n"
          + "      here()/ancestor::dsig:Reference[1]) >         " + "\n"
          + " count(ancestor-or-self::dsig:Reference)            " + "\n"
          + "  or                                                " + "\n"
          + " count(ancestor-or-self::node() |                   " + "\n"
          + "      id('notaries')) =                             " + "\n"
          + " count(ancestor-or-self::node())                    " + "\n";

        XPathFilterParameterSpec xfp = new XPathFilterParameterSpec(expr,
            Collections.singletonMap("dsig", XMLSignature.XMLNS));
        refs.add(fac.newReference("", sha1, Collections.singletonList
            (fac.newTransform(Transform.XPATH, xfp)),
            XMLObject.TYPE, null));

        // Reference 5
        refs.add(fac.newReference("#object-2", sha1, Collections.singletonList
            (fac.newTransform
                (Transform.BASE64, (TransformParameterSpec) null)),
            XMLObject.TYPE, null));

        // Reference 6
        refs.add(fac.newReference
            ("#manifest-1", sha1, null, Manifest.TYPE, null));

        // Reference 7
        refs.add(fac.newReference("#signature-properties-1", sha1, null,
            SignatureProperties.TYPE, null));

        // Reference 8
        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform
            (Transform.ENVELOPED, (TransformParameterSpec) null));
        refs.add(fac.newReference("", sha1, transforms, null, null));

        // Reference 9
        transforms.add(fac.newTransform
            (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                (TransformParameterSpec) null));
        refs.add(fac.newReference("", sha1, transforms, null, null));

        // Reference 10
        Transform env = fac.newTransform
            (Transform.ENVELOPED, (TransformParameterSpec) null);
        refs.add(fac.newReference("#xpointer(/)",
            sha1, Collections.singletonList(env), null, null));

        // Reference 11
        transforms.clear();
        transforms.add(fac.newTransform
            (Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform
            (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
             (TransformParameterSpec) null));
        refs.add(fac.newReference("#xpointer(/)", sha1, transforms,
            null, null));

        // Reference 12
        refs.add
            (fac.newReference("#object-3", sha1, null, XMLObject.TYPE, null));

        // Reference 13
        Transform withComments = fac.newTransform
            (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
             (TransformParameterSpec) null);
        refs.add(fac.newReference("#object-3", sha1,
            Collections.singletonList(withComments), XMLObject.TYPE, null));

        // Reference 14
        refs.add(fac.newReference("#xpointer(id('object-3'))", sha1, null,
            XMLObject.TYPE, null));

        // Reference 15
        withComments = fac.newTransform
            (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
             (TransformParameterSpec) null);
        refs.add(fac.newReference("#xpointer(id('object-3'))", sha1,
            Collections.singletonList(withComments), XMLObject.TYPE, null));

        // Reference 16
        refs.add(fac.newReference("#reference-2", sha1));

        // Reference 17
        refs.add(fac.newReference("#manifest-reference-1", sha1, null,
            null, "reference-1"));

        // Reference 18
        refs.add(fac.newReference("#reference-1", sha1, null, null,
            "reference-2"));

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, dsaSha1, refs);

        // create keyinfo
        XPathFilterParameterSpec xpf = new XPathFilterParameterSpec(
            "ancestor-or-self::dsig:X509Data",
            Collections.singletonMap("dsig", XMLSignature.XMLNS));
        RetrievalMethod rm = kifac.newRetrievalMethod("#object-4",
            X509Data.TYPE, Collections.singletonList(fac.newTransform
            (Transform.XPATH, xpf)));
        KeyInfo ki = kifac.newKeyInfo(Collections.singletonList(rm), null);

        Document doc = db.newDocument();

        // create objects
        List<XMLObject> objs = new ArrayList<>();

        // Object 1
        objs.add(fac.newXMLObject(Collections.singletonList
            (new DOMStructure(doc.createTextNode("I am the text."))),
            "object-1", "text/plain", null));

        // Object 2
        objs.add(fac.newXMLObject(Collections.singletonList
            (new DOMStructure(doc.createTextNode("SSBhbSB0aGUgdGV4dC4="))),
            "object-2", "text/plain", Transform.BASE64));

        // Object 3
        Element nc = doc.createElementNS(null, "NonCommentandus");
        nc.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "");
        nc.appendChild(doc.createComment(" Commentandum "));
        objs.add(fac.newXMLObject(Collections.singletonList
            (new DOMStructure(nc)), "object-3", null, null));

        // Manifest
        List<Reference> manRefs = new ArrayList<>();

        // Manifest Reference 1
        manRefs.add(fac.newReference(STYLESHEET,
            sha1, null, null, "manifest-reference-1"));

        // Manifest Reference 2
        manRefs.add(fac.newReference("#reference-1", sha1));

        // Manifest Reference 3
        List<Transform> manTrans = new ArrayList<>();
        Document docxslt = db.parse(new ByteArrayInputStream(xslt.getBytes()));
        Node xslElem = docxslt.getDocumentElement();

        manTrans.add(fac.newTransform(Transform.XSLT,
            new XSLTTransformParameterSpec(new DOMStructure(xslElem))));
        manTrans.add(fac.newTransform(CanonicalizationMethod.INCLUSIVE,
            (TransformParameterSpec) null));
        manRefs.add(fac.newReference("#notaries", sha1, manTrans, null, null));

        objs.add(fac.newXMLObject(Collections.singletonList
            (fac.newManifest(manRefs, "manifest-1")), null, null, null));

        // SignatureProperties
        Element sa = doc.createElementNS("urn:demo", "SignerAddress");
        sa.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "urn:demo");
        Element ip = doc.createElementNS("urn:demo", "IP");
        ip.appendChild(doc.createTextNode("192.168.21.138"));
        sa.appendChild(ip);
        SignatureProperty sp = fac.newSignatureProperty
            (Collections.singletonList(new DOMStructure(sa)),
            "#signature", null);
        SignatureProperties sps = fac.newSignatureProperties
            (Collections.singletonList(sp), "signature-properties-1");
        objs.add(fac.newXMLObject(Collections.singletonList(sps), null,
            null, null));

        // Object 4
        List<Object> xds = new ArrayList<>();
        xds.add("CN=User");
        xds.add(kifac.newX509IssuerSerial
            ("CN=User", new BigInteger("45ef2729", 16)));
        xds.add(signingCert);
        objs.add(fac.newXMLObject(Collections.singletonList
            (kifac.newX509Data(xds)), "object-4", null, null));

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature(si, ki, objs, "signature", null);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        Document envDoc = dbf.newDocumentBuilder().parse
            (new FileInputStream(ENVELOPE));
        Element ys = (Element)
            envDoc.getElementsByTagName("YoursSincerely").item(0);

        DOMSignContext dsc = new DOMSignContext(signingKey, ys);
        dsc.setURIDereferencer(httpUd);

        sig.sign(dsc);

//      StringWriter sw = new StringWriter();
//        dumpDocument(envDoc, sw);

        NodeList nl =
            envDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Couldn't find signature Element");
        }
        Element sigElement = (Element) nl.item(0);

        DOMValidateContext dvc = new DOMValidateContext
            (new X509KeySelector(ks), sigElement);
        dvc.setURIDereferencer(httpUd);
        File f = new File(
            System.getProperty("dir.test.vector.baltimore") +
            System.getProperty("file.separator") +
            "merlin-xmldsig-twenty-three" +
            System.getProperty("file.separator"));
        dvc.setBaseURI(f.toURI().toString());

        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }
        System.out.println();
    }

    private static void dumpDocument(Document doc, Writer w) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
//      trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.transform(new DOMSource(doc), new StreamResult(w));
    }

    private static void test_create_signature_external
        (SignatureMethod sm, KeyInfo ki, Key signingKey, KeySelector ks,
        boolean b64) throws Exception {

        // create reference
        Reference ref;
        if (b64) {
            ref = fac.newReference
                (STYLESHEET_B64,
                sha1, Collections.singletonList
                (fac.newTransform(Transform.BASE64,
                 (TransformParameterSpec) null)), null, null);
        } else {
            ref = fac.newReference(STYLESHEET, sha1);
        }

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, sm,
            Collections.singletonList(ref));

        Document doc = db.newDocument();

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature(si, ki);

        DOMSignContext dsc = new DOMSignContext(signingKey, doc);
        dsc.setURIDereferencer(httpUd);

        sig.sign(dsc);

        DOMValidateContext dvc = new DOMValidateContext
            (ks, doc.getDocumentElement());
        File f = new File(DATA_DIR);
        dvc.setBaseURI(f.toURI().toString());
        dvc.setURIDereferencer(httpUd);

        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }
    }

    private static void test_create_signature_enveloping
        (DigestMethod dm, SignatureMethod sm, KeyInfo ki, Key signingKey,
         KeySelector ks, boolean b64, boolean secVal) throws Exception {

        // create reference
        Reference ref;
        if (b64) {
            ref = fac.newReference("#object", dm, Collections.singletonList
                (fac.newTransform(Transform.BASE64,
                 (TransformParameterSpec) null)), null, null);
        } else {
            ref = fac.newReference("#object", dm);
        }

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(withoutComments, sm,
            Collections.singletonList(ref));

        Document doc = db.newDocument();
        // create Objects
        String text = b64 ? "c29tZSB0ZXh0" : "some text";
        XMLObject obj = fac.newXMLObject(Collections.singletonList
            (new DOMStructure(doc.createTextNode(text))),
            "object", null, null);

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature
            (si, ki, Collections.singletonList(obj), null, null);

        DOMSignContext dsc = new DOMSignContext(signingKey, doc);

        sig.sign(dsc);

//        dumpDocument(doc, new FileWriter("/tmp/foo.xml"));

        DOMValidateContext dvc = new DOMValidateContext
            (ks, doc.getDocumentElement());
        dvc.setProperty("org.jcp.xml.dsig.secureValidation", secVal);
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }
    }

    static void test_create_exc_signature() throws Exception {
        System.out.println("* Generating exc_signature.xml");
        List<Reference> refs = new ArrayList<>(4);

        // create reference 1
        refs.add(fac.newReference
            ("#xpointer(id('to-be-signed'))", sha1,
             Collections.singletonList
                (fac.newTransform(CanonicalizationMethod.EXCLUSIVE,
                 (TransformParameterSpec) null)),
             null, null));

        // create reference 2
        List<String> prefixList = new ArrayList<>(2);
        prefixList.add("bar");
        prefixList.add("#default");
        ExcC14NParameterSpec params = new ExcC14NParameterSpec(prefixList);
        refs.add(fac.newReference
            ("#xpointer(id('to-be-signed'))", sha1,
             Collections.singletonList
                (fac.newTransform(CanonicalizationMethod.EXCLUSIVE, params)),
             null, null));

        // create reference 3
        refs.add(fac.newReference
            ("#xpointer(id('to-be-signed'))", sha1,
             Collections.singletonList(fac.newTransform
                (CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
                 (TransformParameterSpec) null)),
             null, null));

        // create reference 4
        prefixList = new ArrayList<>(2);
        prefixList.add("bar");
        prefixList.add("#default");
        params = new ExcC14NParameterSpec(prefixList);
        refs.add(fac.newReference
            ("#xpointer(id('to-be-signed'))", sha1,
             Collections.singletonList(fac.newTransform
                (CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS, params)),
             null, null));

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(
            fac.newCanonicalizationMethod
                (CanonicalizationMethod.EXCLUSIVE,
                 (C14NMethodParameterSpec) null),
                    dsaSha1, refs);

        // create KeyInfo
        List<XMLStructure> kits = new ArrayList<>(2);
        kits.add(kifac.newKeyValue(validatingKey));
        KeyInfo ki = kifac.newKeyInfo(kits);

        // create Objects
        Document doc = db.newDocument();
        Element baz = doc.createElementNS("urn:bar", "bar:Baz");
        Comment com = doc.createComment(" comment ");
        baz.appendChild(com);
        XMLObject obj = fac.newXMLObject(Collections.singletonList
            (new DOMStructure(baz)), "to-be-signed", null, null);

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature
            (si, ki, Collections.singletonList(obj), null, null);

        Element foo = doc.createElementNS("urn:foo", "Foo");
        foo.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "urn:foo");
        foo.setAttributeNS
            ("http://www.w3.org/2000/xmlns/", "xmlns:bar", "urn:bar");
        doc.appendChild(foo);

        DOMSignContext dsc = new DOMSignContext(signingKey, foo);
        dsc.putNamespacePrefix(XMLSignature.XMLNS, "dsig");

        sig.sign(dsc);

//      dumpDocument(doc, new FileWriter("/tmp/foo.xml"));

        DOMValidateContext dvc = new DOMValidateContext
            (new KeySelectors.KeyValueKeySelector(), foo.getLastChild());
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }
        System.out.println();
    }

    static void test_create_sign_spec() throws Exception {
        System.out.println("* Generating sign-spec.xml");
        List<Reference> refs = new ArrayList<>(2);

        // create reference 1
        List<XPathType> types = new ArrayList<>(3);
        types.add(new XPathType(" //ToBeSigned ", XPathType.Filter.INTERSECT));
        types.add(new XPathType(" //NotToBeSigned ",
            XPathType.Filter.SUBTRACT));
        types.add(new XPathType(" //ReallyToBeSigned ",
            XPathType.Filter.UNION));
        XPathFilter2ParameterSpec xp1 = new XPathFilter2ParameterSpec(types);
        refs.add(fac.newReference("", sha1,
             Collections.singletonList(fac.newTransform(Transform.XPATH2, xp1)),
             null, null));

        // create reference 2
        List<Transform> trans2 = new ArrayList<>(2);
        trans2.add(fac.newTransform(Transform.ENVELOPED,
            (TransformParameterSpec) null));
        XPathFilter2ParameterSpec xp2 = new XPathFilter2ParameterSpec
            (Collections.singletonList
                (new XPathType(" / ", XPathType.Filter.UNION)));
        trans2.add(fac.newTransform(Transform.XPATH2, xp2));
        refs.add(fac.newReference("#signature-value", sha1, trans2, null, null));

        // create SignedInfo
        SignedInfo si = fac.newSignedInfo(
            fac.newCanonicalizationMethod
                (CanonicalizationMethod.INCLUSIVE,
                 (C14NMethodParameterSpec) null),
                    dsaSha1, refs);

        // create KeyInfo
        List<XMLStructure> kits = new ArrayList<>(2);
        kits.add(kifac.newKeyValue(validatingKey));
        List<Object> xds = new ArrayList<>(2);
        xds.add("CN=User");
        xds.add(signingCert);
        kits.add(kifac.newX509Data(xds));
        KeyInfo ki = kifac.newKeyInfo(kits);

        // create XMLSignature
        XMLSignature sig = fac.newXMLSignature
            (si, ki, null, null, "signature-value");

        Document doc = db.newDocument();
        Element tbs1 = doc.createElementNS(null, "ToBeSigned");
        Comment tbs1Com = doc.createComment(" comment ");
        Element tbs1Data = doc.createElementNS(null, "Data");
        Element tbs1ntbs = doc.createElementNS(null, "NotToBeSigned");
        Element tbs1rtbs = doc.createElementNS(null, "ReallyToBeSigned");
        Comment tbs1rtbsCom = doc.createComment(" comment ");
        Element tbs1rtbsData = doc.createElementNS(null, "Data");
        tbs1rtbs.appendChild(tbs1rtbsCom);
        tbs1rtbs.appendChild(tbs1rtbsData);
        tbs1ntbs.appendChild(tbs1rtbs);
        tbs1.appendChild(tbs1Com);
        tbs1.appendChild(tbs1Data);
        tbs1.appendChild(tbs1ntbs);

        Element tbs2 = doc.createElementNS(null, "ToBeSigned");
        Element tbs2Data = doc.createElementNS(null, "Data");
        Element tbs2ntbs = doc.createElementNS(null, "NotToBeSigned");
        Element tbs2ntbsData = doc.createElementNS(null, "Data");
        tbs2ntbs.appendChild(tbs2ntbsData);
        tbs2.appendChild(tbs2Data);
        tbs2.appendChild(tbs2ntbs);

        Element document = doc.createElementNS(null, "Document");
        document.appendChild(tbs1);
        document.appendChild(tbs2);
        doc.appendChild(document);

        DOMSignContext dsc = new DOMSignContext(signingKey, document);

        sig.sign(dsc);

//      dumpDocument(doc, new FileWriter("/tmp/foo.xml"));

        DOMValidateContext dvc = new DOMValidateContext
            (new KeySelectors.KeyValueKeySelector(), document.getLastChild());
        XMLSignature sig2 = fac.unmarshalXMLSignature(dvc);

        if (sig.equals(sig2) == false) {
            throw new Exception
                ("Unmarshalled signature is not equal to generated signature");
        }
        if (sig2.validate(dvc) == false) {
            throw new Exception("Validation of generated signature failed");
        }
        System.out.println();
    }

    // Only print if there is an error.
    static void test_create_detached_signature(
            String canonicalizationMethod, String signatureMethod,
            String digestMethod, String transform, KeyInfoType keyInfo,
            Content contentType, int port, boolean expectedFailure,
            Class expectedException) {

        String title = "\nTest detached signature:"
                + "\n    Canonicalization method: " + canonicalizationMethod
                + "\n    Signature method: " + signatureMethod
                + "\n    Transform: " + transform
                + "\n    Digest method: " + digestMethod
                + "\n    KeyInfoType: " + keyInfo
                + "\n    Content type: " + contentType
                + "\n    Expected failure: " + (expectedFailure ? "yes" : "no")
                + "\n    Expected exception: " + (expectedException == null ?
                            "no" : expectedException.getName());

        try {
            boolean success = test_create_detached_signature0(
                    canonicalizationMethod,
                    signatureMethod,
                    digestMethod,
                    transform,
                    keyInfo,
                    contentType,
                    port);

            if (success && expectedFailure) {
                System.out.println(title);
                System.out.println("Signature validation unexpectedly passed");
                result = false;
            } else if (!success && !expectedFailure) {
                System.out.println(title);
                System.out.println("Signature validation unexpectedly failed");
                result = false;
            } else if (expectedException != null) {
                System.out.println(title);
                System.out.println("Expected " + expectedException
                        + " not thrown");
                result = false;
            }
        } catch (Exception e) {
            if (expectedException == null
                    || !e.getClass().isAssignableFrom(expectedException)) {
                System.out.println(title);
                System.out.println("Unexpected exception: " + e);
                e.printStackTrace(System.out);
                result = false;
            }
        }
    }

    // Print out as little as possible. This method will be called many times.
    static boolean test_create_detached_signature0(String canonicalizationMethod,
            String signatureMethod, String digestMethod, String transform,
            KeyInfoType keyInfo, Content contentType, int port)
            throws Exception {

        System.out.print("-S");
        System.out.flush();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        // Create SignedInfo
        DigestMethod dm = fac.newDigestMethod(digestMethod, null);

        List transformList = null;
        if (transform != null) {
            TransformParameterSpec params = null;
            switch (transform) {
                case Transform.XPATH:
                    params = new XPathFilterParameterSpec("//.");
                    break;
                case Transform.XPATH2:
                    params = new XPathFilter2ParameterSpec(
                            Collections.singletonList(new XPathType("//.",
                                    XPathType.Filter.INTERSECT)));
                    break;
                case Transform.XSLT:
                    Element element = dbf.newDocumentBuilder()
                            .parse(new ByteArrayInputStream(xslt.getBytes()))
                            .getDocumentElement();
                    DOMStructure stylesheet = new DOMStructure(element);
                    params = new XSLTTransformParameterSpec(stylesheet);
                    break;
            }
            transformList = Collections.singletonList(fac.newTransform(
                    transform, params));
        }

        String url = String.format("http://localhost:%d/%s", port, contentType);
        List refs = Collections.singletonList(fac.newReference(url, dm,
                transformList, null, null));

        CanonicalizationMethod cm = fac.newCanonicalizationMethod(
                canonicalizationMethod, (C14NMethodParameterSpec) null);

        SignatureMethod sm = fac.newSignatureMethod(signatureMethod, null);

        Key[] pair = getCachedKeys(signatureMethod);
        Key signingKey = pair[0];
        Key validationKey = pair[1];

        SignedInfo si = fac.newSignedInfo(cm, sm, refs, null);

        // Create KeyInfo
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List list = null;
        if (keyInfo == KeyInfoType.KeyValue) {
            if (validationKey instanceof PublicKey) {
                KeyValue kv = kif.newKeyValue((PublicKey) validationKey);
                list = Collections.singletonList(kv);
            }
        } else if (keyInfo == KeyInfoType.x509data) {
            list = Collections.singletonList(
                    kif.newX509Data(Collections.singletonList("cn=Test")));
        } else if (keyInfo == KeyInfoType.KeyName) {
            list = Collections.singletonList(kif.newKeyName("Test"));
        } else {
            throw new RuntimeException("Unexpected KeyInfo: " + keyInfo);
        }
        KeyInfo ki = list != null ? kif.newKeyInfo(list) : null;

        // Create an empty doc for detached signature
        Document doc = dbf.newDocumentBuilder().newDocument();
        DOMSignContext xsc = new DOMSignContext(signingKey, doc);

        // Generate signature
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(xsc);

        // Save signature
        String signatureString;
        try (StringWriter writer = new StringWriter()) {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            Node parent = xsc.getParent();
            trans.transform(new DOMSource(parent), new StreamResult(writer));
            signatureString = writer.toString();
        }

        System.out.print("V");
        System.out.flush();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(
                signatureString.getBytes())) {
            doc = dbf.newDocumentBuilder().parse(bis);
        }

        NodeList nodeLst = doc.getElementsByTagName("Signature");
        Node node = nodeLst.item(0);
        if (node == null) {
            throw new RuntimeException("Couldn't find Signature element");
        }
        if (!(node instanceof Element)) {
            throw new RuntimeException("Unexpected node type");
        }
        Element sig = (Element) node;

        // Validate signature
        DOMValidateContext vc = new DOMValidateContext(validationKey, sig);
        vc.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
        signature = fac.unmarshalXMLSignature(vc);

        boolean success = signature.validate(vc);
        if (!success) {
            System.out.print("x");
            System.out.flush();
            return false;
        }

        success = signature.getSignatureValue().validate(vc);
        if (!success) {
            System.out.print("X");
            System.out.flush();
            return false;
        }

        return true;
    }

    private static Key[] getCachedKeys(String signatureMethod) {
        return cachedKeys.computeIfAbsent(signatureMethod, sm -> {
            try {
                System.out.print("<create keys for " + sm + ">");
                System.out.flush();
                if (sm.contains("#hmac-")) {
                    // http://...#hmac-sha1 -> hmac-sha1 -> hmacsha1
                    String algName = sm
                            .substring(sm.indexOf('#') + 1)
                            .replace("-", "");
                    KeyGenerator kg = KeyGenerator.getInstance(algName);
                    Key signingKey = kg.generateKey();
                    return new Key[] { signingKey, signingKey};
                } else {
                    KeyPairGenerator kpg;
                    if (sm.contains("#rsa-")
                            || sm.contains("-rsa-MGF1")) {
                        kpg = KeyPairGenerator.getInstance("RSA");
                        kpg.initialize(
                                sm.contains("#sha512-rsa-MGF1") ? 2048 : 1024);
                    } else if (sm.contains("#dsa-")) {
                        kpg = KeyPairGenerator.getInstance("DSA");
                        kpg.initialize(1024);
                    } else if (sm.contains("#ecdsa-")) {
                        kpg = KeyPairGenerator.getInstance("EC");
                        kpg.initialize(256);
                    } else {
                        throw new RuntimeException("Unsupported signature algorithm");
                    }
                    KeyPair kp = kpg.generateKeyPair();
                    return new Key[] { kp.getPrivate(), kp.getPublic()};
                }
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError("Should not happen", e);
            }
        });
    }

    private static final String DSA_Y =
        "070662842167565771936588335128634396171789331656318483584455493822" +
        "400811200853331373030669235424928346190274044631949560438023934623" +
        "71310375123430985057160";
    private static final String DSA_P =
        "013232376895198612407547930718267435757728527029623408872245156039" +
        "757713029036368719146452186041204237350521785240337048752071462798" +
        "273003935646236777459223";
    private static final String DSA_Q =
        "0857393771208094202104259627990318636601332086981";
    private static final String DSA_G =
        "054216440574364751416096484883257051280474283943804743768346673007" +
        "661082626139005426812890807137245973106730741193551360857959820973" +
        "90670890367185141189796";
    private static final String DSA_X =
        "0527140396812450214498055937934275626078768840117";
    private static final String DSA_2048_Y =
        "15119007057343785981993995134621348945077524760182795513668325877793414638620983617627033248732235626178802906346261435991040697338468329634416089753032362617771631199351767336660070462291411472735835843440140283101463231807789628656218830720378705090795271104661936237385140354825159080766174663596286149653433914842868551355716015585570827642835307073681358328172009941968323702291677280809277843998510864653406122348712345584706761165794179850728091522094227603562280855104749858249588234915206290448353957550635709520273178475097150818955098638774564910092913714625772708285992586894795017709678223469405896699928";
    private static final String DSA_2048_P =
        "18111848663142005571178770624881214696591339256823507023544605891411707081617152319519180201250440615163700426054396403795303435564101919053459832890139496933938670005799610981765220283775567361483662648340339405220348871308593627647076689407931875483406244310337925809427432681864623551598136302441690546585427193224254314088256212718983105131138772434658820375111735710449331518776858786793875865418124429269409118756812841019074631004956409706877081612616347900606555802111224022921017725537417047242635829949739109274666495826205002104010355456981211025738812433088757102520562459649777989718122219159982614304359";
    private static final String DSA_2048_Q =
        "19689526866605154788513693571065914024068069442724893395618704484701";
    private static final String DSA_2048_G =
        "2859278237642201956931085611015389087970918161297522023542900348087718063098423976428252369340967506010054236052095950169272612831491902295835660747775572934757474194739347115870723217560530672532404847508798651915566434553729839971841903983916294692452760249019857108409189016993380919900231322610083060784269299257074905043636029708121288037909739559605347853174853410208334242027740275688698461842637641566056165699733710043802697192696426360843173620679214131951400148855611740858610821913573088059404459364892373027492936037789337011875710759208498486908611261954026964574111219599568903257472567764789616958430";
    private static final String DSA_2048_X =
        "14562787764977288900757387442281559936279834964901963465277698843172";
    private static final String RSA_MOD =
        "010800185049102889923150759252557522305032794699952150943573164381" +
        "936603255999071981574575044810461362008102247767482738822150129277" +
        "490998033971789476107463";
    private static final String RSA_PRIV =
        "016116973584421969795445996229612671947635798429212816611707210835" +
        "915586591340598683996088487065438751488342251960069575392056288063" +
        "6800379454345804879553";
    private static final String RSA_PUB = "065537";
    private static final String RSA_1024_MOD = "098871307553789439961130765" +
        "909423744508062468450669519128736624058048856940468016843888594585" +
        "322862378444314635412341974900625010364163960238734457710620107530" +
        "573945081856371709138380902553309075505688814637544923038853658690" +
        "857672483016239697038853418682988686871489963827000080098971762923" +
        "833614557257607521";
    private static final String RSA_1024_PRIV = "03682574144968491431483287" +
        "297021581096848810374110568017963075809477047466189822987258068867" +
        "704855380407747867998863645890602646601140183818953428006646987710" +
        "237008997971129772408397621801631622129297063463868593083106979716" +
        "204903524890556839550490384015324575598723478554854070823335021842" +
        "210112348400928769";
    private static final String RSA_2048_MOD = "243987087691547796017401146540"
        + "9844666035826535295137885613771811531602666348704672255163984907599"
        + "4298308997053582963763109207465354916871136820987101812436158377530"
        + "6117270010853232249007544652859474372258057062943608962079402484091"
        + "8121307687901225514249308620012025884376216406019656605767311580224"
        + "4715304950770504195751384382230005665573033547124060755957932161045"
        + "7288008201789401237690181537646952377591671113513382933711547044631"
        + "6055957820531234310030119265612054594720774653570278810236807313332"
        + "5293876225940483622056721445101719346295263740434720907474414905706"
        + "086605825077661246082956613711071075569880930102141";
    private static final String RSA_2048_PRIV = "12265063405401593206575340300"
        + "5824698296458954796982342251774894076489082263237675553422307220014"
        + "4395010131540855227949365446755185799985229111139387016816011165826"
        + "5498929552020323994756478872375078784799489891112924298115119573429"
        + "3677627114115546751555523555375278381312502020990154549150867571006"
        + "4470674155961982582802981649643127000520693025433874996570667724459"
        + "3395670697152709457274026580106078581585077146782827694403672461289"
        + "9143004401242754355097671446183871158504602884373174300123820136505"
        + "6449932139773607305129273545117363975014750743804523418307647791195"
        + "6408859873123458434820062206102268853256685162004893";
    private static final String EC_P256_X =
        "335863644451761614592446380116804721648611739647823420286081723541" +
        "6166183710";
    private static final String EC_P256_Y =
        "951559601159729477487064127150143688502130342917782252098602422796" +
        "95457910701";
    private static final String EC_P256_S =
        "425976209773168452211813225517384419928639977904006759709292218082" +
        "7440083936";
    private static final ECParameterSpec EC_P256_PARAMS = initECParams(
        "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
        "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC",
        "5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B",
        "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296",
        "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5",
        "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551",
        1
    );
    private static final String EC_P384_X =
        "12144058647679082341340699736608428955270957565259459672517275506071643671835484144490620216582303669654008841724053";
    private static final String EC_P384_Y =
        "18287745972107701566600963632634101287058332546756092926848497481238534346489545826483592906634896557151987868614320";
    private static final String EC_P384_S =
        "10307785759830534742680442271492590599236624208247590184679565032330507874096079979152605984203102224450595283943382";
    private static final ECParameterSpec EC_P384_PARAMS = initECParams(
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF",
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC",
        "B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF",
        "AA87CA22BE8B05378EB1C71EF320AD746E1D3B628BA79B9859F741E082542A385502F25DBF55296C3A545E3872760AB7",
        "3617DE4A96262C6F5D9E98BF9292DC29F8F41DBD289A147CE9DA3113B5F0B8C00A60B1CE1D7E819D7A431D7C90EA0E5F",
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973",
        1
    );
    private static final String EC_P521_X =
        "4157918188927862838251799402582135611021257663417126086145819679867926857146776190737187582274664373117054717389603317411991660346043842712448912355335343997";
    private static final String EC_P521_Y =
        "4102838062751704796157456866854813794620023146924181568434486703918224542844053923233919899911519054998554969832861957437850996213216829205401947264294066288";
    private static final String EC_P521_S =
        "4857798533181496041050215963883119936300918353498701880968530610687256097257307590162398707429640390843595868713096292822034014722985178583665959048714417342";
    private static final ECParameterSpec EC_P521_PARAMS = initECParams(
        "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
        "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC",
        "0051953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F00",
        "00C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66",
        "011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650",
        "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409",
        1
    );

    private static ECParameterSpec initECParams(
            String sfield, String a, String b, String gx, String gy,
            String n, int h) {
        ECField field = new ECFieldFp(bigInt(sfield));
        EllipticCurve curve = new EllipticCurve(field,
                                                bigInt(a), bigInt(b));
        ECPoint g = new ECPoint(bigInt(gx), bigInt(gy));
        return new ECParameterSpec(curve, g, bigInt(n), h);
    }

    private static BigInteger bigInt(String s) {
        return new BigInteger(s, 16);
    }
    private static PublicKey getPublicKey(String algo, int keysize)
        throws Exception {
        KeyFactory kf = KeyFactory.getInstance(algo);
        KeySpec kspec;
        if (algo.equalsIgnoreCase("DSA")) {
            if (keysize == 1024) {
                kspec = new DSAPublicKeySpec(new BigInteger(DSA_Y),
                                             new BigInteger(DSA_P),
                                             new BigInteger(DSA_Q),
                                             new BigInteger(DSA_G));
            } else if (keysize == 2048) {
                kspec = new DSAPublicKeySpec(new BigInteger(DSA_2048_Y),
                                             new BigInteger(DSA_2048_P),
                                             new BigInteger(DSA_2048_Q),
                                             new BigInteger(DSA_2048_G));
            } else throw new RuntimeException("Unsupported keysize:" + keysize);
        } else if (algo.equalsIgnoreCase("RSA")) {
            if (keysize == 512) {
                kspec = new RSAPublicKeySpec(new BigInteger(RSA_MOD),
                                             new BigInteger(RSA_PUB));
            } else if (keysize == 1024) {
                kspec = new RSAPublicKeySpec(new BigInteger(RSA_1024_MOD),
                                             new BigInteger(RSA_PUB));
            } else if (keysize == 2048) {
                kspec = new RSAPublicKeySpec(new BigInteger(RSA_2048_MOD),
                                             new BigInteger(RSA_PUB));
            } else throw new RuntimeException("Unsupported keysize:" + keysize);
        } else throw new RuntimeException("Unsupported key algorithm " + algo);
        return kf.generatePublic(kspec);
    }

    private static PublicKey getECPublicKey(String curve) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        String x, y;
        ECParameterSpec params;
        switch (curve) {
            case "P256":
                x = EC_P256_X;
                y = EC_P256_Y;
                params = EC_P256_PARAMS;
                break;
            case "P384":
                x = EC_P384_X;
                y = EC_P384_Y;
                params = EC_P384_PARAMS;
                break;
            case "P521":
                x = EC_P521_X;
                y = EC_P521_Y;
                params = EC_P521_PARAMS;
                break;
            default:
                throw new Exception("Unsupported curve: " + curve);
        }
        KeySpec kspec = new ECPublicKeySpec(new ECPoint(new BigInteger(x),
                                                        new BigInteger(y)),
                                            params);
        return kf.generatePublic(kspec);
    }

    private static PrivateKey getPrivateKey(String algo, int keysize)
        throws Exception {
        KeyFactory kf = KeyFactory.getInstance(algo);
        KeySpec kspec;
        if (algo.equalsIgnoreCase("DSA")) {
            if (keysize == 1024) {
                kspec = new DSAPrivateKeySpec
                    (new BigInteger(DSA_X), new BigInteger(DSA_P),
                     new BigInteger(DSA_Q), new BigInteger(DSA_G));
            } else if (keysize == 2048) {
                kspec = new DSAPrivateKeySpec
                    (new BigInteger(DSA_2048_X), new BigInteger(DSA_2048_P),
                     new BigInteger(DSA_2048_Q), new BigInteger(DSA_2048_G));
            } else throw new RuntimeException("Unsupported keysize:" + keysize);
        } else if (algo.equalsIgnoreCase("RSA")) {
            if (keysize == 512) {
                kspec = new RSAPrivateKeySpec
                    (new BigInteger(RSA_MOD), new BigInteger(RSA_PRIV));
            } else if (keysize == 1024) {
                kspec = new RSAPrivateKeySpec(new BigInteger(RSA_1024_MOD),
                        new BigInteger(RSA_1024_PRIV));
            } else if (keysize == 2048) {
                kspec = new RSAPrivateKeySpec(new BigInteger(RSA_2048_MOD),
                        new BigInteger(RSA_2048_PRIV));
            } else throw new RuntimeException("Unsupported key algorithm " + algo);
        } else throw new RuntimeException("Unsupported key algorithm " + algo);
        return kf.generatePrivate(kspec);
    }

    private static PrivateKey getECPrivateKey(String curve) throws Exception {
        String s;
        ECParameterSpec params;
        switch (curve) {
            case "P256":
                s = EC_P256_S;
                params = EC_P256_PARAMS;
                break;
            case "P384":
                s = EC_P384_S;
                params = EC_P384_PARAMS;
                break;
            case "P521":
                s = EC_P521_S;
                params = EC_P521_PARAMS;
                break;
            default:
                throw new Exception("Unsupported curve: " + curve);
        }
        KeyFactory kf = KeyFactory.getInstance("EC");
        KeySpec kspec = new ECPrivateKeySpec(new BigInteger(s), params);
        return kf.generatePrivate(kspec);
    }

    private static SecretKey getSecretKey(final byte[] secret) {
        return new SecretKey() {
            public String getFormat()   { return "RAW"; }
            public byte[] getEncoded()  { return secret; }
            public String getAlgorithm(){ return "SECRET"; }
        };
    }

    /**
     * This URIDereferencer returns locally cached copies of http content to
     * avoid test failures due to network glitches, etc.
     */
    private static class HttpURIDereferencer implements URIDereferencer {
        private URIDereferencer defaultUd;

        HttpURIDereferencer() {
            defaultUd = XMLSignatureFactory.getInstance().getURIDereferencer();
        }

        public Data dereference(final URIReference ref, XMLCryptoContext ctx)
        throws URIReferenceException {
            String uri = ref.getURI();
            if (uri.equals(STYLESHEET) || uri.equals(STYLESHEET_B64)) {
                try {
                    FileInputStream fis = new FileInputStream(new File
                        (DATA_DIR, uri.substring(uri.lastIndexOf('/'))));
                    return new OctetStreamData(fis,ref.getURI(),ref.getType());
                } catch (Exception e) { throw new URIReferenceException(e); }
            }

            // fallback on builtin deref
            return defaultUd.dereference(ref, ctx);
        }
    }

    // local http server
    static class Http implements HttpHandler, AutoCloseable {

        private final HttpServer server;

        private Http(HttpServer server) {
            this.server = server;
        }

        static Http startServer() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            return new Http(server);
        }

        void start() {
            server.createContext("/", this);
            server.start();
        }

        void stop() {
            server.stop(0);
        }

        int getPort() {
            return server.getAddress().getPort();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String type;
                String path = t.getRequestURI().getPath();
                if (path.startsWith("/")) {
                    type = path.substring(1);
                } else {
                    type = path;
                }

                String contentTypeHeader = "";
                byte[] output = new byte[] {};
                int code = 200;
                Content testContentType = Content.valueOf(type);
                switch (testContentType) {
                    case Base64:
                        contentTypeHeader = "application/octet-stream";
                        output = "VGVzdA==".getBytes();
                        break;
                    case Text:
                        contentTypeHeader = "text/plain";
                        output = "Text".getBytes();
                        break;
                    case Xml:
                        contentTypeHeader = "application/xml";
                        output = "<tag>test</tag>".getBytes();
                        break;
                    case NotExisitng:
                        code = 404;
                        break;
                    default:
                        throw new IOException("Unknown test content type");
                }

                t.getResponseHeaders().set("Content-Type", contentTypeHeader);
                t.sendResponseHeaders(code, output.length);
                t.getResponseBody().write(output);
            } catch (IOException e) {
                System.out.println("Exception: " + e);
                t.sendResponseHeaders(500, 0);
            }
            t.close();
        }

        @Override
        public void close() {
            stop();
        }
    }
}
