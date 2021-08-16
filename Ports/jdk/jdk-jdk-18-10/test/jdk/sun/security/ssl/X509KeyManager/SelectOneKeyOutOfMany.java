/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4387949 4302197
 * @summary Need to add Sockets and key arrays to the
 *      X509KeyManager.choose*Alias() methods & There's no mechanism
 *      to select one key out of many in a keystore
 *
 *      chooseServerAlias method is reverted back to accept a single
 *      keytype as a parameter, please see RFE: 4501014
 *      The part of the test on the server-side is changed to test
 *      passing in a single keytype parameter to chooseServerAlias method.
 *
 * @author Brad Wetmore
 */

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;

public class SelectOneKeyOutOfMany {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String passwd = "passphrase";

    public static void main(String[] args) throws Exception {
        KeyStore ks;
        KeyManagerFactory kmf;
        X509KeyManager km;

        char[] passphrase = passwd.toCharArray();

        String keyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + keyStoreFile;
        /*
         * Setup the tests.
         */
        kmf = KeyManagerFactory.getInstance("SunX509");
        ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyFilename), passphrase);
        kmf.init(ks, passphrase);
        km = (X509KeyManager) kmf.getKeyManagers()[0];

        /*
         * There should be one of each key type here.
         */
        String [] nothing = new String [] { "nothing" };
        String [] rsa = new String [] { "RSA" };
        String [] dsa = new String [] { "DSA" };
        String [] rsaDsa = new String [] { "RSA", "DSA" };
        String [] dsaRsa = new String [] { "DSA", "RSA" };

        String resultsRsaDsa, resultsDsaRsa;
        String resultsRsa, resultsDsa;
        String resultsNone;

        String [] resultArrayRSA;
        String [] resultArrayDSA;

        /*
         * Check get*Aliases for null returns
         */
        if (km.getClientAliases("nothing", null) != null)
            throw new Exception("km.getClientAliases(nothing) != null");
        System.out.println("km.getClientAlias(nothing) returning nulls");

        if (km.getServerAliases("nothing", null) != null)
            throw new Exception("km.getServerAliases(nothing) != null");
        System.out.println("km.getServerAlias(nothing) returning nulls");
        System.out.println("=====");

        System.out.println("Dumping Certs...");
        if ((resultArrayRSA = km.getServerAliases("RSA", null)) == null)
            throw new Exception("km.getServerAliases(RSA) == null");
        for (int i = 0; i < resultArrayRSA.length; i++) {
            System.out.println("        resultArrayRSA#" + i + ": " +
                resultArrayRSA[i]);
        }

        if ((resultArrayDSA = km.getServerAliases("DSA", null)) == null)
            throw new Exception("km.getServerAliases(DSA) == null");
        for (int i = 0; i < resultArrayDSA.length; i++) {
            System.out.println("        resultArrayDSA#" + i + ": " +
                resultArrayDSA[i]);
        }
        System.out.println("=====");

        /*
         * Check chooseClientAliases for null returns
         */
        resultsNone = km.chooseClientAlias(nothing, null, null);
        if (resultsNone != null) {
            throw new Exception("km.chooseClientAlias(nothing) != null");
        }
        System.out.println("km.ChooseClientAlias(nothing) passed");

        /*
         * Check chooseClientAlias for RSA keys.
         */
        resultsRsa = km.chooseClientAlias(rsa, null, null);
        if (resultsRsa == null)  {
            throw new Exception(
                "km.chooseClientAlias(rsa, null, null) != null");
        }
        System.out.println("km.chooseClientAlias(rsa) passed");

        /*
         * Check chooseClientAlias for DSA keys.
         */
        resultsDsa = km.chooseClientAlias(dsa, null, null);
        if (resultsDsa == null) {
            throw new Exception(
                "km.chooseClientAlias(dsa, null, null) != null");
        }
        System.out.println("km.chooseClientAlias(dsa) passed");

        /*
         * There should be both an rsa and a dsa entry in the
         * keystore.
         *
         * Check chooseClientAlias for RSA/DSA keys and be sure
         * the ordering is correct.
         */
        resultsRsaDsa = km.chooseClientAlias(rsaDsa, null, null);
        if ((resultsRsaDsa == null) || (resultsRsaDsa != resultsRsa)) {
            throw new Exception("km.chooseClientAlias(rsaDsa) failed");
        }
        System.out.println("km.chooseClientAlias(rsaDsa) passed");

        resultsDsaRsa = km.chooseClientAlias(dsaRsa, null, null);
        if ((resultsDsaRsa == null) || (resultsDsaRsa != resultsDsa)) {
            throw new Exception("km.chooseClientAlias(DsaRsa) failed");
        }
        System.out.println("km.chooseClientAlias(DsaRsa) passed");

        System.out.println("=====");

        /*
         * Check chooseServerAliases for null returns
         */
        resultsNone = km.chooseServerAlias("nothing", null, null);
        if (resultsNone != null) {
            throw new Exception("km.chooseServerAlias(\"nothing\") != null");
        }
        System.out.println("km.ChooseServerAlias(\"nothing\") passed");

        /*
         * Check chooseServerAlias for RSA keys.
         */
        resultsRsa = km.chooseServerAlias("RSA", null, null);
        if (resultsRsa == null)  {
            throw new Exception(
                "km.chooseServerAlias(\"RSA\", null, null) != null");
        }
        System.out.println("km.chooseServerAlias(\"RSA\") passed");

        /*
         * Check chooseServerAlias for DSA keys.
         */
        resultsDsa = km.chooseServerAlias("DSA", null, null);
        if (resultsDsa == null) {
            throw new Exception(
                "km.chooseServerAlias(\"DSA\", null, null) != null");
        }
        System.out.println("km.chooseServerAlias(\"DSA\") passed");

    }
}
