/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8269481
 * @summary Tests that file descriptors are closed
 * @requires (os.family == "linux")
 * @run main/othervm CloseDescriptors
 */

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpMultiChannel;

public class CloseDescriptors {

    private static final int NUM      = 5;
    private static final int SIZE     = 1024;
    private static final int MAX_DESC = 3;

    public static void main(String[] args) throws Exception {
        if (!Util.isSCTPSupported()) {
            System.out.println("SCTP protocol is not supported");
            System.out.println("Test cannot be run");
            return;
        }

        List<String> lsofDirs = List.of("/usr/bin", "/usr/sbin");
        Optional<Path> lsof = lsofDirs.stream()
                .map(s -> Path.of(s, "lsof"))
                .filter(f -> Files.isExecutable(f))
                .findFirst();
        if (!lsof.isPresent()) {
            System.out.println("Cannot locate lsof in " + lsofDirs);
            System.out.println("Test cannot be run");
            return;
        }

        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();

            Thread server = new Server(port);
            server.start();
            Thread.sleep(100); // wait for server to be ready

            System.out.println("begin");
            for (int i = 0; i < 5; ++i) {
                System.out.println(i);
                doIt(port);
                Thread.sleep(100);
            }
            System.out.println("end");
            server.join();
        }

        long pid = ProcessHandle.current().pid();
        ProcessBuilder pb = new ProcessBuilder(
                lsof.get().toString(), "-U", "-a", "-p", Long.toString(pid));
        Process p = pb.start();
        Object[] lines = p.inputReader().lines().toArray();
        p.waitFor();

        int nfds = lines.length - 1;
        if (nfds > MAX_DESC) {
            throw new RuntimeException("Number of open descriptors " +
                nfds + " > " + MAX_DESC);
        }
    }

    static void doIt(int port) throws Exception {
        InetSocketAddress sa = new InetSocketAddress("localhost", port);

        for (int i = 0; i < NUM; ++i) {
            System.out.println("  " + i);
            SctpMultiChannel channel = SctpMultiChannel.open();
            channel.configureBlocking(true);
            MessageInfo info = MessageInfo.createOutgoing(sa, 0);
            ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE);
            channel.send(buffer, info);
            channel.close();

            Thread.sleep(200);
        }
    }

    static class Server extends Thread {
        int port;

        Server(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            for (int i = 0; i < NUM; i++) {
                try {
                    SctpMultiChannel sm = SctpMultiChannel.open();
                    InetSocketAddress sa =
                        new InetSocketAddress("localhost", port);
                    sm.bind(sa);
                    ByteBuffer buffer = ByteBuffer.allocateDirect(SIZE);
                    MessageInfo info = sm.receive(buffer, null, null);
                    sm.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
