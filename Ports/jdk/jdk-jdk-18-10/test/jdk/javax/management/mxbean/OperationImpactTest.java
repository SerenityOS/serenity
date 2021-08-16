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
 * @bug 6320104
 * @summary Check that MXBean operations have impact UNKNOWN.
 * @author Eamonn McManus
 *
 * @run clean OperationImpactTest
 * @run build OperationImpactTest
 * @run main OperationImpactTest
 */

import javax.management.*;
import javax.management.openmbean.*;

public class OperationImpactTest {
    public static interface ThingMXBean {
        void thing();
    }
    public static class Thing implements ThingMXBean {
        public void thing() {}
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        Thing thing = new Thing();
        mbs.registerMBean(thing, on);
        MBeanInfo mbi = mbs.getMBeanInfo(on);
        MBeanOperationInfo[] ops = mbi.getOperations();
        if (ops.length != 1)
            throw new Exception("TEST FAILED: several ops: " + mbi);
        MBeanOperationInfo op = ops[0];
        if (!op.getName().equals("thing"))
            throw new Exception("TEST FAILED: wrong op name: " + op);
        if (op.getImpact() != MBeanOperationInfo.UNKNOWN)
            throw new Exception("TEST FAILED: wrong impact: " + op);

        // Also check that constructing an OpenMBeanOperationInfo with an
        // UNKNOWN impact works
        op = new OpenMBeanOperationInfoSupport("name", "descr", null,
                                               SimpleType.VOID,
                                               MBeanOperationInfo.UNKNOWN);
        if (op.getImpact() != MBeanOperationInfo.UNKNOWN)
            throw new Exception("TEST FAILED: wrong impact: " + op);

        System.out.println("TEST PASSED");
    }
}
