/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8223942
 * @summary Missing methods in ClientCodeWrapper$WrappedJavaFileManager
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.lang.reflect.*;
import java.io.*;

import javax.tools.*;
import java.nio.charset.StandardCharsets;
import com.sun.tools.javac.api.JavacTaskImpl;

public class ClientCodeWrappersShouldOverrideAllMethodsTest {
    public static void main(String[] args) {
        ClientCodeWrappersShouldOverrideAllMethodsTest clientCodeWrappersShouldOverrideAllMethodsTest = new ClientCodeWrappersShouldOverrideAllMethodsTest();
        clientCodeWrappersShouldOverrideAllMethodsTest.testWrappersForJavaFileManager();
        clientCodeWrappersShouldOverrideAllMethodsTest.testWrappersForStandardJavaFileManager();
    }

    void testWrappersForJavaFileManager() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        UserFileManager fileManager = new UserFileManager(standardFileManager);
        JavacTaskImpl task = (JavacTaskImpl)compiler.getTask(null, fileManager, null, null, null, null);
        JavaFileManager wrappedFM = task.getContext().get(JavaFileManager.class);
        checkAllMethodsOverridenInWrapperClass(wrappedFM.getClass(), JavaFileManager.class);
    }

    void testWrappersForStandardJavaFileManager() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        UserStandardJavaFileManager fileManager = new UserStandardJavaFileManager(standardFileManager);
        JavacTaskImpl task = (JavacTaskImpl)compiler.getTask(null, fileManager, null, null, null, null);
        JavaFileManager wrappedFM = task.getContext().get(JavaFileManager.class);
        checkAllMethodsOverridenInWrapperClass(wrappedFM.getClass(), StandardJavaFileManager.class);
    }

    void checkAllMethodsOverridenInWrapperClass(Class<?> subClass, Class<?> superClass) {
        Method[] allMethods = subClass.getMethods();
        for (Method m : allMethods) {
            if (m.getDeclaringClass() == superClass) {
                throw new AssertionError(String.format("method %s not overriden by javac provided wrapper class", m.getName()));
            }
        }
    }

    static class UserFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        UserFileManager(JavaFileManager delegate) {
            super(delegate);
        }
    }

    static class UserStandardJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> implements StandardJavaFileManager {
        StandardJavaFileManager delegate;

        UserStandardJavaFileManager(StandardJavaFileManager delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        public Iterable<? extends File> getLocation(Location location) { return delegate.getLocation(location);}

        public void setLocation(Location location, Iterable<? extends File> files) throws IOException {
            delegate.setLocation(location, files);
        }

        public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
            return delegate.getJavaFileObjects(names);
        }

        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
            return delegate.getJavaFileObjectsFromStrings(names);
        }

        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
            return delegate.getJavaFileObjectsFromFiles(files);
        }

        public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
            return delegate.getJavaFileObjects(files);
        }
    }
}
