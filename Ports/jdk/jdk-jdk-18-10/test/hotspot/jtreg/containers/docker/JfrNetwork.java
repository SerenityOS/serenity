/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.process.OutputAnalyzer;


// This class is intended to run inside a container
public class JfrNetwork {
    // use a unique hostname for container
    public static final String HOST_NAME = "container-unique-8221711";
    public static final String JFR_REPORTED_CONTAINER_HOSTNAME_TAG = "jfr_reported_container_hostname=";

    public static void main(String[] args) throws Exception {
        String event = args[0];
        try (ServerSocket ss = new ServerSocket()) {
            testNetworkInfo(ss, event);
        }
    }

    private static void assertTrue(boolean expr, String msg) {
        if (!expr) {
            throw new RuntimeException(msg);
        }
    }

    private static void testNetworkInfo(ServerSocket ss, String event) throws Exception {
        ServerSocketListener server = new ServerSocketListener(ss);
        server.start();
        SocketWriter writer = new SocketWriter(ss.getLocalSocketAddress());

        // setup and start the recording
        String recordingPath = event + ".jfr";
        log("========= Recording event: " + event);
        Recording r = new Recording();
        r.enable(event);
        r.setDestination(Paths.get("/", "tmp", recordingPath));
        r.start();

        // start the socker writer thread, write some data into the socket
        writer.start();

        // wait for writer thread to terminate, then for server thread, then stop recording
        writer.joinAndThrow();
        server.joinAndThrow();
        r.stop();

        // analyze the recording
        List<RecordedEvent> events = RecordingFile.readAllEvents(r.getDestination());
        events.forEach(e -> log ("event = " + e));
        assertTrue(!events.isEmpty(), "No recorded network events");
        RecordedEvent e = events.get(0);
        log(JFR_REPORTED_CONTAINER_HOSTNAME_TAG + e.getString("host"));

        // compare IP addresses
        boolean matchFound = false;
        InetAddress reportedByJfr = InetAddress.getByName(e.getString("address"));
        for (InetAddress ip : getLocalIp()) {
            if (ip.equals(reportedByJfr)) {
                matchFound = true;
                break;
            }
        }
        assertTrue(matchFound, "IP address match not found");
    }

    private static List<InetAddress> getLocalIp() throws Exception {
        List<InetAddress> addrs = new ArrayList<>();
        InetAddress localHost = InetAddress.getLocalHost();
        if (!localHost.isLoopbackAddress()) {
            addrs.add(localHost);
        }

        log("getLocalIp() returning:");
        for (InetAddress addr : addrs) {
            log(addr.getHostName());
            log(addr.getHostAddress());
        }

        return addrs;
    }

    private static void log(String msg) {
        System.out.println(msg);
    }


    private static class ServerSocketListener extends Thread {
        Exception exception;
        ServerSocket ss;

        ServerSocketListener(ServerSocket socket) throws Exception {
            ss = socket;
            ss.setReuseAddress(true);
            ss.bind(null);
            log("ServerSocker Local Address: " + ss.getLocalSocketAddress());
        }

        public void joinAndThrow() throws Exception {
            join();
            if (exception != null) {
                throw exception;
            }
        }

        public void run() {
            try {
                try (Socket s = ss.accept(); InputStream is = s.getInputStream()) {
                    System.out.println("ServerSocketListener: accepted socket connection: s = " + s);
                    is.read();
                    is.read();
                    is.read();
                }
            } catch (Exception e) {
                exception = e;
            }
        }
    }


    private static class SocketWriter extends Thread {
        Exception exception;
        private SocketAddress ssAddr;

        public SocketWriter(SocketAddress sa) {
            this.ssAddr = sa;
            System.out.println("SocketWriter(): sa = " + sa);
        }

        public void joinAndThrow() throws Exception {
            join();
            if (exception != null) {
                throw exception;
            }
        }

        public void run() {
            try (Socket s = new Socket()) {
                s.connect(ssAddr);
                try (OutputStream os = s.getOutputStream()) {
                    os.write('A');
                    os.write('B');
                    os.write('C');
                }
            } catch (Exception e) {
                exception = e;
            }
        }
    }

}
