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

package java.security;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Objects;
import sun.security.jca.JCAUtil;

/**
 * The {@code AlgorithmParameterGenerator} class is used to generate a
 * set of
 * parameters to be used with a certain algorithm. Parameter generators
 * are constructed using the {@code getInstance} factory methods
 * (static methods that return instances of a given class).
 *
 * <P>The object that will generate the parameters can be initialized
 * in two different ways: in an algorithm-independent manner, or in an
 * algorithm-specific manner:
 *
 * <ul>
 * <li>The algorithm-independent approach uses the fact that all parameter
 * generators share the concept of a "size" and a
 * source of randomness. The measure of size is universally shared
 * by all algorithm parameters, though it is interpreted differently
 * for different algorithms. For example, in the case of parameters for
 * the <i>DSA</i> algorithm, "size" corresponds to the size
 * of the prime modulus (in bits).
 * When using this approach, algorithm-specific parameter generation
 * values - if any - default to some standard values, unless they can be
 * derived from the specified size.
 *
 * <li>The other approach initializes a parameter generator object
 * using algorithm-specific semantics, which are represented by a set of
 * algorithm-specific parameter generation values. To generate
 * Diffie-Hellman system parameters, for example, the parameter generation
 * values usually
 * consist of the size of the prime modulus and the size of the
 * random exponent, both specified in number of bits.
 * </ul>
 *
 * <P>In case the client does not explicitly initialize the
 * AlgorithmParameterGenerator (via a call to an {@code init} method),
 * each provider must supply (and document) a default initialization.
 * See the Keysize Restriction sections of the
 * {@extLink security_guide_jdk_providers JDK Providers}
 * document for information on the AlgorithmParameterGenerator defaults
 * used by JDK providers.
 * However, note that defaults may vary across different providers.
 * Additionally, the default value for a provider may change in a future
 * version. Therefore, it is recommended to explicitly initialize the
 * AlgorithmParameterGenerator instead of relying on provider-specific defaults.
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard {@code AlgorithmParameterGenerator} algorithms and
 * keysizes in parentheses:
 * <ul>
 * <li>{@code DiffieHellman} (1024, 2048)</li>
 * <li>{@code DSA} (1024, 2048)</li>
 * </ul>
 * These algorithms are described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#algorithmparametergenerator-algorithms">
 * AlgorithmParameterGenerator section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other algorithms are supported.
 *
 * @author Jan Luehe
 *
 *
 * @see AlgorithmParameters
 * @see java.security.spec.AlgorithmParameterSpec
 *
 * @since 1.2
 */

public class AlgorithmParameterGenerator {

    // The provider
    private Provider provider;

    // The provider implementation (delegate)
    private AlgorithmParameterGeneratorSpi paramGenSpi;

    // The algorithm
    private String algorithm;

    /**
     * Creates an AlgorithmParameterGenerator object.
     *
     * @param paramGenSpi the delegate
     * @param provider the provider
     * @param algorithm the algorithm
     */
    protected AlgorithmParameterGenerator
    (AlgorithmParameterGeneratorSpi paramGenSpi, Provider provider,
     String algorithm) {
        this.paramGenSpi = paramGenSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Returns the standard name of the algorithm this parameter
     * generator is associated with.
     *
     * @return the string name of the algorithm.
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns an AlgorithmParameterGenerator object for generating
     * a set of parameters to be used with the specified algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new AlgorithmParameterGenerator object encapsulating the
     * AlgorithmParameterGeneratorSpi implementation from the first
     * Provider that supports the specified algorithm is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param algorithm the name of the algorithm this
     * parameter generator is associated with.
     * See the AlgorithmParameterGenerator section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#algorithmparametergenerator-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @return the new {@code AlgorithmParameterGenerator} object
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports an
     *         {@code AlgorithmParameterGeneratorSpi} implementation for the
     *         specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     */
    public static AlgorithmParameterGenerator getInstance(String algorithm)
        throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm, "null algorithm name");
            try {
                Object[] objs = Security.getImpl(algorithm,
                                                 "AlgorithmParameterGenerator",
                                                 (String)null);
                return new AlgorithmParameterGenerator
                    ((AlgorithmParameterGeneratorSpi)objs[0],
                     (Provider)objs[1],
                     algorithm);
            } catch(NoSuchProviderException e) {
                throw new NoSuchAlgorithmException(algorithm + " not found");
            }
    }

    /**
     * Returns an AlgorithmParameterGenerator object for generating
     * a set of parameters to be used with the specified algorithm.
     *
     * <p> A new AlgorithmParameterGenerator object encapsulating the
     * AlgorithmParameterGeneratorSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the algorithm this
     * parameter generator is associated with.
     * See the AlgorithmParameterGenerator section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#algorithmparametergenerator-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the string name of the Provider.
     *
     * @return the new {@code AlgorithmParameterGenerator} object
     *
     * @throws IllegalArgumentException if the provider name is {@code null}
     *         or empty
     *
     * @throws NoSuchAlgorithmException if an
     *         {@code AlgorithmParameterGeneratorSpi}
     *         implementation for the specified algorithm is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     */
    public static AlgorithmParameterGenerator getInstance(String algorithm,
                                                          String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (provider == null || provider.isEmpty())
            throw new IllegalArgumentException("missing provider");
        Object[] objs = Security.getImpl(algorithm,
                                         "AlgorithmParameterGenerator",
                                         provider);
        return new AlgorithmParameterGenerator
            ((AlgorithmParameterGeneratorSpi)objs[0], (Provider)objs[1],
             algorithm);
    }

    /**
     * Returns an AlgorithmParameterGenerator object for generating
     * a set of parameters to be used with the specified algorithm.
     *
     * <p> A new AlgorithmParameterGenerator object encapsulating the
     * AlgorithmParameterGeneratorSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the string name of the algorithm this
     * parameter generator is associated with.
     * See the AlgorithmParameterGenerator section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#algorithmparametergenerator-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the {@code Provider} object.
     *
     * @return the new {@code AlgorithmParameterGenerator} object
     *
     * @throws IllegalArgumentException if the specified provider is
     *         {@code null}
     *
     * @throws NoSuchAlgorithmException if an
     *         {@code AlgorithmParameterGeneratorSpi}
     *         implementation for the specified algorithm is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 1.4
     */
    public static AlgorithmParameterGenerator getInstance(String algorithm,
                                                          Provider provider)
        throws NoSuchAlgorithmException
    {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (provider == null)
            throw new IllegalArgumentException("missing provider");
        Object[] objs = Security.getImpl(algorithm,
                                         "AlgorithmParameterGenerator",
                                         provider);
        return new AlgorithmParameterGenerator
            ((AlgorithmParameterGeneratorSpi)objs[0], (Provider)objs[1],
             algorithm);
    }

    /**
     * Returns the provider of this algorithm parameter generator object.
     *
     * @return the provider of this algorithm parameter generator object
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Initializes this parameter generator for a certain size.
     * To create the parameters, the {@code SecureRandom}
     * implementation of the highest-priority installed provider is used as
     * the source of randomness.
     * (If none of the installed providers supply an implementation of
     * {@code SecureRandom}, a system-provided source of randomness is
     * used.)
     *
     * @param size the size (number of bits).
     */
    public final void init(int size) {
        paramGenSpi.engineInit(size, JCAUtil.getDefSecureRandom());
    }

    /**
     * Initializes this parameter generator for a certain size and source
     * of randomness.
     *
     * @param size the size (number of bits).
     * @param random the source of randomness.
     */
    public final void init(int size, SecureRandom random) {
        paramGenSpi.engineInit(size, random);
    }

    /**
     * Initializes this parameter generator with a set of algorithm-specific
     * parameter generation values.
     * To generate the parameters, the {@code SecureRandom}
     * implementation of the highest-priority installed provider is used as
     * the source of randomness.
     * (If none of the installed providers supply an implementation of
     * {@code SecureRandom}, a system-provided source of randomness is
     * used.)
     *
     * @param genParamSpec the set of algorithm-specific parameter generation values.
     *
     * @throws    InvalidAlgorithmParameterException if the given parameter
     * generation values are inappropriate for this parameter generator.
     */
    public final void init(AlgorithmParameterSpec genParamSpec)
        throws InvalidAlgorithmParameterException {
            paramGenSpi.engineInit(genParamSpec, JCAUtil.getDefSecureRandom());
    }

    /**
     * Initializes this parameter generator with a set of algorithm-specific
     * parameter generation values.
     *
     * @param genParamSpec the set of algorithm-specific parameter generation values.
     * @param random the source of randomness.
     *
     * @throws    InvalidAlgorithmParameterException if the given parameter
     * generation values are inappropriate for this parameter generator.
     */
    public final void init(AlgorithmParameterSpec genParamSpec,
                           SecureRandom random)
        throws InvalidAlgorithmParameterException {
            paramGenSpi.engineInit(genParamSpec, random);
    }

    /**
     * Generates the parameters.
     *
     * @return the new AlgorithmParameters object.
     */
    public final AlgorithmParameters generateParameters() {
        return paramGenSpi.engineGenerateParameters();
    }
}
