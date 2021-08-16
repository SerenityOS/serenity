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

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.security.XMLUtils;
import org.w3c.dom.Document;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.RSAPSSParameterSpec;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

/**
 * @test
 * @bug 8241306
 * @library /test/lib
 * @modules java.xml.crypto
 * @summary Testing marshal and unmarshal of RSAPSSParameterSpec
 */
public class PSSSpec {
    private static final String P2SM = "//ds:Signature/ds:SignedInfo/ds:SignatureMethod";
    private static final String P2PSS = P2SM + "/pss:RSAPSSParams";
    private static final String P2MGF = P2PSS + "/pss:MaskGenerationFunction";

    private static final PSSParameterSpec DEFAULT_SPEC
            = new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), 32, PSSParameterSpec.TRAILER_FIELD_BC);

    public static void main(String[] args) throws Exception {
        unmarshal();
        marshal();
        spec();
    }

    static void unmarshal() throws Exception {
        // Original document with all elements
        Document doc = XMLUtils.string2doc("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ds:Signature
                        xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
                        xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
                        xmlns:pss="http://www.w3.org/2007/05/xmldsig-more#">
                    <ds:SignedInfo>
                        <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#WithComments"/>
                        <ds:SignatureMethod Algorithm="http://www.w3.org/2007/05/xmldsig-more#rsa-pss">
                            <pss:RSAPSSParams>
                                <ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
                                <pss:MaskGenerationFunction Algorithm="http://www.w3.org/2007/05/xmldsig-more#MGF1">
                                    <ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#sha384"/>
                                </pss:MaskGenerationFunction>
                                <pss:SaltLength>32</pss:SaltLength>
                                <pss:TrailerField>2</pss:TrailerField>
                            </pss:RSAPSSParams>
                        </ds:SignatureMethod>
                        <ds:Reference>
                            <ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
                            <ds:DigestValue>abc=</ds:DigestValue>
                        </ds:Reference>
                    </ds:SignedInfo>
                    <ds:SignatureValue>abc=</ds:SignatureValue>
                </ds:Signature>
                """);

        // Unknown DigestMethod
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withAttribute(doc, P2PSS + "/ds:DigestMethod", "Algorithm", "http://unknown")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Invalid digest algorithm"), e.getMessage()));
        // Unknown MGF algorithm
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withAttribute(doc, P2MGF, "Algorithm", "http://unknown")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Unknown MGF algorithm"), e.getMessage()));
        // Unknown MGF DigestMethod
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withAttribute(doc, P2MGF + "/ds:DigestMethod", "Algorithm", "http://unknown")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Invalid digest algorithm"), e.getMessage()));
        // Invalid SaltLength
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withText(doc, P2PSS + "/pss:SaltLength", "big")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Invalid salt length supplied"), e.getMessage()));
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withText(doc, P2PSS + "/pss:SaltLength", "-1")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Invalid salt length supplied"), e.getMessage()));
        // Invalid TrailerField
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withText(doc, P2PSS + "/pss:TrailerField", "small")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Invalid trailer field supplied"), e.getMessage()));
        Utils.runAndCheckException(
                () -> getSpec(XMLUtils.withText(doc, P2PSS + "/pss:TrailerField", "-1")),
                e -> Asserts.assertTrue(e instanceof MarshalException && e.getMessage().contains("Invalid trailer field supplied"), e.getMessage()));

        // Spec in original doc
        checkSpec(doc, new PSSParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-384"), 32, 2));
        // Default MGF1 dm is same as PSS dm
        checkSpec(XMLUtils.withoutNode(doc, P2MGF + "/ds:DigestMethod"), // No dm in MGF
                new PSSParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-512"), 32, 2));
        checkSpec(XMLUtils.withoutNode(doc, P2MGF), // No MGF at all
                new PSSParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-512"), 32, 2));
        // Default TrailerField is 1
        checkSpec(XMLUtils.withoutNode(doc, P2PSS + "/pss:TrailerField"),
                new PSSParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-384"), 32, 1));
        // Default SaltLength is dm's SaltLength
        checkSpec(XMLUtils.withoutNode(doc, P2PSS + "/pss:SaltLength"),
                new PSSParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-384"), 64, 2));
        // Default DigestMethod is 256
        checkSpec(XMLUtils.withoutNode(doc, P2PSS + "/ds:DigestMethod"),
                new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-384"), 32, 2));
        // Default PSS is SHA-256
        checkSpec(XMLUtils.withoutNode(doc, P2PSS), DEFAULT_SPEC);
    }

    static void marshal() throws Exception {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        var signer = XMLUtils.signer(keyPairGenerator.generateKeyPair().getPrivate());
        PSSParameterSpec spec;
        Document doc = XMLUtils.string2doc("<a>x</a>");
        Document signedDoc;

        // Default sm. No need to describe at all
        signer.sm(SignatureMethod.RSA_PSS, new RSAPSSParameterSpec(DEFAULT_SPEC));
        signedDoc = signer.sign(doc);
        Asserts.assertTrue(!XMLUtils.sub(signedDoc, P2SM).hasChildNodes());

        // Special salt.
        spec = new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), 40, PSSParameterSpec.TRAILER_FIELD_BC);
        signer.sm(SignatureMethod.RSA_PSS, new RSAPSSParameterSpec(spec));
        signedDoc = signer.sign(doc);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/pss:SaltLength").getTextContent().equals("40"));
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2MGF) == null);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/ds:DigestMethod") == null);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/pss:TrailerField") == null);

        // Different MGF1 dm
        spec = new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-384"), 32, PSSParameterSpec.TRAILER_FIELD_BC);
        signer.sm(SignatureMethod.RSA_PSS, new RSAPSSParameterSpec(spec));
        signedDoc = signer.sign(doc);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2MGF + "/ds:DigestMethod").getAttribute("Algorithm").equals(DigestMethod.SHA384));
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/ds:DigestMethod") == null);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/pss:SaltLength") == null);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/pss:TrailerField") == null);

        // Non default dm only
        spec = new PSSParameterSpec("SHA-384", "MGF1", new MGF1ParameterSpec("SHA-384"), 48, PSSParameterSpec.TRAILER_FIELD_BC);
        signer.sm(SignatureMethod.RSA_PSS, new RSAPSSParameterSpec(spec));
        signedDoc = signer.sign(doc);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/ds:DigestMethod").getAttribute("Algorithm").equals(DigestMethod.SHA384));
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2MGF) == null);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/pss:SaltLength") == null);
        Asserts.assertTrue(XMLUtils.sub(signedDoc, P2PSS + "/pss:TrailerField") == null);
    }

    static void spec() throws Exception {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        SignatureMethod sm = fac.newSignatureMethod(SignatureMethod.RSA_PSS, null);
        Asserts.assertTrue(equals(
                ((RSAPSSParameterSpec)sm.getParameterSpec()).getPSSParameterSpec(),
                DEFAULT_SPEC));

        PSSParameterSpec special = new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-384"), 33, 2);
        sm = fac.newSignatureMethod(SignatureMethod.RSA_PSS, new RSAPSSParameterSpec(special));
        Asserts.assertTrue(equals(
                ((RSAPSSParameterSpec)sm.getParameterSpec()).getPSSParameterSpec(),
                special));
    }

    static PSSParameterSpec getSpec(Document doc) throws Exception {
        var signatureNode = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature").item(0);
        DOMValidateContext valContext = new DOMValidateContext(new SecretKeySpec(new byte[1], "WHAT"), signatureNode);
        valContext.setProperty("org.jcp.xml.dsig.secureValidation", false);
        var signedInfo = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(valContext).getSignedInfo();
        var spec = signedInfo.getSignatureMethod().getParameterSpec();
        if (spec instanceof RSAPSSParameterSpec pspec) {
            return pspec.getPSSParameterSpec();
        } else {
            Asserts.fail("Not PSSParameterSpec: " + spec.getClass());
            return null;
        }
    }

    static void checkSpec(Document doc, PSSParameterSpec expected) throws Exception {
        Asserts.assertTrue(equals(getSpec(doc), expected));
    }

    static boolean equals(PSSParameterSpec p1, PSSParameterSpec p2) {
        return p1.getDigestAlgorithm().equals(p2.getDigestAlgorithm())
                && p1.getSaltLength() == p2.getSaltLength()
                && p1.getTrailerField() == p2.getTrailerField()
                && p1.getMGFAlgorithm().equals(p2.getMGFAlgorithm())
                && ((MGF1ParameterSpec) p1.getMGFParameters()).getDigestAlgorithm()
                .equals(((MGF1ParameterSpec) p2.getMGFParameters()).getDigestAlgorithm());
    }
}
