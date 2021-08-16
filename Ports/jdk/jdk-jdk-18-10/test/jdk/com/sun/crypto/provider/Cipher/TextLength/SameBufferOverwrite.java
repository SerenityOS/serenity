/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.AlgorithmParameters;
import java.util.Arrays;


/*
 * @test
 * @summary Verify when decrypting over an existing buffer than padding does not
 * overwrite past what the plaintext length is.
 *
 */

public class SameBufferOverwrite {

    private SecretKey skey;
    private Cipher c;
    private int start = 17, end = 17;  // default

    SameBufferOverwrite(String algo, String transformation)
        throws Exception {

        KeyGenerator kg = KeyGenerator.getInstance(algo, "SunJCE");
        skey = kg.generateKey();
        c = Cipher.getInstance(transformation, "SunJCE");
    }

    /*
     * Run the test
     */
    void test() throws Exception {
        byte[] in = new byte[end + (c.getBlockSize() - (end % c.getBlockSize()))];
        Arrays.fill(in, (byte)8);
        int len = start;
        AlgorithmParameters params = null;

        System.out.println("Testing transformation: " + c.getAlgorithm() +
            ",  byte length from " + start + " to " + end);
        while (end >= len) {
            // encrypt
            c.init(Cipher.ENCRYPT_MODE, skey, params);
            byte[] out = c.doFinal(in, 0, len);
            System.out.println("  enc = " + byteToHex(out));
            System.out.println("  => enc " + len + " bytes, ret " +
                (out == null ? "null" : (out.length + " byte")));

            // decrypt
            params = c.getParameters();
            c.init(Cipher.DECRYPT_MODE, skey, params);
            int rLen = c.doFinal(out, 0, out.length, in);
            System.out.println("  dec = " + byteToHex(in));
            System.out.println("  => dec " + out.length + " bytes, ret " +
                rLen + " byte");
            // check if more than rLen bytes are written into 'in'
            for (int j = rLen; j < in.length; j++) {
                if (in[j] != (byte) 8) {
                    throw new Exception("Value check failed at index " + j);
                }
            }
            System.out.println(" Test Passed:  len = " + len);
            len++;

            // Because GCM doesn't allow params reuse
            if (c.getAlgorithm().contains("GCM")) {
                params = null;
            }
        }
    }

    /**
     * Builder method for the test to run data lengths from the start value to
     * the end value.  To do one length, have start and end equal that number.
     * @param start starting data length
     * @param end ending data length
     */
    SameBufferOverwrite iterate(int start, int end) {
        this.start = start;
        this.end = end;
        return this;
    }

    public static void main(String args[]) throws Exception {
        new SameBufferOverwrite("AES", "AES/GCM/NoPadding").iterate(1, 25).
            test();
        new SameBufferOverwrite("AES", "AES/CTR/NoPadding").iterate(1, 25).
            test();
        new SameBufferOverwrite("AES", "AES/CBC/PKCS5Padding").iterate(1, 25).
            test();
        new SameBufferOverwrite("AES", "AES/ECB/PKCS5Padding").iterate(1, 25).
            test();
        new SameBufferOverwrite("DES", "DES/CBC/PKCS5Padding").iterate(1, 17).
            test();
        new SameBufferOverwrite("DESede", "DESede/CBC/PKCS5Padding").iterate(1, 17).
            test();
    }

    private static String byteToHex(byte[] barray) {
        StringBuilder s = new StringBuilder();
        for (byte b : barray) {
            s.append(String.format("%02x", b));
        }
        return s.toString();
    }
}
