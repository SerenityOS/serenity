/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

/**
 * An elliptic curve point used to specify keys as defined by
 * <a href="https://tools.ietf.org/html/rfc8032">RFC 8032: Edwards-Curve
 * Digital Signature Algorithm (EdDSA)</a>. These points are distinct from the
 * points represented by {@code ECPoint}, and they are intended for use with
 * algorithms based on RFC 8032 such as the EdDSA {@code Signature} algorithm.
 * <p>
 * An EdEC point is specified by its y-coordinate value and a boolean that
 * indicates whether the x-coordinate is odd. The y-coordinate is an
 * element of the field of integers modulo some value p that is determined by
 * the algorithm parameters. This field element is represented by a
 * {@code BigInteger}, and implementations that consume objects of this class
 * may reject integer values which are not in the range [0, p).
 *
 * @since 15
 */

public final class EdECPoint {

    private final boolean xOdd;
    private final BigInteger y;

    /**
     * Construct an EdECPoint.
     *
     * @param xOdd whether the x-coordinate is odd.
     * @param y the y-coordinate, represented using a {@code BigInteger}.
     *
     * @throws NullPointerException if {@code y} is null.
     */
    public EdECPoint(boolean xOdd, BigInteger y) {

        Objects.requireNonNull(y, "y must not be null");

        this.xOdd = xOdd;
        this.y = y;
    }

    /**
     * Get whether the x-coordinate of the point is odd.
     *
     * @return a boolean indicating whether the x-coordinate is odd.
     */
    public boolean isXOdd() {
        return xOdd;
    }

    /**
     * Get the y-coordinate of the point.
     *
     * @return the y-coordinate, represented using a {@code BigInteger}.
     */
    public BigInteger getY() {
        return y;
    }
}
