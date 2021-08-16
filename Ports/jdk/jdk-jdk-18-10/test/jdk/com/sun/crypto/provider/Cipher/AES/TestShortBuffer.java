/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4979126
 * @summary Ensure update()/doFinal() matches javadoc description
 * when ShortBufferException is thrown.
 * @author Valerie Peng
 */
import java.util.Arrays;
import java.security.*;
import java.security.spec.*;
import java.math.BigInteger;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class TestShortBuffer {
    private static final String ALGO = "AES";
    private static final String[] MODES = {
        "ECB", "CBC", "PCBC", "CFB16", "OFB8"
    };
    private static final SecretKey KEY =
        new SecretKeySpec(new byte[16], ALGO);
    private static byte[] SHORTBUFFER = new byte[1];

    private static final byte[] PLAINTEXT  = new byte[30];
    static {
        PLAINTEXT[0] = (byte)0x15;
    };
    private Cipher ci = null;
    private byte[] in = null;
    private byte[] expected = null;
    private byte[] out = null;
    private int outOffset = 0;

    private TestShortBuffer(Cipher ci) {
        this.ci = ci;
    }

    private void init(byte[] in, byte[] expected) {
        this.in = (byte[]) in.clone();
        this.expected = (byte[]) expected.clone();
        this.out = new byte[expected.length];
        this.outOffset = 0;
   }

    private static void runTest() throws Exception {
        // Initialization
        for (int i = 0; i < MODES.length; i++) {
            System.out.println("===== TESTING MODE " + MODES[i] + " =====");
            Cipher ci = Cipher.getInstance(ALGO+"/"+MODES[i]+"/PKCS5Padding",
                                           "SunJCE");
            TestShortBuffer test = null;
            int stored = 0;
            AlgorithmParameters params = null;
            byte[] cipherText = null;
            byte[] shortBuffer = new byte[8];
            for (int k = 0; k < 2; k++) {
                byte[] expected = null;
                switch (k) {
                case 0: // Encryption
                    System.out.println("Testing with Cipher.ENCRYPT_MODE");
                    ci.init(Cipher.ENCRYPT_MODE, KEY);
                    cipherText = ci.doFinal(PLAINTEXT);
                    test = new TestShortBuffer(ci);
                    test.init(PLAINTEXT, cipherText);
                    params = ci.getParameters();
                    break;
                case 1: // Decryption
                    System.out.println("Testing with Cipher.DECRYPT_MODE");
                    ci.init(Cipher.DECRYPT_MODE, KEY, params);
                    test = new TestShortBuffer(ci);
                    test.init(cipherText, PLAINTEXT);
                    break;
                }
                int offset = 2 + i*5;
                test.testUpdate();
                test.testUpdateWithUpdate(offset);
                test.testDoFinal();
                test.testDoFinalWithUpdate(offset);
            }
        }
    }

    private void checkOutput() throws Exception {
        if (!Arrays.equals(out, expected)) {
            System.out.println("got: " + new BigInteger(out));
            System.out.println("expect: " + new BigInteger(expected));
            throw new Exception("Generated different outputs");
        }
    }
    private void testUpdate() throws Exception {
        outOffset = 0;
        int stored = 0;
        try {
            stored = ci.update(in, 0, in.length, SHORTBUFFER);
            throw new Exception("Should throw ShortBufferException!");
        } catch (ShortBufferException sbe) {
            System.out.println("Expected SBE thrown....");
            // retry with a large-enough buffer according to javadoc
            stored = ci.update(in, 0, in.length, out);
            stored = ci.doFinal(out, outOffset += stored);
            if (out.length != (stored + outOffset)) {
                throw new Exception("Wrong number of output bytes");
            }
        }
        checkOutput();
    }
    private void testUpdateWithUpdate(int offset) throws Exception {
        outOffset = 0;
        int stored = 0;
        byte[] out1 = ci.update(in, 0, offset);
        if (out1 != null) {
            System.arraycopy(out1, 0, out, 0, out1.length);
            outOffset += out1.length;
        }
        try {
            stored = ci.update(in, offset, in.length-offset, SHORTBUFFER);
            throw new Exception("Should throw ShortBufferException!");
        } catch (ShortBufferException sbe) {
            System.out.println("Expected SBE thrown....");
            // retry with a large-enough buffer according to javadoc
            stored = ci.update(in, offset, in.length-offset,
                               out, outOffset);
            stored = ci.doFinal(out, outOffset+=stored);
            if (out.length != (stored + outOffset)) {
                throw new Exception("Wrong number of output bytes");
            }
        }
        checkOutput();
    }
    private void testDoFinal() throws Exception {
        outOffset = 0;
        int stored = 0;
        try {
            stored = ci.doFinal(in, 0, in.length, SHORTBUFFER);
            throw new Exception("Should throw ShortBufferException!");
        } catch (ShortBufferException sbe) {
            System.out.println("Expected SBE thrown....");
            // retry with a large-enough buffer according to javadoc
            stored = ci.doFinal(in, 0, in.length, out, 0);
            if (out.length != stored) {
                throw new Exception("Wrong number of output bytes");
            }
        }
        checkOutput();
    }

    private void testDoFinalWithUpdate(int offset) throws Exception {
        outOffset = 0;
        int stored = 0;
        byte[] out1 = ci.update(in, 0, offset);
        if (out1 != null) {
            System.arraycopy(out1, 0, out, 0, out1.length);
            outOffset += out1.length;
        }
        try {
            stored = ci.doFinal(in, offset, in.length-offset, SHORTBUFFER);
            throw new Exception("Should throw ShortBufferException!");
        } catch (ShortBufferException sbe) {
            System.out.println("Expected SBE thrown....");
            // retry with a large-enough buffer according to javadoc
            stored = ci.doFinal(in, offset, in.length-offset,
                                out, outOffset);
            if (out.length != (stored + outOffset)) {
                throw new Exception("Wrong number of output bytes");
            }
        }
        checkOutput();
    }
    public static void main(String[] argv) throws Exception {
        runTest();
        System.out.println("Test Passed");
    }
}
