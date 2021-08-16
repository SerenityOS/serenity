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
 * @bug 6331746
 * @summary Test a deadlock and will be blocked forever if the deadlock is present.
 * @author Shanliang JIANG
 *
 * @run main DeadlockTest
 */

import javax.management.*;
import javax.management.timer.*;

public class DeadlockTest extends StandardMBean {
    public <T> DeadlockTest(T implementation, Class<T> mbeanInterface)
    throws NotCompliantMBeanException {
        super(implementation, mbeanInterface);
    }

    public MBeanInfo getCachedMBeanInfo() {
        return super.getCachedMBeanInfo();
    }

    public void cacheMBeanInfo(MBeanInfo mi) {
        super.cacheMBeanInfo(mi);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("main: No deadlock please.");

        System.out.println("main: Create a BadBay to hold the lock forever.");
        DeadlockTest dt = new DeadlockTest(new Timer(), TimerMBean.class);

        BadBoy bb = new BadBoy(dt);
        bb.start();

        synchronized(bb) {
            while(!bb.gotLock) {
                bb.wait(); // if blocked here, means failing to get lock, impossible.
            }
        }

        System.out.println("main: The BadBay is holding the lock forever.");

        System.out.println("main: Create a WorkingBoy to see blocking ...");
        WorkingBoy wb = new WorkingBoy(dt);

        synchronized(wb) {
            wb.start();

            while(!wb.done) {
                wb.wait(); // if blocked here, the deadlock happends
            }
        }

        System.out.println("main: OK, bye bye.");
    }

    private static class BadBoy extends Thread {
        public BadBoy(Object o) {
            setDaemon(true);

            this.o = o;
        }

        public void run() {
            System.out.println("BadBoy-run: keep synchronization lock forever!");

            synchronized(o) {
                synchronized(this) {
                    gotLock = true;

                    this.notify();
                }

                try {
                    Thread.sleep(10000000);
                } catch (Exception e) {
                    // OK
                }
            }
        }

        final Object o;
        public boolean gotLock;
    }

    private static class WorkingBoy extends Thread {
        public WorkingBoy(DeadlockTest sm) {
            setDaemon(true);

            this.sm = sm;
        }

        public void run() {
            try {
                System.out.println("WorkingBoy-run: calling StandardMBean methods ...");

                System.out.println("WorkingBoy-run: calling setImplementation ...");
                sm.setImplementation(new Timer());

                System.out.println("WorkingBoy-run: calling getImplementation ...");
                sm.getImplementation();

                System.out.println("WorkingBoy-run: calling getMBeanInterface ...");
                sm.getMBeanInterface();

                System.out.println("WorkingBoy-run: calling getImplementationClass ...");
                sm.getImplementationClass();

                System.out.println("WorkingBoy-run: calling cacheMBeanInfo ...");
                sm.cacheMBeanInfo(null);

                System.out.println("WorkingBoy-run: calling getCachedMBeanInfo ...");
                sm.getCachedMBeanInfo();

                System.out.println("WorkingBoy-run: All done!");

                synchronized(this) {
                    done = true;

                    this.notifyAll();
                }
            } catch (NotCompliantMBeanException ne) {
                // Impossible?
                throw new RuntimeException(ne);
            }
        }

        final DeadlockTest sm;
        public boolean done;
    }
}
