/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080462
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main TestCICOWithGCMAndAAD
 * @summary Test CipherInputStream/OutputStream with AES GCM mode with AAD.
 * @key randomness
 */
import java.io.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class TestCICOWithGCMAndAAD extends PKCS11Test {
    public static void main(String[] args) throws Exception {
        main(new TestCICOWithGCMAndAAD(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        test("GCM", p);
//        test("CCM", p);
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
        SecretKey key = new SecretKeySpec(new byte[16], "AES");

        //Do initialization of the plainText
        byte[] plainText = new byte[700];
        Random rdm = new Random();
        rdm.nextBytes(plainText);

        byte[] aad = new byte[128];
        rdm.nextBytes(aad);
        byte[] aad2 = aad.clone();
        aad2[50]++;

        Cipher encCipher = Cipher.getInstance("AES/GCM/NoPadding", p);
        encCipher.init(Cipher.ENCRYPT_MODE, key);
        encCipher.updateAAD(aad);
        Cipher decCipher = Cipher.getInstance("AES/GCM/NoPadding", p);
        decCipher.init(Cipher.DECRYPT_MODE, key, encCipher.getParameters());
        decCipher.updateAAD(aad);

        byte[] recovered = test(encCipher, decCipher, plainText);
        if (!Arrays.equals(plainText, recovered)) {
            throw new Exception("sameAAD: diff check failed!");
        } else System.out.println("sameAAD: passed");

        encCipher.init(Cipher.ENCRYPT_MODE, key);
        encCipher.updateAAD(aad2);
        recovered = test(encCipher, decCipher, plainText);
        if (recovered != null && recovered.length != 0) {
            throw new Exception("diffAAD: no data should be returned!");
        } else System.out.println("diffAAD: passed");
   }

   private static byte[] test(Cipher encCipher, Cipher decCipher, byte[] plainText)
            throws Exception {
        //init cipher streams
        ByteArrayInputStream baInput = new ByteArrayInputStream(plainText);
        CipherInputStream ciInput = new CipherInputStream(baInput, encCipher);
        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
        CipherOutputStream ciOutput = new CipherOutputStream(baOutput, decCipher);

        //do test
        byte[] buffer = new byte[200];
        int len = ciInput.read(buffer);
        System.out.println("read " + len + " bytes from input buffer");

        while (len != -1) {
            ciOutput.write(buffer, 0, len);
            System.out.println("wite " + len + " bytes to output buffer");
            len = ciInput.read(buffer);
            if (len != -1) {
                System.out.println("read " + len + " bytes from input buffer");
            } else {
                System.out.println("finished reading");
            }
        }

        ciOutput.flush();
        ciInput.close();
        ciOutput.close();

        return baOutput.toByteArray();
    }
}
