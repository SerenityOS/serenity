/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The management interface for the runtime system of
 * the Java virtual machine.
 *
 * <p> A Java virtual machine has a single instance of the implementation
 * class of this interface.  This instance implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getRuntimeMXBean} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} method.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * the runtime system within an MBeanServer is:
 * <blockquote>
 *    {@link ManagementFactory#RUNTIME_MXBEAN_NAME
 *           java.lang:type=Runtime}
 * </blockquote>
 *
 * It can be obtained by calling the
 * {@link PlatformManagedObject#getObjectName} method.
 *
 * <p> This interface defines several convenient methods for accessing
 * system properties about the Java virtual machine.
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
public interface RuntimeMXBean extends PlatformManagedObject {
    /**
     * Returns the {@linkplain ProcessHandle#pid process ID} representing
     * the running Java virtual machine.
     *
     * @implSpec The default implementation returns {@link ProcessHandle#pid process ID}
     * of the {@linkplain ProcessHandle#current current process}.
     *
     * @return the process ID representing the running Java virtual machine.
     *
     * @since 10
     */
    @SuppressWarnings("removal")
    public default long getPid() {
        return AccessController.doPrivileged((PrivilegedAction<Long>)
                () -> ProcessHandle.current().pid());
    }

    /**
     * Returns the name representing the running Java virtual machine.
     * The returned name string can be any arbitrary string and
     * a Java virtual machine implementation can choose
     * to embed platform-specific useful information in the
     * returned name string.  Each running virtual machine could have
     * a different name.
     *
     * @return the name representing the running Java virtual machine.
     */
    public String getName();

    /**
     * Returns the Java virtual machine implementation name.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.name")}.
     *
     * @return the Java virtual machine implementation name.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getVmName();

    /**
     * Returns the Java virtual machine implementation vendor.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.vendor")}.
     *
     * @return the Java virtual machine implementation vendor.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getVmVendor();

    /**
     * Returns the Java virtual machine implementation version.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.version")}.
     *
     * @return the Java virtual machine implementation version.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getVmVersion();

    /**
     * Returns the Java virtual machine specification name.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.specification.name")}.
     *
     * @return the Java virtual machine specification name.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getSpecName();

    /**
     * Returns the Java virtual machine specification vendor.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.specification.vendor")}.
     *
     * @return the Java virtual machine specification vendor.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getSpecVendor();

    /**
     * Returns the Java virtual machine specification version.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.vm.specification.version")}.
     *
     * @return the Java virtual machine specification version.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getSpecVersion();


    /**
     * Returns the version of the specification for the management interface
     * implemented by the running Java virtual machine.
     *
     * @return the version of the specification for the management interface
     * implemented by the running Java virtual machine.
     */
    public String getManagementSpecVersion();

    /**
     * Returns the Java class path that is used by the system class loader
     * to search for class files.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.class.path")}.
     *
     * <p> Multiple paths in the Java class path are separated by the
     * path separator character of the platform of the Java virtual machine
     * being monitored.
     *
     * @return the Java class path.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getClassPath();

    /**
     * Returns the Java library path.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.library.path")}.
     *
     * <p> Multiple paths in the Java library path are separated by the
     * path separator character of the platform of the Java virtual machine
     * being monitored.
     *
     * @return the Java library path.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to this system property.
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see java.lang.System#getProperty
     */
    public String getLibraryPath();

    /**
     * Tests if the Java virtual machine supports the boot class path
     * mechanism used by the bootstrap class loader to search for class
     * files.
     *
     * @return {@code true} if the Java virtual machine supports the
     * class path mechanism; {@code false} otherwise.
     */
    public boolean isBootClassPathSupported();

    /**
     * Returns the boot class path that is used by the bootstrap class loader
     * to search for class files.
     *
     * <p> Multiple paths in the boot class path are separated by the
     * path separator character of the platform on which the Java
     * virtual machine is running.
     *
     * <p>A Java virtual machine implementation may not support
     * the boot class path mechanism for the bootstrap class loader
     * to search for class files.
     * The {@link #isBootClassPathSupported} method can be used
     * to determine if the Java virtual machine supports this method.
     *
     * @return the boot class path.
     *
     * @throws java.lang.UnsupportedOperationException
     *     if the Java virtual machine does not support this operation.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and the caller does not have
     *     ManagementPermission("monitor").
     */
    public String getBootClassPath();

    /**
     * Returns the input arguments passed to the Java virtual machine
     * which does not include the arguments to the {@code main} method.
     * This method returns an empty list if there is no input argument
     * to the Java virtual machine.
     * <p>
     * Some Java virtual machine implementations may take input arguments
     * from multiple different sources: for examples, arguments passed from
     * the application that launches the Java virtual machine such as
     * the 'java' command, environment variables, configuration files, etc.
     * <p>
     * Typically, not all command-line options to the 'java' command
     * are passed to the Java virtual machine.
     * Thus, the returned input arguments may not
     * include all command-line options.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code List<String>} is {@code String[]}.
     *
     * @return a list of {@code String} objects; each element
     * is an argument passed to the Java virtual machine.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and the caller does not have
     *     ManagementPermission("monitor").
     */
    public java.util.List<String> getInputArguments();

    /**
     * Returns the uptime of the Java virtual machine in milliseconds.
     *
     * @return uptime of the Java virtual machine in milliseconds.
     */
    public long getUptime();

    /**
     * Returns the start time of the Java virtual machine in milliseconds.
     * This method returns the approximate time when the Java virtual
     * machine started.
     *
     * @return start time of the Java virtual machine in milliseconds.
     *
     */
    public long getStartTime();

    /**
     * Returns a map of names and values of all system properties.
     * This method calls {@link System#getProperties} to get all
     * system properties.  Properties whose name or value is not
     * a {@code String} are omitted.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code Map<String,String>} is
     * {@link javax.management.openmbean.TabularData TabularData}
     * with two items in each row as follows:
     * <table class="striped" style="margin-left:2em">
     * <caption style="display:none">Name and Type for each item</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Item Name</th>
     *   <th scope="col">Item Type</th>
     *   </tr>
     * </thead>
     * <tbody>
     * <tr style="text-align:left">
     *   <th scope="row">{@code key}</th>
     *   <td>{@code String}</td>
     *   </tr>
     * <tr>
     *   <th scope="row">{@code value}</th>
     *   <td>{@code String}</td>
     *   </tr>
     * </tbody>
     * </table>
     *
     * @return a map of names and values of all system properties.
     *
     * @throws  java.lang.SecurityException
     *     if a security manager exists and its
     *     {@code checkPropertiesAccess} method doesn't allow access
     *     to the system properties.
     */
    public java.util.Map<String, String> getSystemProperties();
}
