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

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test start/stop/close recording from different recording states.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.state.TestStateInvalid
 */
public class TestStateInvalid {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        CommonHelper.verifyRecordingState(r, RecordingState.NEW);
        verifyIllegalState(() -> r.stop(), "stop() when not started");
        CommonHelper.verifyRecordingState(r, RecordingState.NEW);

        r.start();
        CommonHelper.verifyRecordingState(r, RecordingState.RUNNING);
        verifyIllegalState(() -> r.start(), "double start()");
        CommonHelper.verifyRecordingState(r, RecordingState.RUNNING);

        r.stop();
        CommonHelper.verifyRecordingState(r, RecordingState.STOPPED);
        verifyIllegalState(() -> r.stop(), "double stop()");
        verifyIllegalState(() -> r.start(), "start() after stop()");
        CommonHelper.verifyRecordingState(r, RecordingState.STOPPED);

        r.close();
        CommonHelper.verifyRecordingState(r, RecordingState.CLOSED);
        verifyIllegalState(() -> r.stop(), "stop() after close()");
        verifyIllegalState(() -> r.start(), "start() after close()");
        CommonHelper.verifyRecordingState(r, RecordingState.CLOSED);
    }

    private static void verifyIllegalState(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalStateException.class);
    }
}
