/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031195
 * @summary JDB allows evaluation of calls to static interface methods
 * @comment converted from test/jdk/com/sun/jdi/EvalInterfaceStatic.sh
 *
 * @library /test/lib
 * @build EvalInterfaceStatic
 * @run main/othervm EvalInterfaceStatic
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

/*
 * The test exercises the ability to invoke static methods on interfaces.
 * Static interface methods are a new feature added in JDK8.
 *
 * The test makes sure that it is possible to invoke an interface
 * static method and that the static methods are not inherited by extending
 * interfaces.
 */
interface EvalStaticInterfaces {
    static String staticMethod1() {
        return "base:staticMethod1";
    }

    static String staticMethod2() {
        return "base:staticMethod2";
    }

    public static void main(String[] args) {
        // prove that these work
        System.out.println("base staticMethod1(): " + EvalStaticInterfaces.staticMethod1());
        System.out.println("base staticMethod2(): " + EvalStaticInterfaces.staticMethod2());
        System.out.println("overridden staticMethod2(): " + ExtendedEvalStaticInterfaces.staticMethod2());
        System.out.println("base staticMethod3(): " + ExtendedEvalStaticInterfaces.staticMethod3());

        gus();
    }

    static void gus() {
        int x = 0;             // @1 breakpoint
    }
}

interface ExtendedEvalStaticInterfaces extends EvalStaticInterfaces {
    static String staticMethod2() {
        return "extended:staticMethod2";
    }

    static String staticMethod3() {
        return "extended:staticMethod3";
    }
}


public class EvalInterfaceStatic extends JdbTest {
    public static void main(String argv[]) {
        new EvalInterfaceStatic().run();
    }

    private EvalInterfaceStatic() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = EvalStaticInterfaces.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("EvalInterfaceStatic.java", 1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        evalShouldContain("EvalStaticInterfaces.staticMethod1()", "base:staticMethod1");

        evalShouldContain("EvalStaticInterfaces.staticMethod2()", "base:staticMethod2");

        evalShouldNotContain("ExtendedEvalStaticInterfaces.staticMethod1()", "base:staticMethod1");

        evalShouldContain("ExtendedEvalStaticInterfaces.staticMethod2()", "extended:staticMethod2");

        evalShouldContain("ExtendedEvalStaticInterfaces.staticMethod3()", "extended:staticMethod3");

        jdb.contToExit(1);
    }

}
