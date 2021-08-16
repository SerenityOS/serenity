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
 * @summary Test Recording state with concurrent recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.state.TestStateMultiple
 */
public class TestStateMultiple {

    public static void main(String[] args) throws Throwable {
        Recording rA = new Recording();
        CommonHelper.verifyRecordingState(rA, RecordingState.NEW);
        verifyIllegalState(() -> rA.stop(), "stop() when not started");

        rA.start();
        CommonHelper.verifyRecordingState(rA, RecordingState.RUNNING);

        Recording rB = new Recording();
        CommonHelper.verifyRecordingState(rA, RecordingState.RUNNING);
        verifyIllegalState(() -> rA.start(), "double start()");
        CommonHelper.verifyRecordingState(rB, RecordingState.NEW);
        verifyIllegalState(() -> rB.stop(), "stop() when not started");

        rB.start();
        CommonHelper.verifyRecordingState(rA, RecordingState.RUNNING);
        CommonHelper.verifyRecordingState(rB, RecordingState.RUNNING);

        rB.stop();
        CommonHelper.verifyRecordingState(rA, RecordingState.RUNNING);
        CommonHelper.verifyRecordingState(rB, RecordingState.STOPPED);
        verifyIllegalState(() -> rB.start(), "start() after stop()");

        rB.close();
        CommonHelper.verifyRecordingState(rA, RecordingState.RUNNING);
        CommonHelper.verifyRecordingState(rB, RecordingState.CLOSED);
        verifyIllegalState(() -> rB.start(), "start() after close()");

        rA.stop();
        CommonHelper.verifyRecordingState(rA, RecordingState.STOPPED);
        verifyIllegalState(() -> rA.start(), "start() after stop()");
        CommonHelper.verifyRecordingState(rB, RecordingState.CLOSED);

        rA.close();
        CommonHelper.verifyRecordingState(rA, RecordingState.CLOSED);
        CommonHelper.verifyRecordingState(rB, RecordingState.CLOSED);
        verifyIllegalState(() -> rA.stop(), "stop() after close()");
        verifyIllegalState(() -> rB.start(), "start() after close()");
    }

    private static void verifyIllegalState(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalStateException.class);
    }
}
