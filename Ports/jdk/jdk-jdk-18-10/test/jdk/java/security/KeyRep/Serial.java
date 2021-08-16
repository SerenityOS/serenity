/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4532506 4999599
 * @summary Serializing KeyPair on one VM (Sun),
 *      and Deserializing on another (IBM) fails
 * @run main/othervm/java.security.policy=Serial.policy Serial
 */

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Serial {

    // providers
    private static final String SUN = "SUN";
    private static final String RSA = "SunRsaSign";
    private static final String JCE = "SunJCE";

    public static void main(String[] args) throws Exception {

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

        // generate RSA key pair
        kpg = KeyPairGenerator.getInstance("RSA", RSA);
        kpg.initialize(512);
        KeyPair rsaKp = kpg.genKeyPair();

        // serialize RSA key pair
        baos.reset();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(rsaKp);
        oos.close();

        // deserialize RSA key pair
        ois = new ObjectInputStream
                        (new ByteArrayInputStream(baos.toByteArray()));
        KeyPair rsaKp2 = (KeyPair)ois.readObject();
        ois.close();

        if (!rsaKp2.getPublic().equals(rsaKp.getPublic()) ||
            !rsaKp2.getPrivate().equals(rsaKp.getPrivate())) {
            throw new SecurityException("RSA test failed");
        }

        // generate DH key pair
        kpg = KeyPairGenerator.getInstance("DiffieHellman", JCE);
        kpg.initialize(new DHParameterSpec(skip1024Modulus, skip1024Base));
        KeyPair dhKp = kpg.genKeyPair();

        // serialize DH key pair
        baos.reset();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(dhKp);
        oos.close();

        // deserialize DH key pair
        ois = new ObjectInputStream
                        (new ByteArrayInputStream(baos.toByteArray()));
        KeyPair dhKp2 = (KeyPair)ois.readObject();
        ois.close();

        if (!dhKp2.getPublic().equals(dhKp.getPublic()) ||
            !dhKp2.getPrivate().equals(dhKp.getPrivate())) {
            throw new SecurityException("DH test failed");
        }

        // generate RC5 key
        SecretKeySpec rc5Key = new SecretKeySpec(new byte[128], "RC5");

        // serialize RC5 key
        baos.reset();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(rc5Key);
        oos.close();

        // deserialize RC5 key
        ois = new ObjectInputStream
                        (new ByteArrayInputStream(baos.toByteArray()));
        SecretKey rc5Key2 = (SecretKey)ois.readObject();
        ois.close();

        if (!rc5Key.equals(rc5Key2)) {
            throw new SecurityException("RC5 test failed");
        }

        // generate PBE key

        // Salt
        byte[] salt = {
                (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
                (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
        };

        // Iteration count
        int count = 20;

        // Create PBE parameter set
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);

        char[] password = new char[] {'f', 'o', 'o'};
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKeyFactory keyFac =
                        SecretKeyFactory.getInstance("PBEWithMD5AndDES", JCE);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        // serialize PBE key
        baos.reset();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(pbeKey);
        oos.close();

        // deserialize PBE key
        ois = new ObjectInputStream
                        (new ByteArrayInputStream(baos.toByteArray()));
        SecretKey pbeKey2 = (SecretKey)ois.readObject();
        ois.close();

        if (!pbeKey.equals(pbeKey2)) {
            throw new SecurityException("PBE test failed");
        }

        checkKey("AES", 128);
        checkKey("Blowfish", -1);
        checkKey("DES", 56);
        checkKey("DESede", 168);
        checkKey("HmacMD5", -1);
        checkKey("HmacSHA1", -1);
    }

    private static void checkKey(String algorithm, int size) throws Exception {
        // generate key
        KeyGenerator kg = KeyGenerator.getInstance(algorithm, JCE);
        if (size > 0) {
            kg.init(size);
        }
        SecretKey key = kg.generateKey();

        // serialize key
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(key);
        oos.close();

        // deserialize key
        ObjectInputStream ois = new ObjectInputStream
                                (new ByteArrayInputStream(baos.toByteArray()));
        SecretKey key2 = (SecretKey)ois.readObject();
        ois.close();

        if (!key.equals(key2)) {
            throw new SecurityException(algorithm + " test failed");
        }
    }

    // The 1024 bit Diffie-Hellman modulus values used by SKIP
    private static final byte skip1024ModulusBytes[] = {
        (byte)0xF4, (byte)0x88, (byte)0xFD, (byte)0x58,
        (byte)0x4E, (byte)0x49, (byte)0xDB, (byte)0xCD,
        (byte)0x20, (byte)0xB4, (byte)0x9D, (byte)0xE4,
        (byte)0x91, (byte)0x07, (byte)0x36, (byte)0x6B,
        (byte)0x33, (byte)0x6C, (byte)0x38, (byte)0x0D,
        (byte)0x45, (byte)0x1D, (byte)0x0F, (byte)0x7C,
        (byte)0x88, (byte)0xB3, (byte)0x1C, (byte)0x7C,
        (byte)0x5B, (byte)0x2D, (byte)0x8E, (byte)0xF6,
        (byte)0xF3, (byte)0xC9, (byte)0x23, (byte)0xC0,
        (byte)0x43, (byte)0xF0, (byte)0xA5, (byte)0x5B,
        (byte)0x18, (byte)0x8D, (byte)0x8E, (byte)0xBB,
        (byte)0x55, (byte)0x8C, (byte)0xB8, (byte)0x5D,
        (byte)0x38, (byte)0xD3, (byte)0x34, (byte)0xFD,
        (byte)0x7C, (byte)0x17, (byte)0x57, (byte)0x43,
        (byte)0xA3, (byte)0x1D, (byte)0x18, (byte)0x6C,
        (byte)0xDE, (byte)0x33, (byte)0x21, (byte)0x2C,
        (byte)0xB5, (byte)0x2A, (byte)0xFF, (byte)0x3C,
        (byte)0xE1, (byte)0xB1, (byte)0x29, (byte)0x40,
        (byte)0x18, (byte)0x11, (byte)0x8D, (byte)0x7C,
        (byte)0x84, (byte)0xA7, (byte)0x0A, (byte)0x72,
        (byte)0xD6, (byte)0x86, (byte)0xC4, (byte)0x03,
        (byte)0x19, (byte)0xC8, (byte)0x07, (byte)0x29,
        (byte)0x7A, (byte)0xCA, (byte)0x95, (byte)0x0C,
        (byte)0xD9, (byte)0x96, (byte)0x9F, (byte)0xAB,
        (byte)0xD0, (byte)0x0A, (byte)0x50, (byte)0x9B,
        (byte)0x02, (byte)0x46, (byte)0xD3, (byte)0x08,
        (byte)0x3D, (byte)0x66, (byte)0xA4, (byte)0x5D,
        (byte)0x41, (byte)0x9F, (byte)0x9C, (byte)0x7C,
        (byte)0xBD, (byte)0x89, (byte)0x4B, (byte)0x22,
        (byte)0x19, (byte)0x26, (byte)0xBA, (byte)0xAB,
        (byte)0xA2, (byte)0x5E, (byte)0xC3, (byte)0x55,
        (byte)0xE9, (byte)0x2F, (byte)0x78, (byte)0xC7
    };

    // The SKIP 1024 bit modulus
    private static final BigInteger skip1024Modulus
    = new BigInteger(1, skip1024ModulusBytes);

    // The base used with the SKIP 1024 bit modulus
    private static final BigInteger skip1024Base = BigInteger.valueOf(2);
}
