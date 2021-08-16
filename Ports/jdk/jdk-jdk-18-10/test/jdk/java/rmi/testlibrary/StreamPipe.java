/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Pipe output of one stream into input of another.
 */
public class StreamPipe extends Thread {

    private InputStream in;
    private OutputStream out;

    private static Object countLock = new Object();
    private static int count = 0;

    /**
     * StreamPipe constructor : should only be called by plugTogether() method.
     */
    private StreamPipe(InputStream in, OutputStream out, String name) {
        super(name);
        this.in  = in;
        this.out = out;
    }

    /**
     * Creates a StreamPipe thread that copies in to out and returns
     * the created instance.
     */
    public static StreamPipe plugTogether(InputStream in, OutputStream out) {
        String name;

        synchronized (countLock) {
            name = "java.rmi.testlibrary.StreamPipe-" + (count++);
        }

        StreamPipe pipe = new StreamPipe(in, out, name);
        pipe.setDaemon(true);
        pipe.start();
        return pipe;
    }

    // Starts redirection of streams.
    public void run() {
        try {
            byte[] buf = new byte[1024];

            while (true) {
                int nr = in.read(buf);
                if (nr == -1)
                    break;
                out.write(buf, 0, nr);
            }
        } catch (InterruptedIOException iioe) {
            // Thread interrupted during IO operation. Terminate StreamPipe.
            return;
        } catch (IOException e) {
            System.err.println("*** IOException in StreamPipe.run:");
            e.printStackTrace();
        }
    }
}
