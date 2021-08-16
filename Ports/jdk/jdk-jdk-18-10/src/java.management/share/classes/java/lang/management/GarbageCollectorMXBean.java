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
 * The management interface for the garbage collection of
 * the Java virtual machine.  Garbage collection is the process
 * that the Java virtual machine uses to find and reclaim unreachable
 * objects to free up memory space.  A garbage collector is one type of
 * {@link MemoryManagerMXBean memory manager}.
 *
 * <p> A Java virtual machine may have one or more instances of
 * the implementation class of this interface.
 * An instance implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getGarbageCollectorMXBeans} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} method.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * a garbage collector within an MBeanServer is:
 * <blockquote>
 *   {@link ManagementFactory#GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
 *    java.lang:type=GarbageCollector}{@code ,name=}<i>collector's name</i>
 * </blockquote>
 *
 * It can be obtained by calling the
 * {@link PlatformManagedObject#getObjectName} method.
 *
 * A platform usually includes additional platform-dependent information
 * specific to a garbage collection algorithm for monitoring.
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
public interface GarbageCollectorMXBean extends MemoryManagerMXBean {
    /**
     * Returns the total number of collections that have occurred.
     * This method returns {@code -1} if the collection count is undefined for
     * this collector.
     *
     * @return the total number of collections that have occurred.
     */
    public long getCollectionCount();

    /**
     * Returns the approximate accumulated collection elapsed time
     * in milliseconds.  This method returns {@code -1} if the collection
     * elapsed time is undefined for this collector.
     * <p>
     * The Java virtual machine implementation may use a high resolution
     * timer to measure the elapsed time.  This method may return the
     * same value even if the collection count has been incremented
     * if the collection elapsed time is very short.
     *
     * @return the approximate accumulated collection elapsed time
     * in milliseconds.
     */
    public long getCollectionTime();


}
