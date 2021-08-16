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
 * @summary DHKeyGenSpeed
 * @author Jan Luehe
 */
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.*;

public class DHKeyGenSpeed {

    static byte[] DHPrime = {
(byte)0x00, (byte)0x91, (byte)0x18, (byte)0x26, (byte)0x9A, (byte)0x26,
(byte)0x43, (byte)0xA6, (byte)0x1E, (byte)0x11, (byte)0x02, (byte)0xA0,
(byte)0x88, (byte)0xFE, (byte)0x12, (byte)0xEA, (byte)0x63, (byte)0x20,
(byte)0x6D, (byte)0x4F, (byte)0x40, (byte)0x3C, (byte)0x4F, (byte)0x13,
(byte)0x10, (byte)0x97, (byte)0xEC, (byte)0x3A, (byte)0x38, (byte)0x87,
(byte)0x9B, (byte)0x08, (byte)0x66, (byte)0x0C, (byte)0x82, (byte)0xD0,
(byte)0x57, (byte)0xE0, (byte)0x37, (byte)0x16, (byte)0x8E, (byte)0xB4,
(byte)0xEA, (byte)0xB7, (byte)0xE6, (byte)0xAF, (byte)0x4C, (byte)0xE0,
(byte)0x40, (byte)0x07, (byte)0xF4, (byte)0x81, (byte)0xDD, (byte)0x36,
(byte)0x33, (byte)0xAD, (byte)0x92, (byte)0xC6, (byte)0x0F, (byte)0xB5,
(byte)0xE4, (byte)0x0F, (byte)0x0E, (byte)0xEA, (byte)0x91, (byte)0x35,
(byte)0xFB, (byte)0x55, (byte)0x7A, (byte)0x39, (byte)0xD1, (byte)0xF0,
(byte)0x6B, (byte)0x9A, (byte)0xB9, (byte)0xFA, (byte)0x19, (byte)0xBE,
(byte)0x1B, (byte)0xFD, (byte)0x77
    };
    static byte[] DHBase = {
(byte)0x29, (byte)0xF2, (byte)0x29, (byte)0xC8, (byte)0x42, (byte)0x25,
(byte)0x29, (byte)0xC3, (byte)0xF2, (byte)0xAA, (byte)0xF2, (byte)0x6A,
(byte)0x3C, (byte)0xD2, (byte)0xD2, (byte)0xDE, (byte)0xD3, (byte)0x6B,
(byte)0x85, (byte)0xA5, (byte)0xE1, (byte)0x43, (byte)0x90, (byte)0xA2,
(byte)0xB6, (byte)0xA5, (byte)0x0C, (byte)0xBA, (byte)0xB9, (byte)0x4C,
(byte)0x25, (byte)0xE0, (byte)0xC8, (byte)0xEA, (byte)0xA1, (byte)0x7B,
(byte)0xB9, (byte)0xF8, (byte)0xFF, (byte)0x15, (byte)0x66, (byte)0x5B,
(byte)0xB0, (byte)0x00, (byte)0x18, (byte)0xE2, (byte)0xF4, (byte)0xF1,
(byte)0xB4, (byte)0x7A, (byte)0xC2, (byte)0xCF, (byte)0x9C, (byte)0x61,
(byte)0x36, (byte)0xED, (byte)0x14, (byte)0x72, (byte)0xD7, (byte)0xD4,
(byte)0x94, (byte)0x20, (byte)0x5E, (byte)0x1E, (byte)0xE4, (byte)0xB1,
(byte)0x60, (byte)0xC8, (byte)0x10, (byte)0x85, (byte)0xBD, (byte)0x74,
(byte)0x34, (byte)0x8C, (byte)0x3C, (byte)0x2A, (byte)0xBD, (byte)0x3C,
(byte)0xFF, (byte)0x14
    };

    public static void main(String[] args) throws Exception {
        DHKeyGenSpeed test = new DHKeyGenSpeed();
        test.run();
        System.out.println("Test Passed");
    }

    public void run() throws Exception {
        long start, end;

        BigInteger p = new BigInteger(1, DHPrime);
        BigInteger g = new BigInteger(1, DHBase);
        int l = 576;

        DHParameterSpec spec =
            new DHParameterSpec(p, g, l);

        // generate keyPairs using parameters
        KeyPairGenerator keyGen =
            KeyPairGenerator.getInstance("DH", "SunJCE");
        start = System.currentTimeMillis();
        keyGen.initialize(spec);
        KeyPair keys = keyGen.generateKeyPair();
        end = System.currentTimeMillis();

        System.out.println("PrimeBits\tExponentBits");
        System.out.println(DHPrime.length*8 + "\t\t" + l);
        System.out.println("keyGen(millisecond): " + (end - start));
        System.out.println("Test Passed!");
    }
}
