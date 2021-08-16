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

package jdk.jfr.api.consumer;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertNotNull;
import static jdk.test.lib.Asserts.assertNull;
import static jdk.test.lib.Asserts.assertTrue;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary Verifies that a recorded JFR event has the correct stack trace info
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestGetStackTrace
 */
public class TestGetStackTrace {

    public static void main(String[] args) throws Throwable {
        testWithoutStackTrace(recordEvent("stackTrace", "false"));
        testWithStackTrace(recordEvent("stackTrace", "true"));
    }

    private static RecordedEvent recordEvent(String key, String value) throws Throwable {
        try (Recording r = new Recording()) {
            r.enable(SimpleEvent.class).with(key, value);
            r.start();
            SimpleEvent event = new SimpleEvent();
            event.commit();
            r.stop();
            return Events.fromRecording(r).get(0);
        }
    }

    private static void testWithoutStackTrace(RecordedEvent re) {
        assertNull(re.getStackTrace());
    }

    private static void testWithStackTrace(RecordedEvent re) {
        assertNotNull(re.getStackTrace());
        RecordedStackTrace strace = re.getStackTrace();
        assertEquals(strace.isTruncated(), false);
        List<RecordedFrame> frames = strace.getFrames();
        assertTrue(frames.size() > 0);
        for (RecordedFrame frame : frames) {
            assertFrame(frame);
        }
    }

    private static void assertFrame(RecordedFrame frame) {
        int bci = frame.getBytecodeIndex();
        int line = frame.getLineNumber();
        boolean javaFrame = frame.isJavaFrame();
        RecordedMethod method = frame.getMethod();
        String type = frame.getType();
        System.out.println("*** Frame Info ***");
        System.out.println("bci=" + bci);
        System.out.println("line=" + line);
        System.out.println("type=" + type);
        System.out.println("method=" + method);
        System.out.println("***");
        Asserts.assertTrue(javaFrame, "Only Java frame are currently supported");
        Asserts.assertGreaterThanOrEqual(bci, -1);
        Asserts.assertNotNull(method, "Method should not be null");
    }
}
