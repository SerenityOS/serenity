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
 * @compile -processor ElementRepAnnoTester -proc:only
 * MixRepeatableAndOfficialContainerBasicTest.java
 */

@ExpectedBase(
        value = Bar.class,
        getAnnotation = "@Bar(0)",
        getAnnotationsByType = {
            "@Bar(0)",
            "@Bar(1)",
            "@Bar(2)"
        },
        getAllAnnotationMirrors = {
            "@Bar(0)",
            "@BarContainer({@Bar(1), @Bar(2)})",
            "ExpectedBase",
            "ExpectedContainer"
        },
        getAnnotationMirrors = {
            "@Bar(0)",
            "@BarContainer({@Bar(1), @Bar(2)})",
            "ExpectedBase",
            "ExpectedContainer"
        })
@ExpectedContainer(
        value = BarContainer.class,
        getAnnotation = "@BarContainer({@Bar(1), @Bar(2)})",
        getAnnotationsByType = {"@BarContainer({@Bar(1), @Bar(2)})"})
@Bar(value = 0)
@BarContainer(value = {@Bar(value = 1), @Bar(value = 2)})
class MixRepeatableAndOfficialContainerBasicTest {

    @ExpectedBase(
            value = Bar.class,
            getAnnotation = "@Bar(0)",
            getAnnotationsByType = {
                "@Bar(0)",
                "@Bar(1)",
                "@Bar(2)"
            },
            getAllAnnotationMirrors = {
                "@Bar(0)",
                "@BarContainer({@Bar(1), @Bar(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            },
            getAnnotationMirrors = {
                "@Bar(0)",
                "@BarContainer({@Bar(1), @Bar(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            })
    @ExpectedContainer(
            value = BarContainer.class,
            getAnnotation = "@BarContainer({@Bar(1), @Bar(2)})",
            getAnnotationsByType = {"@BarContainer({@Bar(1), @Bar(2)})"})
    @Bar(value = 0)
    @BarContainer(value = {@Bar(value = 1), @Bar(value = 2)})
    int testField = 0;

    @ExpectedBase(
            value = Bar.class,
            getAnnotation = "@Bar(0)",
            getAnnotationsByType = {
                "@Bar(0)",
                "@Bar(1)",
                "@Bar(2)"
            },
            getAllAnnotationMirrors = {
                "@Bar(0)",
                "@BarContainer({@Bar(1), @Bar(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            },
            getAnnotationMirrors = {
                "@Bar(0)",
                "@BarContainer({@Bar(1), @Bar(2)})",
                "ExpectedBase",
                "ExpectedContainer"
            })
    @ExpectedContainer(
            value = BarContainer.class,
            getAnnotation = "@BarContainer({@Bar(1), @Bar(2)})",
            getAnnotationsByType = {"@BarContainer({@Bar(1), @Bar(2)})"})
    @Bar(value = 0)
    @BarContainer(value = {@Bar(value = 1), @Bar(value = 2)})
    void testMethod() {}
   }
