/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.jpeg;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOException;

import java.io.IOException;

/**
 * A class wrapping a buffer and its state.  For efficiency,
 * the members are made visible to other classes in this package.
 */
class JPEGBuffer {

    private boolean debug = false;

    /**
     * The size of the buffer.  This is large enough to hold all
     * known marker segments (other than thumbnails and icc profiles)
     */
    final int BUFFER_SIZE = 4096;

    /**
     * The actual buffer.
     */
    byte [] buf;

    /**
     * The number of bytes available for reading from the buffer.
     * Anytime data is read from the buffer, this should be updated.
     */
    int bufAvail;

    /**
     * A pointer to the next available byte in the buffer.  This is
     * used to read data from the buffer and must be updated to
     * move through the buffer.
     */
    int bufPtr;

    /**
     * The ImageInputStream buffered.
     */
    ImageInputStream iis;

    JPEGBuffer (ImageInputStream iis) {
        buf = new byte[BUFFER_SIZE];
        bufAvail = 0;
        bufPtr = 0;
        this.iis = iis;
    }

    /**
     * Ensures that there are at least {@code count} bytes available
     * in the buffer, loading more data and moving any remaining
     * bytes to the front.  A count of 0 means to just fill the buffer.
     * If the count is larger than the buffer size, just fills the buffer.
     * If the end of the stream is encountered before a non-0 count can
     * be satisfied, an {@code IIOException} is thrown with the
     * message "Image Format Error".
     */
    void loadBuf(int count) throws IOException {
        if (debug) {
            System.out.print("loadbuf called with ");
            System.out.print("count " + count + ", ");
            System.out.println("bufAvail " + bufAvail + ", ");
        }
        if (count != 0) {
            if (bufAvail >= count) {  // have enough
                return;
            }
        } else {
            if (bufAvail == BUFFER_SIZE) {  // already full
                return;
            }
        }
        // First copy any remaining bytes down to the beginning
        if ((bufAvail > 0) && (bufAvail < BUFFER_SIZE)) {
            System.arraycopy(buf, bufPtr, buf, 0, bufAvail);
        }
        // Now fill the rest of the buffer
        int ret = iis.read(buf, bufAvail, buf.length - bufAvail);
        if (debug) {
            System.out.println("iis.read returned " + ret);
        }
        if (ret != -1) {
            bufAvail += ret;
        }
        bufPtr = 0;
        int minimum = Math.min(BUFFER_SIZE, count);
        if (bufAvail < minimum) {
            throw new IIOException ("Image Format Error");
        }
    }

    /**
     * Fills the data array from the stream, starting with
     * the buffer and then reading directly from the stream
     * if necessary.  The buffer is left in an appropriate
     * state.  If the end of the stream is encountered, an
     * {@code IIOException} is thrown with the
     * message "Image Format Error".
     */
    void readData(byte [] data) throws IOException {
        int count = data.length;
        // First see what's left in the buffer.
        if (bufAvail >= count) {  // It's enough
            System.arraycopy(buf, bufPtr, data, 0, count);
            bufAvail -= count;
            bufPtr += count;
            return;
        }
        int offset = 0;
        if (bufAvail > 0) {  // Some there, but not enough
            System.arraycopy(buf, bufPtr, data, 0, bufAvail);
            offset = bufAvail;
            count -= bufAvail;
            bufAvail = 0;
            bufPtr = 0;
        }
        // Now read the rest directly from the stream
        if (iis.read(data, offset, count) != count) {
            throw new IIOException ("Image format Error");
        }
    }

    /**
     * Skips {@code count} bytes, leaving the buffer
     * in an appropriate state.  If the end of the stream is
     * encountered, an {@code IIOException} is thrown with the
     * message "Image Format Error".
     */
    void skipData(int count) throws IOException {
        // First see what's left in the buffer.
        if (bufAvail >= count) {  // It's enough
            bufAvail -= count;
            bufPtr += count;
            return;
        }
        if (bufAvail > 0) {  // Some there, but not enough
            count -= bufAvail;
            bufAvail = 0;
            bufPtr = 0;
        }
        // Now read the rest directly from the stream
        if (iis.skipBytes(count) != count) {
            throw new IIOException ("Image format Error");
        }
    }

    /**
     * Push back the remaining contents of the buffer by
     * repositioning the input stream.
     */
    void pushBack() throws IOException {
        iis.seek(iis.getStreamPosition()-bufAvail);
        bufAvail = 0;
        bufPtr = 0;
    }

    /**
     * Return the stream position corresponding to the next
     * available byte in the buffer.
     */
    long getStreamPosition() throws IOException {
        return (iis.getStreamPosition()-bufAvail);
    }

    /**
     * Scan the buffer until the next 0xff byte, reloading
     * the buffer as necessary.  The buffer position is left
     * pointing to the first non-0xff byte after a run of
     * 0xff bytes.  If the end of the stream is encountered,
     * an EOI marker is inserted into the buffer and {@code true}
     * is returned.  Otherwise returns {@code false}.
     */
    boolean scanForFF(JPEGImageReader reader) throws IOException {
        boolean retval = false;
        boolean foundFF = false;
        while (foundFF == false) {
            while (bufAvail > 0) {
                if ((buf[bufPtr++] & 0xff) == 0xff) {
                    bufAvail--;
                    foundFF = true;
                    break;  // out of inner while
                }
                bufAvail--;
            }
            // Reload the buffer and keep going
            loadBuf(0);
            // Skip any remaining pad bytes
            if (foundFF == true) {
                while ((bufAvail > 0) && (buf[bufPtr] & 0xff) == 0xff) {
                    bufPtr++;  // Only if it still is 0xff
                    bufAvail--;
                }
            }
            if (bufAvail == 0) {  // Premature EOF
                // send out a warning, but treat it as EOI
                //reader.warningOccurred(JPEGImageReader.WARNING_NO_EOI);
                retval = true;
                buf[0] = (byte)JPEG.EOI;
                bufAvail = 1;
                bufPtr = 0;
                foundFF = true;
            }
        }
        return retval;
    }

    /**
     * Prints the contents of the buffer, in hex.
     * @param count the number of bytes to print,
     * starting at the current available byte.
     */
    void print(int count) {
        System.out.print("buffer has ");
        System.out.print(bufAvail);
        System.out.println(" bytes available");
        if (bufAvail < count) {
            count = bufAvail;
        }
        for (int ptr = bufPtr; count > 0; count--) {
            int val = (int)buf[ptr++] & 0xff;
            System.out.print(" " + Integer.toHexString(val));
        }
        System.out.println();
    }

}
