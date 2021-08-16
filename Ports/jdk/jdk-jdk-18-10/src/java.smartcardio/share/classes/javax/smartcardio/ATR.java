/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

/**
 * A Smart Card's answer-to-reset bytes. A Card's ATR object can be obtained
 * by calling {@linkplain Card#getATR}.
 * This class does not attempt to verify that the ATR encodes a semantically
 * valid structure.
 *
 * <p>Instances of this class are immutable. Where data is passed in or out
 * via byte arrays, defensive cloning is performed.
 *
 * @see Card#getATR
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
public final class ATR implements java.io.Serializable {

    private static final long serialVersionUID = 6695383790847736493L;

    private byte[] atr;

    private transient int startHistorical, nHistorical;

    /**
     * Constructs an ATR from a byte array.
     *
     * @param atr the byte array containing the answer-to-reset bytes
     * @throws NullPointerException if <code>atr</code> is null
     */
    public ATR(byte[] atr) {
        this.atr = atr.clone();
        parse();
    }

    private void parse() {
        if (atr.length < 2) {
            return;
        }
        if ((atr[0] != 0x3b) && (atr[0] != 0x3f)) {
            return;
        }
        int t0 = (atr[1] & 0xf0) >> 4;
        int n = atr[1] & 0xf;
        int i = 2;
        while ((t0 != 0) && (i < atr.length)) {
            if ((t0 & 1) != 0) {
                i++;
            }
            if ((t0 & 2) != 0) {
                i++;
            }
            if ((t0 & 4) != 0) {
                i++;
            }
            if ((t0 & 8) != 0) {
                if (i >= atr.length) {
                    return;
                }
                t0 = (atr[i++] & 0xf0) >> 4;
            } else {
                t0 = 0;
            }
        }
        int k = i + n;
        if ((k == atr.length) || (k == atr.length - 1)) {
            startHistorical = i;
            nHistorical = n;
        }
    }

    /**
     * Returns a copy of the bytes in this ATR.
     *
     * @return a copy of the bytes in this ATR.
     */
    public byte[] getBytes() {
        return atr.clone();
    }

    /**
     * Returns a copy of the historical bytes in this ATR.
     * If this ATR does not contain historical bytes, an array of length
     * zero is returned.
     *
     * @return a copy of the historical bytes in this ATR.
     */
    public byte[] getHistoricalBytes() {
        byte[] b = new byte[nHistorical];
        System.arraycopy(atr, startHistorical, b, 0, nHistorical);
        return b;
    }

    /**
     * Returns a string representation of this ATR.
     *
     * @return a String representation of this ATR.
     */
    public String toString() {
        return "ATR: " + atr.length + " bytes";
    }

    /**
     * Compares the specified object with this ATR for equality.
     * Returns true if the given object is also an ATR and its bytes are
     * identical to the bytes in this ATR.
     *
     * @param obj the object to be compared for equality with this ATR
     * @return true if the specified object is equal to this ATR
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ATR == false) {
            return false;
        }
        ATR other = (ATR)obj;
        return Arrays.equals(this.atr, other.atr);
    }

    /**
     * Returns the hash code value for this ATR.
     *
     * @return the hash code value for this ATR.
     */
    public int hashCode() {
        return Arrays.hashCode(atr);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        atr = (byte[])in.readUnshared();
        parse();
    }

}
