/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6378728
 * @summary Verify -proc:only doesn't produce class files
 * @author  Joseph D. Darcy
 * @modules java.compiler
 *          jdk.compiler
 * @compile T6378728.java
 * @run main T6378728
 */

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Arrays;
import javax.tools.JavaCompiler.CompilationTask;

import javax.tools.*;


public class T6378728 {
    private static class ExceptionalFileManager extends ForwardingJavaFileManager {
        public ExceptionalFileManager(JavaFileManager wrapped) {
            super(wrapped);
        }

        @Override
        public FileObject getFileForOutput(Location location,
                                           String packageName,
                                           String relativeName,
                                           FileObject sibling)
        {
            throw new IllegalArgumentException("No files for you!");
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location,
                                                   String className,
                                                   JavaFileObject.Kind kind,
                                                   FileObject sibling)
        {
            throw new IllegalArgumentException("No files for you!");
        }
    }

    public static void main(String[] args) throws IOException {
        // Get a compiler tool
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String srcdir = System.getProperty("test.src");
        File source = new File(srcdir, "T6378728.java");
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {

            CompilationTask task =
                compiler.getTask(null,
                                 new ExceptionalFileManager(fm),
                                 null,
                                 Arrays.asList("-proc:only"),
                                 null,
                                 fm.getJavaFileObjectsFromFiles(Arrays.asList(source)));
            if (!task.call())
                throw new RuntimeException("Unexpected compilation failure");
        }
    }
}
