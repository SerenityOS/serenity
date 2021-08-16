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
 * @bug 4467887 4913748
 * @summary TTY: NullPointerException at com.sun.tools.jdi.MirrorImpl.validateMirrors
 * @comment converted from test/jdk/com/sun/jdi/NotAField.sh
 *
 * @library /test/lib
 * @run main/othervm NotAField
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class NotAFieldTarg {
    public static void main(String args[]) {
        System.out.println("Hello, world!");
    }
}

public class NotAField extends JdbTest {
    public static void main(String argv[]) {
        new NotAField().run();
    }

    private NotAField() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = NotAFieldTarg.class.getName();

    @Override
    protected void runCases() {
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "main"));
        jdb.command(JdbCommand.run());

        // This works:
        jdb.command(JdbCommand.print("java.lang.Class.reflectionFactory.hashCode()"));
        // This should result in a ParseException: ("No such field in ..."):
        jdb.command(JdbCommand.print("java.lang.Class.reflectionFactory.hashCode"));
        jdb.contToExit(1);

        new OutputAnalyzer(jdb.getJdbOutput())
                .shouldContain("com.sun.tools.example.debug.expr.ParseException");
    }
}
