/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import sun.management.counter.Counter;
/**
 * An interface for the monitoring and management of the
 * Java virtual machine.
 */
public interface VMManagement {

    // Optional supports
    public boolean isCompilationTimeMonitoringSupported();
    public boolean isThreadContentionMonitoringSupported();
    public boolean isThreadContentionMonitoringEnabled();
    public boolean isCurrentThreadCpuTimeSupported();
    public boolean isOtherThreadCpuTimeSupported();
    public boolean isThreadCpuTimeEnabled();
    public boolean isBootClassPathSupported();
    public boolean isObjectMonitorUsageSupported();
    public boolean isSynchronizerUsageSupported();
    public boolean isThreadAllocatedMemorySupported();
    public boolean isThreadAllocatedMemoryEnabled();
    public boolean isGcNotificationSupported();
    public boolean isRemoteDiagnosticCommandsSupported();

    // Class Loading Subsystem
    public long    getTotalClassCount();
    public int     getLoadedClassCount();
    public long    getUnloadedClassCount();
    public boolean getVerboseClass();

    // Memory Subsystem
    public boolean getVerboseGC();

    // Runtime Subsystem
    public String  getManagementVersion();
    public String  getVmId();
    public String  getVmName();
    public String  getVmVendor();
    public String  getVmVersion();
    public String  getVmSpecName();
    public String  getVmSpecVendor();
    public String  getVmSpecVersion();
    public String  getClassPath();
    public String  getLibraryPath();
    public String  getBootClassPath();
    public List<String> getVmArguments();
    public long    getStartupTime();
    public long    getUptime();
    public int     getAvailableProcessors();

    // Compilation Subsystem
    public String  getCompilerName();
    public long    getTotalCompileTime();

    // Thread Subsystem
    public long    getTotalThreadCount();
    public int     getLiveThreadCount();
    public int     getPeakThreadCount();
    public int     getDaemonThreadCount();

    // Operating System
    public String  getOsName();
    public String  getOsArch();
    public String  getOsVersion();

    // Hotspot-specific Runtime support
    public long    getSafepointCount();
    public long    getTotalSafepointTime();
    public long    getSafepointSyncTime();
    public long    getTotalApplicationNonStoppedTime();

    public long    getLoadedClassSize();
    public long    getUnloadedClassSize();
    public long    getClassLoadingTime();
    public long    getMethodDataSize();
    public long    getInitializedClassCount();
    public long    getClassInitializationTime();
    public long    getClassVerificationTime();

    // Performance counter support
    public List<Counter>   getInternalCounters(String pattern);
}
