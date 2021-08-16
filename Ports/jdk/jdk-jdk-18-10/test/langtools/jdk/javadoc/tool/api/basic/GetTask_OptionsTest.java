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
 * @bug 6493690 8141492
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main GetTask_OptionsTest
 */

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Tests for DocumentationTool.getTask  options  parameter.
 */
public class GetTask_OptionsTest extends APITest {
    public static void main(String... args) throws Exception {
        new GetTask_OptionsTest().run();
    }

    /**
     * Verify that expected output files are written for given options.
     */
    @Test
    public void testNoIndex() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
            Iterable<String> options = Arrays.asList("-noindex");
            DocumentationTask t = tool.getTask(null, fm, null, null, options, files);
            if (t.call()) {
                System.err.println("task succeeded");
                Set<String> expectFiles = new TreeSet<String>(noIndexFiles);
                checkFiles(outDir, expectFiles);
            } else {
                error("task failed");
            }
        }
    }

    /**
     * Verify null is handled correctly.
     */
    @Test
    public void testNull() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<String> options = Arrays.asList((String) null);
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
            try {
                DocumentationTask t = tool.getTask(null, fm, null, null, options, files);
                error("getTask succeeded, no exception thrown");
            } catch (NullPointerException e) {
                System.err.println("exception caught as expected: " + e);
            }
        }
    }

}

