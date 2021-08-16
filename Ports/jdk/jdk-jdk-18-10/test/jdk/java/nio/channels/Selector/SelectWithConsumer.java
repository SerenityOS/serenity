/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Unit test for Selector.select/selectNow(Consumer)
 * @bug 8199433 8208780
 * @run testng SelectWithConsumer
 */

/* @test
 * @requires (os.family == "windows")
 * @run testng/othervm -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider SelectWithConsumer
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.concurrent.TimeUnit.*;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class SelectWithConsumer {

    /**
     * Invoke the select methods that take an action and check that the
     * accumulated ready ops notified to the action matches the expected ops.
     */
    void testActionInvoked(SelectionKey key, int expectedOps) throws Exception {
        var callerThread = Thread.currentThread();
        var sel = key.selector();
        var interestOps = key.interestOps();
        var notifiedOps = new AtomicInteger();

        if (expectedOps == 0) {
            // ensure select(Consumer) does not block indefinitely
            sel.wakeup();
        } else {
            // ensure that the channel is ready for all expected operations
            sel.select();
            while ((key.readyOps() & interestOps) != expectedOps) {
                Thread.sleep(100);
                sel.select();
            }
        }

        // select(Consumer)
        notifiedOps.set(0);
        int n = sel.select(k -> {
            assertTrue(Thread.currentThread() == callerThread);
            assertTrue(k == key);
            int readyOps = key.readyOps();
            assertTrue((readyOps & interestOps) != 0);
            assertTrue((readyOps & notifiedOps.get()) == 0);
            notifiedOps.set(notifiedOps.get() | readyOps);
        });
        assertTrue((n == 1) ^ (expectedOps == 0));
        assertTrue(notifiedOps.get() == expectedOps);

        // select(Consumer, timeout)
        notifiedOps.set(0);
        n = sel.select(k -> {
            assertTrue(Thread.currentThread() == callerThread);
            assertTrue(k == key);
            int readyOps = key.readyOps();
            assertTrue((readyOps & interestOps) != 0);
            assertTrue((readyOps & notifiedOps.get()) == 0);
            notifiedOps.set(notifiedOps.get() | readyOps);
        }, 1000);
        assertTrue((n == 1) ^ (expectedOps == 0));
        assertTrue(notifiedOps.get() == expectedOps);

        // selectNow(Consumer)
        notifiedOps.set(0);
        n = sel.selectNow(k -> {
            assertTrue(Thread.currentThread() == callerThread);
            assertTrue(k == key);
            int readyOps = key.readyOps();
            assertTrue((readyOps & interestOps) != 0);
            assertTrue((readyOps & notifiedOps.get()) == 0);
            notifiedOps.set(notifiedOps.get() | readyOps);
        });
        assertTrue((n == 1) ^ (expectedOps == 0));
        assertTrue(notifiedOps.get() == expectedOps);
    }

    /**
     * Test that an action is performed when a channel is ready for reading.
     */
    public void testReadable() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();
            source.configureBlocking(false);
            SelectionKey key = source.register(sel, SelectionKey.OP_READ);

            // write to sink to ensure source is readable
            scheduleWrite(sink, messageBuffer(), 100, MILLISECONDS);

            // test that action is invoked
            testActionInvoked(key, SelectionKey.OP_READ);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test that an action is performed when a channel is ready for writing.
     */
    public void testWritable() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            sink.configureBlocking(false);
            SelectionKey key = sink.register(sel, SelectionKey.OP_WRITE);

            // test that action is invoked
            testActionInvoked(key, SelectionKey.OP_WRITE);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test that an action is performed when a channel is ready for both
     * reading and writing.
     */
    public void testReadableAndWriteable() throws Exception {
        ServerSocketChannel ssc = null;
        SocketChannel sc = null;
        SocketChannel peer = null;
        try (Selector sel = Selector.open()) {
            ssc = ServerSocketChannel.open().bind(new InetSocketAddress(0));
            sc = SocketChannel.open(ssc.getLocalAddress());
            sc.configureBlocking(false);
            SelectionKey key = sc.register(sel, (SelectionKey.OP_READ |
                                                 SelectionKey.OP_WRITE));

            // accept connection and write data so the source is readable
            peer = ssc.accept();
            peer.write(messageBuffer());

            // test that action is invoked
            testActionInvoked(key, (SelectionKey.OP_READ | SelectionKey.OP_WRITE));
        } finally {
            if (ssc != null) ssc.close();
            if (sc != null) sc.close();
            if (peer != null) peer.close();
        }
    }

    /**
     * Test that the action is called for two selected channels
     */
    public void testTwoChannels() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            source.configureBlocking(false);
            sink.configureBlocking(false);
            SelectionKey key1 = source.register(sel, SelectionKey.OP_READ);
            SelectionKey key2 = sink.register(sel, SelectionKey.OP_WRITE);

            // write to sink to ensure that the source is readable
            sink.write(messageBuffer());

            // wait for key1 to be readable
            sel.select();
            assertTrue(key2.isWritable());
            while (!key1.isReadable()) {
                Thread.sleep(20);
                sel.select();
            }

            var counter = new AtomicInteger();

            // select(Consumer)
            counter.set(0);
            int n = sel.select(k -> {
                assertTrue(k == key1 || k == key2);
                counter.incrementAndGet();
            });
            assertTrue(n == 2);
            assertTrue(counter.get() == 2);

            // select(Consumer, timeout)
            counter.set(0);
            n = sel.select(k -> {
                assertTrue(k == key1 || k == key2);
                counter.incrementAndGet();
            }, 1000);
            assertTrue(n == 2);
            assertTrue(counter.get() == 2);

            // selectNow(Consumer)
            counter.set(0);
            n = sel.selectNow(k -> {
                assertTrue(k == key1 || k == key2);
                counter.incrementAndGet();
            });
            assertTrue(n == 2);
            assertTrue(counter.get() == 2);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test calling select twice, the action should be invoked each time
     */
    public void testRepeatedSelect1() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            source.configureBlocking(false);
            SelectionKey key = source.register(sel, SelectionKey.OP_READ);

            // write to sink to ensure that the source is readable
            sink.write(messageBuffer());

            // test that action is invoked
            testActionInvoked(key, SelectionKey.OP_READ);
            testActionInvoked(key, SelectionKey.OP_READ);

        } finally {
            closePipe(p);
        }
    }

    /**
     * Test calling select twice. An I/O operation is performed after the
     * first select so the channel will not be selected by the second select.
     */
    public void testRepeatedSelect2() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            source.configureBlocking(false);
            SelectionKey key = source.register(sel, SelectionKey.OP_READ);

            // write to sink to ensure that the source is readable
            sink.write(messageBuffer());

            // test that action is invoked
            testActionInvoked(key, SelectionKey.OP_READ);

            // read all bytes
            int n;
            ByteBuffer bb = ByteBuffer.allocate(100);
            do {
                n = source.read(bb);
                bb.clear();
            } while (n > 0);

            // test that action is not invoked
            testActionInvoked(key, 0);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test timeout
     */
    public void testTimeout() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            source.configureBlocking(false);
            source.register(sel, SelectionKey.OP_READ);
            long start = System.currentTimeMillis();
            int n = sel.select(k -> assertTrue(false), 1000L);
            long duration = System.currentTimeMillis() - start;
            assertTrue(n == 0);
            assertTrue(duration > 500, "select took " + duration + " ms");
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test wakeup prior to select
     */
    public void testWakeupBeforeSelect() throws Exception {
        // select(Consumer)
        try (Selector sel = Selector.open()) {
            sel.wakeup();
            int n = sel.select(k -> assertTrue(false));
            assertTrue(n == 0);
        }

        // select(Consumer, timeout)
        try (Selector sel = Selector.open()) {
            sel.wakeup();
            long start = System.currentTimeMillis();
            int n = sel.select(k -> assertTrue(false), 60*1000);
            long duration = System.currentTimeMillis() - start;
            assertTrue(n == 0);
            assertTrue(duration < 5000, "select took " + duration + " ms");
        }
    }

    /**
     * Test wakeup during select
     */
    public void testWakeupDuringSelect() throws Exception {
        // select(Consumer)
        try (Selector sel = Selector.open()) {
            scheduleWakeup(sel, 1, SECONDS);
            int n = sel.select(k -> assertTrue(false));
            assertTrue(n == 0);
        }

        // select(Consumer, timeout)
        try (Selector sel = Selector.open()) {
            scheduleWakeup(sel, 1, SECONDS);
            long start = System.currentTimeMillis();
            int n = sel.select(k -> assertTrue(false), 60*1000);
            long duration = System.currentTimeMillis() - start;
            assertTrue(n == 0);
            assertTrue(duration > 500 && duration < 10*1000,
                    "select took " + duration + " ms");
        }
    }

    /**
     * Test invoking select with interrupt status set
     */
    public void testInterruptBeforeSelect() throws Exception {
        // select(Consumer)
        try (Selector sel = Selector.open()) {
            Thread.currentThread().interrupt();
            int n = sel.select(k -> assertTrue(false));
            assertTrue(n == 0);
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(sel.isOpen());
        } finally {
            Thread.currentThread().interrupted();  // clear interrupt status
        }

        // select(Consumer, timeout)
        try (Selector sel = Selector.open()) {
            Thread.currentThread().interrupt();
            long start = System.currentTimeMillis();
            int n = sel.select(k -> assertTrue(false), 60*1000);
            long duration = System.currentTimeMillis() - start;
            assertTrue(n == 0);
            assertTrue(duration < 5000, "select took " + duration + " ms");
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(sel.isOpen());
        } finally {
            Thread.currentThread().interrupted();  // clear interrupt status
        }
    }

    /**
     * Test interrupt thread during select
     */
    public void testInterruptDuringSelect() throws Exception {
        // select(Consumer)
        try (Selector sel = Selector.open()) {
            scheduleInterrupt(Thread.currentThread(), 1, SECONDS);
            int n = sel.select(k -> assertTrue(false));
            assertTrue(n == 0);
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(sel.isOpen());
        } finally {
            Thread.currentThread().interrupted();  // clear interrupt status
        }

        // select(Consumer, timeout)
        try (Selector sel = Selector.open()) {
            scheduleInterrupt(Thread.currentThread(), 1, SECONDS);
            long start = System.currentTimeMillis();
            int n = sel.select(k -> assertTrue(false), 60*1000);
            long duration = System.currentTimeMillis() - start;
            assertTrue(n == 0);
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(sel.isOpen());
        } finally {
            Thread.currentThread().interrupted();  // clear interrupt status
        }
    }

    /**
     * Test invoking select on a closed selector
     */
    @Test(expectedExceptions = ClosedSelectorException.class)
    public void testClosedSelector1() throws Exception {
        Selector sel = Selector.open();
        sel.close();
        sel.select(k -> assertTrue(false));
    }
    @Test(expectedExceptions = ClosedSelectorException.class)
    public void testClosedSelector2() throws Exception {
        Selector sel = Selector.open();
        sel.close();
        sel.select(k -> assertTrue(false), 1000);
    }
    @Test(expectedExceptions = ClosedSelectorException.class)
    public void testClosedSelector3() throws Exception {
        Selector sel = Selector.open();
        sel.close();
        sel.selectNow(k -> assertTrue(false));
    }

    /**
     * Test closing selector while in a selection operation
     */
    public void testCloseDuringSelect() throws Exception {
        // select(Consumer)
        try (Selector sel = Selector.open()) {
            scheduleClose(sel, 3, SECONDS);
            int n = sel.select(k -> assertTrue(false));
            assertTrue(n == 0);
            assertFalse(sel.isOpen());
        }

        // select(Consumer, timeout)
        try (Selector sel = Selector.open()) {
            long before = System.nanoTime();
            scheduleClose(sel, 3, SECONDS);
            long start = System.nanoTime();
            int n = sel.select(k -> assertTrue(false), 60*1000);
            long after = System.nanoTime();
            long selectDuration = (after - start) / 1000000;
            long scheduleDuration = (start - before) / 1000000;
            assertTrue(n == 0);
            assertTrue(selectDuration > 2000 && selectDuration < 10*1000,
                    "select took " + selectDuration + " ms schedule took " +
                    scheduleDuration + " ms");
            assertFalse(sel.isOpen());
        }
    }

    /**
     * Test action closing selector
     */
    @Test(expectedExceptions = ClosedSelectorException.class)
    public void testActionClosingSelector() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            source.configureBlocking(false);
            SelectionKey key = source.register(sel, SelectionKey.OP_READ);

            // write to sink to ensure that the source is readable
            sink.write(messageBuffer());

            // should relay ClosedSelectorException
            sel.select(k -> {
                assertTrue(k == key);
                try {
                    sel.close();
                } catch (IOException ioe) { }
            });
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test that the action is invoked while synchronized on the selector and
     * its selected-key set.
     */
    public void testLocks() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SourceChannel source = p.source();
            Pipe.SinkChannel sink = p.sink();
            source.configureBlocking(false);
            SelectionKey key = source.register(sel, SelectionKey.OP_READ);

            // write to sink to ensure that the source is readable
            sink.write(messageBuffer());

            // select(Consumer)
            sel.select(k -> {
                assertTrue(k == key);
                assertTrue(Thread.holdsLock(sel));
                assertFalse(Thread.holdsLock(sel.keys()));
                assertTrue(Thread.holdsLock(sel.selectedKeys()));
            });

            // select(Consumer, timeout)
            sel.select(k -> {
                assertTrue(k == key);
                assertTrue(Thread.holdsLock(sel));
                assertFalse(Thread.holdsLock(sel.keys()));
                assertTrue(Thread.holdsLock(sel.selectedKeys()));
            }, 1000L);

            // selectNow(Consumer)
            sel.selectNow(k -> {
                assertTrue(k == key);
                assertTrue(Thread.holdsLock(sel));
                assertFalse(Thread.holdsLock(sel.keys()));
                assertTrue(Thread.holdsLock(sel.selectedKeys()));
            });
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test that selection operations remove cancelled keys from the selector's
     * key and selected-key sets.
     */
    public void testCancel() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();

            // write to sink to ensure that the source is readable
            sink.write(messageBuffer());

            source.configureBlocking(false);
            SelectionKey key1 = source.register(sel, SelectionKey.OP_READ);
            // make sure pipe source is readable before we do following checks.
            // this is sometime necessary on windows where pipe is implemented
            // as a pair of connected socket, so there is no guarantee that written
            // bytes on sink side is immediately available on source side.
            sel.select();

            sink.configureBlocking(false);
            SelectionKey key2 = sink.register(sel, SelectionKey.OP_WRITE);
            sel.selectNow();

            assertTrue(sel.keys().contains(key1));
            assertTrue(sel.keys().contains(key2));
            assertTrue(sel.selectedKeys().contains(key1));
            assertTrue(sel.selectedKeys().contains(key2));

            // cancel key1
            key1.cancel();
            int n = sel.selectNow(k -> assertTrue(k == key2));
            assertTrue(n == 1);
            assertFalse(sel.keys().contains(key1));
            assertTrue(sel.keys().contains(key2));
            assertFalse(sel.selectedKeys().contains(key1));
            assertTrue(sel.selectedKeys().contains(key2));

            // cancel key2
            key2.cancel();
            n = sel.selectNow(k -> assertTrue(false));
            assertTrue(n == 0);
            assertFalse(sel.keys().contains(key1));
            assertFalse(sel.keys().contains(key2));
            assertFalse(sel.selectedKeys().contains(key1));
            assertFalse(sel.selectedKeys().contains(key2));
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test an action invoking select()
     */
    public void testReentrantSelect1() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();
            source.configureBlocking(false);
            source.register(sel, SelectionKey.OP_READ);

            // write to sink to ensure that the source is readable
            scheduleWrite(sink, messageBuffer(), 100, MILLISECONDS);

            int n = sel.select(k -> {
                try {
                    sel.select();
                    assertTrue(false);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } catch (IllegalStateException expected) {
                }
            });
            assertTrue(n == 1);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test an action invoking selectNow()
     */
    public void testReentrantSelect2() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();

            // write to sink to ensure that the source is readable
            scheduleWrite(sink, messageBuffer(), 100, MILLISECONDS);

            source.configureBlocking(false);
            source.register(sel, SelectionKey.OP_READ);
            int n = sel.select(k -> {
                try {
                    sel.selectNow();
                    assertTrue(false);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } catch (IllegalStateException expected) {
                }
            });
            assertTrue(n == 1);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Test an action invoking select(Consumer)
     */
    public void testReentrantSelect3() throws Exception {
        Pipe p = Pipe.open();
        try (Selector sel = Selector.open()) {
            Pipe.SinkChannel sink = p.sink();
            Pipe.SourceChannel source = p.source();

            // write to sink to ensure that the source is readable
            scheduleWrite(sink, messageBuffer(), 100, MILLISECONDS);

            source.configureBlocking(false);
            source.register(sel, SelectionKey.OP_READ);
            int n = sel.select(k -> {
                try {
                    sel.select(x -> assertTrue(false));
                    assertTrue(false);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } catch (IllegalStateException expected) {
                }
            });
            assertTrue(n == 1);
        } finally {
            closePipe(p);
        }
    }

    /**
     * Negative timeout
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNegativeTimeout() throws Exception {
        try (Selector sel = Selector.open()) {
            sel.select(k -> { }, -1L);
        }
    }

    /**
     * Null action
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testNull1() throws Exception {
        try (Selector sel = Selector.open()) {
            sel.select(null);
        }
    }
    @Test(expectedExceptions = NullPointerException.class)
    public void testNull2() throws Exception {
        try (Selector sel = Selector.open()) {
            sel.select(null, 1000);
        }
    }
    @Test(expectedExceptions = NullPointerException.class)
    public void testNull3() throws Exception {
        try (Selector sel = Selector.open()) {
            sel.selectNow(null);
        }
    }


    // -- support methods ---

    private final ScheduledExecutorService POOL = Executors.newScheduledThreadPool(1);

    @AfterTest
    void shutdownThreadPool() {
        POOL.shutdown();
    }

    void scheduleWakeup(Selector sel, long delay, TimeUnit unit) {
        POOL.schedule(() -> sel.wakeup(), delay, unit);
    }

    void scheduleInterrupt(Thread t, long delay, TimeUnit unit) {
        POOL.schedule(() -> t.interrupt(), delay, unit);
    }

    void scheduleClose(Closeable c, long delay, TimeUnit unit) {
        POOL.schedule(() -> {
            try {
                c.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }, delay, unit);
    }

    void scheduleWrite(WritableByteChannel sink, ByteBuffer buf, long delay, TimeUnit unit) {
        POOL.schedule(() -> {
            try {
                sink.write(buf);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }, delay, unit);
    }

    static void closePipe(Pipe p) {
        try { p.sink().close(); } catch (IOException ignore) { }
        try { p.source().close(); } catch (IOException ignore) { }
    }

    static ByteBuffer messageBuffer() {
        try {
            return ByteBuffer.wrap("message".getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
