/*
 * Copyright (c) 2021, Azul, Inc. All rights reserved.
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

//
// Please run in othervm mode.  SunJSSE does not support dynamic system
// properties, no way to re-use system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8268965
 * @summary Socket reset issue for TLS socket close
 * @run main/othervm -Djdk.net.usePlainSocketImpl=true SSLSocketReset
 */

import javax.net.ssl.*;
import java.io.*;
import java.net.*;

public class SSLSocketReset {

    public static void main(String[] args){
        ServerThread serverThread = null;
        Exception clientException = null;
        try {
            SSLServerSocketFactory sslserversocketfactory =
                    SSLContext.getDefault().getServerSocketFactory();
            SSLServerSocket sslServerSocket =
                    (SSLServerSocket) sslserversocketfactory.createServerSocket(0);
            serverThread = new ServerThread(sslServerSocket);
            serverThread.start();
            try {
                Socket socket = new Socket(sslServerSocket.getInetAddress(), sslServerSocket.getLocalPort());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                String msg = "Hello";
                out.writeUTF(msg);
                out.flush();
                msg = in.readUTF();
            } catch(Exception e) {
                clientException = e;
                e.printStackTrace();
            }
            serverThread.join();
        } catch(Exception e) {
            throw new RuntimeException("Fails to start SSL server");
        }
        if (serverThread.exception instanceof SSLException &&
                serverThread.exception.getMessage().equals("Unsupported or unrecognized SSL message") &&
                !(clientException instanceof SocketException &&
                clientException.getMessage().equals("Connection reset"))) {
                System.out.println("Test PASSED");
        } else {
            throw new RuntimeException("TCP connection reset");
        }
    }

    // Thread handling the server socket
    private static class ServerThread extends Thread {
        private SSLServerSocket sslServerSocket = null;
        private SSLSocket sslSocket = null;
        Exception exception;

        ServerThread(SSLServerSocket sslServerSocket){
            this.sslServerSocket = sslServerSocket;
        }

        public void run(){
            try {
                SSLSocket sslsocket = null;
                while (true) {
                    sslsocket = (SSLSocket) sslServerSocket.accept();
                    DataInputStream in = new DataInputStream(sslsocket.getInputStream());
                    DataOutputStream out = new DataOutputStream(sslsocket.getOutputStream());
                    String string;
                    while ((string = in.readUTF()) != null) {
                        out.writeUTF(string);
                        out.flush();
                    }
                }
            } catch(Exception e) {
                exception = e;
                e.printStackTrace();
            }
        }
    }
}
