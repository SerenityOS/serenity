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
 * @bug     8004822 8163113
 * @author  mnunez
 * @summary Language model api test basics for repeating annotations
 * @library /tools/javac/lib
 * @library supportingAnnotations
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor ElementRepAnnoTester
 * @compile -processor ElementRepAnnoTester -proc:only MixSingularAndUnofficialContainerBasicTest.java
 */

@ExpectedBase(
        value = Foo.class,
        getAnnotation = "@Foo(0)",
        getAnnotationsByType = {"@Foo(0)"},
        getAllAnnotationMirrors = {
            "@Foo(0)",
            "@UnofficialContainer({@Foo(1), @Foo(2)})",
            "ExpectedBase",
            "ExpectedContainer"
        },
        getAnnotationMirrors = {
            "@Foo(0)",
            "@UnofficialContainer({@Foo(1), @Foo(2)})",
            "ExpectedBase",
            "ExpectedContainer"
        })
@ExpectedContainer(
        value = UnofficialContainer.class,
        getAnnotation = "@UnofficialContainer("
        + "{@Foo(1), @Foo(2)})",
        getAnnotationsByType = {"@UnofficialContainer("
                + "{@Foo(1), @Foo(2)})"})
@Foo(0)
@UnofficialContainer({@Foo(1), @Foo(2)})
class MixSingularAndUnofficialContainerBasicTest {

    @ExpectedBase(
            value = Foo.class,
            getAnnotation = "@Foo(0)",
            getAnnotationsByType = {"@Foo(0)"},
            getAllAnnotationMirrors = {
                "@Foo(0)",
                "@UnofficialContainer({@Foo(1), @Foo(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            },
            getAnnotationMirrors = {
                "@Foo(0)",
                "@UnofficialContainer({@Foo(1), @Foo(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            })
    @ExpectedContainer(
            value = UnofficialContainer.class,
            getAnnotation = "@UnofficialContainer("
            + "{@Foo(1), @Foo(2)})",
            getAnnotationsByType = {"@UnofficialContainer("
                    + "{@Foo(1), @Foo(2)})"})
    @Foo(0)
    @UnofficialContainer({@Foo(1), @Foo(2)})
    int testField = 0;

    @ExpectedBase(
            value = Foo.class,
            getAnnotation = "@Foo(0)",
            getAnnotationsByType = {"@Foo(0)"},
            getAllAnnotationMirrors = {
                "@Foo(0)",
                "@UnofficialContainer({@Foo(1), @Foo(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            },
            getAnnotationMirrors = {
                "@Foo(0)",
                "@UnofficialContainer({@Foo(1), @Foo(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            })
    @ExpectedContainer(
            value = UnofficialContainer.class,
            getAnnotation = "@UnofficialContainer("
            + "{@Foo(1), @Foo(2)})",
            getAnnotationsByType = {"@UnofficialContainer("
                    + "{@Foo(1), @Foo(2)})"})
    @Foo(0)
    @UnofficialContainer({@Foo(1), @Foo(2)})
    void testMethod() {}
}
