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
 * @summary Test population of reference info for field
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java Fields.java
 * @run main Driver Fields
 */
public class Fields {
    // field types
    @TADescription(annotation = "TA", type = FIELD)
    public String fieldAsPrimitive() {
        return "@TA int test;";
    }

    @TADescription(annotation = "TA", type = FIELD)
    public String fieldAsObject() {
        return "@TA Object test;";
    }

    @TADescription(annotation = "TA", type = FIELD)
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "TD", type = FIELD,
            genericLocation = { 3, 1, 3, 0 })
    public String fieldAsParametrized() {
        return "@TA Map<@TB String, @TC List<@TD String>> test;";
    }

    @TADescription(annotation = "TA", type = FIELD)
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = { 0, 0, 0, 0 })
    public String fieldAsArray() {
        return "@TC String @TA [] @TB [] test;";
    }

    @TADescription(annotation = "TA", type = FIELD)
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = { 0, 0, 0, 0 })
    public String fieldAsArrayOld() {
        return "@TC String test @TA [] @TB [];";
    }

    @TADescriptions({})
    public String fieldWithDeclarationAnnotatin() {
        return "@Decl String test;";
    }

    @TADescriptions({})
    public String fieldWithNoTargetAnno() {
        return "@A String test;";
    }

    // Smoke tests
    @TADescription(annotation = "TA", type = FIELD)
    public String interfaceFieldAsObject() {
        return "interface %TEST_CLASS_NAME% { @TA String test = null; }";
    }

    @TADescription(annotation = "TA", type = FIELD)
    public String abstractFieldAsObject() {
        return "abstract class %TEST_CLASS_NAME% { @TA String test; }";
    }

    @TADescription(annotation = "TA", type = FIELD)
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "TD", type = FIELD,
            genericLocation = { 3, 1, 3, 0 })
    public String interfaceFieldAsParametrized() {
        return "interface %TEST_CLASS_NAME% { @TA Map<@TB String, @TC List<@TD String>> test = null; }";
    }


    @TADescription(annotation = "TA", type = FIELD)
    @TADescription(annotation = "TB", type = FIELD,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TC", type = FIELD,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "TD", type = FIELD,
            genericLocation = { 3, 1, 3, 0 })
    public String staticFieldAsParametrized() {
        return "static @TA Map<@TB String, @TC List<@TD String>> test;";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    public String fieldAsPrimitiveRepeatableAnnotation() {
        return "@RTA @RTA int test;";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    public String fieldAsObjectRepeatableAnnotation() {
        return "@RTA @RTA Object test;";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "RTDs", type = FIELD,
            genericLocation = { 3, 1, 3, 0 })
    public String fieldAsParametrizedRepeatableAnnotation() {
        return "@RTA @RTA Map<@RTB @RTB String, @RTC @RTC List<@RTD @RTD String>> test;";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = { 0, 0 })
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = { 0, 0, 0, 0 })
    public String fieldAsArrayRepeatableAnnotation() {
        return "@RTC @RTC String @RTA @RTA [] @RTB @RTB [] test;";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    @TADescription(annotation = "RTBs", type = FIELD,
           genericLocation = { 0, 0 })
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = { 0, 0, 0, 0 })
    public String fieldAsArrayOldRepeatableAnnotation() {
        return "@RTC @RTC String test @RTA @RTA [] @RTB @RTB [];";
    }

    // Smoke tests
    @TADescription(annotation = "RTAs", type = FIELD)
    public String interfaceFieldAsObjectRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { @RTA @RTA String test = null; }";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    public String abstractFieldAsObjectRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME% { @RTA @RTA String test; }";
    }

    @TADescription(annotation = "RTAs", type = FIELD)
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "RTDs", type = FIELD,
            genericLocation = { 3, 1, 3, 0 })
    public String interfaceFieldAsParametrizedRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% { @RTA @RTA Map<@RTB @RTB String, @RTC @RTC List<@RTD @RTD String>> test = null; }";
    }


    @TADescription(annotation = "RTAs", type = FIELD)
    @TADescription(annotation = "RTBs", type = FIELD,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTCs", type = FIELD,
            genericLocation = { 3, 1 })
    @TADescription(annotation = "RTDs", type = FIELD,
            genericLocation = { 3, 1, 3, 0 })
    public String staticFieldAsParametrizedRepeatableAnnotation() {
        return "static @RTA @RTA Map<@RTB @RTB String, @RTC @RTC List<@RTD @RTD String>> test;";
    }
}
