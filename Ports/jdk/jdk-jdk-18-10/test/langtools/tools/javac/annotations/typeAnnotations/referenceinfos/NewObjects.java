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
 * @summary Test population of reference info for new object creations
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java NewObjects.java
 * @run main Driver NewObjects
 */
public class NewObjects {

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObject() {
        return "Object returnObject() { return new @TA String(); }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObjectGeneric() {
        return "Object returnObjectGeneric() { return new @TA ArrayList<@TB String>(); }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObject() {
        return "void initObject() { Object a =  new @TA String(); }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = NEW,
            genericLocation = { 3, 1 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObjectGeneric() {
        return "void initObjectGeneric() { Object a = new @TA HashMap<@TB String, @TC String>(); }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObject() {
        return "void eqtestObject() { if (null == new @TA String()); }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObjectGeneric() {
        return "void eqtestObjectGeneric() { if (null == new @TA ArrayList<@TB String >()); }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    @TADescription(annotation = "TB", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnNewArray1() {
        return "Object returnNewArray1() { return new @TA String @TB[1]; }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "TB", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String returnNewArray2() {
        return "Object returnNewArray2() { return new @TA String @TB [1] @TC [2]; }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "TB", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TD", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String returnNewArray3() {
        return "Object returnNewArray3() { return new @TA Outer. @TB Inner @TC [1] @TD [2]; }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "TB", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "TD", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TE", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String returnNewArray4() {
        return "Object returnNewArray4() { return new @TA Outer. @TB Middle. @TC MInner @TD [1] @TE [2]; }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "TC", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TD", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "TE", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0})
    @TADescription(annotation = "TF", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0})
    public String returnNewArray5() {
        return "Object returnNewArray5() { return new @TA ArrayList<@TB Outer. @TC Middle. @TD MInner @TE [] @TF []>(); }";
    }

    @TADescription(annotation = "TA", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "TB", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "TC", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TD", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE,
    genericLocation = {0, 0})
    public String arrayField() {
        return "@TA Outer. @TB Inner @TC [] @TD [] f;";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObjectRepeatableAnnotation() {
        return "Object returnObject() { return new @RTA @RTA String(); }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnObjectGenericRepeatableAnnotation() {
        return "Object returnObjectGeneric() { return new @RTA @RTA ArrayList<@RTB @RTB String>(); }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObjectRepeatableAnnotation() {
        return "void initObject() { Object a =  new @RTA @RTA String(); }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = NEW,
            genericLocation = { 3, 1 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String initObjectGenericRepeatableAnnotation() {
        return "void initObjectGeneric() { Object a = new @RTA @RTA HashMap<@RTB @RTB String, @RTC @RTC String>(); }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObjectRepeatableAnnotation() {
        return "void eqtestObject() { if (null == new @RTA @RTA String()); }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String eqtestObjectGenericRepeatableAnnotation() {
        return "void eqtestObjectGeneric() { if (null == new @RTA @RTA ArrayList<@RTB @RTB String >()); }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    @TADescription(annotation = "RTBs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String returnNewArrayRepeatableAnnotation1() {
        return "Object returnNewArray1() { return new @RTA @RTA String @RTB @RTB[1]; }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String returnNewArrayRepeatableAnnotation2() {
        return "Object returnNewArray2() { return new @RTA @RTA String @RTB @RTB [1] @RTC @RTC [2]; }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTDs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String returnNewArrayRepeatableAnnotation3() {
        return "Object returnNewArray3() { return new @RTA @RTA Outer. @RTB @RTB Inner @RTC @RTC [1] @RTD @RTD [2]; }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "RTDs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTEs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String returnNewArrayRepeatableAnnotation4() {
        return "Object returnNewArray4() { return new @RTA @RTA Outer." +
                " @RTB @RTB Middle. @RTC @RTC MInner @RTD @RTD [1] @RTE @RTE [2]; }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0, 0, 0})
    @TADescription(annotation = "RTCs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTDs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0, 0, 0, 1, 0, 1, 0})
    @TADescription(annotation = "RTEs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0})
    @TADescription(annotation = "RTFs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {3, 0, 0, 0})
    public String returnNewArrayRepeatableAnnotation5() {
        return "Object returnNewArray5() { return new @RTA @RTA ArrayList<@RTB @RTB Outer." +
                " @RTC @RTC Middle. @RTD @RTD MInner @RTE @RTE [] @RTF @RTF []>(); }";
    }

    @TADescription(annotation = "RTAs", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0})
    @TADescription(annotation = "RTBs", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0, 0, 0, 1, 0})
    @TADescription(annotation = "RTCs", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTDs", type = FIELD, offset = ReferenceInfoUtil.IGNORE_VALUE,
            genericLocation = {0, 0})
    public String arrayFieldRepeatableAnnotation() {
        return "@RTA @RTA Outer. @RTB @RTB Inner @RTC @RTC [] @RTD @RTD [] f;";
    }
}
