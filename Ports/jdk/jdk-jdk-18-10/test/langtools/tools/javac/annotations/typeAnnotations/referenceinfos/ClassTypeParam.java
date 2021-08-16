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
 * @summary Test population of reference info for class type parameters
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java ClassTypeParam.java
 * @run main Driver ClassTypeParam
 */
public class ClassTypeParam {

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClass1() {
        return "class %TEST_CLASS_NAME%<@TA K extends @TB Date, @TC V extends @TD Object & @TE Cloneable> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClass2() {
        return "class %TEST_CLASS_NAME%<@TA K extends @TB Date, @TC V extends @TE Cloneable> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    public String regularClassParameterized() {
        return "class %TEST_CLASS_NAME%<K extends @TA Map<String, @TB String>, V extends @TC List<@TD List<@TE Object>>> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TG", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    public String regularClassParameterized2() {
        return "class %TEST_CLASS_NAME%<K extends @TG Object & @TA Map<String, @TB String>, V extends @TF Object & @TC List<@TD List<@TE Object>>> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String abstractClass() {
        return "abstract class %TEST_CLASS_NAME%<@TA K extends @TB Date, @TC V extends @TD Object & @TE Cloneable> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    public String abstractClassParameterized() {
        return "abstract class %TEST_CLASS_NAME%<K extends @TA Map<String, @TB String>, V extends @TF Object & @TC List<@TD List<@TE Object>>> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularInterface() {
        return "interface %TEST_CLASS_NAME%<@TA K extends @TB Date, @TC V extends @TD Object & @TE Cloneable> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    public String regularInterfaceParameterized() {
        return "interface %TEST_CLASS_NAME%<K extends @TA Map<String, @TB String>, V extends @TC List<@TD List<@TE Object>>> { }";
    }

    @TADescription(annotation = "TA", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TG", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    public String regularInterfaceParameterized2() {
        return "interface %TEST_CLASS_NAME%<K extends @TG Object & @TA Map<String, @TB String>, V extends @TF Object & @TC List<@TD List<@TE Object>>> { }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    public String useInReturn1() {
        return "class %TEST_CLASS_NAME%<T> { @TA T m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN, genericLocation = {3, 0})
    public String useInReturn2() {
        return "class %TEST_CLASS_NAME%<T> { Class<@TA T> m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParam1() {
        return "class %TEST_CLASS_NAME%<T> { void m(Class<@TA T> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParam2() {
        return "class %TEST_CLASS_NAME% { void m(Class<@TA Object> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClassRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME%<@RTA @RTA K extends @RTB @RTB Date, @RTC @RTC V extends @RTD @RTD Object & @RTE @RTE Cloneable> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClassRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME%<@RTA @RTA K extends @RTB @RTB Date, @RTC @RTC V extends @RTE @RTE Cloneable> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    public String regularClassParameterizedRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME%<K extends @RTA @RTA Map<String, @RTB @RTB String>," +
                " V extends @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTGs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    public String regularClassParameterizedRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME%<K extends @RTG @RTG Object & @RTA @RTA Map<String, @RTB @RTB String>," +
                " V extends @RTF @RTF Object & @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String abstractClassRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME%<@RTA @RTA K extends @RTB @RTB Date," +
                " @RTC @RTC V extends @RTD @RTD Object & @RTE @RTE Cloneable> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    public String abstractClassParameterizedRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME%<K extends @RTA @RTA Map<String, @RTB @RTB String>," +
                " V extends @RTF @RTF Object & @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularInterfaceRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME%<@RTA @RTA K extends @RTB @RTB Date," +
                " @RTC @RTC V extends @RTD @RTD Object & @RTE @RTE Cloneable> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    public String regularInterfaceParameterizedRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME%<K extends @RTA @RTA Map<String, @RTB @RTB String>," +
                " V extends @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTGs", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    public String regularInterfaceParameterizedRepeatableAnnotation2() {
        return "interface %TEST_CLASS_NAME%<K extends @RTG @RTG Object & @RTA @RTA Map<String, @RTB @RTB String>," +
                " V extends @RTF @RTF Object & @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String useInReturnRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME%<T> { @RTA @RTA T m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN, genericLocation = {3, 0})
    public String useInReturnRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME%<T> { Class<@RTA @RTA T> m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParamRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME%<T> { void m(Class<@RTA @RTA T> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParamRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { void m(Class<@RTA @RTA Object> p) { throw new RuntimeException(); } }";
    }

}
