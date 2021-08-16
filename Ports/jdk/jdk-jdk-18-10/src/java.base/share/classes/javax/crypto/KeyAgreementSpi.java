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

package javax.crypto;

import java.security.*;
import java.security.spec.*;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the <code>KeyAgreement</code> class.
 * All the abstract methods in this class must be implemented by each
 * cryptographic service provider who wishes to supply the implementation
 * of a particular key agreement algorithm.
 *
 * <p> The keys involved in establishing a shared secret are created by one
 * of the
 * key generators (<code>KeyPairGenerator</code> or
 * <code>KeyGenerator</code>), a <code>KeyFactory</code>, or as a result from
 * an intermediate phase of the key agreement protocol
 * ({@link #engineDoPhase(java.security.Key, boolean) engineDoPhase}).
 *
 * <p> For each of the correspondents in the key exchange,
 * <code>engineDoPhase</code>
 * needs to be called. For example, if the key exchange is with one other
 * party, <code>engineDoPhase</code> needs to be called once, with the
 * <code>lastPhase</code> flag set to <code>true</code>.
 * If the key exchange is
 * with two other parties, <code>engineDoPhase</code> needs to be called twice,
 * the first time setting the <code>lastPhase</code> flag to
 * <code>false</code>, and the second time setting it to <code>true</code>.
 * There may be any number of parties involved in a key exchange.
 *
 * @author Jan Luehe
 *
 * @see KeyGenerator
 * @see SecretKey
 * @since 1.4
 */

public abstract class KeyAgreementSpi {

    /**
     * Constructor for subclasses to call.
     */
    public KeyAgreementSpi() {}

    /**
     * Initializes this key agreement with the given key and source of
     * randomness. The given key is required to contain all the algorithm
     * parameters required for this key agreement.
     *
     * <p> If the key agreement algorithm requires random bytes, it gets them
     * from the given source of randomness, <code>random</code>.
     * However, if the underlying
     * algorithm implementation does not require any random bytes,
     * <code>random</code> is ignored.
     *
     * @param key the party's private information. For example, in the case
     * of the Diffie-Hellman key agreement, this would be the party's own
     * Diffie-Hellman private key.
     * @param random the source of randomness
     *
     * @exception InvalidKeyException if the given key is
     * inappropriate for this key agreement, e.g., is of the wrong type or
     * has an incompatible algorithm type.
     */
    protected abstract void engineInit(Key key, SecureRandom random)
        throws InvalidKeyException;

    /**
     * Initializes this key agreement with the given key, set of
     * algorithm parameters, and source of randomness.
     *
     * @param key the party's private information. For example, in the case
     * of the Diffie-Hellman key agreement, this would be the party's own
     * Diffie-Hellman private key.
     * @param params the key agreement parameters
     * @param random the source of randomness
     *
     * @exception InvalidKeyException if the given key is
     * inappropriate for this key agreement, e.g., is of the wrong type or
     * has an incompatible algorithm type.
     * @exception InvalidAlgorithmParameterException if the given parameters
     * are inappropriate for this key agreement.
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params,
                                       SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Executes the next phase of this key agreement with the given
     * key that was received from one of the other parties involved in this key
     * agreement.
     *
     * @param key the key for this phase. For example, in the case of
     * Diffie-Hellman between 2 parties, this would be the other party's
     * Diffie-Hellman public key.
     * @param lastPhase flag which indicates whether or not this is the last
     * phase of this key agreement.
     *
     * @return the (intermediate) key resulting from this phase, or null if
     * this phase does not yield a key
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * this phase.
     * @exception IllegalStateException if this key agreement has not been
     * initialized.
     */
    protected abstract Key engineDoPhase(Key key, boolean lastPhase)
        throws InvalidKeyException, IllegalStateException;

    /**
     * Generates the shared secret and returns it in a new buffer.
     *
     * <p>This method resets this {@code KeyAgreementSpi} object to the state
     * that it was in after the most recent call to one of the {@code init}
     * methods. After a call to {@code generateSecret}, the object can be reused
     * for further key agreement operations by calling {@code doPhase} to supply
     * new keys, and then calling {@code generateSecret} to produce a new
     * secret. In this case, the private information and algorithm parameters
     * supplied to {@code init} will be used for multiple key agreement
     * operations. The {@code init} method can be called after
     * {@code generateSecret} to change the private information used in
     * subsequent operations.
     *
     * @return the new buffer with the shared secret
     *
     * @exception IllegalStateException if this key agreement has not been
     * initialized or if {@code doPhase} has not been called to supply the
     * keys for all parties in the agreement
     */
    protected abstract byte[] engineGenerateSecret()
        throws IllegalStateException;

    /**
     * Generates the shared secret, and places it into the buffer
     * <code>sharedSecret</code>, beginning at <code>offset</code> inclusive.
     *
     * <p>If the <code>sharedSecret</code> buffer is too small to hold the
     * result, a <code>ShortBufferException</code> is thrown.
     * In this case, this call should be repeated with a larger output buffer.
     *
     * <p>This method resets this {@code KeyAgreementSpi} object to the state
     * that it was in after the most recent call to one of the {@code init}
     * methods. After a call to {@code generateSecret}, the object can be reused
     * for further key agreement operations by calling {@code doPhase} to supply
     * new keys, and then calling {@code generateSecret} to produce a new
     * secret. In this case, the private information and algorithm parameters
     * supplied to {@code init} will be used for multiple key agreement
     * operations. The {@code init} method can be called after
     * {@code generateSecret} to change the private information used in
     * subsequent operations.
     *
     * @param sharedSecret the buffer for the shared secret
     * @param offset the offset in <code>sharedSecret</code> where the
     * shared secret will be stored
     *
     * @return the number of bytes placed into <code>sharedSecret</code>
     *
     * @exception IllegalStateException if this key agreement has not been
     * initialized or if {@code doPhase} has not been called to supply the
     * keys for all parties in the agreement
     * @exception ShortBufferException if the given output buffer is too small
     * to hold the secret
     */
    protected abstract int engineGenerateSecret(byte[] sharedSecret,
                                                int offset)
        throws IllegalStateException, ShortBufferException;

    /**
     * Creates the shared secret and returns it as a secret key object
     * of the requested algorithm type.
     *
     * <p>This method resets this {@code KeyAgreementSpi} object to the state
     * that it was in after the most recent call to one of the {@code init}
     * methods. After a call to {@code generateSecret}, the object can be reused
     * for further key agreement operations by calling {@code doPhase} to supply
     * new keys, and then calling {@code generateSecret} to produce a new
     * secret. In this case, the private information and algorithm parameters
     * supplied to {@code init} will be used for multiple key agreement
     * operations. The {@code init} method can be called after
     * {@code generateSecret} to change the private information used in
     * subsequent operations.
     *
     * @param algorithm the requested secret key algorithm
     *
     * @return the shared secret key
     *
     * @exception IllegalStateException if this key agreement has not been
     * initialized or if {@code doPhase} has not been called to supply the
     * keys for all parties in the agreement
     * @exception NoSuchAlgorithmException if the requested secret key
     * algorithm is not available
     * @exception InvalidKeyException if the shared secret key material cannot
     * be used to generate a secret key of the requested algorithm type (e.g.,
     * the key material is too short)
     */
    protected abstract SecretKey engineGenerateSecret(String algorithm)
        throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException;
}
