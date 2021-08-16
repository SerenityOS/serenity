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

package javax.crypto;

import java.util.*;

import java.security.*;
import java.security.Provider.Service;
import java.security.spec.*;

import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;

/**
 * This class represents a factory for secret keys.
 *
 * <P> Key factories are used to convert <I>keys</I> (opaque
 * cryptographic keys of type {@code Key}) into <I>key specifications</I>
 * (transparent representations of the underlying key material), and vice
 * versa.
 * Secret key factories operate only on secret (symmetric) keys.
 *
 * <P> Key factories are bi-directional, i.e., they allow to build an opaque
 * key object from a given key specification (key material), or to retrieve
 * the underlying key material of a key object in a suitable format.
 *
 * <P> Application developers should refer to their provider's documentation
 * to find out which key specifications are supported by the
 * {@link #generateSecret(java.security.spec.KeySpec) generateSecret} and
 * {@link #getKeySpec(javax.crypto.SecretKey, java.lang.Class) getKeySpec}
 * methods.
 * For example, the DESede (Triple DES) secret-key factory supplied by the
 * "SunJCE" provider supports {@code DESedeKeySpec} as a transparent
 * representation of Triple DES keys.
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard {@code SecretKeyFactory} algorithms:
 * <ul>
 * <li>{@code DESede}</li>
 * </ul>
 * These algorithms are described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#secretkeyfactory-algorithms">
 * SecretKeyFactory section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other algorithms are supported.
 *
 * @author Jan Luehe
 *
 * @see SecretKey
 * @see javax.crypto.spec.DESedeKeySpec
 * @see javax.crypto.spec.PBEKeySpec
 * @since 1.4
 */

public class SecretKeyFactory {

    // The provider
    private Provider provider;

    // The algorithm associated with this factory
    private final String algorithm;

    // The provider implementation (delegate)
    private volatile SecretKeyFactorySpi spi;

    // lock for mutex during provider selection
    private final Object lock = new Object();

    // remaining services to try in provider selection
    // null once provider is selected
    private Iterator<Service> serviceIterator;

    /**
     * Creates a SecretKeyFactory object.
     *
     * @param keyFacSpi the delegate
     * @param provider the provider
     * @param algorithm the secret-key algorithm
     */
    protected SecretKeyFactory(SecretKeyFactorySpi keyFacSpi,
                               Provider provider, String algorithm) {
        this.spi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    private SecretKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        this.algorithm = algorithm;
        List<Service> list =
                GetInstance.getServices("SecretKeyFactory", algorithm);
        serviceIterator = list.iterator();
        // fetch and instantiate initial spi
        if (nextSpi(null) == null) {
            throw new NoSuchAlgorithmException
                (algorithm + " SecretKeyFactory not available");
        }
    }

    /**
     * Returns a {@code SecretKeyFactory} object that converts
     * secret keys of the specified algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new SecretKeyFactory object encapsulating the
     * SecretKeyFactorySpi implementation from the first
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
     * @param algorithm the standard name of the requested secret-key
     * algorithm.
     * See the SecretKeyFactory section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#secretkeyfactory-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @return the new {@code SecretKeyFactory} object
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code SecretKeyFactorySpi} implementation for the
     *         specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final SecretKeyFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        return new SecretKeyFactory(algorithm);
    }

    /**
     * Returns a {@code SecretKeyFactory} object that converts
     * secret keys of the specified algorithm.
     *
     * <p> A new SecretKeyFactory object encapsulating the
     * SecretKeyFactorySpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the standard name of the requested secret-key
     * algorithm.
     * See the SecretKeyFactory section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#secretkeyfactory-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return the new {@code SecretKeyFactory} object
     *
     * @throws IllegalArgumentException if the {@code provider}
     *         is {@code null} or empty
     *
     * @throws NoSuchAlgorithmException if a {@code SecretKeyFactorySpi}
     *         implementation for the specified algorithm is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final SecretKeyFactory getInstance(String algorithm,
            String provider) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = JceSecurity.getInstance("SecretKeyFactory",
                SecretKeyFactorySpi.class, algorithm, provider);
        return new SecretKeyFactory((SecretKeyFactorySpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns a {@code SecretKeyFactory} object that converts
     * secret keys of the specified algorithm.
     *
     * <p> A new SecretKeyFactory object encapsulating the
     * SecretKeyFactorySpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the standard name of the requested secret-key
     * algorithm.
     * See the SecretKeyFactory section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#secretkeyfactory-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the provider.
     *
     * @return the new {@code SecretKeyFactory} object
     *
     * @throws IllegalArgumentException if the {@code provider}
     *         is {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code SecretKeyFactorySpi}
     *         implementation for the specified algorithm is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final SecretKeyFactory getInstance(String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = JceSecurity.getInstance("SecretKeyFactory",
                SecretKeyFactorySpi.class, algorithm, provider);
        return new SecretKeyFactory((SecretKeyFactorySpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns the provider of this {@code SecretKeyFactory} object.
     *
     * @return the provider of this {@code SecretKeyFactory} object
     */
    public final Provider getProvider() {
        synchronized (lock) {
            // disable further failover after this call
            serviceIterator = null;
            return provider;
        }
    }

    /**
     * Returns the algorithm name of this {@code SecretKeyFactory} object.
     *
     * <p>This is the same name that was specified in one of the
     * {@code getInstance} calls that created this
     * {@code SecretKeyFactory} object.
     *
     * @return the algorithm name of this {@code SecretKeyFactory}
     * object.
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Update the active spi of this class and return the next
     * implementation for failover. If no more implemenations are
     * available, this method returns null. However, the active spi of
     * this class is never set to null.
     */
    private SecretKeyFactorySpi nextSpi(SecretKeyFactorySpi oldSpi) {
        synchronized (lock) {
            // somebody else did a failover concurrently
            // try that spi now
            if ((oldSpi != null) && (oldSpi != spi)) {
                return spi;
            }
            if (serviceIterator == null) {
                return null;
            }
            while (serviceIterator.hasNext()) {
                Service s = serviceIterator.next();
                if (JceSecurity.canUseProvider(s.getProvider()) == false) {
                    continue;
                }
                try {
                    Object obj = s.newInstance(null);
                    if (obj instanceof SecretKeyFactorySpi == false) {
                        continue;
                    }
                    SecretKeyFactorySpi spi = (SecretKeyFactorySpi)obj;
                    provider = s.getProvider();
                    this.spi = spi;
                    return spi;
                } catch (NoSuchAlgorithmException e) {
                    // ignore
                }
            }
            serviceIterator = null;
            return null;
        }
    }

    /**
     * Generates a {@code SecretKey} object from the provided key
     * specification (key material).
     *
     * @param keySpec the specification (key material) of the secret key
     *
     * @return the secret key
     *
     * @exception InvalidKeySpecException if the given key specification
     * is inappropriate for this secret-key factory to produce a secret key.
     */
    public final SecretKey generateSecret(KeySpec keySpec)
            throws InvalidKeySpecException {
        if (serviceIterator == null) {
            return spi.engineGenerateSecret(keySpec);
        }
        Exception failure = null;
        SecretKeyFactorySpi mySpi = spi;
        do {
            try {
                return mySpi.engineGenerateSecret(keySpec);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
                mySpi = nextSpi(mySpi);
            }
        } while (mySpi != null);
        if (failure instanceof InvalidKeySpecException) {
            throw (InvalidKeySpecException)failure;
        }
        throw new InvalidKeySpecException
                ("Could not generate secret key", failure);
    }

    /**
     * Returns a specification (key material) of the given key object
     * in the requested format.
     *
     * @param key the key
     * @param keySpec the requested format in which the key material shall be
     * returned
     *
     * @return the underlying key specification (key material) in the
     * requested format
     *
     * @exception InvalidKeySpecException if the requested key specification is
     * inappropriate for the given key (e.g., the algorithms associated with
     * {@code key} and {@code keySpec} do not match, or
     * {@code key} references a key on a cryptographic hardware device
     * whereas {@code keySpec} is the specification of a software-based
     * key), or the given key cannot be dealt with
     * (e.g., the given key has an algorithm or format not supported by this
     * secret-key factory).
     */
    public final KeySpec getKeySpec(SecretKey key, Class<?> keySpec)
            throws InvalidKeySpecException {
        if (serviceIterator == null) {
            return spi.engineGetKeySpec(key, keySpec);
        }
        Exception failure = null;
        SecretKeyFactorySpi mySpi = spi;
        do {
            try {
                return mySpi.engineGetKeySpec(key, keySpec);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
                mySpi = nextSpi(mySpi);
            }
        } while (mySpi != null);
        if (failure instanceof InvalidKeySpecException) {
            throw (InvalidKeySpecException)failure;
        }
        throw new InvalidKeySpecException
                ("Could not get key spec", failure);
    }

    /**
     * Translates a key object, whose provider may be unknown or potentially
     * untrusted, into a corresponding key object of this secret-key factory.
     *
     * @param key the key whose provider is unknown or untrusted
     *
     * @return the translated key
     *
     * @exception InvalidKeyException if the given key cannot be processed
     * by this secret-key factory.
     */
    public final SecretKey translateKey(SecretKey key)
            throws InvalidKeyException {
        if (serviceIterator == null) {
            return spi.engineTranslateKey(key);
        }
        Exception failure = null;
        SecretKeyFactorySpi mySpi = spi;
        do {
            try {
                return mySpi.engineTranslateKey(key);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
                mySpi = nextSpi(mySpi);
            }
        } while (mySpi != null);
        if (failure instanceof InvalidKeyException) {
            throw (InvalidKeyException)failure;
        }
        throw new InvalidKeyException
                ("Could not translate key", failure);
    }
}
