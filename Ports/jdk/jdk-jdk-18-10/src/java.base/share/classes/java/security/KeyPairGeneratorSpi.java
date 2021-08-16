/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.spec.AlgorithmParameterSpec;

/**
 * <p> This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the {@code KeyPairGenerator} class, which is used to generate
 * pairs of public and private keys.
 *
 * <p> All the abstract methods in this class must be implemented by each
 * cryptographic service provider who wishes to supply the implementation
 * of a key pair generator for a particular algorithm.
 *
 * <p> In case the client does not explicitly initialize the KeyPairGenerator
 * (via a call to an {@code initialize} method), each provider must
 * supply (and document) a default initialization.
 * See the Keysize Restriction sections of the
 * {@extLink security_guide_jdk_providers JDK Providers}
 * document for information on the KeyPairGenerator defaults used by
 * JDK providers.
 * However, note that defaults may vary across different providers.
 * Additionally, the default value for a provider may change in a future
 * version. Therefore, it is recommended to explicitly initialize the
 * KeyPairGenerator instead of relying on provider-specific defaults.
 *
 * @author Benjamin Renaud
 * @since 1.2
 *
 *
 * @see KeyPairGenerator
 * @see java.security.spec.AlgorithmParameterSpec
 */

public abstract class KeyPairGeneratorSpi {

    /**
     * Constructor for subclasses to call.
     */
    public KeyPairGeneratorSpi() {}

    /**
     * Initializes the key pair generator for a certain keysize, using
     * the default parameter set.
     *
     * @param keysize the keysize. This is an
     * algorithm-specific metric, such as modulus length, specified in
     * number of bits.
     *
     * @param random the source of randomness for this generator.
     *
     * @throws    InvalidParameterException if the {@code keysize} is not
     * supported by this KeyPairGeneratorSpi object.
     */
    public abstract void initialize(int keysize, SecureRandom random);

    /**
     * Initializes the key pair generator using the specified parameter
     * set and user-provided source of randomness.
     *
     * <p>This concrete method has been added to this previously-defined
     * abstract class. (For backwards compatibility, it cannot be abstract.)
     * It may be overridden by a provider to initialize the key pair
     * generator. Such an override
     * is expected to throw an InvalidAlgorithmParameterException if
     * a parameter is inappropriate for this key pair generator.
     * If this method is not overridden, it always throws an
     * UnsupportedOperationException.
     *
     * @param params the parameter set used to generate the keys.
     *
     * @param random the source of randomness for this generator.
     *
     * @throws    InvalidAlgorithmParameterException if the given parameters
     * are inappropriate for this key pair generator.
     *
     * @since 1.2
     */
    public void initialize(AlgorithmParameterSpec params,
                           SecureRandom random)
        throws InvalidAlgorithmParameterException {
            throw new UnsupportedOperationException();
    }

    /**
     * Generates a key pair. Unless an initialization method is called
     * using a KeyPairGenerator interface, algorithm-specific defaults
     * will be used. This will generate a new key pair every time it
     * is called.
     *
     * @return the newly generated {@code KeyPair}
     */
    public abstract KeyPair generateKeyPair();
}
