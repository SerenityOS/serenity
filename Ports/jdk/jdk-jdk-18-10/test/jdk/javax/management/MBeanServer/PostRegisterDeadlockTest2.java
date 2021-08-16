/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6417044
 * @summary Test that a failing MBean registration does not lead to a deadlock
 * @author Eamonn McManus
 *
 * @run main PostRegisterDeadlockTest2
 */

import javax.management.*;

public class PostRegisterDeadlockTest2 {
    private static String failed;

    public static interface EmptyMBean {}

    public static class Empty implements EmptyMBean, MBeanRegistration {
        public ObjectName preRegister(MBeanServer mbs, ObjectName on) {
            this.mbs = mbs;
            this.on = on;
            return on;
        }
        public void postRegister(Boolean done) {
            Thread t = new Thread() {
                public void run() {
                    if (!mbs.isRegistered(on))
                        failed = "Not registered!";
                }
            };
            t.start();
            try {
                t.join(5000L);
            } catch (InterruptedException e) {
                failed = "Interrupted: " + e;
            }
            if (t.isAlive())
                failed = "Deadlock detected";
        }
        public void preDeregister() {}
        public void postDeregister() {}

        private MBeanServer mbs;
        private ObjectName on;
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        mbs.registerMBean(new Empty(), on);
        try {
            mbs.registerMBean(new Empty(), on);
            throw new Exception("FAILED: did not get expected exception");
        } catch (InstanceAlreadyExistsException e) {
            System.out.println("OK: got expected exception");
        }
        if (failed != null)
            throw new Exception("FAILED: " + failed);
        System.out.println("TEST PASSED");
    }
}
