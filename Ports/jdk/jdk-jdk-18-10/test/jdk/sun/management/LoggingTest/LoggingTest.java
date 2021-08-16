/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class LoggingTest {

    static class TestStream extends PrintStream {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private volatile boolean recording;
        public TestStream(PrintStream wrapped) {
            super(wrapped);
        }

        void startRecording() {
            recording = true;
        }

        void stopRecording() {
            recording = false;
        }

        @Override
        public void write(int b) {
            if (recording) {
                bos.write(b);
            }
            super.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            if (recording) {
                bos.write(buf, off, len);
            }
            super.write(buf, off, len);
        }

        @Override
        public void write(byte[] buf) throws IOException {
            if (recording) {
                bos.write(buf);
            }
            super.write(buf);
        }

    }

    public void run(TestStream ts) {

        // start recording traces and trigger creation of the platform
        // MBeanServer to produce some. This won't work if the platform
        // MBeanServer was already initialized - so it's important to
        // run this test in its own JVM.
        ts.startRecording();
        MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        ts.stopRecording();
        String printed = ts.bos.toString();
        ts.bos.reset();

        // Check that the Platform MBeanServer is emitting the expected
        // log traces. This can be a bit fragile because debug traces
        // could be changed without notice - in which case this test will
        // need to be updated.
        // For each registered MBean we expect to see three traces.
        // If the messages logged by the MBeanServer change then these checks
        // may need to be revisited.
        List<String> checkTraces =
                List.of("ObjectName = %s", "name = %s", "JMX.mbean.registered %s");

        for (ObjectName o : platform.queryNames(ObjectName.WILDCARD, null)) {
            String n = o.toString();
            System.out.println("Checking log for: " + n);
            for (String check : checkTraces) {
                String s = String.format(check, n);
                if (!printed.contains(s)) {
                    throw new RuntimeException("Trace not found: " + s);
                }
            }
        }
    }

}
