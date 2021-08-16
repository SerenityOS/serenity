/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6266377
 * @summary Test to ensure that BufferedWriter releases
 *      resources if flushing the buffer results in an
 *      exception during a call to close().
 */

import java.io.*;

public class Cleanup extends Thread {

    static PipedWriter w;
    static PipedReader r;
    static boolean isWriterClosed = false;

    public void run() {
        try {
            System.out.println("Reader reading...");
            r.read(new char[2048], 0, 2048);

            // Close abruptly before reading all the bytes
            System.out.println("Reader closing stream...");
            r.close();

            Thread.sleep(3000);
        } catch (Throwable e) {
            System.out.println("Reader exception:");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        r = new PipedReader();
        w = new PipedWriter(r);

        Cleanup reader  = new Cleanup();
        reader.start();

        BufferedWriter bw = new BufferedWriter(w);

        boolean isWriterClosed = false;

        try {
            System.out.println("Writer started.");

            for (int i = 0; i < 3; i++) {
              bw.write(new char[1024], 0, (1024));
            }

            // This call to close() will raise an exception during
            // flush() since the reader is already closed
            bw.close();

        } catch (Throwable e) {
            try {
                e.printStackTrace();

                // Make sure that the BufferedWriter releases the resources
                // A write to a properly closed Writer should raise an exception
                bw.write('a');

            } catch (IOException ex) {
                ex.printStackTrace();
                isWriterClosed = true;
            }
        } finally {
            System.out.println("Writer done.");
            reader.join();
            if (!isWriterClosed) {
                throw new Exception("BufferedWriter is not closed properly");
            }
        }
    }
}
