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
 * @bug 4635230 6365103 6366054 6824440 7131084 8046724 8079693
 * @summary Basic unit tests for validating XML Signatures with JSR 105
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 *          java.xml.crypto/org.jcp.xml.dsig.internal.dom
 * @library /test/lib
 * @compile -XDignore.symbol.file KeySelectors.java SignatureValidator.java
 *     X509KeySelector.java ValidationTests.java
 * @run main/othervm ValidationTests
 * @author Sean Mullan
 */
import java.io.File;
import java.io.FileInputStream;
import java.security.*;
import javax.xml.crypto.Data;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;

import jdk.test.lib.security.SecurityUtils;

public class ValidationTests {

    private static SignatureValidator validator;
    private final static String DIR = System.getProperty("test.src", ".");
    private final static String DATA_DIR =
        DIR + System.getProperty("file.separator") + "data";
    private final static String KEYSTORE =
        DATA_DIR + System.getProperty("file.separator") + "certs" +
        System.getProperty("file.separator") + "xmldsig.jks";
    private final static String STYLESHEET =
        "http://www.w3.org/TR/xml-stylesheet";
    private final static String STYLESHEET_B64 =
        "http://www.w3.org/Signature/2002/04/xml-stylesheet.b64";

    static class Test {
        String file;
        KeySelector ks;
        Class exception;

        Test(String file, KeySelector ks, Class exception) {
            this.file = file;
            this.ks = ks;
            this.exception = exception;
        }

        // XMLSignatureException is expected by default
        Test(String file, KeySelector ks) {
            this(file, ks, XMLSignatureException.class);
        }
    }

    static KeySelector skks;
    static {
        try {
            skks =
                new KeySelectors.SecretKeySelector("secret".getBytes("ASCII"));
        } catch (Exception e) {
            //should not occur
        }
    }
    private final static KeySelector SKKS = skks;
    private final static KeySelector KVKS =
        new KeySelectors.KeyValueKeySelector();
    private final static KeySelector CKS =
        new KeySelectors.CollectionKeySelector(new File(DATA_DIR));
    private final static KeySelector RXKS =
        new KeySelectors.RawX509KeySelector();
    private final static KeySelector XKS = null;
    private static URIDereferencer httpUd = null;

    private final static Test[] VALID_TESTS = {
        new Test("signature-enveloped-dsa.xml", KVKS),
        new Test("signature-enveloping-b64-dsa.xml", KVKS),
        new Test("signature-enveloping-dsa.xml", KVKS),
        new Test("signature-enveloping-rsa.xml", KVKS),
        new Test("signature-enveloping-p256-sha1.xml", KVKS),
        new Test("signature-enveloping-p384-sha1.xml", KVKS),
        new Test("signature-enveloping-p521-sha1.xml", KVKS),
        new Test("signature-enveloping-hmac-sha1.xml", SKKS),
        new Test("signature-external-dsa.xml", KVKS),
        new Test("signature-external-b64-dsa.xml", KVKS),
        new Test("signature-retrievalmethod-rawx509crt.xml", CKS),
        new Test("signature-keyname.xml", CKS),
        new Test("signature-x509-crt-crl.xml", RXKS),
        new Test("signature-x509-crt.xml", RXKS),
        new Test("signature-x509-is.xml", CKS),
        new Test("signature-x509-ski.xml", CKS),
        new Test("signature-x509-sn.xml", CKS),
        new Test("signature.xml", XKS),
        new Test("exc-signature.xml", KVKS),
        new Test("sign-spec.xml", RXKS),
        new Test("xmldsig-xfilter2.xml", KVKS)
    };

    private final static Test[] INVALID_TESTS = {
        new Test("signature-enveloping-hmac-sha1-40.xml", SKKS),
        new Test("signature-enveloping-hmac-sha1-trunclen-0-attack.xml", SKKS),
        new Test("signature-enveloping-hmac-sha1-trunclen-8-attack.xml", SKKS),
        new Test("signature-extra-text-in-signed-info.xml", SKKS,
                MarshalException.class),
        new Test("signature-wrong-canonicalization-method-algorithm.xml", SKKS,
                MarshalException.class),
        new Test("signature-wrong-transform-algorithm.xml", SKKS,
                MarshalException.class),
        new Test("signature-no-reference-uri.xml", SKKS),
        new Test("signature-wrong-signature-method-algorithm.xml", SKKS,
                MarshalException.class),
        new Test("signature-wrong-tag-names.xml", SKKS, MarshalException.class)
    };

    public static void main(String args[]) throws Exception {
        // Re-enable sha1 algs
        SecurityUtils.removeAlgsFromDSigPolicy("sha1");

        httpUd = new HttpURIDereferencer();

        validator = new SignatureValidator(new File(DATA_DIR));

        boolean atLeastOneFailed = false;
        for (Test test : VALID_TESTS) {
            System.out.println("Validating " + test.file);
            if (test_signature(test)) {
                System.out.println("PASSED");
            } else {
                System.out.println("FAILED");
                atLeastOneFailed = true;
            }
        }
        // test with reference caching enabled
        System.out.println("Validating sign-spec.xml with caching enabled");
        if (test_signature(new Test("sign-spec.xml", RXKS), true)) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
            atLeastOneFailed = true;
        }

        for (Test test : INVALID_TESTS) {
            System.out.println("Validating " + test.file);
            try {
                test_signature(test);
                System.out.println("FAILED");
                atLeastOneFailed = true;
            } catch (Exception e) {
                System.out.println("Exception: " + e);
                if (e.getClass() != test.exception) {
                    System.out.println("FAILED: unexpected exception");
                    atLeastOneFailed = true;
                } else {
                    System.out.println("PASSED");
                }
            }
        }

        if (atLeastOneFailed) {
            throw new Exception
                ("At least one signature did not validate as expected");
        }
    }

    public static boolean test_signature(Test test) throws Exception {
        return test_signature(test, false);
    }

    public static boolean test_signature(Test test, boolean cache)
        throws Exception
    {
        if (test.ks == null) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(KEYSTORE)) {
                keystore.load(fis, "changeit".toCharArray());
                test.ks = new X509KeySelector(keystore, false);
            }
        }
        return validator.validate(test.file, test.ks, httpUd, cache);
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
}
