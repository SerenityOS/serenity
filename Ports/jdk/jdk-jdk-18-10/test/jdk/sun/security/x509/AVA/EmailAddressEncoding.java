/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4702543
 * @summary  incorrect ASN1 DER encoding of EmailAddress Attribute
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.util
 */

import java.io.*;
import javax.security.auth.x500.*;
import sun.security.util.*;
import sun.security.pkcs.*;

public class EmailAddressEncoding {

    public static void main(String[] args) throws Exception {
        X500Principal p = new X500Principal
                ("c=us, emailaddress=foo@bar, cn=foobar");
        byte[] encoded = p.getEncoded();

        // name is a sequence of RDN's
        DerInputStream dis = new DerInputStream(encoded);
        DerValue[] nameseq = dis.getSequence(3);

        boolean passed = false;
        for (int i = 0; i < nameseq.length; i++) {

            // each RDN is a set of AttributeTypeAndValue
            DerInputStream is = new DerInputStream(nameseq[i].toByteArray());
            DerValue[] ava = is.getSet(3);

            for (int j = 0; j < ava.length; j++) {

                ObjectIdentifier oid = ava[j].data.getOID();

                if (oid.equals(PKCS9Attribute.EMAIL_ADDRESS_OID)) {
                    DerValue value = ava[j].data.getDerValue();
                    if (value.getTag() == DerValue.tag_IA5String) {
                        passed = true;
                        break;
                    } else {
                        throw new SecurityException
                                ("Test failed, expected EMAIL_ADDRESS tag '" +
                                DerValue.tag_IA5String +
                                "', got '" +
                                value.getTag() + "'");
                    }
                }
            }

            if (passed) {
                break;
            }
        }

        if (passed) {
            System.out.println("Test passed");
        } else {
            throw new SecurityException("Test failed");
        }
    }
}
