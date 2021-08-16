/*
 * Copyright (c) 2020, Red Hat Inc.
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

package jdk.internal.platform;

import java.util.Objects;

public class CgroupMetrics implements Metrics {

    private final CgroupSubsystem subsystem;

    CgroupMetrics(CgroupSubsystem subsystem) {
        this.subsystem = Objects.requireNonNull(subsystem);
    }

    @Override
    public String getProvider() {
        return subsystem.getProvider();
    }

    @Override
    public long getCpuUsage() {
        return subsystem.getCpuUsage();
    }

    @Override
    public long[] getPerCpuUsage() {
        return subsystem.getPerCpuUsage();
    }

    @Override
    public long getCpuUserUsage() {
        return subsystem.getCpuUserUsage();
    }

    @Override
    public long getCpuSystemUsage() {
        return subsystem.getCpuSystemUsage();
    }

    @Override
    public long getCpuPeriod() {
        return subsystem.getCpuPeriod();
    }

    @Override
    public long getCpuQuota() {
        return subsystem.getCpuQuota();
    }

    @Override
    public long getCpuShares() {
        return subsystem.getCpuShares();
    }

    @Override
    public long getCpuNumPeriods() {
        return subsystem.getCpuNumPeriods();
    }

    @Override
    public long getCpuNumThrottled() {
        return subsystem.getCpuNumThrottled();
    }

    @Override
    public long getCpuThrottledTime() {
        return subsystem.getCpuThrottledTime();
    }

    @Override
    public long getEffectiveCpuCount() {
        return subsystem.getEffectiveCpuCount();
    }

    @Override
    public int[] getCpuSetCpus() {
        return subsystem.getCpuSetCpus();
    }

    @Override
    public int[] getEffectiveCpuSetCpus() {
        return subsystem.getEffectiveCpuSetCpus();
    }

    @Override
    public int[] getCpuSetMems() {
        return subsystem.getCpuSetMems();
    }

    @Override
    public int[] getEffectiveCpuSetMems() {
        return subsystem.getEffectiveCpuSetMems();
    }

    public long getMemoryFailCount() {
        return subsystem.getMemoryFailCount();
    }

    @Override
    public long getMemoryLimit() {
        return subsystem.getMemoryLimit();
    }

    @Override
    public long getMemoryUsage() {
        return subsystem.getMemoryUsage();
    }

    @Override
    public long getTcpMemoryUsage() {
        return subsystem.getTcpMemoryUsage();
    }

    @Override
    public long getMemoryAndSwapLimit() {
        return subsystem.getMemoryAndSwapLimit();
    }

    @Override
    public long getMemoryAndSwapUsage() {
        return subsystem.getMemoryAndSwapUsage();
    }

    @Override
    public long getMemorySoftLimit() {
        return subsystem.getMemorySoftLimit();
    }

    @Override
    public long getPidsMax() {
        return subsystem.getPidsMax();
    }

    @Override
    public long getBlkIOServiceCount() {
        return subsystem.getBlkIOServiceCount();
    }

    @Override
    public long getBlkIOServiced() {
        return subsystem.getBlkIOServiced();
    }

    public static Metrics getInstance() {
        if (!isUseContainerSupport()) {
            // Return null on -XX:-UseContainerSupport
            return null;
        }
        return CgroupSubsystemFactory.create();
    }

    private static native boolean isUseContainerSupport();

}
