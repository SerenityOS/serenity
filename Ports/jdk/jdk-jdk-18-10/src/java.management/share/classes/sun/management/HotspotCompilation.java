/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import java.util.regex.*;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import sun.management.counter.*;

/**
 * Implementation class of HotspotCompilationMBean interface.
 *
 * Internal, uncommitted management interface for Hotspot compilation
 * system.
 *
 */
class HotspotCompilation
    implements HotspotCompilationMBean {

    private VMManagement jvm;

    /**
     * Constructor of HotspotRuntime class.
     */
    HotspotCompilation(VMManagement vm) {
        jvm = vm;
        initCompilerCounters();
    }

    // Performance counter support
    private static final String JAVA_CI    = "java.ci.";
    private static final String COM_SUN_CI = "com.sun.ci.";
    private static final String SUN_CI     = "sun.ci.";
    private static final String CI_COUNTER_NAME_PATTERN =
        JAVA_CI + "|" + COM_SUN_CI + "|" + SUN_CI;

    private LongCounter compilerThreads;
    private LongCounter totalCompiles;
    private LongCounter totalBailouts;
    private LongCounter totalInvalidates;
    private LongCounter nmethodCodeSize;
    private LongCounter nmethodSize;
    private StringCounter lastMethod;
    private LongCounter lastSize;
    private LongCounter lastType;
    private StringCounter lastFailedMethod;
    private LongCounter lastFailedType;
    private StringCounter lastInvalidatedMethod;
    private LongCounter lastInvalidatedType;

    private class CompilerThreadInfo {
        String name;
        StringCounter method;
        LongCounter type;
        LongCounter compiles;
        LongCounter time;
        CompilerThreadInfo(String bname, int index) {
            String basename = bname + "." + index + ".";
            this.name = bname + "-" + index;
            this.method = (StringCounter) lookup(basename + "method");
            this.type = (LongCounter) lookup(basename + "type");
            this.compiles = (LongCounter) lookup(basename + "compiles");
            this.time = (LongCounter) lookup(basename + "time");
        }

        @SuppressWarnings("deprecation")
        CompilerThreadStat getCompilerThreadStat() {
            MethodInfo minfo = new MethodInfo(method.stringValue(),
                                              (int) type.longValue(),
                                              -1);
            return new CompilerThreadStat(name,
                                          compiles.longValue(),
                                          time.longValue(),
                                          minfo);
        }
    }
    private List<CompilerThreadInfo> threads;
    private int numActiveThreads; // number of active compiler threads

    private Map<String, Counter> counters;
    private Counter lookup(String name) {
        Counter c = null;

        // Only one counter exists with the specified name in the
        // current implementation.  We first look up in the SUN_CI namespace
        // since most counters are in SUN_CI namespace.

        if ((c = counters.get(SUN_CI + name)) != null) {
            return c;
        }
        if ((c = counters.get(COM_SUN_CI + name)) != null) {
            return c;
        }
        if ((c = counters.get(JAVA_CI + name)) != null) {
            return c;
        }

        // FIXME: should tolerate if counter doesn't exist
        throw new AssertionError("Counter " + name + " does not exist");
    }

    private void initCompilerCounters() {
        // Build a tree map of the current list of performance counters
        counters = new TreeMap<>();
        for (Counter c: getInternalCompilerCounters()) {
            counters.put(c.getName(), c);
        }

        compilerThreads = (LongCounter) lookup("threads");
        totalCompiles = (LongCounter) lookup("totalCompiles");
        totalBailouts = (LongCounter) lookup("totalBailouts");
        totalInvalidates = (LongCounter) lookup("totalInvalidates");
        nmethodCodeSize = (LongCounter) lookup("nmethodCodeSize");
        nmethodSize = (LongCounter) lookup("nmethodSize");
        lastMethod = (StringCounter) lookup("lastMethod");
        lastSize = (LongCounter) lookup("lastSize");
        lastType = (LongCounter) lookup("lastType");
        lastFailedMethod = (StringCounter) lookup("lastFailedMethod");
        lastFailedType = (LongCounter) lookup("lastFailedType");
        lastInvalidatedMethod = (StringCounter) lookup("lastInvalidatedMethod");
        lastInvalidatedType = (LongCounter) lookup("lastInvalidatedType");

        numActiveThreads = (int) compilerThreads.longValue();

        // Allocate CompilerThreadInfo for compilerThread and adaptorThread
        threads = new ArrayList<CompilerThreadInfo>();

        for (int i = 0; i < numActiveThreads; i++) {
            if (counters.containsKey(SUN_CI + "compilerThread." + i + ".method")) {
                threads.add(new CompilerThreadInfo("compilerThread", i));
            }
        }
    }

    public int getCompilerThreadCount() {
        return numActiveThreads;
    }

    public long getTotalCompileCount() {
        return totalCompiles.longValue();
    }

    public long getBailoutCompileCount() {
        return totalBailouts.longValue();
    }

    public long getInvalidatedCompileCount() {
        return totalInvalidates.longValue();
    }

    public long getCompiledMethodCodeSize() {
        return nmethodCodeSize.longValue();
    }

    public long getCompiledMethodSize() {
        return nmethodSize.longValue();
    }

    @Deprecated
    public List<CompilerThreadStat> getCompilerThreadStats() {
        List<CompilerThreadStat> list = new ArrayList<>(threads.size());
        for (CompilerThreadInfo info : threads) {
            list.add(info.getCompilerThreadStat());
        }
        return list;
    }

    public MethodInfo getLastCompile() {
        return new MethodInfo(lastMethod.stringValue(),
                              (int) lastType.longValue(),
                              (int) lastSize.longValue());
    }

    public MethodInfo getFailedCompile() {
        return new MethodInfo(lastFailedMethod.stringValue(),
                              (int) lastFailedType.longValue(),
                              -1);
    }

    public MethodInfo getInvalidatedCompile() {
        return new MethodInfo(lastInvalidatedMethod.stringValue(),
                              (int) lastInvalidatedType.longValue(),
                              -1);
    }

    public java.util.List<Counter> getInternalCompilerCounters() {
        return jvm.getInternalCounters(CI_COUNTER_NAME_PATTERN);
    }
}
