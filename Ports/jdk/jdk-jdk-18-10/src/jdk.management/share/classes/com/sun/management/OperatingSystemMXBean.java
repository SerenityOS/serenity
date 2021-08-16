/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management;

/**
 * Platform-specific management interface for the operating system
 * on which the Java virtual machine is running.
 *
 * <p>
 * This interface provides information about the operating environment
 * on which the Java virtual machine is running. That might be a native
 * operating system, a virtualized operating system environment, or a
 * container-managed environment.
 *
 * <p>
 * The {@code OperatingSystemMXBean} object returned by
 * {@link java.lang.management.ManagementFactory#getOperatingSystemMXBean()}
 * is an instance of the implementation class of this interface
 * or {@link UnixOperatingSystemMXBean} interface depending on
 * its underlying operating system.
 *
 * @author  Mandy Chung
 * @since   1.5
 */
public interface OperatingSystemMXBean extends
    java.lang.management.OperatingSystemMXBean {

    /**
     * Returns the amount of virtual memory that is guaranteed to
     * be available to the running process in bytes,
     * or {@code -1} if this operation is not supported.
     *
     * @return the amount of virtual memory that is guaranteed to
     * be available to the running process in bytes,
     * or {@code -1} if this operation is not supported.
     */
    public long getCommittedVirtualMemorySize();

    /**
     * Returns the total amount of swap space in bytes.
     *
     * @return the total amount of swap space in bytes.
     */
    public long getTotalSwapSpaceSize();

    /**
     * Returns the amount of free swap space in bytes.
     *
     * @return the amount of free swap space in bytes.
     */
    public long getFreeSwapSpaceSize();

    /**
     * Returns the CPU time used by the process on which the Java
     * virtual machine is running in nanoseconds.  The returned value
     * is of nanoseconds precision but not necessarily nanoseconds
     * accuracy.  This method returns {@code -1} if the
     * the platform does not support this operation.
     *
     * @return the CPU time used by the process in nanoseconds,
     * or {@code -1} if this operation is not supported.
     */
    public long getProcessCpuTime();

    /**
     * Returns the amount of free physical memory in bytes.
     *
     * @deprecated Use {@link #getFreeMemorySize()} instead of
     * this historically named method.
     *
     * @implSpec This implementation must return the same value
     * as {@link #getFreeMemorySize()}.
     *
     * @return the amount of free physical memory in bytes.
     */
    @Deprecated(since="14")
    public default long getFreePhysicalMemorySize() { return getFreeMemorySize(); }

    /**
     * Returns the amount of free memory in bytes.
     *
     * @return the amount of free memory in bytes.
     * @since 14
     */
    public long getFreeMemorySize();

    /**
     * Returns the total amount of physical memory in bytes.
     *
     * @deprecated Use {@link #getTotalMemorySize()} instead of
     * this historically named method.
     *
     * @implSpec This implementation must return the same value
     * as {@link #getTotalMemorySize()}.
     *
     * @return the total amount of physical memory in  bytes.
     */
    @Deprecated(since="14")
    public default long getTotalPhysicalMemorySize() { return getTotalMemorySize(); }

    /**
     * Returns the total amount of memory in bytes.
     *
     * @return the total amount of memory in  bytes.
     * @since 14
     */
    public long getTotalMemorySize();

    /**
     * Returns the "recent cpu usage" for the whole system. This value is a
     * double in the [0.0,1.0] interval. A value of 0.0 means that all CPUs
     * were idle during the recent period of time observed, while a value
     * of 1.0 means that all CPUs were actively running 100% of the time
     * during the recent period being observed. All values betweens 0.0 and
     * 1.0 are possible depending of the activities going on in the system.
     * If the system recent cpu usage is not available, the method returns a
     * negative value.
     *
     * @deprecated Use {@link #getCpuLoad()} instead of
     * this historically named method.
     *
     * @implSpec This implementation must return the same value
     * as {@link #getCpuLoad()}.
     *
     * @return the "recent cpu usage" for the whole system; a negative
     * value if not available.
     * @since   1.7
     */
    @Deprecated(since="14")
    public default double getSystemCpuLoad() { return getCpuLoad(); }

    /**
     * Returns the "recent cpu usage" for the operating environment. This value
     * is a double in the [0.0,1.0] interval. A value of 0.0 means that all CPUs
     * were idle during the recent period of time observed, while a value
     * of 1.0 means that all CPUs were actively running 100% of the time
     * during the recent period being observed. All values betweens 0.0 and
     * 1.0 are possible depending of the activities going on.
     * If the recent cpu usage is not available, the method returns a
     * negative value.
     *
     * @return the "recent cpu usage" for the whole operating environment;
     * a negative value if not available.
     * @since 14
     */
    public double getCpuLoad();

    /**
     * Returns the "recent cpu usage" for the Java Virtual Machine process.
     * This value is a double in the [0.0,1.0] interval. A value of 0.0 means
     * that none of the CPUs were running threads from the JVM process during
     * the recent period of time observed, while a value of 1.0 means that all
     * CPUs were actively running threads from the JVM 100% of the time
     * during the recent period being observed. Threads from the JVM include
     * the application threads as well as the JVM internal threads. All values
     * betweens 0.0 and 1.0 are possible depending of the activities going on
     * in the JVM process and the whole system. If the Java Virtual Machine
     * recent CPU usage is not available, the method returns a negative value.
     *
     * @return the "recent cpu usage" for the Java Virtual Machine process;
     * a negative value if not available.
     * @since   1.7
     */
    public double getProcessCpuLoad();

}
