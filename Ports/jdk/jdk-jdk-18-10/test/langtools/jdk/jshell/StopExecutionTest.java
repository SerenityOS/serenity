/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8171385
 * @summary Test JShell#stop
 * @modules jdk.jshell/jdk.internal.jshell.tool
 * @build KullaTesting TestingInputStream
 * @run testng StopExecutionTest
 */

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import jdk.internal.jshell.tool.StopDetectingInputStream;
import jdk.internal.jshell.tool.StopDetectingInputStream.State;
import jdk.jshell.JShell;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class StopExecutionTest extends KullaTesting {

    private final Object lock = new Object();
    private boolean isStopped;

    @Test(enabled = false) // TODO 8129546
    public void testStopLoop() throws InterruptedException {
        scheduleStop("while (true) ;");
    }

    @Test(enabled = false) // TODO 8129546
    public void testStopASleep() throws InterruptedException {
        scheduleStop("while (true) { try { Thread.sleep(100); } catch (InterruptedException ex) { } }");
    }

    @Test(enabled = false) // TODO 8129546
    public void testScriptCatchesStop() throws Exception {
        scheduleStop("for (int i = 0; i < 30; i++) { try { Thread.sleep(100); } catch (Throwable ex) { } }");
    }

    private void scheduleStop(String src) throws InterruptedException {
        JShell state = getState();
        isStopped = false;
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        Thread t = new Thread(() -> {
            int i = 1;
            int n = 30;
            synchronized (lock) {
                do {
                    state.stop();
                    if (!isStopped) {
                        out.println("Not stopped. Try again: " + i);
                        try {
                            lock.wait(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                } while (i++ < n && !isStopped);
                if (!isStopped) {
                    System.err.println(writer.toString());
                    fail("Evaluation was not stopped: '" + src + "'");
                }
            }
        });
        t.start();
        assertEval(src);
        synchronized (lock) {
            out.println("Evaluation was stopped successfully: '" + src + "'");
            isStopped = true;
            lock.notify();
        }
        // wait until the background thread finishes to prevent from calling 'stop' on closed state.
        t.join();
    }

    public void testStopDetectingInputRandom() throws IOException {
        long seed = System.nanoTime();
        Random r = new Random(seed);

        for (int m = 0; m < 10; m++) {
            StopDetectingInputStream buffer = new StopDetectingInputStream(null, null);

            buffer.setState(State.BUFFER);

            for (int i = 0; i < 1000; i++) {
                int chunkSize = r.nextInt(StopDetectingInputStream.INITIAL_SIZE * 3);

                doChunk(buffer, chunkSize);
            }
        }
    }

    private void doChunk(StopDetectingInputStream buffer, int chunkSize) throws IOException {
        for (int c = 0; c < chunkSize; c++) {
            buffer.write(c);
        }

        for (int c = 0; c < chunkSize; c++) {
            int read = buffer.read();

            assertEquals(read, c);
        }
    }

    public void testStopDetectingInputBufferWaitStop() throws Exception {
        Runnable shouldNotHappenRun =
                () -> { throw new AssertionError("Should not happen."); };
        Consumer<Exception> shouldNotHappenExc =
                exc -> { throw new AssertionError("Should not happen.", exc); };
        StopDetectingInputStream sd = new StopDetectingInputStream(shouldNotHappenRun, shouldNotHappenExc);
        CountDownLatch reading = new CountDownLatch(1);
        PipedInputStream is = new PipedInputStream() {
            @Override
            public int read() throws IOException {
                reading.countDown();
                return super.read();
            }
        };
        PipedOutputStream os = new PipedOutputStream(is);

        sd.setInputStream(is);
        sd.setState(State.BUFFER);
        reading.await();
        sd.setState(State.WAIT);
        os.write(3);
        int value = sd.read();

        if (value != 3) {
            throw new AssertionError();
        }
    }
}
