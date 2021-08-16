/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6610094
 * @summary Test the OperatingSystemMXBean instance returned by
 *          ManagementFactory.getPlatformMXBeans()
 * @author  Mandy Chung
 *
 * @modules java.management
 *          jdk.management
 * @run main PlatformMXBeanTest
 */

import java.lang.management.*;
import java.util.List;

public class PlatformMXBeanTest {
    public static void main(String[] argv) throws Exception {
        OperatingSystemMXBean osMBean = getOSPlatformMXBean(OperatingSystemMXBean.class);

        // There should have only one single MXBean for the OS MXBean interfaces:
        //   java.lang.management.OperatingSystemMXBean
        //   com.sun.management.OperatingSystemMXBean
        //   com.sun.management.UnixOperatingSystemMXBean
        if (osMBean != getOSPlatformMXBean(com.sun.management.OperatingSystemMXBean.class)) {
            throw new RuntimeException(
                "Invalid com.sun.management.OperatingSystemMXBean instance");
        }

        if (!System.getProperty("os.name").startsWith("Windows") &&
                osMBean != getOSPlatformMXBean(com.sun.management.UnixOperatingSystemMXBean.class)) {
            throw new RuntimeException(
                "Invalid com.sun.management.UnixOperatingSystemMXBean instance");
        }
    }

    private static <T extends OperatingSystemMXBean>
            T getOSPlatformMXBean(Class<T> c) {
        List<T> result = ManagementFactory.getPlatformMXBeans(c);
        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new RuntimeException(c.getName() + " has " +
                result.size() + " number of instances");
        }
    }
}
