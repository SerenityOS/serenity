/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4162868 8130181 8242151 8267397
 * @modules java.base/sun.security.x509
 * @modules java.base/sun.security.util
 * @run main/othervm ExtensibleAlgorithmId
 * @summary Check that AlgorithmId Name-to-OID mapping is extensible and
 *      up-to-date.
 */

import java.security.*;
import sun.security.x509.AlgorithmId;

public class ExtensibleAlgorithmId {

    private static void test(String alg, String expOid) throws Exception {
        System.out.println("Testing " + alg + " and " + expOid );
        try {
            AlgorithmId algid = AlgorithmId.get(alg);
            if (expOid == null) {
                throw new Exception("Expected NSAE not thrown");
            }
            if (!expOid.equals(algid.getOID().toString())) {
                throw new Exception("Oid mismatch, expected " + expOid +
                        ", got " + algid.getOID().toString());
            }
            if (!alg.equals(algid.getName())) {
                throw new Exception("Name mismatch, expected " + alg +
                        ", got " + algid.getName());
            }
            // try AlgorithmId.get() using 'expOid' if (alg != expOid)
            if (alg != expOid) {
                algid = AlgorithmId.get(expOid);
                if (!expOid.equals(algid.getOID().toString())) {
                    throw new Exception("Oid2 mismatch, expected " + expOid +
                        ", got " + algid.getOID().toString());
                }
                if (!alg.equals(algid.getName())) {
                    throw new Exception("Name2 mismatch, expected " + alg +
                            ", got " + algid.getName());
                }
            }
            System.out.println(" => passed");
        } catch (NoSuchAlgorithmException nsae) {
            if (expOid != null) {
                nsae.printStackTrace();
                throw new Exception("Unexpected NSAE for " + alg);
            }
            System.out.println(" => expected NSAE thrown");
        }
    }

    public static void main(String[] args) throws Exception {

        TestProvider p = new TestProvider();
        String alias = "Alg.Alias.Signature.OID." + TestProvider.ALG_OID;
        String stdAlgName = p.getProperty(alias);
        if (stdAlgName == null ||
                !stdAlgName.equalsIgnoreCase(TestProvider.ALG_NAME)) {
            throw new Exception("Wrong OID");
        }

        // scenario#1: test before adding TestProvider
        System.out.println("Before adding test provider");
        test(TestProvider.ALG_NAME, null);
        test(TestProvider.ALG_OID, TestProvider.ALG_OID);
        test(TestProvider.ALG_OID2, TestProvider.ALG_OID2);

        Security.addProvider(p);
        // scenario#2: test again after adding TestProvider
        System.out.println("After adding test provider");
        test(TestProvider.ALG_NAME, TestProvider.ALG_OID);
        test(TestProvider.ALG_OID2, TestProvider.ALG_OID2);

        Security.removeProvider(p.getName());
        // scenario#3: test after removing TestProvider; should be same as
        // scenario#1
        System.out.println("After removing test provider");
        test(TestProvider.ALG_NAME, null);
        test(TestProvider.ALG_OID, TestProvider.ALG_OID);
        test(TestProvider.ALG_OID2, TestProvider.ALG_OID2);
    }

    static class TestProvider extends Provider {

        static String ALG_OID = "1.2.3.4.5.6.7.8.9.0";
        static String ALG_OID2 = "0.2.7.6.5.4.3.2.1.0";
        static String ALG_NAME = "XYZ";

        public TestProvider() {
            super("Dummy", "1.0", "XYZ algorithm");

            put("Signature." + ALG_NAME, "test.xyz");
            // preferred OID for name<->oid mapping
            put("Alg.Alias.Signature.OID." + ALG_OID, ALG_NAME);
            put("Alg.Alias.Signature." + ALG_OID2, ALG_NAME);
        }
    }
}
