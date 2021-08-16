/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.smartcardio;

import java.util.Arrays;

/**
 * A response APDU as defined in ISO/IEC 7816-4. It consists of a conditional
 * body and a two byte trailer.
 * This class does not attempt to verify that the APDU encodes a semantically
 * valid response.
 *
 * <p>Instances of this class are immutable. Where data is passed in or out
 * via byte arrays, defensive cloning is performed.
 *
 * @see CommandAPDU
 * @see CardChannel#transmit CardChannel.transmit
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
public final class ResponseAPDU implements java.io.Serializable {

    private static final long serialVersionUID = 6962744978375594225L;

    /** @serial */
    private byte[] apdu;

    /**
     * Constructs a ResponseAPDU from a byte array containing the complete
     * APDU contents (conditional body and trailed).
     *
     * <p>Note that the byte array is cloned to protect against subsequent
     * modification.
     *
     * @param apdu the complete response APDU
     *
     * @throws NullPointerException if apdu is null
     * @throws IllegalArgumentException if apdu.length is less than 2
     */
    public ResponseAPDU(byte[] apdu) {
        apdu = apdu.clone();
        check(apdu);
        this.apdu = apdu;
    }

    private static void check(byte[] apdu) {
        if (apdu.length < 2) {
            throw new IllegalArgumentException("apdu must be at least 2 bytes long");
        }
    }

    /**
     * Returns the number of data bytes in the response body (Nr) or 0 if this
     * APDU has no body. This call is equivalent to
     * <code>getData().length</code>.
     *
     * @return the number of data bytes in the response body or 0 if this APDU
     * has no body.
     */
    public int getNr() {
        return apdu.length - 2;
    }

    /**
     * Returns a copy of the data bytes in the response body. If this APDU as
     * no body, this method returns a byte array with a length of zero.
     *
     * @return a copy of the data bytes in the response body or the empty
     *    byte array if this APDU has no body.
     */
    public byte[] getData() {
        byte[] data = new byte[apdu.length - 2];
        System.arraycopy(apdu, 0, data, 0, data.length);
        return data;
    }

    /**
     * Returns the value of the status byte SW1 as a value between 0 and 255.
     *
     * @return the value of the status byte SW1 as a value between 0 and 255.
     */
    public int getSW1() {
        return apdu[apdu.length - 2] & 0xff;
    }

    /**
     * Returns the value of the status byte SW2 as a value between 0 and 255.
     *
     * @return the value of the status byte SW2 as a value between 0 and 255.
     */
    public int getSW2() {
        return apdu[apdu.length - 1] & 0xff;
    }

    /**
     * Returns the value of the status bytes SW1 and SW2 as a single
     * status word SW.
     * It is defined as
     * {@code (getSW1() << 8) | getSW2()}
     *
     * @return the value of the status word SW.
     */
    public int getSW() {
        return (getSW1() << 8) | getSW2();
    }

    /**
     * Returns a copy of the bytes in this APDU.
     *
     * @return a copy of the bytes in this APDU.
     */
    public byte[] getBytes() {
        return apdu.clone();
    }

    /**
     * Returns a string representation of this response APDU.
     *
     * @return a String representation of this response APDU.
     */
    public String toString() {
        return "ResponseAPDU: " + apdu.length + " bytes, SW="
            + Integer.toHexString(getSW());
    }

    /**
     * Compares the specified object with this response APDU for equality.
     * Returns true if the given object is also a ResponseAPDU and its bytes are
     * identical to the bytes in this ResponseAPDU.
     *
     * @param obj the object to be compared for equality with this response APDU
     * @return true if the specified object is equal to this response APDU
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ResponseAPDU == false) {
            return false;
        }
        ResponseAPDU other = (ResponseAPDU)obj;
        return Arrays.equals(this.apdu, other.apdu);
    }

    /**
     * Returns the hash code value for this response APDU.
     *
     * @return the hash code value for this response APDU.
     */
    public int hashCode() {
        return Arrays.hashCode(apdu);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        apdu = (byte[])in.readUnshared();
        check(apdu);
    }

}
