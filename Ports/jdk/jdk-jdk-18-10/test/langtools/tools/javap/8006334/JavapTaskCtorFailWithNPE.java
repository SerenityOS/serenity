/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006334
 * @summary javap: JavapTask constructor breaks with null pointer exception if
 * parameter options is null
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.File;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import com.sun.tools.javap.JavapFileManager;
import com.sun.tools.javap.JavapTask;

public class JavapTaskCtorFailWithNPE {

    //we will also check the output just to confirm that we get the expected one
    private static final String expOutput =
        "Compiled from \"JavapTaskCtorFailWithNPE.java\"\n" +
        "public class JavapTaskCtorFailWithNPE {\n" +
        "  public JavapTaskCtorFailWithNPE();\n" +
        "  public static void main(java.lang.String[]);\n" +
        "}\n";

    public static void main(String[] args) {
        new JavapTaskCtorFailWithNPE().run();
    }

    private void run() {
        File classToCheck = new File(System.getProperty("test.classes"),
            getClass().getSimpleName() + ".class");

        DiagnosticCollector<JavaFileObject> dc =
                new DiagnosticCollector<JavaFileObject>();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        JavaFileManager fm = JavapFileManager.create(dc, pw);
        JavapTask t = new JavapTask(pw, fm, dc, null,
                Arrays.asList(classToCheck.getPath()));
        if (t.run() != 0)
            throw new Error("javap failed unexpectedly");

        List<Diagnostic<? extends JavaFileObject>> diags = dc.getDiagnostics();
        for (Diagnostic<? extends JavaFileObject> d: diags) {
            if (d.getKind() == Diagnostic.Kind.ERROR)
                throw new AssertionError(d.getMessage(Locale.ENGLISH));
        }
        String lineSep = System.getProperty("line.separator");
        String out = sw.toString().replace(lineSep, "\n");
        if (!out.equals(expOutput)) {
            throw new AssertionError("The output is not equal to the one expected");
        }
    }

}
