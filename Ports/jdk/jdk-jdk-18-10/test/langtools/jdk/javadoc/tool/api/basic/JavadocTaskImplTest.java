/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6493690
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build APITest
 * @run main JavadocTaskImplTest
 */

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import jdk.javadoc.internal.api.JavadocTaskImpl;
import jdk.javadoc.internal.tool.JavadocLog;

/**
 *  Misc tests for JavacTaskImpl.
 */
public class JavadocTaskImplTest extends APITest {
    public static void main(String... args) throws Exception {
        new JavadocTaskImplTest().run();
    }

    @Test
    public void testRawCall() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);

            @SuppressWarnings("rawtypes")
            Callable t = tool.getTask(null, fm, null, null, null, files);

            if (t.call() == Boolean.TRUE) {
                System.err.println("task succeeded");
            } else {
                throw new Exception("task failed");
            }
        }
    }

    @Test
    public void testDirectAccess1() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
        Context c = new Context();
        JavadocLog.preRegister(c, "javadoc");
        try (StandardJavaFileManager fm = new JavacFileManager(c, true, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            DocumentationTask t = new JavadocTaskImpl(c, null, null, files);
            if (t.call()) {
                System.err.println("task succeeded");
            } else {
                throw new Exception("task failed");
            }
        }
    }

    @Test
    public void testDirectAccess2() throws Exception {
        JavaFileObject srcFile = null; // error, provokes NPE
        Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
        Context c = new Context();
        JavadocLog.preRegister(c, "javadoc");
        try (StandardJavaFileManager fm = new JavacFileManager(c, true, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            try {
                DocumentationTask t = new JavadocTaskImpl(c, null, null, files);;
                error("getTask succeeded, no exception thrown");
            } catch (NullPointerException e) {
                System.err.println("exception caught as expected: " + e);
            }
        }
    }
}

