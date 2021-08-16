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
 * The management interface for a memory manager.
 * A memory manager manages one or more memory pools of the
 * Java virtual machine.
 *
 * <p> A Java virtual machine has one or more memory managers.
 * An instance implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getMemoryManagerMXBeans} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} method.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * a memory manager within an MBeanServer is:
 * <blockquote>
 *   {@link ManagementFactory#MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
 *    java.lang:type=MemoryManager}{@code ,name=}<i>manager's name</i>
 * </blockquote>
 *
 * It can be obtained by calling the
 * {@link PlatformManagedObject#getObjectName} method.
 *
 * @see ManagementFactory#getPlatformMXBeans(Class)
 * @see MemoryMXBean
 *
 * @see <a href="../../../javax/management/package-summary.html">
 *      JMX Specification.</a>
 * @see <a href="package-summary.html#examples">
 *      Ways to Access MXBeans</a>
 *
 * @author  Mandy Chung
 * @since   1.5
 */
public interface MemoryManagerMXBean extends PlatformManagedObject {
    /**
     * Returns the name representing this memory manager.
     *
     * @return the name of this memory manager.
     */
    public String getName();

    /**
     * Tests if this memory manager is valid in the Java virtual
     * machine.  A memory manager becomes invalid once the Java virtual
     * machine removes it from the memory system.
     *
     * @return {@code true} if the memory manager is valid in the
     *               Java virtual machine;
     *         {@code false} otherwise.
     */
    public boolean isValid();

    /**
     * Returns the name of memory pools that this memory manager manages.
     *
     * @return an array of {@code String} objects, each is
     * the name of a memory pool that this memory manager manages.
     */
    public String[] getMemoryPoolNames();
}
