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

package com.sun.management;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

/**
 * Platform-specific management interface for a garbage collector
 * which performs collections in cycles.
 *
 * <p> This platform extension is only available to the garbage
 * collection implementation that supports this extension.
 *
 * @author  Mandy Chung
 * @since   1.5
 */
public interface GarbageCollectorMXBean
    extends java.lang.management.GarbageCollectorMXBean {

    /**
     * Returns the GC information about the most recent GC.
     * This method returns a {@link GcInfo}.
     * If no GC information is available, {@code null} is returned.
     * The collector-specific attributes, if any, can be obtained
     * via the {@link CompositeData CompositeData} interface.
     * <p>
     * <b>MBeanServer access:</b>
     * The mapped type of {@code GcInfo} is {@code CompositeData}
     * with attributes specified in {@link GcInfo#from GcInfo}.
     *
     * @return a {@code GcInfo} object representing
     * the most GC information; or {@code null} if no GC
     * information available.
     */
    public GcInfo getLastGcInfo();
}
