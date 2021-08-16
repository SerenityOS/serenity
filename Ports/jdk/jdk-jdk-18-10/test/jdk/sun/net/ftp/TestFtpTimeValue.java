/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255078
 * @summary verify that datetime in MDTM and MLSD responses are properly parsed
 * @library /test/lib
 * @modules java.base/sun.net.ftp
 * @build jdk.test.lib.Asserts
 * @run main/othervm -Duser.timezone=UTC TestFtpTimeValue
 * @run main/othervm -Duser.timezone=America/Los_Angeles TestFtpTimeValue
 */

import jdk.test.lib.Asserts;
import sun.net.ftp.FtpClient;

import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class TestFtpTimeValue {
    private enum TestCase {
        AmTimeNoMilli(2019, 4, 20, 10, 57, 13, 0),
        AmTimeWithMilli(2019, 4, 20, 10, 57, 13, 50),
        PmTimeNoMilli(2019, 4, 20, 22, 57, 13, 0),
        PmTimeWithMilli(2019, 4, 20, 22, 57, 13, 50),
        ;

        public final Date expectedCreated;
        public final Date expectedModified;
        public final String create;
        public final String modify;

        TestCase(int year, int month, int day, int hrs, int min, int sec, int milliseconds) {
            var calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
            // month is 0-based in Calendar
            calendar.set(year, month - 1, day, hrs, min, sec);
            calendar.set(Calendar.MILLISECOND, milliseconds);
            expectedCreated = calendar.getTime();
            var s = String.format("%4d%02d%02d%02d%02d%02d", year, month, day, hrs, min, sec);
            if (milliseconds != 0) {
                s += "." + String.format("%03d", milliseconds);
            }
            create = s;

            calendar.add(GregorianCalendar.SECOND, 1);
            expectedModified = calendar.getTime();
            s = String.format("%4d%02d%02d%02d%02d%02d", year, month, day, hrs, min, sec + 1);
            if (milliseconds != 0) {
                s += "." + String.format("%03d", milliseconds);
            }
            modify = s;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("user.timezone: " + System.getProperty("user.timezone"));
        try (FtpServer server = new FtpServer();
             FtpClient client = FtpClient.create()) {
            (new Thread(server)).start();
            int port = server.getPort();
            var loopback = InetAddress.getLoopbackAddress();
            client.connect(new InetSocketAddress(loopback, port));
            client.enablePassiveMode(true);
            for (var testCase : TestCase.values()) {
                Asserts.assertEQ(testCase.expectedModified, client.getLastModified(testCase.name()),
                        "wrong modified date from MDTM for " + testCase);
            }
            for (var it = client.listFiles(null); it.hasNext(); ) {
                var e = it.next();
                Asserts.assertEQ(TestCase.valueOf(e.getName()).expectedCreated, e.getCreated(),
                        "wrong created date from MLSD for " + e.getName());
                Asserts.assertEQ(TestCase.valueOf(e.getName()).expectedModified, e.getLastModified(),
                        "wrong modified date from MLSD for " + e.getName());
            }
        }
    }

    private static class FtpServer implements AutoCloseable, Runnable {
        private final ServerSocket serverSocket;
        private final ServerSocket pasv;

        FtpServer() throws IOException {
            var loopback = InetAddress.getLoopbackAddress();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(loopback, 0));
            pasv = new ServerSocket();
            pasv.bind(new InetSocketAddress(loopback, 0));
        }

        public void handleClient(Socket client) throws IOException {
            String str;

            client.setSoTimeout(2000);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println("220 FTP serverSocket is ready.");
            boolean done = false;
            while (!done) {
                try {
                    str = in.readLine();
                } catch (SocketException e) {
                    done = true;
                    continue;
                }
                String cmd = str.substring(0, str.indexOf(" ") > 0 ? str.indexOf(" ") : str.length());
                String args = (cmd.equals(str)) ? "" : str.substring(str.indexOf(" ") + 1);
                System.err.println("C> " + str);
                switch (cmd) {
                    case "QUIT":
                        out.println("221 Goodbye.");
                        System.err.println("S> 221");
                        done = true;
                        break;
                    case "MDTM": {
                        var testCase = TestCase.valueOf(args);
                        out.println("213 " + testCase.modify);
                        System.err.println("S> 213");
                        break;
                    }
                    case "MLSD":
                        try (var socket = pasv.accept();
                             var dout = new PrintWriter(socket.getOutputStream(), true)) {
                            out.println("150 MLSD start");
                            System.err.println("S> 150");
                            for (var testCase : TestCase.values()) {
                                dout.printf("modify=%s;create=%s; %s%n",
                                            testCase.modify, testCase.create, testCase.name());
                            }
                        }
                        out.println("226 MLSD done.");
                        System.err.println("S> 226");
                        break;
                    case "EPSV":
                        if ("all".equalsIgnoreCase(args)) {
                            out.println("200 EPSV ALL command successful.");
                            System.err.println("S> 200");
                            continue;
                        }
                        out.println("229 Entering Extended Passive Mode (|||" + pasv.getLocalPort() + "|)");
                        System.err.println("S> 229");
                        break;
                    default:
                        System.err.println("S> 500");
                        out.println("500 unsupported command: " + str);
                }
            }
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public void close() throws IOException {
            serverSocket.close();
            pasv.close();
        }

        @Override
        public void run() {
            try (Socket client = serverSocket.accept()) {
                handleClient(client);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException("Problem in test execution", t);
            }
        }
    }
}
