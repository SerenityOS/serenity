/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194298
 * @summary Add support for per Socket configuration of TCP keepalive
 * @modules jdk.net
 * @run main TcpKeepAliveTest
 */
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import jdk.net.ExtendedSocketOptions;

public class TcpKeepAliveTest {

    private static final int DEFAULT_KEEP_ALIVE_PROBES = 7;
    private static final int DEFAULT_KEEP_ALIVE_TIME = 1973;
    private static final int DEFAULT_KEEP_ALIVE_INTVL = 53;

    public static void main(String args[]) throws IOException {
        var loopback = InetAddress.getLoopbackAddress();
        try (ServerSocket ss = boundServer(loopback);
                Socket s = new Socket(loopback, ss.getLocalPort());
                DatagramSocket ds = new DatagramSocket(0);
                MulticastSocket mc = new MulticastSocket(0)) {
            if (ss.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPIDLE)) {
                ss.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, DEFAULT_KEEP_ALIVE_TIME);
                if (ss.getOption(ExtendedSocketOptions.TCP_KEEPIDLE) != DEFAULT_KEEP_ALIVE_TIME) {
                    throw new RuntimeException("Test failed, TCP_KEEPIDLE should have been " + DEFAULT_KEEP_ALIVE_TIME);
                }
            }
            if (ss.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPCOUNT)) {
                ss.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, DEFAULT_KEEP_ALIVE_PROBES);
                if (ss.getOption(ExtendedSocketOptions.TCP_KEEPCOUNT) != DEFAULT_KEEP_ALIVE_PROBES) {
                    throw new RuntimeException("Test failed, TCP_KEEPCOUNT should have been " + DEFAULT_KEEP_ALIVE_PROBES);
                }
            }
            if (ss.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPINTERVAL)) {
                ss.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, DEFAULT_KEEP_ALIVE_INTVL);
                if (ss.getOption(ExtendedSocketOptions.TCP_KEEPINTERVAL) != DEFAULT_KEEP_ALIVE_INTVL) {
                    throw new RuntimeException("Test failed, TCP_KEEPINTERVAL should have been " + DEFAULT_KEEP_ALIVE_INTVL);
                }
            }
            if (s.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPIDLE)) {
                s.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, DEFAULT_KEEP_ALIVE_TIME);
                if (s.getOption(ExtendedSocketOptions.TCP_KEEPIDLE) != DEFAULT_KEEP_ALIVE_TIME) {
                    throw new RuntimeException("Test failed, TCP_KEEPIDLE should have been " + DEFAULT_KEEP_ALIVE_TIME);
                }
            }
            if (s.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPCOUNT)) {
                s.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, DEFAULT_KEEP_ALIVE_PROBES);
                if (s.getOption(ExtendedSocketOptions.TCP_KEEPCOUNT) != DEFAULT_KEEP_ALIVE_PROBES) {
                    throw new RuntimeException("Test failed, TCP_KEEPCOUNT should have been " + DEFAULT_KEEP_ALIVE_PROBES);
                }
            }
            if (s.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPINTERVAL)) {
                s.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, DEFAULT_KEEP_ALIVE_INTVL);
                if (s.getOption(ExtendedSocketOptions.TCP_KEEPINTERVAL) != DEFAULT_KEEP_ALIVE_INTVL) {
                    throw new RuntimeException("Test failed, TCP_KEEPINTERVAL should have been " + DEFAULT_KEEP_ALIVE_INTVL);
                }
            }
            if (ds.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPCOUNT)) {
                throw new RuntimeException("Test failed, TCP_KEEPCOUNT is applicable"
                        + " for TCP Sockets only.");
            }
            if (ds.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPIDLE)) {
                throw new RuntimeException("Test failed, TCP_KEEPIDLE is applicable"
                        + " for TCP Sockets only.");
            }
            if (ds.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPINTERVAL)) {
                throw new RuntimeException("Test failed, TCP_KEEPINTERVAL is applicable"
                        + " for TCP Sockets only.");
            }
            if (mc.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPCOUNT)) {
                throw new RuntimeException("Test failed, TCP_KEEPCOUNT is applicable"
                        + " for TCP Sockets only");
            }
            if (mc.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPIDLE)) {
                throw new RuntimeException("Test failed, TCP_KEEPIDLE is applicable"
                        + " for TCP Sockets only");
            }
            if (mc.supportedOptions().contains(ExtendedSocketOptions.TCP_KEEPINTERVAL)) {
                throw new RuntimeException("Test failed, TCP_KEEPINTERVAL is applicable"
                        + " for TCP Sockets only");
            }
        }
    }

    private static ServerSocket boundServer(InetAddress address) throws IOException {
        var socketAddress = new InetSocketAddress(address, 0);
        var server = new ServerSocket();
        server.bind(socketAddress);
        return server;
    }
}
