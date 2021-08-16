/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8172751
 * @summary OSR compilation at unreachable bci causes C1 crash
 *
 * @run main/othervm -XX:-BackgroundCompilation compiler.c1.Test8172751
 */

package compiler.c1;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;

public class Test8172751 {
    private static final MethodHandle CONSTANT_TRUE = MethodHandles.constant(boolean.class, true);
    private static final MethodHandle CONSTANT_FALSE = MethodHandles.constant(boolean.class, false);
    private static final MutableCallSite CALL_SITE = new MutableCallSite(CONSTANT_FALSE);
    private static final int LIMIT = 1_000_000;
    private static volatile int counter;

    private static boolean doSomething() {
        return counter++ < LIMIT;
    }

    private static void executeLoop() {
        /*
         * Start off with executing the first loop, then change the call site
         * target so as to switch over to the second loop but continue running
         * in the first loop. Eventually, an OSR compilation of the first loop
         * is triggered. Yet C1 will not find the OSR entry, since it will
         * have optimized out the first loop already during parsing.
         */
        if (CALL_SITE.getTarget() == CONSTANT_FALSE) {
            int count = 0;
            while (doSomething()) {
                if (count++ == 1) {
                    flipSwitch();
                }
            }
        } else {
            while (doSomething()) {
            }
        }
    }

    private static void flipSwitch() {
        CALL_SITE.setTarget(CONSTANT_TRUE);
    }

    public static void main(String[] args) {
        executeLoop();
    }
}
