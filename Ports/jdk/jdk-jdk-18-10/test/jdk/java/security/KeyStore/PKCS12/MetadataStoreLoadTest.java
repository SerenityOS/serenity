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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PKCS12Attribute;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Set;
import static java.lang.System.out;
import java.util.HashSet;

/**
 * @test
 * @bug 8048830
 * @summary Test store metadata attributes to PKCS12 keystore.
 * @library ../
 * @library /test/lib
 * @run main MetadataStoreLoadTest
 */
public class MetadataStoreLoadTest {
    private static final char[] PASSWORD = "passwd".toCharArray();
    private static final char[] KEY_PASSWORD = "keypasswd".toCharArray();
    private static final String ALIAS = "testkey_metadata";
    private static final String KEYSTORE = "ks.pkcs12";
    private static final String KESTORE_NEW = "ks-attr.pkcs12";
    private static final int MAX_HUGE_SIZE = 2000000;
    private static final String WORKING_DIRECTORY = System.getProperty(
            "test.classes", "." + File.separator);
    private static final String KEYSTORE_PATH = WORKING_DIRECTORY
            + File.separator + KEYSTORE;
    private static KeyStore.Entry.Attribute[] ATTR_SET;

    private void runTest() throws GeneralSecurityException,
            UnrecoverableEntryException, NoSuchAlgorithmException,
            KeyStoreException, IOException {
        storeAttrs();
        checkAttrs();
    }

    private void storeAttrs() throws UnrecoverableEntryException,
            GeneralSecurityException, NoSuchAlgorithmException,
            KeyStoreException, IOException {
        KeyStore ksIn = Utils.loadKeyStore(KEYSTORE_PATH,
                Utils.KeyStoreType.pkcs12, PASSWORD);
        KeyStore ksAttr = KeyStore
                .getInstance(Utils.KeyStoreType.pkcs12.name());
        ksAttr.load(null);
        Key key = ksIn.getKey(ALIAS, PASSWORD);
        Certificate cert = ksIn.getCertificate(ALIAS);
        Set<KeyStore.Entry.Attribute> attrs =
                new HashSet<>(Arrays.asList(ATTR_SET));
        KeyStore.Entry e = new KeyStore.PrivateKeyEntry((PrivateKey) key,
                new Certificate[]{cert}, attrs);
        ksAttr.setEntry(ALIAS, e, new KeyStore.PasswordProtection(
                KEY_PASSWORD));

        out.println("Attributes before store:");
        e.getAttributes().stream().forEach((attr) -> {
            out.println(attr.getName() + ", '" + attr.getValue() + "'");
        });
        Utils.saveKeyStore(ksAttr, WORKING_DIRECTORY + File.separator
                + KESTORE_NEW, PASSWORD);
    }

    private void checkAttrs() throws UnrecoverableEntryException,
            GeneralSecurityException, NoSuchAlgorithmException,
            KeyStoreException, IOException {
        KeyStore ks = Utils.loadKeyStore(WORKING_DIRECTORY
                + File.separator
                + KESTORE_NEW, Utils.KeyStoreType.pkcs12, PASSWORD);
        KeyStore.Entry keyStoreEntry = ks.getEntry(ALIAS,
                new KeyStore.PasswordProtection(KEY_PASSWORD));
        out.println("Attributes after store:");
        //print attribute values
        keyStoreEntry.getAttributes().stream().forEach((attr) -> {
            out.println(attr.getName() + ", '" + attr.getValue() + "'");
        });
        Arrays.stream(ATTR_SET).forEach((attr) -> {
            if (!keyStoreEntry.getAttributes().contains(attr)) {
                throw new RuntimeException("Entry doesn't contain attribute: ("
                        + attr.getName() + ", '" + attr.getValue() + "')");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        MetadataStoreLoadTest test = new MetadataStoreLoadTest();
        test.setUp();
        test.runTest();
        out.println("Test Passed");
    }

    private void setUp() {
        Utils.createKeyStore(Utils.KeyStoreType.pkcs12, KEYSTORE_PATH, ALIAS);
        final String allCharsString = "`1234567890-=qwertyuiop[]asdfghjkl;'\\zx"
                + "cvbnm,./!@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:|>ZXCVBNM<>?\"";
        StringBuilder sbPrintable = new StringBuilder();
        while (sbPrintable.length() < MAX_HUGE_SIZE) {
            sbPrintable.append(allCharsString);
        }
        final String hugePrintable = sbPrintable.toString();
        final String binaryString = "00:11:22:33:44:55:66:77:88:99:AA:BB:DD:"
                + "EE:FF:";
        StringBuilder sbBinary = new StringBuilder();
        sbBinary.append(binaryString);
        while (sbBinary.length() < MAX_HUGE_SIZE) {
            sbBinary.append(":").append(binaryString);
        }
        sbBinary.insert(0, "[").append("]");
        final String hugeBinary = sbBinary.toString();
        ATTR_SET = new PKCS12Attribute[5];
        ATTR_SET[0] = new PKCS12Attribute("1.2.840.113549.1.9.1",
                "Test email addres attr <test@oracle.com>");
        ATTR_SET[1] = new PKCS12Attribute("1.2.110.1", "not registered attr");
        ATTR_SET[2] = new PKCS12Attribute("1.2.110.2", hugePrintable);
        ATTR_SET[3] = new PKCS12Attribute("1.2.110.3", hugeBinary);
        ATTR_SET[4] = new PKCS12Attribute("1.2.110.2", " ");
    }
}
