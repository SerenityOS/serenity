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
 * @bug 6701459
 * @summary Test sequence numbers in RelationService notifications.
 * @author Eamonn McManus
 */

/*
 * Bug 6701459 is for a synchronization problem that is very unlikely to occur
 * in practice and it would be very hard to test it.  Instead we just check that
 * the fix has not introduced any obviously-wrong behavior in the sequence
 * numbers.
 */

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.relation.RelationServiceMBean;
import javax.management.relation.Role;
import javax.management.relation.RoleInfo;
import javax.management.relation.RoleList;

public class RelationNotificationSeqNoTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName relSvcName = new ObjectName("a:type=relationService");
        RelationServiceMBean relSvc =
                JMX.newMBeanProxy(mbs, relSvcName, RelationServiceMBean.class);
        mbs.createMBean("javax.management.relation.RelationService",
                        relSvcName,
                        new Object[] {Boolean.TRUE},
                        new String[] {"boolean"});

        final BlockingQueue<Notification> q =
                new ArrayBlockingQueue<Notification>(100);
        NotificationListener qListener = new NotificationListener() {
            public void handleNotification(Notification notification,
                                           Object handback) {
                q.add(notification);
            }
        };
        mbs.addNotificationListener(relSvcName, qListener, null, null);

        RoleInfo leftInfo =
            new RoleInfo("left", "javax.management.timer.TimerMBean");
        RoleInfo rightInfo =
            new RoleInfo("right", "javax.management.timer.Timer");
        relSvc.createRelationType("typeName", new RoleInfo[] {leftInfo, rightInfo});
        ObjectName timer1 = new ObjectName("a:type=timer,number=1");
        ObjectName timer2 = new ObjectName("a:type=timer,number=2");
        mbs.createMBean("javax.management.timer.Timer", timer1);
        mbs.createMBean("javax.management.timer.Timer", timer2);

        Role leftRole =
            new Role("left", Arrays.asList(new ObjectName[] {timer1}));
        Role rightRole =
            new Role("right", Arrays.asList(new ObjectName[] {timer2}));
        RoleList roles =
            new RoleList(Arrays.asList(new Role[] {leftRole, rightRole}));

        final int NREPEAT = 10;

        for (int i = 0; i < NREPEAT; i++) {
            relSvc.createRelation("relationName", "typeName", roles);
            relSvc.removeRelation("relationName");
        }

        Notification firstNotif = q.remove();
        long seqNo = firstNotif.getSequenceNumber();
        for (int i = 0; i < NREPEAT * 2 - 1; i++) {
            Notification n = q.remove();
            long nSeqNo = n.getSequenceNumber();
            if (nSeqNo != seqNo + 1) {
                throw new Exception(
                        "TEST FAILED: expected seqNo " + (seqNo + 1) + "; got " +
                        nSeqNo);
            }
            seqNo++;
        }
        System.out.println("TEST PASSED: got " + (NREPEAT * 2) + " notifications " +
                "with contiguous sequence numbers");
    }
}
