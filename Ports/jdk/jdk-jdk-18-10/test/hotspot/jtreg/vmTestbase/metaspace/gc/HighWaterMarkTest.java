/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package metaspace.gc;

import java.util.Arrays;
import vm.share.VMRuntimeEnvUtils;

/**
 * Test metaspace ergonomic.
 *
 * <ul>
 * <li>MetaspaceSize
 * <li>MaxMetaspaceSize
 * <li>MinMetaspaceFreeRatio
 * <li>MaxMetaspaceFreeRatio
 * </ul>
 *
 * The test loads classes until the committed metaspace achieves the certain
 * level between MetaspaceSize and MaxMetaspaceSize.
 * Then it counts how many times GC has been induced.
 * Test verifies that MinMetaspaceFreeRatio/MaxMetaspaceFreeRatio settings
 * affect the frequency of GC. (High-water mark)
 *
 * Quoting: Java SE 8 HotSpot[tm] Virtual Machine Garbage Collection Tuning
 * <pre>
 * Class metadata is deallocated when the corresponding Java class is unloaded.
 * Java classes are unloaded as a results of garbage collection and garbage
 * collections may be induced in order to unload classes and deallocate class
 * metadata. When the space used for class metadata reaches a certain level
 * (call it a high-water mark), a garbage collection is induced.
 * After the garbage collection the high-water mark may be raised or lowered
 * depending on the amount of space freed from class metadata. The high-water
 * mark would be raised so as not to induce another garbage collection too soon.
 * The high-water mark is initially set to the value of the command-line
 * flag MetaspaceSize . It is raised or lowered based on the flags
 * MaxMetaspaceFreeRatio and MinMetaspaceFreeRatio.
 * If the committed space available for class metadata as a percentage of
 * the total committed space for class metadata is greater than
 * MaxMetaspaceFreeRatio, the high-water mark will be lowered.
 * If it is less than MinMetaspaceFreeRatio, the high-water mark will be raised.
 * </pre>
 */
public class HighWaterMarkTest extends FirstGCTest {

    public static void main(String... args) {
        new HighWaterMarkTest().run(args);
    }

    // value given in -XX:MetaspaceSize=<value>
    private long metaspaceSize = -1;

    // value given in -XX:MaxMetaspaceSize=<value>
    private long maxMetaspaceSize = -1;

    // value given in -XX:MinMetaspaceFreeRatio=<value>
    private long minMetaspaceFreeRatio = -1;

    // value given in -XX:MaxMetaspaceFreeRatio=<value>
    private long maxMetaspaceFreeRatio = -1;

    /**
     * Parses arguments and vm options.
     * Throws Fault in cases of wrong values or missed parameters.
     *
     * @param args command line options
     */
    @Override
    protected void parseArgs(String[] args) {
        if (args.length > 0) {
            printUsage();
            throw new Fault("Illegal arguments: " + Arrays.asList(args));
        }

        if (gclogFileName == null) {
            printUsage();
            throw new Fault("Log file name is not given");
        }

        final String metaSize    = "-XX:MetaspaceSize=";
        final String maxMetaSize = "-XX:MaxMetaspaceSize=";
        final String minRatio    = "-XX:MinMetaspaceFreeRatio=";
        final String maxRatio    = "-XX:MaxMetaspaceFreeRatio=";

        for (String va: vmArgs) {
            if (va.startsWith(metaSize)) {
                metaspaceSize = parseValue(va.substring(metaSize.length()));
            } else if (va.startsWith(maxMetaSize)) {
                maxMetaspaceSize = parseValue(va.substring(maxMetaSize.length()));
            } else if (va.startsWith(minRatio)) {
                minMetaspaceFreeRatio = parseValue(va.substring(minRatio.length()));
            } else if (va.startsWith(maxRatio)) {
                maxMetaspaceFreeRatio = parseValue(va.substring(maxRatio.length()));
            }
        }

        if (metaspaceSize < 0) {
            printUsage();
            throw new Fault("-XX:MetaspaceSize is not specified");
        } else if (maxMetaspaceSize < 0) {
            printUsage();
            throw new Fault("-XX:MaxMetaspaceSize is not specified");
        } else if (minMetaspaceFreeRatio < 0) {
            printUsage();
            throw new Fault("-XX:MinMetaspaceFreeRatio is not specified");
        } else if (maxMetaspaceFreeRatio < 0) {
            printUsage();
            throw new Fault("-XX:MaxMetaspaceFreeRatio is not specified");
        }

    }

    private void printUsage() {
        System.err.println("Usage: ");
        System.err.println("java [-Xlog:gc:<filename>] [-XX:MetaspaceSize=..] [-XX:MaxMetaspaceSize=..] [-XX:MinMetaspaceFreeRatio=..] [-XX:MaxMetaspaceFreeRatio=..] \\");
        System.err.println("    " + HighWaterMarkTest.class.getCanonicalName());
    }

    /**
     * Check that MinMetaspaceFreeRatio/MaxMetaspaceFreeRatio settings
     * affects the moment of the next GC.
     *
     * Eats memory until amount of committed metaspace achieves a certain level
     * (between MetaspaceSize and MaxMetaspaceSize).
     * Then checks how many times GC has been invoked.
     *
     */
    @Override
    public void doCheck() {

        // to avoid timeouts we limit the number of attempts
        int attempts = 0;
        int maxAttempts = 10_000;

        // in between metaspaceSize and maxMetaspaceSize
        // no OOM is exepcted.
        long committedLevel = (metaspaceSize + maxMetaspaceSize) / 2;

        while (getCommitted() < committedLevel && attempts < maxAttempts) {
            attempts++;
            loadNewClasses(9, true);  // load classes and keep references
            loadNewClasses(1, false); // load classes without keeping references
        }


        System.out.println("% Classes loaded: " + attempts*10);
        System.out.println("% Used metaspace    : " + bytes2k(getUsed()));
        System.out.println("% Committed metaspce: " + bytes2k(getCommitted()));

        cleanLoadedClasses();

        if (attempts == maxAttempts) {
            throw new Fault("Committed amount hasn't achieved " + bytes2k(committedLevel));
        }

        int gcCount = getMetaspaceGCCount();
        if (gcCount < 0) {
            // perhpas, it's better to silently pass here... Let's see.
            throw new Fault ("Unable to count full collections, could be an env issue");
        }
        System.out.println("% GC has been invoked: " + gcCount + " times");

        if (maxMetaspaceFreeRatio <= 1) {
            // min/max = 0/1  boundary value
            // GC should happen very often
            checkGCCount(gcCount, 20, -1);
        } else if (minMetaspaceFreeRatio >= 99) {
            // min/max = 99/100  boundary value
            // GC should happen very rare
            checkGCCount(gcCount, -1, 2);
        } else if (minMetaspaceFreeRatio >= 10  && maxMetaspaceFreeRatio <= 20) {
            // GC should happen quite often
            checkGCCount(gcCount, 3, 30);
        } else if (minMetaspaceFreeRatio >= 70  && maxMetaspaceFreeRatio <= 80) {
            // GC should happen quite often
            checkGCCount(gcCount, 1, 3);
        } else {
            // hard to estimate
        }

    }
    /**
     * Checks that count of GC fits the expected range.
     * Throws Fault if count is unexpected.
     *
     * @param count how many times GC has happened
     * @param min   expected minimum, if under zero - undefined
     * @param max   expected maximum, if under zero - undefined
     */
    void checkGCCount(int count, int min, int max) {
        if (min < 0) {
            if(count > max) {
                throw new Fault("GC has happened too often: " + count + " times, " +
                        "expected count: less than " + max);
            }
        } else if (max < 0) {
            if(count < min) {
                throw new Fault("GC has happened too rare: " + count + " times, " +
                        "expected count greater than " + min);
            }
        } else if (count < min || count > max ) {
            throw new Fault ("GC has happened " + count + " times, " +
                    "approximate count is " + min + " to " + max);
        }
    }

}
