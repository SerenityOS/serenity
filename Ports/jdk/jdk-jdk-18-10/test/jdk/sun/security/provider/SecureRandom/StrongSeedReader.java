 /*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6425477
 * @summary Better support for generation of high entropy random numbers
 * @run main/othervm StrongSeedReader
 */

import java.io.*;
import java.net.*;
import java.security.SecureRandom;

/**
 * A simple test which takes into account knowledge about the underlying
 * implementation. This may change if the implementations change.
 *
 * Create a new EGD file with known bytes, then set the EGD System property. The
 * data read should be the same as what was written.
 */
public class StrongSeedReader {

    public static void main(String[] args) throws Exception {
        // Skip Windows, the SHA1PRNG uses CryptGenRandom.
        if (System.getProperty("os.name", "unknown").startsWith("Windows")) {
            return;
        }

        File file = null;
        try {
            file = new File(System.getProperty("java.io.tmpdir"),
                    "StrongSeedReader.tmpdata");

            // write a bunch of 0's to the file.
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(new byte[2048]);

            System.setProperty("java.security.egd", file.toURI().toString());
            testSeed("NativePRNG");
            testSeed("SHA1PRNG");
            testSeed("DRBG");
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    private static void testSeed(String alg) throws Exception {
        System.out.println("Testing: " + alg);
        SecureRandom sr = SecureRandom.getInstance(alg);
        byte[] ba = sr.generateSeed(20);

        // We should get back a bunch of zeros from the file.
        for (byte b : ba) {
            if (b != 0) {
                throw new Exception("Byte != 0");
            }
        }
    }
}
