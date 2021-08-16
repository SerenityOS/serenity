/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4102896
 * @summary Make sure that a SecureRandom object can be serialized
 * @run main/othervm Serialize
 */

import java.security.*;
import java.io.*;

public class Serialize {

    public static void main(String args[]) throws Exception {
        System.setProperty("java.security.egd", "file:/dev/urandom");

        for (String alg: new String[]{
                "SHA1PRNG", "DRBG", "Hash_DRBG", "HMAC_DRBG", "CTR_DRBG",
                "Hash_DRBG,SHA-512,192,pr_and_reseed"}) {

            System.out.println("Testing " + alg);
            SecureRandom s1;

            // A SecureRandom can be s11ned and des11ned at any time.

            // Brand new.
            s1 = getInstance(alg);
            revive(s1).nextInt();
            if (alg.contains("DRBG")) {
                revive(s1).reseed();
            }

            // Used
            s1 = getInstance(alg);
            s1.nextInt();    // state set
            revive(s1).nextInt();
            if (alg.contains("DRBG")) {
                revive(s1).reseed();
            }

            // Automatically reseeded
            s1 = getInstance(alg);
            if (alg.contains("DRBG")) {
                s1.reseed();
            }
            revive(s1).nextInt();
            if (alg.contains("DRBG")) {
                revive(s1).reseed();
            }

            // Manually seeded
            s1 = getInstance(alg);
            s1.setSeed(1L);
            revive(s1).nextInt();
            if (alg.contains("DRBG")) {
                revive(s1).reseed();
            }
        }
    }

    private static SecureRandom getInstance(String alg) throws Exception {
        if (alg.equals("SHA1PRNG") || alg.equals("DRBG")) {
            return SecureRandom.getInstance(alg);
        } else {
            String old = Security.getProperty("securerandom.drbg.config");
            try {
                Security.setProperty("securerandom.drbg.config", alg);
                return SecureRandom.getInstance("DRBG");
            } finally {
                Security.setProperty("securerandom.drbg.config", old);
            }
        }
    }

    private static SecureRandom revive(SecureRandom oldOne) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new ObjectOutputStream(bout).writeObject(oldOne);
        SecureRandom newOne = (SecureRandom) new ObjectInputStream(
                new ByteArrayInputStream(bout.toByteArray())).readObject();
        if (!oldOne.toString().equals(newOne.toString())) {
            throw new Exception(newOne + " is not " + oldOne);
        }
        return newOne;
    }
}
