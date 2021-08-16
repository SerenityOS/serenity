/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/*
 * @test
 * @bug 8048621
 * @summary Test the basic operations of KeyStore entry, provided by SunJCE
 *  (jceks)
 * @author Yu-Ching Valerie PENG
 */

public class TestKeyStoreEntry {
    private static final char[] PASSWDK = new char[] {
            't', 'e', 'r', 'c', 'e', 's'
    };
    private static final char[] PASSWDF = new String("guardian Angel")
            .toCharArray();
    private static final String[] KS_ALGOS = {
            "DES", "DESede", "Blowfish"
    };
    private static final int NUM_ALGOS = KS_ALGOS.length;

    private final SecretKey[] sks = new SecretKey[NUM_ALGOS];

    TestKeyStoreEntry() throws Exception {
        // generate secret keys which are to be stored in the jce
        // key store object
        KeyGenerator[] kgs = new KeyGenerator[NUM_ALGOS];
        for (int i = 0; i < NUM_ALGOS; i++) {
            kgs[i] = KeyGenerator.getInstance(KS_ALGOS[i], "SunJCE");
            sks[i] = kgs[i].generateKey();
        }

    }

    public static void main(String args[]) throws Exception {
        TestKeyStoreEntry jstest = new TestKeyStoreEntry();
        jstest.run();
    }

    public void run() throws Exception {
        try (FileOutputStream fos = new FileOutputStream("jceks");
                FileInputStream fis = new FileInputStream("jceks");) {

            KeyStore ks = KeyStore.getInstance("jceks");
            // create an empty key store
            ks.load(null, null);

            // store the secret keys
            String aliasHead = new String("secretKey");
            for (int j = 0; j < NUM_ALGOS; j++) {
                ks.setKeyEntry(aliasHead + j, sks[j], PASSWDK, null);
            }

            // write the key store out to a file
            ks.store(fos, PASSWDF);
            // wipe clean the existing key store
            for (int k = 0; k < NUM_ALGOS; k++) {
                ks.deleteEntry(aliasHead + k);
            }
            if (ks.size() != 0) {
                throw new RuntimeException("ERROR: re-initialization failed");
            }

            // reload the key store with the file
            ks.load(fis, PASSWDF);

            // check the integrity/validaty of the key store
            Key temp = null;
            String alias = null;
            if (ks.size() != NUM_ALGOS) {
                throw new RuntimeException("ERROR: wrong number of key"
                        + " entries");
            }

            for (int m = 0; m < ks.size(); m++) {
                alias = aliasHead + m;
                temp = ks.getKey(alias, PASSWDK);
                // compare the keys
                if (!temp.equals(sks[m])) {
                    throw new RuntimeException("ERROR: key comparison (" + m
                            + ") failed");
                }
                // check the type of key
                if (ks.isCertificateEntry(alias) || !ks.isKeyEntry(alias)) {
                    throw new RuntimeException("ERROR: type identification ("
                            + m + ") failed");
                }
            }
        }
    }

}
