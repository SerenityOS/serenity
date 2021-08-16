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

import java.util.Objects;

/**
 * A class representing elliptic curve private keys as defined in
 * <a href="https://tools.ietf.org/html/rfc8032">RFC 8032: Edwards-Curve
 * Digital Signature Algorithm (EdDSA)</a>, including the curve and other
 * algorithm parameters. The private key is a bit string represented using
 * a byte array. This class only supports bit string lengths that are a
 * multiple of 8.
 *
 * @since 15
 */
public final class EdECPrivateKeySpec implements KeySpec {

    private final NamedParameterSpec params;
    private final byte[] bytes;

    /**
     * Construct a private key spec using the supplied parameters and
     * bit string.
     *
     * @param params the algorithm parameters.
     * @param bytes the key as a byte array. This array is copied
     *              to protect against subsequent modification.
     *
     * @throws NullPointerException if {@code params} or {@code bytes}
     *                              is null.
     */
    public EdECPrivateKeySpec(NamedParameterSpec params, byte[] bytes) {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(bytes, "bytes must not be null");

        this.params = params;
        this.bytes = bytes.clone();
    }

    /**
     * Get the algorithm parameters that define the curve and other settings.
     *
     * @return the algorithm parameters.
     */
    public NamedParameterSpec getParams() {
        return params;
    }

    /**
     * Get the byte array representing the private key. A new copy of the array
     * is returned each time this method is called.
     *
     * @return the private key as a byte array.
     */
    public byte[] getBytes() {
        return bytes.clone();
    }
}
