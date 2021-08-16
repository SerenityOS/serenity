/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;

/*
 * @test
 * @bug     8042901
 * @summary If jdk.management is present, GarbageCollectorMXBean and ThreadMXBean
 *          must be from com.sun.management.internal
 * @author  Shanliang Jiang
 */
public class CheckSomeMXBeanImplPackage {
    private static String implPackageName = "com.sun.management.internal";

    public static void main(String[] args) throws Exception {
        boolean present = false;
        try {
            Class.forName("com.sun.management.GarbageCollectorMXBean");
            present = true;
        } catch (ClassNotFoundException cnfe) {}

        if (present) {
            Class <? extends PlatformManagedObject> klazz =
                    java.lang.management.GarbageCollectorMXBean.class;
            for (Object obj :
                    ManagementFactory.getPlatformMXBeans(klazz)) {
                check(klazz.getName(), obj);
            }

            klazz = com.sun.management.GarbageCollectorMXBean.class;
            for (Object obj :
                    ManagementFactory.getPlatformMXBeans(klazz)) {
                check(klazz.getName(), obj);
            }

            klazz = java.lang.management.ThreadMXBean.class;
            check(klazz.getName(),
                    ManagementFactory.getPlatformMXBean(klazz));

            klazz = com.sun.management.ThreadMXBean.class;
            check(klazz.getName(),
                    ManagementFactory.getPlatformMXBean(klazz));

            System.out.println("--- PASSED!");
        } else {
            System.out.println("--- Skip the test, jdk.management module is not present!");
        }
    }

    private static void check(String mbeanName, Object impl) {
        if (!impl.getClass().getName().startsWith(implPackageName)) {
            throw new RuntimeException(mbeanName+" implementation package "
                    + "should be: " + implPackageName
                    + ", but got: " + impl.getClass());
        } else {
            System.out.println("--- Good, "+mbeanName+" got right implementation: " + impl);
        }
    }
}
