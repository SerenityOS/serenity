/*
 * Copyright (c) 2016, 2018, Red Hat, Inc. All rights reserved.
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
 * @key stress randomness
 * @summary Clearing control during final graph reshape causes memory barrier to loose dependency on null check
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server"
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:-TieredCompilation
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressLCM -XX:+StressGCM
 *                   TestWriteBarrierClearControl
 *
 */
public class TestWriteBarrierClearControl {

    int f;

    static void test1(TestWriteBarrierClearControl o) {
        o.f = 0x42;
    }

    static TestWriteBarrierClearControl fo = new TestWriteBarrierClearControl();

    static void test2() {
        TestWriteBarrierClearControl o = fo;
        o.f = 0x42;
    }

    static public void main(String[] args) {
        TestWriteBarrierClearControl o = new TestWriteBarrierClearControl();
        for (int i = 0; i < 20000; i++) {
            test1(o);
            test2();
        }
        try {
            test1(null);
        } catch (NullPointerException npe) {}
        fo = null;
        try {
            test2();
        } catch (NullPointerException npe) {}
    }
}
