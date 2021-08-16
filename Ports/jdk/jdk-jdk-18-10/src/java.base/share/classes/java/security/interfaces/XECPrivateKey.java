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

import java.security.PrivateKey;
import java.util.Optional;

/**
 * An interface for an elliptic curve private key as defined by RFC 7748.
 * These keys are distinct from the keys represented by {@code ECPrivateKey},
 * and they are intended for use with algorithms based on RFC 7748 such as the
 * XDH {@code KeyAgreement} algorithm.
 *
 * An XEC private key is an encoded scalar value as described in RFC 7748.
 * The decoding procedure defined in this RFC includes an operation that forces
 * certain bits of the key to either 1 or 0. This operation is known as
 * "pruning" or "clamping" the private key. Arrays returned by this interface
 * are unpruned, and implementations will need to prune the array before
 * using it in any numerical operations.
 *
 * @since 11
 */
public interface XECPrivateKey extends XECKey, PrivateKey {

    /**
     * Get the scalar value encoded as an unpruned byte array. A new copy of
     * the array is returned each time this method is called.
     *
     * @return the unpruned encoded scalar value, or an empty Optional if the
     *     scalar cannot be extracted (e.g. if the provider is a hardware token
     *     and the private key is not allowed to leave the crypto boundary).
     */
    Optional<byte[]> getScalar();
}

