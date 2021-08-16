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

package com.sun.management;

import java.lang.management.MemoryUsage;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.sun.management.internal.GcInfoCompositeData;
import com.sun.management.internal.GcInfoBuilder;

/**
 * Garbage collection information.  It contains the following
 * information for one garbage collection as well as GC-specific
 * attributes:
 * <blockquote>
 * <ul>
 *   <li>Start time</li>
 *   <li>End time</li>
 *   <li>Duration</li>
 *   <li>Memory usage before the collection starts</li>
 *   <li>Memory usage after the collection ends</li>
 * </ul>
 * </blockquote>
 *
 * <p>
 * {@code GcInfo} is a {@link CompositeData CompositeData}
 * The GC-specific attributes can be obtained via the CompositeData
 * interface.  This is a historical relic, and other classes should
 * not copy this pattern.  Use {@link CompositeDataView} instead.
 *
 * <h2>MXBean Mapping</h2>
 * {@code GcInfo} is mapped to a {@link CompositeData CompositeData}
 * with attributes as specified in the {@link #from from} method.
 *
 * @author  Mandy Chung
 * @since   1.5
 */
public class GcInfo implements CompositeData, CompositeDataView {
    private final long index;
    private final long startTime;
    private final long endTime;
    private final Map<String, MemoryUsage> usageBeforeGc;
    private final Map<String, MemoryUsage> usageAfterGc;
    private final Object[] extAttributes;
    private final CompositeData cdata;
    private final GcInfoBuilder builder;

    private GcInfo(GcInfoBuilder builder,
                   long index, long startTime, long endTime,
                   MemoryUsage[] muBeforeGc,
                   MemoryUsage[] muAfterGc,
                   Object[] extAttributes) {
        this.builder       = builder;
        this.index         = index;
        this.startTime     = startTime;
        this.endTime       = endTime;
        String[] poolNames = builder.getPoolNames();
        this.usageBeforeGc = new HashMap<String, MemoryUsage>(poolNames.length);
        this.usageAfterGc = new HashMap<String, MemoryUsage>(poolNames.length);
        for (int i = 0; i < poolNames.length; i++) {
            this.usageBeforeGc.put(poolNames[i],  muBeforeGc[i]);
            this.usageAfterGc.put(poolNames[i],  muAfterGc[i]);
        }
        this.extAttributes = extAttributes;
        this.cdata = new GcInfoCompositeData(this, builder, extAttributes);
    }

    private GcInfo(CompositeData cd) {
        GcInfoCompositeData.validateCompositeData(cd);

        this.index         = GcInfoCompositeData.getId(cd);
        this.startTime     = GcInfoCompositeData.getStartTime(cd);
        this.endTime       = GcInfoCompositeData.getEndTime(cd);
        this.usageBeforeGc = GcInfoCompositeData.getMemoryUsageBeforeGc(cd);
        this.usageAfterGc  = GcInfoCompositeData.getMemoryUsageAfterGc(cd);
        this.extAttributes = null;
        this.builder       = null;
        this.cdata         = cd;
    }

    /**
     * Returns the identifier of this garbage collection which is
     * the number of collections that this collector has done.
     *
     * @return the identifier of this garbage collection which is
     * the number of collections that this collector has done.
     */
    public long getId() {
        return index;
    }

    /**
     * Returns the start time of this GC in milliseconds
     * since the Java virtual machine was started.
     *
     * @return the start time of this GC.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time of this GC in milliseconds
     * since the Java virtual machine was started.
     *
     * @return the end time of this GC.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Returns the elapsed time of this GC in milliseconds.
     *
     * @return the elapsed time of this GC in milliseconds.
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * Returns the memory usage of all memory pools
     * at the beginning of this GC.
     * This method returns
     * a {@code Map} of the name of a memory pool
     * to the memory usage of the corresponding
     * memory pool before GC starts.
     *
     * @return a {@code Map} of memory pool names to the memory
     * usage of a memory pool before GC starts.
     */
    public Map<String, MemoryUsage> getMemoryUsageBeforeGc() {
        return Collections.unmodifiableMap(usageBeforeGc);
    }

    /**
     * Returns the memory usage of all memory pools
     * at the end of this GC.
     * This method returns
     * a {@code Map} of the name of a memory pool
     * to the memory usage of the corresponding
     * memory pool when GC finishes.
     *
     * @return a {@code Map} of memory pool names to the memory
     * usage of a memory pool when GC finishes.
     */
    public Map<String, MemoryUsage> getMemoryUsageAfterGc() {
        return Collections.unmodifiableMap(usageAfterGc);
    }

   /**
     * Returns a {@code GcInfo} object represented by the
     * given {@code CompositeData}. The given
     * {@code CompositeData} must contain
     * all the following attributes:
     *
     * <blockquote>
     * <table class="striped"><caption style="display:none">description</caption>
     * <thead>
     * <tr>
     *   <th scope="col" style="text-align:left">Attribute Name</th>
     *   <th scope="col" style="text-align:left">Type</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row">index</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">startTime</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">endTime</th>
     *   <td>{@code java.lang.Long}</td>
     * </tr>
     * <tr>
     *   <th scope="row">memoryUsageBeforeGc</th>
     *   <td>{@code javax.management.openmbean.TabularData}</td>
     * </tr>
     * <tr>
     *   <th scope="row">memoryUsageAfterGc</th>
     *   <td>{@code javax.management.openmbean.TabularData}</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @throws IllegalArgumentException if {@code cd} does not
     *   represent a {@code GcInfo} object with the attributes
     *   described above.
     *
     * @return a {@code GcInfo} object represented by {@code cd}
     * if {@code cd} is not {@code null}; {@code null} otherwise.
     */
    public static GcInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

        if (cd instanceof GcInfoCompositeData) {
            return ((GcInfoCompositeData) cd).getGcInfo();
        } else {
            return new GcInfo(cd);
        }

    }

    // Implementation of the CompositeData interface
    public boolean containsKey(String key) {
        return cdata.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return cdata.containsValue(value);
    }

    public boolean equals(Object obj) {
        return cdata.equals(obj);
    }

    public Object get(String key) {
        return cdata.get(key);
    }

    public Object[] getAll(String[] keys) {
        return cdata.getAll(keys);
    }

    public CompositeType getCompositeType() {
        return cdata.getCompositeType();
    }

    public int hashCode() {
        return cdata.hashCode();
    }

    public String toString() {
        return cdata.toString();
    }

    public Collection<?> values() {
        return cdata.values();
    }

    /**
     * Return the {@code CompositeData} representation of this
     * {@code GcInfo}, including any GC-specific attributes.  The
     * returned value will have at least all the attributes described
     * in the {@link #from(CompositeData) from} method, plus optionally
     * other attributes.
     *
     * @param ct the {@code CompositeType} that the caller expects.
     * This parameter is ignored and can be null.
     *
     * @return the {@code CompositeData} representation.
     */
    public CompositeData toCompositeData(CompositeType ct) {
        return cdata;
    }
}
