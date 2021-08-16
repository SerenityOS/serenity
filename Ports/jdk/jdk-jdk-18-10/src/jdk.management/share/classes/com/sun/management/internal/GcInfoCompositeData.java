/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management.internal;

import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import java.io.InvalidObjectException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.OpenDataException;
import com.sun.management.GcInfo;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.management.LazyCompositeData;
import static sun.management.LazyCompositeData.getLong;
import sun.management.MappedMXBeanType;
import sun.management.Util;

/**
 * A CompositeData for GcInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class GcInfoCompositeData extends LazyCompositeData {
    private final GcInfo info;
    private final GcInfoBuilder builder;
    private final Object[] gcExtItemValues;

    public GcInfoCompositeData(GcInfo info,
                        GcInfoBuilder builder,
                        Object[] gcExtItemValues) {
        this.info = info;
        this.builder = builder;
        this.gcExtItemValues = gcExtItemValues;
    }

    public GcInfo getGcInfo() {
        return info;
    }

    public static CompositeData toCompositeData(final GcInfo info) {
        @SuppressWarnings("removal")
        final GcInfoBuilder builder = AccessController.doPrivileged (new PrivilegedAction<GcInfoBuilder>() {
                        public GcInfoBuilder run() {
                            try {
                                Class<?> cl = Class.forName("com.sun.management.GcInfo");
                                Field f = cl.getDeclaredField("builder");
                                f.setAccessible(true);
                                return (GcInfoBuilder)f.get(info);
                            } catch(ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                                return null;
                            }
                        }
                    });
        @SuppressWarnings("removal")
        final Object[] extAttr = AccessController.doPrivileged (new PrivilegedAction<Object[]>() {
                        public Object[] run() {
                            try {
                                Class<?> cl = Class.forName("com.sun.management.GcInfo");
                                Field f = cl.getDeclaredField("extAttributes");
                                f.setAccessible(true);
                                return (Object[])f.get(info);
                            } catch(ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                                return null;
                            }
                        }
                    });
        GcInfoCompositeData gcicd =
            new GcInfoCompositeData(info,builder,extAttr);
        return gcicd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // baseGcInfoItemNames!
        final Object[] baseGcInfoItemValues;

        try {
            baseGcInfoItemValues = new Object[] {
                info.getId(),
                info.getStartTime(),
                info.getEndTime(),
                info.getDuration(),
                memoryUsageMapType.toOpenTypeData(info.getMemoryUsageBeforeGc()),
                memoryUsageMapType.toOpenTypeData(info.getMemoryUsageAfterGc()),
            };
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }

        // Get the item values for the extension attributes
        final int gcExtItemCount = builder.getGcExtItemCount();
        if (gcExtItemCount == 0 &&
            gcExtItemValues != null && gcExtItemValues.length != 0) {
            throw new AssertionError("Unexpected Gc Extension Item Values");
        }

        if (gcExtItemCount > 0 && (gcExtItemValues == null ||
             gcExtItemCount != gcExtItemValues.length)) {
            throw new AssertionError("Unmatched Gc Extension Item Values");
        }

        Object[] values = new Object[baseGcInfoItemValues.length +
                                     gcExtItemCount];
        System.arraycopy(baseGcInfoItemValues, 0, values, 0,
                         baseGcInfoItemValues.length);

        if (gcExtItemCount > 0) {
            System.arraycopy(gcExtItemValues, 0, values,
                             baseGcInfoItemValues.length, gcExtItemCount);
        }

        try {
            return new CompositeDataSupport(builder.getGcInfoCompositeType(),
                                            builder.getItemNames(),
                                            values);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final String ID                     = "id";
    private static final String START_TIME             = "startTime";
    private static final String END_TIME               = "endTime";
    private static final String DURATION               = "duration";
    private static final String MEMORY_USAGE_BEFORE_GC = "memoryUsageBeforeGc";
    private static final String MEMORY_USAGE_AFTER_GC  = "memoryUsageAfterGc";

    private static final String[] baseGcInfoItemNames = {
        ID,
        START_TIME,
        END_TIME,
        DURATION,
        MEMORY_USAGE_BEFORE_GC,
        MEMORY_USAGE_AFTER_GC,
    };


    private static MappedMXBeanType memoryUsageMapType;
    static {
        try {
            Method m = GcInfo.class.getMethod("getMemoryUsageBeforeGc");
            memoryUsageMapType =
                MappedMXBeanType.getMappedType(m.getGenericReturnType());
        } catch (NoSuchMethodException | OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    static String[] getBaseGcInfoItemNames() {
        return baseGcInfoItemNames;
    }

    private static OpenType<?>[] baseGcInfoItemTypes = null;
    static synchronized OpenType<?>[] getBaseGcInfoItemTypes() {
        if (baseGcInfoItemTypes == null) {
            OpenType<?> memoryUsageOpenType = memoryUsageMapType.getOpenType();
            baseGcInfoItemTypes = new OpenType<?>[] {
                SimpleType.LONG,
                SimpleType.LONG,
                SimpleType.LONG,
                SimpleType.LONG,

                memoryUsageOpenType,
                memoryUsageOpenType,
            };
        }
        return baseGcInfoItemTypes;
    }

    public static long getId(CompositeData cd) {
        return getLong(cd, ID);
    }
    public static long getStartTime(CompositeData cd) {
        return getLong(cd, START_TIME);
    }
    public static long getEndTime(CompositeData cd) {
        return getLong(cd, END_TIME);
    }

    public static Map<String, MemoryUsage>
            getMemoryUsageBeforeGc(CompositeData cd) {
        try {
            TabularData td = (TabularData) cd.get(MEMORY_USAGE_BEFORE_GC);
            return cast(memoryUsageMapType.toJavaTypeData(td));
        } catch (InvalidObjectException | OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, MemoryUsage> cast(Object x) {
        return (Map<String, MemoryUsage>) x;
    }
    public static Map<String, MemoryUsage>
            getMemoryUsageAfterGc(CompositeData cd) {
        try {
            TabularData td = (TabularData) cd.get(MEMORY_USAGE_AFTER_GC);
            //return (Map<String,MemoryUsage>)
            return cast(memoryUsageMapType.toJavaTypeData(td));
        } catch (InvalidObjectException | OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    /**
     * Returns true if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).  Otherwise, return false.
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        if (!isTypeMatched(getBaseGcInfoCompositeType(),
                           cd.getCompositeType())) {
           throw new IllegalArgumentException(
                "Unexpected composite type for GcInfo");
        }
    }

    // This is only used for validation.
    private static CompositeType baseGcInfoCompositeType = null;
    static synchronized CompositeType getBaseGcInfoCompositeType() {
        if (baseGcInfoCompositeType == null) {
            try {
                baseGcInfoCompositeType =
                    new CompositeType("sun.management.BaseGcInfoCompositeType",
                                      "CompositeType for Base GcInfo",
                                      getBaseGcInfoItemNames(),
                                      getBaseGcInfoItemNames(),
                                      getBaseGcInfoItemTypes());
            } catch (OpenDataException e) {
                // shouldn't reach here
                throw new RuntimeException(e);
            }
        }
        return baseGcInfoCompositeType;
    }

    private static final long serialVersionUID = -5716428894085882742L;
}
