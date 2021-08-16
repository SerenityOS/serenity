/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4507088
 * @summary TTY: Add a comment delimiter to the jdb command set
 * @comment converted from test/jdk/com/sun/jdi/CommandCommentDelimiter.sh
 *
 * @library /test/lib
 * @build CommandCommentDelimiter
 * @run main/othervm CommandCommentDelimiter
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class CommandCommentDelimiterTarg {
    public static void main(String args[]) {
        System.out.print  ("Hello");
        System.out.print  (", ");
        System.out.print  ("world");
        System.out.println("!");
    }
}


public class CommandCommentDelimiter extends JdbTest {
    public static void main(String argv[]) {
        new CommandCommentDelimiter().run();
    }

    private CommandCommentDelimiter() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = CommandCommentDelimiterTarg.class.getName();

    @Override
    protected void runCases() {
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "main"));
        jdb.command(JdbCommand.run());

        jdb.command(JdbCommand.step());
        jdb.command("#");
        jdb.command("#foo");
        jdb.command("3 #blah");
        jdb.command("# connectors");
        jdb.command(JdbCommand.step());

        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldNotContain("Unrecognized command: '#'.  Try help...")
                .shouldNotContain("Available connectors are");
    }

}
