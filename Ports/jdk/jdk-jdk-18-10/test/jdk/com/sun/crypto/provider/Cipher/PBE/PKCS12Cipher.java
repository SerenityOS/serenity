/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4893959 6383200
 * @summary basic test for PBEWithSHA1AndDESede, PBEWithSHA1AndRC2_40/128
 *          and PBEWithSHA1AndRC4_40/128
 * @author Valerie Peng
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.PBEKey;

public class PKCS12Cipher {

    private static void runTest(String alg, byte[] plaintext,
                                char[] password, Provider p)
        throws Exception {
        Cipher cipher = Cipher.getInstance(alg, p);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBE", p);
        AlgorithmParameters pbeParams = null;
        SecretKey key = keyFac.generateSecret(pbeKeySpec);
        cipher.init(Cipher.ENCRYPT_MODE, key, pbeParams);
        byte[] enc1 = cipher.doFinal(plaintext);
        byte[] enc2 = cipher.doFinal(plaintext);
        if (Arrays.equals(enc1, enc2) == false) {
            throw new Exception("Re-encryption test failed");
        }
        pbeParams = cipher.getParameters();
        cipher.init(Cipher.DECRYPT_MODE, key, pbeParams);
        byte[] dec = cipher.doFinal(enc1);
        if (Arrays.equals(plaintext, dec) == false) {
            throw new Exception("decryption test for " + alg + " failed");
        }

        PBEParameterSpec spec = (PBEParameterSpec)
            pbeParams.getParameterSpec(PBEParameterSpec.class);
        PBEKey key2 = new
            MyPBEKey(password, spec.getSalt(), spec.getIterationCount());
        cipher.init(Cipher.DECRYPT_MODE, key2, pbeParams);
        byte[] dec2 = cipher.doFinal(enc1);
        if (Arrays.equals(dec2, dec) == false) {
            throw new Exception("Re-decryption test#1 failed");
        }

        cipher.init(Cipher.DECRYPT_MODE, key2, (AlgorithmParameters) null);
        byte[] dec3 = cipher.doFinal(enc1);
        if (Arrays.equals(dec3, dec) == false) {
            throw new Exception("Re-decryption test#2 failed");
        }

        System.out.println("passed: " + alg);
    }

    public static void main(String[] argv) throws Exception {
        byte[] input = new byte[1024];
        new SecureRandom().nextBytes(input);
        char[] PASSWD = { 'p','a','s','s','w','o','r','d' };
        long start = System.currentTimeMillis();
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        runTest("PBEWithSHA1AndDESede", input, PASSWD, p);
        runTest("PBEWithSHA1AndRC2_40", input, PASSWD, p);
        runTest("PBEWithSHA1AndRC2_128", input, PASSWD, p);
        runTest("PBEWithSHA1AndRC4_40", input, PASSWD, p);
        runTest("PBEWithSHA1AndRC4_128", input, PASSWD, p);
        System.out.println("All tests passed");
        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }
}

class MyPBEKey implements PBEKey {
    char[] passwd;
    byte[] salt;
    int iCount;
    MyPBEKey(char[] passwd, byte[] salt, int iCount) {
        this.passwd = passwd;
        this.salt = salt;
        this.iCount = iCount;
    }
    public char[] getPassword() { return passwd.clone(); }
    public byte[] getSalt() { return salt; }
    public int getIterationCount() { return iCount; }
    public String getAlgorithm() { return "PBE"; }
    public String getFormat() { return "RAW"; }
    public byte[] getEncoded() { return null; }
}
