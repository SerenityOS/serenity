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
 * @bug 4716675
 * @summary Test that relation type checking uses isInstanceOf
 * @author Eamonn McManus
 *
 * @run clean RelationTypeTest
 * @run build RelationTypeTest
 * @run main RelationTypeTest
 */

import java.util.*;
import javax.management.*;
import javax.management.relation.*;

public class RelationTypeTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Test that relation type checking uses " +
                           "isInstanceOf");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName relSvc = new ObjectName("a:type=relationService");
        mbs.createMBean("javax.management.relation.RelationService",
                        relSvc,
                        new Object[] {Boolean.TRUE},
                        new String[] {"boolean"});
        RoleInfo leftInfo =
            new RoleInfo("left", "javax.management.timer.TimerMBean");
        RoleInfo rightInfo =
            new RoleInfo("right", "javax.management.timer.Timer");
        mbs.invoke(relSvc, "createRelationType",
                   new Object[] {
                        "typeName",
                        new RoleInfo[] {leftInfo, rightInfo},
                   },
                   new String[] {
                       "java.lang.String",
                       "[Ljavax.management.relation.RoleInfo;",
                   });
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
        mbs.invoke(relSvc, "createRelation",
                   new Object[] {
                        "relationName",
                        "typeName",
                        roles,
                   },
                   new String[] {
                       "java.lang.String",
                       "java.lang.String",
                       "javax.management.relation.RoleList",
                   });
        Map assoc =
            (Map) mbs.invoke(relSvc, "findAssociatedMBeans",
                             new Object[] {
                                 timer1,
                                 "typeName",
                                 "left",
                             },
                             new String[] {
                                 "javax.management.ObjectName",
                                 "java.lang.String",
                                 "java.lang.String",
                             });

        if (assoc.size() != 1) {
            System.out.println("TEST FAILS: findAssociatedMBeans should " +
                               "return one association: " + assoc);
            System.exit(1);
        }

        ArrayList list = (ArrayList) assoc.get(timer2);
        if (list.size() != 1 || !list.get(0).equals("relationName")) {
            System.out.println("TEST FAILS: MBean not associated as " +
                               "expected: " + list);
            System.exit(1);
        }
        System.out.println("Test passes");
    }
}
