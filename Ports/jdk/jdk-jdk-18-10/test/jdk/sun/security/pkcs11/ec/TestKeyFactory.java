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
 * @bug 6405536
 * @summary Test the P11ECKeyFactory
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestKeyFactory
 * @run main/othervm -Djava.security.manager=allow TestKeyFactory sm
 */

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class TestKeyFactory extends PKCS11Test {

    /**
     * Test that key1 (reference key) and key2 (key to be tested) are
     * equivalent
     */
    private static void testKey(Key key1, Key key2) throws Exception {
        if (key2.getAlgorithm().equals("EC") == false) {
            throw new Exception("Algorithm not EC");
        }
        if (key1 instanceof PublicKey) {
            if (key2.getFormat().equals("X.509") == false) {
                throw new Exception("Format not X.509");
            }
        } else if (key1 instanceof PrivateKey) {
            if (key2.getFormat().equals("PKCS#8") == false) {
                throw new Exception("Format not PKCS#8");
            }
        }
        if (key1.equals(key2) == false) {
            System.out.println("key1: " + key1);
            System.out.println("key2: " + key2);
            System.out.println("enc1: " + toString(key1.getEncoded()));
            System.out.println("enc2: " + toString(key2.getEncoded()));
            throw new Exception("Keys not equal");
        }
        if (Arrays.equals(key1.getEncoded(), key2.getEncoded()) == false) {
            throw new Exception("Encodings not equal");
        }
    }

    private static void testPublic(KeyFactory kf, PublicKey key) throws Exception {
        System.out.println("Testing public key...");
        PublicKey key2 = (PublicKey)kf.translateKey(key);
        KeySpec keySpec = kf.getKeySpec(key, ECPublicKeySpec.class);
        PublicKey key3 = kf.generatePublic(keySpec);
        KeySpec x509Spec = kf.getKeySpec(key, X509EncodedKeySpec.class);
        PublicKey key4 = kf.generatePublic(x509Spec);
        KeySpec x509Spec2 = new X509EncodedKeySpec(key.getEncoded());
        PublicKey key5 = kf.generatePublic(x509Spec2);
        testKey(key, key);
        testKey(key, key2);
        testKey(key, key3);
        testKey(key, key4);
        testKey(key, key5);
    }

    private static void testPrivate(KeyFactory kf, PrivateKey key) throws Exception {
        System.out.println("Testing private key...");
        PrivateKey key2 = (PrivateKey)kf.translateKey(key);
        KeySpec keySpec = kf.getKeySpec(key, ECPrivateKeySpec.class);
        PrivateKey key3 = kf.generatePrivate(keySpec);
        KeySpec pkcs8Spec = kf.getKeySpec(key, PKCS8EncodedKeySpec.class);
        PrivateKey key4 = kf.generatePrivate(pkcs8Spec);
        KeySpec pkcs8Spec2 = new PKCS8EncodedKeySpec(key.getEncoded());
        PrivateKey key5 = kf.generatePrivate(pkcs8Spec2);
        testKey(key, key);
        testKey(key, key2);
        testKey(key, key3);
        testKey(key, key4);
        testKey(key, key5);
    }

    private static void test(KeyFactory kf, Key key) throws Exception {
        if (key.getAlgorithm().equals("EC") == false) {
            throw new Exception("Not an EC key");
        }
        if (key instanceof PublicKey) {
            testPublic(kf, (PublicKey)key);
        } else if (key instanceof PrivateKey) {
            testPrivate(kf, (PrivateKey)key);
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestKeyFactory(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        if (p.getService("KeyFactory", "EC") == null) {
            System.out.println("Provider does not support EC, skipping");
            return;
        }
        int[] keyLengths = {256, 521};
        KeyFactory kf = KeyFactory.getInstance("EC", p);
        for (int len : keyLengths) {
            System.out.println("Length " + len);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", p);
            kpg.initialize(len);
            KeyPair kp = kpg.generateKeyPair();
            test(kf, kp.getPrivate());
            test(kf, kp.getPublic());
        }
    }
}
