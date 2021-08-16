/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080462 8229243
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main TestGCMKeyAndIvCheck
 * @summary Ensure that same key+iv can't be repeated used for encryption.
 */


import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.*;

import java.util.*;

public class TestGCMKeyAndIvCheck extends PKCS11Test {

    private static final byte[] AAD = new byte[5];
    private static final byte[] PT = new byte[18];

    public static void main(String[] args) throws Exception {
        main(new TestGCMKeyAndIvCheck(), args);
    }

    private static void checkISE(Cipher c) throws Exception {
        // Subsequent encryptions should fail
        try {
            c.updateAAD(AAD);
            throw new Exception("Should throw ISE for updateAAD()");
        } catch (IllegalStateException ise) {
            // expected
        }

        try {
            c.update(PT);
            throw new Exception("Should throw ISE for update()");
        } catch (IllegalStateException ise) {
            // expected
        }
        try {
            c.doFinal(PT);
            throw new Exception("Should throw ISE for doFinal()");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    public void test(String mode, Provider p) throws Exception {
        Cipher c;
        try {
            String transformation = "AES/" + mode + "/NoPadding";
            c = Cipher.getInstance(transformation, p);
        } catch (GeneralSecurityException e) {
            System.out.println("Skip testing " + p.getName() +
                    ", no support for " + mode);
            return;
        }
        System.out.println("Testing against " + p.getName());
        SecretKey key = new SecretKeySpec(new byte[16], "AES");
        // First try parameter-less init.
        c.init(Cipher.ENCRYPT_MODE, key);
        c.updateAAD(AAD);
        byte[] ctPlusTag = c.doFinal(PT);

        // subsequent encryption should fail unless re-init w/ different key+iv
        checkISE(c);

        // Validate the retrieved parameters against the IV and tag length.
        AlgorithmParameters params = c.getParameters();
        if (params == null) {
            throw new Exception("getParameters() should not return null");
        }
        byte[] iv = null;
        int tagLength = 0; // in bits
        if (mode.equalsIgnoreCase("GCM")) {
            GCMParameterSpec spec = params.getParameterSpec(GCMParameterSpec.class);
            tagLength = spec.getTLen();
            iv = spec.getIV();
        } else {
            throw new RuntimeException("Error: Unsupported mode: " + mode);
        }
        if (tagLength != (ctPlusTag.length - PT.length)*8) {
            throw new Exception("Parameters contains incorrect TLen value");
        }
        if (!Arrays.equals(iv, c.getIV())) {
            throw new Exception("Parameters contains incorrect IV value");
        }

        c.init(Cipher.DECRYPT_MODE, key, params);
        c.updateAAD(AAD);
        byte[] recovered = c.doFinal(ctPlusTag);
        if (!Arrays.equals(recovered, PT)) {
            throw new Exception("Decryption result mismatch");
        }

        // Now try to encrypt again using the same key+iv; should fail also
        try {
            c.init(Cipher.ENCRYPT_MODE, key, params);
            throw new Exception("Should throw exception when same key+iv is used");
        } catch (InvalidAlgorithmParameterException iape) {
            // expected
            System.out.println("Expected IAPE thrown");
        }

        // Now try to encrypt again using parameter-less init; should work
        c.init(Cipher.ENCRYPT_MODE, key);
        c.doFinal(PT);

        // make sure a different iv is used
        byte[] ivNew = c.getIV();
        if (Arrays.equals(iv, ivNew)) {
            throw new Exception("IV should be different now");
        }

        // Now try to encrypt again using a different parameter; should work
        AlgorithmParameterSpec spec2 = new GCMParameterSpec(128,
            "Solaris PKCS11 lib does not allow all-zero IV".getBytes());
        c.init(Cipher.ENCRYPT_MODE, key, spec2);
        c.updateAAD(AAD);
        c.doFinal(PT);
        // subsequent encryption should fail unless re-init w/ different key+iv
        checkISE(c);

        // Now try decryption twice in a row; no re-init required and
        // same parameters is used.
        c.init(Cipher.DECRYPT_MODE, key, params);
        c.updateAAD(AAD);
        recovered = c.doFinal(ctPlusTag);

        c.updateAAD(AAD);
        recovered = c.doFinal(ctPlusTag);
        if (!Arrays.equals(recovered, PT)) {
            throw new Exception("Decryption result mismatch");
        }

        // Now try decryption again and re-init using the same parameters
        c.init(Cipher.DECRYPT_MODE, key, params);
        c.updateAAD(AAD);
        recovered = c.doFinal(ctPlusTag);

        // init to decrypt w/o parameters; should fail with IKE as
        // javadoc specified
        try {
            c.init(Cipher.DECRYPT_MODE, key);
            throw new Exception("Should throw IKE for dec w/o params");
        } catch (InvalidKeyException ike) {
            // expected
        }

        // Lastly, try encryption AND decryption w/ wrong type of parameters,
        // e.g. IvParameterSpec
        try {
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            throw new Exception("Should throw IAPE");
        } catch (InvalidAlgorithmParameterException iape) {
            // expected
        }
        try {
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            throw new Exception("Should throw IAPE");
        } catch (InvalidAlgorithmParameterException iape) {
            // expected
        }

        System.out.println("Test Passed!");
    }

    @Override
    public void main(Provider p) throws Exception {
        test("GCM", p);
    }
}

