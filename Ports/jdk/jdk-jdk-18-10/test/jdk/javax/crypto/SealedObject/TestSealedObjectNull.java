/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NullCipher;
import javax.crypto.SealedObject;

/*
 * @test
 * @bug 8048624
 * @summary This test instantiate a NullCipher, seal and unseal a String
 *  object using the SealedObject with the initialized NullCipher,
 *  and then compare the String content.
 */
public class TestSealedObjectNull {

    private static final String SEAL_STR = "Any String!@#$%^";

    public static void main(String[] args) throws IOException,
            IllegalBlockSizeException, ClassNotFoundException,
            BadPaddingException {
        Cipher nullCipher = new NullCipher();

        // Seal
        SealedObject so = new SealedObject(SEAL_STR, nullCipher);

        // Unseal and compare
        if (!(SEAL_STR.equals(so.getObject(nullCipher)))) {
            throw new RuntimeException("Unseal and compare failed.");
        }

        System.out.println("Test passed.");
    }
}
