/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023668
 * @summary Desugar serializable lambda bodies using more robust naming scheme
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TestSerializedLambdaNameStability
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class TestSerializedLambdaNameStability {

    final ClassLoader writingClassLoader;
    final ClassLoader clonedClassLoader;
    final ClassLoader checkingClassLoader;

    TestSerializedLambdaNameStability()  {
        writingClassLoader = new TestClassLoader("before");
        clonedClassLoader = new TestClassLoader("before");
        checkingClassLoader = new TestClassLoader("after");
    }

    public static void main(String... args) throws Exception {
        new TestSerializedLambdaNameStability().doit("NameOfCapturedArgs", true);
        new TestSerializedLambdaNameStability().doit("TypesOfCapturedArgs", true);
        new TestSerializedLambdaNameStability().doit("OrderOfCapturedArgs", true);
        new TestSerializedLambdaNameStability().doit("VariableAssignmentTarget", false);
        new TestSerializedLambdaNameStability().doit("TargetName", true);
        new TestSerializedLambdaNameStability().doit("TargetType", true);
    }

    public void doit(String name, boolean expectFail) throws Exception {
        String iName = "I" + name;
        String testName = "TEST" + name;
        Class<?> kw = writingClassLoader.loadClass(testName);
        Object instw = getInstance(kw);
        Method mw = getMethod(kw, "write", ObjectOutput.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutput out = new ObjectOutputStream(baos)) {
            mw.invoke(instw, out);
        }
        byte[] ser = baos.toByteArray();

        // Read and check clone
        readCheck(iName, testName, clonedClassLoader, ser);
        System.err.printf("cloned test readCheck %s\n", testName);

        // Read and check other
        if (expectFail) {
            try {
                readCheck(iName, testName, checkingClassLoader, ser);
            } catch (InvocationTargetException ite) {
                Throwable underlying = ite;
                while (underlying != null && !(underlying instanceof IllegalArgumentException)) {
                    underlying = underlying.getCause();
                }
                if (underlying != null) {
                    if (underlying.getMessage().contains("deserialization")) {
                        System.err.printf("PASS: other test %s got expected exception %s\n", testName, underlying);
                        return;
                    }
                }
                System.err.printf("FAIL: other test %s got unexpected exception %s\n", testName, ite);
                throw new Exception("unexpected exception ", ite);
            }
            System.err.printf("FAIL: other test %s expected an exception", testName);
            throw new Exception("expected an exception" + testName);
        } else {
            readCheck(iName, testName, checkingClassLoader, ser);
            System.err.printf("PASS: other test %s readCheck\n", testName);
        }
    }

    void readCheck(String iName, String testName, ClassLoader loader, byte[] ser) throws Exception {
        Class<?> k = loader.loadClass(testName);
        Object inst = getInstance(k);
        Method mrc = getMethod(k, "readCheck", ObjectInput.class);
        ByteArrayInputStream bais = new ByteArrayInputStream(ser);
        try (ObjectInput in = new ObjectInputStream(bais)) {
            mrc.invoke(inst, in);
        }
    }

    Method getMethod(Class<?> k, String name, Class<?> argTypes) throws Exception {
        Method meth = k.getDeclaredMethod(name, argTypes);
        meth.setAccessible(true);
        return meth;
    }

    Object getInstance(Class<?> k) throws Exception {
        Constructor<?> cons = k.getConstructors()[0];
        cons.setAccessible(true);
        return cons.newInstance();
    }

    static class TestClassLoader extends ClassLoader  {
        static final String compiledDir = System.getProperty("user.dir");
        static final String sourceBaseDir = System.getProperty("test.src");

        final ToolBox tb = new ToolBox();
        final String context;

        public TestClassLoader(String context) {
            super();
            this.context = context;
        }

        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            byte[] b;

            try {
                b = loadClassData(name);
            } catch (Throwable th) {
                // th.printStackTrace();
                throw new ClassNotFoundException("Loading error", th);
            }
            return defineClass(name, b, 0, b.length);
        }

        private byte[] loadClassData(String name) throws Exception {
            String srcName;
            if (name.startsWith("TEST"))
                srcName = name;
            else if (name.startsWith("I"))
                srcName = "TEST" + name.substring(1);
            else
                throw new Exception("Did not expect to load " + name);
            Path srcFile = Paths.get(sourceBaseDir, context, srcName + ".java");
            new JavacTask(tb)
                    .outdir(compiledDir)
                    .files(srcFile)
                    .run();
            Path cfFile = Paths.get(compiledDir, name + ".class");
            byte[] bytes = Files.readAllBytes(cfFile);
            return bytes;
        }
    }
}
