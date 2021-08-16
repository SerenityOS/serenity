/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This immutable class defines an elliptic curve (EC)
 * characteristic 2 finite field.
 *
 * @see ECField
 *
 * @author Valerie Peng
 *
 * @since 1.5
 */
public class ECFieldF2m implements ECField {

    private int m;
    private int[] ks;
    private BigInteger rp;

    /**
     * Creates an elliptic curve characteristic 2 finite
     * field which has 2^{@code m} elements with normal basis.
     * @param m with 2^{@code m} being the number of elements.
     * @throws    IllegalArgumentException if {@code m}
     * is not positive.
     */
    public ECFieldF2m(int m) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        this.m = m;
        this.ks = null;
        this.rp = null;
    }

    /**
     * Creates an elliptic curve characteristic 2 finite
     * field which has 2^{@code m} elements with
     * polynomial basis.
     * The reduction polynomial for this field is based
     * on {@code rp} whose i-th bit corresponds to
     * the i-th coefficient of the reduction polynomial.<p>
     * Note: A valid reduction polynomial is either a
     * trinomial (X^{@code m} + X^{@code k} + 1
     * with {@code m} &gt; {@code k} &gt;= 1) or a
     * pentanomial (X^{@code m} + X^{@code k3}
     * + X^{@code k2} + X^{@code k1} + 1 with
     * {@code m} &gt; {@code k3} &gt; {@code k2}
     * &gt; {@code k1} &gt;= 1).
     * @param m with 2^{@code m} being the number of elements.
     * @param rp the BigInteger whose i-th bit corresponds to
     * the i-th coefficient of the reduction polynomial.
     * @throws    NullPointerException if {@code rp} is null.
     * @throws    IllegalArgumentException if {@code m}
     * is not positive, or {@code rp} does not represent
     * a valid reduction polynomial.
     */
    public ECFieldF2m(int m, BigInteger rp) {
        // check m and rp
        this.m = m;
        this.rp = rp;
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        int bitCount = this.rp.bitCount();
        if (!this.rp.testBit(0) || !this.rp.testBit(m) ||
            ((bitCount != 3) && (bitCount != 5))) {
            throw new IllegalArgumentException
                ("rp does not represent a valid reduction polynomial");
        }
        // convert rp into ks
        BigInteger temp = this.rp.clearBit(0).clearBit(m);
        this.ks = new int[bitCount-2];
        for (int i = this.ks.length-1; i >= 0; i--) {
            int index = temp.getLowestSetBit();
            this.ks[i] = index;
            temp = temp.clearBit(index);
        }
    }

    /**
     * Creates an elliptic curve characteristic 2 finite
     * field which has 2^{@code m} elements with
     * polynomial basis. The reduction polynomial for this
     * field is based on {@code ks} whose content
     * contains the order of the middle term(s) of the
     * reduction polynomial.
     * Note: A valid reduction polynomial is either a
     * trinomial (X^{@code m} + X^{@code k} + 1
     * with {@code m} &gt; {@code k} &gt;= 1) or a
     * pentanomial (X^{@code m} + X^{@code k3}
     * + X^{@code k2} + X^{@code k1} + 1 with
     * {@code m} &gt; {@code k3} &gt; {@code k2}
     * &gt; {@code k1} &gt;= 1), so {@code ks} should
     * have length 1 or 3.
     * @param m with 2^{@code m} being the number of elements.
     * @param ks the order of the middle term(s) of the
     * reduction polynomial. Contents of this array are copied
     * to protect against subsequent modification.
     * @throws    NullPointerException if {@code ks} is null.
     * @throws    IllegalArgumentException if{@code m}
     * is not positive, or the length of {@code ks}
     * is neither 1 nor 3, or values in {@code ks}
     * are not between {@code m}-1 and 1 (inclusive)
     * and in descending order.
     */
    public ECFieldF2m(int m, int[] ks) {
        // check m and ks
        this.m = m;
        this.ks = ks.clone();
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        if ((this.ks.length != 1) && (this.ks.length != 3)) {
            throw new IllegalArgumentException
                ("length of ks is neither 1 nor 3");
        }
        for (int i = 0; i < this.ks.length; i++) {
            if ((this.ks[i] < 1) || (this.ks[i] > m-1)) {
                throw new IllegalArgumentException
                    ("ks["+ i + "] is out of range");
            }
            if ((i != 0) && (this.ks[i] >= this.ks[i-1])) {
                throw new IllegalArgumentException
                    ("values in ks are not in descending order");
            }
        }
        // convert ks into rp
        this.rp = BigInteger.ONE;
        this.rp = rp.setBit(m);
        for (int j = 0; j < this.ks.length; j++) {
            rp = rp.setBit(this.ks[j]);
        }
    }

    /**
     * Returns the field size in bits which is {@code m}
     * for this characteristic 2 finite field.
     * @return the field size in bits.
     */
    public int getFieldSize() {
        return m;
    }

    /**
     * Returns the value {@code m} of this characteristic
     * 2 finite field.
     * @return {@code m} with 2^{@code m} being the
     * number of elements.
     */
    public int getM() {
        return m;
    }

    /**
     * Returns a BigInteger whose i-th bit corresponds to the
     * i-th coefficient of the reduction polynomial for polynomial
     * basis or null for normal basis.
     * @return a BigInteger whose i-th bit corresponds to the
     * i-th coefficient of the reduction polynomial for polynomial
     * basis or null for normal basis.
     */
    public BigInteger getReductionPolynomial() {
        return rp;
    }

    /**
     * Returns an integer array which contains the order of the
     * middle term(s) of the reduction polynomial for polynomial
     * basis or null for normal basis.
     * @return an integer array which contains the order of the
     * middle term(s) of the reduction polynomial for polynomial
     * basis or null for normal basis. A new array is returned
     * each time this method is called.
     */
    public int[] getMidTermsOfReductionPolynomial() {
        if (ks == null) {
            return null;
        } else {
            return ks.clone();
        }
    }

    /**
     * Compares this finite field for equality with the
     * specified object.
     * @param obj the object to be compared.
     * @return true if {@code obj} is an instance
     * of ECFieldF2m and both {@code m} and the reduction
     * polynomial match, false otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;

        return obj instanceof ECFieldF2m other
            // no need to compare rp here since ks and rp
            // should be equivalent
            && (m == other.m)
            && (Arrays.equals(ks, other.ks));
    }

    /**
     * Returns a hash code value for this characteristic 2
     * finite field.
     * @return a hash code value.
     */
    public int hashCode() {
        int value = m << 5;
        value += (rp==null? 0:rp.hashCode());
        // no need to involve ks here since ks and rp
        // should be equivalent.
        return value;
    }
}
