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

package jdk.jfr.api.recording.state;

import java.time.Duration;
import java.util.function.Consumer;

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Test options in different states
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.state.TestOptionState
 */
public class TestOptionState {

     //                                Name   Age    Size   Dur.   Dest.  Disk
    static boolean[] NEW =     arrayOf(true,  true,  true,  true,  true,  true);
    static boolean[] DELAYED = arrayOf(true,  true,  true,  true,  true,  true);
    static boolean[] RUNNING = arrayOf(true,  true,  true,  true,  true,  false);
    static boolean[] STOPPED = arrayOf(true,  true,  true,  false,  false, false);
    static boolean[] CLOSED =  arrayOf(false, false, false, false, false, false);

    public static void main(String[] args) throws Throwable {
      Recording r = new Recording();
      assertRecordingState(r, NEW);
      r.scheduleStart(Duration.ofHours(2));
      assertRecordingState(r, DELAYED);
      r.start();
      assertRecordingState(r, RUNNING);
      r.stop();
      assertRecordingState(r, STOPPED);
      r.close();
      assertRecordingState(r, CLOSED);
    }

    private static void assertRecordingState(Recording r, boolean[] states) throws Exception {
        assertOperation("setName", r, s -> s.setName("Test Name"), states[0]);
        assertOperation("setMaxAge", r, s -> s.setMaxAge(null), states[1]);
        assertOperation("setMaxSize", r, s -> s.setMaxSize(0), states[2]);
        assertOperation("setDuration", r, s -> s.setDuration(null), states[3]);
        assertOperation("setDestination", r, s -> {
            try {
            s.setDestination(null);
        } catch (IllegalStateException e) {
            throw e; // rethrow for testing
        } catch(Exception e) {
            throw new RuntimeException(e); // should not happen
        }}, states[4]);
        assertOperation("setTodisk", r, s -> s.setToDisk(true), states[5]);
    }

    private static void assertOperation(String opernationName, Recording s, Consumer<Recording> c, boolean ok) {
        try {
            c.accept(s);
            Asserts.assertTrue(ok, opernationName + " should throw ISE when recording is in state " + s.getState());
        } catch (IllegalStateException ise) {
            Asserts.assertFalse(ok, opernationName + " should not throw ISE when recording is in state " + s.getState());
        }
    }

    static boolean[] arrayOf(boolean... array) {
        return array;
    }
}
