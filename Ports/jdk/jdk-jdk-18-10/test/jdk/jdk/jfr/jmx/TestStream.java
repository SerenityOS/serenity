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

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.consumer.RecordedEvent;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestStream
 */
public class TestStream {
    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        Instant startTime = Instant.now();
        SimpleEventHelper.createEvent(0);

        long recId = bean.newRecording();
        bean.startRecording(recId);
        SimpleEventHelper.createEvent(1);

        bean.stopRecording(recId);
        SimpleEventHelper.createEvent(2);

        Instant endTime = Instant.now();
        // Test with ISO-8601
        Map<String, String> options = new HashMap<>();
        options.put("startTime", startTime.toString());
        options.put("endTime", endTime.toString());
        options.put("blockSize", String.valueOf(50_000));
        verifyStream(bean, recId, options);
        // Test with milliseconds since epoch
        options.put("startTime", Long.toString(startTime.toEpochMilli()));
        options.put("endTime", Long.toString(endTime.toEpochMilli()));
        options.put("blockSize", String.valueOf(150_000));
        verifyStream(bean, recId, options);

        bean.closeRecording(recId);
    }

    private static void verifyStream(FlightRecorderMXBean bean, long recId, Map<String, String> options) throws IOException, Exception {
        long streamId = bean.openStream(recId, options);

        List<RecordedEvent> events = JmxHelper.parseStream(streamId, bean);
        SimpleEventHelper.verifyContains(events, 1);
        SimpleEventHelper.verifyNotContains(events, 0, 2);
        bean.closeStream(streamId);
    }
}
