/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.perf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.security.AccessController;

/**
 * Performance counter support for internal JRE classes.
 * This class defines a fixed list of counters for the platform
 * to use as an interim solution until RFE# 6209222 is implemented.
 * The perf counters will be created in the jvmstat perf buffer
 * that the HotSpot VM creates. The default size is 32K and thus
 * the number of counters is bounded.  You can alter the size
 * with {@code -XX:PerfDataMemorySize=<bytes>} option. If there is
 * insufficient memory in the jvmstat perf buffer, the C heap memory
 * will be used and thus the application will continue to run if
 * the counters added exceeds the buffer size but the counters
 * will be missing.
 *
 * See HotSpot jvmstat implementation for certain circumstances
 * that the jvmstat perf buffer is not supported.
 *
 */
public class PerfCounter {
    @SuppressWarnings("removal")
    private static final Perf perf =
        AccessController.doPrivileged(new Perf.GetPerfAction());

    // Must match values defined in hotspot/src/share/vm/runtime/perfdata.hpp
    private static final int V_Constant  = 1;
    private static final int V_Monotonic = 2;
    private static final int V_Variable  = 3;
    private static final int U_None      = 1;

    private final String name;
    private final LongBuffer lb;

    private PerfCounter(String name, int type) {
        this.name = name;
        ByteBuffer bb = perf.createLong(name, type, U_None, 0L);
        bb.order(ByteOrder.nativeOrder());
        this.lb = bb.asLongBuffer();
    }

    public static PerfCounter newPerfCounter(String name) {
        return new PerfCounter(name, V_Variable);
    }

    public static PerfCounter newConstantPerfCounter(String name) {
        PerfCounter c = new PerfCounter(name, V_Constant);
        return c;
    }

    /**
     * Returns the current value of the perf counter.
     */
    public synchronized long get() {
        return lb.get(0);
    }

    /**
     * Sets the value of the perf counter to the given newValue.
     */
    public synchronized void set(long newValue) {
        lb.put(0, newValue);
    }

    /**
     * Adds the given value to the perf counter.
     */
    public synchronized void add(long value) {
        long res = get() + value;
        lb.put(0, res);
    }

    /**
     * Increments the perf counter with 1.
     */
    public void increment() {
        add(1);
    }

    /**
     * Adds the given interval to the perf counter.
     */
    public void addTime(long interval) {
        add(interval);
    }

    /**
     * Adds the elapsed time from the given start time (ns) to the perf counter.
     */
    public void addElapsedTimeFrom(long startTime) {
        add(System.nanoTime() - startTime);
    }

    @Override
    public String toString() {
        return name + " = " + get();
    }

    static class CoreCounters {
        static final PerfCounter pdt   = newPerfCounter("sun.classloader.parentDelegationTime");
        static final PerfCounter lc    = newPerfCounter("sun.classloader.findClasses");
        static final PerfCounter lct   = newPerfCounter("sun.classloader.findClassTime");
        static final PerfCounter rcbt  = newPerfCounter("sun.urlClassLoader.readClassBytesTime");
        static final PerfCounter zfc   = newPerfCounter("sun.zip.zipFiles");
        static final PerfCounter zfot  = newPerfCounter("sun.zip.zipFile.openTime");
    }

    /**
     * Number of findClass calls
     */
    public static PerfCounter getFindClasses() {
        return CoreCounters.lc;
    }

    /**
     * Time (ns) spent in finding classes that includes
     * lookup and read class bytes and defineClass
     */
    public static PerfCounter getFindClassTime() {
        return CoreCounters.lct;
    }

    /**
     * Time (ns) spent in finding classes
     */
    public static PerfCounter getReadClassBytesTime() {
        return CoreCounters.rcbt;
    }

    /**
     * Time (ns) spent in the parent delegation to
     * the parent of the defining class loader
     */
    public static PerfCounter getParentDelegationTime() {
        return CoreCounters.pdt;
    }

    /**
     * Number of zip files opened.
     */
    public static PerfCounter getZipFileCount() {
        return CoreCounters.zfc;
    }

    /**
     * Time (ns) spent in opening the zip files that
     * includes building the entries hash table
     */
    public static PerfCounter getZipFileOpenTime() {
        return CoreCounters.zfot;
    }

}
