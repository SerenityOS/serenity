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

package com.sun.crypto.provider;

import java.util.*;
import java.lang.*;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.PrivilegedAction;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.KeyAgreementSpi;
import javax.crypto.ShortBufferException;
import javax.crypto.SecretKey;
import javax.crypto.spec.*;

import sun.security.util.KeyUtil;

/**
 * This class implements the Diffie-Hellman key agreement protocol between
 * any number of parties.
 *
 * @author Jan Luehe
 *
 */

public final class DHKeyAgreement
extends KeyAgreementSpi {

    private boolean generateSecret = false;
    private BigInteger init_p = null;
    private BigInteger init_g = null;
    private BigInteger x = BigInteger.ZERO; // the private value
    private BigInteger y = BigInteger.ZERO;

    private static class AllowKDF {

        private static final boolean VALUE = getValue();

        @SuppressWarnings("removal")
        private static boolean getValue() {
            return AccessController.doPrivileged(
                (PrivilegedAction<Boolean>)
                () -> Boolean.getBoolean("jdk.crypto.KeyAgreement.legacyKDF"));
        }
    }

    /**
     * Empty constructor
     */
    public DHKeyAgreement() {
    }

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
    protected void engineInit(Key key, SecureRandom random)
        throws InvalidKeyException
    {
        try {
            engineInit(key, null, random);
        } catch (InvalidAlgorithmParameterException e) {
            // never happens, because we did not pass any parameters
        }
    }

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
    protected void engineInit(Key key, AlgorithmParameterSpec params,
                              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        // ignore "random" parameter, because our implementation does not
        // require any source of randomness
        generateSecret = false;
        init_p = null;
        init_g = null;

        if ((params != null) && !(params instanceof DHParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("Diffie-Hellman parameters expected");
        }
        if (!(key instanceof javax.crypto.interfaces.DHPrivateKey)) {
            throw new InvalidKeyException("Diffie-Hellman private key "
                                          + "expected");
        }
        javax.crypto.interfaces.DHPrivateKey dhPrivKey;
        dhPrivKey = (javax.crypto.interfaces.DHPrivateKey)key;

        // check if private key parameters are compatible with
        // initialized ones
        if (params != null) {
            init_p = ((DHParameterSpec)params).getP();
            init_g = ((DHParameterSpec)params).getG();
        }
        BigInteger priv_p = dhPrivKey.getParams().getP();
        BigInteger priv_g = dhPrivKey.getParams().getG();
        if (init_p != null && priv_p != null && !(init_p.equals(priv_p))) {
            throw new InvalidKeyException("Incompatible parameters");
        }
        if (init_g != null && priv_g != null && !(init_g.equals(priv_g))) {
            throw new InvalidKeyException("Incompatible parameters");
        }
        if ((init_p == null && priv_p == null)
            || (init_g == null && priv_g == null)) {
            throw new InvalidKeyException("Missing parameters");
        }
        init_p = priv_p;
        init_g = priv_g;

        // store the x value
        this.x = dhPrivKey.getX();
    }

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
    protected Key engineDoPhase(Key key, boolean lastPhase)
        throws InvalidKeyException, IllegalStateException
    {
        if (!(key instanceof javax.crypto.interfaces.DHPublicKey)) {
            throw new InvalidKeyException("Diffie-Hellman public key "
                                          + "expected");
        }
        javax.crypto.interfaces.DHPublicKey dhPubKey;
        dhPubKey = (javax.crypto.interfaces.DHPublicKey)key;

        if (init_p == null || init_g == null) {
            throw new IllegalStateException("Not initialized");
        }

        // check if public key parameters are compatible with
        // initialized ones
        BigInteger pub_p = dhPubKey.getParams().getP();
        BigInteger pub_g = dhPubKey.getParams().getG();
        if (pub_p != null && !(init_p.equals(pub_p))) {
            throw new InvalidKeyException("Incompatible parameters");
        }
        if (pub_g != null && !(init_g.equals(pub_g))) {
            throw new InvalidKeyException("Incompatible parameters");
        }

        // validate the Diffie-Hellman public key
        KeyUtil.validate(dhPubKey);

        // store the y value
        this.y = dhPubKey.getY();

        // we've received a public key (from one of the other parties),
        // so we are ready to create the secret, which may be an
        // intermediate secret, in which case we wrap it into a
        // Diffie-Hellman public key object and return it.
        generateSecret = true;
        if (lastPhase == false) {
            byte[] intermediate = engineGenerateSecret();
            return new DHPublicKey(new BigInteger(1, intermediate),
                                   init_p, init_g);
        } else {
            return null;
        }
    }

    /**
     * Generates the shared secret and returns it in a new buffer.
     *
     * <p>This method resets this <code>KeyAgreementSpi</code> object,
     * so that it
     * can be reused for further key agreements. Unless this key agreement is
     * reinitialized with one of the <code>engineInit</code> methods, the same
     * private information and algorithm parameters will be used for
     * subsequent key agreements.
     *
     * @return the new buffer with the shared secret
     *
     * @exception IllegalStateException if this key agreement has not been
     * completed yet
     */
    protected byte[] engineGenerateSecret()
        throws IllegalStateException
    {
        int expectedLen = (init_p.bitLength() + 7) >>> 3;
        byte[] result = new byte[expectedLen];
        try {
            engineGenerateSecret(result, 0);
        } catch (ShortBufferException sbe) {
            // should never happen since length are identical
        }
        return result;
    }

    /**
     * Generates the shared secret, and places it into the buffer
     * <code>sharedSecret</code>, beginning at <code>offset</code>.
     *
     * <p>If the <code>sharedSecret</code> buffer is too small to hold the
     * result, a <code>ShortBufferException</code> is thrown.
     * In this case, this call should be repeated with a larger output buffer.
     *
     * <p>This method resets this <code>KeyAgreementSpi</code> object,
     * so that it
     * can be reused for further key agreements. Unless this key agreement is
     * reinitialized with one of the <code>engineInit</code> methods, the same
     * private information and algorithm parameters will be used for
     * subsequent key agreements.
     *
     * @param sharedSecret the buffer for the shared secret
     * @param offset the offset in <code>sharedSecret</code> where the
     * shared secret will be stored
     *
     * @return the number of bytes placed into <code>sharedSecret</code>
     *
     * @exception IllegalStateException if this key agreement has not been
     * completed yet
     * @exception ShortBufferException if the given output buffer is too small
     * to hold the secret
     */
    protected int engineGenerateSecret(byte[] sharedSecret, int offset)
        throws IllegalStateException, ShortBufferException
    {
        if (generateSecret == false) {
            throw new IllegalStateException
                ("Key agreement has not been completed yet");
        }

        if (sharedSecret == null) {
            throw new ShortBufferException
                ("No buffer provided for shared secret");
        }

        BigInteger modulus = init_p;
        int expectedLen = (modulus.bitLength() + 7) >>> 3;
        if ((sharedSecret.length - offset) < expectedLen) {
            throw new ShortBufferException
                    ("Buffer too short for shared secret");
        }

        // Reset the key agreement after checking for ShortBufferException
        // above, so user can recover w/o losing internal state
        generateSecret = false;

        // No further process if z <= 1 or z == (p - 1) (See section 5.7.1,
        // NIST SP 800-56A Rev 3).
        BigInteger z = this.y.modPow(this.x, modulus);
        if ((z.compareTo(BigInteger.ONE) <= 0) ||
                z.equals(modulus.subtract(BigInteger.ONE))) {
            throw new ProviderException(
                    "Generated secret is out-of-range of (1, p -1)");
        }

        /*
         * NOTE: BigInteger.toByteArray() returns a byte array containing
         * the two's-complement representation of this BigInteger with
         * the most significant byte is in the zeroth element. This
         * contains the minimum number of bytes required to represent
         * this BigInteger, including at least one sign bit whose value
         * is always 0.
         *
         * Keys are always positive, and the above sign bit isn't
         * actually used when representing keys.  (i.e. key = new
         * BigInteger(1, byteArray))  To obtain an array containing
         * exactly expectedLen bytes of magnitude, we strip any extra
         * leading 0's, or pad with 0's in case of a "short" secret.
         */
        byte[] secret = z.toByteArray();
        if (secret.length == expectedLen) {
            System.arraycopy(secret, 0, sharedSecret, offset,
                             secret.length);
        } else {
            // Array too short, pad it w/ leading 0s
            if (secret.length < expectedLen) {
                System.arraycopy(secret, 0, sharedSecret,
                    offset + (expectedLen - secret.length),
                    secret.length);
            } else {
                // Array too long, check and trim off the excess
                if ((secret.length == (expectedLen+1)) && secret[0] == 0) {
                    // ignore the leading sign byte
                    System.arraycopy(secret, 1, sharedSecret, offset, expectedLen);
                } else {
                    throw new ProviderException("Generated secret is out-of-range");
                }
            }
        }
        return expectedLen;
    }

    /**
     * Creates the shared secret and returns it as a secret key object
     * of the requested algorithm type.
     *
     * <p>This method resets this <code>KeyAgreementSpi</code> object,
     * so that it
     * can be reused for further key agreements. Unless this key agreement is
     * reinitialized with one of the <code>engineInit</code> methods, the same
     * private information and algorithm parameters will be used for
     * subsequent key agreements.
     *
     * @param algorithm the requested secret key algorithm
     *
     * @return the shared secret key
     *
     * @exception IllegalStateException if this key agreement has not been
     * completed yet
     * @exception NoSuchAlgorithmException if the requested secret key
     * algorithm is not available
     * @exception InvalidKeyException if the shared secret key material cannot
     * be used to generate a secret key of the requested algorithm type (e.g.,
     * the key material is too short)
     */
    protected SecretKey engineGenerateSecret(String algorithm)
        throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException
    {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("null algorithm");
        }

        if (!algorithm.equalsIgnoreCase("TlsPremasterSecret") &&
            !AllowKDF.VALUE) {

            throw new NoSuchAlgorithmException("Unsupported secret key "
                                               + "algorithm: " + algorithm);
        }

        byte[] secret = engineGenerateSecret();
        if (algorithm.equalsIgnoreCase("DES")) {
            // DES
            return new DESKey(secret);
        } else if (algorithm.equalsIgnoreCase("DESede")
                   || algorithm.equalsIgnoreCase("TripleDES")) {
            // Triple DES
            return new DESedeKey(secret);
        } else if (algorithm.equalsIgnoreCase("Blowfish")) {
            // Blowfish
            int keysize = secret.length;
            if (keysize >= BlowfishConstants.BLOWFISH_MAX_KEYSIZE)
                keysize = BlowfishConstants.BLOWFISH_MAX_KEYSIZE;
            SecretKeySpec skey = new SecretKeySpec(secret, 0, keysize,
                                                   "Blowfish");
            return skey;
        } else if (algorithm.equalsIgnoreCase("AES")) {
            // AES
            int keysize = secret.length;
            SecretKeySpec skey = null;
            int idx = AESConstants.AES_KEYSIZES.length - 1;
            while (skey == null && idx >= 0) {
                // Generate the strongest key using the shared secret
                // assuming the key sizes in AESConstants class are
                // in ascending order
                if (keysize >= AESConstants.AES_KEYSIZES[idx]) {
                    keysize = AESConstants.AES_KEYSIZES[idx];
                    skey = new SecretKeySpec(secret, 0, keysize, "AES");
                }
                idx--;
            }
            if (skey == null) {
                throw new InvalidKeyException("Key material is too short");
            }
            return skey;
        } else if (algorithm.equals("TlsPremasterSecret")) {
            // remove leading zero bytes per RFC 5246 Section 8.1.2
            return new SecretKeySpec(
                        KeyUtil.trimZeroes(secret), "TlsPremasterSecret");
        } else {
            throw new NoSuchAlgorithmException("Unsupported secret key "
                                               + "algorithm: "+ algorithm);
        }
    }
}
