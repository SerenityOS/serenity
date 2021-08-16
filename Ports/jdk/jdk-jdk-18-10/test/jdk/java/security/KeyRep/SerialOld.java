/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.1, 03/08/13
 * @bug 4532506
 * @summary Serializing KeyPair on one VM (Sun),
 *      and Deserializing on another (IBM) fails
 * @run main/othervm/java.security.policy=SerialOld.policy SerialOld
 */

import java.io.*;
import java.security.*;

public class SerialOld {
    public static void main(String[] args) throws Exception {

        // verify tiger DSA and RSA public keys still deserialize in our VM

        deserializeTigerKey("DSA");
        deserializeTigerKey("RSA");

        // verify pre-tiger keys still deserialize in our VM

        deserializeKey("DSA");
        deserializeKey("RSA");
        deserializeKey("DH");
        deserializeKey("AES");
        deserializeKey("Blowfish");
        deserializeKey("DES");
        deserializeKey("DESede");
        deserializeKey("RC5");
        deserializeKey("HmacSHA1");
        deserializeKey("HmacMD5");
        deserializeKey("PBE");
    }

    private static void deserializeTigerKey(String algorithm) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream
                        (System.getProperty("test.src", ".") +
                        File.separator +
                        algorithm + ".1.5.key"));
        ois.readObject();
        ois.close();
    }
    private static void deserializeKey(String algorithm) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream
                        (System.getProperty("test.src", ".") +
                        File.separator +
                        algorithm + ".pre.1.5.key"));
        ois.readObject();
        ois.close();
    }
}
