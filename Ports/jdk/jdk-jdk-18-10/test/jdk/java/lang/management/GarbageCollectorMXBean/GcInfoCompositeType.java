/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6396794
 * @summary Check that LastGcInfo contents are reasonable
 * @author  Eamonn McManus
 * @modules jdk.management
 * @run     main/othervm -XX:-ExplicitGCInvokesConcurrent GcInfoCompositeType
 */
// Passing "-XX:-ExplicitGCInvokesConcurrent" to force System.gc()
// run on foreground when a concurrent collector is used and prevent situations when "GcInfo"
// is missing even though System.gc() was successfuly processed.

import java.util.*;
import java.lang.management.*;
import java.lang.reflect.*;
import javax.management.*;
import javax.management.openmbean.*;
import com.sun.management.GcInfo;

public class GcInfoCompositeType {
    private static int tested = 0;

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ObjectName gcMXBeanPattern =
                new ObjectName("java.lang:type=GarbageCollector,*");
        Set<ObjectName> names =
                mbs.queryNames(gcMXBeanPattern, null);
        if (names.isEmpty())
            throw new Exception("Test incorrect: no GC MXBeans");
        System.gc();
        for (ObjectName n : names)
            tested += test(mbs, n);
        if (tested == 0)
            throw new Exception("No MXBeans were tested");
        System.out.println("Test passed");
    }

    private static int test(MBeanServer mbs, ObjectName n) throws Exception {
        System.out.println("Testing " + n);
        MBeanInfo mbi = mbs.getMBeanInfo(n);
        MBeanAttributeInfo lastGcAI = null;
        for (MBeanAttributeInfo mbai : mbi.getAttributes()) {
            if (mbai.getName().equals("LastGcInfo")) {
                lastGcAI = mbai;
                break;
            }
        }
        if (lastGcAI == null)
            throw new Exception("No LastGcInfo attribute");
        CompositeType declaredType =
                (CompositeType) lastGcAI.getDescriptor().getFieldValue("openType");
        checkType(declaredType);
        CompositeData cd =
                (CompositeData) mbs.getAttribute(n, "LastGcInfo");
        if (cd == null) {
            System.out.println("Value of attribute null");
            return 0;
        } else
            return checkType(cd.getCompositeType());
    }

    private static int checkType(CompositeType ct) throws Exception {
        Method[] methods = GcInfo.class.getMethods();
        Set<String> getters = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (Method m : methods) {
            if (m.getName().startsWith("get") && m.getParameterTypes().length == 0)
                getters.add(m.getName().substring(3));
        }
        Set<String> items = new HashSet<String>(ct.keySet());
        System.out.println("Items at start: " + items);

        // Now check that all the getters have a corresponding item in the
        // CompositeType, except the following:
        // getClass() inherited from Object should not be an item;
        // getCompositeType() inherited from CompositeData is not useful so
        // our hack removes it too.
        // Also track which items had corresponding getters, to make sure
        // there is at least one item which does not (GcThreadCount or
        // another gc-type-specific item).
        final String[] surplus = {"Class", "CompositeType"};
        for (String key : ct.keySet()) {
            if (getters.remove(key))
                items.remove(key);
        }
        if (!getters.equals(new HashSet<String>(Arrays.asList(surplus)))) {
            throw new Exception("Wrong getters: " + getters);
        }
        if (items.isEmpty()) {
            System.out.println("No type-specific items");
            return 0;
        } else {
            System.out.println("Type-specific items: " + items);
            return 1;
        }
    }
}
