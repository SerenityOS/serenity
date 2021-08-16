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
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the {@code AlgorithmParameterGenerator} class, which
 * is used to generate a set of parameters to be used with a certain algorithm.
 *
 * <p> All the abstract methods in this class must be implemented by each
 * cryptographic service provider who wishes to supply the implementation
 * of a parameter generator for a particular algorithm.
 *
 * <p> In case the client does not explicitly initialize the
 * AlgorithmParameterGenerator (via a call to an {@code engineInit}
 * method), each provider must supply (and document) a default initialization.
 * See the Keysize Restriction sections of the
 * {@extLink security_guide_jdk_providers JDK Providers}
 * document for information on the AlgorithmParameterGenerator defaults
 * used by JDK providers.
 * However, note that defaults may vary across different providers.
 * Additionally, the default value for a provider may change in a future
 * version. Therefore, it is recommended to explicitly initialize the
 * AlgorithmParameterGenerator instead of relying on provider-specific defaults.
 *
 * @author Jan Luehe
 *
 *
 * @see AlgorithmParameterGenerator
 * @see AlgorithmParameters
 * @see java.security.spec.AlgorithmParameterSpec
 *
 * @since 1.2
 */

public abstract class AlgorithmParameterGeneratorSpi {

    /**
     * Constructor for subclasses to call.
     */
    public AlgorithmParameterGeneratorSpi() {}

    /**
     * Initializes this parameter generator for a certain size
     * and source of randomness.
     *
     * @param size the size (number of bits).
     * @param random the source of randomness.
     */
    protected abstract void engineInit(int size, SecureRandom random);

    /**
     * Initializes this parameter generator with a set of
     * algorithm-specific parameter generation values.
     *
     * @param genParamSpec the set of algorithm-specific parameter generation values.
     * @param random the source of randomness.
     *
     * @throws    InvalidAlgorithmParameterException if the given parameter
     * generation values are inappropriate for this parameter generator.
     */
    protected abstract void engineInit(AlgorithmParameterSpec genParamSpec,
                                       SecureRandom random)
        throws InvalidAlgorithmParameterException;

    /**
     * Generates the parameters.
     *
     * @return the new AlgorithmParameters object.
     */
    protected abstract AlgorithmParameters engineGenerateParameters();
}
