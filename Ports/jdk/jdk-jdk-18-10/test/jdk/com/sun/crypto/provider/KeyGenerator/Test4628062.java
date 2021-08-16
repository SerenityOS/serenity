/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4628062 4963723
 * @summary Verify that AES KeyGenerator supports default initialization
 *      when init is not called
 * @author Valerie Peng
 */
import java.security.*;
import javax.crypto.*;
import java.util.*;

public class Test4628062 {

    private static final int[] AES_SIZES = { 16, 24, 32 }; // in bytes
    private static final int[] HMACSHA224_SIZES = { 28 };
    private static final int[] HMACSHA256_SIZES = { 32 };
    private static final int[] HMACSHA384_SIZES = { 48 };
    private static final int[] HMACSHA512_SIZES = { 64 };

    public boolean execute(String algo, int[] keySizes) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(algo, "SunJCE");

        // TEST FIX 4628062
        Key keyWithDefaultSize = kg.generateKey();
        byte[] encoding = keyWithDefaultSize.getEncoded();
        int defKeyLen = encoding.length ;
        if (defKeyLen == 0) {
            throw new Exception("default key length is 0!");
        } else if (defKeyLen != keySizes[0]) {
            throw new Exception("default key length mismatch!");
        }

        // BONUS TESTS
        if (keySizes.length > 1) {
            // 1. call init(int keysize) with various valid key sizes
            // and see if the generated key is the right size.
            for (int i=0; i<keySizes.length; i++) {
                kg.init(keySizes[i]*8); // in bits
                Key key = kg.generateKey();
                if (key.getEncoded().length != keySizes[i]) {
                    throw new Exception("key is generated with the wrong length!");
                }
            }
            // 2. call init(int keysize) with invalid key size and see
            // if the expected InvalidParameterException is thrown.
            try {
                kg.init(keySizes[0]*8+1);
            } catch (InvalidParameterException ex) {
            } catch (Exception ex) {
                throw new Exception("wrong exception is thrown for invalid key size!");
            }
        }
        // passed all tests...hooray!
        return true;
    }

    public static void main (String[] args) throws Exception {
        Test4628062 test = new Test4628062();
        String testName = test.getClass().getName();
        if (test.execute("AES", AES_SIZES)) {
            System.out.println(testName + ": AES Passed!");
        }
        if (test.execute("HmacSHA224", HMACSHA224_SIZES)) {
            System.out.println(testName + ": HmacSHA224 Passed!");
        }
        if (test.execute("HmacSHA256", HMACSHA256_SIZES)) {
            System.out.println(testName + ": HmacSHA256 Passed!");
        }
        if (test.execute("HmacSHA384", HMACSHA384_SIZES)) {
            System.out.println(testName + ": HmacSHA384 Passed!");
        }
        if (test.execute("HmacSHA512", HMACSHA512_SIZES)) {
            System.out.println(testName + ": HmacSHA512 Passed!");
        }
    }
}
