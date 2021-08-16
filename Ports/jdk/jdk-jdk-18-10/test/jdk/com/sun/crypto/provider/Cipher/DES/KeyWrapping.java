/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 0000000
 * @summary KeyWrapping
 * @author Jan Luehe
 */
import javax.crypto.*;
import java.security.*;

public class KeyWrapping {

    public static void main(String[] args) throws Exception {
        Cipher c1 = Cipher.getInstance("DES", "SunJCE");
        Cipher c2 = Cipher.getInstance("DES");

        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        keyGen.init(56);

        // Generate two DES keys: sKey and sessionKey
        SecretKey sKey = keyGen.generateKey();
        SecretKey sessionKey = keyGen.generateKey();

        // wrap and unwrap the session key
        // make sure the unwrapped session key
        // can decrypt a message encrypted
        // with the session key
        c1.init(Cipher.WRAP_MODE, sKey);

        byte[] wrappedKey = c1.wrap(sessionKey);

        c1.init(Cipher.UNWRAP_MODE, sKey);

        SecretKey unwrappedSessionKey =
                              (SecretKey)c1.unwrap(wrappedKey, "DES",
                                                  Cipher.SECRET_KEY);

        c2.init(Cipher.ENCRYPT_MODE, unwrappedSessionKey);

        String msg = "Hello";

        byte[] cipherText = c2.doFinal(msg.getBytes());

        c2.init(Cipher.DECRYPT_MODE, unwrappedSessionKey);

        byte[] clearText = c2.doFinal(cipherText);

        if (!msg.equals(new String(clearText)))
            throw new Exception("The unwrapped session key is corrupted.");

        KeyPairGenerator kpairGen = KeyPairGenerator.getInstance("DSA");
        kpairGen.initialize(1024);

        KeyPair kpair = kpairGen.genKeyPair();

        PublicKey pub = kpair.getPublic();
        PrivateKey pri = kpair.getPrivate();

        c1.init(Cipher.WRAP_MODE, sKey);

        byte[] wrappedPub = c1.wrap(pub);
        byte[] wrappedPri = c1.wrap(pri);

        c1.init(Cipher.UNWRAP_MODE, sKey);

        Key unwrappedPub = c1.unwrap(wrappedPub, "DSA",
                                           Cipher.PUBLIC_KEY);
        Key unwrappedPri = c1.unwrap(wrappedPri, "DSA",
                                            Cipher.PRIVATE_KEY);
    }
}
