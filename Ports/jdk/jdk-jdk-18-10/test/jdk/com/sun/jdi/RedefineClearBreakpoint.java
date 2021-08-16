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
 * @bug 4705330
 * @summary Netbeans Fix and Continue crashes JVM
 * @comment converted from test/jdk/com/sun/jdi/RedefineClearBreakpoint.sh
 *
 * @library /test/lib
 * @compile -g RedefineClearBreakpoint.java
 * @run main/othervm RedefineClearBreakpoint
 */

/*
 * The failure occurs after a breakpoint is set and then cleared
 * after a class redefinition, in a method that was EMCP.
 * This sequence creates a state in which subsequent operations
 * such as accessing local vars via JVMDI, can cause a hotspot crash.
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import java.util.List;

class RedefineClearBreakpointTarg {

    public RedefineClearBreakpointTarg() {
        int a=23;
        a=m(a);
    }
    public int m(int b){
        int bb=89;
        System.out.println("RedefineClearBreakpointTarg -  constructor" + b); //@1 breakpoint
        return b*b;
    }

    public static void main(java.lang.String[] args) {
        new RedefineClearBreakpointTarg();
        int jj = 0;   //@1 delete
    }

}


public class RedefineClearBreakpoint extends JdbTest {

    public static void main(String argv[]) {
        new RedefineClearBreakpoint().run();
    }

    private RedefineClearBreakpoint() {
        super(DEBUGGEE_CLASS, SOURCE_FILE);
    }

    private static final String DEBUGGEE_CLASS = RedefineClearBreakpointTarg.class.getName();
    private static final String SOURCE_FILE = "RedefineClearBreakpoint.java";

    @Override
    protected void runCases() {
        List<Integer> bps = parseBreakpoints(getTestSourcePath(SOURCE_FILE), 1);
        Asserts.assertEquals(bps.size(), 1, "unexpected breakpoint count");
        jdb.command(JdbCommand.stopAt(DEBUGGEE_CLASS, bps.get(0)));
        jdb.command(JdbCommand.run());
        redefineClass(1, "-g");

        jdb.command(JdbCommand.stopAt(DEBUGGEE_CLASS, bps.get(0)));
        jdb.command(JdbCommand.next());     // This is needed to make the crash happen at the 'locals' cmd

        jdb.command(JdbCommand.clear(DEBUGGEE_CLASS, bps.get(0)));
        jdb.command(JdbCommand.locals());   // The crash happens here

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldNotContain("Internal exception:");
    }
}
