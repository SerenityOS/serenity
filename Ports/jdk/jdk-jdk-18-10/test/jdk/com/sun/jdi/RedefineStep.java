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
 * @bug 4689395
 * @summary "step over" after a class is redefined acts like "step out"
 * @comment converted from test/jdk/com/sun/jdi/RedefineStep.sh
 *
 * @library /test/lib
 * @compile -g RedefineStep.java
 * @run main/othervm RedefineStep
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefineStepTarg {
    static int counter;
    static public void main(String[] args) {
        RedefineStepTarg mine = new RedefineStepTarg();
        mine.a1(10);
        System.out.println("done");  // should not see this
    }

    public void a1(int p1) {
        System.out.println("jj0");   // @1 breakpoint   line 10
        a2();
        System.out.println("jj3");    // @1 delete
    }
    public void a2() {
        System.out.println("a2");
    }
}

public class RedefineStep extends JdbTest {

    public static void main(String argv[]) {
        new RedefineStep().run();
    }

    private RedefineStep() {
        super(RedefineStepTarg.class.getName(), "RedefineStep.java");
    }

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        redefineClass(1, "-g");

        jdb.command(JdbCommand.next());
        jdb.command(JdbCommand.next());
        jdb.command(JdbCommand.next());
        jdb.command(JdbCommand.next());
        jdb.command(JdbCommand.next());

        new OutputAnalyzer(getJdbOutput())
                .shouldNotContain("should not see this");
    }
}
