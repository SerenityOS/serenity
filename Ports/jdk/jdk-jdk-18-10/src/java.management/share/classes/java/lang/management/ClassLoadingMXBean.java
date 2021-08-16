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

package java.lang.management;

/**
 * The management interface for the class loading system of
 * the Java virtual machine.
 *
 * <p> A Java virtual machine has a single instance of the implementation
 * class of this interface.  This instance implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getClassLoadingMXBean} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer}.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * the class loading system within an {@code MBeanServer} is:
 * <blockquote>
 * {@link ManagementFactory#CLASS_LOADING_MXBEAN_NAME
 *        java.lang:type=ClassLoading}
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
public interface ClassLoadingMXBean extends PlatformManagedObject {

    /**
     * Returns the total number of classes that have been loaded since
     * the Java virtual machine has started execution.
     *
     * @return the total number of classes loaded.
     *
     */
    public long getTotalLoadedClassCount();

    /**
     * Returns the number of classes that are currently loaded in the
     * Java virtual machine.
     *
     * @return the number of currently loaded classes.
     */
    public int getLoadedClassCount();

    /**
     * Returns the total number of classes unloaded since the Java virtual machine
     * has started execution.
     *
     * @return the total number of unloaded classes.
     */
    public long getUnloadedClassCount();

    /**
     * Tests if the verbose output for the class loading system is enabled.
     *
     * @return {@code true} if the verbose output for the class loading
     * system is enabled; {@code false} otherwise.
     */
    public boolean isVerbose();

    /**
     * Enables or disables the verbose output for the class loading
     * system.  The verbose output information and the output stream
     * to which the verbose information is emitted are implementation
     * dependent.  Typically, a Java virtual machine implementation
     * prints a message each time a class file is loaded.
     *
     * <p>This method can be called by multiple threads concurrently.
     * Each invocation of this method enables or disables the verbose
     * output globally.
     *
     * @param value {@code true} to enable the verbose output;
     *              {@code false} to disable.
     *
     * @throws  java.lang.SecurityException if a security manager
     *          exists and the caller does not have
     *          ManagementPermission("control").
     */
    public void setVerbose(boolean value);

}
