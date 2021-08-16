/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.util.Set;

/**
 * This interface specifies constraints for cryptographic algorithms,
 * keys (key sizes), and other algorithm parameters.
 * <p>
 * {@code AlgorithmConstraints} objects are immutable.  An implementation
 * of this interface should not provide methods that can change the state
 * of an instance once it has been created.
 * <p>
 * Note that {@code AlgorithmConstraints} can be used to represent the
 * restrictions described by the security properties
 * {@code jdk.certpath.disabledAlgorithms} and
 * {@code jdk.tls.disabledAlgorithms}, or could be used by a
 * concrete {@code PKIXCertPathChecker} to check whether a specified
 * certificate in the certification path contains the required algorithm
 * constraints.
 *
 * @see javax.net.ssl.SSLParameters#getAlgorithmConstraints
 * @see javax.net.ssl.SSLParameters#setAlgorithmConstraints(AlgorithmConstraints)
 *
 * @since 1.7
 */

public interface AlgorithmConstraints {

    /**
     * Determines whether an algorithm is granted permission for the
     * specified cryptographic primitives.
     *
     * @param primitives a set of cryptographic primitives
     * @param algorithm the algorithm name
     * @param parameters the algorithm parameters, or null if no additional
     *     parameters
     *
     * @return true if the algorithm is permitted and can be used for all
     *     of the specified cryptographic primitives
     *
     * @throws IllegalArgumentException if primitives or algorithm is null
     *     or empty
     */
    public boolean permits(Set<CryptoPrimitive> primitives,
            String algorithm, AlgorithmParameters parameters);

    /**
     * Determines whether a key is granted permission for the specified
     * cryptographic primitives.
     * <p>
     * This method is usually used to check key size and key usage.
     *
     * @param primitives a set of cryptographic primitives
     * @param key the key
     *
     * @return true if the key can be used for all of the specified
     *     cryptographic primitives
     *
     * @throws IllegalArgumentException if primitives is null or empty,
     *     or the key is null
     */
    public boolean permits(Set<CryptoPrimitive> primitives, Key key);

    /**
     * Determines whether an algorithm and the corresponding key are granted
     * permission for the specified cryptographic primitives.
     *
     * @param primitives a set of cryptographic primitives
     * @param algorithm the algorithm name
     * @param key the key
     * @param parameters the algorithm parameters, or null if no additional
     *     parameters
     *
     * @return true if the key and the algorithm can be used for all of the
     *     specified cryptographic primitives
     *
     * @throws IllegalArgumentException if primitives or algorithm is null
     *     or empty, or the key is null
     */
    public boolean permits(Set<CryptoPrimitive> primitives,
                String algorithm, Key key, AlgorithmParameters parameters);

}
