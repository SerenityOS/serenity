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
package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

/**
 * An interface for an elliptic curve public key as defined by RFC 7748.
 * These keys are distinct from the keys represented by {@code ECPublicKey},
 * and they are intended for use with algorithms based on RFC 7748 such as the
 * XDH {@code KeyAgreement} algorithm.
 *
 * An XEC public key is a particular point on the curve, which is represented
 * using only its u-coordinate as described in RFC 7748. A u-coordinate is an
 * element of the field of integers modulo some value that is determined by
 * the algorithm parameters. This field element is represented by a BigInteger
 * which may hold any value. That is, the BigInteger is not restricted to the
 * range of canonical field elements.
 *
 * @since 11
 */
public interface XECPublicKey extends XECKey, PublicKey {

    /**
     * Get the u coordinate of the point.
     *
     * @return the u-coordinate, represented using a BigInteger which may hold
     *          any value
     */
    BigInteger getU();

}

