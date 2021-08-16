/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.JavaSecuritySpecAccess;
import jdk.internal.access.SharedSecrets;

import java.util.Arrays;

/**
 * This class represents a public or private key in encoded format.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.Key
 * @see java.security.KeyFactory
 * @see KeySpec
 * @see X509EncodedKeySpec
 * @see PKCS8EncodedKeySpec
 *
 * @since 1.2
 */

public abstract class EncodedKeySpec implements KeySpec {

    private byte[] encodedKey;
    private String algorithmName;

    static {
        SharedSecrets.setJavaSecuritySpecAccess(
                new JavaSecuritySpecAccess() {
                    @Override
                    public void clearEncodedKeySpec(EncodedKeySpec keySpec) {
                        keySpec.clear();
                    }
                });
    }

                    /**
     * Creates a new {@code EncodedKeySpec} with the given encoded key.
     *
     * @param encodedKey the encoded key. The contents of the
     * array are copied to protect against subsequent modification.
     * @throws NullPointerException if {@code encodedKey}
     * is null.
     */
    public EncodedKeySpec(byte[] encodedKey) {
        this.encodedKey = encodedKey.clone();
    }

    /**
     * Creates a new {@code EncodedKeySpec} with the given encoded key.
     * This constructor is useful when subsequent callers of the
     * {@code EncodedKeySpec} object might not know the algorithm
     * of the key.
     *
     * @param encodedKey the encoded key. The contents of the
     * array are copied to protect against subsequent modification.
     * @param algorithm the algorithm name of the encoded key
     * See the KeyFactory section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#keyfactory-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     * @throws NullPointerException if {@code encodedKey}
     * or {@code algorithm} is null.
     * @throws IllegalArgumentException if {@code algorithm} is
     * the empty string {@code ""}
     * @since 9
     */
    protected EncodedKeySpec(byte[] encodedKey, String algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("algorithm name may not be null");
        }
        if (algorithm.isEmpty()) {
            throw new IllegalArgumentException("algorithm name "
                                             + "may not be empty");
        }
        this.encodedKey = encodedKey.clone();
        this.algorithmName = algorithm;

    }

    /**
     * Returns the name of the algorithm of the encoded key.
     *
     * @return the name of the algorithm, or null if not specified
     * @since 9
     */
    public String getAlgorithm() {
        return algorithmName;
    }

    /**
     * Returns the encoded key.
     *
     * @return the encoded key. Returns a new array each time
     * this method is called.
     */
    public byte[] getEncoded() {
        return this.encodedKey.clone();
    }

    /**
     * Returns the name of the encoding format associated with this
     * key specification.
     *
     * <p>If the opaque representation of a key
     * (see {@link java.security.Key Key}) can be transformed
     * (see {@link java.security.KeyFactory KeyFactory})
     * into this key specification (or a subclass of it),
     * {@code getFormat} called
     * on the opaque key returns the same value as the
     * {@code getFormat} method
     * of this key specification.
     *
     * @return a string representation of the encoding format.
     */
    public abstract String getFormat();

    /**
     * Clear the encoding inside.
     */
    void clear() {
        Arrays.fill(encodedKey, (byte)0);
    }
}
