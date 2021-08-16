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
 * @bug 8151149
 * @modules java.base/javax.crypto:open
 *          java.base/com.sun.crypto.provider:+open
 */

import java.lang.reflect.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import com.sun.crypto.provider.*;

public class CheckPBEKeySize {

    private static final String ALGO = "PBEWithSHA1AndDESede";
    private static final int KEYSIZE = 112; // Triple DES effective key size

    public static final void main(String[] args) throws Exception {

        // Generate a PBE key
        SecretKeyFactory skFac = SecretKeyFactory.getInstance("PBE");
        SecretKey skey =
            skFac.generateSecret(new PBEKeySpec("test123".toCharArray()));

        // Initialize the PBE cipher
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, skey);

        // Permit access to the Cipher.spi field (a CipherSpi object)
        Field spi = Cipher.class.getDeclaredField("spi");
        spi.setAccessible(true);
        Object value = spi.get(cipher);

        // Permit access to the CipherSpi.engineGetKeySize method
        Method engineGetKeySize =
            PKCS12PBECipherCore$PBEWithSHA1AndDESede.class
                .getDeclaredMethod("engineGetKeySize", Key.class);
        engineGetKeySize.setAccessible(true);

        // Check the key size
        int keySize = (int) engineGetKeySize.invoke(value, skey);
        if (keySize == KEYSIZE) {
            System.out.println(ALGO + ".engineGetKeySize returns " + keySize +
                " bits, as expected");
            System.out.println("OK");
        } else {
            throw new Exception("ERROR: " + ALGO + " key size is incorrect");
        }
    }
}
