/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4629548
 * @summary Deferred StepRequests are lost in multithreaded debuggee
 * @comment converted from test/jdk/com/sun/jdi/DeferredStepTest.sh
 *
 * @library /test/lib
 * @build DeferredStepTest
 * @run main/othervm DeferredStepTest
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class DeferredStepTestTarg {
    static class  jj1 implements Runnable {
        public void  run() {
            int count = 0;

            for (int ii = 0; ii < 15; ii++) {
                int intInPotato04 = 666;
                ++count;                        // @1 breakpoint
                System.out.println("Thread: " + Thread.currentThread().getName());
            }
        }
    }

    static class jj2 implements Runnable {
        public void run() {
            int count2 = 0;

            for (int ii = 0; ii < 15; ii++) {
                String StringInPotato05 = "I am";
                ++count2;                           // @2 breakpoint
                System.out.println("Thread: " + Thread.currentThread().getName());
            }
        }
    }

    public static void  main(String argv[]) {
        System.out.println("Version = " + System.getProperty("java.version"));

        jj1 obj1 = new jj1();
        jj2 obj2 = new jj2();
        new Thread(obj1, "jj1").start();
        new Thread(obj2, "jj2").start();
    }
}

public class DeferredStepTest extends JdbTest {
    public static void main(String argv[]) {
        new DeferredStepTest().run();
    }

    private DeferredStepTest() {
        super(DeferredStepTestTarg.class.getName());
    }

    private static class ThreadData {
        int lastLine = -1;  // line of the last stop
        int minLine = -1;   // min line (-1 means "not known yet")
        int maxLine = -1;   // max line (-1 means "not known yet")
    }

    private Map<String, ThreadData> threadData = new HashMap<>();

    private Pattern threadRegexp = Pattern.compile("^(.+)\\[\\d+\\].*");
    private Pattern lineRegexp = Pattern.compile("^(\\d+)\\b.*", Pattern.MULTILINE);

    // returns the 1st group of the pattern.
    private String parse(Pattern p, String input) {
        Matcher m = p.matcher(input);
        if (!m.find()) {
            throw new RuntimeException("Input '" + input + "' does not matches '" + p.pattern() + "'");
        }
        return m.group(1);
    }

    private void next() {
        List<String> reply = jdb.command(JdbCommand.next());
        /*
         * Each "next" produces something like ("Breakpoint hit" line only if the line has BP)
         *   Step completed:
         *     Breakpoint hit: "thread=jj2", DeferredStepTestTarg$jj2.run(), line=74 bci=12
         *     74                    ++count2;                           // @ 2 breakpoint
         *     <empty line>
         *     jj2[1]
         */
        // detect thread from the last line
        String lastLine = reply.get(reply.size() - 1);
        String threadName = parse(threadRegexp, lastLine);
        String wholeReply = reply.stream().collect(Collectors.joining(Utils.NEW_LINE));
        int lineNum = Integer.parseInt(parse(lineRegexp, wholeReply));

        System.out.println("got: thread=" + threadName + ", line=" + lineNum);

        ThreadData data = threadData.get(threadName);
        if (data == null) {
            data = new ThreadData();
            threadData.put(threadName, data);
        }
        processThreadData(threadName, lineNum, data);
    }

    private void processThreadData(String threadName, int lineNum, ThreadData data) {
        int lastLine = data.lastLine;
        data.lastLine = lineNum;
        if (lastLine < 0) {
            // the 1st stop in the thread
            return;
        }
        if (lineNum == lastLine + 1) {
            // expected.
            return;
        }
        if (lineNum < lastLine) {
            // looks like step to the beginning of the cycle
            if (data.minLine > 0) {
                // minLine and maxLine are not set - verify
                Asserts.assertEquals(lineNum, data.minLine, threadName + " - minLine");
                Asserts.assertEquals(lastLine, data.maxLine, threadName + " - maxLine");
            } else {
                // set minLine/maxLine
                data.minLine = lineNum;
                data.maxLine = lastLine;
            }
            return;
        }
        throw new RuntimeException(threadName + " (line " + lineNum + ") - unexpected."
                + " lastLine=" + lastLine + ", minLine=" + data.minLine + ", maxLine=" + data.maxLine);
    }

    @Override
    protected void runCases() {
        setBreakpoints(jdb, DeferredStepTestTarg.jj1.class.getName(),
                getTestSourcePath("DeferredStepTest.java"), 1);
        setBreakpoints(jdb, DeferredStepTestTarg.jj2.class.getName(),
                getTestSourcePath("DeferredStepTest.java"), 2);

        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        // 2 cycles (15 iterations) with 4 lines each, 1st break at 3rd line - 58 stops
        for (int i = 0; i < 50; i++) {
            next();
        }
    }
}
