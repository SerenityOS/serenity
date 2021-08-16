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
 * @bug 8006732 8006775 8042451
 * @summary Test population of reference info for multicatch exception parameters
 * @author Werner Dietl
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java MultiCatch.java
 * @run main Driver MultiCatch
 */
public class MultiCatch {

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "TB", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    public String multiCatch1() {
        return "void multiCatch1() { " +
            "try { new Object(); } catch (@TA NullPointerException | @TB IndexOutOfBoundsException e) { e.toString(); } }";
    }

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "TB", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "TC", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multiCatch2() {
        return "void multiCatch2() { " +
            "try { new Object(); } catch (@TA NullPointerException | @TB IndexOutOfBoundsException | @TC IllegalArgumentException e) { e.toString(); } }";
    }

    @TADescription(annotation = "TA", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "TB", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "TC", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    @TADescription(annotation = "TD", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    @TADescription(annotation = "TE", type = EXCEPTION_PARAMETER, exceptionIndex = 3)
    public String multiCatch3() {
        return "void multiCatch3() { " +
            "try { new Object(); } catch (NullPointerException e1) {}" +
            "try { new Object(); } catch (@TA @TB NullPointerException | @TC @TD IndexOutOfBoundsException | @TE IllegalArgumentException e2) { e2.toString(); } }";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "RTBs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    public String multiCatchRepeatableAnnotation1() {
        return "void multiCatch1() { " +
                "try { new Object(); } catch (@RTA @RTA NullPointerException |" +
                " @RTB @RTB IndexOutOfBoundsException e) { e.toString(); } }";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 0)
    @TADescription(annotation = "RTBs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "RTCs", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    public String multiCatchRepeatableAnnotation2() {
        return "void multiCatch2() { " +
                "try { new Object(); } catch (@RTA @RTA NullPointerException |" +
                " @RTB @RTB IndexOutOfBoundsException | @RTC @RTC IllegalArgumentException e) { e.toString(); } }";
    }

    @TADescription(annotation = "RTAs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "RTBs", type = EXCEPTION_PARAMETER, exceptionIndex = 1)
    @TADescription(annotation = "RTCs", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    @TADescription(annotation = "RTDs", type = EXCEPTION_PARAMETER, exceptionIndex = 2)
    @TADescription(annotation = "RTEs", type = EXCEPTION_PARAMETER, exceptionIndex = 3)
    public String multiCatchRepeatableAnnotation3() {
        return "void multiCatch3() { " +
                "try { new Object(); } catch (NullPointerException e1) {}" +
                "try { new Object(); } catch (@RTA @RTA @RTB @RTB NullPointerException |" +
                " @RTC @RTC @RTD @RTD IndexOutOfBoundsException |" +
                " @RTE @RTE IllegalArgumentException e2) { e2.toString(); } }";
    }
}
