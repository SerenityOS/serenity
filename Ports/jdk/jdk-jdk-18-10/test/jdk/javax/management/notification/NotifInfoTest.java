/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4506105 6303698
 * @summary NotificationBroadcasterSupport should have a ctor with MBeanNotificationInfo[]
 * @author Shanliang JIANG
 *
 * @run clean NotifInfoTest
 * @run build NotifInfoTest
 * @run main NotifInfoTest
 */

import java.util.Arrays;
import javax.management.*;

public class NotifInfoTest {
    public static void main(String[] args) throws Exception {
        final MBeanNotificationInfo info1 =
            new MBeanNotificationInfo(new String[] {"t11", "t12"}, "n1", null);

        final MBeanNotificationInfo info2 =
            new MBeanNotificationInfo(new String[] {"t21", "t22"}, "n2", null);

        final MBeanNotificationInfo[] mninfo1 =
            new MBeanNotificationInfo[] {info1, info2};

        final MBeanNotificationInfo[] mninfo2 = new MBeanNotificationInfo[] {info1, info2};

        final NotificationBroadcasterSupport support1 = new NotificationBroadcasterSupport(mninfo1);
        final NotificationBroadcasterSupport support2 = new NotificationBroadcasterSupport(null, mninfo1);

        if (!Arrays.deepEquals(mninfo2, support1.getNotificationInfo()) ||
            !Arrays.deepEquals(mninfo2, support2.getNotificationInfo())) {
            throw new RuntimeException("Not got expected MBeanNotificationInfo!");
        }

        // Ensure evil code can't change the array
        MBeanNotificationInfo[] mninfo3 = support1.getNotificationInfo();
        mninfo3[0] = null;
        mninfo3[1] = null;
        if (!Arrays.deepEquals(mninfo2, support1.getNotificationInfo()))
            throw new RuntimeException("Caller changed info array!");
    }
}
