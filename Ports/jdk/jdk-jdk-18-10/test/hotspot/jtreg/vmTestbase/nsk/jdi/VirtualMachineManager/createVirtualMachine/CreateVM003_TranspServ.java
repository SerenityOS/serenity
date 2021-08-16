/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.VirtualMachineManager.createVirtualMachine;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;
import java.net.*;
import java.io.*;
import java.util.*;

/*
 * A transport service implementation based on a TCP connection used by
 * nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM003 test.
 * It is borrowed from the com.sun.tools.jdi.SocketTransportService
 */

public class CreateVM003_TranspServ extends TransportService {

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

        public String address() {
            InetAddress localaddr = ss.getInetAddress();
            int port = ss.getLocalPort();
            if (localaddr.isAnyLocalAddress()) {
                return "" + port;
            } else {
                return localaddr + ":" + port;
            }
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
                throw new IOException("##> CreateVM003_TranspServ: Handshake timeout");
            }
            if (n < 0) {
                s.close();
                throw new IOException
                    ("##> CreateVM003_TranspServ: Handshake FAILED - connection prematurally closed!");
            }
            received += n;
        }
        for (int i=0; i<hello.length; i++) {
            if (b[i] != hello[i]) {
                throw new IOException
                    ("##> CreateVM003_TranspServ: Handshake FAILED - unrecognized message from target VM");
            }
        }

        // disable read timeout
        s.setSoTimeout(0);
    }

    /**
     * No-arg constructor
     */
    public CreateVM003_TranspServ() {
    }

    /**
     * The name of this transport service
     */
    public String name() {
        return "CreateVM003_TranspServ";
    }

    /**
     * The description of this transport service
     */
    public String description() {
        return "SocketTransportService for nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM003 test.";
    }

    /**
     * Return the capabilities of this transport service
     */
    public Capabilities capabilities() {
        return new CreateVM003_TranspServCapabilities();
    }


    /**
     * Attach to the specified address with optional attach and handshake
     * timeout.
     */
    public Connection attach(String address, long attachTimeout, long handshakeTimeout)
        throws IOException {

        if (address == null) {
            throw new NullPointerException("##> CreateVM003_TranspServ: attach() - address is null");
        }
        if (attachTimeout < 0 || handshakeTimeout < 0) {
            throw new IllegalArgumentException("##> CreateVM003_TranspServ: attach() - timeout is negative");
        }

        int splitIndex = address.indexOf(':');
        String host;
        String portStr;
        if (splitIndex < 0) {
            host = InetAddress.getLocalHost().getHostName();
            portStr = address;
        } else {
            host = address.substring(0, splitIndex);
            portStr = address.substring(splitIndex+1);
        }

        int port;
        try {
            port = Integer.decode(portStr).intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "##> CreateVM003_TranspServ: attach() - unable to parse port number in address");
        }


        // open TCP connection to VM

        InetSocketAddress sa = new InetSocketAddress(host, port);
        Socket s = new Socket();
        try {
            s.connect(sa, (int)attachTimeout);
        } catch (SocketTimeoutException exc) {
            try {
                s.close();
            } catch (IOException x) { }
            throw new TransportTimeoutException
                ("##> CreateVM003_TranspServ: attach() - timed out trying to establish connection");
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

        return new CreateVM003_Connection(s);
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
        // if port is 0 do not set the SO_REUSEADDR flag
        if (port == 0) {
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
        if (address == null || address.length() == 0) {
            address = "0";
        }

        int splitIndex = address.indexOf(':');
        String localaddr = null;
        if (splitIndex >= 0) {
            localaddr = address.substring(0, splitIndex);
            address = address.substring(splitIndex+1);
        }

        int port;
        try {
            port = Integer.decode(address).intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "##> CreateVM003_TranspServ: startListening() - unable to parse port number in address");
        }

        return startListening(localaddr, port);
    }

    /**
     * Listen on the default address
     */
    public ListenKey startListening() throws IOException {
        return startListening(null, 0);
    }

    /**
     * Stop the listener
     */
    public void stopListening(ListenKey listener) throws IOException {
        if (!(listener instanceof SocketListenKey)) {
            throw new IllegalArgumentException
                ("##> CreateVM003_TranspServ: stopListening() - Invalid listener");
        }

        synchronized (listener) {
            ServerSocket ss = ((SocketListenKey)listener).socket();

            // if the ServerSocket has been closed it means
            // the listener is invalid
            if (ss.isClosed()) {
                throw new IllegalArgumentException
                    ("##> CreateVM003_TranspServ: stopListening() - Invalid listener");
            }
            ss.close();
        }
    }

    /**
     * Accept a connection from a debuggee and handshake with it.
     */
    public Connection accept(ListenKey listener, long acceptTimeout, long handshakeTimeout) throws IOException {
        if (acceptTimeout < 0 || handshakeTimeout < 0) {
            throw new IllegalArgumentException
                ("##> CreateVM003_TranspServ: accept() - timeout is negative");
        }
        if (!(listener instanceof SocketListenKey)) {
            throw new IllegalArgumentException
                ("##> CreateVM003_TranspServ: accept() - Invalid listener");
        }
        ServerSocket ss;

        // obtain the ServerSocket from the listener - if the
        // socket is closed it means the listener is invalid
        synchronized (listener) {
            ss = ((SocketListenKey)listener).socket();
            if (ss.isClosed()) {
               throw new IllegalArgumentException
                   ("##> CreateVM003_TranspServ: accept() - Invalid listener");
            }
        }

        ss.setSoTimeout((int)acceptTimeout);
        Socket s;
        try {
            s = ss.accept();
        } catch (SocketTimeoutException x) {
            throw new TransportTimeoutException
                ("##> CreateVM003_TranspServ: accept() - timeout waiting for connection");
        }

        // handshake here
        handshake(s, handshakeTimeout);

        return new CreateVM003_Connection(s);
    }

    public String toString() {
       return name();
    }
} // end of CreateVM003_TranspServ class

class CreateVM003_Connection extends Connection {
    private Socket socket;
    private boolean closed = false;
    private OutputStream socketOutput;
    private InputStream socketInput;
    private Object receiveLock = new Object();
    private Object sendLock = new Object();
    private Object closeLock = new Object();

    CreateVM003_Connection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true);
        socketInput = socket.getInputStream();
        socketOutput = socket.getOutputStream();
    }

    public void close() throws IOException {
        synchronized (closeLock) {
           if (closed) {
                return;
           }
           socketOutput.close();
           socketInput.close();
           socket.close();
           closed = true;
        }
    }

    public boolean isOpen() {
        synchronized (closeLock) {
            return !closed;
        }
    }

    public byte[] readPacket() throws IOException {
        if (!isOpen()) {
            throw new ClosedConnectionException
                ("##> CreateVM003_Connection: readPacket() - connection is closed");
        }
        synchronized (receiveLock) {
            int b1,b2,b3,b4;

            // length
            try {
                b1 = socketInput.read();
                b2 = socketInput.read();
                b3 = socketInput.read();
                b4 = socketInput.read();
            } catch (IOException ioe) {
                if (!isOpen()) {
                    throw new ClosedConnectionException
                        ("##> CreateVM003_Connection: readPacket() - connection is closed");
                } else {
                    throw ioe;
                }
            }

            if (b1<0 || b2<0 || b3<0 || b4<0)
                throw new EOFException();

            int len = ((b1 << 24) | (b2 << 16) | (b3 << 8) | (b4 << 0));

            if (len < 0) {
                throw new IOException
                    ("##> CreateVM003_Connection: readPacket() - protocol error: invalid Packet's length");
            }

            byte b[] = new byte[len];
            b[0] = (byte)b1;
            b[1] = (byte)b2;
            b[2] = (byte)b3;
            b[3] = (byte)b4;

            int off = 4;
            len -= off;

            while (len > 0) {
                int count;
                try {
                    count = socketInput.read(b, off, len);
                } catch (IOException ioe) {
                    if (!isOpen()) {
                        throw new ClosedConnectionException
                            ("##> CreateVM003_Connection: readPacket() - connection is closed");
                    } else {
                        throw ioe;
                    }
                }
                if (count < 0) {
                    throw new EOFException
                        ("##> CreateVM003_Connection: readPacket() - read() method returns negative value");
                }
                len -= count;
                off += count;
            }

            return b;
        }
    }

    public void writePacket(byte b[]) throws IOException {
        if (!isOpen()) {
            throw new ClosedConnectionException
                ("##> CreateVM003_Connection: writePacket() - connection is closed");
        }

        /*
         * Check the packet size
         */
        if (b.length < 11) {
            throw new IllegalArgumentException
                ("##> CreateVM003_Connection: writePacket() - packet is insufficient size");
        }
        int b0 = b[0] & 0xff;
        int b1 = b[1] & 0xff;
        int b2 = b[2] & 0xff;
        int b3 = b[3] & 0xff;
        int len = ((b0 << 24) | (b1 << 16) | (b2 << 8) | (b3 << 0));
        if (len < 11) {
            throw new IllegalArgumentException
                ("##> CreateVM003_Connection: writePacket() - packet is insufficient size");
        }

        /*
         * Check that the byte array contains the complete packet
         */
        if (len > b.length) {
            throw new IllegalArgumentException
                ("##> CreateVM003_Connection: writePacket() - length mis-match");
        }

        synchronized (sendLock) {
            try {
                /*
                 * Send the packet (ignoring any bytes that follow
                 * the packet in the byte array).
                 */
                socketOutput.write(b, 0, len);
            } catch (IOException ioe) {
                if (!isOpen()) {
                    throw new ClosedConnectionException
                        ("##> CreateVM003_Connection: writePacket() - connection is closed");
                } else {
                    throw ioe;
                }
            }
        }
    }

} // end of CreateVM003_Connection class

/*
 * The capabilities of the socket transport service
 */
class CreateVM003_TranspServCapabilities extends TransportService.Capabilities {

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

} // end of CreateVM003_TranspServCapabilities class
