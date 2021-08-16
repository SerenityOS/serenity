/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.common;

import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;

/*
 * Came from GIFEncoder initially.
 * Modified - to allow for output compressed data without the block counts
 * which breakup the compressed data stream for GIF.
 */
public class BitFile {
    ImageOutputStream output;
    byte[] buffer;
    int index;
    int bitsLeft; // bits left at current index that are avail.

    /** note this also indicates gif format BITFile. **/
    boolean blocks = false;

    /*
     * @param output destination for output data
     * @param blocks GIF LZW requires block counts for output data
     */
    public BitFile(ImageOutputStream output, boolean blocks) {
        this.output = output;
        this.blocks = blocks;
        buffer = new byte[256];
        index = 0;
        bitsLeft = 8;
    }

    public void flush() throws IOException {
        int numBytes = index + (bitsLeft == 8 ? 0 : 1);
        if (numBytes > 0) {
            if (blocks) {
                output.write(numBytes);
            }
            output.write(buffer, 0, numBytes);
            buffer[0] = 0;
            index = 0;
            bitsLeft = 8;
        }
    }

    public void writeBits(int bits, int numbits) throws IOException {
        int bitsWritten = 0;
        int numBytes = 255;  // gif block count
        do {
            // This handles the GIF block count stuff
            if ((index == 254 && bitsLeft == 0) || index > 254) {
                if (blocks) {
                    output.write(numBytes);
                }

                output.write(buffer, 0, numBytes);

                buffer[0] = 0;
                index = 0;
                bitsLeft = 8;
            }

            if (numbits <= bitsLeft) { // bits contents fit in current index byte
                if (blocks) { // GIF
                    buffer[index] |= (bits & ((1 << numbits) - 1)) << (8 - bitsLeft);
                    bitsWritten += numbits;
                    bitsLeft -= numbits;
                    numbits = 0;
                } else {
                    buffer[index] |= (bits & ((1 << numbits) - 1)) << (bitsLeft - numbits);
                    bitsWritten += numbits;
                    bitsLeft -= numbits;
                    numbits = 0;
                }
            } else { // bits overflow from current byte to next.
                if (blocks) { // GIF
                    // if bits  > space left in current byte then the lowest order bits
                    // of code are taken and put in current byte and rest put in next.
                    buffer[index] |= (bits & ((1 << bitsLeft) - 1)) << (8 - bitsLeft);
                    bitsWritten += bitsLeft;
                    bits >>= bitsLeft;
                    numbits -= bitsLeft;
                    buffer[++index] = 0;
                    bitsLeft = 8;
                } else {
                    // if bits  > space left in current byte then the highest order bits
                    // of code are taken and put in current byte and rest put in next.
                    // at highest order bit location !!
                    int topbits = (bits >>> (numbits - bitsLeft)) & ((1 << bitsLeft) - 1);
                    buffer[index] |= topbits;
                    numbits -= bitsLeft;  // ok this many bits gone off the top
                    bitsWritten += bitsLeft;
                    buffer[++index] = 0;  // next index
                    bitsLeft = 8;
                }
            }
        } while (numbits != 0);
    }
}
