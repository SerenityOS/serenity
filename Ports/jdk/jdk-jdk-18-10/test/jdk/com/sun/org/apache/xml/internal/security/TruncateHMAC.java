/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test %I% %E%
 * @bug 6824440 6858484
 * @summary Check that Apache XMLSec APIs will not accept HMAC truncation
 *    lengths less than minimum bound
 * @modules java.xml.crypto/com.sun.org.apache.xml.internal.security
 *          java.xml.crypto/com.sun.org.apache.xml.internal.security.c14n
 *          java.xml.crypto/com.sun.org.apache.xml.internal.security.signature
 *          java.xml.crypto/com.sun.org.apache.xml.internal.security.utils
 * @compile -XDignore.symbol.file TruncateHMAC.java
 * @run main TruncateHMAC
 */

import java.io.File;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.security.Init;
import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.signature.XMLSignature;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureException;
import com.sun.org.apache.xml.internal.security.utils.Constants;


public class TruncateHMAC {

    private final static String DIR = System.getProperty("test.src", ".");
    private static DocumentBuilderFactory dbf = null;
    private static boolean atLeastOneFailed = false;

    public static void main(String[] args) throws Exception {

        Init.init();
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        validate("signature-enveloping-hmac-sha1-trunclen-0-attack.xml", false);
        validate("signature-enveloping-hmac-sha1-trunclen-8-attack.xml", false);
        // this one should pass
        validate("signature-enveloping-hmac-sha1.xml", true);
        generate_hmac_sha1_40();

        if (atLeastOneFailed) {
            throw new Exception
                ("At least one signature did not validate as expected");
        }
    }

    private static void validate(String data, boolean pass) throws Exception {
        System.out.println("Validating " + data);
        File file = new File(DIR, data);

        Document doc = dbf.newDocumentBuilder().parse(file);
        NodeList nl =
            doc.getElementsByTagNameNS(Constants.SignatureSpecNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Couldn't find signature Element");
        }
        Element sigElement = (Element) nl.item(0);
        try {
        XMLSignature signature = new XMLSignature
            (sigElement, file.toURI().toString());
        SecretKey sk = signature.createSecretKey("secret".getBytes("ASCII"));
            System.out.println
                ("Validation status: " + signature.checkSignatureValue(sk));
            if (!pass) {
                System.out.println("FAILED");
                atLeastOneFailed = true;
            } else {
                System.out.println("PASSED");
            }
        } catch (XMLSignatureException xse) {
            System.out.println(xse.getMessage());
            if (!pass) {
                System.out.println("PASSED");
            } else {
                System.out.println("FAILED");
                atLeastOneFailed = true;
            }
        }
    }

    private static void generate_hmac_sha1_40() throws Exception {
        System.out.println("Generating ");

        Document doc = dbf.newDocumentBuilder().newDocument();
        try {
        XMLSignature sig = new XMLSignature
            (doc, null, XMLSignature.ALGO_ID_MAC_HMAC_SHA1, 40,
             Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS);
            sig.sign(getSecretKey("secret".getBytes("ASCII")));
            System.out.println("FAILED");
            atLeastOneFailed = true;
        } catch (XMLSignatureException xse) {
            System.out.println(xse.getMessage());
            System.out.println("PASSED");
        }
    }

    private static SecretKey getSecretKey(final byte[] secret) {
        return new SecretKey() {
            public String getFormat()   { return "RAW"; }
            public byte[] getEncoded()  { return secret; }
            public String getAlgorithm(){ return "SECRET"; }
        };
    }
}
