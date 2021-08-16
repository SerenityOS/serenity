/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.platform.cgroupv1;

import java.util.Map;

import jdk.internal.platform.CgroupInfo;
import jdk.internal.platform.CgroupSubsystem;
import jdk.internal.platform.CgroupSubsystemController;
import jdk.internal.platform.CgroupV1Metrics;

public class CgroupV1Subsystem implements CgroupSubsystem, CgroupV1Metrics {
    private CgroupV1MemorySubSystemController memory;
    private CgroupV1SubsystemController cpu;
    private CgroupV1SubsystemController cpuacct;
    private CgroupV1SubsystemController cpuset;
    private CgroupV1SubsystemController blkio;
    private CgroupV1SubsystemController pids;

    private static volatile CgroupV1Subsystem INSTANCE;

    private static final String PROVIDER_NAME = "cgroupv1";

    private CgroupV1Subsystem() {}

    /**
     * Get a singleton instance of CgroupV1Subsystem. Initially, it creates a new
     * object by retrieving the pre-parsed information from cgroup interface
     * files from the provided 'infos' map.
     *
     * See CgroupSubsystemFactory.determineType() where the actual parsing of
     * cgroup interface files happens.
     *
     * @return A singleton CgroupV1Subsystem instance, never null
     */
    public static CgroupV1Subsystem getInstance(Map<String, CgroupInfo> infos) {
        if (INSTANCE == null) {
            CgroupV1Subsystem tmpSubsystem = initSubSystem(infos);
            synchronized (CgroupV1Subsystem.class) {
                if (INSTANCE == null) {
                    INSTANCE = tmpSubsystem;
                }
            }
        }
        return INSTANCE;
    }

    private static CgroupV1Subsystem initSubSystem(Map<String, CgroupInfo> infos) {
        CgroupV1Subsystem subsystem = new CgroupV1Subsystem();

        boolean anyActiveControllers = false;
        /*
         * Find the cgroup mount points for subsystem controllers
         * by looking up relevant data in the infos map
         */
        for (CgroupInfo info: infos.values()) {
            switch (info.getName()) {
            case "memory": {
                if (info.getMountRoot() != null && info.getMountPoint() != null) {
                    CgroupV1MemorySubSystemController controller = new CgroupV1MemorySubSystemController(info.getMountRoot(), info.getMountPoint());
                    controller.setPath(info.getCgroupPath());
                    boolean isHierarchial = getHierarchical(controller);
                    controller.setHierarchical(isHierarchial);
                    boolean isSwapEnabled = getSwapEnabled(controller);
                    controller.setSwapEnabled(isSwapEnabled);
                    subsystem.setMemorySubSystem(controller);
                    anyActiveControllers = true;
                }
                break;
            }
            case "cpuset": {
                if (info.getMountRoot() != null && info.getMountPoint() != null) {
                    CgroupV1SubsystemController controller = new CgroupV1SubsystemController(info.getMountRoot(), info.getMountPoint());
                    controller.setPath(info.getCgroupPath());
                    subsystem.setCpuSetController(controller);
                    anyActiveControllers = true;
                }
                break;
            }
            case "cpuacct": {
                if (info.getMountRoot() != null && info.getMountPoint() != null) {
                    CgroupV1SubsystemController controller = new CgroupV1SubsystemController(info.getMountRoot(), info.getMountPoint());
                    controller.setPath(info.getCgroupPath());
                    subsystem.setCpuAcctController(controller);
                    anyActiveControllers = true;
                }
                break;
            }
            case "cpu": {
                if (info.getMountRoot() != null && info.getMountPoint() != null) {
                    CgroupV1SubsystemController controller = new CgroupV1SubsystemController(info.getMountRoot(), info.getMountPoint());
                    controller.setPath(info.getCgroupPath());
                    subsystem.setCpuController(controller);
                    anyActiveControllers = true;
                }
                break;
            }
            case "blkio": {
                if (info.getMountRoot() != null && info.getMountPoint() != null) {
                    CgroupV1SubsystemController controller = new CgroupV1SubsystemController(info.getMountRoot(), info.getMountPoint());
                    controller.setPath(info.getCgroupPath());
                    subsystem.setBlkIOController(controller);
                    anyActiveControllers = true;
                }
                break;
            }
            case "pids": {
                if (info.getMountRoot() != null && info.getMountPoint() != null) {
                    CgroupV1SubsystemController controller = new CgroupV1SubsystemController(info.getMountRoot(), info.getMountPoint());
                    controller.setPath(info.getCgroupPath());
                    subsystem.setPidsController(controller);
                    anyActiveControllers = true;
                }
                break;
            }
            default:
                throw new AssertionError("Unrecognized controller in infos: " + info.getName());
            }
        }

        // Return Metrics object if we found any subsystems.
        if (anyActiveControllers) {
            return subsystem;
        }

        return null;
    }

    private static boolean getSwapEnabled(CgroupV1MemorySubSystemController controller) {
         long retval = getLongValue(controller, "memory.memsw.limit_in_bytes");
         return retval > 0;
     }


    private static boolean getHierarchical(CgroupV1MemorySubSystemController controller) {
        long hierarchical = getLongValue(controller, "memory.use_hierarchy");
        return hierarchical > 0;
    }

    private void setMemorySubSystem(CgroupV1MemorySubSystemController memory) {
        this.memory = memory;
    }

    private void setCpuController(CgroupV1SubsystemController cpu) {
        this.cpu = cpu;
    }

    private void setCpuAcctController(CgroupV1SubsystemController cpuacct) {
        this.cpuacct = cpuacct;
    }

    private void setCpuSetController(CgroupV1SubsystemController cpuset) {
        this.cpuset = cpuset;
    }

    private void setBlkIOController(CgroupV1SubsystemController blkio) {
        this.blkio = blkio;
    }

    private void setPidsController(CgroupV1SubsystemController pids) {
        this.pids = pids;
    }

    private static long getLongValue(CgroupSubsystemController controller,
                              String parm) {
        return CgroupSubsystemController.getLongValue(controller,
                                                      parm,
                                                      CgroupV1SubsystemController::convertStringToLong,
                                                      CgroupSubsystem.LONG_RETVAL_UNLIMITED);
    }

    public String getProvider() {
        return PROVIDER_NAME;
    }

    /*****************************************************************
     * CPU Accounting Subsystem
     ****************************************************************/


    public long getCpuUsage() {
        return getLongValue(cpuacct, "cpuacct.usage");
    }

    public long[] getPerCpuUsage() {
        String usagelist = CgroupSubsystemController.getStringValue(cpuacct, "cpuacct.usage_percpu");
        if (usagelist == null) {
            return null;
        }

        String list[] = usagelist.split(" ");
        long percpu[] = new long[list.length];
        for (int i = 0; i < list.length; i++) {
            percpu[i] = Long.parseLong(list[i]);
        }
        return percpu;
    }

    public long getCpuUserUsage() {
        return CgroupV1SubsystemController.getLongEntry(cpuacct, "cpuacct.stat", "user");
    }

    public long getCpuSystemUsage() {
        return CgroupV1SubsystemController.getLongEntry(cpuacct, "cpuacct.stat", "system");
    }


    /*****************************************************************
     * CPU Subsystem
     ****************************************************************/


    public long getCpuPeriod() {
        return getLongValue(cpu, "cpu.cfs_period_us");
    }

    public long getCpuQuota() {
        return getLongValue(cpu, "cpu.cfs_quota_us");
    }

    public long getCpuShares() {
        long retval = getLongValue(cpu, "cpu.shares");
        if (retval == 0 || retval == 1024)
            return CgroupSubsystem.LONG_RETVAL_UNLIMITED;
        else
            return retval;
    }

    public long getCpuNumPeriods() {
        return CgroupV1SubsystemController.getLongEntry(cpu, "cpu.stat", "nr_periods");
    }

    public long getCpuNumThrottled() {
        return CgroupV1SubsystemController.getLongEntry(cpu, "cpu.stat", "nr_throttled");
    }

    public long getCpuThrottledTime() {
        return CgroupV1SubsystemController.getLongEntry(cpu, "cpu.stat", "throttled_time");
    }

    public long getEffectiveCpuCount() {
        return Runtime.getRuntime().availableProcessors();
    }


    /*****************************************************************
     * CPUSet Subsystem
     ****************************************************************/

    public int[] getCpuSetCpus() {
        return CgroupSubsystemController.stringRangeToIntArray(CgroupSubsystemController.getStringValue(cpuset, "cpuset.cpus"));
    }

    public int[] getEffectiveCpuSetCpus() {
        return CgroupSubsystemController.stringRangeToIntArray(CgroupSubsystemController.getStringValue(cpuset, "cpuset.effective_cpus"));
    }

    public int[] getCpuSetMems() {
        return CgroupSubsystemController.stringRangeToIntArray(CgroupSubsystemController.getStringValue(cpuset, "cpuset.mems"));
    }

    public int[] getEffectiveCpuSetMems() {
        return CgroupSubsystemController.stringRangeToIntArray(CgroupSubsystemController.getStringValue(cpuset, "cpuset.effective_mems"));
    }

    public double getCpuSetMemoryPressure() {
        return CgroupV1SubsystemController.getDoubleValue(cpuset, "cpuset.memory_pressure");
    }

    public Boolean isCpuSetMemoryPressureEnabled() {
        long val = getLongValue(cpuset, "cpuset.memory_pressure_enabled");
        return (val == 1);
    }


    /*****************************************************************
     * Memory Subsystem
     ****************************************************************/


    public long getMemoryFailCount() {
        return getLongValue(memory, "memory.failcnt");
    }

    public long getMemoryLimit() {
        long retval = getLongValue(memory, "memory.limit_in_bytes");
        if (retval > CgroupV1SubsystemController.UNLIMITED_MIN) {
            if (memory.isHierarchical()) {
                // memory.limit_in_bytes returned unlimited, attempt
                // hierarchical memory limit
                String match = "hierarchical_memory_limit";
                retval = CgroupV1SubsystemController.getLongValueMatchingLine(memory,
                                                            "memory.stat",
                                                            match);
            }
        }
        return CgroupV1SubsystemController.longValOrUnlimited(retval);
    }

    public long getMemoryMaxUsage() {
        return getLongValue(memory, "memory.max_usage_in_bytes");
    }

    public long getMemoryUsage() {
        return getLongValue(memory, "memory.usage_in_bytes");
    }

    public long getKernelMemoryFailCount() {
        return getLongValue(memory, "memory.kmem.failcnt");
    }

    public long getKernelMemoryLimit() {
        return CgroupV1SubsystemController.longValOrUnlimited(getLongValue(memory, "memory.kmem.limit_in_bytes"));
    }

    public long getKernelMemoryMaxUsage() {
        return getLongValue(memory, "memory.kmem.max_usage_in_bytes");
    }

    public long getKernelMemoryUsage() {
        return getLongValue(memory, "memory.kmem.usage_in_bytes");
    }

    public long getTcpMemoryFailCount() {
        return getLongValue(memory, "memory.kmem.tcp.failcnt");
    }

    public long getTcpMemoryLimit() {
        return CgroupV1SubsystemController.longValOrUnlimited(getLongValue(memory, "memory.kmem.tcp.limit_in_bytes"));
    }

    public long getTcpMemoryMaxUsage() {
        return getLongValue(memory, "memory.kmem.tcp.max_usage_in_bytes");
    }

    public long getTcpMemoryUsage() {
        return getLongValue(memory, "memory.kmem.tcp.usage_in_bytes");
    }

    public long getMemoryAndSwapFailCount() {
        if (memory != null && !memory.isSwapEnabled()) {
            return getMemoryFailCount();
        }
        return getLongValue(memory, "memory.memsw.failcnt");
    }

    public long getMemoryAndSwapLimit() {
        if (memory != null && !memory.isSwapEnabled()) {
            return getMemoryLimit();
        }
        long retval = getLongValue(memory, "memory.memsw.limit_in_bytes");
        if (retval > CgroupV1SubsystemController.UNLIMITED_MIN) {
            if (memory.isHierarchical()) {
                // memory.memsw.limit_in_bytes returned unlimited, attempt
                // hierarchical memory limit
                String match = "hierarchical_memsw_limit";
                retval = CgroupV1SubsystemController.getLongValueMatchingLine(memory,
                                                            "memory.stat",
                                                            match);
            }
        }
        return CgroupV1SubsystemController.longValOrUnlimited(retval);
    }

    public long getMemoryAndSwapMaxUsage() {
        if (memory != null && !memory.isSwapEnabled()) {
            return getMemoryMaxUsage();
        }
        return getLongValue(memory, "memory.memsw.max_usage_in_bytes");
    }

    public long getMemoryAndSwapUsage() {
        if (memory != null && !memory.isSwapEnabled()) {
            return getMemoryUsage();
        }
        return getLongValue(memory, "memory.memsw.usage_in_bytes");
    }

    public Boolean isMemoryOOMKillEnabled() {
        long val = CgroupV1SubsystemController.getLongEntry(memory, "memory.oom_control", "oom_kill_disable");
        return (val == 0);
    }

    public long getMemorySoftLimit() {
        return CgroupV1SubsystemController.longValOrUnlimited(getLongValue(memory, "memory.soft_limit_in_bytes"));
    }

    /*****************************************************************
     *  pids subsystem
     ****************************************************************/
    public long getPidsMax() {
        String pidsMaxStr = CgroupSubsystemController.getStringValue(pids, "pids.max");
        return CgroupSubsystem.limitFromString(pidsMaxStr);
    }

    /*****************************************************************
     * BlKIO Subsystem
     ****************************************************************/


    public long getBlkIOServiceCount() {
        return CgroupV1SubsystemController.getLongEntry(blkio, "blkio.throttle.io_service_bytes", "Total");
    }

    public long getBlkIOServiced() {
        return CgroupV1SubsystemController.getLongEntry(blkio, "blkio.throttle.io_serviced", "Total");
    }

}
