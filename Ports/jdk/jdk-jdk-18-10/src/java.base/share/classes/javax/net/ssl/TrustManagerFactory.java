/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.net.ssl;

import java.security.Security;
import java.security.*;
import java.util.Objects;

import sun.security.jca.GetInstance;

/**
 * This class acts as a factory for trust managers based on a
 * source of trust material. Each trust manager manages a specific
 * type of trust material for use by secure sockets. The trust
 * material is based on a KeyStore and/or provider-specific sources.
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard {@code TrustManagerFactory} algorithm:
 * <ul>
 * <li>{@code PKIX}</li>
 * </ul>
 * This algorithm is described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#trustmanagerfactory-algorithms">
 * TrustManagerFactory section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other algorithms are supported.
 *
 * @since 1.4
 * @see TrustManager
 */
public class TrustManagerFactory {
    // The provider
    private Provider provider;

    // The provider implementation (delegate)
    private TrustManagerFactorySpi factorySpi;

    // The name of the trust management algorithm.
    private String algorithm;

    /**
     * Obtains the default TrustManagerFactory algorithm name.
     *
     * <p>The default TrustManager can be changed at runtime by setting
     * the value of the {@code ssl.TrustManagerFactory.algorithm}
     * security property to the desired algorithm name.
     *
     * @see java.security.Security security properties
     * @return the default algorithm name as specified by the
     * {@code ssl.TrustManagerFactory.algorithm} security property, or an
     * implementation-specific default if no such property exists.
     */
    @SuppressWarnings("removal")
    public static final String getDefaultAlgorithm() {
        String type;
        type = AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public String run() {
                return Security.getProperty(
                    "ssl.TrustManagerFactory.algorithm");
            }
        });
        if (type == null) {
            type = "SunX509";
        }
        return type;
    }

    /**
     * Creates a TrustManagerFactory object.
     *
     * @param factorySpi the delegate
     * @param provider the provider
     * @param algorithm the algorithm
     */
    protected TrustManagerFactory(TrustManagerFactorySpi factorySpi,
            Provider provider, String algorithm) {
        this.factorySpi = factorySpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Returns the algorithm name of this <code>TrustManagerFactory</code>
     * object.
     *
     * <p>This is the same name that was specified in one of the
     * <code>getInstance</code> calls that created this
     * <code>TrustManagerFactory</code> object.
     *
     * @return the algorithm name of this <code>TrustManagerFactory</code>
     *          object
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns a <code>TrustManagerFactory</code> object that acts as a
     * factory for trust managers.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new TrustManagerFactory object encapsulating the
     * TrustManagerFactorySpi implementation from the first
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
     * @param algorithm the standard name of the requested trust management
     *          algorithm.  See the <a href=
     *          "{@docRoot}/../specs/security/standard-names.html#trustmanagerfactory-algorithms">
     *          TrustManagerFactory section</a> in the Java Security Standard
     *          Algorithm Names Specification for information about standard
     *          algorithm names.
     *
     * @return the new {@code TrustManagerFactory} object
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code TrustManagerFactorySpi} implementation for the
     *         specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final TrustManagerFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        GetInstance.Instance instance = GetInstance.getInstance
                ("TrustManagerFactory", TrustManagerFactorySpi.class,
                algorithm);
        return new TrustManagerFactory((TrustManagerFactorySpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns a <code>TrustManagerFactory</code> object that acts as a
     * factory for trust managers.
     *
     * <p> A new KeyManagerFactory object encapsulating the
     * KeyManagerFactorySpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the standard name of the requested trust management
     *          algorithm.  See the <a href=
     *          "{@docRoot}/../specs/security/standard-names.html#trustmanagerfactory-algorithms">
     *          TrustManagerFactory section</a> in the Java Security Standard
     *          Algorithm Names Specification for information about standard
     *          algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return the new {@code TrustManagerFactory} object
     *
     * @throws IllegalArgumentException if the provider name is
     *         {@code null} or empty
     *
     * @throws NoSuchAlgorithmException if a {@code TrustManagerFactorySpi}
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
    public static final TrustManagerFactory getInstance(String algorithm,
            String provider) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        GetInstance.Instance instance = GetInstance.getInstance
                ("TrustManagerFactory", TrustManagerFactorySpi.class,
                algorithm, provider);
        return new TrustManagerFactory((TrustManagerFactorySpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns a <code>TrustManagerFactory</code> object that acts as a
     * factory for trust managers.
     *
     * <p> A new TrustManagerFactory object encapsulating the
     * TrustManagerFactorySpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the standard name of the requested trust management
     *          algorithm.  See the <a href=
     *          "{@docRoot}/../specs/security/standard-names.html#trustmanagerfactory-algorithms">
     *          TrustManagerFactory section</a> in the Java Security Standard
     *          Algorithm Names Specification for information about standard
     *          algorithm names.
     *
     * @param provider an instance of the provider.
     *
     * @return the new {@code TrustManagerFactory} object
     *
     * @throws IllegalArgumentException if the provider is {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code TrustManagerFactorySpi}
     *         implementation for the specified algorithm is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final TrustManagerFactory getInstance(String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        GetInstance.Instance instance = GetInstance.getInstance
                ("TrustManagerFactory", TrustManagerFactorySpi.class,
                algorithm, provider);
        return new TrustManagerFactory((TrustManagerFactorySpi)instance.impl,
                instance.provider, algorithm);
    }

    /**
     * Returns the provider of this <code>TrustManagerFactory</code> object.
     *
     * @return the provider of this <code>TrustManagerFactory</code> object
     */
    public final Provider getProvider() {
        return this.provider;
    }


    /**
     * Initializes this factory with a source of certificate
     * authorities and related trust material.
     * <P>
     * The provider typically uses a KeyStore as a basis for making
     * trust decisions.
     * <P>
     * For more flexible initialization, please see
     * {@link #init(ManagerFactoryParameters)}.
     *
     * @param ks the key store, or null
     * @throws KeyStoreException if this operation fails
     */
    public final void init(KeyStore ks) throws KeyStoreException {
        factorySpi.engineInit(ks);
    }


    /**
     * Initializes this factory with a source of provider-specific
     * trust material.
     * <P>
     * In some cases, initialization parameters other than a keystore
     * may be needed by a provider.  Users of that particular provider
     * are expected to pass an implementation of the appropriate
     * <CODE>ManagerFactoryParameters</CODE> as defined by the
     * provider.  The provider can then call the specified methods in
     * the <CODE>ManagerFactoryParameters</CODE> implementation to obtain the
     * needed information.
     *
     * @param spec an implementation of a provider-specific parameter
     *          specification
     * @throws InvalidAlgorithmParameterException if an error is
     *          encountered
     */
    public final void init(ManagerFactoryParameters spec) throws
            InvalidAlgorithmParameterException {
        factorySpi.engineInit(spec);
    }


    /**
     * Returns one trust manager for each type of trust material.
     *
     * @throws IllegalStateException if the factory is not initialized.
     *
     * @return the trust managers
     */
    public final TrustManager[] getTrustManagers() {
        return factorySpi.engineGetTrustManagers();
    }
}
