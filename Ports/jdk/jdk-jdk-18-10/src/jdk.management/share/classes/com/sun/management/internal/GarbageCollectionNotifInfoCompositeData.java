/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Field;
import java.util.HashMap;
import sun.management.LazyCompositeData;
import static sun.management.LazyCompositeData.getString;
import sun.management.Util;

/**
 * A CompositeData for GarbageCollectionNotificationInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class GarbageCollectionNotifInfoCompositeData extends LazyCompositeData {
    private final GarbageCollectionNotificationInfo gcNotifInfo;

    public GarbageCollectionNotifInfoCompositeData(GarbageCollectionNotificationInfo info) {
        this.gcNotifInfo = info;
    }

    public GarbageCollectionNotificationInfo getGarbageCollectionNotifInfo() {
        return gcNotifInfo;
    }

    public static CompositeData toCompositeData(GarbageCollectionNotificationInfo info) {
        GarbageCollectionNotifInfoCompositeData gcnicd =
            new GarbageCollectionNotifInfoCompositeData(info);
        return gcnicd.getCompositeData();
    }

    private CompositeType getCompositeTypeByBuilder() {
        @SuppressWarnings("removal")
        final GcInfoBuilder builder = AccessController.doPrivileged (new PrivilegedAction<GcInfoBuilder>() {
                public GcInfoBuilder run() {
                    try {
                        Class<?> cl = Class.forName("com.sun.management.GcInfo");
                        Field f = cl.getDeclaredField("builder");
                        f.setAccessible(true);
                        return (GcInfoBuilder)f.get(gcNotifInfo.getGcInfo());
                    } catch(ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                        return null;
                    }
                }
            });
        CompositeType gict = null;
        synchronized(compositeTypeByBuilder) {
            gict = compositeTypeByBuilder.get(builder);
            if(gict == null) {
                OpenType<?>[] gcNotifInfoItemTypes = new OpenType<?>[] {
                    SimpleType.STRING,
                    SimpleType.STRING,
                    SimpleType.STRING,
                    builder.getGcInfoCompositeType(),
                };
                try {
                    final String typeName =
                        "sun.management.GarbageCollectionNotifInfoCompositeType";
                    gict = new CompositeType(typeName,
                                             "CompositeType for GC notification info",
                                             gcNotifInfoItemNames,
                                             gcNotifInfoItemNames,
                                             gcNotifInfoItemTypes);
                    compositeTypeByBuilder.put(builder,gict);
                } catch (OpenDataException e) {
                    // shouldn't reach here
                    throw new RuntimeException(e);
                }
            }
        }
        return gict;
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // gcNotifInfoItemNames!
        final Object[] gcNotifInfoItemValues;
        gcNotifInfoItemValues = new Object[] {
            gcNotifInfo.getGcName(),
            gcNotifInfo.getGcAction(),
            gcNotifInfo.getGcCause(),
            GcInfoCompositeData.toCompositeData(gcNotifInfo.getGcInfo())
        };

        CompositeType gict = getCompositeTypeByBuilder();

        try {
            return new CompositeDataSupport(gict,
                                            gcNotifInfoItemNames,
                                            gcNotifInfoItemValues);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    //    private static MappedMXBeanType gcInfoMapType;
    private static final String GC_NAME = "gcName";
    private static final String GC_ACTION = "gcAction";
    private static final String GC_CAUSE = "gcCause";
    private static final String GC_INFO     = "gcInfo";
    private static final String[] gcNotifInfoItemNames = {
        GC_NAME,
        GC_ACTION,
        GC_CAUSE,
        GC_INFO
    };
    private static HashMap<GcInfoBuilder,CompositeType> compositeTypeByBuilder =
        new HashMap<>();

    public static String getGcName(CompositeData cd) {
        String gcname = getString(cd, GC_NAME);
        if (gcname == null) {
            throw new IllegalArgumentException("Invalid composite data: " +
                "Attribute " + GC_NAME + " has null value");
        }
        return gcname;
    }

    public static String getGcAction(CompositeData cd) {
        String gcaction = getString(cd, GC_ACTION);
        if (gcaction == null) {
            throw new IllegalArgumentException("Invalid composite data: " +
                "Attribute " + GC_ACTION + " has null value");
        }
        return gcaction;
    }

    public static String getGcCause(CompositeData cd) {
        String gccause = getString(cd, GC_CAUSE);
        if (gccause == null) {
            throw new IllegalArgumentException("Invalid composite data: " +
                "Attribute " + GC_CAUSE + " has null value");
        }
        return gccause;
    }

    public static GcInfo getGcInfo(CompositeData cd) {
        CompositeData gcInfoData = (CompositeData) cd.get(GC_INFO);
        return GcInfo.from(gcInfoData);
    }

    /** Validate if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        if (!isTypeMatched( getBaseGcNotifInfoCompositeType(), cd.getCompositeType())) {
            throw new IllegalArgumentException(
                "Unexpected composite type for GarbageCollectionNotificationInfo");
        }
    }

    // This is only used for validation.
    private static CompositeType baseGcNotifInfoCompositeType = null;
    private static synchronized CompositeType getBaseGcNotifInfoCompositeType() {
        if (baseGcNotifInfoCompositeType == null) {
            try {
                OpenType<?>[] baseGcNotifInfoItemTypes = new OpenType<?>[] {
                    SimpleType.STRING,
                    SimpleType.STRING,
                    SimpleType.STRING,
                    GcInfoCompositeData.getBaseGcInfoCompositeType()
                };
                baseGcNotifInfoCompositeType =
                    new CompositeType("sun.management.BaseGarbageCollectionNotifInfoCompositeType",
                                      "CompositeType for Base GarbageCollectionNotificationInfo",
                                      gcNotifInfoItemNames,
                                      gcNotifInfoItemNames,
                                      baseGcNotifInfoItemTypes);
            } catch (OpenDataException e) {
                // shouldn't reach here
                throw new RuntimeException(e);
            }
        }
        return baseGcNotifInfoCompositeType;
    }

    private static final long serialVersionUID = -1805123446483771292L;
}
