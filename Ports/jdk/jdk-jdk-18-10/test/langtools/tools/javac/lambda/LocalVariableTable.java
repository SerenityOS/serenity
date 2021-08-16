/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025998 8026749 8054220 8058227
 * @summary Missing LV table in lambda bodies
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g LocalVariableTable.java
 * @run main LocalVariableTable
 */

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import com.sun.tools.classfile.*;

/*
 * The test checks that a LocalVariableTable attribute is generated for the
 * method bodies representing lambda expressions, and checks that the expected
 * set of entries is found in the attribute.
 *
 * Since the bug was about missing entries in the LVT, not malformed entries,
 * the test is not intended to be a detailed test of the contents of each
 * LocalVariableTable entry: it is assumed that if a entry is present, it
 * will have the correct contents.
 *
 * The test looks for test cases represented by nested classes whose
 * name begins with "Lambda".  Each such class contains a lambda expression
 * that will mapped into a lambda method, and because the test is compiled
 * with -g, these methods should have a LocalVariableTable.  The set of
 * expected names in the LVT is provided in an annotation on the class for
 * the test case.
 */
public class LocalVariableTable {
    public static void main(String... args) throws Exception {
        new LocalVariableTable().run();
    }

    void run() throws Exception {
        // the declared classes are returned in an unspecified order,
        // so for neatness, sort them by name before processing them
        Class<?>[] classes = getClass().getDeclaredClasses();
        Arrays.sort(classes, (c1, c2) -> c1.getName().compareTo(c2.getName()));

        for (Class<?> c : classes) {
            if (c.getSimpleName().startsWith("Lambda"))
                check(c);
        }
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    /** Check an individual test case. */
    void check(Class<?> c) throws Exception {
        System.err.println("Checking " + c.getSimpleName());

        Expect expect = c.getAnnotation(Expect.class);
        if (expect == null) {
            error("@Expect not found for class " + c.getSimpleName());
            return;
        }

        ClassFile cf = ClassFile.read(getClass().getResource(c.getName() + ".class").openStream());
        Method m = getLambdaMethod(cf);
        if (m == null) {
            error("lambda method not found");
            return;
        }

        Code_attribute code = (Code_attribute) m.attributes.get(Attribute.Code);
        if (code == null) {
            error("Code attribute not found");
            return;
        }

        LocalVariableTable_attribute lvt =
                (LocalVariableTable_attribute) code.attributes.get(Attribute.LocalVariableTable);
        if (lvt == null) {
            error("LocalVariableTable attribute not found");
            return;
        }

        Set<String> foundNames = new LinkedHashSet<>();
        for (LocalVariableTable_attribute.Entry e: lvt.local_variable_table) {
            foundNames.add(cf.constant_pool.getUTF8Value(e.name_index));
        }

        Set<String> expectNames = new LinkedHashSet<>(Arrays.asList(expect.value()));
        if (!foundNames.equals(expectNames)) {
            Set<String> foundOnly = new LinkedHashSet<>(foundNames);
            foundOnly.removeAll(expectNames);
            for (String s: foundOnly)
                error("Unexpected name found: " + s);
            Set<String> expectOnly = new LinkedHashSet<>(expectNames);
            expectOnly.removeAll(foundNames);
            for (String s: expectOnly)
                error("Expected name not found: " + s);
        }
    }

    /** Get a method whose name begins "lambda$...". */
    Method getLambdaMethod(ClassFile cf) throws ConstantPoolException {
        for (Method m: cf.methods) {
            if (m.getName(cf.constant_pool).startsWith("lambda$"))
                return m;
        }
        return null;
    }

    /** Report an error. */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    /**
     * Annotation used to provide the set of names expected in the LVT attribute.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Expect {
        String[] value();
    }

    /** Functional interface with nullary method. */
    interface Run0 {
        public void run();
    }

    /** Functional interface with 1-ary method. */
    interface Run1 {
        public void run(int a0);
    }

    /** Functional interface with 2-ary method. */
    interface Run2 {
        public void run(int a0, int a1);
    }

    /*
     * ---------- Test cases ---------------------------------------------------
     */

    @Expect({ "x" })
    static class Lambda_Args0_Local1 {
        Run0 r = () -> { int x = 0; };
    }

    @Expect({ "x", "this" })
    static class Lambda_Args0_Local1_this {
        int v;
        Run0 r = () -> { int x = v; };
    }

    @Expect({ "a" })
    static class Lambda_Args1_Local0 {
        Run1 r = (a) -> { };
    }

    @Expect({ "a", "x" })
    static class Lambda_Args1_Local1 {
        Run1 r = (a) -> { int x = a; };
    }

    @Expect({ "a", "x", "v" })
    static class Lambda_Args1_Local1_Captured1 {
        void m() {
            int v = 0;
            Run1 r = (a) -> { int x = a + v; };
        }
    }

    @Expect({ "a1", "a2", "x1", "x2", "this", "v1", "v2" })
    static class Lambda_Args2_Local2_Captured2_this {
        int v;
        void m() {
            int v1 = 0;
            int v2 = 0;
            Run2 r = (a1, a2) -> {
                int x1 = a1 + v1 + v;
                int x2 = a2 + v2 + v;
            };
        }
    }

    @Expect({ "e", "c" })
    static class Lambda_Try_Catch {
        private static Runnable asUncheckedRunnable(Closeable c) {
            return () -> {
                try {
                    c.close();
                } catch (IOException e) {
                   throw new UncheckedIOException(e);
                }
            };
        }
    }
}

