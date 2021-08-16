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
 * @bug 8006582
 * @summary javac should generate method parameters correctly.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build MethodParametersTester ClassFileVisitor ReflectionVisitor
 * @compile -parameters AnnotationTest.java
 * @run main MethodParametersTester AnnotationTest AnnotationTest.out
 */

import java.lang.annotation.*;

/** Test that annotations do not interfere with recording of parameter names */
class AnnotationTest {

    @Repeatable(Annos.class)
    @interface Anno {
        Class f() default int.class;
    }

    @interface Annos { Anno[] value(); String foo() default "hello"; }

    interface I {
        int m(@Anno @Anno int i, @Anno int ji);
    }

    public AnnotationTest(@Anno @Anno I i, @Anno int ji) { }
    public @Anno String foo(@Anno @Anno I i, int ji) { return null; }
}



