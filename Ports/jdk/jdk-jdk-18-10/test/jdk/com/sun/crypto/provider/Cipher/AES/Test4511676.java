/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4511676
 * @summary Verify that AES cipher.init method check key size correctly
 * @author Valerie Peng
 */
import java.io.PrintStream;
import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class Test4511676 {
    private static final String ALGO = "AES";
    private static final int KEYSIZE = 16; // in bytes

    public boolean execute() throws Exception {

        Cipher ci = Cipher.getInstance(ALGO, "SunJCE");

        // TEST FIX 4511676
        KeyGenerator kg = KeyGenerator.getInstance(ALGO, "SunJCE");
        kg.init(KEYSIZE*8);
        SecretKey key = kg.generateKey();
        try {
            ci.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException ex) {
            throw new Exception("key length is mis-intepreted!");
        }

        // passed all tests...hooray!
        return true;
    }

    public static void main (String[] args) throws Exception {

        Test4511676 test = new Test4511676();
        String testName = test.getClass().getName() + "[" + ALGO +
            "]";
        if (test.execute()) {
            System.out.println(testName + ": Passed!");
        }
    }
}
