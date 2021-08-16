/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import java.io.*;
import java.net.*;
import sun.security.util.IOUtils;

public abstract class NetClient implements AutoCloseable {
    public static NetClient getInstance(String protocol, String hostname, int port,
            int timeout) throws IOException {
        if (protocol.equals("TCP")) {
            return new TCPClient(hostname, port, timeout);
        } else {
            return new UDPClient(hostname, port, timeout);
        }
    }

    public abstract void send(byte[] data) throws IOException;
    public abstract byte[] receive() throws IOException;
    public abstract void close() throws IOException;
}

class TCPClient extends NetClient {

    private Socket tcpSocket;
    private BufferedOutputStream out;
    private BufferedInputStream in;

    TCPClient(String hostname, int port, int timeout)
            throws IOException {
        tcpSocket = new Socket();
        tcpSocket.connect(new InetSocketAddress(hostname, port), timeout);
        out = new BufferedOutputStream(tcpSocket.getOutputStream());
        in = new BufferedInputStream(tcpSocket.getInputStream());
        tcpSocket.setSoTimeout(timeout);
    }

    @Override
    public void send(byte[] data) throws IOException {
        byte[] lenField = new byte[4];
        intToNetworkByteOrder(data.length, lenField, 0, 4);
        out.write(lenField);

        out.write(data);
        out.flush();
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] lenField = new byte[4];
        int count = readFully(lenField, 4);

        if (count != 4) {
            if (Krb5.DEBUG) {
                System.out.println(
                    ">>>DEBUG: TCPClient could not read length field");
            }
            return null;
        }

        int len = networkByteOrderToInt(lenField, 0, 4);
        if (Krb5.DEBUG) {
            System.out.println(
                ">>>DEBUG: TCPClient reading " + len + " bytes");
        }
        if (len <= 0) {
            if (Krb5.DEBUG) {
                System.out.println(
                    ">>>DEBUG: TCPClient zero or negative length field: "+len);
            }
            return null;
        }

        try {
            return IOUtils.readExactlyNBytes(in, len);
        } catch (IOException ioe) {
            if (Krb5.DEBUG) {
                System.out.println(
                    ">>>DEBUG: TCPClient could not read complete packet (" +
                    len + "/" + count + ")");
            }
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        tcpSocket.close();
    }

    /**
     * Read requested number of bytes before returning.
     * @return The number of bytes actually read; -1 if none read
     */
    private int readFully(byte[] inBuf, int total) throws IOException {
        int count, pos = 0;

        while (total > 0) {
            count = in.read(inBuf, pos, total);

            if (count == -1) {
                return (pos == 0? -1 : pos);
            }
            pos += count;
            total -= count;
        }
        return pos;
    }

    /**
     * Returns the integer represented by 4 bytes in network byte order.
     */
    private static int networkByteOrderToInt(byte[] buf, int start,
        int count) {
        if (count > 4) {
            throw new IllegalArgumentException(
                "Cannot handle more than 4 bytes");
        }

        int answer = 0;

        for (int i = 0; i < count; i++) {
            answer <<= 8;
            answer |= ((int)buf[start+i] & 0xff);
        }
        return answer;
    }

    /**
     * Encodes an integer into 4 bytes in network byte order in the buffer
     * supplied.
     */
    private static void intToNetworkByteOrder(int num, byte[] buf,
        int start, int count) {
        if (count > 4) {
            throw new IllegalArgumentException(
                "Cannot handle more than 4 bytes");
        }

        for (int i = count-1; i >= 0; i--) {
            buf[start+i] = (byte)(num & 0xff);
            num >>>= 8;
        }
    }
}

class UDPClient extends NetClient {
    InetAddress iaddr;
    int iport;
    int bufSize = 65507;
    DatagramSocket dgSocket;
    DatagramPacket dgPacketIn;

    UDPClient(String hostname, int port, int timeout)
        throws UnknownHostException, SocketException {
        iaddr = InetAddress.getByName(hostname);
        iport = port;
        dgSocket = new DatagramSocket();
        dgSocket.setSoTimeout(timeout);
        dgSocket.connect(iaddr, iport);
    }

    @Override
    public void send(byte[] data) throws IOException {
        DatagramPacket dgPacketOut = new DatagramPacket(data, data.length,
                                                        iaddr, iport);
        dgSocket.send(dgPacketOut);
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] ibuf = new byte[bufSize];
        dgPacketIn = new DatagramPacket(ibuf, ibuf.length);
        try {
            dgSocket.receive(dgPacketIn);
        }
        catch (SocketException e) {
            if (e instanceof PortUnreachableException) {
                throw e;
            }
            dgSocket.receive(dgPacketIn);
        }
        byte[] data = new byte[dgPacketIn.getLength()];
        System.arraycopy(dgPacketIn.getData(), 0, data, 0,
                         dgPacketIn.getLength());
        return data;
    }

    @Override
    public void close() {
        dgSocket.close();
    }
}
