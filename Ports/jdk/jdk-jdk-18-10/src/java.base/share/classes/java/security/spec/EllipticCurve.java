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
 * This immutable class holds the necessary values needed to represent
 * an elliptic curve.
 *
 * @see ECField
 * @see ECFieldFp
 * @see ECFieldF2m
 *
 * @author Valerie Peng
 *
 * @since 1.5
 */
public class EllipticCurve {

    private final ECField field;
    private final BigInteger a;
    private final BigInteger b;
    private final byte[] seed;

    // Check coefficient c is a valid element in ECField field.
    private static void checkValidity(ECField field, BigInteger c,
        String cName) {
        // can only perform check if field is ECFieldFp or ECFieldF2m.
        if (field instanceof ECFieldFp) {
            BigInteger p = ((ECFieldFp)field).getP();
            if (p.compareTo(c) != 1) {
                throw new IllegalArgumentException(cName + " is too large");
            } else if (c.signum() < 0) {
                throw new IllegalArgumentException(cName + " is negative");
            }
        } else if (field instanceof ECFieldF2m) {
            int m = ((ECFieldF2m)field).getM();
            if (c.bitLength() > m) {
                throw new IllegalArgumentException(cName + " is too large");
            }
        }
    }

    /**
     * Creates an elliptic curve with the specified elliptic field
     * {@code field} and the coefficients {@code a} and
     * {@code b}.
     * @param field the finite field that this elliptic curve is over.
     * @param a the first coefficient of this elliptic curve.
     * @param b the second coefficient of this elliptic curve.
     * @throws    NullPointerException if {@code field},
     * {@code a}, or {@code b} is null.
     * @throws    IllegalArgumentException if {@code a}
     * or {@code b} is not null and not in {@code field}.
     */
    public EllipticCurve(ECField field, BigInteger a,
                         BigInteger b) {
        this(field, a, b, null);
    }

    /**
     * Creates an elliptic curve with the specified elliptic field
     * {@code field}, the coefficients {@code a} and
     * {@code b}, and the {@code seed} used for curve generation.
     * @param field the finite field that this elliptic curve is over.
     * @param a the first coefficient of this elliptic curve.
     * @param b the second coefficient of this elliptic curve.
     * @param seed the bytes used during curve generation for later
     * validation. Contents of this array are copied to protect against
     * subsequent modification.
     * @throws    NullPointerException if {@code field},
     * {@code a}, or {@code b} is null.
     * @throws    IllegalArgumentException if {@code a}
     * or {@code b} is not null and not in {@code field}.
     */
    public EllipticCurve(ECField field, BigInteger a,
                         BigInteger b, byte[] seed) {
        if (field == null) {
            throw new NullPointerException("field is null");
        }
        if (a == null) {
            throw new NullPointerException("first coefficient is null");
        }
        if (b == null) {
            throw new NullPointerException("second coefficient is null");
        }
        checkValidity(field, a, "first coefficient");
        checkValidity(field, b, "second coefficient");
        this.field = field;
        this.a = a;
        this.b = b;
        if (seed != null) {
            this.seed = seed.clone();
        } else {
            this.seed = null;
        }
    }

    /**
     * Returns the finite field {@code field} that this
     * elliptic curve is over.
     * @return the field {@code field} that this curve
     * is over.
     */
    public ECField getField() {
        return field;
    }

    /**
     * Returns the first coefficient {@code a} of the
     * elliptic curve.
     * @return the first coefficient {@code a}.
     */
    public BigInteger getA() {
        return a;
    }

    /**
     * Returns the second coefficient {@code b} of the
     * elliptic curve.
     * @return the second coefficient {@code b}.
     */
    public BigInteger getB() {
        return b;
    }

    /**
     * Returns the seeding bytes {@code seed} used
     * during curve generation. May be null if not specified.
     * @return the seeding bytes {@code seed}. A new
     * array is returned each time this method is called.
     */
    public byte[] getSeed() {
        if (seed == null) return null;
        else return seed.clone();
    }

    /**
     * Compares this elliptic curve for equality with the
     * specified object.
     * @param obj the object to be compared.
     * @return true if {@code obj} is an instance of
     * EllipticCurve and the field, A, and B match, false otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;

        return obj instanceof EllipticCurve other
                && field.equals(other.field)
                && a.equals(other.a)
                && b.equals(other.b);
    }

    /**
     * Returns a hash code value for this elliptic curve.
     * @return a hash code value computed from the hash codes of the field, A,
     * and B, as follows:
     * <pre>{@code
     *     (field.hashCode() << 6) + (a.hashCode() << 4) + (b.hashCode() << 2)
     * }</pre>
     */
    public int hashCode() {
        return (field.hashCode() << 6 +
            (a.hashCode() << 4) +
            (b.hashCode() << 2));
    }
}
