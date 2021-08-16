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
import java.util.ArrayList;

/*
 * Class for testing class exclusion filter, this filter is added by
 * request's method addClassExclusionFilter(String classPattern) Expected
 * command line parameter:
 * - class patterns which should be passed to adding filter method (e.g. -classPatterns java.*:*.Foo)
 */
public class ClassExclusionFilterTest extends EventFilterTest {
    protected String[] classPatterns;

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new ClassExclusionFilterTest().runIt(argv, out);
    }

    protected String[] doInit(String[] args, PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-classPatterns") && (i < args.length - 1)) {
                classPatterns = args[i + 1].split(":");
                i++;
            } else {
                standardArgs.add(args[i]);
            }
        }

        if (classPatterns == null || classPatterns.length == 0) {
            throw new TestBug("Test requires at least one class name pattern");
        }

        return standardArgs.toArray(new String[]{});
    }

    protected int getTestFiltersNumber() {
        return classPatterns.length;
    }

    protected EventFilters.DebugEventFilter[] createTestFilters(int testedFilterIndex) {
        if (testedFilterIndex < 0 || testedFilterIndex >= classPatterns.length) {
            throw new TestBug("Invalid testedFilterIndex: " + testedFilterIndex);
        }

        return new EventFilters.DebugEventFilter[]{new EventFilters.ClassExclusionFilter(classPatterns[testedFilterIndex])};
    }
}
