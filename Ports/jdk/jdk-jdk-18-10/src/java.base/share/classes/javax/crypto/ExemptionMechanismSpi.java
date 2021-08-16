/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Key;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the <code>ExemptionMechanism</code> class.
 * All the abstract methods in this class must be implemented by each
 * cryptographic service provider who wishes to supply the implementation
 * of a particular exemption mechanism.
 *
 * @author Sharon Liu
 *
 * @since 1.4
 */

public abstract class ExemptionMechanismSpi {

    /**
     * Constructor for subclasses to call.
     */
    public ExemptionMechanismSpi() {}

    /**
     * Returns the length in bytes that an output buffer would need to be in
     * order to hold the result of the next
     * {@link #engineGenExemptionBlob(byte[], int) engineGenExemptionBlob}
     * operation, given the input length <code>inputLen</code> (in bytes).
     *
     * <p>The actual output length of the next
     * {@link #engineGenExemptionBlob(byte[], int) engineGenExemptionBlob}
     * call may be smaller than the length returned by this method.
     *
     * @param inputLen the input length (in bytes)
     *
     * @return the required output buffer size (in bytes)
     */
    protected abstract int engineGetOutputSize(int inputLen);

    /**
     * Initializes this exemption mechanism with a key.
     *
     * <p>If this exemption mechanism requires any algorithm parameters
     * that cannot be derived from the given <code>key</code>, the underlying
     * exemption mechanism implementation is supposed to generate the required
     * parameters itself (using provider-specific default values); in the case
     * that algorithm parameters must be specified by the caller, an
     * <code>InvalidKeyException</code> is raised.
     *
     * @param key the key for this exemption mechanism
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * this exemption mechanism.
     * @exception ExemptionMechanismException if problem(s) encountered in the
     * process of initializing.
     */
    protected abstract void engineInit(Key key)
    throws InvalidKeyException, ExemptionMechanismException;

    /**
     * Initializes this exemption mechanism with a key and a set of algorithm
     * parameters.
     *
     * <p>If this exemption mechanism requires any algorithm parameters and
     * <code>params</code> is null, the underlying exemption mechanism
     * implementation is supposed to generate the required parameters
     * itself (using provider-specific default values); in the case that
     * algorithm parameters must be specified by the caller, an
     * <code>InvalidAlgorithmParameterException</code> is raised.
     *
     * @param key the key for this exemption mechanism
     * @param params the algorithm parameters
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * this exemption mechanism.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this exemption mechanism.
     * @exception ExemptionMechanismException if problem(s) encountered in the
     * process of initializing.
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params)
    throws InvalidKeyException, InvalidAlgorithmParameterException,
    ExemptionMechanismException;

    /**
     * Initializes this exemption mechanism with a key and a set of algorithm
     * parameters.
     *
     * <p>If this exemption mechanism requires any algorithm parameters
     * and <code>params</code> is null, the underlying exemption mechanism
     * implementation is supposed to generate the required parameters
     * itself (using provider-specific default values); in the case that
     * algorithm parameters must be specified by the caller, an
     * <code>InvalidAlgorithmParameterException</code> is raised.
     *
     * @param key the key for this exemption mechanism
     * @param params the algorithm parameters
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * this exemption mechanism.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this exemption mechanism.
     * @exception ExemptionMechanismException if problem(s) encountered in the
     * process of initializing.
     */
    protected abstract void engineInit(Key key, AlgorithmParameters params)
    throws InvalidKeyException, InvalidAlgorithmParameterException,
    ExemptionMechanismException;

    /**
     * Generates the exemption mechanism key blob.
     *
     * @return the new buffer with the result key blob.
     *
     * @exception ExemptionMechanismException if problem(s) encountered in the
     * process of generating.
     */
    protected abstract byte[] engineGenExemptionBlob()
        throws ExemptionMechanismException;

    /**
     * Generates the exemption mechanism key blob, and stores the result in
     * the <code>output</code> buffer, starting at <code>outputOffset</code>
     * inclusive.
     *
     * <p>If the <code>output</code> buffer is too small to hold the result,
     * a <code>ShortBufferException</code> is thrown. In this case, repeat this
     * call with a larger output buffer. Use
     * {@link #engineGetOutputSize(int) engineGetOutputSize} to determine
     * how big the output buffer should be.
     *
     * @param output the buffer for the result
     * @param outputOffset the offset in <code>output</code> where the result
     * is stored
     *
     * @return the number of bytes stored in <code>output</code>
     *
     * @exception ShortBufferException if the given output buffer is too small
     * to hold the result.
     * @exception ExemptionMechanismException if problem(s) encountered in the
     * process of generating.
     */
    protected abstract int engineGenExemptionBlob
    (byte[] output, int outputOffset)
        throws ShortBufferException, ExemptionMechanismException;
}
