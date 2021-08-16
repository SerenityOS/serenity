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

import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Tests that local streaming works using only
 *          FlightRecordingPermission("accessFlightRecorder")
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm/secure=java.lang.SecurityManager/java.security.policy=local-streaming.policy
 *      jdk.jfr.api.consumer.security.TestStreamingLocal
 */
public class TestStreamingLocal {

    public static void main(String... args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (Recording r = new Recording()) {
            // Enable JVM event, no write permission needed
            r.enable(EventNames.JVMInformation);
            r.start();
            try (Recording r2 = new Recording()){
                r2.start();
                r2.stop();
            }
            r.stop();
            try (EventStream es = EventStream.openRepository()) {
                es.setStartTime(Instant.EPOCH);
                es.onEvent("jdk.JVMInformation", e -> {
                    latch.countDown();
                });
                es.startAsync();
                latch.await();
            }
        }

    }
}
