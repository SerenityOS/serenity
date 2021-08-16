/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.tools.*;

import static java.util.Collections.unmodifiableMap;

/**
 * class for storing source/byte code in memory.
 */
public class InMemoryFileManager extends ForwardingJavaFileManager {

    private final Map<String, InMemoryJavaFile> classes = new HashMap<>();

    public InMemoryFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {

        InMemoryJavaFile javaFile = new InMemoryJavaFile(className);
        classes.put(className, javaFile);
        return javaFile;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return new ClassLoader(this.getClass().getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                InMemoryJavaFile classData = classes.get(name);
                if (classData == null) throw new ClassNotFoundException(name);
                byte[] byteCode = classData.bos.toByteArray();
                return defineClass(name, byteCode, 0, byteCode.length);
            }
        };
    }

    public Map<String, ? extends JavaFileObject> getClasses() {
        return unmodifiableMap(classes);
    }

    private static class InMemoryJavaFile extends SimpleJavaFileObject {

        private final ByteArrayOutputStream bos =
                new ByteArrayOutputStream();


        protected InMemoryJavaFile(String name) {
            super(URI.create("mfm:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return bos;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(bos.toByteArray());
        }
    }
}
