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
 * @summary Create daemon and non-deamon threads.
 *          Check the correctness of thread's status from jstack.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData DaemonThreadTest
 */
import common.ToolResults;
import utils.*;

public class DaemonThreadTest {

    static class NormalThread extends Thread {

        NormalThread() {
        }

        @Override
        public void run() {
            Utils.sleep();
        }

    }

    static class DaemonThread extends Thread {

        DaemonThread() {
            setDaemon(true);
        }

        @Override
        public void run() {
            Utils.sleep();
        }

    }

    public static void main(String[] args) throws Exception {
        testNoDaemon();
        testDaemon();
    }

    private static void testNoDaemon() throws Exception {
        testThread(new NormalThread(), "");
    }

    private static void testDaemon() throws Exception {
        testThread(new DaemonThread(), "daemon");
    }

    private static void testThread(Thread thread, String expectedType) throws Exception {
        // Start the thread
        thread.start();

        // Run jstack tool and collect the output
        JstackTool jstackTool = new JstackTool(ProcessHandle.current().pid());
        ToolResults results = jstackTool.measure();

        // Analyze the jstack output for the correct thread type
        JStack jstack = new DefaultFormat().parse(results.getStdoutString());
        ThreadStack ti = jstack.getThreadStack(thread.getName());

        if (!ti.getType().trim().equals(expectedType)) {
            throw new RuntimeException("incorrect thread type '" + ti.getType() + "' for the thread '" + thread.getName() + "'");
        }

    }

}
