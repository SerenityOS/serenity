/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test population of reference info for method exception clauses
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java MethodThrows.java
 * @run main Driver MethodThrows
 */
public class MethodThrows {

    @TADescription(annotation = "TA", type = THROWS, typeIndex = 0)
    @TADescription(annotation = "TB", type = THROWS, typeIndex = 2)
    public String regularMethod() {
        return "class %TEST_CLASS_NAME% { void test() throws @TA RuntimeException, IllegalArgumentException, @TB Exception { } }";
    }

    @TADescription(annotation = "TA", type = THROWS, typeIndex = 0)
    @TADescription(annotation = "TB", type = THROWS, typeIndex = 2)
    public String abstractMethod() {
        return "abstract class %TEST_CLASS_NAME% { abstract void test() throws @TA RuntimeException, IllegalArgumentException, @TB Exception; }";
    }

    @TADescription(annotation = "TA", type = THROWS, typeIndex = 0)
    @TADescription(annotation = "TB", type = THROWS, typeIndex = 2)
    public String interfaceMethod() {
        return "interface %TEST_CLASS_NAME% { void test() throws @TA RuntimeException, IllegalArgumentException, @TB Exception; }";
    }

    @TADescription(annotation = "TA", type = THROWS, typeIndex = 0,
                   genericLocation = {})
    @TADescription(annotation = "TB", type = THROWS, typeIndex = 0,
                   genericLocation = {1, 0})
    @TADescription(annotation = "TC", type = THROWS, typeIndex = 0,
                   genericLocation = {1, 0, 1, 0})
    @TADescription(annotation = "TD", type = THROWS, typeIndex = 1,
                   genericLocation = {})
    @TADescription(annotation = "TE", type = THROWS, typeIndex = 1,
                   genericLocation = {1, 0})
    @TADescription(annotation = "TF", type = THROWS, typeIndex = 1,
                   genericLocation = {1, 0, 1, 0})
    public String NestedTypes() {
        return "class Outer { class Middle { class Inner1 extends Exception {}" +
                "  class Inner2 extends Exception{} } }" +
                "class %TEST_CLASS_NAME% { void test() throws @TA Outer.@TB Middle.@TC Inner1, @TD Outer.@TE Middle.@TF Inner2 { } }";
    }
}
