/*
 * Copyright (c) 1998, 2008, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.zip.*;


public class MultiSocketFactory  {

    private static RMISocketFactory def =
        RMISocketFactory.getDefaultSocketFactory();


    public static class ServerFactory
        implements RMIServerSocketFactory, Serializable
    {

        private String protocol;
        private byte[] data;

        public ServerFactory(String protocol, byte[] data) {
            this.protocol = protocol;
            this.data = data;
        }

        public ServerSocket createServerSocket(int port) throws IOException
        {
            if (protocol.equals("compress")) {
                return new CompressServerSocket(port);

            } else if (protocol.equals("xor")) {
                if (data == null || data.length != 1)
                    throw new IOException("invalid argument for XOR protocol");
                return new XorServerSocket(port, data[0]);

            }

            return def.createServerSocket(port);
        }
    }

    public static class ClientFactory
        implements RMIClientSocketFactory, Serializable
    {

        private String protocol;
        private byte[] data;

        public ClientFactory(String protocol, byte[] data) {
            this.protocol = protocol;
            this.data = data;
        }

        public Socket createSocket(String host, int port)
            throws IOException
        {
            if (protocol.equals("compress")) {
                return new CompressSocket(host, port);

            } else if (protocol.equals("xor")) {
                if (data == null || data.length != 1)
                    throw new IOException("invalid argument for XOR protocol");
                return new XorSocket(host, port, data[0]);

            }

            return def.createSocket(host, port);
        }
    }

    static class CompressSocket extends Socket {
        private InputStream in;
        private OutputStream out;
        public CompressSocket() { super(); }
        public CompressSocket(String host, int port) throws IOException {
            super(host, port);
        }
        public InputStream getInputStream() throws IOException {
            if (in == null) {
                in = new CompressInputStream(super.getInputStream());
            }
            return in;
        }
        public OutputStream getOutputStream() throws IOException {
            if (out == null) {
                out = new CompressOutputStream(super.getOutputStream());
            }
            return out;
        }
    }

    static class CompressServerSocket extends ServerSocket {
        public CompressServerSocket(int port) throws IOException {
            super(port);
        }
        public Socket accept() throws IOException {
            Socket s = new CompressSocket();
            implAccept(s);
            return s;
        }
    }

    static class XorSocket extends Socket {
        private byte pattern;
        private InputStream in;
        private OutputStream out;
        public XorSocket(byte pattern) { super(); this.pattern = pattern; }
        public XorSocket(String host, int port, byte pattern)
            throws IOException
        {
            super(host, port);
            this.pattern = pattern;
        }
        public InputStream getInputStream() throws IOException {
            if (in == null) {
                in = new XorInputStream(super.getInputStream(), pattern);
            }
            return in;
        }
        public OutputStream getOutputStream() throws IOException {
            if (out == null) {
                out = new XorOutputStream(super.getOutputStream(), pattern);
            }
            return out;
        }
    }

    static class XorServerSocket extends ServerSocket {
        private byte pattern;
        public XorServerSocket(int port, byte pattern) throws IOException {
            super(port);
            this.pattern = pattern;
        }
        public Socket accept() throws IOException {
            Socket s = new XorSocket(pattern);
            implAccept(s);
            return s;
        }
    }

    static class XorOutputStream extends FilterOutputStream {
        private byte pattern;
        public XorOutputStream(OutputStream out, byte pattern) {
            super(out);
            this.pattern = pattern;
        }
        public void write(int b) throws IOException {
            out.write(b ^ pattern);
            out.flush();
        }
        public void write(byte b[], int off, int len) throws IOException {
            for (int i = 0; i < len; i++)
                write(b[off + i]);
        }
    }

    static class XorInputStream extends FilterInputStream {
        private byte pattern;
        public XorInputStream(InputStream in, byte pattern) {
            super(in);
            this.pattern = pattern;
        }
        public int read() throws IOException {
            int b = in.read();
//          System.out.print("BEFORE: " + Integer.toHexString(b));
            if (b != -1)
                b = (b ^ pattern) & 0xFF;
//          System.out.println("\tAFTER: " + Integer.toHexString(b));
            return b;
        }
        public int read(byte b[], int off, int len) throws IOException {
            if (len <= 0) {
                return 0;
            }

            int c = read();
            if (c == -1) {
                return -1;
            }
            b[off] = (byte)c;

            int i = 1;
/*****
            try {
                for (; i < len ; i++) {
                    c = read();
                    if (c == -1) {
                        break;
                    }
                    if (b != null) {
                        b[off + i] = (byte)c;
                    }
                }
            } catch (IOException ee) {
            }
*****/
            return i;
        }
    }
}
