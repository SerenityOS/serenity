/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.io.File;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.io.IOException;
import java.security.cert.CertificateException;
import static java.lang.System.out;

/**
 * @test
 * @bug 8048830
 * @summary Entry's attribute set should be empty
 * @library ../
 * @library /test/lib
 * @run main MetadataEmptyTest
 */
public class MetadataEmptyTest {
    private static final char[] PASSWORD = "passwd".toCharArray();
    private static final String ALIAS = "testkey";
    private static final String KEYSTORE_PATH = System.getProperty(
            "test.classes" + File.separator + "ks.pkcs12",
            "." + File.separator + "ks.pkcs12");

    private void runTest() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException {
        KeyStore ks = Utils.loadKeyStore(KEYSTORE_PATH,
                Utils.KeyStoreType.pkcs12, PASSWORD);
        Key key = ks.getKey(ALIAS, PASSWORD);
        Certificate cert = ks
                .getCertificate(ALIAS);
        KeyStore.Entry entry = new KeyStore.PrivateKeyEntry(
                (PrivateKey) key,
                new Certificate[]{cert});
        if (!entry.getAttributes().isEmpty()) {
            throw new RuntimeException("Entry's attributes set "
                    + "must be empty");
        }
        out.println("Test Passed");
    }

    public static void main(String[] args) throws Exception{
        MetadataEmptyTest test = new MetadataEmptyTest();
        test.setUp();
        test.runTest();
    }

    private void setUp() {
        Utils.createKeyStore(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH, ALIAS);
    }
}
