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
 * @summary Checks that jstack correctly prints the thread names
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library ../share
 * @run main/othervm -XX:+UsePerfData ThreadNamesTest
 */
import common.ToolResults;
import utils.*;

public class ThreadNamesTest {

    private static final String STRANGE_NAME = "-_?+!@#$%^*()";
    private static final String LONG_NAME = "loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong";

    static class NamedThread extends Thread {

        NamedThread(String name) {
            setName(name);
        }

        @Override
        public void run() {
            Utils.sleep();
        }
    }

    public static void main(String[] args) throws Exception {
        testWithName(STRANGE_NAME);
        testWithName("");
        testWithName(LONG_NAME);
    }

    private static void testWithName(String name) throws Exception {
        // Start a thread with some strange name
        NamedThread thread = new NamedThread(name);
        thread.start();

        // Run jstack tool and collect the output
        JstackTool jstackTool = new JstackTool(ProcessHandle.current().pid());
        ToolResults results = jstackTool.measure();

        // Analyze the jstack output for the strange thread name
        JStack jstack1 = new DefaultFormat().parse(results.getStdoutString());
        ThreadStack ti1 = jstack1.getThreadStack(name);

        if (ti1 == null) {
            throw new RuntimeException("jstack output doesn't contain thread info for the thread '" + name + "'");
        }
    }

}
