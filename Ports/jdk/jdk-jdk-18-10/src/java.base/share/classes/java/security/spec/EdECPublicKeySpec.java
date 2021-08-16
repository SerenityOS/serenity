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
 * A class representing elliptic curve public keys as defined in
 * <a href="https://tools.ietf.org/html/rfc8032">RFC 8032: Edwards-Curve
 * Digital Signature Algorithm (EdDSA)</a>, including the curve and other
 * algorithm parameters. The public key is a point on the curve, which is
 * represented using an {@code EdECPoint}.
 *
 * @since 15
 */
public final class EdECPublicKeySpec implements KeySpec {

    private final NamedParameterSpec params;
    private final EdECPoint point;

    /**
     * Construct a public key spec using the supplied parameters and
     * point.
     *
     * @param params the algorithm parameters.
     * @param point the point representing the public key.
     *
     * @throws NullPointerException if {@code params} or {@code point}
     *                              is null.
     */
    public EdECPublicKeySpec(NamedParameterSpec params, EdECPoint point) {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(point, "point must not be null");

        this.params = params;
        this.point = point;
    }

    /**
     * Get the algorithm parameters that define the curve and other settings.
     *
     * @return the parameters.
     */
    public NamedParameterSpec getParams() {
        return params;
    }

    /**
     * Get the point representing the public key.
     *
     * @return the {@code EdECPoint} representing the public key.
     */
    public EdECPoint getPoint() {
        return point;
    }
}
