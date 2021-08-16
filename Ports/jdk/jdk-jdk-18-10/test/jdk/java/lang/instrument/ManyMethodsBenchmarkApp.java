/*
 * Copyright 2015 Google Inc.  All Rights Reserved.
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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * A manual benchmark of the JVMTI RedefineClasses when a
 * single class (and its parent) contains many methods.
 */
public class ManyMethodsBenchmarkApp {
     // Limit is 64k but we can not put such many as the CP limit is 32k.
     // In practice, it means a real limit is much lower (less than 22000).
    static final int METHOD_COUNT = 20000;

    static Class<?> loadClassInNewClassLoader(String className) throws Exception {
        URL[] urls = { new File(".").toURI().toURL() };
        URLClassLoader ucl = new URLClassLoader(urls, null);
        Class<?> klazz = Class.forName(className, true, ucl);
        return klazz;
    }

    static void benchmarkClassOperations(String className) throws Exception {
        Class<?> klazz = loadClassInNewClassLoader(className);

        Method[] methods = klazz.getDeclaredMethods();
        if (methods.length != METHOD_COUNT) {
            throw new AssertionError("unexpected method count: " + methods.length +
                                     " expected: " + METHOD_COUNT);
        }

        methods = klazz.getMethods();
        // returned methods includes those inherited from Object
        int objectMethodSlop = 100;
        if (methods.length <= METHOD_COUNT ||
            methods.length >= METHOD_COUNT + objectMethodSlop) {
            throw new AssertionError("unexpected method count: " + methods.length);
        }

        // Invoke methods to make them appear in the constant pool cache
        Object obj = klazz.newInstance();
        Object[] args = new Object[0];
        for (Method m: methods) {
            try {
                Class<?>[] types = m.getParameterTypes();
                String     name  = m.getName();
             // System.out.println("method: " + name + "; argno: " + types.length);
                if (types.length == 0 && name.length() == 2 && name.startsWith("f")) {
                    m.invoke(obj, args);
                }
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("test started: ManyMethodsBenchmarkApp");

        // Create source files with many methods
        File base = new File("Base.java");
        try (FileWriter fw = new FileWriter(base)) {
            fw.write("public class Base {\n");
            final int L = 10;
            // Each of the first L methods makes calls to its own chunk of METHOD_COUNT/L methods
            for (int k = 0; k < L; k++) {
                fw.write("    public void f" + k + "() {\n");
                int shift = (k == 0) ? L : 0;
                for (int i = (k * (METHOD_COUNT/L)) + shift; i < (k + 1) * METHOD_COUNT/L; i++) {
                    fw.write("        f" + i + "();\n");
                }
                fw.write("    }\n");
            }

            // The rest of (METHOD_COUNT - L) methods have empty body
            for (int i = L; i < METHOD_COUNT; i++) {
                fw.write("    public static void f" + i + "() {}\n");
            }
            fw.write("}\n");
        }

        // Compile the generated source files.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<File> files = Arrays.asList(new File[] { base });
        try (StandardJavaFileManager fileManager =
             compiler.getStandardFileManager(null, null, null)) {
            compiler.getTask(null, fileManager, null, null, null,
                fileManager.getJavaFileObjectsFromFiles(files))
                .call();
        }

        benchmarkClassOperations("Base");

        ManyMethodsBenchmarkAgent.instr();

        // Cleanup
        base.delete();
        new File("Base.class").delete();
        if (!ManyMethodsBenchmarkAgent.completed) {
            throw new Exception("ERROR: ManyMethodsBenchmarkAgent did not complete.");
        }

        if (ManyMethodsBenchmarkAgent.fail) {
            throw new Exception("ERROR: ManyMethodsBenchmarkAgent failed.");
        } else {
            System.out.println("ManyMethodsBenchmarkAgent succeeded.");
        }
        System.out.println("test finished: ManyMethodsBenchmarkApp");
    }
}
