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
 * @summary Test population of reference info for class literals
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java TypeTests.java
 * @run main Driver TypeTests
 */
public class TypeTests {

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObject() {
        return "Object returnObject() { return null instanceof @TA String; }";
    }

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObjectArray() {
        return "Object returnObjectArray() { return null instanceof @TC String @TA [] @TB []; }";
    }

    // no type test for primitives

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnPrimArray() {
        return "Object returnPrimArray() { return null instanceof @TC int @TA [] @TB []; }";
    }

    // no void
    // no void array

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObject() {
        return "void initObject() { Object a =  null instanceof @TA String; }";
    }

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObjectArray() {
        return "void initObjectArray() { Object a = null instanceof @TC String @TA [] @TB []; }";
    }

    // no primitive instanceof

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initPrimArray() {
        return "void initPrimArray() { Object a = null instanceof @TC int @TA [] @TB []; }";
    }

    // no void
    // no void array

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObject() {
        return "void eqtestObject() { if (true == (null instanceof @TA String)); }";
    }

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObjectArray() {
        return "void eqtestObjectArray() { if (true == (null instanceof @TC String @TA [] @TB [])); }";
    }

    // no primitives

    @TADescription(annotation = "TA", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestPrimArray() {
        return "void eqtestPrimArray() { if (true == (null instanceof @TC int @TA [] @TB [])); }";
    }

    // no void
    // no void array

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObjectRepeatableAnnotation() {
        return "Object returnObject() { return null instanceof @RTA @RTA String; }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObjectArrayRepeatableAnnotation() {
        return "Object returnObjectArray() { return null instanceof @RTC @RTC String @RTA @RTA [] @RTB @RTB []; }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnPrimArrayRepeatableAnnotation() {
        return "Object returnPrimArrayRepeatableAnnotation() { return null instanceof @RTC @RTC int @RTA @RTA [] @RTB @RTB []; }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObjectRepeatableAnnotation() {
        return "void initObject() { Object a =  null instanceof @RTA @RTA String; }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObjectArrayRepeatableAnnotation() {
        return "void initObjectArray() { Object a = null instanceof @RTC @RTC String @RTA @RTA [] @RTB @RTB []; }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initPrimArrayRepeatableAnnotation() {
        return "void initPrimArray() { Object a = null instanceof @RTC @RTC int @RTA @RTA [] @RTB @RTB []; }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObjectRepeatableAnnotation() {
        return "void eqtestObject() { if (true == (null instanceof @RTA @RTA String)); }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObjectArrayRepeatableAnnotation() {
        return "void eqtestObjectArray() { if (true == (null instanceof @RTC @RTC String @RTA @RTA [] @RTB @RTB [])); }";
    }

    @TADescription(annotation = "RTAs", type = INSTANCEOF, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = INSTANCEOF,
            genericLocation = { 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = INSTANCEOF,
            genericLocation = { 0, 0, 0, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestPrimArrayRepeatableAnnotation() {
        return "void eqtestPrimArray() { if (true == (null instanceof @RTC @RTC int @RTA @RTA [] @RTB @RTB [])); }";
    }
}
