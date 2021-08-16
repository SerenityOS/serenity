/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryUsage;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

/**
 * A CompositeData for MemoryNotificationInfo for the local management support.
 * This class avoids the performance penalty paid to the
 * construction of a CompositeData use in the local case.
 */
public class MemoryNotifInfoCompositeData extends LazyCompositeData {
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private final MemoryNotificationInfo memoryNotifInfo;

    private MemoryNotifInfoCompositeData(MemoryNotificationInfo info) {
        this.memoryNotifInfo = info;
    }

    public MemoryNotificationInfo getMemoryNotifInfo() {
        return memoryNotifInfo;
    }

    public static CompositeData toCompositeData(MemoryNotificationInfo info) {
        MemoryNotifInfoCompositeData mnicd =
            new MemoryNotifInfoCompositeData(info);
        return mnicd.getCompositeData();
    }

    protected CompositeData getCompositeData() {
        // CONTENTS OF THIS ARRAY MUST BE SYNCHRONIZED WITH
        // memoryNotifInfoItemNames!
        final Object[] memoryNotifInfoItemValues = {
            memoryNotifInfo.getPoolName(),
            MemoryUsageCompositeData.toCompositeData(memoryNotifInfo.getUsage()),
            memoryNotifInfo.getCount(),
        };

        try {
            return new CompositeDataSupport(memoryNotifInfoCompositeType,
                                            memoryNotifInfoItemNames,
                                            memoryNotifInfoItemValues);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final CompositeType memoryNotifInfoCompositeType;
    static {
        try {
            memoryNotifInfoCompositeType = (CompositeType)
                MappedMXBeanType.toOpenType(MemoryNotificationInfo.class);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new AssertionError(e);
        }
    }

    private static final String POOL_NAME = "poolName";
    private static final String USAGE     = "usage";
    private static final String COUNT     = "count";
    private static final String[] memoryNotifInfoItemNames = {
        POOL_NAME,
        USAGE,
        COUNT,
    };


    public static String getPoolName(CompositeData cd) {
        String poolname = getString(cd, POOL_NAME);
        if (poolname == null) {
            throw new IllegalArgumentException("Invalid composite data: " +
                "Attribute " + POOL_NAME + " has null value");
        }
        return poolname;
    }

    public static MemoryUsage getUsage(CompositeData cd) {
        CompositeData usageData = (CompositeData) cd.get(USAGE);
        return MemoryUsage.from(usageData);
    }

    public static long getCount(CompositeData cd) {
        return getLong(cd, COUNT);
    }

    /** Validate if the input CompositeData has the expected
     * CompositeType (i.e. contain all attributes with expected
     * names and types).
     */
    public static void validateCompositeData(CompositeData cd) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }

        if (!isTypeMatched(memoryNotifInfoCompositeType, cd.getCompositeType())) {
            throw new IllegalArgumentException(
                "Unexpected composite type for MemoryNotificationInfo");
        }
    }

    private static final long serialVersionUID = -1805123446483771291L;
}
