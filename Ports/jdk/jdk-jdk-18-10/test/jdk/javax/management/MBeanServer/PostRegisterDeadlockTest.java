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
 * @summary Test deadlock in MBeanRegistration.postRegister method
 * @author Eamonn McManus, Daniel Fuchs
 *
 * @run clean PostRegisterDeadlockTest
 * @run build PostRegisterDeadlockTest
 * @run main PostRegisterDeadlockTest
 */

import java.lang.Thread.State;
import java.util.concurrent.*;
import javax.management.*;

public class PostRegisterDeadlockTest {
    public static interface BlibbyMBean {}

    public static class Blibby implements BlibbyMBean, MBeanRegistration {
        public Blibby(MBeanServer mbs, ObjectName otherName) {
            this.mbs = mbs;
            this.otherName = otherName;
        }

        public ObjectName preRegister(MBeanServer mbs, ObjectName on) {
            return on;
        }

        public void preDeregister() {}

        public void postRegister(Boolean done) {
            // If no other MBean was registered
            // do nothing.
            //
            if (otherName == null) return;

            // Check that we can unregister
            // other MBean
            try {
                Thread t = new Thread() {
                    public void run() {
                        try {
                            try {
                                mbs.unregisterMBean(otherName);
                            } catch (InstanceNotFoundException x) {
                                 // Race condition!
                                 System.out.println(otherName+
                                         " was unregistered by main thread.");
                            }
                        } catch (Throwable e) {
                            e.printStackTrace(System.out);
                            fail(e.toString());
                        }
                    }
                };
                t.start();
                t.join(5000L);
                if (t.isAlive()) {
                    if (t.getState().equals(State.BLOCKED))
                        fail("Deadlock detected");
                    else
                        fail("Test not conclusive: "+
                             "Thread is alive but not blocked.");
                }
            } catch (Throwable e) {
                e.printStackTrace(System.out);
                fail(e.toString());
            }
        }

        public void postDeregister() {}

        private final MBeanServer mbs;
        private final ObjectName otherName;
    }

    public static void main(String[] args) throws Exception {
        String previous = null;
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on1 = new ObjectName("a:type=Blibby,name=\"1\"");
        ObjectName on2 = new ObjectName("a:type=Blibby,name=\"2\"");


        // Test 1:
        // 1 MBean is registered with on1
        // Another MBean is registered with on1, postRegister(FALSE) is
        // called, and the second MBean attempts to unregister first MBean in
        // postRegister:
        // postRegister starts a thread which unregisters the first MBean:
        // this must not deadlock
        //
        System.out.println("\n****  TEST #1 ****\n");
        System.out.println("Registering Blibby #1 with name: " + on1);
        mbs.registerMBean(new Blibby(mbs, null), on1);
        try {
            System.out.println("Registering Blibby #2 with same name: " + on1);
            mbs.registerMBean(new Blibby(mbs, on1), on1);
        } catch (InstanceAlreadyExistsException x) {
            System.out.println("Received expected exception: " + x);
        }
        if (mbs.isRegistered(on1)) {
            try {
                mbs.unregisterMBean(on1);
                if (failure == null)
                    fail(on1+" should have been unregistered");
            } catch (InstanceNotFoundException x) {
                // Race condition!
                System.out.println(on1+" was unregistered by mbean thread.");
            }
        }  else {
            System.out.println(on1+" was correctly unregistered.");
        }

        if (failure == previous)
            System.out.println("\n****  TEST #1 PASSED ****\n");

        previous = failure;

        // Test 2:
        // 1 MBean is registered with on1
        // Another MBean is registered with on2, postRegister(TRUE) is
        // called, and the second MBean attempts to unregister first MBean in
        // postRegister:
        // postRegister starts a thread which unregisters the first MBean:
        // this must not deadlock
        //
        System.out.println("\n****  TEST #2 ****\n");
        System.out.println("Registering Blibby #1 with name: " + on1);
        mbs.registerMBean(new Blibby(mbs, null), on1);
        System.out.println("Registering Blibby #2 with other name: " + on2);
        mbs.registerMBean(new Blibby(mbs, on1), on2);
        if (mbs.isRegistered(on1)) {
            try {
                mbs.unregisterMBean(on1);
                if (failure == null)
                    fail(on1+" should have been unregistered");
            } catch (InstanceNotFoundException x) {
                // Race condition!
                System.out.println(on1+" was unregistered by mbean thread.");
            }
        }  else {
            System.out.println(on1+" was correctly unregistered.");
        }

        System.out.println("unregistering "+on2);
        mbs.unregisterMBean(on2);
        if (failure == previous)
            System.out.println("\n****  TEST #2 PASSED ****\n");
        previous = failure;

        if (failure == null)
            System.out.println("OK: Test passed");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void fail(String why) {
        System.out.println("FAILED: " + why);
        failure = (failure == null)?why:(failure+",\n"+why);
    }

    private static volatile String failure;
}
