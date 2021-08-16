/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug     4982289
 * @summary Test MemoryNotificationInfo.from to return a valid
 *          MemoryNotificationInfo object. Or throw exception if
 *          the input CompositeData is invalid.
 * @author  Mandy Chung
 *
 * @modules java.management/sun.management
 * @compile OpenTypeConverter.java
 * @build MemoryNotifInfoCompositeData
 * @run main MemoryNotifInfoCompositeData
 */

import javax.management.openmbean.*;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryUsage;
import sun.management.MemoryUsageCompositeData;

public class MemoryNotifInfoCompositeData {
    public static void main(String[] argv) throws Exception {
        createGoodCompositeData();
        badNameCompositeData();
        badTypeCompositeData();
        System.out.println("Test passed");
    }

    private static final int POOL_NAME = 1;
    private static final int COUNT     = 2;
    private static final int USAGE     = 3;
    private static final String[] validItemNames = {
        "dummy1",
        "poolName",
        "count",
        "usage",
        "dummy2",
    };

    // these values are synchronized with the item names
    private static final Object[] values = {
        "Dummy",
        "Foo",
        new Long(100),
        MemoryUsageCompositeData.
            toCompositeData(new MemoryUsage(0, 100, 1000, 5000)),
        "Dummy",
    };

    public static void createGoodCompositeData() throws Exception {

        // get the CompositeType for MemoryUsage
        validItemTypes[USAGE] = OpenTypeConverter.toOpenType(MemoryUsage.class);
        CompositeType ct =
            new CompositeType("MyCompositeType",
                              "CompositeType for MemoryNotificationInfo",
                              validItemNames,
                              validItemNames,
                              validItemTypes);
        CompositeData cd =
            new CompositeDataSupport(ct,
                                     validItemNames,
                                     values);

        MemoryNotificationInfo info = MemoryNotificationInfo.from(cd);
        if (!info.getPoolName().equals(values[POOL_NAME])) {
            throw new RuntimeException("pool name = " + info.getPoolName() +
               " expected = " + values[POOL_NAME]);
        }
        if (info.getCount() != ((Long) values[COUNT]).longValue()) {
            throw new RuntimeException("count = " + info.getCount() +
               " expected = " + values[COUNT]);
        }
        if (info.getUsage().getInit() != 0) {
            throw new RuntimeException("usage init = " +
               info.getUsage().getInit() +
               " expected = 0");
        }
        if (info.getUsage().getUsed() != 100) {
            throw new RuntimeException("usage used = " +
               info.getUsage().getUsed() +
               " expected = 100");
        }
        if (info.getUsage().getCommitted () != 1000) {
            throw new RuntimeException("usage committed = " +
               info.getUsage().getCommitted() +
               " expected = 1000");
        }
        if (info.getUsage().getMax() != 5000) {
            throw new RuntimeException("usage max = " +
               info.getUsage().getMax() +
               " expected = 5000");
        }
        System.out.print("Pool name = " + info.getPoolName());
        System.out.println(" Count = " + info.getCount());
        System.out.println("Usage = " + info.getUsage());
    }

    public static void badNameCompositeData() throws Exception {
        CompositeType ct =
            new CompositeType("MyCompositeType",
                              "CompositeType for MemoryNotificationInfo",
                              badItemNames,
                              badItemNames,
                              validItemTypes);
        CompositeData cd =
            new CompositeDataSupport(ct,
                                     badItemNames,
                                     values);

        try {
            MemoryNotificationInfo info = MemoryNotificationInfo.from(cd);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected exception: " +
                e.getMessage());
            return;
        }
        throw new RuntimeException(
            "IllegalArgumentException not thrown");
    }

    public static void badTypeCompositeData() throws Exception {
        CompositeType ct =
            new CompositeType("MyCompositeType",
                              "CompositeType for MemoryNotificationInfo",
                              validItemNames,
                              validItemNames,
                              badItemTypes);

        final Object[] values1 = {
            "Dummy",
            "Foo",
            new Long(100),
            "Bad memory usage object",
            "Dummy",
        };

        CompositeData cd =
            new CompositeDataSupport(ct,
                                     validItemNames,
                                     values1);

        try {
            MemoryNotificationInfo info = MemoryNotificationInfo.from(cd);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected exception: " +
                e.getMessage());
            return;
        }
        throw new RuntimeException(
            "IllegalArgumentException not thrown");
    }

    private static OpenType[] validItemTypes = {
        SimpleType.STRING,
        SimpleType.STRING,
        SimpleType.LONG,
        null, // OpenTypeConverter.toOpenType(MemoryUsage.class)
        SimpleType.STRING,
    };
    private static final String[] badItemNames = {
        "poolName",
        "BadCountName",
        "usage",
        "dummy1",
        "dummy2",
    };
    private static final OpenType[] badItemTypes = {
        SimpleType.STRING,
        SimpleType.STRING,
        SimpleType.LONG,
        SimpleType.STRING, // Bad type
        SimpleType.STRING,
    };
}
