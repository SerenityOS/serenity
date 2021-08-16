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
 * @bug 5053846 8011432
 * @summary javac: MethodRef entries are duplicated in the constant pool
 * @summary javac, compiler regression iterable + captured type
 * @modules jdk.compiler
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.*;

public class MethodRefDupInConstantPoolTest {

    private static final String methodToLookFor =
            "java/util/Vector.iterator:()Ljava/util/Iterator;";

    public static void main(String[] args) {
        new MethodRefDupInConstantPoolTest().run();
    }

    void run() {
        check("-v", Paths.get(System.getProperty("test.classes"),
                this.getClass().getSimpleName() + "$TestHelper1.class").toString());
        check("-v", Paths.get(System.getProperty("test.classes"),
                this.getClass().getSimpleName() + "$TestHelper2.class").toString());
        check("-v", Paths.get(System.getProperty("test.classes"),
                this.getClass().getSimpleName() + "$TestHelper3.class").toString());
        check("-v", Paths.get(System.getProperty("test.classes"),
                this.getClass().getSimpleName() + "$TestHelper4.class").toString());
    }

    void check(String... params) {
        StringWriter s;
        String out;
        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            com.sun.tools.javap.Main.run(params, pw);
            out = s.toString();
        }
        String constantPool = getConstantPool(out);
        if (constantPool.indexOf(methodToLookFor) !=
                constantPool.lastIndexOf(methodToLookFor)) {
            throw new AssertionError("There is more than one entry for the method seek "  +
                    methodToLookFor);
        }
    }

    String getConstantPool(String out) {
        int start = out.indexOf("Constant pool:");
        int end = out.indexOf("{");
        return out.substring(start, end);
    }

    class TestHelper1 {
        void m() {
            Vector v = new Vector();
            Iterator iter = v.iterator();
            while (iter.hasNext()) {
                Object o = iter.next();
                Object o2 = o;
            }
            for (Object o: v) {
                Object o2 = o;
            }
        }
    }

    class TestHelper2<X extends Number & Iterable<String>> {
        void test(X x) {
            for (String s : x) { }
        }
    }

    interface Data extends Iterable<String> {}

    class TestHelper3<X extends Number & Iterable<? extends Data>> {
        void test(X x) {
            for (Data s : x) { }
        }
    }

    class TestHelper4 {
         void test(Iterable<? extends Data> t) {
             for(Object a: t.iterator().next());
         }
    }
}
