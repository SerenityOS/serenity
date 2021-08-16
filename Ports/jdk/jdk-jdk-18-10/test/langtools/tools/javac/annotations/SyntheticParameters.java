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
 * @bug 8065132
 * @summary Test generation of annotations on inner class parameters.
 * @library /lib/annotations/
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build annotations.classfile.ClassfileInspector SyntheticParameters
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
                     null,
                     new ExpectedParameterAnnotation[] {
                         (ExpectedParameterAnnotation)
                         // Assert there is an annotation on the
                         // first parameter.
                         new ExpectedParameterAnnotation(
                             "<init>",
                             0,
                             "A",
                             true,
                             1),
                         (ExpectedParameterAnnotation)
                         new ExpectedParameterAnnotation(
                             "foo",
                             0,
                             "A",
                             true,
                             1),
                         (ExpectedParameterAnnotation)
                         // Assert there is an annotation on the
                         // first parameter.
                         new ExpectedParameterAnnotation(
                             "<init>",
                             0,
                             "B",
                             false,
                             1),
                         (ExpectedParameterAnnotation)
                         new ExpectedParameterAnnotation(
                             "foo",
                             0,
                             "B",
                             false,
                             1),
                         (ExpectedParameterAnnotation)
                         new ExpectedParameterAnnotation(
                             "foo",
                             1,
                             "B",
                             false,
                             0)
                     },
                     null);
    private static final Expected Foo_expected =
        new Expected("SyntheticParameters$Foo",
                     null,
                     null,
                     new ExpectedParameterAnnotation[] {
                         (ExpectedParameterAnnotation)
                         // Assert there is an annotation on the
                         // first parameter.
                         new ExpectedParameterAnnotation(
                             "<init>",
                             0,
                             "A",
                             true,
                             1),
                         (ExpectedParameterAnnotation)
                         // Assert there is an annotation on the
                         // first parameter.
                         new ExpectedParameterAnnotation(
                             "<init>",
                             0,
                             "B",
                             false,
                             1)
                     },
                     null);

    public static void main(String... args) throws Exception {
        new SyntheticParameters().run(
            new ClassFile[] { getClassFile(Inner_class, Inner.class),
                              getClassFile(Foo_class, Foo.class) },
            new Expected[] { Inner_expected, Foo_expected });
    }

    public class Inner {
        public Inner(@A @B int a) {}
        public void foo(@A @B int a, int b) {}
    }

    public static enum Foo {
        ONE(null);
        Foo(@A @B Object a) {}
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface A {}

@Retention(RetentionPolicy.CLASS)
@interface B {}
