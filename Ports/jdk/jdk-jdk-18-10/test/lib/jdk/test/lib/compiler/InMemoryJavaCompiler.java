/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * {@code InMemoryJavaCompiler} can be used for compiling a {@link
 * CharSequence} to a {@code byte[]}.
 *
 * The compiler will not use the file system at all, instead using a {@link
 * ByteArrayOutputStream} for storing the byte code. For the source code, any
 * kind of {@link CharSequence} can be used, e.g. {@link String}, {@link
 * StringBuffer} or {@link StringBuilder}.
 *
 * The {@code InMemoryCompiler} can easily be used together with a {@code
 * ByteClassLoader} to easily compile and load source code in a {@link String}:
 *
 * <pre>
 * {@code
 * import jdk.test.lib.compiler.InMemoryJavaCompiler;
 * import jdk.test.lib.ByteClassLoader;
 *
 * class Example {
 *     public static void main(String[] args) {
 *         String className = "Foo";
 *         String sourceCode = "public class " + className + " {" +
 *                             "    public void bar() {" +
 *                             "        System.out.println("Hello from bar!");" +
 *                             "    }" +
 *                             "}";
 *         byte[] byteCode = InMemoryJavaCompiler.compile(className, sourceCode);
 *         Class fooClass = ByteClassLoader.load(className, byteCode);
 *     }
 * }
 * }
 * </pre>
 */
public class InMemoryJavaCompiler {
    private static class MemoryJavaFileObject extends SimpleJavaFileObject {
        private final String className;
        private final CharSequence sourceCode;
        private final ByteArrayOutputStream byteCode;

        public MemoryJavaFileObject(String className, CharSequence sourceCode) {
            super(URI.create("string:///" + className.replace('.','/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.className = className;
            this.sourceCode = sourceCode;
            this.byteCode = new ByteArrayOutputStream();
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return byteCode;
        }

        public byte[] getByteCode() {
            return byteCode.toByteArray();
        }

        public String getClassName() {
            return className;
        }
    }

    private static class FileManagerWrapper extends ForwardingJavaFileManager {
        private static final Location PATCH_LOCATION = new Location() {
            @Override
            public String getName() {
                return "patch module location";
            }

            @Override
            public boolean isOutputLocation() {
                return false;
            }
        };
        private final MemoryJavaFileObject file;
        private final String moduleOverride;

        public FileManagerWrapper(MemoryJavaFileObject file, String moduleOverride) {
            super(getCompiler().getStandardFileManager(null, null, null));
            this.file = file;
            this.moduleOverride = moduleOverride;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   Kind kind, FileObject sibling)
            throws IOException {
            if (!file.getClassName().equals(className)) {
                throw new IOException("Expected class with name " + file.getClassName() +
                                      ", but got " + className);
            }
            return file;
        }

        @Override
        public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
            if (fo == file && moduleOverride != null) {
                return PATCH_LOCATION;
            }
            return super.getLocationForModule(location, fo);
        }

        @Override
        public String inferModuleName(Location location) throws IOException {
            if (location == PATCH_LOCATION) {
                return moduleOverride;
            }
            return super.inferModuleName(location);
        }

        @Override
        public boolean hasLocation(Location location) {
            return super.hasLocation(location) || location == StandardLocation.PATCH_MODULE_PATH;
        }

    }

    /**
     * Compiles the class with the given name and source code.
     *
     * @param className The name of the class
     * @param sourceCode The source code for the class with name {@code className}
     * @param options additional command line options
     * @throws RuntimeException if the compilation did not succeed
     * @return The resulting byte code from the compilation
     */
    public static byte[] compile(String className, CharSequence sourceCode, String... options) {
        MemoryJavaFileObject file = new MemoryJavaFileObject(className, sourceCode);
        CompilationTask task = getCompilationTask(file, options);

        if(!task.call()) {
            throw new RuntimeException("Could not compile " + className + " with source code " + sourceCode);
        }

        return file.getByteCode();
    }

    private static JavaCompiler getCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    private static CompilationTask getCompilationTask(MemoryJavaFileObject file, String... options) {
        List<String> opts = new ArrayList<>();
        String moduleOverride = null;
        for (String opt : options) {
            if (opt.startsWith("--patch-module=")) {
                moduleOverride = opt.substring("--patch-module=".length());
            } else {
                opts.add(opt);
            }
        }
        return getCompiler().getTask(null, new FileManagerWrapper(file, moduleOverride), null, opts, null, Arrays.asList(file));
    }
}
