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
import java.util.Objects;

import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;

/**
 * A class for validating certification paths (also known as certificate
 * chains).
 * <p>
 * This class uses a provider-based architecture.
 * To create a {@code CertPathValidator},
 * call one of the static {@code getInstance} methods, passing in the
 * algorithm name of the {@code CertPathValidator} desired and
 * optionally the name of the provider desired.
 *
 * <p>Once a {@code CertPathValidator} object has been created, it can
 * be used to validate certification paths by calling the {@link #validate
 * validate} method and passing it the {@code CertPath} to be validated
 * and an algorithm-specific set of parameters. If successful, the result is
 * returned in an object that implements the
 * {@code CertPathValidatorResult} interface.
 *
 * <p>The {@link #getRevocationChecker} method allows an application to specify
 * additional algorithm-specific parameters and options used by the
 * {@code CertPathValidator} when checking the revocation status of
 * certificates. Here is an example demonstrating how it is used with the PKIX
 * algorithm:
 *
 * <pre>
 * CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
 * PKIXRevocationChecker rc = (PKIXRevocationChecker)cpv.getRevocationChecker();
 * rc.setOptions(EnumSet.of(Option.SOFT_FAIL));
 * params.addCertPathChecker(rc);
 * CertPathValidatorResult cpvr = cpv.validate(path, params);
 * </pre>
 *
 * <p>Every implementation of the Java platform is required to support the
 * following standard {@code CertPathValidator} algorithm:
 * <ul>
 * <li>{@code PKIX}</li>
 * </ul>
 * This algorithm is described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#certpathvalidator-algorithms">
 * CertPathValidator section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other algorithms are supported.
 *
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * The static methods of this class are guaranteed to be thread-safe.
 * Multiple threads may concurrently invoke the static methods defined in
 * this class with no ill effects.
 * <p>
 * However, this is not true for the non-static methods defined by this class.
 * Unless otherwise documented by a specific provider, threads that need to
 * access a single {@code CertPathValidator} instance concurrently should
 * synchronize amongst themselves and provide the necessary locking. Multiple
 * threads each manipulating a different {@code CertPathValidator}
 * instance need not synchronize.
 *
 * @see CertPath
 *
 * @since       1.4
 * @author      Yassir Elley
 */
public class CertPathValidator {

    /*
     * Constant to lookup in the Security properties file to determine
     * the default certpathvalidator type. In the Security properties file,
     * the default certpathvalidator type is given as:
     * <pre>
     * certpathvalidator.type=PKIX
     * </pre>
     */
    private static final String CPV_TYPE = "certpathvalidator.type";
    private final CertPathValidatorSpi validatorSpi;
    private final Provider provider;
    private final String algorithm;

    /**
     * Creates a {@code CertPathValidator} object of the given algorithm,
     * and encapsulates the given provider implementation (SPI object) in it.
     *
     * @param validatorSpi the provider implementation
     * @param provider the provider
     * @param algorithm the algorithm name
     */
    protected CertPathValidator(CertPathValidatorSpi validatorSpi,
        Provider provider, String algorithm)
    {
        this.validatorSpi = validatorSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Returns a {@code CertPathValidator} object that implements the
     * specified algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new CertPathValidator object encapsulating the
     * CertPathValidatorSpi implementation from the first
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
     * @param algorithm the name of the requested {@code CertPathValidator}
     * algorithm. See the CertPathValidator section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#certpathvalidator-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @return a {@code CertPathValidator} object that implements the
     *         specified algorithm
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code CertPathValidatorSpi} implementation for the
     *         specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static CertPathValidator getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("CertPathValidator",
            CertPathValidatorSpi.class, algorithm);
        return new CertPathValidator((CertPathValidatorSpi)instance.impl,
            instance.provider, algorithm);
    }

    /**
     * Returns a {@code CertPathValidator} object that implements the
     * specified algorithm.
     *
     * <p> A new CertPathValidator object encapsulating the
     * CertPathValidatorSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the requested {@code CertPathValidator}
     * algorithm. See the CertPathValidator section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#certpathvalidator-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return a {@code CertPathValidator} object that implements the
     *         specified algorithm
     *
     * @throws IllegalArgumentException if the {@code provider} is
     *         {@code null} or empty
     *
     * @throws NoSuchAlgorithmException if a {@code CertPathValidatorSpi}
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
    public static CertPathValidator getInstance(String algorithm,
            String provider) throws NoSuchAlgorithmException,
            NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("CertPathValidator",
            CertPathValidatorSpi.class, algorithm, provider);
        return new CertPathValidator((CertPathValidatorSpi)instance.impl,
            instance.provider, algorithm);
    }

    /**
     * Returns a {@code CertPathValidator} object that implements the
     * specified algorithm.
     *
     * <p> A new CertPathValidator object encapsulating the
     * CertPathValidatorSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the name of the requested {@code CertPathValidator}
     * algorithm. See the CertPathValidator section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#certpathvalidator-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the provider.
     *
     * @return a {@code CertPathValidator} object that implements the
     *          specified algorithm
     *
     * @throws IllegalArgumentException if the {@code provider} is
     *         {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code CertPathValidatorSpi}
     *         implementation for the specified algorithm is not available
     *         from the specified Provider object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static CertPathValidator getInstance(String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = GetInstance.getInstance("CertPathValidator",
            CertPathValidatorSpi.class, algorithm, provider);
        return new CertPathValidator((CertPathValidatorSpi)instance.impl,
            instance.provider, algorithm);
    }

    /**
     * Returns the {@code Provider} of this
     * {@code CertPathValidator}.
     *
     * @return the {@code Provider} of this {@code CertPathValidator}
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Returns the algorithm name of this {@code CertPathValidator}.
     *
     * @return the algorithm name of this {@code CertPathValidator}
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Validates the specified certification path using the specified
     * algorithm parameter set.
     * <p>
     * The {@code CertPath} specified must be of a type that is
     * supported by the validation algorithm, otherwise an
     * {@code InvalidAlgorithmParameterException} will be thrown. For
     * example, a {@code CertPathValidator} that implements the PKIX
     * algorithm validates {@code CertPath} objects of type X.509.
     *
     * @param certPath the {@code CertPath} to be validated
     * @param params the algorithm parameters
     * @return the result of the validation algorithm
     * @throws    CertPathValidatorException if the {@code CertPath}
     * does not validate
     * @throws    InvalidAlgorithmParameterException if the specified
     * parameters or the type of the specified {@code CertPath} are
     * inappropriate for this {@code CertPathValidator}
     */
    public final CertPathValidatorResult validate(CertPath certPath,
        CertPathParameters params)
        throws CertPathValidatorException, InvalidAlgorithmParameterException
    {
        return validatorSpi.engineValidate(certPath, params);
    }

    /**
     * Returns the default {@code CertPathValidator} type as specified by
     * the {@code certpathvalidator.type} security property, or the string
     * {@literal "PKIX"} if no such property exists.
     *
     * <p>The default {@code CertPathValidator} type can be used by
     * applications that do not want to use a hard-coded type when calling one
     * of the {@code getInstance} methods, and want to provide a default
     * type in case a user does not specify its own.
     *
     * <p>The default {@code CertPathValidator} type can be changed by
     * setting the value of the {@code certpathvalidator.type} security
     * property to the desired type.
     *
     * @see java.security.Security security properties
     * @return the default {@code CertPathValidator} type as specified
     * by the {@code certpathvalidator.type} security property, or the string
     * {@literal "PKIX"} if no such property exists.
     */
    public static final String getDefaultType() {
        @SuppressWarnings("removal")
        String cpvtype =
            AccessController.doPrivileged(new PrivilegedAction<>() {
                public String run() {
                    return Security.getProperty(CPV_TYPE);
                }
            });
        return (cpvtype == null) ? "PKIX" : cpvtype;
    }

    /**
     * Returns a {@code CertPathChecker} that the encapsulated
     * {@code CertPathValidatorSpi} implementation uses to check the revocation
     * status of certificates. A PKIX implementation returns objects of
     * type {@code PKIXRevocationChecker}. Each invocation of this method
     * returns a new instance of {@code CertPathChecker}.
     *
     * <p>The primary purpose of this method is to allow callers to specify
     * additional input parameters and options specific to revocation checking.
     * See the class description for an example.
     *
     * @return a {@code CertPathChecker}
     * @throws UnsupportedOperationException if the service provider does not
     *         support this method
     * @since 1.8
     */
    public final CertPathChecker getRevocationChecker() {
        return validatorSpi.engineGetRevocationChecker();
    }
}
