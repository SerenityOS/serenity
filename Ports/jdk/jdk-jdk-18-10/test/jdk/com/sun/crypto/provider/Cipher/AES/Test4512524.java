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
 * @bug 4512524
 * @summary Verify that AES cipher can work with mode "CBC", "OFB", "CFB"
 * @author Valerie Peng
 */
import java.io.PrintStream;
import java.security.*;
import java.security.spec.*;
import java.util.Random;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class Test4512524 {

    private static final String ALGO = "AES";
    private static final String PADDING = "NoPadding";
    private static final int KEYSIZE = 16; // in bytes

    public void execute(String mode) throws Exception {

        String transformation = ALGO+"/"+mode+"/"+PADDING;
        Cipher ci = Cipher.getInstance(transformation, "SunJCE");

        // TEST FIX 4512524
        KeyGenerator kg = KeyGenerator.getInstance(ALGO, "SunJCE");
        kg.init(KEYSIZE*8);
        SecretKey key = kg.generateKey();

        try{
            AlgorithmParameterSpec aps = null;
            ci.init(Cipher.ENCRYPT_MODE, key, aps);
        } catch (NullPointerException ex) {
            throw new Exception("null parameter is not handled correctly!");
        }

        // passed all tests...hooray!
        System.out.println(transformation + ": Passed");
    }

    public static void main (String[] args) throws Exception {
        Test4512524 test = new Test4512524();
        test.execute("CBC");
        test.execute("GCM");
    }
}
