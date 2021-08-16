/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package jdk.internal.icu.impl;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jdk.internal.icu.util.VersionInfo;

public final class ICUBinary {

    private static final class IsAcceptable implements Authenticate {
        @Override
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0] == 1;
        }
    }

    // public inner interface ------------------------------------------------

    /**
     * Special interface for data authentication
     */
    public static interface Authenticate
    {
        /**
         * Method used in ICUBinary.readHeader() to provide data format
         * authentication.
         * @param version version of the current data
         * @return true if dataformat is an acceptable version, false otherwise
         */
        public boolean isDataVersionAcceptable(byte version[]);
    }

    // public methods --------------------------------------------------------

    /**
     * Loads an ICU binary data file and returns it as a ByteBuffer.
     * The buffer contents is normally read-only, but its position etc. can be modified.
     *
     * @param itemPath Relative ICU data item path, for example "root.res" or "coll/ucadata.icu".
     * @return The data as a read-only ByteBuffer.
     */
    public static ByteBuffer getRequiredData(String itemPath) {
        final Class<ICUBinary> root = ICUBinary.class;

        try (@SuppressWarnings("removal") InputStream is = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                public InputStream run() {
                    return root.getResourceAsStream(itemPath);
                }
            })) {

            // is.available() may return 0, or 1, or the total number of bytes in the stream,
            // or some other number.
            // Do not try to use is.available() == 0 to find the end of the stream!
            byte[] bytes;
            int avail = is.available();
            if (avail > 32) {
                // There are more bytes available than just the ICU data header length.
                // With luck, it is the total number of bytes.
                bytes = new byte[avail];
            } else {
                bytes = new byte[128];  // empty .res files are even smaller
            }
            // Call is.read(...) until one returns a negative value.
            int length = 0;
            for(;;) {
                if (length < bytes.length) {
                    int numRead = is.read(bytes, length, bytes.length - length);
                    if (numRead < 0) {
                        break;  // end of stream
                    }
                    length += numRead;
                } else {
                    // See if we are at the end of the stream before we grow the array.
                    int nextByte = is.read();
                    if (nextByte < 0) {
                        break;
                    }
                    int capacity = 2 * bytes.length;
                    if (capacity < 128) {
                        capacity = 128;
                    } else if (capacity < 0x4000) {
                        capacity *= 2;  // Grow faster until we reach 16kB.
                    }
                    bytes = Arrays.copyOf(bytes, capacity);
                    bytes[length++] = (byte) nextByte;
                }
           }
            return ByteBuffer.wrap(bytes, 0, length);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Same as readHeader(), but returns a VersionInfo rather than a compact int.
     */
    public static VersionInfo readHeaderAndDataVersion(ByteBuffer bytes,
                                                             int dataFormat,
                                                             Authenticate authenticate)
                                                                throws IOException {
        return getVersionInfoFromCompactInt(readHeader(bytes, dataFormat, authenticate));
    }

    private static final byte BIG_ENDIAN_ = 1;
    public static final byte[] readHeader(InputStream inputStream,
                                        byte dataFormatIDExpected[],
                                        Authenticate authenticate)
                                                          throws IOException
    {
        DataInputStream input = new DataInputStream(inputStream);
        char headersize = input.readChar();
        int readcount = 2;
        //reading the header format
        byte magic1 = input.readByte();
        readcount ++;
        byte magic2 = input.readByte();
        readcount ++;
        if (magic1 != MAGIC1 || magic2 != MAGIC2) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }

        input.readChar(); // reading size
        readcount += 2;
        input.readChar(); // reading reserved word
        readcount += 2;
        byte bigendian    = input.readByte();
        readcount ++;
        byte charset      = input.readByte();
        readcount ++;
        byte charsize     = input.readByte();
        readcount ++;
        input.readByte(); // reading reserved byte
        readcount ++;

        byte dataFormatID[] = new byte[4];
        input.readFully(dataFormatID);
        readcount += 4;
        byte dataVersion[] = new byte[4];
        input.readFully(dataVersion);
        readcount += 4;
        byte unicodeVersion[] = new byte[4];
        input.readFully(unicodeVersion);
        readcount += 4;
        if (headersize < readcount) {
            throw new IOException("Internal Error: Header size error");
        }
        input.skipBytes(headersize - readcount);

        if (bigendian != BIG_ENDIAN_ || charset != CHAR_SET_
            || charsize != CHAR_SIZE_
            || !Arrays.equals(dataFormatIDExpected, dataFormatID)
            || (authenticate != null
                && !authenticate.isDataVersionAcceptable(dataVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        return unicodeVersion;
    }

    /**
     * Reads an ICU data header, checks the data format, and returns the data version.
     *
     * <p>Assumes that the ByteBuffer position is 0 on input.
     * The buffer byte order is set according to the data.
     * The buffer position is advanced past the header (including UDataInfo and comment).
     *
     * <p>See C++ ucmndata.h and unicode/udata.h.
     *
     * @return dataVersion
     * @throws IOException if this is not a valid ICU data item of the expected dataFormat
     */
    public static int readHeader(ByteBuffer bytes, int dataFormat, Authenticate authenticate)
            throws IOException {
        assert bytes.position() == 0;
        byte magic1 = bytes.get(2);
        byte magic2 = bytes.get(3);
        if (magic1 != MAGIC1 || magic2 != MAGIC2) {
            throw new IOException(MAGIC_NUMBER_AUTHENTICATION_FAILED_);
        }

        byte isBigEndian = bytes.get(8);
        byte charsetFamily = bytes.get(9);
        byte sizeofUChar = bytes.get(10);
        if (isBigEndian < 0 || 1 < isBigEndian ||
                charsetFamily != CHAR_SET_ || sizeofUChar != CHAR_SIZE_) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_);
        }
        bytes.order(isBigEndian != 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int headerSize = bytes.getChar(0);
        int sizeofUDataInfo = bytes.getChar(4);
        if (sizeofUDataInfo < 20 || headerSize < (sizeofUDataInfo + 4)) {
            throw new IOException("Internal Error: Header size error");
        }
        // TODO: Change Authenticate to take int major, int minor, int milli, int micro
        // to avoid array allocation.
        byte[] formatVersion = new byte[] {
            bytes.get(16), bytes.get(17), bytes.get(18), bytes.get(19)
        };
        if (bytes.get(12) != (byte)(dataFormat >> 24) ||
                bytes.get(13) != (byte)(dataFormat >> 16) ||
                bytes.get(14) != (byte)(dataFormat >> 8) ||
                bytes.get(15) != (byte)dataFormat ||
                (authenticate != null && !authenticate.isDataVersionAcceptable(formatVersion))) {
            throw new IOException(HEADER_AUTHENTICATION_FAILED_ +
                    String.format("; data format %02x%02x%02x%02x, format version %d.%d.%d.%d",
                            bytes.get(12), bytes.get(13), bytes.get(14), bytes.get(15),
                            formatVersion[0] & 0xff, formatVersion[1] & 0xff,
                            formatVersion[2] & 0xff, formatVersion[3] & 0xff));
        }

        bytes.position(headerSize);
        return  // dataVersion
                ((int)bytes.get(20) << 24) |
                ((bytes.get(21) & 0xff) << 16) |
                ((bytes.get(22) & 0xff) << 8) |
                (bytes.get(23) & 0xff);
    }

    public static void skipBytes(ByteBuffer bytes, int skipLength) {
        if (skipLength > 0) {
            bytes.position(bytes.position() + skipLength);
        }
    }

    public static byte[] getBytes(ByteBuffer bytes, int length, int additionalSkipLength) {
        byte[] dest = new byte[length];
        bytes.get(dest);
        if (additionalSkipLength > 0) {
            skipBytes(bytes, additionalSkipLength);
        }
        return dest;
    }

    public static String getString(ByteBuffer bytes, int length, int additionalSkipLength) {
        CharSequence cs = bytes.asCharBuffer();
        String s = cs.subSequence(0, length).toString();
        skipBytes(bytes, length * 2 + additionalSkipLength);
        return s;
    }

    public static char[] getChars(ByteBuffer bytes, int length, int additionalSkipLength) {
        char[] dest = new char[length];
        bytes.asCharBuffer().get(dest);
        skipBytes(bytes, length * 2 + additionalSkipLength);
        return dest;
    }

    public static int[] getInts(ByteBuffer bytes, int length, int additionalSkipLength) {
        int[] dest = new int[length];
        bytes.asIntBuffer().get(dest);
        skipBytes(bytes, length * 4 + additionalSkipLength);
        return dest;
    }

    /**
     * Returns a VersionInfo for the bytes in the compact version integer.
     */
    public static VersionInfo getVersionInfoFromCompactInt(int version) {
        return VersionInfo.getInstance(
                version >>> 24, (version >> 16) & 0xff, (version >> 8) & 0xff, version & 0xff);
    }

    // private variables -------------------------------------------------

    /**
     * Magic numbers to authenticate the data file
     */
    private static final byte MAGIC1 = (byte)0xda;
    private static final byte MAGIC2 = (byte)0x27;

    /**
     * File format authentication values
     */
    private static final byte CHAR_SET_ = 0;
    private static final byte CHAR_SIZE_ = 2;

    /**
     * Error messages
     */
    private static final String MAGIC_NUMBER_AUTHENTICATION_FAILED_ =
                       "ICUBinary data file error: Magic number authentication failed";
    private static final String HEADER_AUTHENTICATION_FAILED_ =
        "ICUBinary data file error: Header authentication failed";
}
