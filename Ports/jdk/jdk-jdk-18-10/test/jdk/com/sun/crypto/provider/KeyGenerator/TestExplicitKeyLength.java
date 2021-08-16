/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4959235
 * @summary Verify that ARCFOUR KeyGenerator generate keys according
 * to the key length explicitly specified in the init() call.
 *
 * @author Valerie Peng
 */
import java.security.*;
import javax.crypto.*;
import java.util.*;

public class TestExplicitKeyLength {

    private static final String ALGOS[] = { "RC2", "ARCFOUR" };

    private static final int KEY_SIZES[] =
        { 64, 80 }; // in bits

    public static void runTest(String algo, int keysize) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(algo, "SunJCE");
        kg.init(keysize);
        Key generatedKey = kg.generateKey();
        int actualSizeInBits = generatedKey.getEncoded().length*8;
        if (actualSizeInBits != keysize) {
            throw new Exception("generated key has wrong length: " +
                                actualSizeInBits + " bits");
        }
    }

    public static void main (String[] args) throws Exception {
        for (int i = 0; i < ALGOS.length; i++) {
            System.out.println("Testing " + ALGOS[i] + " KeyGenerator with " +
                               KEY_SIZES[i] + "-bit keysize");
            runTest(ALGOS[i], KEY_SIZES[i]);
        }
        System.out.println("Tests Passed");
    }
}
