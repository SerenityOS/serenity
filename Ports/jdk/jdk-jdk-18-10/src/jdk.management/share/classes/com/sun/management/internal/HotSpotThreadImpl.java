/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.management.ThreadMXBean;
import sun.management.ManagementFactoryHelper;
import sun.management.ThreadImpl;
import sun.management.VMManagement;

/**
 *
 */
public class HotSpotThreadImpl extends ThreadImpl implements ThreadMXBean {
    public HotSpotThreadImpl(VMManagement vm) {
        super(ManagementFactoryHelper.getVMManagement());
    }

    @Override
    public boolean isThreadAllocatedMemorySupported() {
        return super.isThreadAllocatedMemorySupported();
    }

    @Override
    public boolean isThreadAllocatedMemoryEnabled() {
        return super.isThreadAllocatedMemoryEnabled();
    }

    @Override
    public long[] getThreadCpuTime(long[] ids) {
        return super.getThreadCpuTime(ids);
    }

    @Override
    public long[] getThreadUserTime(long[] ids) {
        return super.getThreadUserTime(ids);
    }

    @Override
    public long getCurrentThreadAllocatedBytes() {
        return super.getCurrentThreadAllocatedBytes();
    }

    @Override
    public long getThreadAllocatedBytes(long id) {
        return super.getThreadAllocatedBytes(id);
    }

    @Override
    public long[] getThreadAllocatedBytes(long[] ids) {
        return super.getThreadAllocatedBytes(ids);
    }

    @Override
    public void setThreadAllocatedMemoryEnabled(boolean enable) {
        super.setThreadAllocatedMemoryEnabled(enable);
    }
}
