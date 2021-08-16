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
 * @bug 4092038
 * @library /test/lib
 * @summary TCP Urgent data support
 * @run main UrgentDataTest
 * @run main/othervm -Djava.net.preferIPv4Stack=true UrgentDataTest
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;

public class UrgentDataTest {

    Object opref;
    boolean isClient, isServer;
    String  clHost;
    ServerSocket listener;
    Socket client, server;
    int  clPort;
    InputStream clis, sis;
    OutputStream clos, sos;

    static void usage () {
        System.out.println ("   usage: java UrgentDataTest <runs client and server together>");
        System.out.println ("   usage: java UrgentDataTest -server <runs server alone>");
        System.out.println ("   usage: java UrgentDataTest -client host port <runs client and connects to"+
            " specified server>");
    }

    public static void main (String args[]) {
        IPSupport.throwSkippedExceptionIfNonOperational();

        try {
            UrgentDataTest test = new UrgentDataTest ();
            if (args.length == 0) {
                InetAddress loopback = InetAddress.getLoopbackAddress();
                test.listener = new ServerSocket ();
                test.listener.bind(new InetSocketAddress(loopback, 0));
                test.isClient = true;
                test.isServer = true;
                test.clHost = loopback.getHostAddress();
                test.clPort = test.listener.getLocalPort();
                test.run();
            } else if (args[0].equals ("-server")) {
                test.listener = new ServerSocket (0);
                System.out.println ("Server listening on port " + test.listener.getLocalPort());
                test.isClient = false;
                test.isServer = true;
                test.run();
                System.out.println ("Server: Completed OK");
            } else if (args[0].equals ("-client")) {
                test.isClient = true;
                test.isServer = false;
                test.clHost = args [1];
                test.clPort = Integer.parseInt (args[2]);
                test.run();
                System.out.println ("Client: Completed OK");
            } else {
                usage ();
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }
        catch (NumberFormatException e) {
            usage();
        }
        catch (Exception e) {
            e.printStackTrace ();
            throw new RuntimeException ("Exception caught");
        }
    }

    public void run () throws Exception {
        try {
            if (isClient) {
                client = new Socket (clHost, clPort);
                clis = client.getInputStream();
                clos = client.getOutputStream();
                client.setOOBInline (true);
                if (client.getOOBInline() != true) {
                    throw new RuntimeException ("Setting OOBINLINE failed");
                }
            }
            if (isServer) {
                server = listener.accept ();
                sis = server.getInputStream();
                sos = server.getOutputStream();
            }
            if (isClient) {
                clos.write ("Hello".getBytes ());
                client.sendUrgentData (100);
                clos.write ("world".getBytes ());
            }
            // read Hello world from server (during which oob byte must have been dropped)
            String s = "Helloworld";
            if (isServer) {
                for (int y=0; y<s.length(); y++) {
                    int c = sis.read ();
                    if (c != (int)s.charAt (y)) {
                        throw new RuntimeException ("Unexpected character read");
                    }
                }
                // Do the same from server to client
                sos.write ("Hello".getBytes ());
                server.sendUrgentData (101);
                sos.write ("World".getBytes ());
            }
            if (isClient) {
                // read Hello world from client (during which oob byte must have been read)
                s="Hello";
                for (int y=0; y<s.length(); y++) {
                    int c = clis.read ();
                    if (c != (int)s.charAt (y)) {
                        throw new RuntimeException ("Unexpected character read");
                    }
                }
                if (clis.read() != 101) {
                    throw new RuntimeException ("OOB byte not received");
                }
                s="World";
                for (int y=0; y<s.length(); y++) {
                    int c = clis.read ();
                    if (c != (int)s.charAt (y)) {
                        throw new RuntimeException ("Unexpected character read");
                    }
                }
            }
        } finally {
            if (listener != null) listener.close();
            if (client != null) client.close ();
            if (server != null) server.close ();
        }
    }
}
