/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.io.*;

public class OutputRedirect {
    private static final PrintStream originalOutput = System.err;

    public static void main(String[] args) {
        try {
            doTest();
        } finally {
            // restore things
            System.setOut(originalOutput);
        }
    }

    static void doTest() {
        ByteArrayOutputStream redirectedOutput = new ByteArrayOutputStream();

        // redirect System.err to a buffer
        System.setErr(new PrintStream(redirectedOutput));

        PrintWriter sink = new PrintWriter(new ByteArrayOutputStream());

        // execute javadoc
        int result = jdk.javadoc.internal.tool.Main.execute(new String[] {"p"}, sink);

        // tests whether javadoc wrote to System.out
        if (redirectedOutput.toByteArray().length > 0) {
            originalOutput.println("Test failed; here's what javadoc wrote on its standard output:");
            originalOutput.println(redirectedOutput.toString());
            throw new Error("javadoc output wasn\'t properly redirected");
        } else if (result != 0) {
            throw new Error("javadoc run failed");
        } else {
            originalOutput.println("OK, good");
        }
    }
}
