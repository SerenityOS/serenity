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
import java.rmi.server.*;
import java.net.*;

public class Compress {

    interface CompressConstants {
        // constants for 6-bit code values
        static final int NOP  = 0;      // no operation: used to pad words on flush()
        static final int RAW  = 1;      // introduces raw byte format
        static final int BASE = 2;      // base for codes found in lookup table
        static final String codeTable =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ,.!?\"'()";
    }

    public static class CompressRMIClientSocketFactory
        implements java.rmi.server.RMIClientSocketFactory, Serializable {

        public Socket createSocket(String host, int port)
            throws IOException {

            return ((Socket) new CompressSocket(host, port));
        }
    }

    public static class CompressRMIServerSocketFactory
        implements RMIServerSocketFactory,
                   Serializable {

        public ServerSocket createServerSocket(int port)
            throws IOException {

            return ((ServerSocket) new CompressServerSocket(port));
        }
    }

    public static class CompressSocket extends Socket {
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

    public static class CompressServerSocket extends ServerSocket {
        public CompressServerSocket(int port) throws IOException {
            super(port);
        }
        public Socket accept() throws IOException {
            Socket s = new CompressSocket();
            implAccept(s);
            return s;
        }
    }

    public static class CompressInputStream extends FilterInputStream
        implements CompressConstants
    {

        public CompressInputStream(InputStream in) {
            super(in);
        }

        // buffer of unpacked 6-bit codes from last 32-word read
        int buf[] = new int[5];

        // position of next code to read in buffer (5 == end of buffer)
        int bufPos = 5;

        public int read() throws IOException {
            try {
                int code;
                do {
                    code = readCode();
                } while (code == NOP);  // ignore NOP codes

                if (code >= BASE)
                    return codeTable.charAt(code - BASE);
                else if (code == RAW) {
                    int high = readCode();
                    int low = readCode();
                    return (high << 4) | low;
                } else
                    throw new IOException("unknown compression code: " + code);
            } catch (EOFException e) {
                return -1;
            }
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

        private int readCode() throws IOException {
            if (bufPos == 5) {
                int b1 = in.read();
                int b2 = in.read();
                int b3 = in.read();
                int b4 = in.read();
                if ((b1 | b2 | b3 | b4) < 0)
                    throw new EOFException();
                int pack = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
                buf[0] = (pack >>> 24) & 0x3F;
                buf[1] = (pack >>> 18) & 0x3F;
                buf[2] = (pack >>> 12) & 0x3F;
                buf[3] = (pack >>>  6) & 0x3F;
                buf[4] = (pack >>>  0) & 0x3F;
                bufPos = 0;
            }
            return buf[bufPos++];
        }
    }

    public static class CompressOutputStream extends FilterOutputStream
        implements CompressConstants
    {

        public CompressOutputStream(OutputStream out) {
            super(out);
        }

        // buffer of 6-bit codes to pack into next 32-bit word
        int buf[] = new int[5];

        // number of valid codes pending in buffer
        int bufPos = 0;

        public void write(int b) throws IOException {
            b &= 0xFF;                  // force argument to a byte

            int pos = codeTable.indexOf((char)b);
            if (pos != -1)
                writeCode(BASE + pos);
            else {
                writeCode(RAW);
                writeCode(b >> 4);
                writeCode(b & 0xF);
            }
        }

        public void write(byte b[], int off, int len) throws IOException {
            /*
             * This is quite an inefficient implementation, because it has to
             * call the other write method for every byte in the array.  It
             * could be optimized for performance by doing all the processing
             * in this method.
             */
            for (int i = 0; i < len; i++)
                write(b[off + i]);
        }

        public void flush() throws IOException {
            while (bufPos > 0)
                writeCode(NOP);
        }

        private void writeCode(int c) throws IOException {
            buf[bufPos++] = c;
            if (bufPos == 5) {  // write next word when we have 5 codes
                int pack = (buf[0] << 24) | (buf[1] << 18) | (buf[2] << 12) |
                    (buf[3] << 6) | buf[4];
                out.write((pack >>> 24) & 0xFF);
                out.write((pack >>> 16) & 0xFF);
                out.write((pack >>> 8)  & 0xFF);
                out.write((pack >>> 0)  & 0xFF);
                bufPos = 0;
            }
        }
    }
}
