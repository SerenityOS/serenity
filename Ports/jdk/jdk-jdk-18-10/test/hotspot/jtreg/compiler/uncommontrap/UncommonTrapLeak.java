/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;

/*
 * @test
 * @bug 8260473
 * @summary Handle leak might cause object not collected as expected
 *
 * @run main/othervm -XX:-Inline -XX:-TieredCompilation -XX:CompileCommand=compileonly,UncommonTrapLeak.foo
 *                   -XX:CompileThreshold=100 -XX:-BackgroundCompilation UncommonTrapLeak
 *
 * @author Hui Shi
 */
public class UncommonTrapLeak {
    static WeakReference<Object> ref = null;
    static int val = 0;
    public static void main(String args[]) {
        for (int i = 0; i < 300; i++) {
            val++;
            foo(i);
            System.gc();
            if (ref.get() != null) {
                throw new RuntimeException("Failed: referent not collected after trap " + ref.get());
            }
            if (i % 100 == 0) {
                System.out.println(i);
            }
        }
    }

    static void foo(int i) {
        Object o = new Object();
        ref = new WeakReference<Object>(o);
        if (val == 200) {
            // trigger Deoptimization::uncommon_trap
            if (o instanceof UncommonTrapLeak) {
            }
        }
    }
}

