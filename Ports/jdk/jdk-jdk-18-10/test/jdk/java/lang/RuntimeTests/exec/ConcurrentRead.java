/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4619744
 * @summary Test that Process input/out can be concurrently read/written
 * @author kladko
 */

import java.io.InputStream;
import java.io.OutputStream;

public class ConcurrentRead {

    static volatile Exception savedException;

    public static void main(String[] args) throws Exception {
        if (! UnixCommands.isUnix) {
            System.out.println("For UNIX only");
            return;
        }
        UnixCommands.ensureCommandsAvailable("tee");

        Process p = Runtime.getRuntime().exec(UnixCommands.tee());
        OutputStream out = p.getOutputStream();
        InputStream in = p.getInputStream();
        Thread t1 = new WriterThread(out, in);
        t1.start();
        Thread t2 = new WriterThread(out, in);
        t2.start();
        t1.join();
        t2.join();
        if (savedException != null)
            throw savedException;
    }

    static class WriterThread extends Thread {
        OutputStream out;
        InputStream in;
        WriterThread(OutputStream out, InputStream in) {
            this.out = out;
            this.in = in;
        }
        public void run(){
            try {
                out.write('a');
                out.flush();
                if (in.read() == -1) // got end-of-stream
                    throw new Exception("End of stream in writer thread");
            } catch (Exception e) {
                savedException = e;
            }
        }
    }
}
