/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8149802
 * @summary Ensure that Signature objects are reset after verification errored out.
 */
import java.util.Arrays;
import java.security.*;

public class ResetAfterException {

    public static void main(String[] args) throws Exception {

        byte[] data = "data to be signed".getBytes();
        byte[] shortBuffer = new byte[2];

        Provider[] provs = Security.getProviders();
        boolean failed = false;

        for (Provider p : provs) {
            Signature sig;
            try {
                sig = Signature.getInstance("SHA256withRSA", p);
            } catch (NoSuchAlgorithmException nsae) {
                // no support, skip
                continue;
            }

            boolean res = true;
            System.out.println("Testing Provider: " + p.getName());
            KeyPairGenerator keyGen = null;
            try {
                // It's possible that some provider, e.g. SunMSCAPI,
                // doesn't work well with keys from other providers
                // so we use the same provider to generate key first
                keyGen = KeyPairGenerator.getInstance("RSA", p);
            } catch (NoSuchAlgorithmException nsae) {
                keyGen = KeyPairGenerator.getInstance("RSA");
            }
            if (keyGen == null) {
                throw new RuntimeException("Error: No support for RSA KeyPairGenerator");
            }
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.generateKeyPair();

            sig.initSign(keyPair.getPrivate());
            sig.update(data);
            byte[] signature = sig.sign();
            // First check signing
            try {
                sig.update(data);
                // sign with short output buffer to cause exception
                int len = sig.sign(shortBuffer, 0, shortBuffer.length);
                System.out.println("FAIL: Should throw SE with short buffer");
                res = false;
            } catch (SignatureException e) {
                // expected exception; ignore
                System.out.println("Expected Ex for short output buffer: " + e);
            }
            // Signature object should reset after a failed generation
            sig.update(data);
            byte[] signature2 = sig.sign();
            if (!Arrays.equals(signature, signature2)) {
                System.out.println("FAIL: Generated different signature");
                res = false;
            } else {
                System.out.println("Generated same signature");
            }

            // Now, check signature verification
            sig.initVerify(keyPair.getPublic());
            sig.update(data);
            try {
                // first verify with valid signature bytes
                res = sig.verify(signature);
            } catch (SignatureException e) {
                System.out.println("FAIL: Valid signature rejected");
                e.printStackTrace();
                res = false;
            }

            try {
                sig.update(data);
                // verify with short signaure to cause exception
                if (sig.verify(shortBuffer)) {
                    System.out.println("FAIL: Invalid signature verified");
                    res = false;
                } else {
                    System.out.println("Invalid signature rejected");
                }
            } catch (SignatureException e) {
                // expected exception; ignore
                System.out.println("Expected Ex for short output buffer: " + e);
            }
            // Signature object should reset after an a failed verification
            sig.update(data);
            try {
                // verify with valid signature bytes again
                res = sig.verify(signature);
                if (!res) {
                    System.out.println("FAIL: Valid signature is rejected");
                } else {
                    System.out.println("Valid signature is accepted");
                }
            } catch (GeneralSecurityException e) {
                System.out.println("FAIL: Valid signature is rejected");
                e.printStackTrace();
                res = false;
            }
            failed |= !res;
        }
        if (failed) {
            throw new RuntimeException("One or more test failed");
        } else {
            System.out.println("Test Passed");
        }
   }
}
