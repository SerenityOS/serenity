/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8149743
 * @summary crash when adding a breakpoint after redefining to add a private static method
 * @comment converted from test/jdk/com/sun/jdi/RedefineAddPrivateMethod.sh
 *
 * @library /test/lib
 * @compile -g RedefineAddPrivateMethod.java
 * @run main/othervm RedefineAddPrivateMethod
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefineAddPrivateMethodTarg {
    static public void main(String[] args) {
        System.out.println("@1 breakpoint");
        System.out.println("@2 breakpoint");
    }

    // @1 uncomment private static void test() {}
}

public class RedefineAddPrivateMethod extends JdbTest {
    static private final String ALLOW_ADD_DELETE_OPTION = "-XX:+AllowRedefinitionToAddDeleteMethods";

    public static void main(String argv[]) {
        RedefineAddPrivateMethod test = new RedefineAddPrivateMethod();
        test.launchOptions.addVMOptions(ALLOW_ADD_DELETE_OPTION);
        test.run();
    }

    private RedefineAddPrivateMethod() {
        super(DEBUGGEE_CLASS, SOURCE_FILE);
    }

    private static final String DEBUGGEE_CLASS = RedefineAddPrivateMethodTarg.class.getName();
    private static final String SOURCE_FILE = "RedefineAddPrivateMethod.java";

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());

        redefineClass(1, "-g");
        // ensure "test()" method has been added successfully
        execCommand(JdbCommand.eval(DEBUGGEE_CLASS + ".test()"))
                .shouldNotContain("ParseException");

        setBreakpoints(2);
        jdb.command(JdbCommand.run());

        jdb.quit();

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldNotContain("Internal exception:");
    }
}
