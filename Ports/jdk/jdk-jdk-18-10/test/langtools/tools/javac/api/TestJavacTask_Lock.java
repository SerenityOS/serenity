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
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import com.sun.source.util.*;

public class TestJavacTask_Lock {
    public static void main(String... args) throws Exception {
        new TestJavacTask_Lock().run();
    }

    enum MethodKind {
        CALL {
            int test(CompilationTask t) {
                boolean ok = t.call();
                if (!ok)
                    throw new Error("compilation failed");
                return 1;
            }
        },
        PARSE {
            int test(CompilationTask t) {
                try {
                    ((JavacTask) t).parse();
                    return 1;
                } catch (IOException ex) {
                    throw new Error(ex);
                }
            }

        };
        abstract int test(CompilationTask t);
    }

    JavaCompiler comp;
    StandardJavaFileManager fm;

    void run() throws Exception {
        comp = ToolProvider.getSystemJavaCompiler();
        fm = comp.getStandardFileManager(null, null, null);
        try {
            for (MethodKind first: MethodKind.values()) {
                for (MethodKind second: MethodKind.values()) {
                    test(first, second);
                }
            }

            if (errors > 0)
                throw new Exception(errors + " errors found");
        } finally {
            fm.close();
        }
    }

    void test(MethodKind first, MethodKind second) {
        System.err.println("test: " + first + ", " + second);
        File testSrc = new File(System.getProperty("test.src"));
        String thisClassName = TestJavacTask_Lock.class.getName();
        Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjects(new File(testSrc, thisClassName + ".java"));
        File tmpDir = new File(first + "_" + second);
        tmpDir.mkdirs();
        List<String> options = Arrays.asList( "-d", tmpDir.getPath() );
        CompilationTask t = comp.getTask(null, fm, null, options, null, files);

        try {
            first.test(t);
            second.test(t);
            error("No exception thrown");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            System.err.println("Expected exception caught: " + e);
        } catch (Exception e) {
            error("Unexpected exception caught: " + e);
            e.printStackTrace(System.err);
        }

    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
