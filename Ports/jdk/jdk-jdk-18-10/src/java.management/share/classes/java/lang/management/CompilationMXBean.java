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

package java.lang.management;

/**
 * The management interface for the compilation system of
 * the Java virtual machine.
 *
 * <p> A Java virtual machine has a single instance of the implementation
 * class of this interface.  This instance implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getCompilationMXBean} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} method.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * the compilation system within an MBeanServer is:
 * <blockquote>
 *  {@link ManagementFactory#COMPILATION_MXBEAN_NAME
 *         java.lang:type=Compilation}
 * </blockquote>
 *
 * It can be obtained by calling the
 * {@link PlatformManagedObject#getObjectName} method.
 *
 * @see ManagementFactory#getPlatformMXBeans(Class)
 * @see <a href="../../../javax/management/package-summary.html">
 *      JMX Specification.</a>
 * @see <a href="package-summary.html#examples">
 *      Ways to Access MXBeans</a>
 *
 * @author  Mandy Chung
 * @since   1.5
 */
public interface CompilationMXBean extends PlatformManagedObject {
    /**
     * Returns the name of the Just-in-time (JIT) compiler.
     *
     * @return the name of the JIT compiler.
     */
    public java.lang.String    getName();

    /**
     * Tests if the Java virtual machine supports the monitoring of
     * compilation time.
     *
     * @return {@code true} if the monitoring of compilation time is
     * supported; {@code false} otherwise.
     */
    public boolean isCompilationTimeMonitoringSupported();

    /**
     * Returns the approximate accumulated elapsed time (in milliseconds)
     * spent in compilation.
     * If multiple threads are used for compilation, this value is
     * summation of the approximate time that each thread spent in compilation.
     *
     * <p>This method is optionally supported by the platform.
     * A Java virtual machine implementation may not support the compilation
     * time monitoring. The {@link #isCompilationTimeMonitoringSupported}
     * method can be used to determine if the Java virtual machine
     * supports this operation.
     *
     * <p> This value does not indicate the level of performance of
     * the Java virtual machine and is not intended for performance comparisons
     * of different virtual machine implementations.
     * The implementations may have different definitions and different
     * measurements of the compilation time.
     *
     * @return Compilation time in milliseconds
     * @throws java.lang.UnsupportedOperationException if the Java
     * virtual machine does not support
     * this operation.
     *
     */
    public long                getTotalCompilationTime();
}
