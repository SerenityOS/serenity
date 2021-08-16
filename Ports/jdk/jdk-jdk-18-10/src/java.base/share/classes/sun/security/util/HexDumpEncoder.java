/*
 * Copyright (c) 1995, 2019, Oracle and/or its affiliates. All rights reserved.
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


package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * This class encodes a buffer into the classic: "Hexadecimal Dump" format of
 * the past. It is useful for analyzing the contents of binary buffers.
 * The format produced is as follows:
 * <pre>
 * xxxx: 00 11 22 33 44 55 66 77   88 99 aa bb cc dd ee ff ................
 * </pre>
 * Where xxxx is the offset into the buffer in 16 byte chunks, followed
 * by ascii coded hexadecimal bytes followed by the ASCII representation of
 * the bytes or '.' if they are not valid bytes.
 *
 * @author      Chuck McManis
 */

public class HexDumpEncoder {

    private int offset;
    private int thisLineLength;
    private int currentByte;
    private byte thisLine[] = new byte[16];

    static void hexDigit(PrintStream p, byte x) {
        char c;

        c = (char) ((x >> 4) & 0xf);
        if (c > 9)
            c = (char) ((c-10) + 'A');
        else
            c = (char)(c + '0');
        p.write(c);
        c = (char) (x & 0xf);
        if (c > 9)
            c = (char)((c-10) + 'A');
        else
            c = (char)(c + '0');
        p.write(c);
    }

    protected int bytesPerAtom() {
        return (1);
    }

    protected int bytesPerLine() {
        return (16);
    }

    protected void encodeBufferPrefix(OutputStream o) throws IOException {
        offset = 0;
        pStream = new PrintStream(o);
    }

    protected void encodeLinePrefix(OutputStream o, int len) throws IOException {
        hexDigit(pStream, (byte)((offset >>> 8) & 0xff));
        hexDigit(pStream, (byte)(offset & 0xff));
        pStream.print(": ");
        currentByte = 0;
        thisLineLength = len;
    }

    protected void encodeAtom(OutputStream o, byte buf[], int off, int len) throws IOException {
        thisLine[currentByte] = buf[off];
        hexDigit(pStream, buf[off]);
        pStream.print(" ");
        currentByte++;
        if (currentByte == 8)
            pStream.print("  ");
    }

    protected void encodeLineSuffix(OutputStream o) throws IOException {
        if (thisLineLength < 16) {
            for (int i = thisLineLength; i < 16; i++) {
                pStream.print("   ");
                if (i == 7)
                    pStream.print("  ");
            }
        }
        pStream.print(" ");
        for (int i = 0; i < thisLineLength; i++) {
            if ((thisLine[i] < ' ') || (thisLine[i] > 'z')) {
                pStream.print(".");
            } else {
                pStream.write(thisLine[i]);
            }
        }
        pStream.println();
        offset += thisLineLength;
    }

    /** Stream that understands "printing" */
    protected PrintStream pStream;

    /**
     * This method works around the bizarre semantics of BufferedInputStream's
     * read method.
     */
    protected int readFully(InputStream in, byte buffer[])
            throws java.io.IOException {
        for (int i = 0; i < buffer.length; i++) {
            int q = in.read();
            if (q == -1)
                return i;
            buffer[i] = (byte)q;
        }
        return buffer.length;
    }

    /**
     * Encode bytes from the input stream, and write them as text characters
     * to the output stream. This method will run until it exhausts the
     * input stream, but does not print the line suffix for a final
     * line that is shorter than bytesPerLine().
     */
    public void encode(InputStream inStream, OutputStream outStream)
        throws IOException
    {
        int     j;
        int     numBytes;
        byte    tmpbuffer[] = new byte[bytesPerLine()];

        encodeBufferPrefix(outStream);

        while (true) {
            numBytes = readFully(inStream, tmpbuffer);
            if (numBytes == 0) {
                break;
            }
            encodeLinePrefix(outStream, numBytes);
            for (j = 0; j < numBytes; j += bytesPerAtom()) {

                if ((j + bytesPerAtom()) <= numBytes) {
                    encodeAtom(outStream, tmpbuffer, j, bytesPerAtom());
                } else {
                    encodeAtom(outStream, tmpbuffer, j, (numBytes)- j);
                }
            }
            if (numBytes < bytesPerLine()) {
                break;
            } else {
                encodeLineSuffix(outStream);
            }
        }
    }

    /**
     * A 'streamless' version of encode that simply takes a buffer of
     * bytes and returns a string containing the encoded buffer.
     */
    public String encode(byte aBuffer[]) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        try {
            encode(inStream, outStream);
            // explicit ascii->unicode conversion
            return outStream.toString(ISO_8859_1);
        } catch (IOException ignore) {
            // This should never happen.
            throw new Error("CharacterEncoder.encode internal error");
        }
    }

    /**
     * Return a byte array from the remaining bytes in this ByteBuffer.
     * <P>
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     * <P>
     * To avoid an extra copy, the implementation will attempt to return the
     * byte array backing the ByteBuffer.  If this is not possible, a
     * new byte array will be created.
     */
    private byte [] getBytes(ByteBuffer bb) {
        /*
         * This should never return a BufferOverflowException, as we're
         * careful to allocate just the right amount.
         */
        byte [] buf = null;

        /*
         * If it has a usable backing byte buffer, use it.  Use only
         * if the array exactly represents the current ByteBuffer.
         */
        if (bb.hasArray()) {
            byte [] tmp = bb.array();
            if ((tmp.length == bb.capacity()) &&
                    (tmp.length == bb.remaining())) {
                buf = tmp;
                bb.position(bb.limit());
            }
        }

        if (buf == null) {
            /*
             * This class doesn't have a concept of encode(buf, len, off),
             * so if we have a partial buffer, we must reallocate
             * space.
             */
            buf = new byte[bb.remaining()];

            /*
             * position() automatically updated
             */
            bb.get(buf);
        }

        return buf;
    }

    /**
     * A 'streamless' version of encode that simply takes a ByteBuffer
     * and returns a string containing the encoded buffer.
     * <P>
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     */
    public String encode(ByteBuffer aBuffer) {
        byte [] buf = getBytes(aBuffer);
        return encode(buf);
    }

    /**
     * Encode bytes from the input stream, and write them as text characters
     * to the output stream. This method will run until it exhausts the
     * input stream. It differs from encode in that it will add the
     * line at the end of a final line that is shorter than bytesPerLine().
     */
    public void encodeBuffer(InputStream inStream, OutputStream outStream)
        throws IOException
    {
        int     j;
        int     numBytes;
        byte    tmpbuffer[] = new byte[bytesPerLine()];

        encodeBufferPrefix(outStream);

        while (true) {
            numBytes = readFully(inStream, tmpbuffer);
            if (numBytes == 0) {
                break;
            }
            encodeLinePrefix(outStream, numBytes);
            for (j = 0; j < numBytes; j += bytesPerAtom()) {
                if ((j + bytesPerAtom()) <= numBytes) {
                    encodeAtom(outStream, tmpbuffer, j, bytesPerAtom());
                } else {
                    encodeAtom(outStream, tmpbuffer, j, (numBytes)- j);
                }
            }
            encodeLineSuffix(outStream);
            if (numBytes < bytesPerLine()) {
                break;
            }
        }
    }

    /**
     * Encode the buffer in <i>aBuffer</i> and write the encoded
     * result to the OutputStream <i>aStream</i>.
     */
    public void encodeBuffer(byte aBuffer[], OutputStream aStream)
        throws IOException
    {
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        encodeBuffer(inStream, aStream);
    }

    /**
     * A 'streamless' version of encode that simply takes a buffer of
     * bytes and returns a string containing the encoded buffer.
     */
    public String encodeBuffer(byte aBuffer[]) {
        ByteArrayOutputStream   outStream = new ByteArrayOutputStream();
        ByteArrayInputStream    inStream = new ByteArrayInputStream(aBuffer);
        try {
            encodeBuffer(inStream, outStream);
        } catch (Exception IOException) {
            // This should never happen.
            throw new Error("CharacterEncoder.encodeBuffer internal error");
        }
        return (outStream.toString());
    }

    /**
     * Encode the <i>aBuffer</i> ByteBuffer and write the encoded
     * result to the OutputStream <i>aStream</i>.
     * <P>
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     */
    public void encodeBuffer(ByteBuffer aBuffer, OutputStream aStream)
        throws IOException
    {
        byte [] buf = getBytes(aBuffer);
        encodeBuffer(buf, aStream);
    }

}
