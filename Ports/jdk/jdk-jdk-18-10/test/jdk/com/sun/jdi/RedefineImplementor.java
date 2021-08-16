/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6173560
 * @summary Redefine a class that implements an interface
 *          and verify that a subclass calls the right method.
 * @comment converted from test/jdk/com/sun/jdi/RedefineImplementor.sh
 *
 * @library /test/lib
 * @compile -g RedefineImplementor.java
 * @run main/othervm RedefineImplementor
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefineImplementorTarg implements Runnable {
    public void run() {
        System.out.println("RedefineImplementorTarg's run");
        // @1 uncomment System.out.println("This is the new version of RedefineImplementorTarg");
    }

    public static void main(String[] args) {
        Runnable r = new RedefineImplementorB();
        RedefineImplementorB.func(r);
        RedefineImplementorB.func(r);  // @1 breakpoint
    }

}

class RedefineImplementorB extends RedefineImplementorTarg {
    static void func(Runnable r) {
        r.run();
    }
}

public class RedefineImplementor extends JdbTest {
    public static void main(String argv[]) {
        new RedefineImplementor().run();
    }

    private RedefineImplementor() {
        super(RedefineImplementorTarg.class.getName(),
                "RedefineImplementor.java");
    }

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());

        redefineClass(1, "-g");

        jdb.contToExit(1);

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldContain("This is the new version of ");
    }
}
