/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

import com.sun.jdi.connect.TransportTimeoutException;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;

/*
 * A transport service based on a TCP connection between the
 * debugger and debugee.
 */

public class SocketTransportService extends TransportService {
    private ResourceBundle messages = null;

    /**
     * The listener returned by startListening encapsulates
     * the ServerSocket.
     */
    static class SocketListenKey extends ListenKey {
        ServerSocket ss;

        SocketListenKey(ServerSocket ss) {
            this.ss = ss;
        }

        ServerSocket socket() {
            return ss;
        }

        /*
         * Returns the string representation of the address that this
         * listen key represents.
         */
        public String address() {
            InetAddress address = ss.getInetAddress();

            /*
             * If bound to the wildcard address then use current local
             * hostname. In the event that we don't know our own hostname
             * then assume that host supports IPv4 and return something to
             * represent the loopback address.
             */
            if (address.isAnyLocalAddress()) {
                try {
                    address = InetAddress.getLocalHost();
                } catch (UnknownHostException uhe) {
                    address = InetAddress.getLoopbackAddress();
                }
            }

            /*
             * Now decide if we return a hostname or IP address. Where possible
             * return a hostname but in the case that we are bound to an
             * address that isn't registered in the name service then we
             * return an address.
             */
            String result;
            String hostname = address.getHostName();
            String hostaddr = address.getHostAddress();
            if (hostname.equals(hostaddr)) {
                if (address instanceof Inet6Address) {
                    result = "[" + hostaddr + "]";
                } else {
                    result = hostaddr;
                }
            } else {
                result = hostname;
            }

            /*
             * Finally return "hostname:port", "ipv4-address:port" or
             * "[ipv6-address]:port".
             */
            return result + ":" + ss.getLocalPort();
        }

        public String toString() {
            return address();
        }
    }

    /**
     * Handshake with the debuggee
     */
    void handshake(Socket s, long timeout) throws IOException {
        s.setSoTimeout((int)timeout);

        byte[] hello = "JDWP-Handshake".getBytes("UTF-8");
        s.getOutputStream().write(hello);

        byte[] b = new byte[hello.length];
        int received = 0;
        while (received < hello.length) {
            int n;
            try {
                n = s.getInputStream().read(b, received, hello.length-received);
            } catch (SocketTimeoutException x) {
                throw new IOException("handshake timeout");
            }
            if (n < 0) {
                s.close();
                throw new IOException("handshake failed - connection prematurally closed");
            }
            received += n;
        }
        for (int i=0; i<hello.length; i++) {
            if (b[i] != hello[i]) {
                throw new IOException("handshake failed - unrecognized message from target VM");
            }
        }

        // disable read timeout
        s.setSoTimeout(0);
    }

    /**
     * No-arg constructor
     */
    public SocketTransportService() {
    }

    /**
     * The name of this transport service
     */
    public String name() {
        return "Socket";
    }

    /**
     * Return localized description of this transport service
     */
    public String description() {
        synchronized (this) {
            if (messages == null) {
                messages = ResourceBundle.getBundle("com.sun.tools.jdi.resources.jdi");
            }
        }
        return messages.getString("socket_transportservice.description");
    }

    /**
     * Return the capabilities of this transport service
     */
    public Capabilities capabilities() {
        return new TransportService.Capabilities() {
            public boolean supportsMultipleConnections() {
                return true;
            }

            public boolean supportsAttachTimeout() {
                return true;
            }

            public boolean supportsAcceptTimeout() {
                return true;
            }

            public boolean supportsHandshakeTimeout() {
                return true;
            }
        };
    }

    private static class HostPort {
        public final String host;
        public final int port;
        private HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * Creates an instance for given URN, which can be either <port> or <host>:<port>.
         * If host is '*', the returned HostPort instance has host set to null.
         * If <code>host</code> is a literal IPv6 address, it may be in square brackets.
         */
        public static HostPort parse(String hostPort) {
            int splitIndex = hostPort.lastIndexOf(':');

            int port;
            try {
                port = Integer.decode(hostPort.substring(splitIndex + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("unable to parse port number in address");
            }
            if (port < 0 || port > 0xFFFF) {
                throw new IllegalArgumentException("port out of range");
            }

            if (splitIndex <= 0) {  // empty host means local connection
                return new HostPort(InetAddress.getLoopbackAddress().getHostAddress(), port);
            } else if (splitIndex == 1 && hostPort.charAt(0) == '*') {
                return new HostPort(null, port);
            } else if (hostPort.charAt(0) == '[' && hostPort.charAt(splitIndex - 1) == ']') {
                return new HostPort(hostPort.substring(1, splitIndex - 1), port);
            } else {
                return new HostPort(hostPort.substring(0, splitIndex), port);
            }
        }
    }

    /**
     * Attach to the specified address with optional attach and handshake
     * timeout.
     */
    public Connection attach(String address, long attachTimeout, long handshakeTimeout)
        throws IOException {

        if (address == null) {
            throw new NullPointerException("address is null");
        }
        if (attachTimeout < 0 || handshakeTimeout < 0) {
            throw new IllegalArgumentException("timeout is negative");
        }

        HostPort hostPort = HostPort.parse(address);

        // open TCP connection to VM
        // formally "*" is not correct hostname to attach
        // but lets connect to localhost
        InetSocketAddress sa = new InetSocketAddress(hostPort.host == null
                                                     ? InetAddress.getLoopbackAddress().getHostAddress()
                                                     : hostPort.host, hostPort.port);
        Socket s = new Socket();
        try {
            s.connect(sa, (int)attachTimeout);
        } catch (SocketTimeoutException exc) {
            try {
                s.close();
            } catch (IOException x) { }
            throw new TransportTimeoutException("timed out trying to establish connection");
        }

        // handshake with the target VM
        try {
            handshake(s, handshakeTimeout);
        } catch (IOException exc) {
            try {
                s.close();
            } catch (IOException x) { }
            throw exc;
        }

        return new SocketConnection(s);
    }

    /*
     * Listen on the specified address and port. Return a listener
     * that encapsulates the ServerSocket.
     */
    ListenKey startListening(String localaddress, int port) throws IOException {
        InetSocketAddress sa;
        if (localaddress == null) {
            sa = new InetSocketAddress(port);
        } else {
            sa = new InetSocketAddress(localaddress, port);
        }
        ServerSocket ss = new ServerSocket();
        if (port == 0) {
            // Only need SO_REUSEADDR if we're using a fixed port. If we
            // start seeing EADDRINUSE due to collisions in free ports
            // then we should retry the bind() a few times.
            ss.setReuseAddress(false);
        }
        ss.bind(sa);
        return new SocketListenKey(ss);
    }

    /**
     * Listen on the specified address
     */
    public ListenKey startListening(String address) throws IOException {
        // use ephemeral port if address isn't specified.
        HostPort hostPort = HostPort.parse((address == null || address.isEmpty()) ? "0" : address);
        return startListening(hostPort.host, hostPort.port);
    }

    /**
     * Listen on the default address
     */
    public ListenKey startListening() throws IOException {
        return startListening(null);
    }

    /**
     * Stop the listener
     */
    public void stopListening(ListenKey listener) throws IOException {
        if (!(listener instanceof SocketListenKey)) {
            throw new IllegalArgumentException("Invalid listener");
        }

        synchronized (listener) {
            ServerSocket ss = ((SocketListenKey)listener).socket();

            // if the ServerSocket has been closed it means
            // the listener is invalid
            if (ss.isClosed()) {
                throw new IllegalArgumentException("Invalid listener");
            }
            ss.close();
        }
    }

    /**
     * Accept a connection from a debuggee and handshake with it.
     */
    public Connection accept(ListenKey listener, long acceptTimeout, long handshakeTimeout) throws IOException {
        if (acceptTimeout < 0 || handshakeTimeout < 0) {
            throw new IllegalArgumentException("timeout is negative");
        }
        if (!(listener instanceof SocketListenKey)) {
            throw new IllegalArgumentException("Invalid listener");
        }
        ServerSocket ss;

        // obtain the ServerSocket from the listener - if the
        // socket is closed it means the listener is invalid
        synchronized (listener) {
            ss = ((SocketListenKey)listener).socket();
            if (ss.isClosed()) {
               throw new IllegalArgumentException("Invalid listener");
            }
        }

        // from here onwards it's possible that the ServerSocket
        // may be closed by a call to stopListening - that's okay
        // because the ServerSocket methods will throw an
        // IOException indicating the socket is closed.
        //
        // Additionally, it's possible that another thread calls accept
        // with a different accept timeout - that creates a same race
        // condition between setting the timeout and calling accept.
        // As it is such an unlikely scenario (requires both threads
        // to be using the same listener we've chosen to ignore the issue).

        ss.setSoTimeout((int)acceptTimeout);
        Socket s;
        try {
            s = ss.accept();
        } catch (SocketTimeoutException x) {
            throw new TransportTimeoutException("timeout waiting for connection");
        }

        // handshake here
        handshake(s, handshakeTimeout);

        return new SocketConnection(s);
    }

    public String toString() {
       return name();
    }
}
