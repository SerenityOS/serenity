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

/**
 * @test
 * @bug 5028661
 * @summary Test the error conditions of Cipher initialized
 * with wrong mode with EncryptedPrivateKeyInfo.getKeySpec
 * (Cipher) method.
 * @author Valerie Peng
 */
import java.security.*;
import java.util.Arrays;
import java.util.Vector;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.*;

public class GetKeySpecException2 {
    private static final String cipherAlg = "PBEWithMD5AndDES";
    private static final char[] passwd = { 'p','a','s','s','w','d' };

    public static void main(String[] argv) throws Exception {

        // use random data
        byte[] encryptedData = new byte[30];
        encryptedData[20] = (byte) 8;

        // generate encrypted data and EncryptedPrivateKeyInfo objects
        EncryptedPrivateKeyInfo epki =
            new EncryptedPrivateKeyInfo(cipherAlg, encryptedData);

        // TEST#1: getKeySpec(Cipher) with Cipher in an illegal state,
        // i.e. WRAP_MODE, UNWRAP_MODE.
        System.out.println("Testing getKeySpec(Cipher) with WRAP_MODE...");
        Cipher c = Cipher.getInstance(cipherAlg, "SunJCE");
        MyPBEKey key = new MyPBEKey(passwd);
        c.init(Cipher.WRAP_MODE, key);
        try {
            epki.getKeySpec(c);
            throw new Exception("Should throw InvalidKeyException");
        } catch (InvalidKeySpecException npe) {
            System.out.println("Expected IKE thrown");
        }
        AlgorithmParameters params = c.getParameters();
        System.out.println("Testing getKeySpec(Cipher) with UNWRAP_MODE...");
        c.init(Cipher.UNWRAP_MODE, key, params);
        try {
            epki.getKeySpec(c);
            throw new Exception("Should throw InvalidKeyException");
        } catch (InvalidKeySpecException npe) {
            System.out.println("Expected IKE thrown");
        }
        System.out.println("All Tests Passed");
    }
}

class MyPBEKey implements PBEKey {

    private char[] password = null;

    MyPBEKey(char[] password) {
        this.password = (char[]) password.clone();
    }
    public int getIterationCount() {
        return 0;
    }
    public char[] getPassword() {
        return (char[]) password.clone();
    }
    public byte[] getSalt() {
        return null;
    }
    public String getAlgorithm() {
        return "PBE";
    }
    public String getFormat() {
        return "RAW";
    }
    public byte[] getEncoded() {
        return new byte[8];
    }
}
