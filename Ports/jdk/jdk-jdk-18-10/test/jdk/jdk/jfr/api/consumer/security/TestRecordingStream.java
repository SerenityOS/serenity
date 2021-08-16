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

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.consumer.RecordingStream;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Tests that a RecordingStream works using only
 *          FlightRecordingPermission("accessFlightRecorder")
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm/secure=java.lang.SecurityManager/java.security.policy=local-streaming.policy
 *      jdk.jfr.api.consumer.security.TestStreamingLocal
 */
public class TestRecordingStream {

    public static void main(String... args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            // Enable JVM event, no write permission needed
            r.enable(EventNames.JVMInformation);
            r.setStartTime(Instant.EPOCH);
            r.onEvent(EventNames.JVMInformation, e -> {
                latch.countDown();
            });
            r.startAsync();
            latch.await();
        }
    }
}
