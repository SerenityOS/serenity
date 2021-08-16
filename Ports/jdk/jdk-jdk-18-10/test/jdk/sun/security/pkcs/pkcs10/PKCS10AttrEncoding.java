/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8048357 8242151
 * @summary test DER encoding of PKCS10 attributes
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.pkcs10
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @compile -XDignore.symbol.file PKCS10AttrEncoding.java
 * @run main PKCS10AttrEncoding
 */
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attribute;
import sun.security.pkcs10.PKCS10Attributes;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.X500Name;
import sun.security.x509.X509Key;

public class PKCS10AttrEncoding {

    static final ObjectIdentifier[] ids = {
        PKCS9Attribute.CONTENT_TYPE_OID, // ContentType
        PKCS9Attribute.SIGNING_TIME_OID, // SigningTime
        PKCS9Attribute.CHALLENGE_PASSWORD_OID // ChallengePassword
    };
    static int failedCount = 0;
    static HashMap<ObjectIdentifier, Object> constructedMap = new HashMap<>();

    public static void main(String[] args) throws Exception {

        // initializations
        int len = ids.length;
        Object[] values = {
            ObjectIdentifier.of("1.2.3.4"),
            new GregorianCalendar(1970, 1, 25, 8, 56, 7).getTime(),
            "challenging"
        };
        for (int j = 0; j < len; j++) {
            constructedMap.put(ids[j], values[j]);
        }

        X500Name subject = new X500Name("cn=Test");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        String sigAlg = "DSA";

        keyGen.initialize(512);

        KeyPair pair = keyGen.generateKeyPair();
        X509Key publicKey = (X509Key) pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        // Create the PKCS10 request
        PKCS10Attribute[] attrs = new PKCS10Attribute[len];
        for (int j = 0; j < len; j++) {
            attrs[j] = new PKCS10Attribute(ids[j], values[j]);
        }
        PKCS10 req = new PKCS10(publicKey, new PKCS10Attributes(attrs));
        System.out.println("List of attributes in constructed PKCS10 "
                + "request: ");
        checkAttributes(req.getAttributes().getElements());

        // Encode the PKCS10 request and generate another PKCS10 request from
        // the encoded byte array
        req.encodeAndSign(subject, privateKey, sigAlg);
        PKCS10 resp = new PKCS10(req.getEncoded());
        System.out.println("List of attributes in DER encoded PKCS10 Request:");
        checkAttributes(resp.getAttributes().getElements());

        if (failedCount > 0) {
            throw new RuntimeException("Attributes Compared : Failed");
        }
        System.out.println("Attributes Compared : Pass");
    }

    static void checkAttributes(Enumeration attrs) {
        int numOfAttrs = 0;
        while (attrs.hasMoreElements()) {
            numOfAttrs ++;
            PKCS10Attribute attr = (PKCS10Attribute) attrs.nextElement();

            if (constructedMap.containsKey(attr.getAttributeId())) {
                if (constructedMap.get(attr.getAttributeId()).
                        equals(attr.getAttributeValue())) {
                    System.out.print("AttributeId: " + attr.getAttributeId());
                    System.out.println(" AttributeValue: "
                            + attr.getAttributeValue());
                } else {
                    failedCount++;
                    System.out.print("< AttributeId: " + attr.getAttributeId());
                    System.out.println("  AttributeValue: " + constructedMap.
                            get(attr.getAttributeId()));
                    System.out.print("< AttributeId: " + attr.getAttributeId());
                    System.out.println("  AttributeValue: "
                            + attr.getAttributeValue());
                }
            } else {
                failedCount++;
                System.out.println("No " + attr.getAttributeId()
                        + " in DER encoded PKCS10 Request");
            }
        }
        if(numOfAttrs != constructedMap.size()){
            failedCount++;
            System.out.println("Incorrect number of attributes.");

        }
        System.out.println();
    }

}
