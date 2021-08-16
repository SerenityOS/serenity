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

/*
 * @test
 * @bug 6493690
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main GetTask_FileObjectsTest
 */

import java.io.File;
import java.util.Arrays;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Tests for DocumentationTool.getTask  fileObjects  parameter.
 */
public class GetTask_FileObjectsTest extends APITest {
    public static void main(String... args) throws Exception {
        new GetTask_FileObjectsTest().run();
    }

    /**
     * Verify that expected output files are written via the file manager,
     * for a source file read from the file system with StandardJavaFileManager.
     */
    @Test
    public void testStandardFileObject() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File srcFile = new File(testSrc, "pkg/C.java");
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(srcFile);
            DocumentationTask t = tool.getTask(null, fm, null, null, null, files);
            if (t.call()) {
                System.err.println("task succeeded");
                checkFiles(outDir, standardExpectFiles);
            } else {
                throw new Exception("task failed");
            }
        }
    }

    /**
     * Verify that expected output files are written via the file manager,
     * for an in-memory file object.
     */
    @Test
    public void testMemoryFileObject() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
            DocumentationTask t = tool.getTask(null, fm, null, null, null, files);
            if (t.call()) {
                System.err.println("task succeeded");
                checkFiles(outDir, standardExpectFiles);
            } else {
                throw new Exception("task failed");
            }
        }
    }

    /**
     * Verify bad file object is handled correctly.
     */
    @Test
    public void testBadFileObject() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File srcFile = new File(testSrc, "pkg/C.class");  // unacceptable file kind
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(srcFile);
            try {
                DocumentationTask t = tool.getTask(null, fm, null, null, null, files);
                error("getTask succeeded, no exception thrown");
            } catch (IllegalArgumentException e) {
                System.err.println("exception caught as expected: " + e);
            }
        }
    }

    /**
     * Verify null is handled correctly.
     */
    @Test
    public void testNull() throws Exception {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList((JavaFileObject) null);
            try {
                DocumentationTask t = tool.getTask(null, fm, null, null, null, files);
                error("getTask succeeded, no exception thrown");
            } catch (NullPointerException e) {
                System.err.println("exception caught as expected: " + e);
            }
        }
    }

}

