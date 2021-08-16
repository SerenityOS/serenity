/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @author Weijun Wang
 * @bug 6418422 6418425 6418433 8242151
 * @summary ObjectIdentifier should reject 1.2.3.-4 and throw IOException on all format errors
 * @modules java.base/sun.security.util
 *          java.security.jgss
 */

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import javax.crypto.EncryptedPrivateKeyInfo;
import sun.security.util.*;
import java.util.Arrays;

public class OidFormat {
    public static void main(String[] args) throws Exception {
        String[] badOids = {
            // number problems
            "0", "1", "2",
            "3.1.1", "3", "4",
            "1.40", "1.111.1",
            "-1.2", "0,-2", "1.-2", "2.-2",
            "1.2.-3.4", "1.2.3.-4",
            // format problems
            "aa", "aa.aa",
            "", "....", "1.2..4", "1.2.3.", "1.", "1.2.", "0.1.",
            "1,2",
        };

        for (String s: badOids) {
            testBad(s);
        }

        String[] goodOids = {
            "0.0", "0.1", "1.0", "1.2",
            "0.39", "1.39", "2.47", "2.40.3.6", "2.100.3", "2.123456.3",
            "1.2.3", "1.2.3445",
            "1.3.6.1.4.1.42.2.17",
            // 4811968: ASN.1 cannot handle huge OID components
            "2.16.764.1.3101555394.1.0.100.2.1",
            "2.2726957624935694386592435",  // as huge as possible
            "1.2.777777777777777777",
            "1.2.888888888888888888.111111111111111.2222222222222.33333333333333333.44444444444444",
            "1.2." +
                "1111111111111111111111111111111111111111111111111111111111111." +
                "2222222222222222222222222222222222222222222222222222222222222222." +
                "333333333333333333333333333333333333333333333333333333333333333." +
                "4444444444444444444444444444444444444444444444444444444." +
                "55555555555555555555555555555555555555555555555555555555555555555555555." +
                "666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666." +
                "77777777777777777777777777777777777777777777777777777777777777777777777777." +
                "8888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888." +
                "9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999",
            "1.2.2147483647.4",
            "1.2.268435456.4",
        };

        for (String s: goodOids) {
            testGood(s);
        }
    }

    static void testGood(String s) throws Exception {
        System.err.println("Trying " + s);
        ObjectIdentifier oid = ObjectIdentifier.of(s);
        if (!oid.toString().equals(s)) {
            throw new Exception("equal test fail");
        }
        DerOutputStream os = new DerOutputStream();
        os.putOID(oid);
        DerInputStream is = new DerInputStream(os.toByteArray());
        ObjectIdentifier oid2 = is.getOID();
        if (!oid.equals(oid2)) {
            throw new Exception("Test DER I/O fails: " + oid + " and " + oid2);
        }
    }

    static void testBad(String s) throws Exception {
        System.err.println("Trying " + s);
        try {
            ObjectIdentifier.of(s);
            throw new Exception("should be invalid ObjectIdentifier");
        } catch (IOException ioe) {
            System.err.println(ioe);
        }

        try {
            new Oid(s);
            throw new Exception("should be invalid Oid");
        } catch (GSSException gsse) {
            ;
        }

        try {
            new EncryptedPrivateKeyInfo(s, new byte[8]);
            throw new Exception("should be invalid algorithm");
        } catch (NoSuchAlgorithmException e) {
            ;
        }
    }
}
