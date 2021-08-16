/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4099527
 * @summary javadoc tool: want flag to exit nonzero if there were warnings.
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @run main XWerror
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;

public class XWerror implements Doclet {

    private static final String message = "warning message";

    public static void main(String[] args) {
        StringWriter sw = new StringWriter();
        PrintWriter output = new PrintWriter(sw);

        String[] aargs = {
            "-docletpath", System.getProperty("test.classes", "."),
            "-doclet", "XWerror",
            "-Xwerror",
            System.getProperty("test.src", ".") + java.io.File.separatorChar
            + "XWerror.java"
        };
        if (jdk.javadoc.internal.tool.Main.execute(aargs, output) == 0) {
            throw new Error("did not get non-zero exit code");
        }
        if (!sw.toString().contains(message)) {
            throw new Error("expected message not found: " + message);
        }
    }
    Reporter reporter;

    public boolean run(DocletEnvironment root) {
        reporter.print(Diagnostic.Kind.WARNING , message);
        return false;
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
        return;
    }
}
