/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072452
 * @summary Support DHE sizes up to 8192-bits and DSA sizes up to 3072-bits
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm SupportedDHKeys
 * @run main/othervm -Djava.security.manager=allow SupportedDHKeys sm
 */

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

public class SupportedDHKeys extends PKCS11Test {

    /*
     * Sizes and values for various lengths.
     */
    private enum SupportedKeySize {
        dhp512(512),   dhp768(768),    dhp832(832),
        dhp1024(1024), dhp1536(1536),  dhp2048(2048);

        // the underlying pkcs11 may not support the following sizes yet
        //
        // dhp3072(3072), dhp4096(4096),  dhp6144(6144),
        // dhp8192(8192);

        final int primeSize;

        SupportedKeySize(int primeSize) {
            this.primeSize = primeSize;
        }
    }

    @Override
    public void main(Provider provider) throws Exception {
        if (provider.getService("KeyPairGenerator", "DiffieHellman") == null) {
            System.out.println("No support of DH KeyPairGenerator, skipping");
            return;
        }

        for (SupportedKeySize keySize : SupportedKeySize.values()) {
            System.out.println("Checking " + keySize.primeSize + " ...");
            KeyPairGenerator kpg =
                    KeyPairGenerator.getInstance("DiffieHellman", provider);
            kpg.initialize(keySize.primeSize);
            KeyPair kp = kpg.generateKeyPair();
            checkKeyPair(kp, keySize.primeSize, provider);

            DHPublicKey publicKey = (DHPublicKey)kp.getPublic();
            BigInteger p = publicKey.getParams().getP();
            BigInteger g = publicKey.getParams().getG();
            kpg.initialize(new DHParameterSpec(p, g));
            kp = kpg.generateKeyPair();
            checkKeyPair(kp, keySize.primeSize, provider);
        }
    }

    private static void checkKeyPair(KeyPair kp, int pSize,
                Provider provider) throws Exception {

        DHPrivateKey privateKey = (DHPrivateKey)kp.getPrivate();
        BigInteger p = privateKey.getParams().getP();
        if (p.bitLength() != pSize) {
            throw new Exception(
                "Invalid modulus size: " + p.bitLength() + "/" + pSize);
        }

        // System.out.println("P(" + pSize + "): " + p.toString());
        if (!p.isProbablePrime(128)) {
            throw new Exception("Good luck, the modulus is composite!");
        }

        DHPublicKey publicKey = (DHPublicKey)kp.getPublic();
        p = publicKey.getParams().getP();
        if (p.bitLength() != pSize) {
            throw new Exception(
                "Invalid modulus size: " + p.bitLength() + "/" + pSize);
        }

        BigInteger leftOpen = BigInteger.ONE;
        BigInteger rightOpen = p.subtract(BigInteger.ONE);

        BigInteger x = privateKey.getX();
        if ((x.compareTo(leftOpen) <= 0) ||
            (x.compareTo(rightOpen) >= 0)) {
            throw new Exception(
                "X outside range [2, p - 2]:  x: " + x + " p: " + p);
        }

        BigInteger y = publicKey.getY();
        if ((y.compareTo(leftOpen) <= 0) ||
                (y.compareTo(rightOpen) >= 0)) {
            throw new Exception(
                "Y outside range [2, p - 2]:  y: " + y + " p: " + p);
        }
    }

    public static void main(String[] args) throws Exception {
        main(new SupportedDHKeys(), args);
    }
}
