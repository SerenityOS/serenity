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
 * @bug 4925866
 * @summary ensures various salt lengths can be used for
 * HmacPBESHA1.
 * @author Valerie Peng
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.PBEKey;

public class HmacSaltLengths {

    private static final String[] ALGOS = {
        "HmacPBESHA1",
        "PBEWithHmacSHA1",
        "PBEWithHmacSHA224",
        "PBEWithHmacSHA256",
        "PBEWithHmacSHA384",
        "PBEWithHmacSHA512"
    };

    private static void runTest(String alg, byte[] plaintext,
                                char[] password, Provider p)
        throws Exception {
        Mac mac = Mac.getInstance(alg, p);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBE", p);
        SecretKey key = keyFac.generateSecret(pbeKeySpec);

        System.out.println("testing parameters with 4-byte salt...");
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec
            (new byte[4], 1024);
        try {
            mac.init(key, pbeParamSpec);
            throw new Exception("ERROR: should throw IAPE for short salts");
        } catch (InvalidAlgorithmParameterException iape) {
            // expected; do nothing
        }

        System.out.println("testing parameters with 8-byte salt...");
        pbeParamSpec = new PBEParameterSpec(new byte[8], 1024);
        mac.init(key, pbeParamSpec);
        mac.doFinal(plaintext);

        System.out.println("testing parameters with 20-byte salt...");
        pbeParamSpec = new PBEParameterSpec(new byte[20], 1024);
        mac.init(key, pbeParamSpec);
        mac.doFinal(plaintext);

        System.out.println("testing parameters with 30-byte salt...");
        pbeParamSpec = new PBEParameterSpec(new byte[30], 1024);
        mac.init(key, pbeParamSpec);
        mac.doFinal(plaintext);

        System.out.println("passed: " + alg);
    }

    public static void main(String[] argv) throws Exception {
        byte[] input = new byte[1024];
        new SecureRandom().nextBytes(input);
        char[] PASSWD = { 'p','a','s','s','w','o','r','d' };
        long start = System.currentTimeMillis();
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        for (String algo : ALGOS) {
            runTest(algo, input, PASSWD, p);
        }
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
    public char[] getPassword() { return passwd; }
    public byte[] getSalt() { return salt; }
    public int getIterationCount() { return iCount; }
    public String getAlgorithm() { return "PBE"; }
    public String getFormat() { return "RAW"; }
    public byte[] getEncoded() { return null; }
}
