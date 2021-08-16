/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6377058 7055362
 * @library ../../../java/security/testlibrary
 * @summary SunJCE depends on sun.security.provider.SignatureImpl
 * behaviour, BC can't load into 1st slot.
 * @author Brad R. Wetmore
 * @key randomness
 */

import java.security.*;
import javax.crypto.*;
import java.io.*;

public class SunJCE_BC_LoadOrdering {

    public static void main(String[] args) throws Exception {
        ProvidersSnapshot snapshot = ProvidersSnapshot.create();
        try {
            main0(args);
        } finally {
            snapshot.restore();
        }
    }

    public static void main0(String[] args) throws Exception {
        /*
         * Generate a random key, and encrypt the data
         */
        Security.insertProviderAt(new MyProvider(), 1);
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        keyGen.init(new SecureRandom());

        Key key = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        cipher.doFinal("some string".getBytes());
    }
}
