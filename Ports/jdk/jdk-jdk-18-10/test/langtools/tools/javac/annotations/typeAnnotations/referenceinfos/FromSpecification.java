/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

/*
 * @test
 * @bug 8042451
 * @summary Test that the examples from the manual are stored as expected
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java FromSpecification.java
 * @run main Driver FromSpecification
 */
public class FromSpecification {

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0, 2, 0}, paramIndex = 0)
    @TADescription(annotation = "TD", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 1}, paramIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 1, 3, 0}, paramIndex = 0)
    public String testSpec1() {
        return "void test(@TA Map<@TB ? extends @TC String, @TD List<@TE Object>> a) { }";
    }

    @TADescription(annotation = "TF", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {0, 0}, paramIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TI", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {0, 0, 0, 0, 0, 0}, paramIndex = 0)
    public String testSpec2() {
        return "void test(@TI String @TF [] @TG [] @TH [] a) { }";
    }

    // Note first "1, 0" for top-level class Test.
    @TADescription(annotation = "TJ", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TK", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TL", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TM", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0}, paramIndex = 0)
    public String testSpec3() {
        return "class %TEST_CLASS_NAME% { class O1 { class O2 { class O3 { class NestedStatic {} } } }" +
                "void test(@TM O1.@TL O2.@TK O3.@TJ NestedStatic a) { } }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0}, paramIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TD", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0, 3, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0, 3, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TF", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 0, 3, 0, 0, 0, 0, 0, 0, 0}, paramIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 1}, paramIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {3, 1, 3, 0}, paramIndex = 0)
    public String testSpec4() {
        return "void test(@TA Map<@TB Comparable<@TF Object @TC [] @TD [] @TE []>, @TG List<@TH String>> a) { }";
    }

    // Note first "1, 0" for top-level class Test.
    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 1, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 1, 0, 1, 0, 3, 1}, paramIndex = 0)
    @TADescription(annotation = "TD", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0}, paramIndex = 0)
    @TADescription(annotation = "TF", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 3, 0}, paramIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0, 1, 0, 3, 1}, paramIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_FORMAL_PARAMETER,
                genericLocation = {1, 0}, paramIndex = 0)
    public String testSpec5() {
        return "class %TEST_CLASS_NAME% { class O1 { class O2<A, B> { class O3 { class Nested<X, Y> {} } } }" +
                "void test(@TH O1.@TE O2<@TF String, @TG String>.@TD O3.@TA Nested<@TB String, @TC String> a) { } }";
    }
}
