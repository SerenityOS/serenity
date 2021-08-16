/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4953553
 * @summary Ensure that InvalidKeyException is thrown when decrypting
 * without parameters as javadoc has stated.
 * @author Valerie Peng
 */

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.PBEKey;

public class DecryptWithoutParameters {

    public static void main(String argv[]) throws Exception {
        String algo = "PBEWithMD5AndDES";
        Cipher cipher = Cipher.getInstance(algo, "SunJCE");
        SecretKey key = new SecretKeySpec(new byte[5], algo);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            throw new Exception("Should throw InvalidKeyException when " +
                                "decrypting without parameters");
        } catch (InvalidKeyException ike) {
            System.out.println("Test Passed: InvalidKeyException thrown");
        }
    }
}
