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
 * @bug  6219755
 * @summary Write end loops infinitely when the
 *          buffer is full and the read end has closed.
 */

import java.io.*;

public class WriterLoop extends Thread {

    static PipedOutputStream out;
    static PipedInputStream  in;

    public void run() {
        try {
            System.out.println("Writer started.");

            // without the fix, this test will hang at this point,
            // i.e inside the call to write()
            out.write(new byte[64*1024]);
        } catch (Throwable e) {

            // with the fix an IOException is caught
            System.out.println("Writer exception:");
            e.printStackTrace();
        } finally {
            System.out.println("Writer done.");
        }
    }

    public static void main(String args[]) throws Exception {
        in = new PipedInputStream();
        out = new PipedOutputStream(in);
        WriterLoop writer = new WriterLoop();
        writer.start();

        try {
            System.out.println("Reader reading...");
            in.read(new byte[2048]);

            System.out.println("Reader closing stream...");
            in.close();

            System.out.println("Reader sleeping 3 seconds...");
            Thread.sleep(3000);
        } catch (Throwable e) {
            System.out.println("Reader exception:");
            e.printStackTrace();
        } finally {
            System.out.println("Active threads:");
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            for (int i = 0; i < threads.length; i++) {
                System.out.println("  " + threads[i]);
            }
            System.out.println("Waiting for writer...");
            writer.join();
            System.out.println("Done.");
        }
    }
}
