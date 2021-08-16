/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx.streaming;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Test constructors of RemoteRecordingStream
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.streaming.TestNew
 */
public class TestNew {

    private final static ObjectName MXBEAN = createObjectName();

    public static void main(String... args) throws Exception {
        testNullArguments();
        testMissingDirectory();
        testNotDirectory();
        testDefaultDIrectory();
        TestUserDefinedDirectory();

        testMissingFlightRecorderMXBean();
    }

    private static void TestUserDefinedDirectory() throws IOException {
        Path p = Paths.get("user-repository-" + System.currentTimeMillis());
        Files.createDirectory(p);
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (RemoteRecordingStream s = new RemoteRecordingStream(conn, p)) {
            // success
        }
    }

    private static void testDefaultDIrectory() throws IOException {
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (RemoteRecordingStream s = new RemoteRecordingStream(conn)) {
            // success
        }
    }

    private static void testNotDirectory() throws Exception {
        Path p = Paths.get("file.txt");
        RandomAccessFile raf = new RandomAccessFile(p.toFile(), "rw");
        raf.close();
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (var s = new RemoteRecordingStream(conn, p)) {
            throw new Exception("Expected IOException");
        } catch (IOException ioe) {
            if (!ioe.getMessage().contains("Download location must be a directory")) {
                throw new Exception("Unexpected message " + ioe.getMessage());
            }
        }
    }

    private static void testMissingDirectory() throws Exception {
        Path p = Paths.get("/missing");
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (var s = new RemoteRecordingStream(conn, p)) {
            throw new Exception("Expected IOException");
        } catch (IOException ioe) {
            if (!ioe.getMessage().contains("Download directory doesn't exist")) {
                throw new Exception("Unexpected message " + ioe.getMessage());
            }
        }
    }

    private static void testNullArguments() throws Exception {
        try (var s = new RemoteRecordingStream(null)) {
            throw new Exception("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // as expected
        }
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (var s = new RemoteRecordingStream(conn, null)) {
            throw new Exception("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // as expected
        }
    }

    private static void testMissingFlightRecorderMXBean() throws Exception {

        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        conn.unregisterMBean(MXBEAN);
        try (var s = new RemoteRecordingStream(conn)) {
            throw new Exception("Expected IOException");
        } catch (IOException npe) {
            // as expected
        }
    }

    private static ObjectName createObjectName() {
        try {
            return new ObjectName(FlightRecorderMXBean.MXBEAN_NAME);
        } catch (Exception e) {
            throw new InternalError("Unexpected exception", e);
        }
    }
}
