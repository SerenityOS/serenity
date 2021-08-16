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

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerConnection;

import jdk.jfr.Event;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Sanity test methods that delegates to an ordinary stream
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.streaming.TestDelegated
 */
public class TestDelegated {

    private static MBeanServerConnection CONNECTION = ManagementFactory.getPlatformMBeanServer();

    static class TestDelegatedEvent extends Event {
    }

    // The assumption here is that the following methods don't
    // need t be tested fully since they all delegate to the
    // same implementation class that is tested elsewhere.

    public static void main(String[] args) throws Exception {
        testRemove();
        testReuse();
        testOrdered();
        testOnEvent();
        testOnEventName();
        testOnFlush();
        testOnError();
        testOnClose();
        testSetMaxAge();
        testAwaitTermination();
        testAwaitTerminationWithDuration();
    }

    private static void testSetMaxAge() throws Exception {
        try (RemoteRecordingStream stream = new RemoteRecordingStream(CONNECTION)) {
            try {
                stream.setMaxAge(null);
                throw new Exception("Expected NullPointerException");
            } catch (NullPointerException npe) {
                // As expected
            }
        }
    }

    private static void testAwaitTerminationWithDuration() throws Exception {
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onEvent(e -> {
                rs.close();
            });
            rs.startAsync();
            TestDelegatedEvent e = new TestDelegatedEvent();
            e.commit();
            rs.awaitTermination(Duration.ofDays(1));
        }
    }

    private static void testAwaitTermination() throws Exception {
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onEvent(e -> {
                rs.close();
            });
            rs.startAsync();
            TestDelegatedEvent e = new TestDelegatedEvent();
            e.commit();
            rs.awaitTermination();
        }
    }

    private static void testOnClose() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onClose(() -> {
                latch.countDown();
            });
            rs.startAsync();
            rs.close();
            latch.await();
        }
    }

    private static void testOnError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onEvent(TestDelegatedEvent.class.getName(), e -> {
                throw new RuntimeException("Testing");
            });
            rs.onError(t -> {
                latch.countDown();
            });
            rs.startAsync();
            TestDelegatedEvent e = new TestDelegatedEvent();
            e.commit();
            latch.await();
        }
    }

    private static void testOnFlush() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onFlush(() -> {
                latch.countDown();
            });
            rs.startAsync();
            TestDelegatedEvent e = new TestDelegatedEvent();
            e.commit();
            latch.await();
        }
    }

    private static void testOnEventName() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onEvent(TestDelegatedEvent.class.getName(), e -> {
                latch.countDown();
            });
            rs.startAsync();
            TestDelegatedEvent e = new TestDelegatedEvent();
            e.commit();
            latch.await();
        }
    }

    private static void testOnEvent() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.onEvent(e -> {
                System.out.println(e);
                latch.countDown();
            });
            rs.startAsync();
            TestDelegatedEvent e = new TestDelegatedEvent();
            e.commit();
            latch.await();
        }

    }

    private static void testOrdered() throws Exception {
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.setOrdered(true);
            rs.setOrdered(false);
        }
    }

    private static void testReuse() throws Exception {
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            rs.setReuse(true);
            rs.setReuse(false);
        }
    }

    private static void testRemove() throws Exception {
        try (RemoteRecordingStream rs = new RemoteRecordingStream(CONNECTION)) {
            Runnable r1 = () -> {
            };
            Runnable r2 = () -> {
            };
            rs.onFlush(r1);
            if (!rs.remove(r1)) {
                throw new Exception("Expected remove to return true");
            }
            if (rs.remove(r2)) {
                throw new Exception("Expected remove to return false");
            }
        }
    }
}
