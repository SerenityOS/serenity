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
 * @summary Read in a file containing a DER encoded PKCS10 certificate request,
 * flanked with "begin" and "end" lines.
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.pkcs10
 *          java.base/sun.security.util
 * @compile -XDignore.symbol.file PKCS10AttributeReader.java
 * @run main PKCS10AttributeReader
 */
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Date;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs10.PKCS10Attribute;
import sun.security.pkcs10.PKCS10Attributes;
import sun.security.util.DerInputStream;
import sun.security.util.ObjectIdentifier;

/*
 Tests only reads DER encoding files, contents of corresponding asn.1 files
 are copied below for reference.

 # An attribute set for testing with PKCS10.

 {A0  # implicit tag
    {SEQ  # Content Type
        {OID 1.2.840.113549.1.9.3}
        {SET
            {OID "1234"}
        }
    }
     {SEQ  # Challenge Password
         {OID 1.2.840.113549.1.9.7}
         {SET
             {T61String "GuessWhoAmI"}
         }
     }
     {SEQ  # Signing Time
        {OID 1.2.840.113549.1.9.5}
        {SET
            {UTCTime "970422145010Z"}
        }
     }
 }
 */
public class PKCS10AttributeReader {
    // DER encoded files are binary files, to avoid attaching binary files,
    // DER files were encoded in base64
    static final String ATTRIBS = "oE8wEwYJKoZIhvcNAQkDMQYGBDEyMzQwGgYJKoZIhv"
            + "cNAQkHMQ0UC0d1ZXNzV2hv\nQW1JMBwGCSqGSIb3DQEJBTEPFw05NzA0MjIxND"
            + "UwMTBa";

    public static void main(String[] args) throws Exception {

        // Decode base64 encoded DER file
        byte[] pkcs10Bytes = Base64.getMimeDecoder().decode(ATTRIBS.getBytes());

        HashMap<ObjectIdentifier, Object> RequestStander = new HashMap() {
            {
                put(PKCS9Attribute.CHALLENGE_PASSWORD_OID, "GuessWhoAmI");
                put(PKCS9Attribute.SIGNING_TIME_OID, new Date(861720610000L));
                put(PKCS9Attribute.CONTENT_TYPE_OID,
                        ObjectIdentifier.of("1.9.50.51.52"));
            }
        };

        int invalidNum = 0;
        PKCS10Attributes resp = new PKCS10Attributes(
                new DerInputStream(pkcs10Bytes));
        Enumeration eReq = resp.getElements();
        int numOfAttrs = 0;
        while (eReq.hasMoreElements()) {
            numOfAttrs++;
            PKCS10Attribute attr = (PKCS10Attribute) eReq.nextElement();
            if (RequestStander.containsKey(attr.getAttributeId())) {
                if (RequestStander.get(attr.getAttributeId())
                        .equals(attr.getAttributeValue())) {
                    System.out.println(attr.getAttributeId() + " "
                            + attr.getAttributeValue());
                } else {
                    invalidNum++;
                    System.out.println("< " + attr.getAttributeId() + " "
                            + attr.getAttributeValue());
                    System.out.println("< " + attr.getAttributeId() + " "
                            + RequestStander.get(attr.getAttributeId()));
                }
            } else {
                invalidNum++;
                System.out.println("No" + attr.getAttributeId()
                        + "in Certificate Request list");
            }
        }
        if (numOfAttrs != RequestStander.size()) {
            invalidNum++;
            System.out.println("Incorrect number of attributes.");
        }
        System.out.println();
        if (invalidNum > 0) {
            throw new RuntimeException(
                    "Attributes Compared with Stander :" + " Failed");
        }
        System.out.println("Attributes Compared with Stander: Pass");
    }

}
