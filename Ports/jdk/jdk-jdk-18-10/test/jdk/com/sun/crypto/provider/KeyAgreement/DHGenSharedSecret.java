/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 0000000
 * @summary DHGenSharedSecret
 * @author Jan Luehe
 */
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.BigInteger;

public class DHGenSharedSecret {

    static byte[] DHPrime = {
(byte)0x00, (byte)0x8D, (byte)0x8A, (byte)0x6C, (byte)0x7F, (byte)0xCC,
(byte)0xA5, (byte)0xBF, (byte)0x9C, (byte)0xE1, (byte)0xFA, (byte)0x3C,
(byte)0xCA, (byte)0x98, (byte)0xB7, (byte)0x99, (byte)0xD1, (byte)0xE5,
(byte)0x2C, (byte)0xC0, (byte)0x26, (byte)0x97, (byte)0x12, (byte)0x80,
(byte)0x12, (byte)0xEF, (byte)0x0B, (byte)0xDE, (byte)0x71, (byte)0x76,
(byte)0xAA, (byte)0x2D, (byte)0x86, (byte)0x41, (byte)0x0E, (byte)0x6A,
(byte)0xC2, (byte)0x12, (byte)0xAA, (byte)0xAA, (byte)0xE4, (byte)0x84,
(byte)0x80, (byte)0x13, (byte)0x95, (byte)0x06, (byte)0xC4, (byte)0x83,
(byte)0xB9, (byte)0xD3, (byte)0x72, (byte)0xC5, (byte)0xC8, (byte)0x85,
(byte)0x96, (byte)0x59, (byte)0x08, (byte)0xFA, (byte)0x9E, (byte)0x3C,
(byte)0xDC, (byte)0x92, (byte)0x28, (byte)0xC3, (byte)0x1D, (byte)0x6F,
(byte)0x44, (byte)0x36, (byte)0x70, (byte)0x40, (byte)0x80, (byte)0xF1,
(byte)0x35
    };

    static byte[] DHBase = {
(byte)0x72, (byte)0x21, (byte)0xB3, (byte)0xA8, (byte)0x83, (byte)0xDD,
(byte)0x76, (byte)0xF5, (byte)0x0D, (byte)0x9B, (byte)0x81, (byte)0x11,
(byte)0x15, (byte)0x03, (byte)0x6D, (byte)0x4D, (byte)0x46, (byte)0x65,
(byte)0x30, (byte)0xB0, (byte)0xFA, (byte)0xFE, (byte)0xBE, (byte)0xA8,
(byte)0xD9, (byte)0x83, (byte)0x33, (byte)0x54, (byte)0xC7, (byte)0xF6,
(byte)0x81, (byte)0xAC, (byte)0xCC, (byte)0xA3, (byte)0xAE, (byte)0xAA,
(byte)0xC8, (byte)0x11, (byte)0x38, (byte)0xD4, (byte)0x4F, (byte)0xC4,
(byte)0x89, (byte)0xD3, (byte)0x72, (byte)0xEE, (byte)0x22, (byte)0x5A,
(byte)0x68, (byte)0xF7, (byte)0xAC, (byte)0x24, (byte)0x01, (byte)0x9B,
(byte)0xE9, (byte)0x08, (byte)0xFE, (byte)0x58, (byte)0x0A, (byte)0xCF,
(byte)0xB9, (byte)0x52, (byte)0xB4, (byte)0x02, (byte)0x73, (byte)0xA4,
(byte)0xA6, (byte)0xB9, (byte)0x0C, (byte)0x8D, (byte)0xA7, (byte)0xFB,
    };

    public static void main(String[] args) throws Exception {
        DHGenSharedSecret test = new DHGenSharedSecret();
        test.run();
    }

    public void run() throws Exception {
        long start, end;

        BigInteger p = new BigInteger(1, DHPrime);
        BigInteger g = new BigInteger(1, DHBase);
        int l = 512;

        DHParameterSpec spec =
            new DHParameterSpec(p, g, l);

        // generate keyPairs using parameters
        KeyPairGenerator keyGen =
            KeyPairGenerator.getInstance("DH", "SunJCE");
        keyGen.initialize(spec);

        // Alice generates her key pairs
        KeyPair keyA = keyGen.generateKeyPair();

        // Bob generates his key pairs
        KeyPair keyB = keyGen.generateKeyPair();

        // Alice encodes her public key in x509 format
        // , and sends it over to Bob.
        byte[] alicePubKeyEnc = keyA.getPublic().getEncoded();

        // bob encodes his publicKey in x509 format and
        // sends it over to Alice
        byte[] bobPubKeyEnc = keyB.getPublic().getEncoded();

        // bob uses it to generate Secret
        X509EncodedKeySpec x509Spec =
            new X509EncodedKeySpec(alicePubKeyEnc);
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH", "SunJCE");
        PublicKey alicePubKey = bobKeyFac.generatePublic(x509Spec);


        KeyAgreement bobAlice = KeyAgreement.getInstance("DH", "SunJCE");
        start = System.currentTimeMillis();
        bobAlice.init(keyB.getPrivate());
        bobAlice.doPhase(alicePubKey, true);
        byte[] bobSecret = bobAlice.generateSecret();
        end = System.currentTimeMillis();

        System.out.println("Time elapsed: " + (end - start));
        System.out.println("Test Passed!");
    }
}
