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
 * @summary implicit null check on brooks pointer must not cause crash
 * @requires vm.gc.Shenandoah
 * @requires vm.bits == "64"
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:-TieredCompilation
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC
 *                   -Xmx4G -XX:HeapBaseMinAddress=0x800000000 TestNullCheck
 */

// HeapBaseMinAddress above forces compressed oops with a base

public class TestNullCheck {

    int f;

    static int test1(TestNullCheck o) {
        return o.f;
    }

    static TestNullCheck static_obj = new TestNullCheck();

    static int test2() {
        return static_obj.f;
    }

    static public void main(String[] args) {
        TestNullCheck o = new TestNullCheck();
        for (int i = 0; i < 20000; i++) {
            test1(o);
            test2();
        }
        try {
            test1(null);
        } catch (NullPointerException npe) {}
        static_obj = null;
        try {
            test2();
        } catch (NullPointerException npe) {}
    }
}
