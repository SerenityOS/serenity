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
 * @bug 6175517
 * @summary Ensure that preRegister etc are called, but not when wrapped
 * by the class StandardMBean
 * @author Eamonn McManus
 *
 * @run clean MXBeanPreRegisterTest
 * @run build MXBeanPreRegisterTest
 * @run main MXBeanPreRegisterTest
 */

import javax.management.*;

public class MXBeanPreRegisterTest {
    public static interface EmptyMBean {}

    public static interface EmptyMXBean extends EmptyMBean {}

    public static class Base implements MBeanRegistration {
        public ObjectName preRegister(MBeanServer mbs, ObjectName n) {
            count++;
            return n;
        }

        public void postRegister(Boolean done) {
            count++;
        }

        public void preDeregister() {
            count++;
        }

        public void postDeregister() {
            count++;
        }

        int count;
    }

    public static class Empty extends Base implements EmptyMBean {}

    public static class EmptyMX extends Base implements EmptyMXBean {}

    public static void main(String[] args) throws Exception {
        for (boolean mx : new boolean[] {false, true})
            for (boolean wrapped : new boolean[] {false, true})
                test(mx, wrapped);
        if (failure != null)
            throw new Exception("TEST FAILED: " + failure);
        System.out.println("TEST PASSED");
    }

    private static void test(boolean mx, boolean wrapped) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        Base mbean = mx ? new EmptyMX() : new Empty();
        Object reg = wrapped ? new StandardMBean(mbean, null, mx) : mbean;
        mbs.registerMBean(reg, on);
        mbs.unregisterMBean(on);
        String testDescr =
            (mx ? "MXBean" : "Standard MBean") +
            (wrapped ? " wrapped in StandardMBean class should not" :
             " should") +
            " call MBeanRegistration methods";
        boolean ok = mbean.count == (wrapped ? 0 : 4);
        if (ok)
            System.out.println("OK: " + testDescr);
        else {
            failure = testDescr;
            System.out.println("FAILED: " + failure);
        }
    }

    private static String failure;
}
