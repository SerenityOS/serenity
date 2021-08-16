/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @bug 8212673
 * @summary Node::eqv_uncast() shouldn't step over load barriers unconditionally
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-UseOnStackReplacement -XX:-TieredCompilation -XX:-BackgroundCompilation EqvUncastStepOverBarrier
 */

import sun.hotspot.WhiteBox;
import java.lang.reflect.Method;

public class EqvUncastStepOverBarrier {
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private static Object field = new A();

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20_000; i++) {
            test();
            test();
            test_helper(null, 0);
        }
        Method m = EqvUncastStepOverBarrier.class.getDeclaredMethod("test");
        WHITE_BOX.enqueueMethodForCompilation(m, 4);
        if (!WHITE_BOX.isMethodCompiled(m, false)) {
            throw new RuntimeException("Method compilation failed");
        }
    }

    private static Object test() {
        Object o = field;
        if (o == null) {}
        for (int i = 1; i < 100; i *= 2) {
            int j = 0;
            for (; j < 4; j++) ;
            o = test_helper(o, j);
        }
        return o;
    }

    private static Object test_helper(Object o, int j) {
        if (j == 4) {
            A a = (A) o;
            o = a;
        } else {
            o = new Object();
        }
        return o;
    }

    private static class A {
    }
}
