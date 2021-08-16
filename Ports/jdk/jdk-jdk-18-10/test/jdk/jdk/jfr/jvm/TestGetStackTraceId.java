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

import jdk.jfr.FlightRecorder;
import jdk.jfr.internal.JVM;
import jdk.test.lib.Asserts;

/**
 * @test TestGetStackTraceId
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jvm.TestGetStackTraceId
 */
public class TestGetStackTraceId {

    public static void main(String... args) {
        FlightRecorder.getFlightRecorder();
        JVM jvm = JVM.getJVM();

        long id10 = getStackIdOfDepth(10);
        assertValid(id10);

        long id5 = getStackIdOfDepth(5);
        assertValid(id5);

        Asserts.assertNotEquals(id5, id10, "Different stack depth must return different stack trace ids");

        assertMaxSkip(jvm);
    }

    private static void assertMaxSkip(JVM jvm) {
        assertValid(jvm.getStackTraceId(Integer.MAX_VALUE));
    }

    private static void assertValid(long value) {
        Asserts.assertGreaterThan(value, 0L, "Stack trace id must be greater than 0, was " + value);
    }

    public static long getStackIdOfDepth(int depth) {
        if (depth > 0) {
            return getStackIdOfDepth(depth - 1);
        }
        return JVM.getJVM().getStackTraceId(0);
    }
}
