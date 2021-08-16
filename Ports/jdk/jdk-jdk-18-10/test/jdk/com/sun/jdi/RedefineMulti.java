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
 * @bug 4724076
 * @summary Redefine does not work in for/while loop
 * @comment converted from test/jdk/com/sun/jdi/RedefineMulti.sh
 *
 * @library /test/lib
 * @compile -g RedefineMulti.java
 * @run main/othervm RedefineMulti
 */

/*
 * The failure occurs when a method is active and
 * a method that it calls multiple times is redefined
 * more than once.
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefineMultiTarg {

    String field1;
    String field2;

    // The first time thru the loop in start,
    // "Before update..." should be printed.
    // After the first redefine, "After update..." should be printed
    // After the 2nd redefine, "abcde..." should be printed.
    // The bug is that "After update..." is printed instead because
    // stat() calls version 2 of doSomething() instead of
    // version 3.
    private void doSomething()  {
        System.out.println("Before update...");  // @1 commentout
        // @1 uncomment System.out.println("After update...");  // @2 commentout
        // @2 uncomment System.out.println("abcde...");
    }

    public void start() {
        for (int i=0; i < 3; i++)   {
            doSomething();      // @1 breakpoint here  line 16
            System.out.println("field1 = " + field1);
            System.out.println("field2 = " + field2);
        }
        // Redefinex myx = new Redefinex();
        //  for (int i = 0; i < 5; i++) {
        //    myx.methodx1();                     // line 22
        //    System.out.println("fieldx1 = " + myx.fieldx1);
        //    System.out.println("fieldx2 = " + myx.fieldx2);
        //  }
    }

    public static void main(String[] args) {
        RedefineMultiTarg xxx = new RedefineMultiTarg();
        xxx.field1 = "field1";
        xxx.field2 = "field2";
        xxx.start();
    }
}

class Redefinex {
    public String fieldx1;
    public String fieldx2;

    Redefinex() {
        fieldx1 = "fieldx1";
        fieldx2 = "fieldx2";
    }

    public void methodx1() {
        System.out.println("redefinex 1");
        //System.out.println("redefinex 2");
        //System.out.println("redefinex 3");
    }
}

/*********
Steps to reproduce this problem:
   a. add line breakpoint  in start()
   b. debug
   c. when breakpoint is hit, type continue. You should see output "Before update..."
   d. change "Before update" to  "After update"
   e. redefine,  and set line breakpoint (see step a)
   f. type continue. You should see output "After update"
   g. change "After update" to "abcde"
   h. redefine, and set line breakpoint (see step a)
   i.  type continue. The output is shown as "After update"
   j. to see "abcde" output,  users will have to pop the stack, and re-execute method start().
************/

public class RedefineMulti extends JdbTest {

    public static void main(String argv[]) {
        new RedefineMulti().run();
    }

    private RedefineMulti() {
        super(RedefineMultiTarg.class.getName(), "RedefineMulti.java");
    }

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.cont());
        redefineClass(1, "-g");
        setBreakpoints(1);
        jdb.command(JdbCommand.cont());
        redefineClass(2, "-g");
        jdb.contToExit(1);

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldNotContain("Internal exception:")
                .shouldContain("abcde");
    }
}
