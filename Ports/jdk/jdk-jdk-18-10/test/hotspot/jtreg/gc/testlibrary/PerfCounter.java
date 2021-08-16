/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package gc.testlibrary;

import sun.jvmstat.monitor.Monitor;

/**
 * Represents a performance counter in the JVM.
 *
 * See http://openjdk.java.net/groups/hotspot/docs/Serviceability.html#bjvmstat
 * for more details about performance counters.
 */
public class PerfCounter {
    private final Monitor monitor;
    private final String name;

    PerfCounter(Monitor monitor, String name) {
        this.monitor = monitor;
        this.name = name;
    }

    /**
     * Returns the value of this performance counter as an Object.
     *
     * @return The value of this performance counter
     */
    public Object value() {
        return monitor.getValue();
    }

    /**
     * Returns the value of this performance counter as a long.
     *
     * @return The long value of this performance counter
     * @throws RuntimeException If the value of the performance counter isn't a long
     */
    public long longValue() {
        Object value = monitor.getValue();
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        throw new RuntimeException("Expected " + monitor.getName() + " to have a long value");
    }

    /**
     * Returns the name of the performance counter.
     *
     * @return The name of the performance counter.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
