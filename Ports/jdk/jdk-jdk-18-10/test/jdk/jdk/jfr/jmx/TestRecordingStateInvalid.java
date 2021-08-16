/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx;

import java.util.List;

import jdk.jfr.RecordingState;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestRecordingStateInvalid
 */
public class TestRecordingStateInvalid {
    public static void main(String[] args) throws Throwable {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        long recId = createRecording(bean);
        verifyIllegalState(()->{ bean.stopRecording(recId); }, "Stop not started");

        startRecording(recId, bean);
        verifyIllegalState(()->{ bean.startRecording(recId); }, "Start already started");

        stopRecording(recId, bean);
        verifyIllegalState(()->{ bean.startRecording(recId); }, "Start already stopped");
        verifyIllegalState(()->{ bean.stopRecording(recId); }, "Stop already stopped");

        destroyRecording(recId, bean);
        verifyIllegalArg(()->{ bean.startRecording(recId); }, "Start already destroyed");
        verifyIllegalArg(()->{ bean.stopRecording(recId); }, "Stop already destroyed");

    }

    private static long createRecording(FlightRecorderMXBean bean) throws Exception {
        List<RecordingInfo> preCreateRecordings = bean.getRecordings();
        long recId = bean.newRecording();
        JmxHelper.verifyNotExists(recId, preCreateRecordings);
        JmxHelper.verifyState(recId, RecordingState.NEW, bean);
        return recId;
    }

    private static void startRecording(long recId, FlightRecorderMXBean bean) throws Exception {
        JmxHelper.verifyState(recId, RecordingState.NEW, bean);
        bean.startRecording(recId);
        JmxHelper.verifyState(recId, RecordingState.RUNNING, bean);
    }

    private static void stopRecording(long recId, FlightRecorderMXBean bean) throws Exception {
        JmxHelper.verifyState(recId, RecordingState.RUNNING, bean);
        bean.stopRecording(recId);
        JmxHelper.verifyState(recId, RecordingState.STOPPED, bean);
    }

    private static void destroyRecording(long recId, FlightRecorderMXBean bean) throws Exception {
        JmxHelper.verifyState(recId, RecordingState.STOPPED, bean);
        bean.closeRecording(recId);
        JmxHelper.verifyNotExists(recId, bean.getRecordings());
    }

    private static void verifyIllegalState(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalStateException.class);
    }

    private static void verifyIllegalArg(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalArgumentException.class);
    }

}
