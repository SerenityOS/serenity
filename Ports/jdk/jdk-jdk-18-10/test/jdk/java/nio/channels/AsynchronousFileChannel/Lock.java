/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4607272 6814948 6842687
 * @summary Unit test for AsynchronousFileChannel#lock method
 * @key randomness
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;
import java.nio.channels.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.*;

public class Lock {

    static final Random rand = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("-lockworker")) {
            int port = Integer.parseInt(args[1]);
            runLockWorker(port);
            System.exit(0);
        }

        LockWorkerMirror worker = startLockWorker();
        try {

            // create temporary file
            File blah = File.createTempFile("blah", null);
            blah.deleteOnExit();

            // run tests
            testLockProtocol(blah, worker);
            testAsyncClose(blah, worker);

            // eagerly clean-up
            blah.delete();

        } finally {
            worker.shutdown();
        }
    }

    // test locking protocol
    static void testLockProtocol(File file, LockWorkerMirror worker)
        throws Exception
    {
        FileLock fl;

        // worker VM opens file and acquires exclusive lock
        worker.open(file.getPath()).lock();

        AsynchronousFileChannel ch = AsynchronousFileChannel
            .open(file.toPath(), READ, WRITE);

        // this VM tries to acquire lock
        // (lock should not be acquire until released by worker VM)
        Future<FileLock> result = ch.lock();
        try {
            result.get(2, TimeUnit.SECONDS);
            throw new RuntimeException("Timeout expected");
        } catch (TimeoutException x) {
        }

        // worker VM releases lock
        worker.unlock();

        // this VM should now acquire lock
        fl = result.get();
        fl.release();

        // worker VM acquires lock on range
        worker.lock(0, 10, false);

        // this VM acquires lock on non-overlapping range
        fl = ch.lock(10, 10, false).get();
        fl.release();

        // done
        ch.close();
        worker.close();
    }

    // test close of channel with outstanding lock operation
    static void testAsyncClose(File file, LockWorkerMirror worker) throws Exception {
        // worker VM opens file and acquires exclusive lock
        worker.open(file.getPath()).lock();

        for (int i=0; i<100; i++) {
            AsynchronousFileChannel ch = AsynchronousFileChannel
                .open(file.toPath(), READ, WRITE);

            // try to lock file (should not complete because file is locked by worker)
            Future<FileLock> result = ch.lock();
            try {
                result.get(rand.nextInt(100), TimeUnit.MILLISECONDS);
                throw new RuntimeException("Timeout expected");
            } catch (TimeoutException x) {
            }

            // close channel with lock operation outstanding
            ch.close();

            // operation should complete with AsynchronousCloseException
            try {
                result.get();
                throw new RuntimeException("ExecutionException expected");
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof AsynchronousCloseException)) {
                    x.getCause().printStackTrace();
                    throw new RuntimeException("AsynchronousCloseException expected");
                }
            }
        }

        worker.close();
    }

    // starts a "lock worker" in another process, returning a mirror object to
    // control the worker
    static LockWorkerMirror startLockWorker() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open()
            .bind(new InetSocketAddress(0));
        int port = ((InetSocketAddress)(ssc.getLocalAddress())).getPort();

        String sep = FileSystems.getDefault().getSeparator();

        String command = System.getProperty("java.home") +
            sep + "bin" + sep + "java";
        String testClasses = System.getProperty("test.classes");
        if (testClasses != null)
            command += " -cp " + testClasses;
        command += " Lock -lockworker " + port;

        Process p = Runtime.getRuntime().exec(command);
        IOHandler.handle(p.getInputStream());
        IOHandler.handle(p.getErrorStream());

        // wait for worker to connect
        SocketChannel sc = ssc.accept();
        return new LockWorkerMirror(sc);
    }

    // commands that the worker understands
    static final String OPEN_CMD    = "open";
    static final String CLOSE_CMD   = "close";
    static final String LOCK_CMD    = "lock";
    static final String UNLOCK_CMD  = "unlock";
    static final char TERMINATOR    = ';';

    // provides a proxy to a "lock worker"
    static class LockWorkerMirror {
        private final SocketChannel sc;

        LockWorkerMirror(SocketChannel sc) {
            this.sc = sc;
        }

        private void sendCommand(String cmd, String... params)
            throws IOException
        {
            for (String s: params) {
                cmd += " " + s;
            }
            cmd += TERMINATOR;

            ByteBuffer buf = Charset.defaultCharset().encode(cmd);
            while (buf.hasRemaining()) {
                sc.write(buf);
            }

            // wait for ack
            buf = ByteBuffer.allocate(1);
            int n = sc.read(buf);
            if (n != 1)
                throw new RuntimeException("Reply expected");
            if (buf.get(0) != TERMINATOR)
                throw new RuntimeException("Terminated expected");
        }

        LockWorkerMirror open(String file) throws IOException {
            sendCommand(OPEN_CMD, file);
            return this;
        }

        void close() throws IOException {
            sendCommand(CLOSE_CMD);
        }

        LockWorkerMirror lock() throws IOException {
            sendCommand(LOCK_CMD);
            return this;
        }


        LockWorkerMirror lock(long position, long size, boolean shared)
            throws IOException
        {
            sendCommand(LOCK_CMD, position + "," + size + "," + shared);
            return this;
        }

        LockWorkerMirror unlock() throws IOException {
            sendCommand(UNLOCK_CMD);
            return this;
        }

        void shutdown() throws IOException {
            sc.close();
        }
    }

    // Helper class to direct process output to the parent System.out
    static class IOHandler implements Runnable {
        private final InputStream in;

        IOHandler(InputStream in) {
            this.in = in;
        }

        static void handle(InputStream in) {
            IOHandler handler = new IOHandler(in);
            Thread thr = new Thread(handler);
            thr.setDaemon(true);
            thr.start();
        }

        public void run() {
            try {
                byte b[] = new byte[100];
                for (;;) {
                    int n = in.read(b);
                    if (n < 0) return;
                    for (int i=0; i<n; i++) {
                        System.out.print((char)b[i]);
                    }
                }
            } catch (IOException ioe) { }
        }
    }

    // worker process that responds to simple commands a socket connection
    static void runLockWorker(int port) throws Exception {

        // establish connection to parent
        SocketChannel sc = SocketChannel.open(new InetSocketAddress(port));
        ByteBuffer buf = ByteBuffer.allocateDirect(1024);

        FileChannel fc = null;
        FileLock fl = null;
        try {
            for (;;) {

                // read command (ends with ";")
                buf.clear();
                int n, last = 0;
                do {
                    n = sc.read(buf);
                    if (n < 0)
                        return;
                    if (n == 0)
                        throw new AssertionError();
                    last += n;
                } while (buf.get(last-1) != TERMINATOR);

                // decode into command and optional parameter
                buf.flip();
                String s = Charset.defaultCharset().decode(buf).toString();
                int sp = s.indexOf(" ");
                String cmd = (sp < 0) ? s.substring(0, s.length()-1) :
                    s.substring(0, sp);
                String param = (sp < 0) ? "" : s.substring(sp+1, s.length()-1);

                // execute
                if (cmd.equals(OPEN_CMD)) {
                    if (fc != null)
                        throw new RuntimeException("File already open");
                    fc = FileChannel.open(Paths.get(param),READ, WRITE);
                }
                if (cmd.equals(CLOSE_CMD)) {
                    if (fc == null)
                        throw new RuntimeException("No file open");
                    fc.close();
                    fc = null;
                    fl = null;
                }
                if (cmd.equals(LOCK_CMD)) {
                    if (fl != null)
                        throw new RuntimeException("Already holding lock");

                    if (param.length() == 0) {
                        fl = fc.lock();
                    } else {
                        String[] values = param.split(",");
                        if (values.length != 3)
                            throw new RuntimeException("Lock parameter invalid");
                        long position = Long.parseLong(values[0]);
                        long size = Long.parseLong(values[1]);
                        boolean shared = Boolean.parseBoolean(values[2]);
                        fl = fc.lock(position, size, shared);
                    }
                }

                if (cmd.equals(UNLOCK_CMD)) {
                    if (fl == null)
                        throw new RuntimeException("Not holding lock");
                    fl.release();
                    fl = null;
                }

                // send reply
                byte[] reply = { TERMINATOR };
                n = sc.write(ByteBuffer.wrap(reply));
            }

        } finally {
            sc.close();
            if (fc != null) fc.close();
        }
    }
}
