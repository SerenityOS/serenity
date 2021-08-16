/*
 * Copyright (c) 2017, Google Inc. All rights reserved.
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
 * @bug 8144185
 * @summary javac produces incorrect RuntimeInvisibleTypeAnnotations length attribute
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeVisibleTypeAnnotations_attribute;
import com.sun.tools.classfile.TypeAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;

public class TypeAnnotationPropagationTest extends ClassfileTestHelper {
    public static void main(String[] args) throws Exception {
        new TypeAnnotationPropagationTest().run();
    }

    public void run() throws Exception {
        ClassFile cf = getClassFile("TypeAnnotationPropagationTest$Test.class");

        Method f = null;
        for (Method m : cf.methods) {
            if (m.getName(cf.constant_pool).contains("f")) {
                f = m;
                break;
            }
        }

        int idx = f.attributes.getIndex(cf.constant_pool, Attribute.Code);
        Code_attribute cattr = (Code_attribute) f.attributes.get(idx);
        idx = cattr.attributes.getIndex(cf.constant_pool, Attribute.RuntimeVisibleTypeAnnotations);
        RuntimeVisibleTypeAnnotations_attribute attr =
                (RuntimeVisibleTypeAnnotations_attribute) cattr.attributes.get(idx);

        TypeAnnotation anno = attr.annotations[0];
        assertEquals(anno.position.lvarOffset, new int[] {3}, "start_pc");
        assertEquals(anno.position.lvarLength, new int[] {8}, "length");
        assertEquals(anno.position.lvarIndex, new int[] {1}, "index");
    }

    void assertEquals(int[] actual, int[] expected, String message) {
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(
                    String.format(
                            "actual: %s, expected: %s, %s",
                            Arrays.toString(actual), Arrays.toString(expected), message));
        }
    }

    /** ********************* Test class ************************ */
    static class Test {
        void f() {
            @A String s = "";
            Runnable r =
                    () -> {
                        Objects.requireNonNull(s);
                        Objects.requireNonNull(s);
                        Objects.requireNonNull(s);
                        Objects.requireNonNull(s);
                        Objects.requireNonNull(s);
                        Objects.requireNonNull(s);
                    };
        }

        @Retention(RUNTIME)
        @Target(TYPE_USE)
        @interface A {}
    }
}

