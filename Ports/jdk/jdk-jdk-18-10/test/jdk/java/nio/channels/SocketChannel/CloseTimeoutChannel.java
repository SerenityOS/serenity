/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4726957 4724030 6232954
   @summary Test if the SocketChannel with timeout set can be closed immediately
   @run main/timeout=20 CloseTimeoutChannel
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class CloseTimeoutChannel {
    public static void main(String args[]) throws Exception {
        int port = -1;
        try {
            ServerSocketChannel listener=ServerSocketChannel.open();
            listener.socket().bind(new InetSocketAddress(0));
            port = listener.socket().getLocalPort();
            AcceptorThread thread=new AcceptorThread(listener);
            thread.start();
        } catch (IOException e) {
            System.out.println("Mysterious IO problem");
            e.printStackTrace();
            System.exit(1);
        }

        //Establish connection.  Bug only happens if we open with channel.
        try {
            System.out.println("Establishing connection");
            Socket socket=SocketChannel.open(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port)).socket();
            OutputStream out=socket.getOutputStream();
            InputStream in=socket.getInputStream();

            System.out.println("1. Writing byte 1");
            out.write((byte)1);

            int n=read(socket, in);
            System.out.println("Read byte "+n+"\n");

            System.out.println("3. Writing byte 3");
            out.write((byte)3);

            System.out.println("Closing");
            socket.close();
        } catch (IOException e) {
            System.out.println("Mysterious IO problem");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Reads one byte from in, which must be s.getInputStream.  */
    private static int read(Socket s, InputStream in) throws IOException {
        try {
            s.setSoTimeout(8000);     //causes a bug!
            return in.read();
        } finally {
            s.setSoTimeout(0);
        }

    }

    /** Server thread */
    static class AcceptorThread extends Thread {
        final String INDENT="\t\t\t\t";
        ServerSocketChannel _listener;
        /** @param listener MUST be bound to a port */
        AcceptorThread(ServerSocketChannel listener) {
            _listener=listener;
        }

        public void run() {
            try {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) { }

                System.out.println(INDENT+"Listening on port "+
                    _listener.socket().getLocalPort());
                ByteBuffer buf=ByteBuffer.allocate(5);
                Socket client=_listener.accept().socket();;
                System.out.println(INDENT+"Accepted client");

                OutputStream out=client.getOutputStream();
                InputStream in=client.getInputStream();

                int n=in.read();
                System.out.println(INDENT+"Read byte "+n+"\n");

                System.out.println(INDENT+"2. Writing byte 2");
                out.write((byte)2);

                n=in.read();
                System.out.println(INDENT+"Read byte "+n+"\n");

                n=in.read();
                System.out.println(INDENT+"Read byte "
                                   +(n<0 ? "EOF" : Integer.toString(n)));

                System.out.println(INDENT+"Closing");
                client.close();
            } catch (IOException e) {
                System.out.println(INDENT+"Error accepting!");
            } finally {
                try { _listener.close(); } catch (IOException ignore) { }
            }
        }
    }
}
