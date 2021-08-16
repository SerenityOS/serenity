/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
   @bug 5105464 6269047 6541631
 * @summary Test to transfer bytes with a size bigger than Integer.MAX_VALUE
 */


import java.io.*;
import java.net.*;
import java.nio.channels.*;

public class LongTransferTest {
    public static void main(String[] args) throws Exception {
        System.out.println("LongTransferTest-main: "+
         "Test to transfer bytes with a size bigger than Integer.MAX_VALUE.");

        System.out.println("LongTransferTest-main: Test at first "+
               "the private method transferFromFileChannel with files...");

        final String dir = (String)System.getProperty("java.io.tmpdir");
        System.out.println(
            "LongTransferTest-main: using the temp dir (java.io.tmpdir) "+dir);

        File inFile = new File(dir, "LongTransferTest_channelTestInFile_tmp");
        if (!inFile.exists()) {
            inFile.createNewFile();
        }

        File outFile = new File(dir, "LongTransferTest_channelTestOutFile_tmp");
        if (!outFile.exists()) {
            outFile.createNewFile();
        }

        FileInputStream inStream = new FileInputStream(inFile);
        FileChannel inChannel = inStream.getChannel();

        FileOutputStream outStream = new FileOutputStream(outFile);
        FileChannel outChannel = outStream.getChannel();

        outChannel.transferFrom(inChannel, 0, (long)Integer.MAX_VALUE+1L);

        System.out.println("LongTransferTest-main: Test the method transferTo with files.");

        inChannel.transferTo(0, (long)Integer.MAX_VALUE+1L, outChannel);


        System.out.println("LongTransferTest-main: Test the "+
             "private method transferFromArbitraryChannel with sockets ...");

        ServerSocket server = new ServerSocket(0);
        MyJob job = new MyJob(server);
        job.start();

        SocketChannel socket = SocketChannel.open();
        socket.socket().connect(new InetSocketAddress(server.getInetAddress(), server.getLocalPort()));

        outChannel.transferFrom(socket, 0, (long)Integer.MAX_VALUE + 1L);

        System.out.println("LongTransferTest-main: OK!");

        socket.close();
        server.close();

        inChannel.close();
        outChannel.close();

        inFile.delete();
        outFile.delete();
    }

    private static class MyJob extends Thread {
        public MyJob(ServerSocket server) {
            setDaemon(true);
            this.server = server;
        }

        public void run() {
            try {
                Socket s = server.accept();
                System.out.println("MyJob-run: client connected: "+s);

                byte[] bs = new byte[10];
                System.out.println("MyJob-run: write some bytes to client.");

                s.getOutputStream().write(bs);
                s.getOutputStream().flush();

                // no need to write all Integer.MAX_VALUE + 1 bytes
                // it will take too much time
                System.out.println("MyJob-run: close the client socket.");
                s.close();
            } catch (Exception e) {
                // unexpected
                e.printStackTrace();

                System.exit(1);
            }
        }

        private ServerSocket server;
    }
}
