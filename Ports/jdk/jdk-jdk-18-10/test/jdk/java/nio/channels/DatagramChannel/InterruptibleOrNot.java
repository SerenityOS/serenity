/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8236246
 * @modules java.base/sun.nio.ch
 * @run testng InterruptibleOrNot
 * @summary Test SelectorProviderImpl.openDatagramChannel(boolean) to create
 *     DatagramChannel objects that optionally support interrupt
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sun.nio.ch.DefaultSelectorProvider;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class InterruptibleOrNot {

    public void testInterruptBeforeInterruptibleReceive() throws Exception {
        testInterruptBeforeReceive(true);
    }

    public void testInterruptDuringInterruptibleReceive() throws Exception {
        testInterruptDuringReceive(true);
    }

    public void testInterruptBeforeUninterruptibleReceive() throws Exception {
        testInterruptBeforeReceive(false);
    }

    public void testInterruptDuringUninterruptibleReceive() throws Exception {
        testInterruptDuringReceive(false);
    }

    public void testInterruptBeforeInterruptibleSend() throws Exception {
        testInterruptBeforeSend(true);
    }

    public void testInterruptBeforeUninterruptibleSend() throws Exception {
        testInterruptBeforeSend(false);
    }

    /**
     * Test invoking DatagramChannel receive with interrupt status set
     */
    static void testInterruptBeforeReceive(boolean interruptible)
        throws Exception
    {
        try (DatagramChannel dc = openDatagramChannel(interruptible)) {
            dc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            Future<?> timeout = scheduleClose(dc, Duration.ofSeconds(2));
            try {
                ByteBuffer buf = ByteBuffer.allocate(100);
                Thread.currentThread().interrupt();
                assertThrows(expectedException(interruptible), () -> dc.receive(buf));
            } finally {
                timeout.cancel(false);
            }
        } finally {
            Thread.interrupted();  // clear interrupt
        }
    }

    /**
     * Test Thread.interrupt when target thread is blocked in DatagramChannel receive
     */
    static void testInterruptDuringReceive(boolean interruptible)
        throws Exception
    {
        try (DatagramChannel dc = openDatagramChannel(interruptible)) {
            dc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            Future<?> timerTask = scheduleClose(dc, Duration.ofSeconds(5));
            Future<?> interruptTask = scheduleInterrupt(Thread.currentThread(), Duration.ofSeconds(1));
            try {
                ByteBuffer buf = ByteBuffer.allocate(100);
                assertThrows(expectedException(interruptible), () -> dc.receive(buf));
            } finally {
                timerTask.cancel(false);
                interruptTask.cancel(false);
            }
        } finally {
            Thread.interrupted();  // clear interrupt
        }
    }

    /**
     * Test invoking DatagramChannel send with interrupt status set
     */
    static void testInterruptBeforeSend(boolean interruptible)
        throws Exception
    {
        try (DatagramChannel dc = openDatagramChannel(interruptible)) {
            dc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            Future<?> timeout = scheduleClose(dc, Duration.ofSeconds(2));
            try {
                ByteBuffer buf = ByteBuffer.allocate(100);
                SocketAddress target = dc.getLocalAddress();
                Thread.currentThread().interrupt();
                if (interruptible) {
                    assertThrows(ClosedByInterruptException.class, () -> dc.send(buf, target));
                } else {
                    int n = dc.send(buf, target);
                    assertTrue(n == 100);
                }
            } finally {
                timeout.cancel(false);
            }
        } finally {
            Thread.interrupted();  // clear interrupt
        }
    }

    /**
     * Creates a DatagramChannel that is interruptible or not.
     */
    static DatagramChannel openDatagramChannel(boolean interruptible) throws IOException {
        if (interruptible) {
            return DatagramChannel.open();
        } else {
            return DefaultSelectorProvider.get().openUninterruptibleDatagramChannel();
        }
    }

    /**
     * Expect ClosedByInterruptException if interruptible.
     */
    static Class<? extends Exception> expectedException(boolean expectInterrupt) {
        if (expectInterrupt) {
            return ClosedByInterruptException.class;
        } else {
            return AsynchronousCloseException.class;
        }
    }

    /**
     * Schedule the given object to be closed.
     */
    static Future<?> scheduleClose(Closeable c, Duration timeout) {
        long nanos = TimeUnit.NANOSECONDS.convert(timeout);
        return STPE.schedule(() -> {
            c.close();
            return null;
        }, nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Schedule the given thread to be interrupted.
     */
    static Future<?> scheduleInterrupt(Thread t, Duration timeout) {
        long nanos = TimeUnit.NANOSECONDS.convert(timeout);
        return STPE.schedule(t::interrupt, nanos, TimeUnit.NANOSECONDS);
    }

    static final ScheduledExecutorService STPE = Executors.newScheduledThreadPool(0);
}
