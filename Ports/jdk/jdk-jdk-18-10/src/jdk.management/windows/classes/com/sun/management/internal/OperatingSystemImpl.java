/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management.internal;

import com.sun.management.OperatingSystemMXBean;
import sun.management.BaseOperatingSystemImpl;
import sun.management.VMManagement;

/**
 * Implementation class for the operating system.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getOperatingSystemMXBean() returns an instance
 * of this class.
 */
class OperatingSystemImpl extends BaseOperatingSystemImpl
    implements OperatingSystemMXBean {

    // psapiLock is a lock to make sure only one thread loading
    // PSAPI DLL.
    private static Object psapiLock = new Object();

    OperatingSystemImpl(VMManagement vm) {
        super(vm);
    }

    @Override
    public long getCommittedVirtualMemorySize() {
        synchronized (psapiLock) {
            return getCommittedVirtualMemorySize0();
        }
    }

    @Override
    public long getTotalSwapSpaceSize() {
        return getTotalSwapSpaceSize0();
    }

    @Override
    public long getFreeSwapSpaceSize() {
        return getFreeSwapSpaceSize0();
    }

    @Override
    public long getProcessCpuTime() {
        return getProcessCpuTime0();
    }

    @Override
    public long getFreeMemorySize() {
        return getFreeMemorySize0();
    }

    @Override
    public long getTotalMemorySize() {
        return getTotalMemorySize0();
    }

    @Override
    public double getCpuLoad() {
        return getCpuLoad0();
    }

    @Override
    public double getProcessCpuLoad() {
        return getProcessCpuLoad0();
    }

    /* native methods */
    private native long getCommittedVirtualMemorySize0();
    private native long getFreeMemorySize0();
    private native long getFreeSwapSpaceSize0();
    private native double getProcessCpuLoad0();
    private native long getProcessCpuTime0();
    private native double getCpuLoad0();
    private native long getTotalMemorySize0();
    private native long getTotalSwapSpaceSize0();

    static {
        initialize0();
    }

    private static native void initialize0();
}
