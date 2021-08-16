/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -Djdk.attach.allowAttachSelf=true -Dcom.sun.management.jmxremote jdk.jfr.jmx.TestGetRecordings
 */
public class TestGetRecordings {
    public static void main(String[] args) throws Throwable {
        FlightRecorderMXBean bean =JmxHelper.getFlighteRecorderMXBean();

        List<RecordingInfo> preCreateRecordings = bean.getRecordings();
        long recId = bean.newRecording();
        JmxHelper.verifyNotExists(recId, preCreateRecordings);
        bean.closeRecording(recId);
        JmxHelper.verifyNotExists(recId, bean.getRecordings());

        long selfPID = JmxHelper.getPID();
        FlightRecorderMXBean remoteBean = JmxHelper.getFlighteRecorderMXBean(selfPID);
        long remoteRecId = remoteBean.newRecording();
        remoteBean.getRecordings();
        remoteBean.closeRecording(remoteRecId);
    }
}
