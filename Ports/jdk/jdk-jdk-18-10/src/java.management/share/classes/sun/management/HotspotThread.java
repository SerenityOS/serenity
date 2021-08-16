/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.HashMap;
import sun.management.counter.Counter;


/**
 * Implementation class of HotspotThreadMBean interface.
 *
 * Internal, uncommitted management interface for Hotspot threading
 * system.
 */
class HotspotThread
    implements HotspotThreadMBean {

    private VMManagement jvm;

    /**
     * Constructor of HotspotThread class.
     */
    HotspotThread(VMManagement vm) {
        jvm = vm;
    }

    public native int getInternalThreadCount();

    public Map<String, Long> getInternalThreadCpuTimes() {
        int count = getInternalThreadCount();
        if (count == 0) {
            return java.util.Collections.emptyMap();
        }
        String[] names = new String[count];
        long[] times = new long[count];
        int numThreads = getInternalThreadTimes0(names, times);
        Map<String, Long> result = new HashMap<>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            result.put(names[i], times[i]);
        }
        return result;
    }
    public native int getInternalThreadTimes0(String[] names, long[] times);

    // Performance counter support
    private static final String JAVA_THREADS    = "java.threads.";
    private static final String COM_SUN_THREADS = "com.sun.threads.";
    private static final String SUN_THREADS     = "sun.threads.";
    private static final String THREADS_COUNTER_NAME_PATTERN =
        JAVA_THREADS + "|" + COM_SUN_THREADS + "|" + SUN_THREADS;

    public java.util.List<Counter> getInternalThreadingCounters() {
        return jvm.getInternalCounters(THREADS_COUNTER_NAME_PATTERN);
    }
}
