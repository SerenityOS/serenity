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
import static java.lang.System.lineSeparator;

/*
 * @test
 * @bug 8042451
 * @summary Test population of reference info for method type parameters
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java MethodTypeParam.java
 * @run main Driver MethodTypeParam
 */
public class MethodTypeParam {

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClass() {
        return "<@TA K extends @TB Date, @TC V extends @TD Object & @TE Cloneable> void test() { }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClass2() {
        return "<@TA K extends @TB Date, @TC V extends @TE Cloneable> void test() { }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    public String regularClassParameterized() {
        return "<K extends @TA Map<String, @TB String>, V extends @TF Object & @TC List<@TD List<@TE Object>>> void test() { }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String abstractClass() {
        return "abstract class %TEST_CLASS_NAME% { abstract <@TA K extends @TB Date, @TC V extends @TD Object & @TE Cloneable> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    public String abstractClassParameterized() {
        return "abstract class %TEST_CLASS_NAME% { abstract <K extends @TG Object & @TA Map<String, @TB String>, V extends @TF Object & @TC List<@TD List<@TE Object>>> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    public String abstractClassParameterized2() {
        return "abstract class %TEST_CLASS_NAME% { abstract <K extends @TA Map<String, @TB String>, V extends @TC List<@TD List<@TE Object>>> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String abstractClassParameterized3() {
        return "abstract class %TEST_CLASS_NAME% { abstract <K extends @TA List<String>, V extends @TB List<Object>> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularInterface() {
        return "interface %TEST_CLASS_NAME% { <@TA K extends @TB Date, @TC V extends @TD Object & @TE Cloneable> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TH", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TI", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    public String regularInterfaceParameterized() {
        return "interface %TEST_CLASS_NAME% { <@TH K extends @TG Object & @TA Map<String, @TB String>, @TI V extends @TF Object & @TC List<@TD List<@TE Object>>> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "TF", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "TG", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    public String regularInterfaceParameterized2() {
        return "interface %TEST_CLASS_NAME% { <@TF K extends @TA Map<String, @TB String>, @TG V extends @TC List<@TD List<@TE Object>>> void test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    public String useInReturn1() {
        return "class %TEST_CLASS_NAME% { <T> @TA T m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN, genericLocation = {3, 0})
    public String useInReturn2() {
        return "class %TEST_CLASS_NAME% { <T> Class<@TA T> m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_RETURN)
        public String useInReturn3() {
        return "class %TEST_CLASS_NAME% { <T extends @TA Object> @TB T m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParam1() {
        return "class %TEST_CLASS_NAME% { <T> void m(Class<@TA T> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParam2() {
        return "class %TEST_CLASS_NAME% { void m(Class<@TA Object> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 2)
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    public String useInParam3() {
        return "interface IA {} " +
               "interface IB<XB> {} " +
               "interface IC<XC> {} " +
               "class %TEST_CLASS_NAME% { <T extends @TA IB<IA> & @TB IC<IA>> void m(@TC T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 1,
                genericLocation = {})
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 2,
                genericLocation = {})
    @TADescription(annotation = "TC", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 0)
    public String useInParam4() {
        return "class %TEST_CLASS_NAME% {" +
               "  interface IA {} " +
               "  interface IB<XB> {} " +
               "  interface IC<XC> {} " +
               "  <T extends @TA IB<IA> & @TB IC<IA>> void m(@TC T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 0,
                genericLocation = {})
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 0,
                genericLocation = {1, 0})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 0,
                genericLocation = {1, 0, 3, 0})
        public String useInParam5() {
    return "class %TEST_CLASS_NAME% {" +
               "  interface IA {} " +
               "  class CB<XC> {} " +
               "  <T extends @TA %TEST_CLASS_NAME%. @TB CB<@TC IA>> void m(T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "TA", type = METHOD_TYPE_PARAMETER,
                paramIndex = 0)
    @TADescription(annotation = "TB", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 0,
                genericLocation = {})
    @TADescription(annotation = "TC", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 0,
                genericLocation = {1, 0, 3, 0})
    @TADescription(annotation = "TD", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 1,
                genericLocation = {})
    @TADescription(annotation = "TE", type = METHOD_TYPE_PARAMETER_BOUND,
                paramIndex = 0, boundIndex = 1,
                genericLocation = {3, 0})
    public String useInParam6() {
        return "class %TEST_CLASS_NAME% {" +
               "  interface IA {} " +
               "  interface IB<XB> {} " +
               "  class CC<XC> {} " +
               "  interface ID<XD> {} " +
               "  <@TA T extends @TB %TEST_CLASS_NAME%.CC<@TC IA> & %TEST_CLASS_NAME%. @TD ID<@TE IA>> void m(T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClassRepeatableAnnotation() {
        return "<@RTA @RTA K extends @RTB @RTB Date, @RTC @RTC V extends @RTD @RTD Object & @RTE @RTE Cloneable> void test() { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularClassRepeatableAnnotation2() {
        return "<@RTA @RTA K extends @RTB @RTB Date, @RTC @RTC V extends @RTE @RTE Cloneable> void test() { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    @TADescription(annotation = "RTFs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    public String regularClassParameterizedRepeatableAnnotation() {
        return "<K extends @RTA @RTA Map<String, @RTB @RTB String>, V extends @RTF @RTF Object" +
                " & @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> void test() { }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String abstractClassRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME% { abstract <@RTA @RTA K extends @RTB @RTB Date," +
                " @RTC @RTC V extends @RTD @RTD Object & @RTE @RTE Cloneable> void test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTFs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    public String abstractClassParameterizedRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME% { abstract <K extends @RTE @RTE Object &" +
                " @RTA @RTA Map<String, @RTB @RTB String>, V extends @RTF @RTF Object &" +
                " @RTC @RTC List<@RTD @RTD Object>> void test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0, 3, 0})
    public String abstractClassParameterizedRepeatableAnnotation2() {
        return "abstract class %TEST_CLASS_NAME% { abstract <K extends @RTA @RTA Map<String, @RTB @RTB String>," +
                " V extends @RTC @RTC List<@RTD @RTD List<@RTE @RTE Object>>> void test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String abstractClassParameterizedRepeatableAnnotation3() {
        return "abstract class %TEST_CLASS_NAME% { abstract <K extends @RTA @RTA List<String>," +
                " V extends @RTB @RTB List<Object>> void test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 0)
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    public String regularInterfaceRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { <@RTA @RTA K extends @RTB @RTB Date," +
                " @RTC @RTC V extends @RTD @RTD Object & @RTE @RTE Cloneable> void test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1, genericLocation = {3, 1})
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1)
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 1, boundIndex = 1, genericLocation = {3, 0})
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER, paramIndex = 1)
    @TADescription(annotation = "RTFs", type = METHOD_TYPE_PARAMETER, paramIndex = 0)
    public String regularInterfaceParameterizedRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { <@RTF @RTF K extends @RTA @RTA Map<String, @RTB @RTB String>," +
                " @RTE @RTE V extends @RTC @RTC List<@RTD @RTD Object>> void test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String useInReturnRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% { <T> @RTA @RTA T m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN, genericLocation = {3, 0})
    public String useInReturnRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { <T> Class<@RTA @RTA T> m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN)
    public String useInReturnRepeatableAnnotation3() {
        return "class %TEST_CLASS_NAME% { <T extends @RTA @RTA Object> @RTB @RTB T m() { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParamRepeatableAnnotation1() {
        return "class %TEST_CLASS_NAME% { <T> void m(Class<@RTA @RTA T> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0, genericLocation = {3, 0})
    public String useInParamRepeatableAnnotation2() {
        return "class %TEST_CLASS_NAME% { void m(Class<@RTA @RTA Object> p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 1)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex = 0, boundIndex = 2)
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER, paramIndex = 0)
    public String useInParamRepeatableAnnotation3() {
        return "interface IA {} " +
                "interface IB<XB> {} " +
                "interface IC<XC> {} " +
                "class %TEST_CLASS_NAME% { <T extends @RTA @RTA IB<IA> & @RTB @RTB IC<IA>>" +
                " void m(@RTC @RTC T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 1,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 2,
            genericLocation = {})
    @TADescription(annotation = "RTCs", type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0)
    public String useInParamRepeatableAnnotation4() {
        return "class %TEST_CLASS_NAME% {" +
                "  interface IA {} " +
                "  interface IB<XB> {} " +
                "  interface IC<XC> {} " +
                "  <T extends @RTA @RTA IB<IA> & @RTB @RTB IC<IA>>" +
                " void m(@RTC @RTC T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 0,
            genericLocation = {})
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 0,
            genericLocation = {1, 0})
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 0,
            genericLocation = {1, 0, 3, 0})
    public String useInParamRepeatableAnnotation5() {
        return "class %TEST_CLASS_NAME% {" +
                "  interface IA {} " +
                "  class CB<XC> {} " +
                "  <T extends @RTA @RTA %TEST_CLASS_NAME%. @RTB @RTB" +
                " CB<@RTC @RTC IA>> void m(T p) { throw new RuntimeException(); } }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_TYPE_PARAMETER,
            paramIndex = 0)
    @TADescription(annotation = "RTBs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 0,
            genericLocation = {})
    @TADescription(annotation = "RTCs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 0,
            genericLocation = {1, 0, 3, 0})
    @TADescription(annotation = "RTDs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 1,
            genericLocation = {})
    @TADescription(annotation = "RTEs", type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0, boundIndex = 1,
            genericLocation = {3, 0})
    public String useInParamRepeatableAnnotation6() {
        return "class %TEST_CLASS_NAME% {" +
                "  interface IA {} " +
                "  interface IB<XB> {} " +
                "  class CC<XC> {} " +
                "  interface ID<XD> {} " +
                "  <@RTA @RTA T extends @RTB @RTB %TEST_CLASS_NAME%.CC<@RTC @RTC IA> &" +
                " %TEST_CLASS_NAME%. @RTD @RTD ID<@RTE @RTE IA>> void m(T p) { throw new RuntimeException(); } }";
    }
}
