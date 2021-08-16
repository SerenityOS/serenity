/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4499763
 * @summary Check for race condition between close and available
 */

import java.io.*;

/*
 * Note: this is a probabalistic test and will not always fail on
 * a workspace where the race condition can occur. However it should
 * never fail on a workspace where the bug has been fixed.
 */
public class CloseAndAvailableRC {
    public static void main(final String[] args) throws Exception {
        CloseAndAvailableRC rc = new CloseAndAvailableRC();
        rc.go();
    }

    private PipedInputStream inPipe = null;
    private PipedOutputStream outPipe = null;
    private Thread sink = null;
    private volatile boolean stop = false;
    private volatile boolean stopTest = false;

    private void go() throws Exception {
        for (long i=0; i<2000; i++) {
            if (stopTest) {
                cleanup();
                throw new RuntimeException("Test failed");
            }
            resetPipes();
            System.err.println("Closing...");
            inPipe.close();
        }
        cleanup();
    }

    // Cleanup old threads
    private void cleanup() throws Exception {
        if (sink != null) {
            stop = true;
            sink.interrupt();
            try {
                sink.join();
            } catch (InterruptedException e) {
            }
            stop = false;
            // Input Stream will have been closed already
            outPipe.close();
        }
    }

    private void resetPipes() throws Exception {
        cleanup();

        // Init pipe; Note: output never read
        inPipe = new PipedInputStream();
        outPipe = new PipedOutputStream(inPipe);

        // Put stuff in pipe so that available() > 0
        for (byte b = 0; b < 10; b++)
            outPipe.write(b);

        sink = new Sink();
        sink.start();
    }

    private class Sink extends Thread {
        public void run() {
            while (!stop) {
                try {
                    final int num = inPipe.available();
                    if (num < 0) {
                        // Bug detected; stop the test
                        stopTest = true;
                    }
                } catch (final IOException e) {
                    System.err.println("Error on available:" + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }
    }
}
