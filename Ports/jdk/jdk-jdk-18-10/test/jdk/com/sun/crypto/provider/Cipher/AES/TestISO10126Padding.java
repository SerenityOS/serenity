/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4921443
 * @summary Ensure ISO10126Padding works correctly.
 * @author Valerie Peng
 * @key randomness
 */
import java.util.Arrays;
import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class TestISO10126Padding {
    private static final String ALGO = "AES";
    private static final String TRANS = "AES/ECB";
    private static final int KEYSIZE = 16; // in bytes

    private SecretKey key;

    private TestISO10126Padding() throws Exception {
        // setup
        KeyGenerator kg = KeyGenerator.getInstance(ALGO, "SunJCE");
        kg.init(KEYSIZE*8);
        key = kg.generateKey();
    }

    private void runTest(int dataLength) throws Exception {
        // setup
        byte[] data = new byte[dataLength];
        new SecureRandom().nextBytes(data);
        System.out.println("Testing data length: " + dataLength);

        // TEST#1 --
        // generate the cipher text using manually-supplied
        // XML Encryption padding
        Cipher ci = Cipher.getInstance(TRANS + "/NoPadding", "SunJCE");
        ci.init(Cipher.ENCRYPT_MODE, key);
        byte[] paddedData = new byte[ci.getBlockSize()];
        System.arraycopy(data, 0, paddedData, 0, data.length);
        int padValue = paddedData.length - data.length;
        paddedData[paddedData.length-1] = (byte) padValue;
        byte[] cipherText = ci.doFinal(paddedData);

        // decrypt using ISO10126Padding
        ci = Cipher.getInstance(TRANS + "/ISO10126Padding", "SunJCE");
        ci.init(Cipher.DECRYPT_MODE, key);
        byte[] recovered = ci.doFinal(cipherText);
        if (!Arrays.equals(data, recovered)) {
            throw new Exception("TEST#1: decryption failed");
        }
        // TEST#2 --
        // generate the cipher text using ISO10126Padding
        ci = Cipher.getInstance(TRANS + "/ISO10126Padding", "SunJCE");
        ci.init(Cipher.ENCRYPT_MODE, key);
        cipherText = ci.doFinal(data);

        // decrypt using ISO10126Padding
        ci.init(Cipher.DECRYPT_MODE, key);
        recovered = ci.doFinal(cipherText);
        if (!Arrays.equals(data, recovered)) {
            throw new Exception("TEST#2: decryption failed");
        }
    }

    public static void main(String[] argv) throws Exception {
        TestISO10126Padding test = new TestISO10126Padding();
        for (int i = 0; i<16; i++) {
            test.runTest(i);
        }
        System.out.println("Test Passed");
    }
}
