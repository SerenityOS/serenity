/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.loopopts;

import java.lang.reflect.Method;
import sun.hotspot.WhiteBox;
import jdk.test.lib.Asserts;
import compiler.whitebox.CompilerWhiteBoxTest;

public class UseCountedLoopSafepoints {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final String METHOD_NAME = "testMethod";

    private long accum = 0;

    public static void main (String args[]) throws Exception {
        new UseCountedLoopSafepoints().testMethod();
        Method m = UseCountedLoopSafepoints.class.getDeclaredMethod(METHOD_NAME);
        String directive = "[{ match: \"" + UseCountedLoopSafepoints.class.getName().replace('.', '/')
                + "." + METHOD_NAME + "\", " + "BackgroundCompilation: false }]";
        Asserts.assertTrue(WB.addCompilerDirective(directive) == 1, "Can't add compiler directive");
        Asserts.assertTrue(WB.enqueueMethodForCompilation(m,
                CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION), "Can't enqueue method");
    }

    private void testMethod() {
        for (int i = 0; i < 100; i++) {
            accum += accum << 5 + accum >> 4 - accum >>> 5;
        }
    }
}
