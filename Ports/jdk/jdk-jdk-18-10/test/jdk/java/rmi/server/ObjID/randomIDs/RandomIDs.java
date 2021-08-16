/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6364692
 * @summary When the "java.rmi.server.randomIDs" system property is
 * not defined, the ObjID() constructor should behave as if it were
 * set to "true" and generate random object numbers; if the property
 * is defined to something other than "true" (ignoring case), then
 * ObjID() should still generate sequential object numbers.
 * @author Peter Jones
 *
 * @run main/othervm RandomIDs random
 * @run main/othervm -Djava.rmi.server.randomIDs=true RandomIDs random
 * @run main/othervm -Djava.rmi.server.randomIDs=false RandomIDs sequential
 */

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.server.ObjID;

public class RandomIDs {
    private static final int COUNT = 10000;

    public static void main(String[] args) throws Exception {
        boolean shouldBeRandom = false;
        boolean shouldBeSequential = false;

        String usage = "Usage: java RandomIDs [random|sequential]";
        if (args.length != 1) {
            System.err.println(usage);
            throw new Error("wrong number of arguments");
        } else if (args[0].equals("random")) {
            shouldBeRandom = true;
        } else if (args[0].equals("sequential")) {
            shouldBeSequential = true;
        } else {
            System.err.println(usage);
            throw new Error("invalid argument");
        }

        System.err.println("\nRegression test for bug 6364692\n");
        String propertyValue = System.getProperty("java.rmi.server.randomIDs");
        System.err.println(
            "Value of java.rmi.server.randomIDs system property: " +
            (propertyValue != null ? "\"" + propertyValue + "\"" : null));
        System.err.println(
            "Expecting object numbers of unique ObjIDs to be: " + args[0]);

        /*
         * Get the 64-bit "object number" component of COUNT number of
         * unique (not "well-known") ObjID instances created in
         * sequence, by writing each to a dummy ObjectOutputStream and
         * trapping the first writeLong invocation on the stream.
         */
        final long[] objnums = new long[COUNT];
        for (int i = 0; i < COUNT; i++) {
            final int j = i;
            class Escape extends RuntimeException { }
            try {
                new ObjID().write(new ObjectOutputStream(new OutputStream() {
                    public void write(int b) { }
                }) {
                    public void writeLong(long val) throws IOException {
                        objnums[j] = val;
                        throw new Escape();
                    }
                });
                throw new Error("writeLong not invoked");
            } catch (Escape e) {
            }
        }

        /*
         * If the object numbers should be random, then verify that
         * they are.  (This verification is certainly not a thorough
         * evaluation of randomness, but it performs a couple of
         * simple checks to catch mistakes in ObjID's application of a
         * CSPRNG: are roughly half the bits set, and can the sequence
         * be used to get a rough Monte Carlo estimate of pi.  Errors
         * up to 5% are tolerated for both checks.)
         */
        if (shouldBeRandom) {
            int bitCount = 0;
            int piHitCount = 0;
            for (int i = 0; i < COUNT; i++) {
                bitCount += Long.bitCount(objnums[i]);
                double x = ((double) (objnums[i] >>> 32)) / (1L << 32);
                double y = ((double) (objnums[i] & 0xFFFFFFFFL)) / (1L << 32);
                if (((x * x) + (y * y)) <= 1.0) {
                    piHitCount++;
                }
            }

            int bitCountTarget = COUNT * 32;
            double bitCountError =
                ((double) (bitCount - bitCountTarget)) / bitCountTarget;
            if (Math.abs(bitCountError) > 0.05) { // tolerate 5% error
                throw new Error("TEST FAILED: " +
                                "bitCount == " + bitCount);
            }

            double piEstimate = ((double) piHitCount / COUNT) * 4.0;
            double piEstimateError = (piEstimate - Math.PI) / Math.PI;
            if (Math.abs(piEstimateError) > 0.05) { // tolerate 5% error
                throw new Error("TEST FAILED: " +
                                "piEstimate == " + piEstimate);
            }
        }

        /*
         * If the object numbers should be sequential, then verify
         * that they are.
         */
        if (shouldBeSequential) {
            long first = objnums[0];
            /*
             * This test currently verifies that the first object
             * number is zero, but that could be false if one or more
             * remote objects get exported as part of VM startup-- if
             * that starts happening, this check could be relaxed.
             */
            if (first != 0) {
                throw new Error("TEST FAILED: " +
                                "first object number == " + first +
                                " (not zero)");
            }
            for (int i = 1; i < COUNT; i++) {
                if (objnums[i] != first + i) {
                    throw new Error("TEST FAILED: first == " + first + ", " +
                                    "objnums[" + i + "] == " + objnums[i]);
                }
            }
        }

        System.err.println("TEST PASSED");
    }
}
