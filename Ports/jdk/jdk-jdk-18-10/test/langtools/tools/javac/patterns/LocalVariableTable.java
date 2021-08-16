/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231827
 * @summary Ensure the LV table entries are generated for bindings
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
 * method bodies containing patterns, and checks that the expected
 * set of entries is found in the attribute.
 *
 * The test looks for test cases represented by nested classes whose
 * name begins with "Pattern".  Each such class contains a method
 * with patterns, and because the test is compiled
 * with -g, these methods should have a LocalVariableTable.  The set of
 * expected names in the LVT is provided in an annotation on the class for
 * the test case.
 */
//Copied from: test/langtools/tools/javac/lambda/LocalVariableTable.java
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
            if (c.getSimpleName().startsWith("Pattern"))
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
        Method m = getMethodByName(cf, c.getSimpleName().contains("Lambda") ? "lambda$" : "test");
        if (m == null) {
            error("test method not found");
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

    Method getMethodByName(ClassFile cf, String name) throws ConstantPoolException {
        for (Method m: cf.methods) {
            if (m.getName(cf.constant_pool).startsWith(name))
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

    /*
     * ---------- Test cases ---------------------------------------------------
     */

    @Expect({ "o", "s" })
    static class Pattern_Simple {
        public static void test(Object o) {
            if (o instanceof String s) {
                s.length();
            }
        }
    }

    @Expect({ "s" })
    static class Pattern_Lambda {
        public static void test(Object o) {
            if (o instanceof String s) {
                Runnable r = () -> {
                    s.length();
                };
            }
        }
    }

}

