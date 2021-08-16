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

package jdk.jfr.api.recording.time;

import java.time.Duration;
import java.time.Instant;

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Test Recording.setDuration() and Recording.get*Time()
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.time.TestTimeDuration
 */

public class TestTimeDuration {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        final Duration duration = Duration.ofMillis(30000);

        r.setDuration(duration);
        Asserts.assertNull(r.getStartTime(), "getStartTime() not null before start");
        Asserts.assertNull(r.getStopTime(), "getStopTime() not null before start");

        final Instant beforeStart = Instant.now();
        r.start();
        final Instant afterStart = Instant.now();

        Asserts.assertNotNull(r.getStartTime(), "getStartTime() null after start()");
        Asserts.assertGreaterThanOrEqual(r.getStartTime(), beforeStart, "getStartTime() < beforeStart");
        Asserts.assertLessThanOrEqual(r.getStartTime(), afterStart, "getStartTime() > afterStart");

        Asserts.assertNotNull(r.getStopTime(), "getStopTime() null after start with duration");
        Asserts.assertGreaterThanOrEqual(r.getStopTime(), beforeStart.plus(duration), "getStopTime() < beforeStart + duration");
        Asserts.assertLessThanOrEqual(r.getStopTime(), afterStart.plus(duration), "getStopTime() > afterStart + duration");

        r.close();
    }

}
