/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6232513
 * @summary RMI interoperability issue with DSAPublicKey obj between
 *              JDK1.4 & JDK1.5
 * @run main/othervm/java.security.policy=SerialDSAPubKey.policy -Dsun.security.key.serial.interop=true -Dsun.security.pkcs11.enable-solaris=false SerialDSAPubKey
 */

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class SerialDSAPubKey {

    // provider
    private static final String SUN = "SUN";

    public static void main(String[] args) throws Exception {

        // This test generates sun.security.DSAPublicKey
        // (not sun.security.DSAPublicKeyImpl, as Serial.java does)

        // generate DSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA", SUN);
        kpg.initialize(512);
        KeyPair dsaKp = kpg.genKeyPair();

        // serialize DSA key pair
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(dsaKp);
        oos.close();

        // deserialize DSA key pair
        ObjectInputStream ois = new ObjectInputStream
                        (new ByteArrayInputStream(baos.toByteArray()));
        KeyPair dsaKp2 = (KeyPair)ois.readObject();
        ois.close();

        if (!dsaKp2.getPublic().equals(dsaKp.getPublic()) ||
            !dsaKp2.getPrivate().equals(dsaKp.getPrivate())) {
            throw new SecurityException("DSA test failed");
        }
    }
}
