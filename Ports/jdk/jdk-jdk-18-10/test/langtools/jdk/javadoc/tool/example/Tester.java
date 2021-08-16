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

/*
 * @test
 * @bug 8164316
 * @summary tests the example used in package-info.java and doclet options.
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner Example
 * @run main Tester
 */

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import toolbox.*;
import toolbox.Task.Expect;

import static toolbox.Task.OutputKind.*;

public class Tester extends TestRunner {
    final ToolBox tb;
    final File testFile;
    final File testSrc;
    final Class<?> docletClass;
    final static String OV_FN = "overview.html";

    Tester() {
        super(System.err);
        testSrc = new File(System.getProperty("test.src"));
        testFile = new File(testSrc, "Example.java");
        tb = new ToolBox();
           ClassLoader cl = Tester.class.getClassLoader();
        try {
            docletClass = cl.loadClass("Example");
        } catch (ClassNotFoundException cfe) {
            throw new Error(cfe);
        }
    }

    public static void main(String... args) throws Exception {
        new Tester().runTests();
    }

    private Task.Result execTask(String... extraArgs) {
        return execTask(false, extraArgs);
    }

    private Task.Result execTask(boolean isNegative, String... extraArgs) {
        JavadocTask et = new JavadocTask(tb, Task.Mode.API);
        et.docletClass(docletClass);
        List<String> args = new ArrayList<>();
        args.add("-sourcepath");
        args.add(testSrc.getAbsolutePath());
        args.add(testFile.getAbsolutePath());
        args.addAll(Arrays.asList(extraArgs));
        //args.forEach((a -> System.err.println("arg: " + a)));
        System.err.println(Arrays.asList(extraArgs));
        Task.Result result = isNegative
                ? et.options(args).run(Expect.FAIL)
                : et.options(args).run();
        return result;
    }

    void assertPresence(String regex, List<String> output) throws Exception {
        List<String> foundList = tb.grep(regex, output);
        if (foundList.isEmpty()) {
            throw new Exception("Not found, expected: " + regex);
        }
    }

    @Test
    public void testOption() throws Exception {
        Task.Result result = execTask("-overviewfile", OV_FN);
        assertPresence("overviewfile: " + OV_FN, result.getOutputLines(DIRECT));
    }

    @Test
    public void testOptionAlias() throws Exception {
        Task.Result result = execTask("-overview-file", OV_FN);
        assertPresence("overviewfile: " + OV_FN, result.getOutputLines(DIRECT));
    }

    @Test
    public void testOptionAliasDoubleDash() throws Exception {
        Task.Result result = execTask("--over-view-file", OV_FN);
        assertPresence("overviewfile: " + OV_FN, result.getOutputLines(DIRECT));
    }
}
