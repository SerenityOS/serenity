/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.util.HexDumpEncoder;

import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * A DNS UDP message tracer.
 *
 * It listens for DNS UDP requests, forward request to real DNS server, receives
 * response message and sends back to requester, at same time dump all messages
 * into capture file
 *
 * The capture file contains an DNS protocol exchange in the hexadecimal
 * dump format emitted by HexDumpEncoder:
 *
 * xxxx: 00 11 22 33 44 55 66 77   88 99 aa bb cc dd ee ff  ................
 *
 * Typically, the capture file data will be used by DNSServer for playback
 */
public class DNSTracer extends Thread implements Server {
    public static final int DNS_DEFAULT_PORT = 53;
    public static final int DNS_PACKET_SIZE = 512;
    static HexDumpEncoder encoder = new HexDumpEncoder();

    private DatagramSocket inSocket;
    private SocketAddress dnsServerAddress;
    private ByteBuffer reqBuffer = ByteBuffer.allocate(DNS_PACKET_SIZE);
    private ByteBuffer resBuffer = ByteBuffer.allocate(DNS_PACKET_SIZE);
    private PrintStream out = null;
    private volatile boolean isRunning;

    public DNSTracer(String dnsHostname) throws SocketException {
        this(dnsHostname, DNS_DEFAULT_PORT);
    }

    public DNSTracer(PrintStream outStream, String dnsHostname)
            throws SocketException {
        this(outStream, dnsHostname, DNS_DEFAULT_PORT);
    }

    public DNSTracer(String dnsHostname, int dnsPort) throws SocketException {
        this(System.out, dnsHostname, dnsPort);
    }

    public DNSTracer(PrintStream outStream, String dnsHostname, int dnsPort)
            throws SocketException {
        inSocket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        out = outStream;
        dnsServerAddress = new InetSocketAddress(dnsHostname, dnsPort);
    }

    public void run() {
        isRunning = true;
        System.out.println(
                "DNSTracer: listening on port " + inSocket.getLocalPort());

        System.out.println("DNSTracer: will forward request to server "
                + dnsServerAddress);

        try (DatagramSocket outSocket = new DatagramSocket()) {
            while (true) {
                DatagramPacket reqPacket = new DatagramPacket(reqBuffer.array(),
                        reqBuffer.array().length);
                inSocket.receive(reqPacket);

                out.println("-> " + reqPacket.getSocketAddress());
                out.println();
                // dump dns request data
                out.println(encoder.encodeBuffer(
                        Arrays.copyOf(reqPacket.getData(),
                                reqPacket.getLength())));
                out.println();

                outSocket.send(new DatagramPacket(reqPacket.getData(),
                        reqPacket.getLength(), dnsServerAddress));
                DatagramPacket resPacket = new DatagramPacket(resBuffer.array(),
                        resBuffer.array().length);
                outSocket.receive(resPacket);

                out.println("<- " + resPacket.getSocketAddress());
                out.println();
                // dump dns response data
                out.println(encoder.encodeBuffer(
                        Arrays.copyOf(resPacket.getData(),
                                resPacket.getLength())));
                out.println();

                inSocket.send(new DatagramPacket(resPacket.getData(),
                        resPacket.getLength(), reqPacket.getSocketAddress()));
            }
        } catch (SocketException se) {
            if (!isRunning) {
                out.flush();
                System.out.println("DNSTracer: Exit");
            } else {
                se.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void stopServer() {
        isRunning = false;
        if (inSocket != null) {
            try {
                inSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override public int getPort() {
        if (inSocket != null) {
            return inSocket.getLocalPort();
        } else {
            return -1;
        }
    }
}
