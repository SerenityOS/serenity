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
 * @bug 8028576 8042451
 * @summary Test population of reference info for exception parameters
 * @author Werner Dietl
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java ExceptionParameters.java
 * @run main Driver ExceptionParameters
 */
public class ExceptionParameters {

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    public String exception() {
        return "void exception() { try { new Object(); } catch(@TA Exception e) { } }";
    }

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    public String finalException() {
        return "void finalException() { try { new Object(); } catch(final @TA Exception e) { } }";
    }

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "TB", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "TC", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multipleExceptions1() {
        return "void multipleExceptions() { " +
            "try { new Object(); } catch(@TA Exception e) { }" +
            "try { new Object(); } catch(@TB Exception e) { }" +
            "try { new Object(); } catch(@TC Exception e) { }" +
            " }";
    }

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "TB", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "TC", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multipleExceptions2() {
        return "void multipleExceptions() { " +
            "  try { new Object(); " +
            "    try { new Object(); " +
            "      try { new Object(); } catch(@TA Exception e) { }" +
            "    } catch(@TB Exception e) { }" +
            "  } catch(@TC Exception e) { }" +
            "}";
    }

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "TB", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "TC", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multipleExceptions3() {
        return "void multipleExceptions() { " +
            "  try { new Object(); " +
            "  } catch(@TA Exception e1) { "+
            "    try { new Object(); " +
            "    } catch(@TB Exception e2) {" +
            "      try { new Object(); } catch(@TC Exception e3) { }" +
            "    }" +
            "  }" +
            "}";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    public String exceptionRepeatableAnnotation() {
        return "void exception() { try { new Object(); } catch(@RTA @RTA Exception e) { } }";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "RTBs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "RTCs", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multipleExceptionsRepeatableAnnotation1() {
        return "void multipleExceptions() { " +
                "try { new Object(); } catch(@RTA @RTA Exception e) { }" +
                "try { new Object(); } catch(@RTB @RTB Exception e) { }" +
                "try { new Object(); } catch(@RTC @RTC Exception e) { }" +
                " }";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "RTBs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "RTCs", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multipleExceptionsRepeatableAnnotation2() {
        return "void multipleExceptions() { " +
                "  try { new Object(); " +
                "    try { new Object(); " +
                "      try { new Object(); } catch(@RTA @RTA Exception e) { }" +
                "    } catch(@RTB @RTB Exception e) { }" +
                "  } catch(@RTC @RTC Exception e) { }" +
                "}";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "RTBs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "RTCs", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multipleExceptionsRepeatableAnnotation3() {
        return "void multipleExceptions() { " +
                "  try { new Object(); " +
                "  } catch(@RTA @RTA Exception e1) { "+
                "    try { new Object(); " +
                "    } catch(@RTB @RTB Exception e2) {" +
                "      try { new Object(); } catch(@RTC @RTC Exception e3) { }" +
                "    }" +
                "  }" +
                "}";
    }
}
