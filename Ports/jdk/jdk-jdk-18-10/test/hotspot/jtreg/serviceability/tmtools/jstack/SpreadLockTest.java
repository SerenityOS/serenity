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
 *          After checking that lock info is correct invoke another method
 *          and get the lock again. Repeat this action.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData SpreadLockTest
 */
import common.ToolResults;
import java.util.Iterator;
import utils.*;

class SpreadLockDebuggee extends Thread {

    static final String THREAD_NAME = "MyThread";

    SpreadLockDebuggee() {
        setName(THREAD_NAME);
    }

    Object monitor = new Object();

    public void c() {
        synchronized (monitor) {
            Utils.sleep();
        }
    }

    public void b() {
        synchronized (monitor) {
            try {
                while (true) {
                    Thread.sleep(Long.MAX_VALUE);
                }
            } catch (InterruptedException e) {
                c();
            }
        }
    }

    public void a() {
        synchronized (monitor) {
            try {
                while (true) {
                    Thread.sleep(Long.MAX_VALUE);
                }
            } catch (InterruptedException e) {
                b();
            }
        }
    }

    @Override
    public void run() {
        a();
    }

}

public class SpreadLockTest {

    public static void main(String[] args) throws Exception {
        new SpreadLockTest().doTest();
    }

    private void doTest() throws Exception {
        SpreadLockDebuggee debuggee = new SpreadLockDebuggee();

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

    // Analyzing the outputs from the 3 jstack runs
    public void analyse(String result1, String result2, String result3) {
        String jstackStr1 = result1;
        String jstackStr2 = result2;
        String jstackStr3 = result3;

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

        ThreadStack ts1 = jstack1.getThreadStack(SpreadLockDebuggee.THREAD_NAME);
        ThreadStack ts2 = jstack2.getThreadStack(SpreadLockDebuggee.THREAD_NAME);
        ThreadStack ts3 = jstack3.getThreadStack(SpreadLockDebuggee.THREAD_NAME);

        if (ts1 == null || ts2 == null || ts3 == null) {
            throw new RuntimeException(
                    "One of thread stack trace is null in the first jstack output : "
                    + ts1 + ", " + ts2 + ", " + ts3);
        }

        MonitorInfo[] monitorInfo = new MonitorInfo[6];
        int counter = 0;

        Iterator<MethodInfo> it = ts1.getStack().iterator();
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(SpreadLockDebuggee.class.getName() + ".a")) {
                monitorInfo[counter++] = haveToHaveOneLock(mi);
            }
        }

        it = ts2.getStack().iterator();
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(SpreadLockDebuggee.class.getName() + ".a")
                    || mi.getName().startsWith(SpreadLockDebuggee.class.getName() + ".b")) {
                monitorInfo[counter++] = haveToHaveOneLock(mi);
            }
        }

        it = ts3.getStack().iterator();
        while (it.hasNext()) {
            MethodInfo mi = it.next();
            if (mi.getName().startsWith(SpreadLockDebuggee.class.getName() + ".a")
                    || mi.getName().startsWith(SpreadLockDebuggee.class.getName() + ".b")
                    || mi.getName().startsWith(SpreadLockDebuggee.class.getName() + ".c")) {
                monitorInfo[counter++] = haveToHaveOneLock(mi);
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

}
