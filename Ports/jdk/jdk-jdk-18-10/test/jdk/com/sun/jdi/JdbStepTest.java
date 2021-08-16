/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163127
 * @summary Debugger classExclusionFilter does not work correctly with method references
 *
 * @library /test/lib
 * @compile -g JdbStepTest.java
 * @run main/othervm JdbStepTest
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class JdbStepTestTarg {

    public static void actualMethod(long[] input, long min, long max) {
        Map<Long, List<Long>> lookup = new HashMap<>();  //@2 breakpoint, just a marker,
                                                         // not a real breakpoint
        long range = max - min + 1;
        for (long number : input) {
            lookup.compute(number / range, (key, list) -> list != null ? list :
                    new ArrayList<>()).add(number);
        }
    }

    interface Func {
        void call(long[] input, long min, long max);
    }

    public static void main(String args[]) {
        Func methodRef = JdbStepTestTarg::actualMethod;
        methodRef.call(new long[]{1, 2, 3, 4, 5, 6}, 1, 6);  //@1 breakpoint
    }

}

public class JdbStepTest extends JdbTest {
    public static void main(String argv[]) {
        new JdbStepTest().run();
    }

    private JdbStepTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = JdbStepTestTarg.class.getName();
    private static final String PATTERN_TEMPLATE = "^Step completed: \"thread=main\", " +
            "JdbStepTestTarg\\.actualMethod\\(\\), line=%LINE_NUMBER.*\\R" +
            "%LINE_NUMBER\\s+Map<Long, List<Long>> lookup = new HashMap<>\\(\\);.*\\R";

    @Override
    protected void runCases() {

        setBreakpoints(jdb, DEBUGGEE_CLASS, System.getProperty("test.src") +
                "/JdbStepTest.java", 1);

        int expectedLineToStopAfterStep = parseBreakpoints(getTestSourcePath("JdbStepTest.java"),
                2).get(0);

        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.step());

        String pattern = PATTERN_TEMPLATE.replaceAll("%LINE_NUMBER",
                String.valueOf(expectedLineToStopAfterStep));
        new OutputAnalyzer(jdb.getJdbOutput()).shouldMatch(pattern);
    }
}
