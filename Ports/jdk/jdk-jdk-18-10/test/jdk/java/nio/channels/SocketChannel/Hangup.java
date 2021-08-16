/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4617165
 * @summary Ensure that socket hangups are handled correctly
 * @library ..
 * @build TestUtil
 * @run main Hangup
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class Hangup {

    static PrintStream log = System.err;
    static int failures = 0;

    private static class Failure
        extends RuntimeException
    {

        Failure(String s) {
            super(s);
        }

    }

    static void doSelect(Selector sel, SelectionKey sk, int count)
        throws IOException
    {
        int n = sel.select();
        if (n != 1)
            throw new Failure("Select returned zero");
        Set sks = sel.selectedKeys();
        if (sks.size() != 1)
            throw new Failure("Wrong size for selected-key set: "
                              + sks.size());
        if (!sks.remove(sk))
            throw new Failure("Key not in selected-key set");
        log.println("S: Socket selected #" + count);
    }

    static void dally() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException x) { }
    }

    static void test(boolean writeFromClient, boolean readAfterClose)
        throws IOException
    {

        ServerSocketChannel ssc = null;
        SocketChannel cl = null;        // client end
        SocketChannel sv = null;        // server end
        Selector sel = null;

        log.println();
        log.println("Test: writeFromClient = " + writeFromClient
                    + ", readAfterClose = " + readAfterClose);

        try {

            int ns = 0;                 // Number of selection operations done

            // Set up server socket
            ssc = ServerSocketChannel.open();
            SocketAddress sa = TestUtil.bindToRandomPort(ssc);
            log.println("S: Listening on port "
                        + ssc.socket().getLocalPort());

            // Connect client
            cl = SocketChannel.open(sa);
            log.println("C: Connected via port "
                        + cl.socket().getLocalPort());

            // Accept client connection
            sv = ssc.accept();
            log.println("S: Client connection accepted");

            // Create selector and register server side
            sel = Selector.open();
            sv.configureBlocking(false);
            SelectionKey sk = sv.register(sel, SelectionKey.OP_READ);

            ByteBuffer stuff = ByteBuffer.allocate(10);
            int n;

            if (writeFromClient) {

                // Write from client, read from server

                stuff.clear();
                if (cl.write(stuff) != stuff.capacity())
                    throw new Failure("Incorrect number of bytes written");
                log.println("C: Wrote stuff");
                dally();

                doSelect(sel, sk, ++ns);

                stuff.clear();
                if (sv.read(stuff) != stuff.capacity())
                    throw new Failure("Wrong number of bytes read");
                log.println("S: Read stuff");
            }

            // Close client side
            cl.close();
            log.println("C: Socket closed");
            dally();

            // Select again
            doSelect(sel, sk, ++ns);

            if (readAfterClose) {
                // Read from client after client has disconnected
                stuff.clear();
                if (sv.read(stuff) != -1)
                    throw new Failure("Wrong number of bytes read");
                log.println("S: Read EOF");
            }

            // Select a couple more times just to make sure we're doing
            // the right thing

            doSelect(sel, sk, ++ns);
            doSelect(sel, sk, ++ns);

        } finally {
            if (ssc != null)
                ssc.close();
            if (cl != null)
                cl.close();
            if (sv != null)
                sv.close();
            if (sel != null)
                sel.close();
        }

    }

    public static void main(String[] args) throws IOException {

        for (boolean writeFromClient = false;; writeFromClient = true) {
            for (boolean readAfterClose = false;; readAfterClose = true) {
                try {
                    test(writeFromClient, readAfterClose);
                } catch (Failure x) {
                    x.printStackTrace(log);
                    failures++;
                }
                if (readAfterClose)
                    break;
            }
            if (writeFromClient)
                break;
        }

        if (failures > 0) {
            log.println();
            throw new RuntimeException("Some tests failed");
        }

    }

}
