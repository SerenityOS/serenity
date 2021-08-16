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
 * @bug 4660756
 * @summary TTY: Need to clear source cache after doing a redefine class
 * @comment converted from test/jdk/com/sun/jdi/RedefineTTYLineNumber.sh
 *
 * @library /test/lib
 * @compile -g RedefineTTYLineNumber.java
 * @run main/othervm RedefineTTYLineNumber
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RedefineTTYLineNumberTarg {

    public void B() {
        System.out.println("in B: @1 delete"); // delete 1 line before A method
    }

    public void A() {
        System.out.println("expected statement printed by jdb");
    }

    public static void main(String[] args) {
        RedefineTTYLineNumberTarg untitled41 = new RedefineTTYLineNumberTarg();
        untitled41.A();
        System.out.println("done");
    }
}

public class RedefineTTYLineNumber extends JdbTest {

    public static void main(String argv[]) {
        new RedefineTTYLineNumber().run();
    }

    private RedefineTTYLineNumber() {
        super(DEBUGGEE_CLASS, SOURCE_FILE);
    }

    private final static String DEBUGGEE_CLASS = RedefineTTYLineNumberTarg.class.getName();
    private final static String SOURCE_FILE = "RedefineTTYLineNumber.java";

    // parses line number from the jdb "Breakpoint hit" message
    private static int parseLineNum(String s) {
        // Breakpoint hit: "thread=main", RedefineTTYLineNumberTarg.A(), line=49 bci=0
        // 49            System.out.println("expected statement printed by jdb");
        Matcher m = Pattern.compile("\\bline=(\\d+)\\b").matcher(s);
        if (!m.find()) {
            throw new RuntimeException("could not parse line number");
        }
        return Integer.parseInt(m.group(1));
    }

    private void verifyBPSource(int n, String reply) {
        if (!reply.contains("expected statement printed by jdb")) {
            throw new RuntimeException("Breakpoint source (" + n + ") is not correct");
        }
    }

    @Override
    protected void runCases() {
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "A"));
        String bp1Reply = execCommand(JdbCommand.run()).getStdout();
        int bp1Line = parseLineNum(bp1Reply);
        redefineClass(1, "-g");
        jdb.command(JdbCommand.pop());
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "A"));
        String bp2Reply = execCommand(JdbCommand.cont()).getStdout();
        int bp2Line = parseLineNum(bp2Reply);

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldNotContain("Internal exception:");
        // 1 line is deleted before RedefineTTYLineNumberTarg.A(),
        // so bp2Line should be equals bp1Line-1
        Asserts.assertEquals(bp2Line, bp1Line - 1, "BP line numbers");
        verifyBPSource(1, bp1Reply);
        // uncomment the following line to reproduce JDK-8210927
        //verifyBPSource(2, bp2Reply);
    }
}
