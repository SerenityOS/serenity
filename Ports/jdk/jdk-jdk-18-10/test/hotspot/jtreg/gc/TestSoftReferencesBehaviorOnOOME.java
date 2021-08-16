/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/**
 * @test TestSoftReferencesBehaviorOnOOME
 * @key randomness
 * @summary Tests that all SoftReferences has been cleared at time of OOM.
 * @requires vm.gc != "Z"
 * @requires vm.gc != "Shenandoah"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -Xmx128m gc.TestSoftReferencesBehaviorOnOOME 512 2k
 * @run main/othervm -Xmx128m gc.TestSoftReferencesBehaviorOnOOME 128k 256k
 * @run main/othervm -Xmx128m gc.TestSoftReferencesBehaviorOnOOME 2k 32k
 */
import jdk.test.lib.Utils;
import jdk.test.lib.Asserts;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Random;

public class TestSoftReferencesBehaviorOnOOME {

    /**
     * Test generates a lot of soft references to objects with random payloads.
     * Then it provokes OOME and checks that all SoftReferences has been gone
     * @param args - [minSize] [maxSize] [freq]
     *  where
     *  - minSize - min size of random objects
     *  - maxSize - max size of random objects
     */
    public static void main(String[] args) {
        long minSize = DEFAULT_MIN_SIZE;
        long maxSize = DEFAULT_MAX_SIZE;

        if ( args.length >= 2) {
            maxSize = getBytesCount(args[1]);
        }

        if ( args.length >= 1) {
            minSize = getBytesCount(args[0]);
        }

        new TestSoftReferencesBehaviorOnOOME().softReferencesOom(minSize, maxSize);
    }

    /**
     * Test that all SoftReferences has been cleared at time of OOM.
     */
    void softReferencesOom(long minSize, long maxSize) {
        System.out.format( "minSize = %d, maxSize = %d%n", minSize, maxSize );

        LinkedList<SoftReference<byte[]>> arrSoftRefs = new LinkedList<>();
        staticRef = arrSoftRefs;
        LinkedList<byte[]> arrObjects = new LinkedList<>();
        staticRef = arrObjects;

        long multiplier = maxSize - minSize;
        long numberOfNotNulledObjects = 0;

        try {

            // Lets allocate as many as we can - taking size of all SoftRerefences
            // by minimum. So it can provoke some GC but we surely will allocate enough.
            long numSofts = (long) ((0.95 * Runtime.getRuntime().totalMemory()) / minSize);
            System.out.println("num Soft: " + numSofts);

            while (numSofts-- > 0) {
                int allocationSize = ((int) (RND_GENERATOR.nextDouble() * multiplier))
                            + (int)minSize;
                arrSoftRefs.add(new SoftReference<byte[]>(new byte[allocationSize]));
            }

            System.out.println("free: " + Runtime.getRuntime().freeMemory());

            // provoke OOME.
            while (true) {
                arrObjects.add(new byte[(int) Runtime.getRuntime().totalMemory()]);
            }

        } catch (OutOfMemoryError oome) {

            // Clear allocated ballast, so we don't get another OOM.
            staticRef = null;
            arrObjects = null;
            long oomSoftArraySize = arrSoftRefs.size();

            for (SoftReference<byte[]> sr : arrSoftRefs) {
                Object o = sr.get();

                if (o != null) {
                    numberOfNotNulledObjects++;
                }
            }

            // Make sure we clear all refs before we return failure
            arrSoftRefs = null;
            Asserts.assertFalse(numberOfNotNulledObjects > 0,
                    "" + numberOfNotNulledObjects + " out of "
                    + oomSoftArraySize + " SoftReferences was not "
                    + "null at time of OutOfMemoryError"
            );
        } finally {
            Asserts.assertTrue(arrObjects == null, "OOME hasn't been provoked");
            Asserts.assertTrue(arrSoftRefs == null, "OOME hasn't been provoked");
        }
    }

    private static final long getBytesCount(String arg) {
        String postfixes = "kMGT";
        long mod = 1;

        if (arg.trim().length() >= 2) {
            mod = postfixes.indexOf(arg.trim().charAt(arg.length() - 1));

            if (mod != -1) {
                mod = (long) Math.pow(1024, mod+1);
                arg = arg.substring(0, arg.length() - 1);
            } else {
                mod = 1; // 10^0
            }
        }

        return Long.parseLong(arg) * mod;
    }

    private static final Random RND_GENERATOR = Utils.getRandomInstance();
    private static final long DEFAULT_MIN_SIZE = 512;
    private static final long DEFAULT_MAX_SIZE = 1024;
    private static Object staticRef; // to prevent compile optimisations
}
