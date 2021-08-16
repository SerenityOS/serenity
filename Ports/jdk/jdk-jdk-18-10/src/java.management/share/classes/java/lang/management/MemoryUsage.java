/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.openmbean.CompositeData;
import sun.management.MemoryUsageCompositeData;

/**
 * A {@code MemoryUsage} object represents a snapshot of memory usage.
 * Instances of the {@code MemoryUsage} class are usually constructed
 * by methods that are used to obtain memory usage
 * information about individual memory pool of the Java virtual machine or
 * the heap or non-heap memory of the Java virtual machine as a whole.
 *
 * <p> A {@code MemoryUsage} object contains four values:
 * <table class="striped">
 * <caption style="display:none">Describes the MemoryUsage object content</caption>
 * <thead>
 * <tr><th scope="col">Value</th><th scope="col">Description</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr>
 * <th scope="row" style="vertical-align:top"> {@code init} </th>
 * <td style="vertical-align:top"> represents the initial amount of memory (in bytes) that
 *      the Java virtual machine requests from the operating system
 *      for memory management during startup.  The Java virtual machine
 *      may request additional memory from the operating system and
 *      may also release memory to the system over time.
 *      The value of {@code init} may be undefined.
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" style="vertical-align:top"> {@code used} </th>
 * <td style="vertical-align:top"> represents the amount of memory currently used (in bytes).
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" style="vertical-align:top"> {@code committed} </th>
 * <td style="vertical-align:top"> represents the amount of memory (in bytes) that is
 *      guaranteed to be available for use by the Java virtual machine.
 *      The amount of committed memory may change over time (increase
 *      or decrease).  The Java virtual machine may release memory to
 *      the system and {@code committed} could be less than {@code init}.
 *      {@code committed} will always be greater than
 *      or equal to {@code used}.
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" style="vertical-align:top"> {@code max} </th>
 * <td style="vertical-align:top"> represents the maximum amount of memory (in bytes)
 *      that can be used for memory management. Its value may be undefined.
 *      The maximum amount of memory may change over time if defined.
 *      The amount of used and committed memory will always be less than
 *      or equal to {@code max} if {@code max} is defined.
 *      A memory allocation may fail if it attempts to increase the
 *      used memory such that {@code used > committed} even
 *      if {@code used <= max} would still be true (for example,
 *      when the system is low on virtual memory).
 * </td>
 * </tr>
 * </tbody>
 * </table>
 *
 * Below is a picture showing an example of a memory pool:
 *
 * <pre>
 *        +----------------------------------------------+
 *        +////////////////           |                  +
 *        +////////////////           |                  +
 *        +----------------------------------------------+
 *
 *        |--------|
 *           init
 *        |---------------|
 *               used
 *        |---------------------------|
 *                  committed
 *        |----------------------------------------------|
 *                            max
 * </pre>
 *
 * <h2>MXBean Mapping</h2>
 * {@code MemoryUsage} is mapped to a {@link CompositeData CompositeData}
 * with attributes as specified in the {@link #from from} method.
 *
 * @author   Mandy Chung
 * @since   1.5
 */
public class MemoryUsage {
    private final long init;
    private final long used;
    private final long committed;
    private final long max;

    /**
     * Constructs a {@code MemoryUsage} object.
     *
     * @param init      the initial amount of memory in bytes that
     *                  the Java virtual machine allocates;
     *                  or {@code -1} if undefined.
     * @param used      the amount of used memory in bytes.
     * @param committed the amount of committed memory in bytes.
     * @param max       the maximum amount of memory in bytes that
     *                  can be used; or {@code -1} if undefined.
     *
     * @throws IllegalArgumentException if
     * <ul>
     * <li> the value of {@code init} or {@code max} is negative
     *      but not {@code -1}; or</li>
     * <li> the value of {@code used} or {@code committed} is negative;
     *      or</li>
     * <li> {@code used} is greater than the value of {@code committed};
     *      or</li>
     * <li> {@code committed} is greater than the value of {@code max}
     *      {@code max} if defined.</li>
     * </ul>
     */
    public MemoryUsage(long init,
                       long used,
                       long committed,
                       long max) {
        if (init < -1) {
            throw new IllegalArgumentException( "init parameter = " +
                init + " is negative but not -1.");
        }
        if (max < -1) {
            throw new IllegalArgumentException( "max parameter = " +
                max + " is negative but not -1.");
        }
        if (used < 0) {
            throw new IllegalArgumentException( "used parameter = " +
                used + " is negative.");
        }
        if (committed < 0) {
            throw new IllegalArgumentException( "committed parameter = " +
                committed + " is negative.");
        }
        if (used > committed) {
            throw new IllegalArgumentException( "used = " + used +
                " should be <= committed = " + committed);
        }
        if (max >= 0 && committed > max) {
            throw new IllegalArgumentException( "committed = " + committed +
                " should be < max = " + max);
        }

        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    /**
     * Constructs a {@code MemoryUsage} object from a
     * {@link CompositeData CompositeData}.
     */
    private MemoryUsage(CompositeData cd) {
        // validate the input composite data
        MemoryUsageCompositeData.validateCompositeData(cd);

        this.init = MemoryUsageCompositeData.getInit(cd);
        this.used = MemoryUsageCompositeData.getUsed(cd);
        this.committed = MemoryUsageCompositeData.getCommitted(cd);
        this.max = MemoryUsageCompositeData.getMax(cd);
    }

    /**
     * Returns the amount of memory in bytes that the Java virtual machine
     * initially requests from the operating system for memory management.
     * This method returns {@code -1} if the initial memory size is undefined.
     *
     * @return the initial size of memory in bytes;
     * {@code -1} if undefined.
     */
    public long getInit() {
        return init;
    }

    /**
     * Returns the amount of used memory in bytes.
     *
     * @return the amount of used memory in bytes.
     *
     */
    public long getUsed() {
        return used;
    };

    /**
     * Returns the amount of memory in bytes that is committed for
     * the Java virtual machine to use.  This amount of memory is
     * guaranteed for the Java virtual machine to use.
     *
     * @return the amount of committed memory in bytes.
     *
     */
    public long getCommitted() {
        return committed;
    };

    /**
     * Returns the maximum amount of memory in bytes that can be
     * used for memory management.  This method returns {@code -1}
     * if the maximum memory size is undefined.
     *
     * <p> This amount of memory is not guaranteed to be available
     * for memory management if it is greater than the amount of
     * committed memory.  The Java virtual machine may fail to allocate
     * memory even if the amount of used memory does not exceed this
     * maximum size.
     *
     * @return the maximum amount of memory in bytes;
     * {@code -1} if undefined.
     */
    public long getMax() {
        return max;
    };

    /**
     * Returns a descriptive representation of this memory usage.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("init = " + init + "(" + (init >> 10) + "K) ");
        buf.append("used = " + used + "(" + (used >> 10) + "K) ");
        buf.append("committed = " + committed + "(" +
                   (committed >> 10) + "K) " );
        buf.append("max = " + max + "(" + (max >> 10) + "K)");
        return buf.toString();
    }

    /**
     * Returns a {@code MemoryUsage} object represented by the
     * given {@code CompositeData}. The given {@code CompositeData}
     * must contain the following attributes:
     *
     * <table class="striped" style="margin-left:2em;">
     * <caption style="display:none">The attributes and the types the given CompositeData contains</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Attribute Name</th>
     *   <th scope="col">Type</th>
     * </tr>
     * </thead>
     * <tbody style="text-align:left">
     * <tr>
     *   <th scope="row">init</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">used</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">committed</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">max</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param cd {@code CompositeData} representing a {@code MemoryUsage}
     *
     * @throws IllegalArgumentException if {@code cd} does not
     *   represent a {@code MemoryUsage} with the attributes described
     *   above.
     *
     * @return a {@code MemoryUsage} object represented by {@code cd}
     *         if {@code cd} is not {@code null};
     *         {@code null} otherwise.
     */
    public static MemoryUsage from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

        if (cd instanceof MemoryUsageCompositeData) {
            return ((MemoryUsageCompositeData) cd).getMemoryUsage();
        } else {
            return new MemoryUsage(cd);
        }

    }
}
