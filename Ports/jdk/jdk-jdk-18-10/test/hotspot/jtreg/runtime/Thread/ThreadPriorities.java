/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7194254
 * @summary Creates several threads with different java priorities and checks
 *      whether jstack reports correct priorities for them.
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @comment Use othervm mode so that we don't capture unrelated threads created by other tests
 * @run main/othervm ThreadPriorities
 */

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import static jdk.test.lib.Asserts.*;

public class ThreadPriorities {

    public static void main(String[] args) throws Throwable {
        final int NUMBER_OF_JAVA_PRIORITIES =
                Thread.MAX_PRIORITY - Thread.MIN_PRIORITY + 1;
        final CyclicBarrier barrier =
                new CyclicBarrier(NUMBER_OF_JAVA_PRIORITIES + 1);

        for (int p = Thread.MIN_PRIORITY; p <= Thread.MAX_PRIORITY; ++p) {
            final int priority = p;
            new Thread("Priority=" + p) {
                {
                    setPriority(priority);
                }
                public void run() {
                    try {
                        barrier.await(); // 1st
                        barrier.await(); // 2nd
                    } catch (Exception exc) {
                        // ignore
                    }
                }
            }.start();
        }
        barrier.await(); // 1st

        int matches = 0;
        ArrayList<String> failed = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(
                JDKToolFinder.getJDKTool("jstack"),
                String.valueOf(ProcessTools.getProcessId()));

        String[] output = new OutputAnalyzer(pb.start()).getOutput().split("\\R");

        Pattern pattern = Pattern.compile(
                "\\\"Priority=(\\d+)\\\".* prio=(\\d+).*");
        for (String line : output) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                matches += 1;
                String expected = matcher.group(1);
                String actual = matcher.group(2);
                if (!expected.equals(actual)) {
                    failed.add(line);
                }
            }
        }
        barrier.await(); // 2nd
        barrier.reset();

        boolean success = false;
        try {
            assertEquals(matches, NUMBER_OF_JAVA_PRIORITIES);
            assertTrue(failed.isEmpty(), failed.size() + ":" + failed);
            success = true;
        }
        finally {
            if (!success) {
                System.out.println("Failure detected - dumping jstack output:");
                for (String line : output) {
                    System.out.println(line);
                }
            }
        }
    }
}

