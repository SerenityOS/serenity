/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013852 8042451
 * @summary Test population of reference info for instance and class initializers
 * @author Werner Dietl
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java Initializers.java
 * @run main Driver Initializers
 */
public class Initializers {

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
        public String instanceInit1() {
        return "class %TEST_CLASS_NAME% { { Object o = new @TA ArrayList<@TB String>(); } }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TD", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
        public String instanceInit2() {
        return "class %TEST_CLASS_NAME% { Object f = new @TA ArrayList<@TB String>(); " +
                " { Object o = new @TC ArrayList<@TD String>(); } }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
        public String staticInit1() {
        return "class %TEST_CLASS_NAME% { static { Object o = new @TA ArrayList<@TB String>(); } }";
    }

    @TADescription(annotation = "TA", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TB", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TC", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TD", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TE", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "TF", type = NEW,
                genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
        public String staticInit2() {
        return "class %TEST_CLASS_NAME% { Object f = new @TA ArrayList<@TB String>(); " +
                " static Object g = new @TC ArrayList<@TD String>(); " +
                " static { Object o = new @TE ArrayList<@TF String>(); } }";
    }

    @TADescription(annotation = "TA", type = CAST,
                typeIndex = 0, offset = ReferenceInfoUtil.IGNORE_VALUE)
        public String lazyConstantCast1() {
        return "class %TEST_CLASS_NAME% { public static final Object o = (@TA Object) null; }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String instanceInitRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% { { Object o = new @RTA @RTA ArrayList<@RTB @RTB String>(); } }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTDs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String instanceInitRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { Object f = new @RTA @RTA ArrayList<@RTB @RTB String>(); " +
                " { Object o = new @RTC @RTC ArrayList<@RTD @RTD String>(); } }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String staticInitRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% { static { Object o = new @RTA @RTA ArrayList<@RTB @RTB String>(); } }";
    }

    @TADescription(annotation = "RTAs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTBs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTCs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTDs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTEs", type = NEW, offset = ReferenceInfoUtil.IGNORE_VALUE)
    @TADescription(annotation = "RTFs", type = NEW,
            genericLocation = { 3, 0 }, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String staticInitRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { Object f = new @RTA @RTA ArrayList<@RTB @RTB String>(); " +
                " static Object g = new @RTC @RTC ArrayList<@RTD @RTD String>(); " +
                " static { Object o = new @RTE @RTE ArrayList<@RTF @RTF String>(); } }";
    }

    // TODO: test interaction with several constructors, especially non-initial constructors.
    // I don't think this kind of test is possible here.

    @TADescription(annotation = "RTAs", type = CAST,
            typeIndex = 0, offset = ReferenceInfoUtil.IGNORE_VALUE)
    public String lazyConstantCastRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% { public static final Object o = (@RTA @RTA Object) null; }";
    }
}
