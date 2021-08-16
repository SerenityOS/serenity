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

/*
 * @test
 * @bug 8026791 8042451
 * @summary Test population of reference info for constructor results
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java Constructors.java
 * @run main Driver Constructors
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

public class Constructors {

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    @TADescription(annotation = "TB", type = METHOD_RETURN)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    public String regularClass() {
        return "class %TEST_CLASS_NAME% { @TA %TEST_CLASS_NAME%() {}" +
                           " @TB %TEST_CLASS_NAME%(@TC int b) {} }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "TB", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String innerClass() {
        return "class %TEST_CLASS_NAME% { class Inner {" +
               " @TA Inner() {}" +
               " @TB Inner(@TC int b) {}" +
               " } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER)
    @TADescription(annotation = "TB", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "TC", type = METHOD_RECEIVER)
    @TADescription(annotation = "TD", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "TE", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String innerClass2() {
        return "class %TEST_CLASS_NAME% { class Inner {" +
               " @TB Inner(@TA %TEST_CLASS_NAME% %TEST_CLASS_NAME%.this) {}" +
               " @TD Inner(@TC %TEST_CLASS_NAME% %TEST_CLASS_NAME%.this, @TE int b) {}" +
               " } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RECEIVER)
    @TADescription(annotation = "TB", type = METHOD_RECEIVER, genericLocation = {1, 0})
    @TADescription(annotation = "TC", type = METHOD_RETURN, genericLocation = {1, 0, 1, 0})
    @TADescription(annotation = "TD", type = METHOD_RECEIVER, genericLocation = {1, 0})
    @TADescription(annotation = "TE", type = METHOD_RETURN, genericLocation = {1, 0, 1, 0})
    @TADescription(annotation = "TF", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TestClass("Outer$Middle$Inner")
    public String innerClass3() {
        return "class Outer { class Middle { class Inner {" +
               " @TC Inner(@TA Outer. @TB Middle Middle.this) {}" +
               " @TE Inner(@TD Middle Outer.Middle.this, @TF int b) {}" +
               " } } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN)
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    public String regularClassRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME% { @RTA  @RTA %TEST_CLASS_NAME%() {}" +
                " @RTB @RTB %TEST_CLASS_NAME%(@RTC @RTC int b) {} }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "RTBs", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String innerClassRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME% { class Inner {" +
                " @RTA @RTA Inner() {}" +
                " @RTB @RTB Inner(@RTC @RTC int b) {}" +
                " } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "RTCs", type = METHOD_RECEIVER)
    @TADescription(annotation = "RTDs", type = METHOD_RETURN, genericLocation = {1, 0})
    @TADescription(annotation = "RTEs", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TestClass("%TEST_CLASS_NAME%$Inner")
    public String innerClassRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { class Inner {" +
                " @RTB @RTB Inner(@RTA @RTA %TEST_CLASS_NAME% %TEST_CLASS_NAME%.this) {}" +
                " @RTD @RTD Inner(@RTC @RTC %TEST_CLASS_NAME% %TEST_CLASS_NAME%.this, @RTE @RTE int b) {}" +
                " } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RECEIVER)
    @TADescription(annotation = "RTBs", type = METHOD_RECEIVER, genericLocation = {1, 0})
    @TADescription(annotation = "RTCs", type = METHOD_RETURN, genericLocation = {1, 0, 1, 0})
    @TADescription(annotation = "RTDs", type = METHOD_RECEIVER, genericLocation = {1, 0})
    @TADescription(annotation = "RTEs", type = METHOD_RETURN, genericLocation = {1, 0, 1, 0})
    @TADescription(annotation = "RTFs", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    @TestClass("Outer$Middle$Inner")
    public String innerClassRepatableAnnotation3() {
        return "class Outer { class Middle { class Inner {" +
                " @RTC @RTC Inner(@RTA @RTA Outer. @RTB @RTB Middle Middle.this) {}" +
                " @RTE @RTE Inner(@RTD @RTD Middle Outer.Middle.this, @RTF @RTF int b) {}" +
                " } } }";
    }
}
