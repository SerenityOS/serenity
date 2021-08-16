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
 * @run main Task_reuseTest
 */

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Tests for reusing a documentation task.
 */
public class Task_reuseTest extends APITest {
    public static void main(String... args) throws Exception {
        new Task_reuseTest().run();
    }

    /**
     * Verify that call can only be called once.
     */
    @Test
    public void testReuse() throws Exception {
        DocumentationTask t = getAndRunTask();
        try {
            t.call();
            error("task was reused without exception");
        } catch (IllegalStateException e) {
            System.err.println("caught exception " + e);
        }
    }

    /**
     * Verify that cannot update task after call
     */
    @Test
    public void testUpdateSetLocale() throws Exception {
        DocumentationTask t = getAndRunTask();
        try {
            t.setLocale(Locale.getDefault());
            error("task was reused without exception");
        } catch (IllegalStateException e) {
            System.err.println("caught exception " + e);
        }
    }

    private DocumentationTask getAndRunTask() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
            DocumentationTask t = tool.getTask(null, fm, null, null, null, files);
            if (t.call()) {
                System.err.println("task succeeded");
                return t;
            } else {
                throw new Exception("task failed");
            }
        }
    }
}

