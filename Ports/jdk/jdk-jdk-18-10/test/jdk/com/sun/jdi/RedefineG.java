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
 * @bug 4777868
 * @summary Compile with java -g, do a RedefineClasses, and you don't get local vars
 * @comment converted from test/jdk/com/sun/jdi/Redefine-g.sh
 *
 * @library /test/lib
 * @compile RedefineG.java
 * @run main/othervm RedefineG
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;


// Compile the first version without -g and the 2nd version with -g.
class RedefineGTarg {
    public RedefineGTarg() {
    }
    public static void main(String[] args){
        int gus=22;
        RedefineGTarg kk=new RedefineGTarg();
        kk.m1("ab");
      }

    void m1(String p1) {
        int m1l1 = 1;
        System.out.println("m1(String) called");
        m1(p1, "2nd");
        // @1 uncomment System.out.println("Hello Milpitas!");
    }

    void m1(String p1, String p2) {
        int m1l2 = 2;
        System.out.println("m2" + p1 + p2);  // @1 breakpoint
    }

}

public class RedefineG extends JdbTest {
    public static void main(String argv[]) {
        new RedefineG().run();
    }

    private RedefineG() {
        super(DEBUGGEE_CLASS,
                "RedefineG.java");
    }

    private static final String DEBUGGEE_CLASS = RedefineGTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.where(""));
        jdb.command(JdbCommand.locals());

        redefineClass(1, "-g");
        jdb.command(JdbCommand.where(""));
        jdb.command(JdbCommand.locals());

        jdb.command(JdbCommand.pop());
        jdb.command(JdbCommand.where(""));
        jdb.command(JdbCommand.locals());

        jdb.command(JdbCommand.pop());
        jdb.command(JdbCommand.where(""));
        jdb.command(JdbCommand.locals());

        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldContain("p1 = \"ab\"")
                .shouldContain("p2 = \"2nd\"")
                .shouldContain("m1l2 = 2")
                .shouldNotContain("m1l1")
                .shouldContain("args = instance of java.lang.String")
                .shouldContain("gus = 22")
                .shouldContain("kk = instance of " + DEBUGGEE_CLASS);
    }
}
