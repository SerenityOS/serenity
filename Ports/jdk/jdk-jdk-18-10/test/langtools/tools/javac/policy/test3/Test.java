/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6813059
 * @summary
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;

// Simple test of --should-stop=at.
// For each of the permissable values, we compile a file with an error in it,
// then using -XDverboseCompilePolicy we check that the compilation gets as
// far as expected, but no further.

public class Test {
    enum ShouldStopPolicy {
        BLANK(false, null, "attr"),
        PROCESS(true, null, "attr"),
        ATTR(true, "attr", "flow"),
        FLOW(true, "flow", "desugar"),
        TRANSTYPES(true, "desugar", "generate"),
        LOWER(true, "desugar", "generate"),
        GENERATE(true, "generate", null);
        ShouldStopPolicy(boolean needOption, String expect, String dontExpect) {
            this.needOption = needOption;
            this.expect = expect;
            this.dontExpect = dontExpect;
        }
        boolean needOption;
        String expect;
        String dontExpect;
    }

    enum CompilePolicy {
        BYFILE,
        BYTODO
    }

    public static void main(String... args) throws Exception {
        new Test().run();
    }

    public void run() throws Exception {
        for (CompilePolicy cp: CompilePolicy.values()) {
            for (ShouldStopPolicy ssp: ShouldStopPolicy.values()) {
                test(cp, ssp);
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    public void test(CompilePolicy cp, ShouldStopPolicy ssp) {
        System.err.println();
        System.err.println("test " + cp + " " + ssp);
        List<String> args = new ArrayList<String>();
        args.add("-XDverboseCompilePolicy");
        args.add("-XDcompilePolicy=" + cp.toString().toLowerCase());
        args.add("-d");
        args.add(".");
        if (ssp.needOption)
            args.add("--should-stop=at=" + ssp);
        args.add(new File(System.getProperty("test.src", "."), "A.java").getPath());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        System.err.println("compile " + args);
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        if (rc == 0)
            throw new Error("compilation succeeded unexpectedly");
        //System.err.println(sw);

        // The following is a workaround for the current javac implementation,
        // that in bytodo mode, it will still attribute files after syntax errors.
        // Changing that behavior may surprise existing users, so for now, we
        // work around it.
        if (cp == CompilePolicy.BYTODO && ssp == ShouldStopPolicy.PROCESS)
            ssp = ShouldStopPolicy.ATTR;

        boolean foundExpected = (ssp.expect == null);
        String[] lines = sw.toString().split("\n");
        for (String line: lines) {
            if (ssp.expect != null && line.startsWith("[" + ssp.expect))
                foundExpected = true;
            if (ssp.dontExpect != null && line.startsWith("[" + ssp.dontExpect)) {
                error("Unexpected output: " + ssp.dontExpect + "\n" + sw);
                return;
            }
        }

        if (!foundExpected)
            error("Expected output not found: " + ssp.expect + "\n" + sw);
    }

    void error(String message) {
        System.err.println(message);
        errors++;
    }

    int errors;
}
