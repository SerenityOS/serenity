/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8002306
 * @summary Ensure that a Pipe can open even if its thread has already
 *          been interrupted.
 * @author Dan Xu
 */

import java.io.IOException;
import java.nio.channels.Pipe;


public class PipeInterrupt {

    private Exception exc = null;

    public static void main(String[] args) throws Exception {
        PipeInterrupt instance = new PipeInterrupt();
        instance.test();
    }

    public void test() throws Exception {

        Thread tester = new Thread("PipeTester") {
            private Pipe testPipe = null;

            @Override
            public void run() {
                for (;;) {
                    boolean interrupted = this.isInterrupted();
                    try {
                        testPipe = Pipe.open();
                        close();
                        if (interrupted) {
                            if (!this.isInterrupted())
                               exc = new RuntimeException("interrupt status reset");
                            break;
                        }
                    } catch (IOException ioe) {
                        exc = ioe;
                    }
                }
            }

            private void close() throws IOException {
                if (testPipe != null) {
                    testPipe.sink().close();
                    testPipe.source().close();
                }
            }
        };

        tester.start();
        Thread.sleep(200);
        tester.interrupt();
        tester.join();

        if (exc != null)
            throw exc;
    }
}
