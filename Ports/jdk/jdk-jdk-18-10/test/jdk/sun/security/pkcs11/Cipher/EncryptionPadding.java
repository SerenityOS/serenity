/*
 * Copyright (c) 2021, Red Hat, Inc.
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
 * @bug 8261355
 * @library /test/lib ..
 * @run main/othervm EncryptionPadding
 */

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.Provider;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionPadding extends PKCS11Test {

    private static String transformation = "AES/ECB/PKCS5Padding";
    private static Key key = new SecretKeySpec(new byte[16], "AES");

    public static void main(String[] args) throws Exception {
        main(new EncryptionPadding(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        testWithInputSize(p, 1);
        testWithInputSize(p, 15);
        testWithInputSize(p, 16);
        testWithInputSize(p, 17);
        System.out.println("TEST PASS - OK");
    }

    private static void testWithInputSize(Provider p, int inputSize)
            throws Exception {
        testWithInputSize(p, inputSize, false);
        testWithInputSize(p, inputSize, true);
    }

    private static void testWithInputSize(Provider p, int inputSize,
            boolean isByteBuffer) throws Exception {
        byte[] plainText = new byte[inputSize];
        Arrays.fill(plainText, (byte)(inputSize & 0xFF));
        ByteBuffer cipherText =
                ByteBuffer.allocate(((inputSize / 16 ) + 1) * 16);
        byte[] tmp;

        Cipher sunPKCS11cipher = Cipher.getInstance(transformation, p);
        sunPKCS11cipher.init(Cipher.ENCRYPT_MODE, key);
        for (int i = 0; i < ((inputSize - 1) / 16) + 1; i++) {
            int updateLength = Math.min(inputSize - (16 * i), 16);
            if (!isByteBuffer) {
                tmp = sunPKCS11cipher.update(plainText, i * 16,
                        updateLength);
                if (tmp != null) {
                    cipherText.put(tmp);
                }
            } else {
                ByteBuffer bb = ByteBuffer.allocate(updateLength);
                bb.put(plainText, i * 16, updateLength);
                bb.flip();
                sunPKCS11cipher.update(bb, cipherText);
            }
        }
        if (!isByteBuffer) {
            tmp = sunPKCS11cipher.doFinal();
            if (tmp != null) {
                cipherText.put(tmp);
            }
        } else {
            sunPKCS11cipher.doFinal(ByteBuffer.allocate(0), cipherText);
        }

        Cipher sunJCECipher = Cipher.getInstance(transformation, "SunJCE");
        sunJCECipher.init(Cipher.DECRYPT_MODE, key);
        byte[] sunJCEPlain = sunJCECipher.doFinal(cipherText.array());

        if (!Arrays.equals(plainText, sunJCEPlain)) {
            throw new Exception("Cross-provider cipher test failed.");
        }
    }
}
