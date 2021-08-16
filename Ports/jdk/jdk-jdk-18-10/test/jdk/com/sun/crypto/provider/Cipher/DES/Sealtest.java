/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 0000000 7055362
 * @summary Sealtest
 * @author Jan Luehe
 */
import java.io.*;
import java.security.*;
import javax.crypto.*;

public class Sealtest {

    public static void main(String[] args) throws Exception {

        // create DSA keypair
        KeyPairGenerator kpgen = KeyPairGenerator.getInstance("DSA");
        kpgen.initialize(512);
        KeyPair kp = kpgen.generateKeyPair();

        // create DES key
        KeyGenerator kg = KeyGenerator.getInstance("DES", "SunJCE");
        SecretKey skey = kg.generateKey();

        // create cipher
        Cipher c = Cipher.getInstance("DES/CFB16/PKCS5Padding", "SunJCE");
        c.init(Cipher.ENCRYPT_MODE, skey);

        // seal the DSA private key
        SealedObject sealed = new SealedObject(kp.getPrivate(), c);

        // serialize
        try (FileOutputStream fos = new FileOutputStream("sealed");
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(sealed);
        }

        // deserialize
        try (FileInputStream fis = new FileInputStream("sealed");
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            sealed = (SealedObject)ois.readObject();
        }

        System.out.println(sealed.getAlgorithm());

        // compare unsealed private key with original
        PrivateKey priv = (PrivateKey)sealed.getObject(skey);
        if (!priv.equals(kp.getPrivate()))
            throw new Exception("TEST FAILED");

        System.out.println("TEST SUCCEEDED");
    }
}
