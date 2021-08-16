/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss;

import org.ietf.jgss.GSSException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import sun.security.util.*;

/**
 * This class represents the mechanism independent part of a GSS-API
 * context establishment token. Some mechanisms may choose to encode
 * all subsequent tokens as well such that they start with an encoding
 * of an instance of this class. e.g., The Kerberos v5 GSS-API Mechanism
 * uses this header for all GSS-API tokens.
 * <p>
 * The format is specified in RFC 2743 section 3.1.
 *
 * @author Mayank Upadhyay
 */

/*
 * The RFC states that implementations should explicitly follow the
 * encoding scheme descibed in this section rather than use ASN.1
 * compilers. However, we should consider removing duplicate ASN.1
 * like code from here and depend on sun.security.util if possible.
 */

public class GSSHeader {

    private ObjectIdentifier mechOid = null;
    private byte[] mechOidBytes = null;
    private int mechTokenLength = 0;

    /**
     * The tag defined in the GSS-API mechanism independent token
     * format.
     */
    public static final int TOKEN_ID=0x60;

    /**
     * Creates a GSSHeader instance whose encoding can be used as the
     * prefix for a particular mechanism token.
     * @param mechOid the Oid of the mechanism which generated the token
     * @param mechTokenLength the length of the subsequent portion that
     * the mechanism will be adding.
     */
    public GSSHeader(ObjectIdentifier mechOid, int mechTokenLength)
        throws IOException {

        this.mechOid = mechOid;
        DerOutputStream temp = new DerOutputStream();
        temp.putOID(mechOid);
        mechOidBytes = temp.toByteArray();
        this.mechTokenLength = mechTokenLength;
    }

    /**
     * Reads in a GSSHeader from an InputStream. Typically this would be
     * used as part of reading the complete token from an InputStream
     * that is obtained from a socket.
     */
    public GSSHeader(InputStream is)
        throws IOException, GSSException {

        //      debug("Parsing GSS token: ");

        int tag = is.read();

        //      debug("tag=" + tag);

        if (tag != TOKEN_ID)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                   "GSSHeader did not find the right tag");

        int length = getLength(is);

        DerValue temp = new DerValue(is);
        mechOidBytes = temp.toByteArray();
        mechOid = temp.getOID();
        //      debug (" oid=" + mechOid);

        //      debug (" len starting with oid=" + length);
        mechTokenLength = length - mechOidBytes.length;

        //      debug("  mechToken length=" + mechTokenLength);

    }

    /**
     * Used to obtain the Oid stored in this GSSHeader instance.
     * @return the Oid of the mechanism.
     */
    public ObjectIdentifier getOid() {
        return mechOid;
    }

    /**
     * Used to obtain the length of the mechanism specific token that
     * will follow the encoding of this GSSHeader instance.
     * @return the length of the mechanism specific token portion that
     * will follow this GSSHeader.
     */
    public int getMechTokenLength() {
        return mechTokenLength;
    }

    /**
     * Used to obtain the length of the encoding of this GSSHeader.
     * @return the lenght of the encoding of this GSSHeader instance.
     */
    public int getLength() {
        int lenField = mechOidBytes.length + mechTokenLength;
        return (1 + getLenFieldSize(lenField) + mechOidBytes.length);
    }

    /**
     * Used to determine what the maximum possible mechanism token
     * size is if the complete GSSToken returned to the application
     * (including a GSSHeader) is not to exceed some pre-determined
     * value in size.
     * @param mechOid the Oid of the mechanism that will generate
     * this GSS-API token
     * @param maxTotalSize the pre-determined value that serves as a
     * maximum size for the complete GSS-API token (including a
     * GSSHeader)
     * @return the maximum size of mechanism token that can be used
     * so as to not exceed maxTotalSize with the GSS-API token
     */
    public static int getMaxMechTokenSize(ObjectIdentifier mechOid,
                                          int maxTotalSize) {

        int mechOidBytesSize = 0;
        try {
            DerOutputStream temp = new DerOutputStream();
            temp.putOID(mechOid);
            mechOidBytesSize = temp.toByteArray().length;
        } catch (IOException e) {
        }

        // Subtract bytes needed for 0x60 tag and mechOidBytes
        maxTotalSize -= (1 + mechOidBytesSize);

        // Subtract maximum len bytes
        maxTotalSize -= 5;

        return maxTotalSize;

        /*
         * Len field and mechanism token must fit in remaining
         * space. The range of the len field that we allow is
         * 1 through 5.
         *

         int mechTokenSize = 0;
         for (int lenFieldSize = 1; lenFieldSize <= 5;
         lenFieldSize++) {
         mechTokenSize = maxTotalSize - lenFieldSize;
         if (getLenFieldSize(mechTokenSize + mechOidBytesSize +
         lenFieldSize) <= lenFieldSize)
         break;
         }

         return mechTokenSize;
        */


    }

    /**
     * Used to determine the number of bytes that will be need to encode
     * the length field of the GSSHeader.
     */
    private int getLenFieldSize(int len) {
        int retVal = 1;
        if (len < 128) {
            retVal=1;
        } else if (len < (1 << 8)) {
            retVal=2;
        } else if (len < (1 << 16)) {
            retVal=3;
        } else if (len < (1 << 24)) {
            retVal=4;
        } else {
            retVal=5; // See getMaxMechTokenSize
        }
        return retVal;
    }

    /**
     * Encodes this GSSHeader instance onto the provided OutputStream.
     * @param os the OutputStream to which the token should be written.
     * @return the number of bytes that are output as a result of this
     * encoding
     */
    public int encode(OutputStream os) throws IOException {
        int retVal = 1 + mechOidBytes.length;
        os.write(TOKEN_ID);
        int length = mechOidBytes.length + mechTokenLength;
        retVal += putLength(length, os);
        os.write(mechOidBytes);
        return retVal;
    }

    /**
     * Get a length from the input stream, allowing for at most 32 bits of
     * encoding to be used. (Not the same as getting a tagged integer!)
     *
     * @return the length or -1 if indefinite length found.
     * @exception IOException on parsing error or unsupported lengths.
     */
    // shameless lifted from sun.security.util.DerInputStream.
    private int getLength(InputStream in) throws IOException {
        return getLength(in.read(), in);
    }

    /**
     * Get a length from the input stream, allowing for at most 32 bits of
     * encoding to be used. (Not the same as getting a tagged integer!)
     *
     * @return the length or -1 if indefinite length found.
     * @exception IOException on parsing error or unsupported lengths.
     */
    // shameless lifted from sun.security.util.DerInputStream.
    private int getLength(int lenByte, InputStream in) throws IOException {
        int value, tmp;

        tmp = lenByte;
        if ((tmp & 0x080) == 0x00) { // short form, 1 byte datum
            value = tmp;
        } else {                                         // long form or indefinite
            tmp &= 0x07f;

            /*
             * NOTE:  tmp == 0 indicates indefinite length encoded data.
             * tmp > 4 indicates more than 4Gb of data.
             */
            if (tmp == 0)
                return -1;
            if (tmp < 0 || tmp > 4)
                throw new IOException("DerInputStream.getLength(): lengthTag="
                                      + tmp + ", "
                                      + ((tmp < 0) ? "incorrect DER encoding." : "too big."));

            for (value = 0; tmp > 0; tmp --) {
                value <<= 8;
                value += 0x0ff & in.read();
            }
            if (value < 0) {
                throw new IOException("Invalid length bytes");
            }
        }
        return value;
    }

    /**
     * Put the encoding of the length in the specified stream.
     *
     * @params len the length of the attribute.
     * @param out the outputstream to write the length to
     * @return the number of bytes written
     * @exception IOException on writing errors.
     */
    // Shameless lifted from sun.security.util.DerOutputStream.
    private int putLength(int len, OutputStream out) throws IOException {
        int retVal = 0;
        if (len < 128) {
            out.write((byte)len);
            retVal=1;

        } else if (len < (1 << 8)) {
            out.write((byte)0x081);
            out.write((byte)len);
            retVal=2;

        } else if (len < (1 << 16)) {
            out.write((byte)0x082);
            out.write((byte)(len >> 8));
            out.write((byte)len);
            retVal=3;

        } else if (len < (1 << 24)) {
            out.write((byte)0x083);
            out.write((byte)(len >> 16));
            out.write((byte)(len >> 8));
            out.write((byte)len);
            retVal=4;

        } else {
            out.write((byte)0x084);
            out.write((byte)(len >> 24));
            out.write((byte)(len >> 16));
            out.write((byte)(len >> 8));
            out.write((byte)len);
            retVal=5;
        }

        return retVal;
    }

    // XXX Call these two in some central class
    private void debug(String str) {
        System.err.print(str);
    }

    private  String getHexBytes(byte[] bytes, int len)
        throws IOException {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {

            int b1 = (bytes[i]>>4) & 0x0f;
            int b2 = bytes[i] & 0x0f;

            sb.append(Integer.toHexString(b1));
            sb.append(Integer.toHexString(b2));
            sb.append(' ');
        }
        return sb.toString();
    }
}
