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

/**
 * @test
 *
 * @bug 4424096
 *
 * @summary DatagramPacket spec needs clarification (reuse buf)
 */
import java.net.*;

public class ReuseBuf {
    static String msgs[] = {"Hello World", "Java", "Good Bye"};
    static int port;

    static class ServerThread extends Thread{
        DatagramSocket ds;
        public ServerThread() {
            try {
                InetAddress local = InetAddress.getLocalHost();
                InetSocketAddress bindaddr = new InetSocketAddress(local, 0);
                ds = new DatagramSocket(bindaddr);
                port = ds.getLocalPort();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        public void run() {
            byte b[] = new byte[100];
            DatagramPacket dp = new DatagramPacket(b,b.length);
            while (true) {
                try {
                    ds.receive(dp);
                    String reply = new String(dp.getData(), dp.getOffset(), dp.getLength());
                    ds.send(new DatagramPacket(reply.getBytes(),reply.length(),
                                               dp.getAddress(),dp.getPort()));
                    if (reply.equals(msgs[msgs.length-1])) {
                        break;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            ds.close();
        }
    }

    public static void main(String args[]) throws Exception {
        ServerThread st = new ServerThread();
        st.start();
        InetAddress local = InetAddress.getLocalHost();
        InetSocketAddress bindaddr = new InetSocketAddress(local, 0);
        DatagramSocket ds = new DatagramSocket(bindaddr);
        byte b[] = new byte[100];
        DatagramPacket dp = new DatagramPacket(b,b.length);
        for (int i = 0; i < msgs.length; i++) {
            ds.send(new DatagramPacket(msgs[i].getBytes(),msgs[i].length(),
                                       InetAddress.getLocalHost(),
                                       port));
            ds.receive(dp);
            if (!msgs[i].equals(new String(dp.getData(), dp.getOffset(), dp.getLength()))) {
                throw new RuntimeException("Msg expected: "+msgs[i] +msgs[i].length()+
                                           "msg received: "+new String(dp.getData(), dp.getOffset(), dp.getLength())+dp.getLength());
            }
        }
        ds.close();
        System.out.println("Test Passed!!!");
    }
}
