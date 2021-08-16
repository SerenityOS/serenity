/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4508341 7055362
 * @library ../../../java/security/testlibrary
 * @summary Test the error conditions of
 * EncryptedPrivateKeyInfo.getKeySpec(...) methods.
 * @author Valerie Peng
 */
import java.security.*;
import java.util.Arrays;
import java.util.Vector;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class GetKeySpecException {
    private static final String cipherAlg = "PBEWithMD5AndDES";
    private static final char[] passwd = { 'p','a','s','s','w','d' };
    private static SecretKey cipherKey;
    private static Cipher cipher = null;
    private static byte[] encryptedData = null;
    private static Provider sunjce = null;
    private static final SecretKey INVALID_KEY =
        new SecretKeySpec(new byte[8], "DES");
    private static AlgorithmParameters BAD_PARAMS;
    private static AlgorithmParameters GOOD_PARAMS;

    static {
        try {
            sunjce = Security.getProvider("SunJCE");
            PBEParameterSpec badParamSpec =
                new PBEParameterSpec(new byte[10], 10);
            BAD_PARAMS = AlgorithmParameters.getInstance(cipherAlg, sunjce);
            BAD_PARAMS.init(badParamSpec);
            PBEParameterSpec goodParamSpec =
                new PBEParameterSpec(new byte[8], 1024);
            GOOD_PARAMS = AlgorithmParameters.getInstance(cipherAlg, sunjce);
            GOOD_PARAMS.init(goodParamSpec);
            PBEKeySpec keySpec = new PBEKeySpec(passwd);
            SecretKeyFactory skf =
                SecretKeyFactory.getInstance(cipherAlg, "SunJCE");
            cipherKey = skf.generateSecret(keySpec);
        } catch (Exception ex) {
            // should never happen
            BAD_PARAMS = null;
            GOOD_PARAMS = null;
        }
    }

    private static void throwException(String msg) throws Exception {
        throw new Exception(msg);
    }

    private static Provider[] removeProviders(String cipherAlg) {
        Vector providers = new Vector();
        boolean done = false;
        while (!done) {
            try {
                Cipher c = Cipher.getInstance(cipherAlg);
                Provider p = c.getProvider();
                providers.add(p);
                Security.removeProvider(p.getName());
            } catch (NoSuchAlgorithmException nsae) {
                done = true;
            } catch (NoSuchPaddingException nspe) {
                // should never happen
            }
        }
        return (Provider[]) (providers.toArray(new Provider[0]));
    }

    private static void addProviders(Provider[] provs) {
        for (int i=0; i<provs.length; i++) {
            Security.addProvider(provs[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        ProvidersSnapshot snapshot = ProvidersSnapshot.create();
        try {
            main0(args);
        } finally {
            snapshot.restore();
        }
    }

    public static void main0(String[] args) throws Exception {
        if ((GOOD_PARAMS == null) || (BAD_PARAMS == null)) {
            throw new Exception("Static parameter generation failed");
        }
        // use random data
        byte[] encryptedData = new byte[30];
        encryptedData[20] = (byte) 8;

        PKCS8EncodedKeySpec pkcs8Spec = null;

        // generate encrypted data and EncryptedPrivateKeyInfo objects
        EncryptedPrivateKeyInfo epki =
            new EncryptedPrivateKeyInfo(GOOD_PARAMS, encryptedData);
        EncryptedPrivateKeyInfo epkiBad =
            new EncryptedPrivateKeyInfo(BAD_PARAMS, encryptedData);

        // TEST#1: getKeySpec(Cipher)
        System.out.println("Testing getKeySpec(Cipher)...");
        try {
            pkcs8Spec = epki.getKeySpec((Cipher) null);
            throwException("Should throw NPE for null Cipher!");
        } catch (NullPointerException npe) {
            System.out.println("Expected NPE thrown");
        }

        // TEST#2: getKeySpec(Key)
        System.out.println("Testing getKeySpec(Key)...");
        try {
            pkcs8Spec = epki.getKeySpec((Key) null);
            throwException("Should throw NPE for null Key!");
        } catch (NullPointerException npe) {
            System.out.println("Expected NPE thrown");
        }
        try {
            pkcs8Spec = epki.getKeySpec(INVALID_KEY);
            throwException("Should throw IKE for invalid Key!");
        } catch (InvalidKeyException ikse) {
            System.out.println("Expected IKE thrown");
        }
        try {
            pkcs8Spec = epkiBad.getKeySpec(cipherKey);
            throwException("Should throw IKE for corrupted epki!");
        } catch (InvalidKeyException ike) {
            System.out.println("Expected IKE thrown");
        }
        Provider[] removedProvs = null;
        try {
            removedProvs = removeProviders(cipherAlg);
            pkcs8Spec = epki.getKeySpec(cipherKey);
            throwException("Should throw NSAE if no matching impl!");
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Expected NSAE thrown");
            addProviders(removedProvs);
        }
        // TEST#3: getKeySpec(Key, String)
        System.out.println("Testing getKeySpec(Key, String)...");
        try {
            pkcs8Spec = epki.getKeySpec(null, "SunJCE");
            throwException("Should throw NPE for null Key!");
        } catch (NullPointerException npe) {
            System.out.println("Expected NPE thrown");
        }
        try {
            pkcs8Spec = epki.getKeySpec(cipherKey, (String)null);
            throwException("Should throw NPE for null String!");
        } catch (NullPointerException npe) {
            System.out.println("Expected NPE thrown");
        }
        try {
            pkcs8Spec = epki.getKeySpec(INVALID_KEY, "SunJCE");
            throwException("Should throw IKE for invalid Key!");
        } catch (InvalidKeyException ikse) {
            System.out.println("Expected IKE thrown");
        }
        try {
            pkcs8Spec = epkiBad.getKeySpec(cipherKey, "SunJCE");
            throwException("Should throw IKE for corrupted epki!");
        } catch (InvalidKeyException ike) {
            System.out.println("Expected IKE thrown");
        }
        try {
            pkcs8Spec = epki.getKeySpec(cipherKey, "SUN");
            throwException("Should throw NSAE for provider without " +
                           "matching implementation!");
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Expected NSAE thrown");
        }
        try {
            Security.removeProvider("SunJCE");
            pkcs8Spec = epki.getKeySpec(cipherKey, "SunJCE");
            throwException("Should throw NSPE for unconfigured provider!");
        } catch (NoSuchProviderException nspe) {
            System.out.println("Expected NSPE thrown");
            Security.addProvider(sunjce);
        }
        // TEST#4: getKeySpec(Key, Provider)
        System.out.println("Testing getKeySpec(Key, Provider)...");
        try {
            pkcs8Spec = epki.getKeySpec(null, sunjce);
            throwException("Should throw NPE for null Key!");
        } catch (NullPointerException npe) {
            System.out.println("Expected NPE thrown");
        }
        try {
            pkcs8Spec = epki.getKeySpec(cipherKey, (Provider)null);
            throwException("Should throw NPE for null Provider!");
        } catch (NullPointerException npe) {
            System.out.println("Expected NPE thrown");
        }
        try {
            pkcs8Spec = epki.getKeySpec(INVALID_KEY, sunjce);
            throwException("Should throw IKE for invalid Key!");
        } catch (InvalidKeyException ikse) {
            System.out.println("Expected IKE thrown");
        }
        try {
            pkcs8Spec = epkiBad.getKeySpec(cipherKey, sunjce);
            throwException("Should throw IKE for corrupted epki!");
        } catch (InvalidKeyException ike) {
            System.out.println("Expected IKE thrown");
        }
        System.out.println("All Tests Passed");
    }
}
