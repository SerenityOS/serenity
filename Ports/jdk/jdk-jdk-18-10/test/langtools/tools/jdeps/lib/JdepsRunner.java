/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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


import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/**
 * JdepsRunner class to invoke jdeps with the given command line argument
 */
public class JdepsRunner {
    private static final ToolProvider JDEPS_TOOL = ToolProvider.findFirst("jdeps")
        .orElseThrow(() -> new RuntimeException("jdeps tool not found"));

    public static JdepsRunner run(String... args) {
        JdepsRunner jdeps = new JdepsRunner(args);
        int rc = jdeps.run(true);
        if (rc != 0)
           throw new Error("jdeps failed: rc=" + rc);
        return jdeps;
    }

    final StringWriter stdout = new StringWriter();
    final StringWriter stderr = new StringWriter();
    final String[] args;
    public JdepsRunner(String... args) {
        System.out.println("jdeps " + Arrays.stream(args)
                                            .collect(Collectors.joining(" ")));
        this.args = args;
    }

    public JdepsRunner(List<String> args) {
        this(args.toArray(new String[0]));
    }

    public int run() {
        return run(false);
    }

    public int run(boolean showOutput) {
        try (PrintWriter pwout = new PrintWriter(stdout);
             PrintWriter pwerr = new PrintWriter(stderr)) {
            int rc = JDEPS_TOOL.run(pwout, pwerr, args);
            if (showOutput) {
                printStdout(System.out);
                printStderr(System.out);
            }
            return rc;
        }
    }

    public boolean outputContains(String s) {
        return stdout.toString().contains(s) || stderr.toString().contains(s);
    }

    public void printStdout(PrintStream stream) {
        stream.println(stdout.toString());
    }

    public void printStderr(PrintStream stream) {
        stream.println(stderr.toString());
    }

    public String[] output() {
        String lineSep = System.getProperty("line.separator");
        return stdout.toString().split(lineSep);
    }
}
