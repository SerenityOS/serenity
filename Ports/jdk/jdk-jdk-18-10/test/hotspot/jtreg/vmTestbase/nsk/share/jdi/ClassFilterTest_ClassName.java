/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import nsk.share.Consts;
import nsk.share.TestBug;

import java.io.PrintStream;

/*
 * Subclass of ClassExclusionFilterTest, create ClassFilter instead of
 * ClassExclusionFilter, this filter is added by request's method
 * addClassFilter(String classPattern)
 */
public class ClassFilterTest_ClassName extends ClassExclusionFilterTest {
    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new ClassFilterTest_ClassName().runIt(argv, out);
    }

    protected EventFilters.DebugEventFilter[] createTestFilters(int testedFilterIndex) {
        if (testedFilterIndex < 0 || testedFilterIndex >= classPatterns.length) {
            throw new TestBug("Invalid testedFilterIndex: " + testedFilterIndex);
        }

        return new EventFilters.DebugEventFilter[]{new EventFilters.ClassFilter(classPatterns[testedFilterIndex])};
    }
}
