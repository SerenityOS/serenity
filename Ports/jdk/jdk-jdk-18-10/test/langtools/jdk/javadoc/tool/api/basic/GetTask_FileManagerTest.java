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
 * @bug 6493690 8024434
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build APITest
 * @run main GetTask_FileManagerTest
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;

/**
 * Tests for DocumentationTool.getTask  fileManager  parameter.
 */
public class GetTask_FileManagerTest extends APITest {
    public static void main(String... args) throws Exception {
        new GetTask_FileManagerTest().run();
    }

    /**
     * Verify that an alternate file manager can be specified:
     * in this case, a TestFileManager.
     */
    @Test
    public void testFileManager() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        StandardJavaFileManager fm = new TestFileManager();
        File outDir = getOutDir();
        fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
        Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
        DocumentationTask t = tool.getTask(null, fm, null, null, Arrays.asList("-verbose"), files);
        if (t.call()) {
            System.err.println("task succeeded");
            checkFiles(outDir, standardExpectFiles);
        } else {
            throw new Exception("task failed");
        }
    }

    /**
     * Verify that exceptions from a bad file manager are thrown as expected.
     */
    @Test
    public void testBadFileManager() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        StandardJavaFileManager fm = new TestFileManager() {
            @Override
            public Iterable<JavaFileObject> list(Location location,
                    String packageName,
                    Set<Kind> kinds,
                    boolean recurse)
                    throws IOException {
                throw new UnexpectedError();
            }
        };
        fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(getOutDir()));
        Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
        DocumentationTask t = tool.getTask(null, fm, null, null, null, files);
        try {
            t.call();
            error("call completed without exception");
        } catch (RuntimeException e) {
            Throwable c = e.getCause();
            if (c.getClass() == UnexpectedError.class)
                System.err.println("exception caught as expected: " + c);
            else
                throw e;
        }
    }

    public static class UnexpectedError extends Error { }

    /*
     * A JavaFileManager which is not a JavacFileManager, even though it uses one internally for
     * convenience.
     */
    static class TestFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
            implements StandardJavaFileManager  {
        TestFileManager() {
            super(new JavacFileManager(new Context(), false, null));
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
            return fileManager.getJavaFileObjectsFromFiles(files);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
            return fileManager.getJavaFileObjects(files);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
            return fileManager.getJavaFileObjectsFromStrings(names);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
            return fileManager.getJavaFileObjects(names);
        }

        @Override
        public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
            fileManager.setLocation(location, path);
        }

        @Override
        public Iterable<? extends File> getLocation(Location location) {
            return fileManager.getLocation(location);
        }

    }
}
