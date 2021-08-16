/*
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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
 * @bug 8150646 8153013
 * @summary Add support for blocking compiles through whitebox API
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @requires vm.compiler1.enabled | !vm.graal.enabled
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *        -Xbootclasspath/a:.
 *        -Xmixed
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -XX:+PrintCompilation
 *        compiler.whitebox.BlockingCompilation
 */

package compiler.whitebox;

import compiler.testlibrary.CompilerUtils;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class BlockingCompilation {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static int foo() {
        return 42; //constant's value is arbitrary and meaningless
    }

    public static void main(String[] args) throws Exception {
        Method m = BlockingCompilation.class.getMethod("foo");
        int[] levels = CompilerUtils.getAvailableCompilationLevels();
        int highest_level = levels[levels.length-1];

        // If there are no compilers available these tests don't make any sense.
        if (levels.length == 0) return;

        // Make sure no compilations can progress, blocking compiles will hang
        WB.lockCompilation();

        // Verify method state before test
        if (WB.isMethodCompiled(m)) {
            throw new Exception("Should not be compiled after deoptimization");
        }
        if (WB.isMethodQueuedForCompilation(m)) {
            throw new Exception("Should not be enqueued on any level");
        }

        // Try compiling on highest available comp level.
        // If the compiles are blocking, this call will block until the test time out,
        // Progress == success
        // (Don't run with -Xcomp since that can cause long timeouts due to many compiles)
        if (!WB.enqueueMethodForCompilation(m, highest_level)) {
            throw new Exception("Failed to enqueue method on level: " + highest_level);
        }

        if (!WB.isMethodQueuedForCompilation(m)) {
            throw new Exception("Must be enqueued because of locked compilation");
        }

        // restore state
        WB.unlockCompilation();
        while (!WB.isMethodCompiled(m)) {
          Thread.sleep(100);
        }
        WB.deoptimizeMethod(m);
        WB.clearMethodState(m);

        // Blocking compilations on all levels, using the default versions of
        // WB.enqueueMethodForCompilation() and manually setting compiler directives.
        String directive = "[{ match: \""
                + BlockingCompilation.class.getName().replace('.', '/')
                + ".foo\", BackgroundCompilation: false }]";
        if (WB.addCompilerDirective(directive) != 1) {
            throw new Exception("Failed to add compiler directive");
        }

        try {
            for (int l : levels) {
                // Make uncompiled
                WB.deoptimizeMethod(m);

                // Verify that it's not compiled
                if (WB.isMethodCompiled(m)) {
                    throw new Exception("Should not be compiled after deoptimization");
                }
                if (WB.isMethodQueuedForCompilation(m)) {
                    throw new Exception("Should not be enqueued on any level");
                }

                // Add to queue and verify that it went well
                if (!WB.enqueueMethodForCompilation(m, l)) {
                    throw new Exception("Could not be enqueued for compilation");
                }

                // Verify that it is compiled
                if (!WB.isMethodCompiled(m)) {
                    throw new Exception("Must be compiled here");
                }
                // And verify the level
                if (WB.getMethodCompilationLevel(m) != l) {
                    String msg = m + " should be compiled at level " + l +
                                 "(but is actually compiled at level " +
                                 WB.getMethodCompilationLevel(m) + ")";
                    System.out.println("==> " + msg);
                    throw new Exception(msg);
                }
            }
        } finally {
            WB.removeCompilerDirective(1);
        }

        // Clean up
        WB.deoptimizeMethod(m);
        WB.clearMethodState(m);

        // Make sure no compilations can progress, blocking compiles will hang
        WB.lockCompilation();

        // Verify method state before test
        if (WB.isMethodCompiled(m)) {
            throw new Exception("Should not be compiled after deoptimization");
        }
        if (WB.isMethodQueuedForCompilation(m)) {
            throw new Exception("Should not be enqueued on any level");
        }

        // Try compiling on highest available comp level.
        // If the compiles are blocking, this call will block until the test time out,
        // Progress == success
        // (Don't run with -Xcomp since that can cause long timeouts due to many compiles)
        WB.enqueueMethodForCompilation(m, highest_level);

        // restore state
        WB.unlockCompilation();
    }
}
