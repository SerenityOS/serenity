/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4930397
 * @summary Make sure MemoryMXBean has two notification types.
 * @author  Mandy Chung
 *
 * @modules jdk.management
 * @run main GetMBeanInfo
 */

import java.lang.management.*;
import javax.management.*;

public class GetMBeanInfo {
    private static final int EXPECTED_NOTIF_TYPES = 2;
    private static int count = 0;

    public static void main(String argv[]) throws Exception {
        final ObjectName objName;
        objName = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        MBeanInfo minfo = mbs.getMBeanInfo(objName);
        MBeanNotificationInfo[] notifs = minfo.getNotifications();
        for (int i = 0; i < notifs.length; i++) {
            printNotifType(notifs[i]);
        }

        if (count != EXPECTED_NOTIF_TYPES) {
            throw new RuntimeException("Unexpected number of notification types"
                                       + " count = " + count +
                                       " expected = " + EXPECTED_NOTIF_TYPES);
        }
    }

    private static void printNotifType(MBeanNotificationInfo notif) {
        String[] types = notif.getNotifTypes();
        for (int i = 0; i < types.length; i++) {
            System.out.println(types[i]);
            count++;
        }
    }
}
