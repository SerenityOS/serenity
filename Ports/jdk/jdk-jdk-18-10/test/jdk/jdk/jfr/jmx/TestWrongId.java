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

import java.util.HashMap;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;


/**
 * @test
 * @key jfr
 * @summary Call functions with invalid argument id. Verify Exception.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestWrongId
 */
public class TestWrongId {
    public static void main(String[] args) throws Throwable {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        final int id = 123456;
        verifyIllegalArg(() ->  bean.startRecording(id), "startRecording(invalidId)");
        verifyIllegalArg(() ->  bean.stopRecording(id), "stopRecording(invalidId)");
        verifyIllegalArg(() ->  bean.closeRecording(id), "destroyRecording(invalidId)");
        verifyIllegalArg(() ->  bean.openStream(id, null), "openStream(invalidId)");
        verifyIllegalArg(() ->  bean.closeStream(id), "closeStream(invalidId)");
        verifyIllegalArg(() ->  bean.readStream(id), "readStream(invalidId)");
        verifyIllegalArg(() ->  bean.getRecordingSettings(id), "getRecordingSettings(invalidId)");
        verifyIllegalArg(() ->  bean.setConfiguration(id, "dummy"), "setConfiguration(invalidId)");
        verifyIllegalArg(() ->  bean.setPredefinedConfiguration(id, "dummy"), "setNamedConfiguration(invalidId)");
        verifyIllegalArg(() ->  bean.setRecordingSettings(id, new HashMap<String, String>()), "setRecordingSettings(invalidId)");
        verifyIllegalArg(() ->  bean.copyTo(id, "./dummy.jfr"), "dumpRecording(invalidId)");
    }

    private static void verifyIllegalArg(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalArgumentException.class);
    }

}
