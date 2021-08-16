/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7026509
 * @summary Cannot use JavaCompiler to create multiple CompilationTasks for partial compilations
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;
import javax.lang.model.element.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

public class TestJavacTask_ParseAttrGen {
    public static void main(String... args) throws Exception {
        new TestJavacTask_ParseAttrGen().run();
    }

    JavaCompiler comp;
    StandardJavaFileManager fm;

    void run() throws Exception {
        comp = ToolProvider.getSystemJavaCompiler();
        fm = comp.getStandardFileManager(null, null, null);
        try {
            final boolean[] booleanValues = { false, true };
            for (boolean pk: booleanValues) {
                for (boolean ak: booleanValues) {
                    for (boolean gk: booleanValues) {
                        test(pk, ak, gk);
                    }
                }
            }
        } finally {
            fm.close();
        }
    }

    void test(boolean pk, boolean ak, boolean gk) throws Exception {
        if (!pk && !ak && !gk)  // nothing to do
            return;

        System.err.println("test: pk:" + pk + ", ak:" + ak + ", gk: " + gk);
        File testSrc = new File(System.getProperty("test.src"));
        String thisClassName = TestJavacTask_ParseAttrGen.class.getName();
        Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjects(new File(testSrc, thisClassName + ".java"));
        File tmpDir = new File((pk ? "p" : "") + (ak ? "a" : "") + (gk ? "g" : ""));
        tmpDir.mkdirs();
        fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tmpDir));
        JavacTask t = (JavacTask) comp.getTask(null, fm, null, null, null, files);
        //t.setTaskListener(createTaskListener());

        try {
            if (pk) {
                Iterable<? extends CompilationUnitTree> trees = t.parse();
                System.err.println(count(trees) + " trees parsed");
            }

            if (ak) {
                Iterable<? extends Element> elems = t.analyze();
                System.err.println(count(elems) + " elements analyzed");
            }

            if (gk) {
                Iterable<? extends JavaFileObject> classfiles = t.generate();
                System.err.println(count(classfiles) + " class files generated");
            }
        } catch (IOException e) {
            error("unexpected exception caught: " + e);
        }

        File[] genFiles = tmpDir.listFiles();
        int expect = (gk ? 2 : 0); // main class and anon class for TaskListener
        if (genFiles.length != expect)
            error("unexpected number of files generated: " + genFiles.length
                    + ", expected: " + expect);

        System.err.println();
    }

    TaskListener createTaskListener() {
        return new TaskListener() {
            public void started(TaskEvent e) {
                System.err.println(e + " started");
            }

            public void finished(TaskEvent e) {
                System.err.println(e + " finished");
            }
        };
    }

    <T> int count(Iterable<T> items) {
        int count = 0;
        for (T item: items)
            count++;
        return count;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
