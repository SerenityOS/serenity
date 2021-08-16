/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004832 8000103
 * @summary Add new doclint package
 * @summary Create doclint utility
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jdk.javadoc.internal.doclint.DocLint;
import jdk.javadoc.internal.doclint.DocLint.BadArgs;

/** javadoc error on toplevel:  a & b. */
public class RunTest {
    /** javadoc error on member: a < b */
    public static void main(String... args) throws Exception {
        new RunTest().run();
    }


    File testSrc = new File(System.getProperty("test.src"));
    File thisFile = new File(testSrc, RunTest.class.getSimpleName() + ".java");

    void run() throws Exception {
        for (Method m: getClass().getDeclaredMethods()) {
            Annotation a = m.getAnnotation(Test.class);
            if (a != null) {
                System.err.println("test: " + m.getName());
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);;
                    m.invoke(this, new Object[] { pw });
                    String out = sw.toString();
                    System.err.println(">>> " + out.replace("\n", "\n>>> "));
                    if (!out.contains("a < b"))
                        error("\"a < b\" not found");
                    if (!out.contains("a & b"))
                        error("\"a & b\" not found");
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    throw (cause instanceof Exception) ? ((Exception) cause) : e;
                }
                System.err.println();
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }


    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    /** Marker annotation for test cases. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Test { }

    @Test
    void testMain(PrintWriter pw) throws BadArgs, IOException {
        String[] args = { "-Xmsgs", thisFile.getPath() };
        DocLint d = new DocLint();
        d.run(pw, args);
    }
}


