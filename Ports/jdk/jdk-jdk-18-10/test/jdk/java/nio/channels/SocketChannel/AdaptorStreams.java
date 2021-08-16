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

/* @test
 * @bug 8222774 4430139
 * @run testng AdaptorStreams
 * @summary Exercise socket adaptor input/output streams
 */

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class AdaptorStreams {

    /**
     * Test read when bytes are available
     */
    public void testRead1() throws Exception {
        withConnection((sc, peer) -> {
            peer.getOutputStream().write(99);
            int n = sc.socket().getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test read blocking before bytes are available
     */
    public void testRead2() throws Exception {
        withConnection((sc, peer) -> {
            scheduleWrite(peer.getOutputStream(), 99, 1000);
            int n = sc.socket().getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test read when peer has closed connection
     */
    public void testRead3() throws Exception {
        withConnection((sc, peer) -> {
            peer.close();
            int n = sc.socket().getInputStream().read();
            assertEquals(n, -1);
        });
    }

    /**
     * Test read blocking before peer closes connection
     */
    public void testRead4() throws Exception {
        withConnection((sc, peer) -> {
            scheduleClose(peer, 1000);
            int n = sc.socket().getInputStream().read();
            assertEquals(n, -1);
        });
    }

    /**
     * Test async close of socket when thread blocked in read
     */
    public void testRead5() throws Exception {
        withConnection((sc, peer) -> {
            scheduleClose(sc, 2000);
            InputStream in = sc.socket().getInputStream();
            expectThrows(IOException.class, () -> in.read());
        });
    }

    /**
     * Test interrupt status set before read
     */
    public void testRead6() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();
            Thread.currentThread().interrupt();
            try {
                InputStream in = s.getInputStream();
                expectThrows(IOException.class, () -> in.read());
            } finally {
                Thread.interrupted();  // clear interrupt
            }
            assertTrue(s.isClosed());
        });
    }

    /**
     * Test interrupt of thread blocked in read
     */
    public void testRead7() throws Exception {
        withConnection((sc, peer) -> {
            Future<?> interrupter = scheduleInterrupt(Thread.currentThread(), 2000);
            Socket s = sc.socket();
            try {
                InputStream in = s.getInputStream();
                expectThrows(IOException.class, () -> in.read());
            } finally {
                interrupter.cancel(true);
                Thread.interrupted();  // clear interrupt
            }
            assertTrue(s.isClosed());
        });
    }

    /**
     * Test read when channel is configured non-blocking
     */
    public void testRead8() throws Exception {
        withConnection((sc, peer) -> {
            sc.configureBlocking(false);
            InputStream in = sc.socket().getInputStream();
            expectThrows(IllegalBlockingModeException.class, () -> in.read());
        });
    }

    /**
     * Test timed read when bytes are available
     */
    public void testTimedRead1() throws Exception {
        withConnection((sc, peer) -> {
            peer.getOutputStream().write(99);
            Socket s = sc.socket();
            s.setSoTimeout(1000);
            int n = s.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test timed read blocking before bytes are available
     */
    public void testTimedRead2() throws Exception {
        withConnection((sc, peer) -> {
            scheduleWrite(peer.getOutputStream(), 99, 1000);
            Socket s = sc.socket();
            s.setSoTimeout(5000);
            int n = s.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test timed read when the read times out
     */
    public void testTimedRead3() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();
            s.setSoTimeout(1000);
            InputStream in = s.getInputStream();
            expectThrows(SocketTimeoutException.class, () -> in.read());
        });
    }

    /**
     * Test async close of socket when thread blocked in timed read
     */
    public void testTimedRead4() throws Exception {
        withConnection((sc, peer) -> {
            scheduleClose(sc, 2000);
            Socket s = sc.socket();
            s.setSoTimeout(60*1000);
            InputStream in = s.getInputStream();
            expectThrows(IOException.class, () -> in.read());
        });
    }

    /**
     * Test interrupt status set before timed read
     */
    public void testTimedRead5() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();
            Thread.currentThread().interrupt();
            try {
                s.setSoTimeout(60*1000);
                InputStream in = s.getInputStream();
                expectThrows(IOException.class, () -> in.read());
            } finally {
                Thread.interrupted();  // clear interrupt
            }
            assertTrue(s.isClosed());
        });
    }

    /**
     * Test interrupt of thread blocked in timed read
     */
    public void testTimedRead6() throws Exception {
        withConnection((sc, peer) -> {
            Future<?> interrupter = scheduleInterrupt(Thread.currentThread(), 2000);
            Socket s = sc.socket();
            try {
                s.setSoTimeout(60*1000);
                InputStream in = s.getInputStream();
                expectThrows(IOException.class, () -> in.read());
                assertTrue(s.isClosed());
            } finally {
                interrupter.cancel(true);
                Thread.interrupted();  // clear interrupt
            }
            assertTrue(s.isClosed());
        });
    }

    /**
     * Test async close of socket when thread blocked in write
     */
    public void testWrite1() throws Exception {
        withConnection((sc, peer) -> {
            scheduleClose(sc, 2000);
            expectThrows(IOException.class, () -> {
                OutputStream out = sc.socket().getOutputStream();
                byte[] data = new byte[64*1000];
                while (true) {
                    out.write(data);
                }
            });
        });
    }

    /**
     * Test interrupt status set before write
     */
    public void testWrite2() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();
            Thread.currentThread().interrupt();
            try {
                OutputStream out = s.getOutputStream();
                expectThrows(IOException.class, () -> out.write(99));
            } finally {
                Thread.interrupted();  // clear interrupt
            }
            assertTrue(s.isClosed());
        });
    }

    /**
     * Test interrupt of thread blocked in write
     */
    public void testWrite3() throws Exception {
        withConnection((sc, peer) -> {
            Future<?> interrupter = scheduleInterrupt(Thread.currentThread(), 2000);
            Socket s = sc.socket();
            try {
                expectThrows(IOException.class, () -> {
                    OutputStream out = sc.socket().getOutputStream();
                    byte[] data = new byte[64*1000];
                    while (true) {
                        out.write(data);
                    }
                });
            } finally {
                interrupter.cancel(true);
                Thread.interrupted();  // clear interrupt
            }
            assertTrue(s.isClosed());
        });
    }

    /**
     * Test write when channel is configured non-blocking
     */
    public void testWrite4() throws Exception {
        withConnection((sc, peer) -> {
            sc.configureBlocking(false);
            OutputStream out = sc.socket().getOutputStream();
            expectThrows(IllegalBlockingModeException.class, () -> out.write(99));
        });
    }

    /**
     * Test read when there are bytes available and another thread is blocked
     * in write
     */
    public void testConcurrentReadWrite1() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();

            // block thread in write
            execute(() -> {
                var data = new byte[64*1024];
                OutputStream out = s.getOutputStream();
                for (;;) {
                    out.write(data);
                }
            });
            Thread.sleep(1000); // give writer time to block

            // test read when bytes are available
            peer.getOutputStream().write(99);
            int n = s.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test read blocking when another thread is blocked in write
     */
    public void testConcurrentReadWrite2() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();

            // block thread in write
            execute(() -> {
                var data = new byte[64*1024];
                OutputStream out = s.getOutputStream();
                for (;;) {
                    out.write(data);
                }
            });
            Thread.sleep(1000); // give writer time to block

            // test read blocking until bytes are available
            scheduleWrite(peer.getOutputStream(), 99, 500);
            int n = s.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test writing when another thread is blocked in read
     */
    public void testConcurrentReadWrite3() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();

            // block thread in read
            execute(() -> {
                s.getInputStream().read();
            });
            Thread.sleep(100); // give reader time to block

            // test write
            s.getOutputStream().write(99);
            int n = peer.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test timed read when there are bytes available and another thread is
     * blocked in write
     */
    public void testConcurrentTimedReadWrite1() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();

            // block thread in write
            execute(() -> {
                var data = new byte[64*1024];
                OutputStream out = s.getOutputStream();
                for (;;) {
                    out.write(data);
                }
            });
            Thread.sleep(1000); // give writer time to block

            // test read when bytes are available
            peer.getOutputStream().write(99);
            s.setSoTimeout(60*1000);
            int n = s.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test timed read blocking when another thread is blocked in write
     */
    public void testConcurrentTimedReadWrite2() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();

            // block thread in write
            execute(() -> {
                var data = new byte[64*1024];
                OutputStream out = s.getOutputStream();
                for (;;) {
                    out.write(data);
                }
            });
            Thread.sleep(1000); // give writer time to block

            // test read blocking until bytes are available
            scheduleWrite(peer.getOutputStream(), 99, 500);
            s.setSoTimeout(60*1000);
            int n = s.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    /**
     * Test writing when another thread is blocked in read
     */
    public void testConcurrentTimedReadWrite3() throws Exception {
        withConnection((sc, peer) -> {
            Socket s = sc.socket();

            // block thread in read
            execute(() -> {
                s.setSoTimeout(60*1000);
                s.getInputStream().read();
            });
            Thread.sleep(100); // give reader time to block

            // test write
            s.getOutputStream().write(99);
            int n = peer.getInputStream().read();
            assertEquals(n, 99);
        });
    }

    // -- test infrastructure --

    interface ThrowingTask {
        void run() throws Exception;
    }

    interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws Exception;
    }

    /**
     * Invokes the consumer with a connected pair of socket channel and socket
     */
    static void withConnection(ThrowingBiConsumer<SocketChannel, Socket> consumer)
        throws Exception
    {
        var loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket ss = new ServerSocket()) {
            ss.bind(new InetSocketAddress(loopback, 0));
            try (SocketChannel sc = SocketChannel.open(ss.getLocalSocketAddress())) {
                try (Socket peer = ss.accept()) {
                    consumer.accept(sc, peer);
                }
            }
        }
    }

    static Future<?> scheduleWrite(OutputStream out, byte[] data, long delay) {
        return schedule(() -> {
            try {
                out.write(data);
            } catch (IOException ioe) { }
        }, delay);
    }

    static Future<?> scheduleWrite(OutputStream out, int b, long delay) {
        return scheduleWrite(out, new byte[] { (byte)b }, delay);
    }

    static Future<?> scheduleClose(Closeable c, long delay) {
        return schedule(() -> {
            try {
                c.close();
            } catch (IOException ioe) { }
        }, delay);
    }

    static Future<?> scheduleInterrupt(Thread t, long delay) {
        return schedule(() -> t.interrupt(), delay);
    }

    static Future<?> schedule(Runnable task, long delay) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            return executor.schedule(task, delay, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdown();
        }
    }

    static Future<?> execute(ThrowingTask task) {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            return pool.submit(() -> {
                task.run();
                return null;
            });
        } finally {
            pool.shutdown();
        }
    }
}
