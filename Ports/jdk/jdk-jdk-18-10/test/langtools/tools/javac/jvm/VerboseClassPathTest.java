/*
 * Copyright (c) 2018, Google Inc. All rights reserved.
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
 * @bug 8179373
 * @summary javac -verbose logs the class path multiple times
 * @modules jdk.compiler
 * @run main VerboseClassPathTest
 */

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.spi.ToolProvider;

public class VerboseClassPathTest {
    public static void main(String... args) throws Exception {
        new VerboseClassPathTest().run();
    }

    void run() throws Exception {
        String className = getClass().getName();
        Path testSrc = Paths.get(System.getProperty("test.src"));
        Path file = testSrc.resolve(className + ".java");
        ToolProvider javac = ToolProvider.findFirst("javac").orElseThrow();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        int rc =
                javac.run(
                        pw,
                        pw,
                        "-d",
                        ",",
                        "-source",
                        "8",
                        "-target",
                        "8",
                        "-verbose",
                        file.toString());
        String log = sw.toString();
        System.out.println(log);
        if (rc != 0) {
            throw new Exception("compilation failed: rc=" + rc);
        }
        String expect = "[search path for class files:";
        long count =
                new BufferedReader(new StringReader(log))
                        .lines()
                        .filter(line -> line.startsWith(expect))
                        .count();
        if (count != 1) {
            throw new Exception("expected '" + expect + "' to appear once, actual: " + count);
        }
    }
}
