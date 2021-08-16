/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary DHKeyAgreement3
 * @author Jan Luehe
 */

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.HexFormat;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

/**
 * This test utility executes the Diffie-Hellman key agreement protocol
 * between 3 parties: Alice, Bob, and Carol.
 *
 * We use the same 1024 bit prime modulus and base generator that are used by
 * SKIP.
 */

public class DHKeyAgreement3 {

    // Hex formatter to upper case with ":" delimiter
    private static final HexFormat HEX_FORMATTER = HexFormat.ofDelimiter(":").withUpperCase();

    private DHKeyAgreement3() {}

    public static void main(String argv[]) throws Exception {
            DHKeyAgreement3 keyAgree = new DHKeyAgreement3();
            keyAgree.run();
            System.out.println("Test Passed");
    }

    private void run() throws Exception {

        DHParameterSpec dhSkipParamSpec;

        System.err.println("Using SKIP Diffie-Hellman parameters");
        dhSkipParamSpec = new DHParameterSpec(skip1024Modulus, skip1024Base);

        // Alice creates her own DH key pair
        System.err.println("ALICE: Generate DH keypair ...");
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH", "SunJCE");
        aliceKpairGen.initialize(dhSkipParamSpec);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        // Bob creates his own DH key pair
        System.err.println("BOB: Generate DH keypair ...");
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH", "SunJCE");
        bobKpairGen.initialize(dhSkipParamSpec);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();

        // Carol creates her own DH key pair
        System.err.println("CAROL: Generate DH keypair ...");
        KeyPairGenerator carolKpairGen = KeyPairGenerator.getInstance("DH", "SunJCE");
        carolKpairGen.initialize(dhSkipParamSpec);
        KeyPair carolKpair = carolKpairGen.generateKeyPair();


        // Alice initialize
        System.err.println("ALICE: Initialize ...");
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH", "SunJCE");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        // Bob initialize
        System.err.println("BOB: Initialize ...");
        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH", "SunJCE");
        bobKeyAgree.init(bobKpair.getPrivate());

        // Carol initialize
        System.err.println("CAROL: Initialize ...");
        KeyAgreement carolKeyAgree = KeyAgreement.getInstance("DH", "SunJCE");
        carolKeyAgree.init(carolKpair.getPrivate());


        // Alice uses Carol's public key
        Key ac = aliceKeyAgree.doPhase(carolKpair.getPublic(), false);

        // Bob uses Alice's public key
        Key ba = bobKeyAgree.doPhase(aliceKpair.getPublic(), false);

        // Carol uses Bob's public key
        Key cb = carolKeyAgree.doPhase(bobKpair.getPublic(), false);


        // Alice uses Carol's result from above
        aliceKeyAgree.doPhase(cb, true);

        // Bob uses Alice's result from above
        bobKeyAgree.doPhase(ac, true);

        // Carol uses Bob's result from above
        carolKeyAgree.doPhase(ba, true);


        // Alice, Bob and Carol compute their secrets
        byte[] aliceSharedSecret = aliceKeyAgree.generateSecret();
        int aliceLen = aliceSharedSecret.length;
        System.out.println("Alice secret: " + HEX_FORMATTER.formatHex(aliceSharedSecret));

        byte[] bobSharedSecret = bobKeyAgree.generateSecret();
        int bobLen = bobSharedSecret.length;
        System.out.println("Bob secret: " + HEX_FORMATTER.formatHex(bobSharedSecret));

        byte[] carolSharedSecret = carolKeyAgree.generateSecret();
        int carolLen = carolSharedSecret.length;
        System.out.println("Carol secret: " + HEX_FORMATTER.formatHex(carolSharedSecret));


        // Compare Alice and Bob
        if (aliceLen != bobLen) {
            throw new Exception("Alice and Bob have different lengths");
        }
        for (int i=0; i<aliceLen; i++) {
            if (aliceSharedSecret[i] != bobSharedSecret[i]) {
                throw new Exception("Alice and Bob differ");
            }
        }
        System.err.println("Alice and Bob are the same");

        // Compare Bob and Carol
        if (bobLen != carolLen) {
            throw new Exception("Bob and Carol have different lengths");
        }
        for (int i=0; i<bobLen; i++) {
            if (bobSharedSecret[i] != carolSharedSecret[i]) {
                throw new Exception("Bob and Carol differ");
            }
        }
        System.err.println("Bob and Carol are the same");
    }


    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    private void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                            '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Prints the usage of this test.
     */
    private void usage() {
        System.err.print("DHKeyAgreement usage: ");
        System.err.println("[-gen]");
    }

    // The 1024 bit Diffie-Hellman modulus values used by SKIP
    private static final byte skip1024ModulusBytes[] = {
        (byte)0xF4, (byte)0x88, (byte)0xFD, (byte)0x58,
        (byte)0x4E, (byte)0x49, (byte)0xDB, (byte)0xCD,
        (byte)0x20, (byte)0xB4, (byte)0x9D, (byte)0xE4,
        (byte)0x91, (byte)0x07, (byte)0x36, (byte)0x6B,
        (byte)0x33, (byte)0x6C, (byte)0x38, (byte)0x0D,
        (byte)0x45, (byte)0x1D, (byte)0x0F, (byte)0x7C,
        (byte)0x88, (byte)0xB3, (byte)0x1C, (byte)0x7C,
        (byte)0x5B, (byte)0x2D, (byte)0x8E, (byte)0xF6,
        (byte)0xF3, (byte)0xC9, (byte)0x23, (byte)0xC0,
        (byte)0x43, (byte)0xF0, (byte)0xA5, (byte)0x5B,
        (byte)0x18, (byte)0x8D, (byte)0x8E, (byte)0xBB,
        (byte)0x55, (byte)0x8C, (byte)0xB8, (byte)0x5D,
        (byte)0x38, (byte)0xD3, (byte)0x34, (byte)0xFD,
        (byte)0x7C, (byte)0x17, (byte)0x57, (byte)0x43,
        (byte)0xA3, (byte)0x1D, (byte)0x18, (byte)0x6C,
        (byte)0xDE, (byte)0x33, (byte)0x21, (byte)0x2C,
        (byte)0xB5, (byte)0x2A, (byte)0xFF, (byte)0x3C,
        (byte)0xE1, (byte)0xB1, (byte)0x29, (byte)0x40,
        (byte)0x18, (byte)0x11, (byte)0x8D, (byte)0x7C,
        (byte)0x84, (byte)0xA7, (byte)0x0A, (byte)0x72,
        (byte)0xD6, (byte)0x86, (byte)0xC4, (byte)0x03,
        (byte)0x19, (byte)0xC8, (byte)0x07, (byte)0x29,
        (byte)0x7A, (byte)0xCA, (byte)0x95, (byte)0x0C,
        (byte)0xD9, (byte)0x96, (byte)0x9F, (byte)0xAB,
        (byte)0xD0, (byte)0x0A, (byte)0x50, (byte)0x9B,
        (byte)0x02, (byte)0x46, (byte)0xD3, (byte)0x08,
        (byte)0x3D, (byte)0x66, (byte)0xA4, (byte)0x5D,
        (byte)0x41, (byte)0x9F, (byte)0x9C, (byte)0x7C,
        (byte)0xBD, (byte)0x89, (byte)0x4B, (byte)0x22,
        (byte)0x19, (byte)0x26, (byte)0xBA, (byte)0xAB,
        (byte)0xA2, (byte)0x5E, (byte)0xC3, (byte)0x55,
        (byte)0xE9, (byte)0x2F, (byte)0x78, (byte)0xC7
    };

    // The SKIP 1024 bit modulus
    private static final BigInteger skip1024Modulus
    = new BigInteger(1, skip1024ModulusBytes);

    // The base used with the SKIP 1024 bit modulus
    private static final BigInteger skip1024Base = BigInteger.valueOf(2);
}
