/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Objects;

import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;

/**
 * A class for retrieving {@code Certificate}s and {@code CRL}s
 * from a repository.
 * <p>
 * This class uses a provider-based architecture.
 * To create a {@code CertStore}, call one of the static
 * {@code getInstance} methods, passing in the type of
 * {@code CertStore} desired, any applicable initialization parameters
 * and optionally the name of the provider desired.
 * <p>
 * Once the {@code CertStore} has been created, it can be used to
 * retrieve {@code Certificate}s and {@code CRL}s by calling its
 * {@link #getCertificates(CertSelector selector) getCertificates} and
 * {@link #getCRLs(CRLSelector selector) getCRLs} methods.
 * <p>
 * Unlike a {@link java.security.KeyStore KeyStore}, which provides access
 * to a cache of private keys and trusted certificates, a
 * {@code CertStore} is designed to provide access to a potentially
 * vast repository of untrusted certificates and CRLs. For example, an LDAP
 * implementation of {@code CertStore} provides access to certificates
 * and CRLs stored in one or more directories using the LDAP protocol and the
 * schema as defined in the RFC service attribute.
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard {@code CertStore} type:
 * <ul>
 * <li>{@code Collection}</li>
 * </ul>
 * This type is described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#certstore-types">
 * CertStore section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other types are supported.
 *
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * All public methods of {@code CertStore} objects must be thread-safe.
 * That is, multiple threads may concurrently invoke these methods on a
 * single {@code CertStore} object (or more than one) with no
 * ill effects. This allows a {@code CertPathBuilder} to search for a
 * CRL while simultaneously searching for further certificates, for instance.
 * <p>
 * The static methods of this class are also guaranteed to be thread-safe.
 * Multiple threads may concurrently invoke the static methods defined in
 * this class with no ill effects.
 *
 * @since       1.4
 * @author      Sean Mullan, Steve Hanna
 */
public class CertStore {
    /*
     * Constant to lookup in the Security properties file to determine
     * the default certstore type. In the Security properties file, the
     * default certstore type is given as:
     * <pre>
     * certstore.type=LDAP
     * </pre>
     */
    private static final String CERTSTORE_TYPE = "certstore.type";
    private CertStoreSpi storeSpi;
    private Provider provider;
    private String type;
    private CertStoreParameters params;

    /**
     * Creates a {@code CertStore} object of the given type, and
     * encapsulates the given provider implementation (SPI object) in it.
     *
     * @param storeSpi the provider implementation
     * @param provider the provider
     * @param type the type
     * @param params the initialization parameters (may be {@code null})
     */
    protected CertStore(CertStoreSpi storeSpi, Provider provider,
                        String type, CertStoreParameters params) {
        this.storeSpi = storeSpi;
        this.provider = provider;
        this.type = type;
        if (params != null)
            this.params = (CertStoreParameters) params.clone();
    }

    /**
     * Returns a {@code Collection} of {@code Certificate}s that
     * match the specified selector. If no {@code Certificate}s
     * match the selector, an empty {@code Collection} will be returned.
     * <p>
     * For some {@code CertStore} types, the resulting
     * {@code Collection} may not contain <b>all</b> of the
     * {@code Certificate}s that match the selector. For instance,
     * an LDAP {@code CertStore} may not search all entries in the
     * directory. Instead, it may just search entries that are likely to
     * contain the {@code Certificate}s it is looking for.
     * <p>
     * Some {@code CertStore} implementations (especially LDAP
     * {@code CertStore}s) may throw a {@code CertStoreException}
     * unless a non-null {@code CertSelector} is provided that
     * includes specific criteria that can be used to find the certificates.
     * Issuer and/or subject names are especially useful criteria.
     *
     * @param selector A {@code CertSelector} used to select which
     *  {@code Certificate}s should be returned. Specify {@code null}
     *  to return all {@code Certificate}s (if supported).
     * @return A {@code Collection} of {@code Certificate}s that
     *         match the specified selector (never {@code null})
     * @throws CertStoreException if an exception occurs
     */
    public final Collection<? extends Certificate> getCertificates
            (CertSelector selector) throws CertStoreException {
        return storeSpi.engineGetCertificates(selector);
    }

    /**
     * Returns a {@code Collection} of {@code CRL}s that
     * match the specified selector. If no {@code CRL}s
     * match the selector, an empty {@code Collection} will be returned.
     * <p>
     * For some {@code CertStore} types, the resulting
     * {@code Collection} may not contain <b>all</b> of the
     * {@code CRL}s that match the selector. For instance,
     * an LDAP {@code CertStore} may not search all entries in the
     * directory. Instead, it may just search entries that are likely to
     * contain the {@code CRL}s it is looking for.
     * <p>
     * Some {@code CertStore} implementations (especially LDAP
     * {@code CertStore}s) may throw a {@code CertStoreException}
     * unless a non-null {@code CRLSelector} is provided that
     * includes specific criteria that can be used to find the CRLs.
     * Issuer names and/or the certificate to be checked are especially useful.
     *
     * @param selector A {@code CRLSelector} used to select which
     *  {@code CRL}s should be returned. Specify {@code null}
     *  to return all {@code CRL}s (if supported).
     * @return A {@code Collection} of {@code CRL}s that
     *         match the specified selector (never {@code null})
     * @throws CertStoreException if an exception occurs
     */
    public final Collection<? extends CRL> getCRLs(CRLSelector selector)
            throws CertStoreException {
        return storeSpi.engineGetCRLs(selector);
    }

    /**
     * Returns a {@code CertStore} object that implements the specified
     * {@code CertStore} type and is initialized with the specified
     * parameters.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new CertStore object encapsulating the
     * CertStoreSpi implementation from the first
     * Provider that supports the specified type is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * <p>The {@code CertStore} that is returned is initialized with the
     * specified {@code CertStoreParameters}. The type of parameters
     * needed may vary between different types of {@code CertStore}s.
     * Note that the specified {@code CertStoreParameters} object is
     * cloned.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param type the name of the requested {@code CertStore} type.
     * See the CertStore section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#certstore-types">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard types.
     *
     * @param params the initialization parameters (may be {@code null}).
     *
     * @return a {@code CertStore} object that implements the specified
     *          {@code CertStore} type
     *
     * @throws InvalidAlgorithmParameterException if the specified
     *         initialization parameters are inappropriate for this
     *         {@code CertStore}
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code CertStoreSpi} implementation for the specified type
     *
     * @throws NullPointerException if {@code type} is {@code null}
     *
     * @see java.security.Provider
     */
    public static CertStore getInstance(String type, CertStoreParameters params)
            throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException {
        Objects.requireNonNull(type, "null type name");
        try {
            Instance instance = GetInstance.getInstance("CertStore",
                CertStoreSpi.class, type, params);
            return new CertStore((CertStoreSpi)instance.impl,
                instance.provider, type, params);
        } catch (NoSuchAlgorithmException e) {
            return handleException(e);
        }
    }

    private static CertStore handleException(NoSuchAlgorithmException e)
            throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        Throwable cause = e.getCause();
        if (cause instanceof InvalidAlgorithmParameterException) {
            throw (InvalidAlgorithmParameterException)cause;
        }
        throw e;
    }

    /**
     * Returns a {@code CertStore} object that implements the specified
     * {@code CertStore} type.
     *
     * <p> A new CertStore object encapsulating the
     * CertStoreSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * <p>The {@code CertStore} that is returned is initialized with the
     * specified {@code CertStoreParameters}. The type of parameters
     * needed may vary between different types of {@code CertStore}s.
     * Note that the specified {@code CertStoreParameters} object is
     * cloned.
     *
     * @param type the requested {@code CertStore} type.
     * See the CertStore section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#certstore-types">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard types.
     *
     * @param params the initialization parameters (may be {@code null}).
     *
     * @param provider the name of the provider.
     *
     * @return a {@code CertStore} object that implements the
     *          specified type
     *
     * @throws IllegalArgumentException if the {@code provider} is
     *         {@code null} or empty
     *
     * @throws InvalidAlgorithmParameterException if the specified
     *         initialization parameters are inappropriate for this
     *         {@code CertStore}
     *
     * @throws NoSuchAlgorithmException if a {@code CertStoreSpi}
     *         implementation for the specified type is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code type} is {@code null}
     *
     * @see java.security.Provider
     */
    public static CertStore getInstance(String type,
            CertStoreParameters params, String provider)
            throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(type, "null type name");
        try {
            Instance instance = GetInstance.getInstance("CertStore",
                CertStoreSpi.class, type, params, provider);
            return new CertStore((CertStoreSpi)instance.impl,
                instance.provider, type, params);
        } catch (NoSuchAlgorithmException e) {
            return handleException(e);
        }
    }

    /**
     * Returns a {@code CertStore} object that implements the specified
     * {@code CertStore} type.
     *
     * <p> A new CertStore object encapsulating the
     * CertStoreSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * <p>The {@code CertStore} that is returned is initialized with the
     * specified {@code CertStoreParameters}. The type of parameters
     * needed may vary between different types of {@code CertStore}s.
     * Note that the specified {@code CertStoreParameters} object is
     * cloned.
     *
     * @param type the requested {@code CertStore} type.
     * See the CertStore section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#certstore-types">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard types.
     *
     * @param params the initialization parameters (may be {@code null}).
     *
     * @param provider the provider.
     *
     * @return a {@code CertStore} object that implements the
     *          specified type
     *
     * @throws IllegalArgumentException if the {@code provider} is
     *         null
     *
     * @throws InvalidAlgorithmParameterException if the specified
     *         initialization parameters are inappropriate for this
     *         {@code CertStore}
     *
     * @throws NoSuchAlgorithmException if a {@code CertStoreSpi}
     *         implementation for the specified type is not available
     *         from the specified Provider object
     *
     * @throws NullPointerException if {@code type} is {@code null}
     *
     * @see java.security.Provider
     */
    public static CertStore getInstance(String type, CertStoreParameters params,
            Provider provider) throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        Objects.requireNonNull(type, "null type name");
        try {
            Instance instance = GetInstance.getInstance("CertStore",
                CertStoreSpi.class, type, params, provider);
            return new CertStore((CertStoreSpi)instance.impl,
                instance.provider, type, params);
        } catch (NoSuchAlgorithmException e) {
            return handleException(e);
        }
    }

    /**
     * Returns the parameters used to initialize this {@code CertStore}.
     * Note that the {@code CertStoreParameters} object is cloned before
     * it is returned.
     *
     * @return the parameters used to initialize this {@code CertStore}
     * (may be {@code null})
     */
    public final CertStoreParameters getCertStoreParameters() {
        return (params == null ? null : (CertStoreParameters) params.clone());
    }

    /**
     * Returns the type of this {@code CertStore}.
     *
     * @return the type of this {@code CertStore}
     */
    public final String getType() {
        return this.type;
    }

    /**
     * Returns the provider of this {@code CertStore}.
     *
     * @return the provider of this {@code CertStore}
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Returns the default {@code CertStore} type as specified by the
     * {@code certstore.type} security property, or the string
     * {@literal "LDAP"} if no such property exists.
     *
     * <p>The default {@code CertStore} type can be used by applications
     * that do not want to use a hard-coded type when calling one of the
     * {@code getInstance} methods, and want to provide a default
     * {@code CertStore} type in case a user does not specify its own.
     *
     * <p>The default {@code CertStore} type can be changed by setting
     * the value of the {@code certstore.type} security property to the
     * desired type.
     *
     * @see java.security.Security security properties
     * @return the default {@code CertStore} type as specified by the
     * {@code certstore.type} security property, or the string
     * {@literal "LDAP"} if no such property exists.
     */
    @SuppressWarnings("removal")
    public static final String getDefaultType() {
        String cstype;
        cstype = AccessController.doPrivileged(new PrivilegedAction<>() {
            public String run() {
                return Security.getProperty(CERTSTORE_TYPE);
            }
        });
        if (cstype == null) {
            cstype = "LDAP";
        }
        return cstype;
    }
}
