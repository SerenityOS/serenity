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

package sun.security.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A package private utility class to convert indefinite length DER
 * encoded byte arrays to definite length DER encoded byte arrays.
 *
 * This assumes that the basic data structure is "tag, length, value"
 * triplet. In the case where the length is "indefinite", terminating
 * end-of-contents bytes are expected.
 *
 * @author Hemma Prafullchandra
 */
class DerIndefLenConverter {

    private static final int TAG_MASK            = 0x1f; // bits 5-1
    private static final int FORM_MASK           = 0x20; // bits 6
    private static final int CLASS_MASK          = 0xC0; // bits 8 and 7

    private static final int LEN_LONG            = 0x80; // bit 8 set
    private static final int LEN_MASK            = 0x7f; // bits 7 - 1
    private static final int SKIP_EOC_BYTES      = 2;

    private byte[] data, newData;
    private int newDataPos, dataPos, dataSize, index;
    private int unresolved = 0;

    private ArrayList<Object> ndefsList = new ArrayList<Object>();

    private int numOfTotalLenBytes = 0;

    private boolean isEOC(int tag) {
        return (((tag & TAG_MASK) == 0x00) &&  // EOC
                ((tag & FORM_MASK) == 0x00) && // primitive
                ((tag & CLASS_MASK) == 0x00)); // universal
    }

    // if bit 8 is set then it implies either indefinite length or long form
    static boolean isLongForm(int lengthByte) {
        return ((lengthByte & LEN_LONG) == LEN_LONG);
    }

    /*
     * Default package private constructor
     */
    DerIndefLenConverter() { }

    /**
     * Checks whether the given length byte is of the form
     * <em>Indefinite</em>.
     *
     * @param lengthByte the length byte from a DER encoded
     *        object.
     * @return true if the byte is of Indefinite form otherwise
     *         returns false.
     */
    static boolean isIndefinite(int lengthByte) {
        return (isLongForm(lengthByte) && ((lengthByte & LEN_MASK) == 0));
    }

    /**
     * Parse the tag and if it is an end-of-contents tag then
     * add the current position to the <code>eocList</code> vector.
     */
    private void parseTag() throws IOException {
        if (isEOC(data[dataPos]) && (data[dataPos + 1] == 0)) {
            int numOfEncapsulatedLenBytes = 0;
            Object elem = null;
            int index;
            for (index = ndefsList.size()-1; index >= 0; index--) {
                // Determine the first element in the vector that does not
                // have a matching EOC
                elem = ndefsList.get(index);
                if (elem instanceof Integer) {
                    break;
                } else {
                    numOfEncapsulatedLenBytes += ((byte[])elem).length - 3;
                }
            }
            if (index < 0) {
                throw new IOException("EOC does not have matching " +
                                      "indefinite-length tag");
            }
            int sectionLen = dataPos - ((Integer)elem).intValue() +
                             numOfEncapsulatedLenBytes;
            byte[] sectionLenBytes = getLengthBytes(sectionLen);
            ndefsList.set(index, sectionLenBytes);
            unresolved--;

            // Add the number of bytes required to represent this section
            // to the total number of length bytes,
            // and subtract the indefinite-length tag (1 byte) and
            // EOC bytes (2 bytes) for this section
            numOfTotalLenBytes += (sectionLenBytes.length - 3);
        }
        dataPos++;
    }

    /**
     * Write the tag and if it is an end-of-contents tag
     * then skip the tag and its 1 byte length of zero.
     */
    private void writeTag() {
        if (dataPos == dataSize)
            return;
        int tag = data[dataPos++];
        if (isEOC(tag) && (data[dataPos] == 0)) {
            dataPos++;  // skip length
            writeTag();
        } else
            newData[newDataPos++] = (byte)tag;
    }

    /**
     * Parse the length and if it is an indefinite length then add
     * the current position to the <code>ndefsList</code> vector.
     *
     * @return the length of definite length data next, or -1 if there is
     *         not enough bytes to determine it
     * @throws IOException if invalid data is read
     */
    private int parseLength() throws IOException {
        int curLen = 0;
        if (dataPos == dataSize)
            return curLen;
        int lenByte = data[dataPos++] & 0xff;
        if (isIndefinite(lenByte)) {
            ndefsList.add(dataPos);
            unresolved++;
            return curLen;
        }
        if (isLongForm(lenByte)) {
            lenByte &= LEN_MASK;
            if (lenByte > 4) {
                throw new IOException("Too much data");
            }
            if ((dataSize - dataPos) < (lenByte + 1)) {
                return -1;
            }
            for (int i = 0; i < lenByte; i++) {
                curLen = (curLen << 8) + (data[dataPos++] & 0xff);
            }
            if (curLen < 0) {
                throw new IOException("Invalid length bytes");
            }
        } else {
           curLen = (lenByte & LEN_MASK);
        }
        return curLen;
    }

    /**
     * Write the length and if it is an indefinite length
     * then calculate the definite length from the positions
     * of the indefinite length and its matching EOC terminator.
     * Then, write the value.
     */
    private void writeLengthAndValue() throws IOException {
        if (dataPos == dataSize)
           return;
        int curLen = 0;
        int lenByte = data[dataPos++] & 0xff;
        if (isIndefinite(lenByte)) {
            byte[] lenBytes = (byte[])ndefsList.get(index++);
            System.arraycopy(lenBytes, 0, newData, newDataPos,
                             lenBytes.length);
            newDataPos += lenBytes.length;
            return;
        }
        if (isLongForm(lenByte)) {
            lenByte &= LEN_MASK;
            for (int i = 0; i < lenByte; i++) {
                curLen = (curLen << 8) + (data[dataPos++] & 0xff);
            }
            if (curLen < 0) {
                throw new IOException("Invalid length bytes");
            }
        } else {
            curLen = (lenByte & LEN_MASK);
        }
        writeLength(curLen);
        writeValue(curLen);
    }

    private void writeLength(int curLen) {
        if (curLen < 128) {
            newData[newDataPos++] = (byte)curLen;

        } else if (curLen < (1 << 8)) {
            newData[newDataPos++] = (byte)0x81;
            newData[newDataPos++] = (byte)curLen;

        } else if (curLen < (1 << 16)) {
            newData[newDataPos++] = (byte)0x82;
            newData[newDataPos++] = (byte)(curLen >> 8);
            newData[newDataPos++] = (byte)curLen;

        } else if (curLen < (1 << 24)) {
            newData[newDataPos++] = (byte)0x83;
            newData[newDataPos++] = (byte)(curLen >> 16);
            newData[newDataPos++] = (byte)(curLen >> 8);
            newData[newDataPos++] = (byte)curLen;

        } else {
            newData[newDataPos++] = (byte)0x84;
            newData[newDataPos++] = (byte)(curLen >> 24);
            newData[newDataPos++] = (byte)(curLen >> 16);
            newData[newDataPos++] = (byte)(curLen >> 8);
            newData[newDataPos++] = (byte)curLen;
        }
    }

    private byte[] getLengthBytes(int curLen) {
        byte[] lenBytes;
        int index = 0;

        if (curLen < 128) {
            lenBytes = new byte[1];
            lenBytes[index++] = (byte)curLen;

        } else if (curLen < (1 << 8)) {
            lenBytes = new byte[2];
            lenBytes[index++] = (byte)0x81;
            lenBytes[index++] = (byte)curLen;

        } else if (curLen < (1 << 16)) {
            lenBytes = new byte[3];
            lenBytes[index++] = (byte)0x82;
            lenBytes[index++] = (byte)(curLen >> 8);
            lenBytes[index++] = (byte)curLen;

        } else if (curLen < (1 << 24)) {
            lenBytes = new byte[4];
            lenBytes[index++] = (byte)0x83;
            lenBytes[index++] = (byte)(curLen >> 16);
            lenBytes[index++] = (byte)(curLen >> 8);
            lenBytes[index++] = (byte)curLen;

        } else {
            lenBytes = new byte[5];
            lenBytes[index++] = (byte)0x84;
            lenBytes[index++] = (byte)(curLen >> 24);
            lenBytes[index++] = (byte)(curLen >> 16);
            lenBytes[index++] = (byte)(curLen >> 8);
            lenBytes[index++] = (byte)curLen;
        }

        return lenBytes;
    }

    // Returns the number of bytes needed to represent the given length
    // in ASN.1 notation
    private int getNumOfLenBytes(int len) {
        int numOfLenBytes = 0;

        if (len < 128) {
            numOfLenBytes = 1;
        } else if (len < (1 << 8)) {
            numOfLenBytes = 2;
        } else if (len < (1 << 16)) {
            numOfLenBytes = 3;
        } else if (len < (1 << 24)) {
            numOfLenBytes = 4;
        } else {
            numOfLenBytes = 5;
        }
        return numOfLenBytes;
    }

    /**
     * Parse the value;
     */
    private void parseValue(int curLen) {
        dataPos += curLen;
    }

    /**
     * Write the value;
     */
    private void writeValue(int curLen) {
        for (int i=0; i < curLen; i++)
            newData[newDataPos++] = data[dataPos++];
    }

    /**
     * Converts a indefinite length DER encoded byte array to
     * a definte length DER encoding.
     *
     * @param indefData the byte array holding the indefinite
     *        length encoding.
     * @return the byte array containing the definite length
     *         DER encoding, or null if there is not enough data.
     * @exception IOException on parsing or re-writing errors.
     */
    byte[] convertBytes(byte[] indefData) throws IOException {
        data = indefData;
        dataPos=0; index=0;
        dataSize = data.length;
        int len=0;
        int unused = 0;

        // parse and set up the vectors of all the indefinite-lengths
        while (dataPos < dataSize) {
            if (dataPos + 2 > dataSize) {
                // There should be at least one tag and one length
                return null;
            }
            parseTag();
            len = parseLength();
            if (len < 0) {
                return null;
            }
            parseValue(len);
            if (unresolved == 0) {
                unused = dataSize - dataPos;
                dataSize = dataPos;
                break;
            }
        }

        if (unresolved != 0) {
            return null;
        }

        newData = new byte[dataSize + numOfTotalLenBytes + unused];
        dataPos=0; newDataPos=0; index=0;

        // write out the new byte array replacing all the indefinite-lengths
        // and EOCs
        while (dataPos < dataSize) {
           writeTag();
           writeLengthAndValue();
        }
        System.arraycopy(indefData, dataSize,
                         newData, dataSize + numOfTotalLenBytes, unused);

        return newData;
    }

    /**
     * Read the input stream into a DER byte array. If an indef len BER is
     * not resolved this method will try to read more data until EOF is reached.
     * This may block.
     *
     * @param in the input stream with tag and lenByte already read
     * @param tag the tag to remember
     * @return a DER byte array
     * @throws IOException if not all indef len BER
     *         can be resolved or another I/O error happens
     */
    public static byte[] convertStream(InputStream in, byte tag)
            throws IOException {
        int offset = 2;     // for tag and length bytes
        int readLen = in.available();
        byte[] indefData = new byte[readLen + offset];
        indefData[0] = tag;
        indefData[1] = (byte)0x80;
        while (true) {
            int bytesRead = in.readNBytes(indefData, offset, readLen);
            if (bytesRead != readLen) {
                readLen = bytesRead;
                indefData = Arrays.copyOf(indefData, offset + bytesRead);
            }
            DerIndefLenConverter derIn = new DerIndefLenConverter();
            byte[] result = derIn.convertBytes(indefData);
            if (result == null) {
                int next = in.read(); // This could block, but we need more
                if (next == -1) {
                    throw new IOException("not all indef len BER resolved");
                }
                int more = in.available();
                // expand array to include next and more
                indefData = Arrays.copyOf(indefData, offset + readLen + 1 + more);
                indefData[offset + readLen] = (byte)next;
                offset = offset + readLen + 1;
                readLen = more;
            } else {
                return result;
            }
        }
    }
}
