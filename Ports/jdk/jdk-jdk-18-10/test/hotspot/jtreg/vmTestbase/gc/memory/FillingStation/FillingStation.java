/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/memory/FillingStation.
 * VM Testbase keywords: [gc, stress, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.memory.FillingStation.FillingStation
 */

package gc.memory.FillingStation;

import jdk.test.lib.Utils;
import java.util.Random;

public class FillingStation {

    public static final long minObjectSize  = 4;
    public static final long freeSpaceLimit = 64;
    public static final long maxObjectSize  = 32*1024;

    public static final boolean debug        = false;

    public static void main(String[] arg) {
        prologue();
        fill();
        epilogue();
    }

    public static void prologue() {
        _beforeMillis = System.currentTimeMillis();
    }

    public static void epilogue() {
        _afterMillis = System.currentTimeMillis();
        if (_overflow) {
            System.out.println("Overflowed!");
        }
        final double deltaSecs = (_afterMillis - _beforeMillis) / 1000.0;
        final double freeMegs = ((double) _freeBytes) / (1024.0 * 1024.0);
        final double totalMegs = ((double) _totalBytes) / (1024.0 * 1024.0);
        final double memRatio = freeMegs / totalMegs;
        System.out.println("Runtime.freeMemory()/Runtime.totalMemory: " +
                           Long.toString(_freeBytes) +
                           "/" +
                           Long.toString(_totalBytes) +
                           " = " +
                           Double.toString(memRatio));
        System.out.println("That took: " +
                           Double.toString(deltaSecs) +
                           " seconds");
    }

    public static void fill() {
        boolean _overflow = false;
        Runtime rt = java.lang.Runtime.getRuntime();
        Random stream = Utils.getRandomInstance();
        Space next = null;
        try {
            for (long available = rt.freeMemory();
                 available > freeSpaceLimit;
                 available = rt.freeMemory()) {
                long request   = (available - freeSpaceLimit) / 2;
                int maxRequest = (int) Math.min(maxObjectSize, request);
                int minRequest = (int) Math.max(minObjectSize, maxRequest);
                int size = stream.nextInt(minRequest);
                if (debug) {
                    System.err.println("available: " + Long.toString(available) +
                                       "  maxRequest: " + Integer.toString(maxRequest) +
                                       "  minRequest: " + Integer.toString(minRequest) +
                                       "  size: " + Integer.toString(size));
                }
                next = new Space(size, next);
            }
        } catch (OutOfMemoryError oome) {
            _overflow = true;
        }
        _freeBytes = rt.freeMemory();
        _totalBytes = rt.totalMemory();
    }

    static long    _beforeMillis = 0L;
    static long    _afterMillis  = 0L;
    static long    _freeBytes    = 0L;
    static long    _totalBytes   = 0L;
    static boolean _overflow     = false;
}

class Space {
    public Space(int bytes, Space next) {
        _next = next;
        _space = new byte[bytes];
    }
    private Space              _next  = null;
    private byte[]             _space = null;
}
