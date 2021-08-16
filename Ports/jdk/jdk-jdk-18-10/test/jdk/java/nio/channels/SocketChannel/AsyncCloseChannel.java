/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6285901 6501089
 * @summary Check no data is written to wrong socket channel during async closing.
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class AsyncCloseChannel {
    static volatile boolean failed = false;
    static volatile boolean keepGoing = true;
    static int maxAcceptCount = 100;
    static volatile int acceptCount = 0;
    static int sensorPort;
    static int targetPort;

    public static void main(String args[]) throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            System.err.println("WARNING: Still does not work on Windows!");
            return;
        }
        Thread ss = new SensorServer(); ss.start();
        Thread ts = new TargetServer(); ts.start();

        sensorPort = ((ServerThread)ss).server.getLocalPort();
        targetPort = ((ServerThread)ts).server.getLocalPort();

        Thread sc = new SensorClient(); sc.start();
        Thread tc = new TargetClient(); tc.start();

        while(acceptCount < maxAcceptCount && !failed) {
            Thread.sleep(10);
        }
        keepGoing = false;
        try {
            ss.interrupt();
            ts.interrupt();
            sc.interrupt();
            tc.interrupt();
        } catch (Exception e) {}
        if (failed)
            throw new RuntimeException("AsyncCloseChannel2 failed after <"
                                       + acceptCount + "> times of accept!");
    }

    static class SensorServer extends ServerThread {
        public void runEx() throws Exception {
            while(keepGoing) {
                try {
                    final Socket s = server.accept();
                    new Thread() {
                        public void run() {
                            try {
                                int c = s.getInputStream().read();
                                if(c != -1) {
                                    // No data is ever written to the peer's socket!
                                    System.err.println("Oops: read a character: "
                                                       + (char) c);
                                    failed = true;
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            } finally {
                                closeIt(s);
                            }
                        }
                    }.start();
                } catch (IOException ex) {
                    System.err.println("Exception on sensor server " + ex.getMessage());
                }
            }
        }
    }

    static class TargetServer extends ServerThread {
        public void runEx() throws Exception {
            while (keepGoing) {
                try {
                    final Socket s = server.accept();
                    acceptCount++;
                    new Thread() {
                        public void run() {
                            boolean empty = true;
                            try {
                                while (keepGoing) {
                                    int c = s.getInputStream().read();
                                    if(c == -1) {
                                        if(!empty)
                                        break;
                                    }
                                    empty = false;
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            } finally {
                                closeIt(s);
                            }
                        }
                    }.start();
                } catch (IOException ex) {
                    System.err.println("Exception on target server " + ex.getMessage());
                }
            }
        }
    }

    static class SensorClient extends Thread {
        private static boolean wake;
        private static SensorClient theClient;
        public void run() {
            while (keepGoing) {
                Socket s = null;
                try {
                    s = new Socket();
                    synchronized(this) {
                        while(!wake && keepGoing) {
                            try {
                                wait();
                            } catch (InterruptedException ex) { }
                        }
                        wake = false;
                    }
                    s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), sensorPort));
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) { }
                } catch (IOException ex) {
                    System.err.println("Exception on sensor client " + ex.getMessage());
                } finally {
                    if(s != null) {
                        try {
                            s.close();
                        } catch(IOException ex) { ex.printStackTrace();}
                    }
                }
            }
        }

        public SensorClient() {
            theClient = this;
        }

        public static void wakeMe() {
            synchronized(theClient) {
                wake = true;
                theClient.notify();
            }
        }
    }

    static class TargetClient extends Thread {
        volatile boolean ready = false;
        public void run() {
            while(keepGoing) {
                try {
                    final SocketChannel s = SocketChannel.open(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), targetPort));
                    s.finishConnect();
                    s.socket().setSoLinger(false, 0);
                    ready = false;
                    Thread t = new Thread() {
                        public void run() {
                            ByteBuffer b = ByteBuffer.allocate(1);
                            try {
                                for(;;) {
                                    b.clear();
                                    b.put((byte) 'A');
                                    b.flip();
                                    s.write(b);
                                    ready = true;
                                }
                            } catch (IOException ex) {
                                if(!(ex instanceof ClosedChannelException))
                                    System.err.println("Exception in target client child "
                                                       + ex.toString());
                            }
                        }
                    };
                    t.start();
                    while(!ready && keepGoing) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {}
                    }
                    s.close();
                    SensorClient.wakeMe();
                    t.join();
                } catch (IOException ex) {
                     System.err.println("Exception in target client parent "
                                        + ex.getMessage());
                } catch (InterruptedException ex) {}
            }
        }
    }

    static abstract class ServerThread extends Thread {
        ServerSocket server;
        public ServerThread() {
            super();
            try {
                server = new ServerSocket(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void interrupt() {
            super.interrupt();
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        public void run() {
            try {
                runEx();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        abstract void runEx() throws Exception;
    }

    public static void closeIt(Socket s) {
        try {
            if(s != null)
                s.close();
        } catch (IOException ex) { }
    }
}
