/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer.security;

import java.io.IOException;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests that streaming doesn't work if
 *          FlightRecordingPermission("accessFlightRecorder") is missing
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm/secure=java.lang.SecurityManager/java.security.policy=no-permission.policy
 *      jdk.jfr.api.consumer.security.TestMissingPermission
 */
public class TestMissingPermission {

    public static void main(String... args) throws Exception {
        testOpenRepository();
        testRecordingStream();
    }

    private static void testRecordingStream() throws IOException {
        try {
            try (EventStream es = EventStream.openRepository()) {
                throw new AssertionError("Should not be able to create EventStream without FlightRecorderPermission");
            }
        } catch (SecurityException se) {
            // OK, as expected
        }
    }

    private static void testOpenRepository() throws IOException {
        try {
            try (RecordingStream es = new RecordingStream()) {
                throw new AssertionError("Should not be able to create RecordingStream without FlightRecorderPermission");
            }
        } catch (SecurityException se) {
            // OK, as expected
        }
    }
}
