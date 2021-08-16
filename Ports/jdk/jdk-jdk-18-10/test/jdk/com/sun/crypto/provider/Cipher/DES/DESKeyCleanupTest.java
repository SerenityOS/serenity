/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/com.sun.crypto.provider:+open
 * @run main/othervm DESKeyCleanupTest
 * @summary Verify that key storage is cleared
 */

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Test that the array holding the key bytes is cleared when it is
 * no longer referenced by the key.
 */

public class DESKeyCleanupTest {

    private final static String SunJCEProvider = "SunJCE";

    public static void main(String[] args) throws Exception {
        testCleanupSecret("DES");
        testCleanupSecret("DESede");
    }

    static void testCleanupSecret(String algorithm) throws Exception {
        KeyGenerator desGen = KeyGenerator.getInstance(algorithm, SunJCEProvider);
        SecretKey key = desGen.generateKey();

        // Break into the implementation to observe the key byte array.
        Class<?> keyClass = key.getClass();
        Field keyField = keyClass.getDeclaredField("key");
        keyField.setAccessible(true);
        byte[] array = (byte[])keyField.get(key);

        byte[] zeros = new byte[array.length];
        do {
            // Wait for array to be cleared;  if not cleared test will timeout
            System.out.printf("%s array: %s%n", algorithm, Arrays.toString(array));
            key = null;
            System.gc();        // attempt to reclaim the key
        } while (Arrays.compare(zeros, array) != 0);
        System.out.printf("%s array: %s%n", algorithm, Arrays.toString(array));

        Reference.reachabilityFence(key); // Keep key alive
        Reference.reachabilityFence(array); // Keep array alive
    }
}


