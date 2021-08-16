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

import java.util.Objects;

/**
 * A class representing elliptic curve private keys as defined in RFC 7748,
 * including the curve and other algorithm parameters. The private key is
 * represented as an encoded scalar value. The decoding procedure defined in
 * the RFC includes an operation that forces certain bits of the key to either
 * 1 or 0. This operation is known as "pruning" or "clamping" the private key.
 * All arrays in this spec are unpruned, and implementations will need to prune
 * the array before using it in any numerical operations.
 *
 * @since 11
 */
public class XECPrivateKeySpec implements KeySpec {

    private final AlgorithmParameterSpec params;
    private final byte[] scalar;

    /**
     * Construct a private key spec using the supplied parameters and
     * encoded scalar value.
     *
     * @param params the algorithm parameters
     * @param scalar the unpruned encoded scalar value. This array is copied
     *               to protect against subsequent modification.
     *
     * @throws NullPointerException if {@code params} or {@code scalar}
     *                              is null.
     */
    public XECPrivateKeySpec(AlgorithmParameterSpec params, byte[] scalar) {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(scalar, "scalar must not be null");

        this.params = params;
        this.scalar = scalar.clone();
    }

    /**
     * Get the algorithm parameters that define the curve and other settings.
     *
     * @return the algorithm parameters
     */
    public AlgorithmParameterSpec getParams() {
        return params;
    }

    /**
     * Get the scalar value encoded as an unpruned byte array. A new copy of
     * the array is returned each time this method is called.
     *
     * @return the unpruned encoded scalar value
     */
    public byte[] getScalar() {
        return scalar.clone();
    }
}
