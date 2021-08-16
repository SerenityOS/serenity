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
 * @bug 4559100
 * @summary The VM crashes when a method in a redefined class throws an exception.
 * @comment converted from test/jdk/com/sun/jdi/RedefineException.sh
 *
 * @library /test/lib
 * @run main/othervm RedefineException
 */

/* This is another symptomm of 4559100
 * This causes a bus error on solsparc:
 *  ---- called from signal handler with signal 10 (SIGBUS) ------
 *    [11] constantPoolOopDesc::klass_at_if_loaded(0xffbed4d8, 0x16, 0xffbed5cc, 0x0, 0x1, 0xfa40b330), at 0xfe11568c
 *    [12] methodOopDesc::fast_exception_handler_bci_for(0x6, 0x1, 0xfe400a0c, 0x0, 0x2d1f0, 0x0), at 0xfe12e620
 *    [13] jvmdi::post_exception_throw_event(0x2d1f0, 0xf61036f8, 0xf6103752, 0xf20414a8, 0x2e2928, 0xfe12e190), at 0xfe2a4fa4
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefineExceptionTarg {
    String str;
    int ii;
    static public void main(String[] args) {
        System.out.println("In Main");
        RedefineExceptionTarg mine = new RedefineExceptionTarg();
        mine.a1();
    }

    public void a1() {
        int a1local = 1;
        String a1string = "a1";

        ii = 89;                                 // @1 delete this line
        str = "foo";
        System.out.println("a1: Calling the original a2/a3.  'The @@@ deleted lines should appear");
        System.out.println("ii = " + ii);        // @1 delete this line
        a2();
    }

    public void a2() {
        int a2local = 2;
        String a2string = "a2";
        //System.out.println("a2: @ @@delete this line");
        try {
            a3();
        } catch (Exception ee) {
            System.out.println("a2: Exception caught");
        }
        System.out.println("a2: done");
    }

    public void a3() throws Exception {
        int a3local = 3;
        String a3string = "a3";
        System.out.println("a3: @@ delete this line");   // If this line is deleted, the test passes!
        System.out.println("a3: @1 breakpoint here a3");
        throw new Exception("This is the exception");
    }
}

public class RedefineException extends JdbTest {

    public static void main(String argv[]) {
        new RedefineException().run();
    }

    private RedefineException() {
        super(RedefineExceptionTarg.class.getName(), "RedefineException.java");
    }

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        redefineClass(1);
        jdb.command(JdbCommand.pop());
        jdb.contToExit(1);
    }
}
