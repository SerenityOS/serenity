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
 * nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM002 test.
 * It is borrowed from the com.sun.tools.jdi.SocketTransportService
 */

public class CreateVM002_TranspServ extends TransportService {

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
                throw new IOException("##> CreateVM002_TranspServ: Handshake timeout");
            }
            if (n < 0) {
                s.close();
                throw new IOException
                    ("##> CreateVM002_TranspServ: Handshake FAILED - connection prematurally closed!");
            }
            received += n;
        }
        for (int i=0; i<hello.length; i++) {
            if (b[i] != hello[i]) {
                throw new IOException
                    ("##> CreateVM002_TranspServ: Handshake FAILED - unrecognized message from target VM");
            }
        }

        // disable read timeout
        s.setSoTimeout(0);
    }

    /**
     * No-arg constructor
     */
    public CreateVM002_TranspServ() {
    }

    /**
     * The name of this transport service
     */
    public String name() {
        return "CreateVM002_TranspServ";
    }

    /**
     * The description of this transport service
     */
    public String description() {
        return "SocketTransportService for nsk/jdi/VirtualMachineManager/createVirtualMachine/createVM002 test.";
    }

    /**
     * Return the capabilities of this transport service
     */
    public Capabilities capabilities() {
        return new CreateVM002_TranspServCapabilities();
    }


    /**
     * Attach to the specified address with optional attach and handshake
     * timeout.
     */
    public Connection attach(String address, long attachTimeout, long handshakeTimeout)
        throws IOException {

        if (address == null) {
            throw new NullPointerException("##> CreateVM002_TranspServ: attach() - address is null");
        }
        if (attachTimeout < 0 || handshakeTimeout < 0) {
            throw new IllegalArgumentException("##> CreateVM002_TranspServ: attach() - timeout is negative");
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
                "##> CreateVM002_TranspServ: attach() - unable to parse port number in address");
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
                ("##> CreateVM002_TranspServ: attach() - timed out trying to establish connection");
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

        return new CreateVM002_Connection(s);
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
                    "##> CreateVM002_TranspServ: startListening() - unable to parse port number in address");
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
                ("##> CreateVM002_TranspServ: stopListening() - Invalid listener");
        }

        synchronized (listener) {
            ServerSocket ss = ((SocketListenKey)listener).socket();

            // if the ServerSocket has been closed it means
            // the listener is invalid
            if (ss.isClosed()) {
                throw new IllegalArgumentException
                    ("##> CreateVM002_TranspServ: stopListening() - Invalid listener");
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
                ("##> CreateVM002_TranspServ: accept() - timeout is negative");
        }
        if (!(listener instanceof SocketListenKey)) {
            throw new IllegalArgumentException
                ("##> CreateVM002_TranspServ: accept() - Invalid listener");
        }
        ServerSocket ss;

        // obtain the ServerSocket from the listener - if the
        // socket is closed it means the listener is invalid
        synchronized (listener) {
            ss = ((SocketListenKey)listener).socket();
            if (ss.isClosed()) {
               throw new IllegalArgumentException
                   ("##> CreateVM002_TranspServ: accept() - Invalid listener");
            }
        }

        ss.setSoTimeout((int)acceptTimeout);
        Socket s;
        try {
            s = ss.accept();
        } catch (SocketTimeoutException x) {
            throw new TransportTimeoutException
                ("##> CreateVM002_TranspServ: accept() - timeout waiting for connection");
        }

        // handshake here
        handshake(s, handshakeTimeout);

        return new CreateVM002_Connection(s);
    }

    public String toString() {
       return name();
    }
} // end of CreateVM002_TranspServ class

class CreateVM002_Connection extends Connection {
    private Socket socket;
    private boolean closed = false;
    private OutputStream socketOutput;
    private InputStream socketInput;
    private Object receiveLock = new Object();
    private Object sendLock = new Object();
    private Object closeLock = new Object();
    private boolean toPrintPacket = false;
    private int readPacketRequestNumber = 0;
    private int writePacketRequestNumber = 0;
    public boolean wasIOException = false;

    CreateVM002_Connection(Socket socket) throws IOException {
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
                ("##> CreateVM002_Connection: readPacket() - connection is closed: N1");
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
                        ("##> CreateVM002_Connection: readPacket() - connection is closed: N2");
                } else {
                    throw ioe;
                }
            }

            if (b1<0 || b2<0 || b3<0 || b4<0)
                throw new EOFException();

            int len = ((b1 << 24) | (b2 << 16) | (b3 << 8) | (b4 << 0));

            if (len < 0) {
                throw new IOException
                    ("##> CreateVM002_Connection: readPacket() - protocol error: invalid Packet's length");
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
                            ("##> CreateVM002_Connection: readPacket() - connection is closed: N3");
                    } else {
                        throw ioe;
                    }
                }
                if (count < 0) {
                    throw new EOFException
                        ("##> CreateVM002_Connection: readPacket() - read() method returns negative value");
                }
                len -= count;
                off += count;
            }

            readPacketRequestNumber++;
            if ( readPacketRequestNumber == -1 ) {
                throw new IOException
                    ("Dummy IOException in CreateVM002_Connection.readPacket(); readPacketRequestNumber = "
                    + readPacketRequestNumber);
            }
            printPacket("readPacket:", b);
            return b;
        }
    }

    public void writePacket(byte b[]) throws IOException {
        writePacketRequestNumber++;
        if ( writePacketRequestNumber == 2 ) {
            wasIOException = true;
            throw new IOException
                ("Dummy IOException in CreateVM002_Connection.writePacket(); writePacketRequestNumber = "
                + writePacketRequestNumber);
        }
        if (!isOpen()) {
            throw new ClosedConnectionException
                ("##> CreateVM002_Connection: writePacket() - connection is closed: N1");
        }

        printPacket("writePacket:", b);

        /*
         * Check the packet size
         */
        if (b.length < 11) {
            throw new IllegalArgumentException
                ("##> CreateVM002_Connection: writePacket() - packet is insufficient size: N1");
        }
        int b0 = b[0] & 0xff;
        int b1 = b[1] & 0xff;
        int b2 = b[2] & 0xff;
        int b3 = b[3] & 0xff;
        int len = ((b0 << 24) | (b1 << 16) | (b2 << 8) | (b3 << 0));
        if (len < 11) {
            throw new IllegalArgumentException
                ("##> CreateVM002_Connection: writePacket() - packet is insufficient size: N2");
        }

        /*
         * Check that the byte array contains the complete packet
         */
        if (len > b.length) {
            throw new IllegalArgumentException
                ("##> CreateVM002_Connection: writePacket() - length mis-match");
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
                        ("##> CreateVM002_Connection: writePacket() - connection is closed: N2");
                } else {
                    throw ioe;
                }
            }
        }
    }

    public void toPrintPacket(boolean toPrint) {

        if ( toPrintPacket ) {
            if ( ! toPrint ) {
                toPrintPacket = false;
                System.out.println("\n>>>> CreateVM002_Connection: toPrintPacket - Off! "
                    + currentDateWithMlsecs());
            }
        } else {
            if ( toPrint ) {
                toPrintPacket = true;
                System.out.println("\n>>>> CreateVM002_Connection: toPrintPacket - On! "
                    + currentDateWithMlsecs());
            }
        }

    }

    String currentDateWithMlsecs() {
        GregorianCalendar calendar = new GregorianCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        String strMonth = month > 9 ? "" + month : "0" + month;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String strDay = day > 9 ? "" + day : "0" + day;
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        String strHours = hours > 9 ? "" + hours : "0" + hours;
        int minutes = calendar.get(Calendar.MINUTE);
        String strMinutes = minutes > 9 ? "" + minutes : "0" + minutes;
        int seconds = calendar.get(Calendar.SECOND);
        String strSeconds = seconds > 9 ? "" + seconds : "0" + seconds;
        int mlsecs = (int)(calendar.getTimeInMillis() % 1000);
        String strMlsecs = mlsecs < 10 ? "00" + mlsecs : (mlsecs < 100 ? "0" + mlsecs : "" + mlsecs);
        return "" + year + "." + strMonth + "." + strDay + "; " + strHours
            + ":" + strMinutes + ":" + strSeconds + "::" + strMlsecs;
    }

    public void printPacket(String headMessage, byte b[]) {

        if ( ! toPrintPacket ) {
            return;
        }

        System.out.println("\n>>>> CreateVM005_Connection: printPacket: " + currentDateWithMlsecs());
        System.out.println("  >> " + headMessage);
        int  packetLength = b.length;

        String handsHake = "JDWP-Handshake";

        System.out.println("  >> packet length = " + packetLength);
        if ( packetLength < 11 ) {
            System.out.println("  >> Invalid packet length!");
            System.out.println("\n>>>> CreateVM005_Connection: printPacket - END\n");
            return;
        }

        String packetString = new String(b);
        if ( handsHake.equals(packetString) ) {
            System.out.println("  >> Packet as String = " + packetString);
            System.out.println("\n>>>> CreateVM005_Connection: printPacket - END\n");
            return;
        }

        int b0 = b[0] & 0xff;
        int b1 = b[1] & 0xff;
        int b2 = b[2] & 0xff;
        int b3 = b[3] & 0xff;
        int lengthField = ((b0 << 24) | (b1 << 16) | (b2 << 8) | (b3 << 0));
        System.out.println("  >> lengthField = " + lengthField);

        int b4 = b[4] & 0xff;
        int b5 = b[5] & 0xff;
        int b6 = b[6] & 0xff;
        int b7 = b[7] & 0xff;
        int idField = ((b4 << 24) | (b5 << 16) | (b6 << 8) | (b7 << 0));
        System.out.println("  >> idField(integer) = " + idField);

        int flagsField = b[8] & 0xff;
        System.out.println("  >> flagsField(integer) = " + flagsField);
        int replyPacket = b[8] & 0x80;
        if (  replyPacket != 0 ) {
            System.out.println("  >> Replay Packet:");
            int b9 = b[9] & 0xff;
            int b10 = b[10] & 0xff;
            int errorCodeField = ((b9 << 8) | (b10 << 0));
            System.out.println("  >> errorCodeField(integer) = " + errorCodeField);
        } else {
            System.out.println("  >> Command Packet:");
            int commandSetField = b[9] & 0xff;
            System.out.println("  >> commandSetField(integer) = " + commandSetField);
            int commandField = b[10] & 0xff;
            System.out.println("  >> commandField(integer) = " + commandField);
        }

        if ( packetLength > 11 ) {
            int dataLength = packetLength-11;
            String data = "";
            for (int i=0; i < dataLength; i++) {
                data = data + "," + (int)(b[11+i] & 0xff);
            }
            System.out.println("  >> Packet's data = " + data);
        }
        System.out.println(">>>> CreateVM005_Connection: printPacket - END");
    }

} // end of CreateVM002_Connection class

/*
 * The capabilities of the socket transport service
 */
class CreateVM002_TranspServCapabilities extends TransportService.Capabilities {

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

} // end of CreateVM002_TranspServCapabilities class
