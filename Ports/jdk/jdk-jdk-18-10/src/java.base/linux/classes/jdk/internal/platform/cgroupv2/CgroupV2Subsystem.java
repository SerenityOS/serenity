/*
 * Copyright (c) 2020, 2021, Red Hat Inc.
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

package jdk.internal.platform.cgroupv2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jdk.internal.platform.CgroupInfo;
import jdk.internal.platform.CgroupSubsystem;
import jdk.internal.platform.CgroupSubsystemController;
import jdk.internal.platform.CgroupUtil;

public class CgroupV2Subsystem implements CgroupSubsystem {

    private static volatile CgroupV2Subsystem INSTANCE;
    private static final long[] LONG_ARRAY_NOT_SUPPORTED = null;
    private static final int[] INT_ARRAY_UNAVAILABLE = null;
    private final CgroupSubsystemController unified;
    private static final String PROVIDER_NAME = "cgroupv2";
    private static final int PER_CPU_SHARES = 1024;
    private static final Object EMPTY_STR = "";
    private static final long NO_SWAP = 0;

    private CgroupV2Subsystem(CgroupSubsystemController unified) {
        this.unified = unified;
    }

    private long getLongVal(String file, long defaultValue) {
        return CgroupSubsystemController.getLongValue(unified,
                                                      file,
                                                      CgroupV2SubsystemController::convertStringToLong,
                                                      defaultValue);
    }

    private long getLongVal(String file) {
        return getLongVal(file, CgroupSubsystem.LONG_RETVAL_UNLIMITED);
    }

    /**
     * Get the singleton instance of a cgroups v2 subsystem. On initialization,
     * a new object from the given cgroup information 'anyController' is being
     * created. Note that the cgroup information has been parsed from cgroup
     * interface files ahead of time.
     *
     * See CgroupSubsystemFactory.determineType() for the cgroup interface
     * files parsing logic.
     *
     * @return A singleton CgroupSubsystem instance, never null.
     */
    public static CgroupSubsystem getInstance(CgroupInfo anyController) {
        if (INSTANCE == null) {
            CgroupSubsystemController unified = new CgroupV2SubsystemController(
                    anyController.getMountPoint(),
                    anyController.getCgroupPath());
            CgroupV2Subsystem tmpCgroupSystem = new CgroupV2Subsystem(unified);
            synchronized (CgroupV2Subsystem.class) {
                if (INSTANCE == null) {
                    INSTANCE = tmpCgroupSystem;
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public long getCpuUsage() {
        long micros = CgroupV2SubsystemController.getLongEntry(unified, "cpu.stat", "usage_usec");
        if (micros < 0) {
            return micros;
        }
        return TimeUnit.MICROSECONDS.toNanos(micros);
    }

    @Override
    public long[] getPerCpuUsage() {
        return LONG_ARRAY_NOT_SUPPORTED;
    }

    @Override
    public long getCpuUserUsage() {
        long micros = CgroupV2SubsystemController.getLongEntry(unified, "cpu.stat", "user_usec");
        if (micros < 0) {
            return micros;
        }
        return TimeUnit.MICROSECONDS.toNanos(micros);
    }

    @Override
    public long getCpuSystemUsage() {
        long micros = CgroupV2SubsystemController.getLongEntry(unified, "cpu.stat", "system_usec");
        if (micros < 0) {
            return micros;
        }
        return TimeUnit.MICROSECONDS.toNanos(micros);
    }

    @Override
    public long getCpuPeriod() {
        return getFromCpuMax(1 /* $PERIOD index */);
    }

    @Override
    public long getCpuQuota() {
        return getFromCpuMax(0 /* $MAX index */);
    }

    private long getFromCpuMax(int tokenIdx) {
        String cpuMaxRaw = CgroupSubsystemController.getStringValue(unified, "cpu.max");
        if (cpuMaxRaw == null) {
            // likely file not found
            return CgroupSubsystem.LONG_RETVAL_UNLIMITED;
        }
        // $MAX $PERIOD
        String[] tokens = cpuMaxRaw.split("\\s+");
        if (tokens.length != 2) {
            return CgroupSubsystem.LONG_RETVAL_UNLIMITED;
        }
        String quota = tokens[tokenIdx];
        return CgroupSubsystem.limitFromString(quota);
    }

    @Override
    public long getCpuShares() {
        long sharesRaw = getLongVal("cpu.weight");
        if (sharesRaw == 100 || sharesRaw <= 0) {
            return CgroupSubsystem.LONG_RETVAL_UNLIMITED;
        }
        int shares = (int)sharesRaw;
        // CPU shares (OCI) value needs to get translated into
        // a proper Cgroups v2 value. See:
        // https://github.com/containers/crun/blob/master/crun.1.md#cpu-controller
        //
        // Use the inverse of (x == OCI value, y == cgroupsv2 value):
        // ((262142 * y - 1)/9999) + 2 = x
        //
        int x = 262142 * shares - 1;
        double frac = x/9999.0;
        x = ((int)frac) + 2;
        if ( x <= PER_CPU_SHARES ) {
            return PER_CPU_SHARES; // mimic cgroups v1
        }
        int f = x/PER_CPU_SHARES;
        int lower_multiple = f * PER_CPU_SHARES;
        int upper_multiple = (f + 1) * PER_CPU_SHARES;
        int distance_lower = Math.max(lower_multiple, x) - Math.min(lower_multiple, x);
        int distance_upper = Math.max(upper_multiple, x) - Math.min(upper_multiple, x);
        x = distance_lower <= distance_upper ? lower_multiple : upper_multiple;
        return x;
    }

    @Override
    public long getCpuNumPeriods() {
        return CgroupV2SubsystemController.getLongEntry(unified, "cpu.stat", "nr_periods");
    }

    @Override
    public long getCpuNumThrottled() {
        return CgroupV2SubsystemController.getLongEntry(unified, "cpu.stat", "nr_throttled");
    }

    @Override
    public long getCpuThrottledTime() {
        long micros = CgroupV2SubsystemController.getLongEntry(unified, "cpu.stat", "throttled_usec");
        if (micros < 0) {
            return micros;
        }
        return TimeUnit.MICROSECONDS.toNanos(micros);
    }

    @Override
    public long getEffectiveCpuCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public int[] getCpuSetCpus() {
        String cpuSetVal = CgroupSubsystemController.getStringValue(unified, "cpuset.cpus");
        return getCpuSet(cpuSetVal);
    }

    @Override
    public int[] getEffectiveCpuSetCpus() {
        String effCpuSetVal = CgroupSubsystemController.getStringValue(unified, "cpuset.cpus.effective");
        return getCpuSet(effCpuSetVal);
    }

    @Override
    public int[] getCpuSetMems() {
        String cpuSetMems = CgroupSubsystemController.getStringValue(unified, "cpuset.mems");
        return getCpuSet(cpuSetMems);
    }

    @Override
    public int[] getEffectiveCpuSetMems() {
        String effCpuSetMems = CgroupSubsystemController.getStringValue(unified, "cpuset.mems.effective");
        return getCpuSet(effCpuSetMems);
    }

    private int[] getCpuSet(String cpuSetVal) {
        if (cpuSetVal == null || EMPTY_STR.equals(cpuSetVal)) {
            return INT_ARRAY_UNAVAILABLE;
        }
        return CgroupSubsystemController.stringRangeToIntArray(cpuSetVal);
    }

    @Override
    public long getMemoryFailCount() {
        return CgroupV2SubsystemController.getLongEntry(unified, "memory.events", "max");
    }

    @Override
    public long getMemoryLimit() {
        String strVal = CgroupSubsystemController.getStringValue(unified, "memory.max");
        return CgroupSubsystem.limitFromString(strVal);
    }

    @Override
    public long getMemoryUsage() {
        return getLongVal("memory.current");
    }

    @Override
    public long getTcpMemoryUsage() {
        return CgroupV2SubsystemController.getLongEntry(unified, "memory.stat", "sock");
    }

    /**
     * Note that for cgroups v2 the actual limits set for swap and
     * memory live in two different files, memory.swap.max and memory.max
     * respectively. In order to properly report a cgroup v1 like
     * compound value we need to sum the two values. Setting a swap limit
     * without also setting a memory limit is not allowed.
     */
    @Override
    public long getMemoryAndSwapLimit() {
        String strVal = CgroupSubsystemController.getStringValue(unified, "memory.swap.max");
        // We only get a null string when file memory.swap.max doesn't exist.
        // In that case we return the memory limit without any swap.
        if (strVal == null) {
            return getMemoryLimit();
        }
        long swapLimit = CgroupSubsystem.limitFromString(strVal);
        if (swapLimit >= 0) {
            long memoryLimit = getMemoryLimit();
            assert memoryLimit >= 0;
            return memoryLimit + swapLimit;
        }
        return swapLimit;
    }

    /**
     * Note that for cgroups v2 the actual values set for swap usage and
     * memory usage live in two different files, memory.current and memory.swap.current
     * respectively. In order to properly report a cgroup v1 like
     * compound value we need to sum the two values. Setting a swap limit
     * without also setting a memory limit is not allowed.
     */
    @Override
    public long getMemoryAndSwapUsage() {
        long memoryUsage = getMemoryUsage();
        if (memoryUsage >= 0) {
            // If file memory.swap.current doesn't exist, only return the regular
            // memory usage (without swap). Thus, use default value of NO_SWAP.
            long swapUsage = getLongVal("memory.swap.current", NO_SWAP);
            return memoryUsage + swapUsage;
        }
        return memoryUsage; // case of no memory limits
    }

    @Override
    public long getMemorySoftLimit() {
        String softLimitStr = CgroupSubsystemController.getStringValue(unified, "memory.low");
        return CgroupSubsystem.limitFromString(softLimitStr);
    }

    @Override
    public long getPidsMax() {
        String pidsMaxStr = CgroupSubsystemController.getStringValue(unified, "pids.max");
        return CgroupSubsystem.limitFromString(pidsMaxStr);
    }

    @Override
    public long getBlkIOServiceCount() {
        return sumTokensIOStat(CgroupV2Subsystem::lineToRandWIOs);
    }


    @Override
    public long getBlkIOServiced() {
        return sumTokensIOStat(CgroupV2Subsystem::lineToRBytesAndWBytesIO);
    }

    private long sumTokensIOStat(Function<String, Long> mapFunc) {
        try {
            return CgroupUtil.readFilePrivileged(Paths.get(unified.path(), "io.stat"))
                                .map(mapFunc)
                                .collect(Collectors.summingLong(e -> e));
        } catch (UncheckedIOException e) {
            return CgroupSubsystem.LONG_RETVAL_UNLIMITED;
        } catch (IOException e) {
            return CgroupSubsystem.LONG_RETVAL_UNLIMITED;
        }
    }

    private static String[] getRWIOMatchTokenNames() {
        return new String[] { "rios", "wios" };
    }

    private static String[] getRWBytesIOMatchTokenNames() {
        return new String[] { "rbytes", "wbytes" };
    }

    public static Long lineToRandWIOs(String line) {
        String[] matchNames = getRWIOMatchTokenNames();
        return ioStatLineToLong(line, matchNames);
    }

    public static Long lineToRBytesAndWBytesIO(String line) {
        String[] matchNames = getRWBytesIOMatchTokenNames();
        return ioStatLineToLong(line, matchNames);
    }

    private static Long ioStatLineToLong(String line, String[] matchNames) {
        if (line == null || EMPTY_STR.equals(line)) {
            return Long.valueOf(0);
        }
        String[] tokens = line.split("\\s+");
        long retval = 0;
        for (String t: tokens) {
            String[] valKeys = t.split("=");
            if (valKeys.length != 2) {
                // ignore device ids $MAJ:$MIN
                continue;
            }
            for (String match: matchNames) {
                if (match.equals(valKeys[0])) {
                    retval += longOrZero(valKeys[1]);
                }
            }
        }
        return Long.valueOf(retval);
    }

    private static long longOrZero(String val) {
        long lVal = 0;
        try {
            lVal = Long.parseLong(val);
        } catch (NumberFormatException e) {
            // keep at 0
        }
        return lVal;
    }
}
