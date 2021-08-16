/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jvm;

import static jdk.test.lib.Asserts.assertGreaterThan;

import jdk.jfr.internal.JVM;

/**
 * @test TestCounterTime
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jvm.TestCounterTime
 */
public class TestCounterTime {

    public static void main(String... args) throws InterruptedException {
        // Not enabled
        assertCounterTime();

        JVM jvm = JVM.getJVM();
        jvm.createNativeJFR();
        assertCounterTime();
        // Enabled
        jvm.destroyNativeJFR();
    }

    private static void assertCounterTime() throws InterruptedException {
        long time1 = JVM.counterTime();
        assertGreaterThan(time1, 0L, "Counter time can't be negative.");

        Thread.sleep(1);

        long time2 = JVM.counterTime();
        assertGreaterThan(time2, time1, "Counter time must be increasing.");
    }
}
