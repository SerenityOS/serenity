/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test SyntheticParameters
 * @summary Test generation of annotations on inner class parameters.
 * @library /lib/annotations/
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main SyntheticParameters
 */

import annotations.classfile.ClassfileInspector;

import java.io.*;
import java.lang.annotation.*;

import com.sun.tools.classfile.*;

public class SyntheticParameters extends ClassfileInspector {

    private static final String Inner_class = "SyntheticParameters$Inner.class";
    private static final String Foo_class = "SyntheticParameters$Foo.class";
    private static final Expected Inner_expected =
        new Expected("SyntheticParameters$Inner",
                     null,
                     new ExpectedMethodTypeAnnotation[] {
                         (ExpectedMethodTypeAnnotation)
                         // Assert there is an annotation on the
                         // first parameter.
                         new ExpectedMethodTypeAnnotation.Builder(
                             "<init>",
                             "A",
                             TypeAnnotation.TargetType.METHOD_FORMAL_PARAMETER,
                             false,
                             1).setParameterIndex(0).build()
                     },
                     null);
    private static final Expected Foo_expected =
        new Expected("SyntheticParameters$Foo",
                     null,
                     new ExpectedMethodTypeAnnotation[] {
                         (ExpectedMethodTypeAnnotation)
                         // Assert there is no annotation on the
                         // $enum$name parameter.
                         new ExpectedMethodTypeAnnotation.Builder(
                             "<init>",
                             "A",
                             TypeAnnotation.TargetType.METHOD_FORMAL_PARAMETER,
                             false,
                             1).setParameterIndex(0).build()                     },
                     null);

    public static void main(String... args) throws Exception {
        new SyntheticParameters().run(
            new ClassFile[] { getClassFile(Inner_class, Inner.class),
                              getClassFile(Foo_class, Foo.class) },
            new Expected[] { Inner_expected, Foo_expected });
    }

    public class Inner {
        public Inner(@A int a) {}
        public void foo(@A int a, int b) {}
    }

    public static enum Foo {
        ONE(null);
        Foo(@A Object a) {}
    }
}

@Target({ElementType.TYPE_USE})
@interface A {}
