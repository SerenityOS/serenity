/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4460583 4470470 4840199 6419424 6710579 6596323 6824135 6395224 7142919
 *      8151582 8068693 8153209
 * @run main/othervm AsyncCloseAndInterrupt
 * @key intermittent
 * @summary Comprehensive test of asynchronous closing and interruption
 * @author Mark Reinhold
 */

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncCloseAndInterrupt {

    static PrintStream log = System.err;

    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException x) { }
    }

    // Wildcard address localized to this machine -- Windoze doesn't allow
    // connecting to a server socket that was previously bound to a true
    // wildcard, namely new InetSocketAddress((InetAddress)null, 0).
    //
    private static InetSocketAddress wildcardAddress;


    // Server socket that blindly accepts all connections

    static ServerSocketChannel acceptor;

    private static void initAcceptor() throws IOException {
        acceptor = ServerSocketChannel.open();
        acceptor.socket().bind(wildcardAddress);

        Thread th = new Thread("Acceptor") {
                public void run() {
                    try {
                        for (;;) {
                            SocketChannel sc = acceptor.accept();
                        }
                    } catch (IOException x) {
                        x.printStackTrace();
                    }
                }
            };

        th.setDaemon(true);
        th.start();
    }


    // Server socket that refuses all connections

    static ServerSocketChannel refuser;

    private static void initRefuser() throws IOException {
        refuser = ServerSocketChannel.open();
        refuser.bind(wildcardAddress, 1);      // use minimum backlog
    }

    // Dead pipe source and sink

    static Pipe.SourceChannel deadSource;
    static Pipe.SinkChannel deadSink;

    private static void initPipes() throws IOException {
        if (deadSource != null)
            deadSource.close();
        deadSource = Pipe.open().source();
        if (deadSink != null)
            deadSink.close();
        deadSink = Pipe.open().sink();
    }


    // Files

    private static File fifoFile = null; // File that blocks on reads and writes
    private static File diskFile = null; // Disk file

    private static void initFile() throws Exception {

        diskFile = File.createTempFile("aci", ".tmp");
        diskFile.deleteOnExit();
        FileChannel fc = new FileOutputStream(diskFile).getChannel();
        buffer.clear();
        if (fc.write(buffer) != buffer.capacity())
            throw new RuntimeException("Cannot create disk file");
        fc.close();

        if (TestUtil.onWindows()) {
            log.println("WARNING: Cannot completely test FileChannels on Windows");
            return;
        }
        fifoFile = new File("x.fifo");
        if (fifoFile.exists()) {
            if (!fifoFile.delete())
                throw new IOException("Cannot delete existing fifo " + fifoFile);
        }
        Process p = Runtime.getRuntime().exec("mkfifo " + fifoFile);
        if (p.waitFor() != 0)
            throw new IOException("Error creating fifo");
        new RandomAccessFile(fifoFile, "rw").close();

    }


    // Channel factories

    static abstract class ChannelFactory {
        private final String name;
        ChannelFactory(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
        abstract InterruptibleChannel create() throws IOException;
    }

    static ChannelFactory socketChannelFactory
        = new ChannelFactory("SocketChannel") {
                InterruptibleChannel create() throws IOException {
                    return SocketChannel.open();
                }
            };

    static ChannelFactory connectedSocketChannelFactory
        = new ChannelFactory("SocketChannel") {
                InterruptibleChannel create() throws IOException {
                    SocketAddress sa = acceptor.socket().getLocalSocketAddress();
                    return SocketChannel.open(sa);
                }
            };

    static ChannelFactory serverSocketChannelFactory
        = new ChannelFactory("ServerSocketChannel") {
                InterruptibleChannel create() throws IOException {
                    ServerSocketChannel ssc = ServerSocketChannel.open();
                    ssc.socket().bind(wildcardAddress);
                    return ssc;
                }
            };

    static ChannelFactory datagramChannelFactory
        = new ChannelFactory("DatagramChannel") {
                InterruptibleChannel create() throws IOException {
                    DatagramChannel dc = DatagramChannel.open();
                    InetAddress lb = InetAddress.getLoopbackAddress();
                    dc.bind(new InetSocketAddress(lb, 0));
                    dc.connect(new InetSocketAddress(lb, 80));
                    return dc;
                }
            };

    static ChannelFactory pipeSourceChannelFactory
        = new ChannelFactory("Pipe.SourceChannel") {
                InterruptibleChannel create() throws IOException {
                    // ## arrange to close sink
                    return Pipe.open().source();
                }
            };

    static ChannelFactory pipeSinkChannelFactory
        = new ChannelFactory("Pipe.SinkChannel") {
                InterruptibleChannel create() throws IOException {
                    // ## arrange to close source
                    return Pipe.open().sink();
                }
            };

    static ChannelFactory fifoFileChannelFactory
        = new ChannelFactory("FileChannel") {
                InterruptibleChannel create() throws IOException {
                    return new RandomAccessFile(fifoFile, "rw").getChannel();
                }
            };

    static ChannelFactory diskFileChannelFactory
        = new ChannelFactory("FileChannel") {
                InterruptibleChannel create() throws IOException {
                    return new RandomAccessFile(diskFile, "rw").getChannel();
                }
            };


    // I/O operations

    static abstract class Op {
        private final String name;
        protected Op(String name) {
            this.name = name;
        }
        abstract void doIO(InterruptibleChannel ich) throws IOException;
        void setup() throws IOException { }
        public String toString() { return name; }
    }

    static ByteBuffer buffer = ByteBuffer.allocateDirect(1 << 20);

    static ByteBuffer[] buffers = new ByteBuffer[] {
        ByteBuffer.allocateDirect(1 << 19),
        ByteBuffer.allocateDirect(1 << 19)
    };

    static void clearBuffers() {
        buffers[0].clear();
        buffers[1].clear();
    }

    static void show(Channel ch) {
        log.print("Channel " + (ch.isOpen() ? "open" : "closed"));
        if (ch.isOpen() && (ch instanceof SocketChannel)) {
            SocketChannel sc = (SocketChannel)ch;
            if (sc.socket().isInputShutdown())
                log.print(", input shutdown");
            if (sc.socket().isOutputShutdown())
                log.print(", output shutdown");
        }
        log.println();
    }

    static final Op READ = new Op("read") {
            void doIO(InterruptibleChannel ich) throws IOException {
                ReadableByteChannel rbc = (ReadableByteChannel)ich;
                buffer.clear();
                int n = rbc.read(buffer);
                log.println("Read returned " + n);
                show(rbc);
                if     (rbc.isOpen()
                        && (n == -1)
                        && (rbc instanceof SocketChannel)
                        && ((SocketChannel)rbc).socket().isInputShutdown()) {
                    return;
                }
                throw new RuntimeException("Read succeeded");
            }
        };

    static final Op READV = new Op("readv") {
            void doIO(InterruptibleChannel ich) throws IOException {
                ScatteringByteChannel sbc = (ScatteringByteChannel)ich;
                clearBuffers();
                int n = (int)sbc.read(buffers);
                log.println("Read returned " + n);
                show(sbc);
                if     (sbc.isOpen()
                        && (n == -1)
                        && (sbc instanceof SocketChannel)
                        && ((SocketChannel)sbc).socket().isInputShutdown()) {
                    return;
                }
                throw new RuntimeException("Read succeeded");
            }
        };

    static final Op RECEIVE = new Op("receive") {
            void doIO(InterruptibleChannel ich) throws IOException {
                DatagramChannel dc = (DatagramChannel)ich;
                buffer.clear();
                dc.receive(buffer);
                show(dc);
                throw new RuntimeException("Read succeeded");
            }
        };

    static final Op WRITE = new Op("write") {
            void doIO(InterruptibleChannel ich) throws IOException {

                WritableByteChannel wbc = (WritableByteChannel)ich;

                SocketChannel sc = null;
                if (wbc instanceof SocketChannel)
                    sc = (SocketChannel)wbc;

                int n = 0;
                for (;;) {
                    buffer.clear();
                    int d = wbc.write(buffer);
                    n += d;
                    if (!wbc.isOpen())
                        break;
                    if ((sc != null) && sc.socket().isOutputShutdown())
                        break;
                }
                log.println("Wrote " + n + " bytes");
                show(wbc);
            }
        };

    static final Op WRITEV = new Op("writev") {
            void doIO(InterruptibleChannel ich) throws IOException {

                GatheringByteChannel gbc = (GatheringByteChannel)ich;

                SocketChannel sc = null;
                if (gbc instanceof SocketChannel)
                    sc = (SocketChannel)gbc;

                int n = 0;
                for (;;) {
                    clearBuffers();
                    int d = (int)gbc.write(buffers);
                    n += d;
                    if (!gbc.isOpen())
                        break;
                    if ((sc != null) && sc.socket().isOutputShutdown())
                        break;
                }
                log.println("Wrote " + n + " bytes");
                show(gbc);

            }
        };

    static final Op CONNECT = new Op("connect") {
            void setup() {
                waitPump("connect waiting for pumping refuser ...");
            }
            void doIO(InterruptibleChannel ich) throws IOException {
                SocketChannel sc = (SocketChannel)ich;
                if (sc.connect(refuser.socket().getLocalSocketAddress()))
                    throw new RuntimeException("Connection succeeded");
                throw new RuntimeException("Connection did not block");
            }
        };

    static final Op FINISH_CONNECT = new Op("finishConnect") {
            void setup() {
                waitPump("finishConnect waiting for pumping refuser ...");
            }
            void doIO(InterruptibleChannel ich) throws IOException {
                SocketChannel sc = (SocketChannel)ich;
                sc.configureBlocking(false);
                SocketAddress sa = refuser.socket().getLocalSocketAddress();
                if (sc.connect(sa))
                    throw new RuntimeException("Connection succeeded");
                sc.configureBlocking(true);
                if (sc.finishConnect())
                    throw new RuntimeException("Connection succeeded");
                throw new RuntimeException("Connection did not block");
            }
        };

    static final Op ACCEPT = new Op("accept") {
            void doIO(InterruptibleChannel ich) throws IOException {
                ServerSocketChannel ssc = (ServerSocketChannel)ich;
                ssc.accept();
                throw new RuntimeException("Accept succeeded");
            }
        };

    // Use only with diskFileChannelFactory
    static final Op TRANSFER_TO = new Op("transferTo") {
            void doIO(InterruptibleChannel ich) throws IOException {
                FileChannel fc = (FileChannel)ich;
                long n = fc.transferTo(0, fc.size(), deadSink);
                log.println("Transferred " + n + " bytes");
                show(fc);
            }
        };

    // Use only with diskFileChannelFactory
    static final Op TRANSFER_FROM = new Op("transferFrom") {
            void doIO(InterruptibleChannel ich) throws IOException {
                FileChannel fc = (FileChannel)ich;
                long n = fc.transferFrom(deadSource, 0, 1 << 20);
                log.println("Transferred " + n + " bytes");
                show(fc);
            }
        };



    // Test modes

    static final int TEST_PREINTR = 0;  // Interrupt thread before I/O
    static final int TEST_INTR = 1;     // Interrupt thread during I/O
    static final int TEST_CLOSE = 2;    // Close channel during I/O
    static final int TEST_SHUTI = 3;    // Shutdown input during I/O
    static final int TEST_SHUTO = 4;    // Shutdown output during I/O

    static final String[] testName = new String[] {
        "pre-interrupt", "interrupt", "close",
        "shutdown-input", "shutdown-output"
    };


    static class Tester extends TestThread {

        private InterruptibleChannel ch;
        private Op op;
        private int test;
        volatile boolean ready = false;

        protected Tester(ChannelFactory cf, InterruptibleChannel ch,
                         Op op, int test)
        {
            super(cf + "/" + op + "/" + testName[test]);
            this.ch = ch;
            this.op = op;
            this.test = test;
        }

        @SuppressWarnings("fallthrough")
        private void caught(Channel ch, IOException x) {
            String xn = x.getClass().getName();
            switch (test) {

            case TEST_PREINTR:
            case TEST_INTR:
                if (!xn.equals("java.nio.channels.ClosedByInterruptException"))
                    throw new RuntimeException("Wrong exception thrown: " + x);
                break;

            case TEST_CLOSE:
            case TEST_SHUTO:
                if (!xn.equals("java.nio.channels.AsynchronousCloseException"))
                    throw new RuntimeException("Wrong exception thrown: " + x);
                break;

            case TEST_SHUTI:
                if (TestUtil.onWindows())
                    break;
                // FALL THROUGH

            default:
                throw new Error(x);
            }

            if (ch.isOpen()) {
                if (test == TEST_SHUTO) {
                    SocketChannel sc = (SocketChannel)ch;
                    if (!sc.socket().isOutputShutdown())
                        throw new RuntimeException("Output not shutdown");
                } else if ((test == TEST_INTR) && (op == TRANSFER_FROM)) {
                    // Let this case pass -- CBIE applies to other channel
                } else {
                    throw new RuntimeException("Channel still open");
                }
            }

            log.println("Thrown as expected: " + x);
        }

        final void go() throws Exception {
            if (test == TEST_PREINTR)
                Thread.currentThread().interrupt();
            ready = true;
            try {
                op.doIO(ch);
            } catch (ClosedByInterruptException x) {
                caught(ch, x);
            } catch (AsynchronousCloseException x) {
                caught(ch, x);
            } finally {
                ch.close();
            }
        }

    }

    private static volatile boolean pumpDone = false;
    private static volatile boolean pumpReady = false;

    private static void waitPump(String msg){
        log.println(msg);
        while (!pumpReady){
            sleep(200);
        }
        log.println(msg + " done");
    }

    // Create a pump thread dedicated to saturate refuser's connection backlog
    private static Future<Integer> pumpRefuser(ExecutorService pumperExecutor) {

        Callable<Integer> pumpTask = new Callable<Integer>() {

            @Override
            public Integer call() throws IOException {
                // Can't reliably saturate connection backlog on Windows Server editions
                assert !TestUtil.onWindows();
                log.println("Start pumping refuser ...");
                List<SocketChannel> refuserClients = new ArrayList<>();

                // Saturate the refuser's connection backlog so that further connection
                // attempts will be blocked
                pumpReady = false;
                while (!pumpDone) {
                    SocketChannel sc = SocketChannel.open();
                    sc.configureBlocking(false);
                    boolean connected = sc.connect(refuser.socket().getLocalSocketAddress());

                    // Assume that the connection backlog is saturated if a
                    // client cannot connect to the refuser within 50 milliseconds
                    long start = System.currentTimeMillis();
                    while (!pumpReady && !connected
                            && (System.currentTimeMillis() - start < 50)) {
                        connected = sc.finishConnect();
                    }

                    if (connected) {
                        // Retain so that finalizer doesn't close
                        refuserClients.add(sc);
                    } else {
                        sc.close();
                        pumpReady = true;
                    }
                }

                for (SocketChannel sc : refuserClients) {
                    sc.close();
                }
                refuser.close();

                log.println("Stop pumping refuser ...");
                return refuserClients.size();
            }
        };

        return pumperExecutor.submit(pumpTask);
    }

    // Test
    static void test(ChannelFactory cf, Op op, int test) throws Exception {
        test(cf, op, test, true);
    }

    static void test(ChannelFactory cf, Op op, int test, boolean extraSleep)
        throws Exception
    {
        log.println();
        initPipes();
        InterruptibleChannel ch = cf.create();
        Tester t = new Tester(cf, ch, op, test);
        log.println(t);
        op.setup();
        t.start();
        do {
            sleep(50);
        } while (!t.ready);

        if (extraSleep) {
            sleep(100);
        }

        switch (test) {

        case TEST_INTR:
            t.interrupt();
            break;

        case TEST_CLOSE:
            ch.close();
            break;

        case TEST_SHUTI:
            if (TestUtil.onWindows()) {
                log.println("WARNING: Asynchronous shutdown not working on Windows");
                ch.close();
            } else {
                ((SocketChannel)ch).socket().shutdownInput();
            }
            break;

        case TEST_SHUTO:
            if (TestUtil.onWindows()) {
                log.println("WARNING: Asynchronous shutdown not working on Windows");
                ch.close();
            } else {
                ((SocketChannel)ch).socket().shutdownOutput();
            }
            break;

        default:
            break;
        }

        t.finishAndThrow(10000);
    }

    static void test(ChannelFactory cf, Op op) throws Exception {
        test(cf, op, true);
    }

    static void test(ChannelFactory cf, Op op, boolean extraSleep) throws Exception {
        // Test INTR cases before PREINTER cases since sometimes
        // interrupted threads can't load classes
        test(cf, op, TEST_INTR, extraSleep);
        test(cf, op, TEST_PREINTR, extraSleep);

        // Bugs, see FileChannelImpl for details
        if (op == TRANSFER_FROM) {
            log.println("WARNING: transferFrom/close not tested");
            return;
        }
        if ((op == TRANSFER_TO) && !TestUtil.onWindows()) {
            log.println("WARNING: transferTo/close not tested");
            return;
        }

        test(cf, op, TEST_CLOSE, extraSleep);
    }

    static void test(ChannelFactory cf)
        throws Exception
    {
        InterruptibleChannel ch = cf.create(); // Sample channel
        ch.close();

        if (ch instanceof ReadableByteChannel) {
            test(cf, READ);
            if (ch instanceof SocketChannel)
                test(cf, READ, TEST_SHUTI);
        }

        if (ch instanceof ScatteringByteChannel) {
            test(cf, READV);
            if (ch instanceof SocketChannel)
                test(cf, READV, TEST_SHUTI);
        }

        if (ch instanceof DatagramChannel) {
            test(cf, RECEIVE);

            // Return here: We can't effectively test writes since, if they
            // block, they do so only for a fleeting moment unless the network
            // interface is overloaded.
            return;

        }

        if (ch instanceof WritableByteChannel) {
            test(cf, WRITE);
            if (ch instanceof SocketChannel)
                test(cf, WRITE, TEST_SHUTO);
        }

        if (ch instanceof GatheringByteChannel) {
            test(cf, WRITEV);
            if (ch instanceof SocketChannel)
                test(cf, WRITEV, TEST_SHUTO);
        }

    }

    public static void main(String[] args) throws Exception {

        wildcardAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        initAcceptor();
        if (!TestUtil.onWindows())
            initRefuser();
        initPipes();
        initFile();

        if (TestUtil.onWindows()) {
            log.println("WARNING: Cannot test FileChannel transfer operations"
                        + " on Windows");
        } else {
            test(diskFileChannelFactory, TRANSFER_TO);
            test(diskFileChannelFactory, TRANSFER_FROM);
        }
        if (fifoFile != null)
            test(fifoFileChannelFactory);

        // Testing positional file reads and writes is impractical: It requires
        // access to a large file soft-mounted via NFS, and even then isn't
        // completely guaranteed to work.
        //
        // Testing map is impractical and arguably unnecessary: It's
        // unclear under what conditions mmap(2) will actually block.

        test(connectedSocketChannelFactory);

        if (TestUtil.onWindows()) {
            log.println("WARNING Cannot reliably test connect/finishConnect"
                + " operations on this platform");
        } else {
            // Only the following tests need refuser's connection backlog
            // to be saturated
            ExecutorService pumperExecutor =
                    Executors.newSingleThreadExecutor(
                    new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setDaemon(true);
                            t.setName("Pumper");
                            return t;
                        }
                    });

            pumpDone = false;
            try {
                Future<Integer> pumpFuture = pumpRefuser(pumperExecutor);
                waitPump("\nWait for initial Pump");

                test(socketChannelFactory, CONNECT, false);
                test(socketChannelFactory, FINISH_CONNECT, false);

                pumpDone = true;
                Integer newConn = pumpFuture.get(30, TimeUnit.SECONDS);
                log.println("Pump " + newConn + " connections.");
            } finally {
                pumperExecutor.shutdown();
            }
        }

        test(serverSocketChannelFactory, ACCEPT);
        test(datagramChannelFactory);
        test(pipeSourceChannelFactory);
        test(pipeSinkChannelFactory);
    }
}
