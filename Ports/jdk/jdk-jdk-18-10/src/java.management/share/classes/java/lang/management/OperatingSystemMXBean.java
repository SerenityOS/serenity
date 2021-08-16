/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * The management interface for the operating system on which
 * the Java virtual machine is running.
 *
 * <p> A Java virtual machine has a single instance of the implementation
 * class of this interface.  This instance implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getOperatingSystemMXBean} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} method.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * the operating system within an MBeanServer is:
 * <blockquote>
 *    {@link ManagementFactory#OPERATING_SYSTEM_MXBEAN_NAME
 *      java.lang:type=OperatingSystem}
 * </blockquote>
 *
 * It can be obtained by calling the
 * {@link PlatformManagedObject#getObjectName} method.
 *
 * <p> This interface defines several convenient methods for accessing
 * system properties about the operating system on which the Java
 * virtual machine is running.
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
public interface OperatingSystemMXBean extends PlatformManagedObject {
    /**
     * Returns the operating system name.
     * This method is equivalent to {@code System.getProperty("os.name")}.
     *
     * @return the operating system name.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getName();

    /**
     * Returns the operating system architecture.
     * This method is equivalent to {@code System.getProperty("os.arch")}.
     *
     * @return the operating system architecture.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getArch();

    /**
     * Returns the operating system version.
     * This method is equivalent to {@code System.getProperty("os.version")}.
     *
     * @return the operating system version.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     <code>checkPropertiesAccess</code> method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getVersion();

    /**
     * Returns the number of processors available to the Java virtual machine.
     * This method is equivalent to the {@link Runtime#availableProcessors()}
     * method.
     * <p> This value may change during a particular invocation of
     * the virtual machine.
     *
     * @return  the number of processors available to the virtual
     *          machine; never smaller than one.
     */
    public int getAvailableProcessors();

    /**
     * Returns the system load average for the last minute.
     * The system load average is the sum of the number of runnable entities
     * queued to the {@linkplain #getAvailableProcessors available processors}
     * and the number of runnable entities running on the available processors
     * averaged over a period of time.
     * The way in which the load average is calculated is operating system
     * specific but is typically a damped time-dependent average.
     * <p>
     * If the load average is not available, a negative value is returned.
     * <p>
     * This method is designed to provide a hint about the system load
     * and may be queried frequently.
     * The load average may be unavailable on some platform where it is
     * expensive to implement this method.
     *
     * @return the system load average; or a negative value if not available.
     *
     * @since 1.6
     */
    public double getSystemLoadAverage();
}
