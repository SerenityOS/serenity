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
 * @bug 8241306
 * @summary Tests for the jdk.xml.dsig.secureValidationPolicy security property
 *          on the RSASSA-PSS signature method
 * @library /test/lib
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 */
import jdk.test.lib.Asserts;
import jdk.test.lib.security.XMLUtils;
import jdk.test.lib.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.spec.RSAPSSParameterSpec;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Iterator;
import java.util.Objects;

import static java.security.spec.PSSParameterSpec.TRAILER_FIELD_BC;

public class SecureValidation {

    public static void main(String[] args) throws Exception {

        Document doc = XMLUtils.string2doc("<a><b>Text</b>Raw</a>");

        CertAndKeyGen g = new CertAndKeyGen("RSASSA-PSS", "RSASSA-PSS");
        g.generate(2048);
        X509Certificate cert = g.getSelfCertificate(new X500Name("CN=Me"), 100);
        PrivateKey privateKey = g.getPrivateKey();
        PSSParameterSpec pspec = new PSSParameterSpec("SHA-384", "MGF1",
                MGF1ParameterSpec.SHA512, 48, TRAILER_FIELD_BC);

        // Sign with PSS with SHA-384 and SHA-512
        Document signed = XMLUtils.signer(privateKey, cert)
                .dm(DigestMethod.SHA384)
                .sm(SignatureMethod.RSA_PSS, new RSAPSSParameterSpec(pspec))
                .sign(doc);

        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return switch (prefix) {
                    case "ds" -> "http://www.w3.org/2000/09/xmldsig#";
                    case "pss" -> "http://www.w3.org/2007/05/xmldsig-more#";
                    default -> throw new IllegalArgumentException();
                };
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        });

        var validator = XMLUtils.validator();
        XMLUtils.addPolicy("disallowAlg " + DigestMethod.SHA256);
        Element e;

        // 1. Modify the MGF1 digest algorithm in PSSParams to SHA-256
        e = (Element) xp.evaluate(
                "/a/ds:Signature/ds:SignedInfo/ds:SignatureMethod" +
                        "/pss:RSAPSSParams/pss:MaskGenerationFunction/ds:DigestMethod",
                signed, XPathConstants.NODE);
        e.setAttribute("Algorithm", DigestMethod.SHA256);

        // When secureValidation is true, validate() throws an exception
        Utils.runAndCheckException(() -> validator.secureValidation(true).validate(signed),
                t -> Asserts.assertTrue(t instanceof MarshalException
                    && t.getMessage().contains("in MGF1")
                    && t.getMessage().contains(DigestMethod.SHA256), Objects.toString(t)));
        // When secureValidation is false, validate() returns false
        Asserts.assertFalse(validator.secureValidation(false).validate(signed));

        // Revert the change and confirm validate() returns true
        e.setAttribute("Algorithm", DigestMethod.SHA512);
        Asserts.assertTrue(validator.secureValidation(true).validate(signed));

        // 2. Modify the digest algorithm in PSSParams to SHA-256
        e = (Element) xp.evaluate(
                "/a/ds:Signature/ds:SignedInfo/ds:SignatureMethod" +
                        "/pss:RSAPSSParams/ds:DigestMethod",
                signed, XPathConstants.NODE);
        e.setAttribute("Algorithm", DigestMethod.SHA256);

        // When secureValidation is true, validate() throws an exception
        Utils.runAndCheckException(() -> validator.secureValidation(true).validate(signed),
                t -> Asserts.assertTrue(t instanceof MarshalException
                        && t.getMessage().contains("in PSS")
                        && t.getMessage().contains(DigestMethod.SHA256), Objects.toString(t)));
        // When secureValidation is false, validate() returns false
        Asserts.assertFalse(validator.secureValidation(false).validate(signed));

        // 3. Modify the digest algorithm in PSSParams to SHA-512
        e.setAttribute("Algorithm", DigestMethod.SHA512);

        // No matter if secureValidation is true or false, validate()
        // returns false. This means the policy allows it.
        Asserts.assertFalse(validator.secureValidation(true).validate(signed));
        Asserts.assertFalse(validator.secureValidation(false).validate(signed));
    }
}
