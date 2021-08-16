/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4856966
 * @summary basic test of SHA1withDSA and RawDSA signing/verifying
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestDSA
 * @run main/othervm -Djava.security.manager=allow TestDSA sm
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.util.Random;

public class TestDSA extends PKCS11Test {

    // values of the keys we use for the tests

    private final static String ps =
        "fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669" +
        "455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b7" +
        "6b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb" +
        "83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7";

    private final static String qs =
        "9760508f15230bccb292b982a2eb840bf0581cf5";

    private final static String gs =
        "f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d078267" +
        "5159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e1" +
        "3c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243b" +
        "cca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a";

    private final static String xs =
        "2952afd9aef9527f9b40d23c8916f7d046028f9d";

    private final static String ys =
        "b16ddb0f9394c328c983ecf23b20014ace368a1af5728dffbf1162de9ed8ebf6" +
        "384f323930e091503035caa797e3674221fc16136240b5474799ede2b7b11313" +
        "7574a9c26bcf900940027b4bcd511ef1d1daf2e69c416aebaf3bdf39f02473b9" +
        "d963f99414c09d97bb0830d9fbdcf7bb9dad8a2179fcdf296838c4cfab8f4d8f";

    private final static BigInteger p = new BigInteger(ps, 16);
    private final static BigInteger q = new BigInteger(qs, 16);
    private final static BigInteger g = new BigInteger(gs, 16);
    private final static BigInteger x = new BigInteger(xs, 16);
    private final static BigInteger y = new BigInteger(ys, 16);

    // data for test 1, original and SHA-1 hashed
    private final static byte[] data1Raw = b("0102030405060708090a0b0c0d0e0f10111213");
    private final static byte[] data1SHA = b("00:e2:5f:c9:1c:8f:d6:8c:6a:dc:c6:bd:f0:46:60:5e:a2:cd:8d:ad");

    // valid signatures of data1. sig1b uses incorrect ASN.1 encoding,
    // which we want to accept anyway for compatibility
    private final static byte[] sig1a = b("30:2d:02:14:53:06:3f:7d:ec:48:3c:99:17:9a:2c:a9:4d:e8:00:da:70:fb:35:d7:02:15:00:92:6a:39:6b:15:63:2f:e7:32:90:35:bf:af:47:55:e7:ff:33:a5:13");
    private final static byte[] sig1b = b("30:2c:02:14:53:06:3f:7d:ec:48:3c:99:17:9a:2c:a9:4d:e8:00:da:70:fb:35:d7:02:14:92:6a:39:6b:15:63:2f:e7:32:90:35:bf:af:47:55:e7:ff:33:a5:13");

    // data for test 2 (invalid signatures)
    private final static byte[] data2Raw = {};
    private final static byte[] data2SHA = b("da:39:a3:ee:5e:6b:4b:0d:32:55:bf:ef:95:60:18:90:af:d8:07:09");

    private static void verify(Provider provider, String alg, PublicKey key, byte[] data, byte[] sig, boolean result) throws Exception {
        Signature s = Signature.getInstance(alg, provider);
        s.initVerify(key);
        boolean r;
        s.update(data);
        r = s.verify(sig);
        if (r != result) {
            throw new Exception("Result mismatch, actual: " + r);
        }
        s.update(data);
        r = s.verify(sig);
        if (r != result) {
            throw new Exception("Result mismatch, actual: " + r);
        }
        System.out.println("Passed");
    }

    public static void main(String[] args) throws Exception {
        main(new TestDSA(), args);
    }

    @Override
    public void main(Provider provider) throws Exception {
        long start = System.currentTimeMillis();

        System.out.println("Testing provider " + provider + "...");

        if (provider.getService("Signature", "SHA1withDSA") == null) {
            System.out.println("DSA not supported, skipping");
            return;
        }

        KeyFactory kf = KeyFactory.getInstance("DSA", provider);
        DSAPrivateKeySpec privSpec = new DSAPrivateKeySpec(x, p, q, g);
        DSAPublicKeySpec pubSpec = new DSAPublicKeySpec(y, p, q, g);
        PrivateKey privateKey = kf.generatePrivate(privSpec);
        PublicKey publicKey = kf.generatePublic(pubSpec);

        // verify known-good and known-bad signatures using SHA1withDSA and RawDSA
        verify(provider, "SHA1withDSA", publicKey, data1Raw, sig1a, true);
        verify(provider, "SHA1withDSA", publicKey, data1Raw, sig1b, true);
        verify(provider, "SHA1withDSA", publicKey, data2Raw, sig1a, false);
        verify(provider, "SHA1withDSA", publicKey, data2Raw, sig1b, false);

        verify(provider, "RawDSA", publicKey, data1SHA, sig1a, true);
        verify(provider, "RawDSA", publicKey, data1SHA, sig1b, true);
        verify(provider, "RawDSA", publicKey, data2SHA, sig1a, false);
        verify(provider, "RawDSA", publicKey, data2SHA, sig1b, false);

        testSigning(provider, privateKey, publicKey);

        long stop = System.currentTimeMillis();
        System.out.println("All tests passed (" + (stop - start) + " ms).");
    }

    private void testSigning(Provider provider, PrivateKey privateKey,
            PublicKey publicKey) throws Exception {
        byte[] data = new byte[2048];
        new Random().nextBytes(data);

        // sign random data using SHA1withDSA and verify using
        // SHA1withDSA and RawDSA
        Signature s = Signature.getInstance("SHA1withDSA", provider);
        s.initSign(privateKey);
        s.update(data);
        byte[] s1 = s.sign();

        s.initVerify(publicKey);
        s.update(data);
        if (!s.verify(s1)) {
            throw new Exception("Sign/verify 1 failed");
        }

        s = Signature.getInstance("RawDSA", provider);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(data);
        s.initVerify(publicKey);
        s.update(digest);
        if (!s.verify(s1)) {
            throw new Exception("Sign/verify 2 failed");
        }

        // sign random data using RawDSA and verify using
        // SHA1withDSA and RawDSA
        s.initSign(privateKey);
        s.update(digest);
        byte[] s2 = s.sign();

        s.initVerify(publicKey);
        s.update(digest);
        if (!s.verify(s2)) {
            throw new Exception("Sign/verify 3 failed");
        }

        s = Signature.getInstance("SHA1withDSA", provider);
        s.initVerify(publicKey);
        s.update(data);
        if (!s.verify(s2)) {
            throw new Exception("Sign/verify 4 failed");
        }

        // test behavior if data of incorrect length is passed
        s = Signature.getInstance("RawDSA", provider);
        s.initSign(privateKey);
        s.update(new byte[8]);
        s.update(new byte[64]);
        try {
            s.sign();
            throw new Exception("No error RawDSA signing long data");
        } catch (SignatureException e) {
            // expected
        }
    }

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String toString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }

    public static byte[] parse(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out = new ByteArrayOutputStream(n / 3);
            StringReader r = new StringReader(s);
            while (true) {
                int b1 = nextNibble(r);
                if (b1 < 0) {
                    break;
                }
                int b2 = nextNibble(r);
                if (b2 < 0) {
                    throw new RuntimeException("Invalid string " + s);
                }
                int b = (b1 << 4) | b2;
                out.write(b);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] b(String s) {
        return parse(s);
    }

    private static int nextNibble(StringReader r) throws IOException {
        while (true) {
            int ch = r.read();
            if (ch == -1) {
                return -1;
            } else if ((ch >= '0') && (ch <= '9')) {
                return ch - '0';
            } else if ((ch >= 'a') && (ch <= 'f')) {
                return ch - 'a' + 10;
            } else if ((ch >= 'A') && (ch <= 'F')) {
                return ch - 'A' + 10;
            }
        }
    }

}
