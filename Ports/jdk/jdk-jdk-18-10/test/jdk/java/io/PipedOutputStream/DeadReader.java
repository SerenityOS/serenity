/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 1267045
 * @summary Make sure write will fail if reading
 *          end thread is dead.
 *
 */


import java.io.*;

public class DeadReader {

    public static void main(String[] argv) throws Exception {
        PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream();
        is.connect(os);

        // create reader thread
        LazyReader lr = new LazyReader(is);

        os.write(new byte[1000]);

        lr.start();
        while (lr.isAlive()) {
            Thread.sleep(100);
        }

        try{
            os.write(27);
            throw new Exception
                ("Test failed: shouldn't be able to write");
        } catch (IOException e) {
            // test passed
        }
    }
}

class LazyReader extends Thread {
    private PipedInputStream snk;
    private int delay;

    public LazyReader(PipedInputStream snk) {
        this.snk = snk;
    }

    public void run() {
        try {
            snk.read();
        } catch (Exception e) {
            System.err.println("Test failed: unexpected exception");
        }
        return;
    }
}
