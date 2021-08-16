/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047183
 * @summary JDK build fails with sjavac enabled
 * @modules jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper
 * @run main Wrapper IgnoreSymbolFile
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;

public class IgnoreSymbolFile {
    public static void main(String... args) throws Exception {
        IgnoreSymbolFile test = new IgnoreSymbolFile();
        test.run();
    }

    void run() throws Exception {
        String body =
                "package p;\n"
                + "import sun.reflect.annotation.*;\n"
                + "class X {\n"
                + "    ExceptionProxy proxy;"
                + "}";
        writeFile("src/p/X.java", body);

        new File("classes").mkdirs();

        int rc1 = compile("-d", "classes",
                          "--state-dir=classes",
                          "-Werror",
                          "src");
        if (rc1 == 0)
            error("compilation succeeded unexpectedly");

        int rc2 = compile("-d", "classes",
                          "--state-dir=classes",
                          "-Werror",
                          "-XDignore.symbol.file=true",
                          "src");
        if (rc2 != 0)
            error("compilation failed unexpectedly: rc=" + rc2);

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    int compile(String... args) throws ReflectiveOperationException {
        // Use reflection to avoid a compile-time dependency on sjavac Main
        System.err.println("compile: " + Arrays.toString(args));
        Class<?> c = Class.forName("com.sun.tools.sjavac.Main");
        Method m = c.getDeclaredMethod("go", String[].class);
        int rc = (Integer) m.invoke(null, (Object) args);
        System.err.println("rc=" + rc);
        return rc;
    }

    void writeFile(String path, String body) throws IOException {
        File f = new File(path);
        if (f.getParentFile() != null)
            f.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(f)) {
            w.write(body);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
