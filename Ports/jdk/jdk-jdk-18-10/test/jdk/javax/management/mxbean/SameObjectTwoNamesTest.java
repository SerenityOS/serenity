/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test SameObjectTwoNamesTest.java
 * @bug 6283873
 * @summary Check that registering the same MXBean under two different
 * names produces an exception
 * @author Alexander Shusherov
 * @author Eamonn McManus
 *
 * @run main SameObjectTwoNamesTest
 * @run main/othervm -Djmx.mxbean.multiname=true SameObjectTwoNamesTest
 */

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

public class SameObjectTwoNamesTest {

    public static void main(String[] args) throws Exception {
        boolean expectException =
                (System.getProperty("jmx.mxbean.multiname") == null);
        try {
            ObjectName objectName1 = new ObjectName("test:index=1");
            ObjectName objectName2 = new ObjectName("test:index=2");
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();
            MXBC_SimpleClass01 mxBeanObject = new MXBC_SimpleClass01();

            mbs.registerMBean(mxBeanObject, objectName1);

            mbs.registerMBean(mxBeanObject, objectName2);

            if (expectException) {
                throw new Exception("TEST FAILED: " +
                        "InstanceAlreadyExistsException was not thrown");
            } else
                System.out.println("Correctly got no exception with compat property");
        } catch (InstanceAlreadyExistsException e) {
            if (expectException) {
                System.out.println("Got expected InstanceAlreadyExistsException:");
                e.printStackTrace(System.out);
            } else {
                throw new Exception(
                        "TEST FAILED: Got exception even though compat property set", e);
            }
        }
        System.out.println("TEST PASSED");
    }

    public interface MXBC_Simple01MXBean {}

    public static class MXBC_SimpleClass01 implements MXBC_Simple01MXBean {}

}
