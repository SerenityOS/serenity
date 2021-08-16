/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6414980
 * @summary Test that the PKCS#11 KeyStore handles RSA, DSA, and EC keys
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm AddPrivateKey
 * @run main/othervm -Djava.security.manager=allow AddPrivateKey sm policy
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// this test is currently only run for the NSS KeyStore provider, but it
// is really a generic KeyStore test so it should be modified to run for
// all providers.
public class AddPrivateKey extends SecmodTest {

    private static final String ALIAS1 = "entry1";
    private static final String ALIAS2 = "entry2";
    private static final String ALIAS3 = "entry3";
    private static final int MAX_LINE = 85;
    private static final int DATA_LENGTH = 4096;
    private static final byte[] DATA = generateData(DATA_LENGTH);

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
        }

        if (initSecmod() == false) {
            return;
        }

        String configName = BASE + SEP + "nss.cfg";
        Provider p = getSunPKCS11(configName);

        boolean supportsEC = (p.getService("KeyFactory", "EC") != null);

        System.out.println(p);
        System.out.println();
        Security.addProvider(p);

        if (args.length > 1 && "sm".equals(args[0])) {
            System.setSecurityManager(new SecurityManager());
        }

        KeyStore ks = KeyStore.getInstance(PKCS11, p);
        ks.load(null, password);
        for (String alias : aliases(ks)) {
            System.out.println("Deleting: " + alias);
            ks.deleteEntry(alias);
        }

        KeyStore jks = KeyStore.getInstance("JKS");
        try (InputStream in = new FileInputStream(BASE + SEP + "keystore.jks")) {
            char[] jkspass = "passphrase".toCharArray();
            jks.load(in, jkspass);
            for (String alias : Collections.list(jks.aliases())) {
                if (jks.entryInstanceOf(alias, PrivateKeyEntry.class)) {
                    PrivateKeyEntry entry = (PrivateKeyEntry)jks.getEntry(alias,
                            new PasswordProtection(jkspass));
                    String algorithm = entry.getPrivateKey().getAlgorithm();
                    System.out.printf("-Entry %s (%s)%n", alias, algorithm);
                    if ((supportsEC == false) && algorithm.equals("EC")) {
                        System.out.println("EC not supported by provider, "
                                + "skipping");
                        continue;
                    }
                    if ((supportsEC == false) && algorithm.equals("DSA")) {
                        System.out.println("Provider does not appear to have "
                                + "CKA_NETSCAPE_DB fix, skipping");
                        continue;
                    }
                    test(p, entry);
                } // else ignore
            }
        }
        System.out.println("OK");
    }

    private static List<String> aliases(KeyStore ks) throws KeyStoreException {
        return Collections.list(ks.aliases());
    }

    private static void test(Provider p, PrivateKeyEntry entry) throws Exception {
        PrivateKey key = entry.getPrivateKey();
        X509Certificate[] chain = (X509Certificate[])entry.getCertificateChain();
        PublicKey publicKey = chain[0].getPublicKey();
        System.out.println(toString(key));
        sign(p, key, publicKey);

        KeyStore ks = KeyStore.getInstance("PKCS11", p);
        ks.load(null, null);
        if (ks.size() != 0) {
            throw new Exception("KeyStore not empty");
        }
        List<String> aliases;

        // test 1: add entry
        ks.setKeyEntry(ALIAS1, key, null, chain);
        aliases = aliases(ks);
        if (aliases.size() != 1) {
            throw new Exception("size not 1: " + aliases);
        }
        if (aliases.get(0).equals(ALIAS1) == false) {
            throw new Exception("alias mismatch: " + aliases);
        }

        PrivateKey key2 = (PrivateKey)ks.getKey(ALIAS1, null);
        System.out.println(toString(key2));
        X509Certificate[] chain2 =
                (X509Certificate[]) ks.getCertificateChain(ALIAS1);
        if (Arrays.equals(chain, chain2) == false) {
            throw new Exception("chain mismatch");
        }
        sign(p, key2, publicKey);

        ks.deleteEntry(ALIAS1);
        if (ks.size() != 0) {
            throw new Exception("KeyStore not empty");
        }

        // test 2: translate to session object, then add entry
        KeyFactory kf = KeyFactory.getInstance(key.getAlgorithm(), p);
        PrivateKey key3 = (PrivateKey)kf.translateKey(key);
        System.out.println(toString(key3));
        sign(p, key3, publicKey);

        ks.setKeyEntry(ALIAS2, key3, null, chain);
        aliases = aliases(ks);
        if (aliases.size() != 1) {
            throw new Exception("size not 1");
        }
        if (aliases.get(0).equals(ALIAS2) == false) {
            throw new Exception("alias mismatch: " + aliases);
        }

        PrivateKey key4 = (PrivateKey)ks.getKey(ALIAS2, null);
        System.out.println(toString(key4));
        X509Certificate[] chain4 = (X509Certificate[])
                ks.getCertificateChain(ALIAS2);
        if (Arrays.equals(chain, chain4) == false) {
            throw new Exception("chain mismatch");
        }
        sign(p, key4, publicKey);

        // test 3: change alias
        ks.setKeyEntry(ALIAS3, key3, null, chain);
        aliases = aliases(ks);
        if (aliases.size() != 1) {
            throw new Exception("size not 1");
        }
        if (aliases.get(0).equals(ALIAS3) == false) {
            throw new Exception("alias mismatch: " + aliases);
        }

        PrivateKey key5 = (PrivateKey)ks.getKey(ALIAS3, null);
        System.out.println(toString(key5));
        X509Certificate[] chain5 = (X509Certificate[])
                ks.getCertificateChain(ALIAS3);
        if (Arrays.equals(chain, chain5) == false) {
            throw new Exception("chain mismatch");
        }
        sign(p, key5, publicKey);

        ks.deleteEntry(ALIAS3);
        if (ks.size() != 0) {
            throw new Exception("KeyStore not empty");
        }

        System.out.println("OK");
    }

    private static void sign(Provider p, PrivateKey privateKey,
            PublicKey publicKey) throws Exception {
        String keyAlg = privateKey.getAlgorithm();
        String alg;
        switch (keyAlg) {
            case "RSA":
                alg = "SHA1withRSA";
                break;
            case "DSA":
                alg = "SHA1withDSA";
                break;
            case "EC":
                alg = "SHA1withECDSA";
                break;
            default:
                throw new Exception("Unknown algorithm " + keyAlg);
        }
        Signature s = Signature.getInstance(alg, p);
        s.initSign(privateKey);
        s.update(DATA);
        byte[] sig = s.sign();

        s.initVerify(publicKey);
        s.update(DATA);
        if (s.verify(sig) == false) {
            throw new Exception("Signature did not verify");
        }
    }

    private static String toString(Object o) {
        String s = String.valueOf(o).split("\n")[0];
        return (s.length() <= MAX_LINE) ? s : s.substring(0, MAX_LINE);
    }

}
