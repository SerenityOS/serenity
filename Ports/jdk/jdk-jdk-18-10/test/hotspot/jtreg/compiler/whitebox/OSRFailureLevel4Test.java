/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test OSRFailureLevel4Test
 * @summary check that not compilable OSR level 4 results in falling back to level 1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @comment the test can't be run w/ TieredStopAtLevel < 4
 * @requires vm.flavor == "server" & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4)
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+TieredCompilation compiler.whitebox.OSRFailureLevel4Test
 */

package compiler.whitebox;

import sun.hotspot.WhiteBox;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import jtreg.SkippedException;

public class OSRFailureLevel4Test extends Thread {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final long BACKEDGE_THRESHOLD = 150000;
    private Method method;

    public static void main(String[] args) throws Exception {
        if (CompilerWhiteBoxTest.skipOnTieredCompilation(false)) {
            throw new SkippedException("Test isn't applicable for non-tiered mode");
        }
        OSRFailureLevel4Test test = new OSRFailureLevel4Test();
        test.test();
    }

    /**
     * Triggers two different OSR compilations for the same method and
     * checks if WhiteBox.deoptimizeMethod() deoptimizes both.
     *
     * @throws Exception
     */
    public void test() throws Exception {
        method = OSRFailureLevel4Test.class.getDeclaredMethod("run");
        WHITE_BOX.makeMethodNotCompilable(method, 4, true);

        Thread t = new OSRFailureLevel4Test();
        t.setDaemon(true);
        t.start();

        int currentLevel = 0;
        int loops = 0;
        while (true) {
            int level = WHITE_BOX.getMethodCompilationLevel(method, true);
            if (level == 1) {
                System.err.println("success");
                running = false;
                return;
            }
            if (level == currentLevel) {
                Thread.sleep(1000);
                loops++;
                if (loops > 100) {
                    running = false;
                    throw new AssertionError("Never reached level 1");
                }
                continue;
            }
            currentLevel = level;
            System.err.println("Current level = " + currentLevel);
        }

    }

    static volatile int counter = 0;
    static boolean running = true;

    public void run() {
        while (running) {
            counter++;
        }
    }
}
