/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Create a thread which stops in methods a(), a()->b(), a()->b()->c(),
 *          synchronizing on one monitor inside of each method.
 *          After checking that lock info is correct free the lock and
 *          invoke another method. Repeat this action.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData TraveledLockTest
 */
import common.ToolResults;
import java.util.Iterator;
import utils.*;

class TraveledLockDebuggee extends Thread {

    static final String THREAD_NAME = "MyThread";

    TraveledLockDebuggee() {
        setName(THREAD_NAME);
    }

    Object monitor = new Object();

    public void c() {
        synchronized (monitor) {
            Utils.sleep();
        }
    }

    public void b() {
        try {
            synchronized (monitor) {
                while (true) {
                    Thread.sleep(Long.MAX_VALUE);
                }
            }
        } catch (InterruptedException e) {
            c();
        }
    }

    public void a() {
        try {
            synchronized (monitor) {
                while (true) {
                    Thread.sleep(Long.MAX_VALUE);
                }
            }
        } catch (InterruptedException e) {
            b();
        }
    }

    public void run() {
        a();
    }

}

public class TraveledLockTest {

    public static void main(String[] args) throws Exception {
        new TraveledLockTest().doTest();
    }

    private void doTest() throws Exception {
        TraveledLockDebuggee debuggee = new TraveledLockDebuggee();

        // Start in method a()
        debuggee.start();

        // Collect output from the jstack tool
        JstackTool jstackTool = new JstackTool(ProcessHandle.current().pid());
        ToolResults results1 = jstackTool.measure();

        // Go to method b()
        debuggee.interrupt();

        // Collect output from the jstack tool
        ToolResults results2 = jstackTool.measure();

        // Go to method c()
        debuggee.interrupt();

        // Collect output from the jstack tool
        ToolResults results3 = jstackTool.measure();

        analyse(results1.getStdoutString(), results2.getStdoutString(), results3.getStdoutString());
    }

    // Analyzsing the outputs from the 3 jstack runs
    public void analyse(String results1, String results2, String results3) {

        String jstackStr1 = results1;
        String jstackStr2 = results2;
        String jstackStr3 = results3;

        if (jstackStr1 == null) {
            throw new RuntimeException("First jstack output is empty");
        }
        if (jstackStr2 == null) {
            throw new RuntimeException("Second jstack output is empty");
        }
        if (jstackStr3 == null) {
            throw new RuntimeException("Third jstack output is empty");
        }

        Format format = new DefaultFormat();
        JStack jstack1 = format.parse(jstackStr1);
        JStack jstack2 = format.parse(jstackStr2);
        JStack jstack3 = format.parse(jstackStr3);

        ThreadStack ts1 = jstack1.getThreadStack(TraveledLockDebuggee.THREAD_NAME);
        ThreadStack ts2 = jstack2.getThreadStack(TraveledLockDebuggee.THREAD_NAME);
        ThreadStack ts3 = jstack3.getThreadStack(TraveledLockDebuggee.THREAD_NAME);

        if (ts1 == null || ts2 == null || ts3 == null) {
            throw new RuntimeException(
                    "One of thread stack trace is null in the first jstack output : "
                    + ts1 + ", " + ts2 + ", " + ts3);
        }

        MonitorInfo monitorInfo1 = null;
        MonitorInfo monitorInfo2 = null;
        MonitorInfo monitorInfo3 = null;

        Iterator<MethodInfo> it = ts1.getStack().iterator();
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(TraveledLockDebuggee.class.getName() + ".a")) {
                monitorInfo1 = haveToHaveOneLock(mi);
            }
        }

        it = ts2.getStack().iterator();
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(TraveledLockDebuggee.class.getName() + ".a")) {
                haveToBeEmpty(mi);
            } else if (mi.getName().startsWith(TraveledLockDebuggee.class.getName() + ".b")) {
                monitorInfo2 = haveToHaveOneLock(mi);
            }
        }

        it = ts3.getStack().iterator();
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(TraveledLockDebuggee.class.getName() + ".a")
                    || mi.getName().startsWith(TraveledLockDebuggee.class.getName() + ".b")) {
                haveToBeEmpty(mi);
            } else if (mi.getName().startsWith(TraveledLockDebuggee.class.getName() + ".c")) {
                monitorInfo3 = haveToHaveOneLock(mi);
            }
        }

        System.out.println("All monitors found - passed");
    }

    private MonitorInfo haveToHaveOneLock(MethodInfo mi) {
        if (mi.getLocks().size() == 1) {
            System.out.println("Method \"" + mi.getName()
                    + "\" contain 1 lock - correct");
            return mi.getLocks().getFirst();
        } else {
            throw new RuntimeException("Lock count ("
                    + mi.getLocks().size() + ") is incorrect in method \""
                    + mi.getName() + "\"");
        }
    }

    private void haveToBeEmpty(MethodInfo mi) {
        if (mi.getLocks().size() == 0) {
            System.out.println("Method \"" + mi.getName()
                    + "\" does not lock anything - correct");
        } else {
            throw new RuntimeException(
                    "Unexpected lock found in method \"" + mi.getName() + "\"");
        }
    }

}
