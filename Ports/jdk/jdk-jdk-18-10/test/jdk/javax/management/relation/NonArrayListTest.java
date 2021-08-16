/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4848474
 * @summary Test that relation service doesn't require List params to be ArrayList
 * @author Eamonn McManus
 *
 * @run clean NonArrayListTest
 * @run build NonArrayListTest
 * @run main NonArrayListTest
 */

import java.util.*;
import javax.management.*;
import javax.management.relation.*;
import javax.management.loading.MLet;

public class NonArrayListTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        RelationService rs = new RelationService(true);
        ObjectName rsName = new ObjectName("r:type=RelationService");
        mbs.registerMBean(rs, rsName);
        RelationServiceMBean rsProxy = (RelationServiceMBean)
            MBeanServerInvocationHandler.newProxyInstance(mbs,
                                                          rsName,
                                                          RelationServiceMBean.class,
                                                          false);

        ObjectName mlet1Name = new ObjectName("r:type=MLet,instance=1");
        ObjectName mlet2Name = new ObjectName("r:type=MLet,instance=2");
        mbs.createMBean(MLet.class.getName(), mlet1Name);
        mbs.createMBean(MLet.class.getName(), mlet2Name);

        RoleInfo leftRoleInfo = new RoleInfo("left", MLet.class.getName());
        RoleInfo rightRoleInfo = new RoleInfo("right", MLet.class.getName());

        ArrayList leftRoleValues =
            new ArrayList(Arrays.asList(new ObjectName[] {mlet1Name}));
        ArrayList rightRoleValues =
            new ArrayList(Arrays.asList(new ObjectName[] {mlet2Name}));
        Role leftRole = new Role("left", leftRoleValues);
        Role rightRole = new Role("right", rightRoleValues);

        RelationType leftRightType =
            new RelationTypeSupport("leftRight",
                                    new RoleInfo[] {leftRoleInfo,
                                                    rightRoleInfo});
        RoleList roleList =
            new RoleList(new ArrayList(Arrays.asList(new Role[] {
                leftRole, rightRole,
            })));
        rsProxy.addRelationType(leftRightType);
        rsProxy.createRelation("relId", "leftRight", roleList);

        boolean ok = true;
        ObjectName oname = new ObjectName("a:b=c");
        List onameList =
            new Vector(Arrays.asList(new ObjectName[] {oname}));

        String testName;

        testName = "RelationNotification constructor with only 9 arguments";
        try {
            RelationNotification notif =
                new RelationNotification(RelationNotification.RELATION_BASIC_CREATION,
                                         rs, // theSrcObj
                                         0L, // TheSeqNbr
                                         0L, // theTimeStamp
                                         "theMsg",
                                         "theRelId",
                                         "theRelTypeName",
                                         oname,
                                         onameList);
            System.out.println("OK: " + testName);
        } catch (Exception e) {
            System.err.println("Exception for " + testName);
            e.printStackTrace();
            ok = false;
        }

        testName = "RelationNotification constructor with 11 arguments";
        try {
            RelationNotification notif =
                new RelationNotification(RelationNotification.RELATION_BASIC_UPDATE,
                                         rs, // theSrcObj
                                         0L, // TheSeqNbr
                                         0L, // theTimeStamp
                                         "theMsg",
                                         "theRelId",
                                         "theRelTypeName",
                                         oname,
                                         "theRoleName",
                                         onameList,
                                         onameList);
            System.out.println("OK: " + testName);
        } catch (Exception e) {
            System.err.println("Exception for " + testName);
            e.printStackTrace();
            ok = false;
        }

        testName = "RelationService.sendNotification";
        try {
            rsProxy.sendRoleUpdateNotification("relId", leftRole, onameList);
            System.out.println("OK: " + testName);
        } catch (Exception e) {
            System.err.println("Exception for " + testName);
            e.printStackTrace();
            ok = false;
        }

        testName = "RelationService.updateRoleMap";
        try {
            rsProxy.updateRoleMap("relId", leftRole, onameList);
            System.out.println("OK: " + testName);
        } catch (Exception e) {
            System.err.println("Exception for " + testName);
            e.printStackTrace();
            ok = false;
        }

        if (ok)
            System.out.println("Tests passed");
        else
            System.err.println("SOME TESTS FAILED");
    }
}
