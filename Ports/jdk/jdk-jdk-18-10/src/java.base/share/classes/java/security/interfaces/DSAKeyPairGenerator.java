/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;

/**
 * An interface to an object capable of generating DSA key pairs.
 *
 * <p>The {@code initialize} methods may each be called any number
 * of times. If no {@code initialize} method is called on a
 * DSAKeyPairGenerator, each provider that implements this interface
 * should supply (and document) a default initialization. Note that
 * defaults may vary across different providers. Additionally, the default
 * value for a provider may change in a future version. Therefore, it is
 * recommended to explicitly initialize the DSAKeyPairGenerator instead
 * of relying on provider-specific defaults.
 *
 * <p>Users wishing to indicate DSA-specific parameters, and to generate a key
 * pair suitable for use with the DSA algorithm typically
 *
 * <ol>
 *
 * <li>Get a key pair generator for the DSA algorithm by calling the
 * KeyPairGenerator {@code getInstance} method with "DSA"
 * as its argument.
 *
 * <li>Check if the returned key pair generator is an instance of
 * DSAKeyPairGenerator before casting the result to a DSAKeyPairGenerator
 * and calling one of the {@code initialize} methods from this
 * DSAKeyPairGenerator interface.
 *
 * <li>Generate a key pair by calling the {@code generateKeyPair}
 * method of the KeyPairGenerator class.
 *
 * </ol>
 *
 * <p>Note: it is not always necessary to do algorithm-specific
 * initialization for a DSA key pair generator. That is, it is not always
 * necessary to call an {@code initialize} method in this interface.
 * Algorithm-independent initialization using the {@code initialize} method
 * in the KeyPairGenerator
 * interface is all that is needed when you accept defaults for algorithm-specific
 * parameters.
 *
 * <p>Note: Some earlier implementations of this interface may not support
 * larger values of DSA parameters such as 3072-bit.
 *
 * @since 1.1
 * @see java.security.KeyPairGenerator
 */
public interface DSAKeyPairGenerator {

    /**
     * Initializes the key pair generator using the DSA family parameters
     * (p,q and g) and an optional SecureRandom bit source. If a
     * SecureRandom bit source is needed but not supplied, i.e. null, a
     * default SecureRandom instance will be used.
     *
     * @param params the parameters to use to generate the keys.
     *
     * @param random the random bit source to use to generate key bits;
     * can be null.
     *
     * @throws    InvalidParameterException if the {@code params}
     * value is invalid, null, or unsupported.
     */
   public void initialize(DSAParams params, SecureRandom random)
   throws InvalidParameterException;

    /**
     * Initializes the key pair generator for a given modulus length
     * (instead of parameters), and an optional SecureRandom bit source.
     * If a SecureRandom bit source is needed but not supplied, i.e.
     * null, a default SecureRandom instance will be used.
     *
     * <p>If {@code genParams} is true, this method generates new
     * p, q and g parameters. If it is false, the method uses precomputed
     * parameters for the modulus length requested. If there are no
     * precomputed parameters for that modulus length, an exception will be
     * thrown.
     *
     * @param modlen the modulus length in bits. Valid values are any
     * multiple of 64 between 512 and 1024, inclusive, 2048, and 3072.
     *
     * @param random the random bit source to use to generate key bits;
     * can be null.
     *
     * @param genParams whether or not to generate new parameters for
     * the modulus length requested.
     *
     * @throws    InvalidParameterException if {@code modlen} is
     * invalid, or unsupported, or if {@code genParams} is false and there
     * are no precomputed parameters for the requested modulus length.
     */
    public void initialize(int modlen, boolean genParams, SecureRandom random)
    throws InvalidParameterException;
}
