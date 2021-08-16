/*
 * Copyright (c) 2016 SAP SE. All rights reserved.
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
 * @bug 8159620
 * @summary testing that -XX:-UseOnStackReplacement works with both -XX:(+/-)TieredCompilation
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:+PrintCompilation
 *                   -XX:-BackgroundCompilation -XX:-TieredCompilation -XX:-UseOnStackReplacement
 *                   compiler.interpreter.DisableOSRTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:+PrintCompilation
 *                   -XX:-BackgroundCompilation -XX:+TieredCompilation -XX:-UseOnStackReplacement
 *                   compiler.interpreter.DisableOSRTest
 */

package compiler.interpreter;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.Random;

public class DisableOSRTest {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final Random RANDOM = new Random(42);

    public static int foo() {
        return RANDOM.nextInt();
    }

    public static void main(String[] args) throws Exception {
        Method m = DisableOSRTest.class.getMethod("main", String[].class);

        for (int i = 0; i < 100_000; i++) {
            foo();
        }

        if (WB.isMethodCompiled(m, true /* isOsr */)) {
            throw new RuntimeException("\"" + m + "\" shouldn't be OSR compiled if running with -XX:-UseOnStackReplacement!");
        }
    }
}
