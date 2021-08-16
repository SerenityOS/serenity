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
 * @bug 4622663
 * @summary redefine and pop top frame from jdb gets assertion failure
 * @comment converted from test/jdk/com/sun/jdi/RedefinePop.sh
 *
 * @library /test/lib
 * @compile -g RedefinePop.java
 * @run main/othervm RedefinePop
 */

/*
 * The failure occurs with debug java when the pop deletes the
 * line that called the method which is being popped.
 */

/*
 * assert(index<len, "should have found method")
 * [8] report_assertion_failure(0xfe2a54d9, 0xfe2a54e3, 0x2cc, 0xfe2a5527, 0xfffffff8, 0x3f2b8), at 0xfda1e5e8
 * [9] methodOopDesc::jni_id(0xf590a2f0, 0x3e868, 0x8, 0xffbed760, 0xf590a3ac, 0xffbed664), at 0xfdcd7a2c
 * [10] JvmdiThreadState::compare_and_set_current_location(0x3f450, 0xf590a2f0, 0xf590a33f, 0x1, 0x1, 0x3e868), at 0xfdc0f670
 * [11] jvmdi::at_single_stepping_point(0x3e868, 0xf590a2f0, 0xf590a33f, 0x5, 0x0, 0x0), at 0xfdc29184
 * [12] InterpreterRuntime::at_safepoint(0x3e868, 0xb6, 0x2, 0xf9c28744, 0xf590a038, 0xffbed880), at 0xfdb0d590
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefinePopTarg {
    static public void main(String[] args) {
        RedefinePopTarg mine = new RedefinePopTarg();
        mine.a1(44);   // @1 delete the call that we are in when the pop occurs
        mine.a4();
    }

    public void a1(int p1) {
        System.out.println("a1: @1 breakpoint here");
    }

    public void a4() {
        System.out.println("a4: The next line should not say Ni!");
        System.out.println("a4: Ni!");   // @1 delete
    }
}

public class RedefinePop extends JdbTest {

    public static void main(String argv[]) {
        new RedefinePop().run();
    }

    private RedefinePop() {
        super(RedefinePopTarg.class.getName(), "RedefinePop.java");
    }

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        redefineClass(1, "-g");
        jdb.command(JdbCommand.pop());
        jdb.contToExit(1);

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldNotContain("Internal exception:");
    }
}
