/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6330287 6331386 7044060
 * @summary verify that DHKeyPairGenerator returns keys of the expected size
 * (modulus and exponent)
 * -and-
 *      DHKeyPairGenerator is using BigInteger.setBit
 * @author Andreas Sterbenz
 */

import java.math.BigInteger;
import java.util.Arrays;

import java.security.*;
import javax.crypto.*;
import javax.crypto.interfaces.*;
import javax.crypto.spec.*;

/*
 * NOTE:  BigInteger's bitlength() doesn't report leading zeros, only
 * the number of bits needed to actually represent the number.  i.e.
 *
 *     new BigInteger("4").bitLength() = 3
 *
 * Since the private key x can vary 1 <= x <= p-2, we can't do any
 * bitlength-based calculations here.  But we can check that p conforms
 * as expected.
 *
 * If not specified, we're currently using an lsize of Math.max(384, psize/2).
 */
public class TestExponentSize {

    /*
     * Sizes and values for various lengths.
     */
    private enum Sizes {
        two56(256), three84(384), five12(512), seven68(768), ten24(1024),
        twenty48(2048);

        private final int intSize;
        private final BigInteger bigIntValue;

        Sizes(int size) {
            intSize = size;
            byte [] bits = new byte[intSize/8];
            Arrays.fill(bits, (byte)0xff);
            bigIntValue = new BigInteger(1, bits);
        }

        int getIntSize() {
            return intSize;
        }

        BigInteger getBigIntValue() {
            return bigIntValue;
        }
    }

    public static void main(String[] args) throws Exception {
        KeyPair kp;
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH", "SunJCE");

        // Sun's default uses a default psize of 2048 and
        // lsize of (pSize / 2) but at least 384 bits
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.twenty48, Sizes.ten24);

        DHPublicKey publicKey = (DHPublicKey)kp.getPublic();
        BigInteger p = publicKey.getParams().getP();
        BigInteger g = publicKey.getParams().getG();

        kpg.initialize(Sizes.ten24.getIntSize());
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.ten24, Sizes.five12);

        kpg.initialize(new DHParameterSpec(p, g, Sizes.ten24.getIntSize()));
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.twenty48, Sizes.ten24);

        kpg.initialize(new DHParameterSpec(p, g, Sizes.five12.getIntSize()));
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.twenty48, Sizes.five12);

        kpg.initialize(new DHParameterSpec(p, g, Sizes.two56.getIntSize()));
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.twenty48, Sizes.two56);

        kpg.initialize(Sizes.five12.getIntSize());
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.five12, Sizes.three84);

        kpg.initialize(Sizes.seven68.getIntSize());
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.seven68, Sizes.three84);

        // test w/ only pSize
        kpg.initialize(Sizes.twenty48.getIntSize());
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.twenty48, Sizes.ten24);

        publicKey = (DHPublicKey)kp.getPublic();
        p = publicKey.getParams().getP();
        g = publicKey.getParams().getG();

        // test w/ all values specified
        kpg.initialize(new DHParameterSpec(p, g, Sizes.five12.getIntSize()));
        kp = kpg.generateKeyPair();
        checkKeyPair(kp, Sizes.twenty48, Sizes.five12);

        System.out.println("OK");
    }

    private static void checkKeyPair(KeyPair kp, Sizes modulusSize,
            Sizes exponentSize) throws Exception {

        System.out.println("Checking (" + modulusSize + ", " +
            exponentSize + ")");
        DHPrivateKey privateKey = (DHPrivateKey)kp.getPrivate();
        BigInteger p = privateKey.getParams().getP();
        BigInteger x = privateKey.getX();

        if (p.bitLength() != modulusSize.getIntSize()) {
            throw new Exception("Invalid modulus size: " + p.bitLength());
        }

        if (x.bitLength() > exponentSize.getIntSize()) {
            throw new Exception("X has more bits than expected: " +
                x.bitLength());
        }

        BigInteger pMinus2 =
            p.subtract(BigInteger.ONE).subtract(BigInteger.ONE);

        if ((x.compareTo(BigInteger.ONE) < 0 ||
                (x.compareTo(pMinus2)) > 0)) {
            throw new Exception(
                "X outside range 1<=x<p-2:  x: " + x + " p: " + p);
        }
    }
}
