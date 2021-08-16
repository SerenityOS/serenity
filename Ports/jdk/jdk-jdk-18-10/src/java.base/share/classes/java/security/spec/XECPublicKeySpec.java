/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A class representing elliptic curve public keys as defined in RFC 7748,
 * including the curve and other algorithm parameters. The public key is a
 * particular point on the curve, which is represented using only its
 * u-coordinate. A u-coordinate is an element of the field of integers modulo
 * some value that is determined by the algorithm parameters. This field
 * element is represented by a BigInteger which may hold any value. That is,
 * the BigInteger is not restricted to the range of canonical field elements.
 *
 * @since 11
 */
public class XECPublicKeySpec implements KeySpec {

    private final AlgorithmParameterSpec params;
    private final BigInteger u;

    /**
     * Construct a public key spec using the supplied parameters and
     * u coordinate.
     *
     * @param params the algorithm parameters
     * @param u the u-coordinate of the point, represented using a BigInteger
     *          which may hold any value
     *
     * @throws NullPointerException if {@code params} or {@code u}
     *                              is null.
     */
    public XECPublicKeySpec(AlgorithmParameterSpec params, BigInteger u) {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(u, "u must not be null");

        this.params = params;
        this.u = u;
    }

    /**
     * Get the algorithm parameters that define the curve and other settings.
     *
     * @return the parameters
     */
    public AlgorithmParameterSpec getParams() {
        return params;
    }

    /**
     * Get the u coordinate of the point.
     *
     * @return the u-coordinate, represented using a BigInteger which may hold
     *          any value
     */
    public BigInteger getU() {
        return u;
    }
}
