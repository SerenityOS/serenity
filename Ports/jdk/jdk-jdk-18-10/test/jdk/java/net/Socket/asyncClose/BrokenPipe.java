/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4511404
 * @library /test/lib
 * @summary Check that a broken pipe error doesn't throw an exception
 *          indicating the socket is closed.
 * @run main BrokenPipe
 * @run main/othervm -Djava.net.preferIPv4Stack=true BrokenPipe
 */
import java.io.*;
import java.net.*;
import jdk.test.lib.net.IPSupport;

public class BrokenPipe {

    private static class Closer implements Runnable {
        private final Socket s;

        Closer(Socket s) {
            this.s = s;
        }

        public void run() {
            try {
                /* gives time for 'write' to block */
                Thread.sleep(5000);
                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        ServerSocket ss = new ServerSocket(0, 0, InetAddress.getLocalHost());
        Socket client = new Socket(InetAddress.getLocalHost(),
                                   ss.getLocalPort());
        Socket server = ss.accept();
        ss.close();
        new Thread(new Closer(server)).start();

        try {
            client.getOutputStream().write(new byte[1000000]);
        } catch (IOException ioe) {
            /*
             * Check that the exception text doesn't indicate the
             * socket is closed. In tiger we should be able to
             * replace this by catching a more specific exception.
             */
            String text = ioe.getMessage();
            if (text.toLowerCase().indexOf("closed") >= 0) {
                throw ioe;
            }
        } finally {
            server.close();
        }
    }

}
