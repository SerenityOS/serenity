/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.regex.*;
import java.security.Provider.Service;

import jdk.internal.util.random.RandomSupport.RandomGeneratorProperties;
import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;
import sun.security.provider.SunEntries;
import sun.security.util.Debug;

/**
 * This class provides a cryptographically strong random number
 * generator (RNG).
 *
 * <p>A cryptographically strong random number minimally complies with the
 * statistical random number generator tests specified in
 * <a href="http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.140-2.pdf">
 * <i>FIPS 140-2, Security Requirements for Cryptographic Modules</i></a>,
 * section 4.9.1.
 * Additionally, {@code SecureRandom} must produce non-deterministic output.
 * Therefore any seed material passed to a {@code SecureRandom} object must be
 * unpredictable, and all {@code SecureRandom} output sequences must be
 * cryptographically strong, as described in
 * <a href="http://tools.ietf.org/html/rfc4086">
 * <i>RFC 4086: Randomness Requirements for Security</i></a>.
 *
 * <p> Many {@code SecureRandom} implementations are in the form of a
 * pseudo-random number generator (PRNG, also known as deterministic random
 * bits generator or DRBG), which means they use a deterministic algorithm
 * to produce a pseudo-random sequence from a random seed.
 * Other implementations may produce true random numbers,
 * and yet others may use a combination of both techniques.
 *
 * <p>A caller obtains a {@code SecureRandom} instance via the
 * no-argument constructor or one of the {@code getInstance} methods.
 * For example:
 *
 * <blockquote><pre>
 * SecureRandom r1 = new SecureRandom();
 * SecureRandom r2 = SecureRandom.getInstance("NativePRNG");
 * SecureRandom r3 = SecureRandom.getInstance("DRBG",
 *         DrbgParameters.instantiation(128, RESEED_ONLY, null));</pre>
 * </blockquote>
 *
 * <p> The third statement above returns a {@code SecureRandom} object of the
 * specific algorithm supporting the specific instantiate parameters. The
 * implementation's effective instantiated parameters must match this minimum
 * request but is not necessarily the same. For example, even if the request
 * does not require a certain feature, the actual instantiation can provide
 * the feature. An implementation may lazily instantiate a {@code SecureRandom}
 * until it's actually used, but the effective instantiate parameters must be
 * determined right after it's created and {@link #getParameters()} should
 * always return the same result unchanged.
 *
 * <p> Typical callers of {@code SecureRandom} invoke the following methods
 * to retrieve random bytes:
 *
 * <blockquote><pre>
 * SecureRandom random = new SecureRandom();
 * byte[] bytes = new byte[20];
 * random.nextBytes(bytes);</pre>
 * </blockquote>
 *
 * <p> Callers may also invoke the {@link #generateSeed} method
 * to generate a given number of seed bytes (to seed other random number
 * generators, for example):
 *
 * <blockquote><pre>
 * byte[] seed = random.generateSeed(20);</pre>
 * </blockquote>
 *
 * <p> A newly created PRNG {@code SecureRandom} object is not seeded (except
 * if it is created by {@link #SecureRandom(byte[])}). The first call to
 * {@code nextBytes} will force it to seed itself from an implementation-
 * specific entropy source. This self-seeding will not occur if {@code setSeed}
 * was previously called.
 *
 * <p> A {@code SecureRandom} can be reseeded at any time by calling the
 * {@code reseed} or {@code setSeed} method. The {@code reseed} method
 * reads entropy input from its entropy source to reseed itself.
 * The {@code setSeed} method requires the caller to provide the seed.
 *
 * <p> Please note that {@code reseed} may not be supported by all
 * {@code SecureRandom} implementations.
 *
 * <p> Some {@code SecureRandom} implementations may accept a
 * {@link SecureRandomParameters} parameter in its
 * {@link #nextBytes(byte[], SecureRandomParameters)} and
 * {@link #reseed(SecureRandomParameters)} methods to further
 * control the behavior of the methods.
 *
 * <p> Note: Depending on the implementation, the {@code generateSeed},
 * {@code reseed} and {@code nextBytes} methods may block as entropy is being
 * gathered, for example, if the entropy source is /dev/random on various
 * Unix-like operating systems.
 *
 * <h2> Thread safety </h2>
 * {@code SecureRandom} objects are safe for use by multiple concurrent threads.
 *
 * @implSpec
 * A {@code SecureRandom} service provider can advertise that it is thread-safe
 * by setting the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#service-attributes">service
 * provider attribute</a> "ThreadSafe" to "true" when registering the provider.
 * Otherwise, this class will instead synchronize access to the following
 * methods of the {@code SecureRandomSpi} implementation:
 * <ul>
 * <li>{@link SecureRandomSpi#engineSetSeed(byte[])}
 * <li>{@link SecureRandomSpi#engineNextBytes(byte[])}
 * <li>{@link SecureRandomSpi#engineNextBytes(byte[], SecureRandomParameters)}
 * <li>{@link SecureRandomSpi#engineGenerateSeed(int)}
 * <li>{@link SecureRandomSpi#engineReseed(SecureRandomParameters)}
 * </ul>
 *
 * @see java.security.SecureRandomSpi
 * @see java.util.Random
 *
 * @author Benjamin Renaud
 * @author Josh Bloch
 * @since 1.1
 */

@RandomGeneratorProperties(
        name = "SecureRandom",
        isStochastic = true
)
public class SecureRandom extends java.util.Random {

    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("securerandom");

    /**
     * The provider.
     *
     * @serial
     * @since 1.2
     */
    private Provider provider = null;

    /**
     * The provider implementation.
     *
     * @serial
     * @since 1.2
     */
    private SecureRandomSpi secureRandomSpi = null;

    /**
     * Thread safety.
     *
     * @serial
     * @since 9
     */
    private final boolean threadSafe;

    /**
     * The algorithm name or {@code null} if unknown.
     *
     * @serial
     * @since 1.5
     */
    private String algorithm;

    // Seed Generator
    private static volatile SecureRandom seedGenerator;

    /**
     * Constructs a secure random number generator (RNG) implementing the
     * default random number algorithm.
     *
     * <p> This constructor traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the first
     * Provider that supports a {@code SecureRandom} (RNG) algorithm is returned.
     * If none of the Providers support a RNG algorithm,
     * then an implementation-specific default is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * <p> See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     */
    public SecureRandom() {
        /*
         * This call to our superclass constructor will result in a call
         * to our own {@code setSeed} method, which will return
         * immediately when it is passed zero.
         */
        super(0);
        getDefaultPRNG(false, null);
        this.threadSafe = getThreadSafe();
    }

    private boolean getThreadSafe() {
        if (provider == null || algorithm == null) {
            return false;
        } else {
            return Boolean.parseBoolean(provider.getProperty(
                    "SecureRandom." + algorithm + " ThreadSafe", "false"));
        }
    }

    /**
     * Constructs a secure random number generator (RNG) implementing the
     * default random number algorithm.
     * The {@code SecureRandom} instance is seeded with the specified seed bytes.
     *
     * <p> This constructor traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the first
     * Provider that supports a {@code SecureRandom} (RNG) algorithm is returned.
     * If none of the Providers support a RNG algorithm,
     * then an implementation-specific default is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * <p> See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @param seed the seed.
     */
    public SecureRandom(byte[] seed) {
        super(0);
        getDefaultPRNG(true, seed);
        this.threadSafe = getThreadSafe();
    }

    private void getDefaultPRNG(boolean setSeed, byte[] seed) {
        Service prngService = null;
        String prngAlgorithm = null;
        for (Provider p : Providers.getProviderList().providers()) {
            // SUN provider uses the SunEntries.DEF_SECURE_RANDOM_ALGO
            // as the default SecureRandom algorithm; for other providers,
            // Provider.getDefaultSecureRandom() will use the 1st
            // registered SecureRandom algorithm
            if (p.getName().equals("SUN")) {
                prngAlgorithm = SunEntries.DEF_SECURE_RANDOM_ALGO;
                prngService = p.getService("SecureRandom", prngAlgorithm);
                break;
            } else {
                prngService = p.getDefaultSecureRandomService();
                if (prngService != null) {
                    prngAlgorithm = prngService.getAlgorithm();
                    break;
                }
            }
        }
        // per javadoc, if none of the Providers support a RNG algorithm,
        // then an implementation-specific default is returned.
        if (prngService == null) {
            prngAlgorithm = "SHA1PRNG";
            this.secureRandomSpi = new sun.security.provider.SecureRandom();
            this.provider = Providers.getSunProvider();
        } else {
            try {
                this.secureRandomSpi = (SecureRandomSpi)
                    prngService.newInstance(null);
                this.provider = prngService.getProvider();
            } catch (NoSuchAlgorithmException nsae) {
                // should not happen
                throw new RuntimeException(nsae);
            }
        }
        if (setSeed) {
            this.secureRandomSpi.engineSetSeed(seed);
        }
        // JDK 1.1 based implementations subclass SecureRandom instead of
        // SecureRandomSpi. They will also go through this code path because
        // they must call a SecureRandom constructor as it is their superclass.
        // If we are dealing with such an implementation, do not set the
        // algorithm value as it would be inaccurate.
        if (getClass() == SecureRandom.class) {
            this.algorithm = prngAlgorithm;
        }
    }

    /**
     * Creates a {@code SecureRandom} object.
     *
     * @param secureRandomSpi the {@code SecureRandom} implementation.
     * @param provider the provider.
     */
    protected SecureRandom(SecureRandomSpi secureRandomSpi,
                           Provider provider) {
        this(secureRandomSpi, provider, null);
    }

    private SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider,
            String algorithm) {
        super(0);
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = algorithm;
        this.threadSafe = getThreadSafe();

        if (!skipDebug && pdebug != null) {
            pdebug.println("SecureRandom." + algorithm +
                " algorithm from: " + getProviderName());
        }
    }

    private String getProviderName() {
        return (provider == null) ? "(no provider)" : provider.getName();
    }

    /**
     * Returns a {@code SecureRandom} object that implements the specified
     * Random Number Generator (RNG) algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the first
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
     * @param algorithm the name of the RNG algorithm.
     * See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @return the new {@code SecureRandom} object
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code SecureRandomSpi} implementation for the
     *         specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 1.2
     */
    public static SecureRandom getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns a {@code SecureRandom} object that implements the specified
     * Random Number Generator (RNG) algorithm.
     *
     * <p> A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the RNG algorithm.
     * See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return the new {@code SecureRandom} object
     *
     * @throws IllegalArgumentException if the provider name is {@code null}
     *         or empty
     *
     * @throws NoSuchAlgorithmException if a {@code SecureRandomSpi}
     *         implementation for the specified algorithm is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 1.2
     */
    public static SecureRandom getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }

    /**
     * Returns a {@code SecureRandom} object that implements the specified
     * Random Number Generator (RNG) algorithm.
     *
     * <p> A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the specified {@code Provider}
     * object is returned.  Note that the specified {@code Provider} object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the name of the RNG algorithm.
     * See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @param provider the provider.
     *
     * @return the new {@code SecureRandom} object
     *
     * @throws IllegalArgumentException if the specified provider is
     *         {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code SecureRandomSpi}
     *         implementation for the specified algorithm is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 1.4
     */
    public static SecureRandom getInstance(String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }

    /**
     * Returns a {@code SecureRandom} object that implements the specified
     * Random Number Generator (RNG) algorithm and supports the specified
     * {@code SecureRandomParameters} request.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the first
     * Provider that supports the specified algorithm and the specified
     * {@code SecureRandomParameters} is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param algorithm the name of the RNG algorithm.
     * See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @param params the {@code SecureRandomParameters}
     *               the newly created {@code SecureRandom} object must support.
     *
     * @return the new {@code SecureRandom} object
     *
     * @throws IllegalArgumentException if the specified params is
     *         {@code null}
     *
     * @throws NoSuchAlgorithmException if no Provider supports a
     *         {@code SecureRandomSpi} implementation for the specified
     *         algorithm and parameters
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 9
     */
    public static SecureRandom getInstance(
            String algorithm, SecureRandomParameters params)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm, params);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns a {@code SecureRandom} object that implements the specified
     * Random Number Generator (RNG) algorithm and supports the specified
     * {@code SecureRandomParameters} request.
     *
     * <p> A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the RNG algorithm.
     * See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @param params the {@code SecureRandomParameters}
     *               the newly created {@code SecureRandom} object must support.
     *
     * @param provider the name of the provider.
     *
     * @return the new {@code SecureRandom} object
     *
     * @throws IllegalArgumentException if the provider name is {@code null}
     *         or empty, or params is {@code null}
     *
     * @throws NoSuchAlgorithmException if the specified provider does not
     *         support a {@code SecureRandomSpi} implementation for the
     *         specified algorithm and parameters
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 9
     */
    public static SecureRandom getInstance(String algorithm,
            SecureRandomParameters params, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm, params, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns a {@code SecureRandom} object that implements the specified
     * Random Number Generator (RNG) algorithm and supports the specified
     * {@code SecureRandomParameters} request.
     *
     * <p> A new {@code SecureRandom} object encapsulating the
     * {@code SecureRandomSpi} implementation from the specified
     * {@code Provider} object is returned.  Note that the specified
     * {@code Provider} object does not have to be registered in the
     * provider list.
     *
     * @param algorithm the name of the RNG algorithm.
     * See the {@code SecureRandom} section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard RNG algorithm names.
     *
     * @param params the {@code SecureRandomParameters}
     *               the newly created {@code SecureRandom} object must support.
     *
     * @param provider the provider.
     *
     * @return the new {@code SecureRandom} object
     *
     * @throws IllegalArgumentException if the specified provider or params
     *         is {@code null}
     *
     * @throws NoSuchAlgorithmException if the specified provider does not
     *         support a {@code SecureRandomSpi} implementation for the
     *         specified algorithm and parameters
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 9
     */
    public static SecureRandom getInstance(String algorithm,
            SecureRandomParameters params, Provider provider)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Instance instance = GetInstance.getInstance("SecureRandom",
                SecureRandomSpi.class, algorithm, params, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns the provider of this {@code SecureRandom} object.
     *
     * @return the provider of this {@code SecureRandom} object.
     */
    public final Provider getProvider() {
        return provider;
    }

    /**
     * Returns the name of the algorithm implemented by this
     * {@code SecureRandom} object.
     *
     * @return the name of the algorithm or {@code unknown}
     *          if the algorithm name cannot be determined.
     * @since 1.5
     */
    public String getAlgorithm() {
        return Objects.toString(algorithm, "unknown");
    }

    /**
     * Returns a Human-readable string representation of this
     * {@code SecureRandom}.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return secureRandomSpi.toString();
    }

    /**
     * Returns the effective {@link SecureRandomParameters} for this
     * {@code SecureRandom} instance.
     * <p>
     * The returned value can be different from the
     * {@code SecureRandomParameters} object passed into a {@code getInstance}
     * method, but it cannot change during the lifetime of this
     * {@code SecureRandom} object.
     * <p>
     * A caller can use the returned value to find out what features this
     * {@code SecureRandom} supports.
     *
     * @return the effective {@link SecureRandomParameters} parameters,
     * or {@code null} if no parameters were used.
     *
     * @since 9
     * @see SecureRandomSpi
     */
    public SecureRandomParameters getParameters() {
        return secureRandomSpi.engineGetParameters();
    }

    /**
     * Reseeds this random object with the given seed. The seed supplements,
     * rather than replaces, the existing seed. Thus, repeated calls are
     * guaranteed never to reduce randomness.
     * <p>
     * A PRNG {@code SecureRandom} will not seed itself automatically if
     * {@code setSeed} is called before any {@code nextBytes} or {@code reseed}
     * calls. The caller should make sure that the {@code seed} argument
     * contains enough entropy for the security of this {@code SecureRandom}.
     *
     * @param seed the seed.
     *
     * @see #getSeed
     */
    public void setSeed(byte[] seed) {
        if (threadSafe) {
            secureRandomSpi.engineSetSeed(seed);
        } else {
            synchronized (this) {
                secureRandomSpi.engineSetSeed(seed);
            }
        }
    }

    /**
     * Reseeds this random object, using the eight bytes contained
     * in the given {@code long seed}. The given seed supplements,
     * rather than replaces, the existing seed. Thus, repeated calls
     * are guaranteed never to reduce randomness.
     *
     * <p>This method is defined for compatibility with
     * {@code java.util.Random}.
     *
     * @param seed the seed.
     *
     * @see #getSeed
     */
    @Override
    public void setSeed(long seed) {
        /*
         * Ignore call from super constructor as well as any other calls
         * unfortunate enough to be passing 0. All SecureRandom
         * constructors call `super(0)` which leads to `setSeed(0)`.
         * We either keep the object unseeded (in `new SecureRandom()`)
         * or we seed the object explicitly (in `new SecureRandom(byte[])`).
         */
        if (seed != 0) {
            setSeed(longToByteArray(seed));
        }
    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @param bytes the array to be filled in with random bytes.
     */
    @Override
    public void nextBytes(byte[] bytes) {
        if (threadSafe) {
            secureRandomSpi.engineNextBytes(bytes);
        } else {
            synchronized (this) {
                secureRandomSpi.engineNextBytes(bytes);
            }
        }
    }

    /**
     * Generates a user-specified number of random bytes with
     * additional parameters.
     *
     * @param bytes the array to be filled in with random bytes
     * @param params additional parameters
     * @throws NullPointerException if {@code bytes} is null
     * @throws UnsupportedOperationException if the underlying provider
     *         implementation has not overridden this method
     * @throws IllegalArgumentException if {@code params} is {@code null},
     *         illegal or unsupported by this {@code SecureRandom}
     *
     * @since 9
     */
    public void nextBytes(byte[] bytes, SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (threadSafe) {
            secureRandomSpi.engineNextBytes(
                    Objects.requireNonNull(bytes), params);
        } else {
            synchronized (this) {
                secureRandomSpi.engineNextBytes(
                        Objects.requireNonNull(bytes), params);
            }
        }
    }

    /**
     * Generates an integer containing the user-specified number of
     * pseudo-random bits (right justified, with leading zeros).  This
     * method overrides a {@code java.util.Random} method, and serves
     * to provide a source of random bits to all of the methods inherited
     * from that class (for example, {@code nextInt},
     * {@code nextLong}, and {@code nextFloat}).
     *
     * @param numBits number of pseudo-random bits to be generated, where
     * {@code 0 <= numBits <= 32}.
     *
     * @return an {@code int} containing the user-specified number
     * of pseudo-random bits (right justified, with leading zeros).
     */
    @Override
    protected final int next(int numBits) {
        int numBytes = (numBits+7)/8;
        byte[] b = new byte[numBytes];
        int next = 0;

        nextBytes(b);
        for (int i = 0; i < numBytes; i++) {
            next = (next << 8) + (b[i] & 0xFF);
        }

        return next >>> (numBytes*8 - numBits);
    }

    /**
     * Returns the given number of seed bytes, computed using the seed
     * generation algorithm that this class uses to seed itself.  This
     * call may be used to seed other random number generators.
     *
     * <p>This method is only included for backwards compatibility.
     * The caller is encouraged to use one of the alternative
     * {@code getInstance} methods to obtain a {@code SecureRandom} object, and
     * then call the {@code generateSeed} method to obtain seed bytes
     * from that object.
     *
     * @param numBytes the number of seed bytes to generate.
     *
     * @throws IllegalArgumentException if {@code numBytes} is negative
     * @return the seed bytes.
     *
     * @see #setSeed
     */
    public static byte[] getSeed(int numBytes) {
        SecureRandom seedGen = seedGenerator;
        if (seedGen == null) {
            seedGen = new SecureRandom();
            seedGenerator = seedGen;
        }
        return seedGen.generateSeed(numBytes);
    }

    /**
     * Returns the given number of seed bytes, computed using the seed
     * generation algorithm that this class uses to seed itself.  This
     * call may be used to seed other random number generators.
     *
     * @param numBytes the number of seed bytes to generate.
     * @throws IllegalArgumentException if {@code numBytes} is negative
     * @return the seed bytes.
     */
    public byte[] generateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        if (threadSafe) {
            return secureRandomSpi.engineGenerateSeed(numBytes);
        } else {
            synchronized (this) {
                return secureRandomSpi.engineGenerateSeed(numBytes);
            }
        }
    }

    /**
     * Helper function to convert a long into a byte array (least significant
     * byte first).
     */
    private static byte[] longToByteArray(long l) {
        byte[] retVal = new byte[8];

        for (int i = 0; i < 8; i++) {
            retVal[i] = (byte) l;
            l >>= 8;
        }

        return retVal;
    }

    /*
     * Lazily initialize since Pattern.compile() is heavy.
     * Effective Java (2nd Edition), Item 71.
     */
    private static final class StrongPatternHolder {
        /*
         * Entries are alg:prov separated by ,
         * Allow for prepended/appended whitespace between entries.
         *
         * Capture groups:
         *     1 - alg
         *     2 - :prov (optional)
         *     3 - prov (optional)
         *     4 - ,nextEntry (optional)
         *     5 - nextEntry (optional)
         */
        private static Pattern pattern =
            Pattern.compile(
                "\\s*([\\S&&[^:,]]*)(\\:([\\S&&[^,]]*))?\\s*(\\,(.*))?");
    }

    /**
     * Returns a {@code SecureRandom} object that was selected by using
     * the algorithms/providers specified in the {@code
     * securerandom.strongAlgorithms} {@link Security} property.
     * <p>
     * Some situations require strong random values, such as when
     * creating high-value/long-lived secrets like RSA public/private
     * keys.  To help guide applications in selecting a suitable strong
     * {@code SecureRandom} implementation, Java distributions
     * include a list of known strong {@code SecureRandom}
     * implementations in the {@code securerandom.strongAlgorithms}
     * Security property.
     * <p>
     * Every implementation of the Java platform is required to
     * support at least one strong {@code SecureRandom} implementation.
     *
     * @return a strong {@code SecureRandom} implementation as indicated
     * by the {@code securerandom.strongAlgorithms} Security property
     *
     * @throws NoSuchAlgorithmException if no algorithm is available
     *
     * @see Security#getProperty(String)
     *
     * @since 1.8
     */
    public static SecureRandom getInstanceStrong()
            throws NoSuchAlgorithmException {

        @SuppressWarnings("removal")
        String property = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                @Override
                public String run() {
                    return Security.getProperty(
                        "securerandom.strongAlgorithms");
                }
            });

        if (property == null || property.isEmpty()) {
            throw new NoSuchAlgorithmException(
                "Null/empty securerandom.strongAlgorithms Security Property");
        }

        String remainder = property;
        while (remainder != null) {
            Matcher m;
            if ((m = StrongPatternHolder.pattern.matcher(
                    remainder)).matches()) {

                String alg = m.group(1);
                String prov = m.group(3);

                try {
                    if (prov == null) {
                        return SecureRandom.getInstance(alg);
                    } else {
                        return SecureRandom.getInstance(alg, prov);
                    }
                } catch (NoSuchAlgorithmException |
                        NoSuchProviderException e) {
                }
                remainder = m.group(5);
            } else {
                remainder = null;
            }
        }

        throw new NoSuchAlgorithmException(
            "No strong SecureRandom impls available: " + property);
    }

    /**
     * Reseeds this {@code SecureRandom} with entropy input read from its
     * entropy source.
     *
     * @throws UnsupportedOperationException if the underlying provider
     *         implementation has not overridden this method.
     *
     * @since 9
     */
    public void reseed() {
        if (threadSafe) {
            secureRandomSpi.engineReseed(null);
        } else {
            synchronized (this) {
                secureRandomSpi.engineReseed(null);
            }
        }
    }

    /**
     * Reseeds this {@code SecureRandom} with entropy input read from its
     * entropy source with additional parameters.
     * <p>
     * Note that entropy is obtained from an entropy source. While
     * some data in {@code params} may contain entropy, its main usage is to
     * provide diversity.
     *
     * @param params extra parameters
     * @throws UnsupportedOperationException if the underlying provider
     *         implementation has not overridden this method.
     * @throws IllegalArgumentException if {@code params} is {@code null},
     *         illegal or unsupported by this {@code SecureRandom}
     *
     * @since 9
     */
    public void reseed(SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (threadSafe) {
            secureRandomSpi.engineReseed(params);
        } else {
            synchronized (this) {
                secureRandomSpi.engineReseed(params);
            }
        }
    }

    // Declare serialVersionUID to be compatible with JDK1.1
    @java.io.Serial
    static final long serialVersionUID = 4940670005562187L;

    // Retain unused values serialized from JDK1.1
    /**
     * @serial
     */
    private byte[] state;
    /**
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private MessageDigest digest = null;
    /**
     * @serial
     *
     * We know that the MessageDigest class does not implement
     * java.io.Serializable.  However, since this field is no longer
     * used, it will always be NULL and won't affect the serialization
     * of the {@code SecureRandom} class itself.
     */
    private byte[] randomBytes;
    /**
     * @serial
     */
    private int randomBytesUsed;
    /**
     * @serial
     */
    private long counter;
}
