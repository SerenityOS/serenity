/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidAlgorithmParameterException;

/**
 *
 * The <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the {@link CertPathValidator CertPathValidator} class. All
 * {@code CertPathValidator} implementations must include a class (the
 * SPI class) that extends this class ({@code CertPathValidatorSpi})
 * and implements all of its methods. In general, instances of this class
 * should only be accessed through the {@code CertPathValidator} class.
 * For details, see the Java Cryptography Architecture.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Instances of this class need not be protected against concurrent
 * access from multiple threads. Threads that need to access a single
 * {@code CertPathValidatorSpi} instance concurrently should synchronize
 * amongst themselves and provide the necessary locking before calling the
 * wrapping {@code CertPathValidator} object.
 * <p>
 * However, implementations of {@code CertPathValidatorSpi} may still
 * encounter concurrency issues, since multiple threads each
 * manipulating a different {@code CertPathValidatorSpi} instance need not
 * synchronize.
 *
 * @since       1.4
 * @author      Yassir Elley
 */
public abstract class CertPathValidatorSpi {

    /**
     * The default constructor.
     */
    public CertPathValidatorSpi() {}

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
    public abstract CertPathValidatorResult
        engineValidate(CertPath certPath, CertPathParameters params)
        throws CertPathValidatorException, InvalidAlgorithmParameterException;

    /**
     * Returns a {@code CertPathChecker} that this implementation uses to
     * check the revocation status of certificates. A PKIX implementation
     * returns objects of type {@code PKIXRevocationChecker}.
     *
     * <p>The primary purpose of this method is to allow callers to specify
     * additional input parameters and options specific to revocation checking.
     * See the class description of {@code CertPathValidator} for an example.
     *
     * <p>This method was added to version 1.8 of the Java Platform Standard
     * Edition. In order to maintain backwards compatibility with existing
     * service providers, this method cannot be abstract and by default throws
     * an {@code UnsupportedOperationException}.
     *
     * @return a {@code CertPathChecker} that this implementation uses to
     * check the revocation status of certificates
     * @throws UnsupportedOperationException if this method is not supported
     * @since 1.8
     */
    public CertPathChecker engineGetRevocationChecker() {
        throw new UnsupportedOperationException();
    }
}
