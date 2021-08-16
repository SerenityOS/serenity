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
 * @summary Test population of reference info for method return
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java MethodReturns.java
 * @run main Driver MethodReturns
 */
public class MethodReturns {

    // Method returns
    @TADescription(annotation = "TA", type = METHOD_RETURN)
    public String methodReturnAsPrimitive() {
        return "@TA int test() { return 0; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    public String methodReturnAsObject() {
        return "@TA Object test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "TD", type = METHOD_RETURN,
            genericLocation = { 3, 1, 3, 0 })
    public String methodReturnAsParametrized() {
        return "@TA Map<@TB String, @TC List<@TD String>> test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 0, 0, 0, 0 })
    public String methodReturnAsArray() {
        return "@TC String @TA [] @TB [] test() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 0, 0, 0, 0 })
    public String methodReturnAsArrayOld() {
        return "@TC String test() @TA [] @TB [] { return null; }";
    }

    @TADescriptions({})
    public String methodWithDeclarationAnnotation() {
        return "@Decl String test() { return null; }";
    }

    @TADescriptions({})
    public String methodWithNoTargetAnno() {
        return "@A String test() { return null; }";
    }

    // Smoke tests
    @TADescription(annotation = "TA", type = METHOD_RETURN)
    public String interfaceMethodReturnAsObject() {
        return "interface %TEST_CLASS_NAME% { @TA Object test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN)
    public String abstractMethodReturnAsObject() {
        return "abstract class %TEST_CLASS_NAME% { abstract @TA Object test(); }";
    }


    @TADescription(annotation = "TA", type = METHOD_RETURN)
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "TD", type = METHOD_RETURN,
            genericLocation = { 3, 1, 3, 0 })
    public String interfaceMethodReturnAsParametrized() {
        return "interface %TEST_CLASS_NAME% { @TA Map<@TB String, @TC List<@TD String>> test(); }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    @TADescription(annotation = "TD", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 0 })
    @TADescription(annotation = "TE", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 1 })
    @TADescription(annotation = "TF", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 1, 2, 0 })
    public String methodReturnAsNestedWildcard() {
        return "Set<@TA ? extends @TB GOuter<String, String>. @TC GInner<@TD String, @TE ? super @TF Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = { 3, 0, 1, 0, 3, 0 })
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0, 1, 0, 3, 1 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 0, 1, 0, 3, 1, 2, 0 })
    public String methodReturnAsNestedWildcard2() {
        return "class GOuter<X, Y> { class GInner<X, Y> {} } " +
                "class %TEST_CLASS_NAME%<K> { Set<GOuter<String, String>.GInner<@TA K, @TB ? extends @TC Object>> entrySet() { return null; } }";
    }

    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcard3() {
        return "Set<? extends @TB GOuter<String, String>. @TC GInner<String, Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcard4() {
        return "Set<? extends GOuter<String, String>. @TC GInner<String, Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcard5() {
        return "Set<? extends @TB Outer. @TC Inner> entrySet() { return null; }";
    }

    @TADescription(annotation = "TA", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 0 })
    @TADescription(annotation = "TB", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 1 })
    @TADescription(annotation = "TC", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcard6() {
        return "Set<? extends GOuter<String, String>. @TC GInner<@TA String, @TB Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String methodReturnAsPrimitiveRepeatableAnnotation() {
        return "@RTA @RTA int test() { return 0; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String methodReturnAsObjectRepeatableAnnotation() {
        return "@RTA @RTA Object test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "RTDs", type = METHOD_RETURN,
            genericLocation = { 3, 1, 3, 0 })
    public String methodReturnAsParametrizedRepeatableAnnotation() {
        return "@RTA @RTA Map<@RTB @RTB String, @RTC @RTC List<@RTD @RTD String>> test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 0, 0, 0, 0 })
    public String methodReturnAsArrayRepeatableAnnotation() {
        return "@RTC @RTC String @RTA @RTA [] @RTB @RTB [] test() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 0, 0, 0, 0 })
    public String methodReturnAsArrayOldRepeatableAnnotation() {
        return "@RTC @RTC String test() @RTA @RTA [] @RTB @RTB [] { return null; }";
    }

    // Smoke tests
    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String interfaceMethodReturnAsObjectRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { @RTA @RTA Object test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String abstractMethodReturnAsObjectRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME% { abstract @RTA @RTA Object test(); }";
    }


    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "RTDs", type = METHOD_RETURN,
            genericLocation = { 3, 1, 3, 0 })
    public String interfaceMethodReturnAsParametrizedRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { @RTA @RTA Map<@RTB @RTB String, @RTC @RTC List<@RTD @RTD String>> test(); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    @TADescription(annotation = "RTDs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 0 })
    @TADescription(annotation = "RTEs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 1 })
    @TADescription(annotation = "RTFs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 1, 2, 0 })
    public String methodReturnAsNestedWildcardRepeatableAnnotation() {
        return "Set<@RTA @RTA ? extends @RTB @RTB GOuter<String, String>. @RTC @RTC GInner<@RTD @RTD String," +
                " @RTE @RTE ? super @RTF @RTF Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 1, 0, 3, 0 })
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 1, 0, 3, 1 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 1, 0, 3, 1, 2, 0 })
    public String methodReturnAsNestedWildcardRepeatableAnnotation2() {
        return "class GOuter<X, Y> { class GInner<X, Y> {} } " +
                "class %TEST_CLASS_NAME%<K> { Set<GOuter<String, String>.GInner<@RTA @RTA K," +
                " @RTB @RTB ? extends @RTC @RTC Object>> entrySet() { return null; } }";
    }

    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcardRepeatableAnnotation3() {
        return "Set<? extends @RTB @RTB GOuter<String, String>. @RTC @RTC GInner<String, Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcardRepeatableAnnotation4() {
        return "Set<? extends GOuter<String, String>. @RTC @RTC GInner<String, Object>> entrySet() { return null; }";
    }

    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcardRepeatableAnnotation5() {
        return "Set<? extends @RTB @RTB Outer. @RTC @RTC Inner> entrySet() { return null; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 0 })
    @TADescription(annotation = "RTBs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0, 3, 1 })
    @TADescription(annotation = "RTCs", type = METHOD_RETURN,
            genericLocation = { 3, 0, 2, 0, 1, 0 })
    public String methodReturnAsNestedWildcardRepeatableAnnotation6() {
        return "Set<? extends GOuter<String, String>. @RTC @RTC GInner<@RTA @RTA String," +
                " @RTB @RTB Object>> entrySet() { return null; }";
    }
}
