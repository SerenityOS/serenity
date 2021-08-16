/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
 * @bug 8007720 8177486
 * @summary class reading of named parameters
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @compile -parameters ClassReaderTest.java MethodParameterProcessor.java
 * @compile/process/ref=ClassReaderTest.out -proc:only -processor MethodParameterProcessor ClassReaderTest ClassReaderTest$I ClassReaderTest$E
 */

import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

public class ClassReaderTest {

    @Retention(RUNTIME)
    @interface RuntimeAnnoOne {
        int value() default 0;
    }

    @Retention(RUNTIME)
    @interface RuntimeAnnoTwo {
        int value() default 0;
    }

    @Retention(CLASS)
    @interface ClassAnno {
        int value() default 0;
    }

    @MethodParameterProcessor.ParameterNames
    void f(
            @RuntimeAnnoOne(1) @RuntimeAnnoTwo(2) @ClassAnno(3) int a,
            @RuntimeAnnoOne(4) @RuntimeAnnoTwo(5) @ClassAnno(6) String b) {}

    class I {
        @MethodParameterProcessor.ParameterNames
        I(@ClassAnno(7) int d, @RuntimeAnnoOne(8) String e, Object o) {}
    }

    enum E {
        ONE(42, "");

        @MethodParameterProcessor.ParameterNames
        E(int x, @RuntimeAnnoOne(9) String s) {}
    }
}
