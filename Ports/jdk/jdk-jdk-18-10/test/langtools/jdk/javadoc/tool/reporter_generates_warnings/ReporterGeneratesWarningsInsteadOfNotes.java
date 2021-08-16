/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224083 8224082
 * @summary javadoc Reporter generates "warning" for Kind.NOTE
 *          NPE in javadoc Reporter
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @compile pkg/MyDoclet.java
 * @run main ReporterGeneratesWarningsInsteadOfNotes
 */

import java.io.PrintWriter;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javadoc.tester.JavadocTester;
import toolbox.JavacTask;
import toolbox.JavadocTask;
import toolbox.TestRunner;
import toolbox.Task;
import toolbox.ToolBox;

public class ReporterGeneratesWarningsInsteadOfNotes extends TestRunner {
    ToolBox tb = new ToolBox();

    static final String[] outputToCheckFor = new String[]{
            ": PACKAGE pkg",
            ": CLASS pkg.MyDoclet",
            ": CLASS pkg.MyDoclet.MyScanner",
            ": CONSTRUCTOR MyScanner()",
            ": METHOD scan(javax.lang.model.element.Element,java.lang.Integer)",
            ": PARAMETER e",
            ": PARAMETER depth",
            ": CLASS pkg.MyDoclet.Option",
            ": FIELD names",
            ": FIELD hasArg",
            ": FIELD description",
            ": CONSTRUCTOR Option(java.lang.String,boolean,java.lang.String)",
            ": PARAMETER names",
            ": PARAMETER hasArg",
            ": PARAMETER description",
            ": METHOD getArgumentCount()",
            ": METHOD getDescription()",
            ": METHOD getKind()",
            ": METHOD getNames()",
            ": METHOD getParameters()",
            ": CONSTRUCTOR MyDoclet()",
            ": FIELD OK",
            ": FIELD verbose",
            ": FIELD reporter",
            ": FIELD options",
            ": METHOD init(java.util.Locale,jdk.javadoc.doclet.Reporter)",
            ": PARAMETER locale",
            ": PARAMETER reporter",
            ": METHOD getName()",
            ": METHOD getSupportedOptions()",
            ": METHOD getSupportedSourceVersion()",
            ": METHOD run(jdk.javadoc.doclet.DocletEnvironment)",
            ": PARAMETER environment"
    };

    ReporterGeneratesWarningsInsteadOfNotes() throws Exception {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        ReporterGeneratesWarningsInsteadOfNotes tester = new ReporterGeneratesWarningsInsteadOfNotes();
        tester.runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testMain(Path base) throws Exception {
        String testSrc = System.getProperty("test.src");
        String testClasses = System.getProperty("test.classes");
        String log = new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", testClasses, "-doclet",  "pkg.MyDoclet", "-sourcepath", testSrc, "pkg")
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        for (String output : outputToCheckFor) {
            if (!log.contains(output)) {
                throw new AssertionError("was expecting to find: " + output);
            }
        }
    }
}
