/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048604
 * @summary This test checks boundary conditions for testing
 *          ShortBufferException.
 */
import static java.lang.System.out;

import java.security.AlgorithmParameters;
import java.security.Provider;
import java.security.Security;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TextPKCS5PaddingTest {
    /**
     * Test plain text.
     */
    private static final byte[] PLAIN_TEXT = {
        0b10001, 0b10001, 0b10001, 0b10001,
        0b10001, 0b10001, 0b11,    0b11
    };

    public static void main(String[] args) throws Exception {
        Provider provider = Security.getProvider("SunJCE");
        if (provider == null) {
            throw new RuntimeException("SunJCE provider not exist");
        }
        // generate no-padding cipher with secret key
        Cipher c = Cipher.getInstance("DES/CBC/NoPadding", provider);
        KeyGenerator kgen = KeyGenerator.getInstance("DES", provider);
        SecretKey skey = kgen.generateKey();
        // this is the improperly padded plaintext

        c.init(Cipher.ENCRYPT_MODE, skey);
        // encrypt plaintext
        byte[] cipher = c.doFinal(PLAIN_TEXT);
        AlgorithmParameters params = c.getParameters();
        // generate cipher that enforces PKCS5 padding
        c = Cipher.getInstance("DES/CBC/PKCS5Padding", provider);
        c.init(Cipher.DECRYPT_MODE, skey, params);
        try {
            c.doFinal(cipher);
            throw new RuntimeException(
                    "ERROR: Expected BadPaddingException not thrown");
        } catch (BadPaddingException expected) {
            out.println("Expected BadPaddingException thrown");
        }

    }
}
