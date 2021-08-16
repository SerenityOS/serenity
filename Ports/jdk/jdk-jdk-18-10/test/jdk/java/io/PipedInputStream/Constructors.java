/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4028462
 * @summary Test for new constructors that set the pipe size
 */

import java.io.*;
public class Constructors extends Thread {

    static PipedOutputStream out;
    static PipedInputStream  in;
    static int totalToWrite = (8 * 1024);
    static int pipeSize = totalToWrite;

    public void run() {
        try {
            for (int times = (totalToWrite / pipeSize); times > 0; times--) {
                System.out.println("Pipe Input stream reading...");
                int read = in.read(new byte[pipeSize]);
                System.out.println("read: " + read);
                if (read < pipeSize) {
                    throw new Exception("Pipe Size is not set to:" + pipeSize);
                }
            }
        } catch (Throwable e) {
            System.out.println("Pipe Input stream exception:");
            e.printStackTrace();
        } finally {
            System.out.println("Pipe Input stream done.");
        }
    }

    public static void main(String args[]) throws Exception {

        in = new PipedInputStream(pipeSize);
        out = new PipedOutputStream(in);
        testPipe();

        out = new PipedOutputStream();
        in = new PipedInputStream(out, pipeSize);
        testPipe();
   }


   private static void testPipe() throws Exception {
        Constructors reader = new Constructors();
        reader.start();

        try {
            System.out.println("Pipe Output stream started.");
            out.write(new byte[totalToWrite]);
        } catch (Throwable e) {
            System.out.println("Pipe Output stream exception:");
            e.printStackTrace();
        } finally {
            out.close();
            System.out.println("Waiting for Pipe Input stream...");
            reader.join();
            in.close();
            System.out.println("Done.");
        }
    }
}
