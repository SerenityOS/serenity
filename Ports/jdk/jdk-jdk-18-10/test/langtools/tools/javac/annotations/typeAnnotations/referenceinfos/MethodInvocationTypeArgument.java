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

/*
 * @test
 * @bug 8042451
 * @summary Test population of reference info for method invocation type arguments
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java MethodInvocationTypeArgument.java
 * @run main Driver MethodInvocationTypeArgument
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.METHOD_INVOCATION_TYPE_ARGUMENT;
import static java.lang.System.lineSeparator;

public class MethodInvocationTypeArgument {

    @TADescription(annotation = "TA", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 0, offset = 4)
    @TADescription(annotation = "TB", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 1, offset = 4)
    @TADescription(annotation = "TC", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 2, offset = 4)
    @TADescription(annotation = "TD", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 0, offset = 24)
    @TADescription(annotation = "TE", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 1, offset = 24)
    @TADescription(annotation = "TF", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 2, offset = 24)
    public String genericMethod() {
        return
                "public <T1, T2, T3> void function(T1 t1, T2 t2, T3 t3) {}" + lineSeparator() +
                        "{ new %TEST_CLASS_NAME%().<@TA Integer, @TB String, @TC Double>function(0, \"\", 0.0); " + lineSeparator() +
                        "  this.<@TD Integer, @TE String, @TF Double>function(0, \"\", 0.0); }";
    }

    @TADescription(annotation = "TA", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 0, offset = 0)
    @TADescription(annotation = "TB", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 1, offset = 0)
    @TADescription(annotation = "TC", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 2, offset = 0)
    public String genericStaticMethod() {
        return
                "public static <T1, T2, T3> void staticFunction(T1 t1, T2 t2, T3 t3) {}" + lineSeparator() +
                        "static { %TEST_CLASS_NAME%.<@TA Integer, @TB String, @TC Double>staticFunction(0, \"\", 0.0); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 0, offset = 4)
    @TADescription(annotation = "RTBs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 1, offset = 4)
    @TADescription(annotation = "RTCs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 0, offset = 20)
    @TADescription(annotation = "RTDs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 1, offset = 20)
    public String genericMethodRepeatableAnnotation() {
        return
                "public <T1, T2> void function(T1 t1, T2 t2) {}" + lineSeparator() +
                        "{ new %TEST_CLASS_NAME%().<@RTA @RTA Integer, @RTB @RTB String>" +
                        "function(0, \"\"); " + lineSeparator() +
                        "  this.<@RTC @RTC Integer, @RTD @RTD String>function(0, \"\"); }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 0, offset = 0)
    @TADescription(annotation = "RTBs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 1, offset = 0)
    @TADescription(annotation = "RTCs", type = METHOD_INVOCATION_TYPE_ARGUMENT,
            typeIndex = 2, offset = 0)
    public String genericStaticMethodRepeatableAnnotation() {
        return
                "public static <T1, T2, T3> void staticFunction(T1 t1, T2 t2, T3 t3) {}" + lineSeparator() +
                        "static { %TEST_CLASS_NAME%.<@RTA @RTA Integer, @RTB @RTB String, @RTC @RTC Double>staticFunction(0, \"\", 0.0); }";
    }

}
