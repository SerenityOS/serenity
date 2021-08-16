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
 * @bug 6318664
 * @summary Test deadlock in MBeanRegistration.preDeregister method
 * @author Eamonn McManus
 *
 * @run clean PreDeregisterDeadlockTest
 * @run build PreDeregisterDeadlockTest
 * @run main PreDeregisterDeadlockTest
 */

import java.util.concurrent.*;
import javax.management.*;

public class PreDeregisterDeadlockTest {
    public static interface BlibbyMBean {}

    public static class Blibby implements BlibbyMBean, MBeanRegistration {
        public Blibby(MBeanServer mbs, ObjectName otherName) {
            this.mbs = mbs;
            this.otherName = otherName;
        }

        public ObjectName preRegister(MBeanServer mbs, ObjectName on) {
            return on;
        }

        public void postRegister(Boolean done) {}

        public void preDeregister() {
            if (otherName == null)
                return;
            try {
                Thread t = new Thread() {
                    public void run() {
                        try {
                            mbs.unregisterMBean(otherName);
                        } catch (Throwable e) {
                            e.printStackTrace(System.out);
                            fail(e.toString());
                        }
                    }
                };
                t.start();
                t.join(5000L);
                if (t.isAlive())
                    fail("Deadlock detected");
            } catch (Throwable e) {
                e.printStackTrace(System.out);
                fail(e.toString());
            }
        }

        public void postDeregister() {}

        private final MBeanServer mbs;
        private final ObjectName otherName;
    }

    public static interface BlobbyMBean {}

    public static class Blobby implements BlobbyMBean, MBeanRegistration {
        public Blobby(MBeanServer mbs, Semaphore semaphore) {
            this.mbs = mbs;
            this.semaphore = semaphore;
        }

        public ObjectName preRegister(MBeanServer mbs, ObjectName on) {
            this.objectName = on;
            return on;
        }

        public void postRegister(Boolean done) {}

        public void preDeregister() throws Exception {
            Thread t = new Thread() {
                public void run() {
                    try {
                        mbs.unregisterMBean(objectName);
                        fail("Nested unregister succeeded");
                    } catch (InstanceNotFoundException e) {
                        semaphore.release();
                    } catch (Throwable e) {
                        e.printStackTrace(System.out);
                        fail(e.toString());
                    }
                }
            };
            t.start();
            // Give the thread a chance to block so we are really
            // testing parallelism.  (On slow machines we might not
            // really be testing it but we should be covered by our
            // faster machines.)
            Thread.sleep(500L);
        }

        public void postDeregister() {}

        private final MBeanServer mbs;
        private ObjectName objectName;
        private final Semaphore semaphore;
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on1 = new ObjectName("a:type=Blibby,name=\"1\"");
        ObjectName on2 = new ObjectName("a:type=Blibby,name=\"2\"");

        // Test 1: preDeregister starts a thread which unregisters a
        // different MBean: this must not deadlock
        mbs.registerMBean(new Blibby(mbs, on2), on1);
        mbs.registerMBean(new Blibby(mbs, null), on2);
        mbs.unregisterMBean(on1);

        // Test 2: preDeregister starts a thread which tries to
        // unregister the same MBean: this thread should block until
        // the original thread succeeds in unregistering, then
        // get an InstanceNotFoundException.  We wait for it to
        // complete here, using the semaphore.
        Semaphore semaphore = new Semaphore(0);
        mbs.registerMBean(new Blobby(mbs, semaphore), on1);
        mbs.unregisterMBean(on1);
        boolean ok = semaphore.tryAcquire(1, 5, TimeUnit.SECONDS);
        if (!ok)
            fail("Second unregister thread did not complete");

        if (failure == null)
            System.out.println("OK: Test passed");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void fail(String why) {
        System.out.println("FAILED: " + why);
        failure = why;
    }

    private static volatile String failure;
}
