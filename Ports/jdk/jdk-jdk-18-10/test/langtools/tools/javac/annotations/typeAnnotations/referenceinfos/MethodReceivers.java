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
 * @summary Test population of reference info for method receivers
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java MethodReceivers.java
 * @run main Driver MethodReceivers
 */
public class MethodReceivers {

    @TADescription(annotation = "TA", type = METHOD_RECEIVER)
    public String regularMethod() {
        return "class %TEST_CLASS_NAME% { void test(@TA %TEST_CLASS_NAME% this) { } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER)
    public String abstractMethod() {
        return "abstract class %TEST_CLASS_NAME% { abstract void test(@TA %TEST_CLASS_NAME% this); }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER)
    public String interfaceMethod() {
        return "interface %TEST_CLASS_NAME% { void test(@TA %TEST_CLASS_NAME% this); }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER)
    public String regularWithThrows() {
        return "class %TEST_CLASS_NAME% { void test(@TA %TEST_CLASS_NAME% this) throws Exception { } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER,
            genericLocation = {})
    @TADescription(annotation = "TB", type = METHOD_RECEIVER,
            genericLocation = {1, 0})
    @TestClass("%TEST_CLASS_NAME%$TestInner")
    public String nestedtypes1() {
        return "class %TEST_CLASS_NAME% { class TestInner { void test(@TA %TEST_CLASS_NAME%. @TB TestInner this) { } } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER,
            genericLocation = {})
    @TestClass("%TEST_CLASS_NAME%$TestInner")
    public String nestedtypes2() {
        return "class %TEST_CLASS_NAME% { class TestInner { void test(@TA %TEST_CLASS_NAME%.TestInner this) { } } }";
    }

    @TADescription(annotation = "TB", type = METHOD_RECEIVER,
            genericLocation = {1, 0})
    @TestClass("%TEST_CLASS_NAME%$TestInner")
    public String nestedtypes3() {
        return "class %TEST_CLASS_NAME% { class TestInner { void test(%TEST_CLASS_NAME%. @TB TestInner this) { } } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER)
    public String regularMethodRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME% { void test(@RTA @RTA %TEST_CLASS_NAME% this) { } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER)
    public String abstractMethodRepeatablaAnnotation() {
        return "abstract class %TEST_CLASS_NAME% { abstract void test(@RTA @RTA %TEST_CLASS_NAME% this); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER)
    public String interfaceMethodRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { void test(@RTA @RTA %TEST_CLASS_NAME% this); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER)
    public String regularWithThrowsRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME% { void test(@RTA @RTA %TEST_CLASS_NAME% this) throws Exception { } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = METHOD_RECEIVER,
            genericLocation = {1, 0})
    @TestClass("%TEST_CLASS_NAME%$TestInner")
    public String nestedtypesRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% { class TestInner { void test(@RTA @RTA %TEST_CLASS_NAME%. @RTB @RTB TestInner this) { } } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER,
            genericLocation = {})
    @TestClass("%TEST_CLASS_NAME%$TestInner")
    public String nestedtypesRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { class TestInner { void test(@RTA @RTA %TEST_CLASS_NAME%.TestInner this) { } } }";
    }

    @TADescription(annotation = "RTBs", type = METHOD_RECEIVER,
            genericLocation = {1, 0})
    @TestClass("%TEST_CLASS_NAME%$TestInner")
    public String nestedtypesRepeatableAnnotation3() {
        return "class %TEST_CLASS_NAME% { class TestInner { void test(%TEST_CLASS_NAME%. @RTB @RTB TestInner this) { } } }";
    }
}
