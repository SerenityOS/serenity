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

package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Objects;

/**
 * This class specifies the parameters used with the
 * <a href="https://tools.ietf.org/html/rfc7539"><i>ChaCha20</i></a>
 * algorithm.
 *
 * <p> The parameters consist of a 12-byte nonce and an initial
 * counter value expressed as a 32-bit integer.
 *
 * <p> This class can be used to initialize a {@code Cipher} object that
 * implements the <i>ChaCha20</i> algorithm.
 *
 * @since 11
 */
public final class ChaCha20ParameterSpec implements AlgorithmParameterSpec {

    // The nonce length is defined by the spec as 96 bits (12 bytes) in length.
    private static final int NONCE_LENGTH = 12;

    private final byte[] nonce;
    private final int counter;

    /**
     * Constructs a parameter set for ChaCha20 from the given nonce
     * and counter.
     *
     * @param nonce a 12-byte nonce value
     * @param counter the initial counter value
     *
     * @throws NullPointerException if {@code nonce} is {@code null}
     * @throws IllegalArgumentException if {@code nonce} is not 12 bytes
     *      in length
     */
    public ChaCha20ParameterSpec(byte[] nonce, int counter) {
        this.counter = counter;

        Objects.requireNonNull(nonce, "Nonce must be non-null");
        this.nonce = nonce.clone();
        if (this.nonce.length != NONCE_LENGTH) {
            throw new IllegalArgumentException(
                    "Nonce must be 12-bytes in length");
        }
    }

    /**
     * Returns the nonce value.
     *
     * @return the nonce value.  This method returns a new array each time
     * this method is called.
     */
    public byte[] getNonce() {
        return nonce.clone();
    }

    /**
     * Returns the configured counter value.
     *
     * @return the counter value
     */
    public int getCounter() {
        return counter;
    }
}
