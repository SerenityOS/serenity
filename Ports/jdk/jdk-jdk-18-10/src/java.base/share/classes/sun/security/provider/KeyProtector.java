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

package sun.security.provider;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.*;

import sun.security.pkcs.PKCS8Key;
import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.x509.AlgorithmId;
import sun.security.util.ObjectIdentifier;
import sun.security.util.KnownOIDs;
import sun.security.util.DerValue;

/**
 * This is an implementation of a Sun proprietary, exportable algorithm
 * intended for use when protecting (or recovering the cleartext version of)
 * sensitive keys.
 * This algorithm is not intended as a general purpose cipher.
 *
 * This is how the algorithm works for key protection:
 *
 * p - user password
 * s - random salt
 * X - xor key
 * P - to-be-protected key
 * Y - protected key
 * R - what gets stored in the keystore
 *
 * Step 1:
 * Take the user's password, append a random salt (of fixed size) to it,
 * and hash it: d1 = digest(p, s)
 * Store d1 in X.
 *
 * Step 2:
 * Take the user's password, append the digest result from the previous step,
 * and hash it: dn = digest(p, dn-1).
 * Store dn in X (append it to the previously stored digests).
 * Repeat this step until the length of X matches the length of the private key
 * P.
 *
 * Step 3:
 * XOR X and P, and store the result in Y: Y = X XOR P.
 *
 * Step 4:
 * Store s, Y, and digest(p, P) in the result buffer R:
 * R = s + Y + digest(p, P), where "+" denotes concatenation.
 * (NOTE: digest(p, P) is stored in the result buffer, so that when the key is
 * recovered, we can check if the recovered key indeed matches the original
 * key.) R is stored in the keystore.
 *
 * The protected key is recovered as follows:
 *
 * Step1 and Step2 are the same as above, except that the salt is not randomly
 * generated, but taken from the result R of step 4 (the first length(s)
 * bytes).
 *
 * Step 3 (XOR operation) yields the plaintext key.
 *
 * Then concatenate the password with the recovered key, and compare with the
 * last length(digest(p, P)) bytes of R. If they match, the recovered key is
 * indeed the same key as the original key.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.KeyStore
 * @see JavaKeyStore
 * @see KeyTool
 *
 * @since 1.2
 */

final class KeyProtector {

    private static final int SALT_LEN = 20; // the salt length
    private static final String DIGEST_ALG = "SHA";
    private static final int DIGEST_LEN = 20;

    // The password used for protecting/recovering keys passed through this
    // key protector. We store it as a byte array, so that we can digest it.
    private byte[] passwdBytes;

    private MessageDigest md;


    /**
     * Creates an instance of this class, and initializes it with the given
     * password.
     */
    public KeyProtector(byte[] passwordBytes)
        throws NoSuchAlgorithmException
    {
        if (passwordBytes == null) {
           throw new IllegalArgumentException("password can't be null");
        }
        md = MessageDigest.getInstance(DIGEST_ALG);
        this.passwdBytes = passwordBytes;
    }

    /*
     * Protects the given plaintext key, using the password provided at
     * construction time.
     */
    public byte[] protect(Key key) throws KeyStoreException
    {
        int i;
        int numRounds;
        byte[] digest;
        int xorOffset; // offset in xorKey where next digest will be stored
        int encrKeyOffset = 0;

        if (key == null) {
            throw new IllegalArgumentException("plaintext key can't be null");
        }

        if (!"PKCS#8".equalsIgnoreCase(key.getFormat())) {
            throw new KeyStoreException(
                "Cannot get key bytes, not PKCS#8 encoded");
        }

        byte[] plainKey = key.getEncoded();
        if (plainKey == null) {
            throw new KeyStoreException(
                "Cannot get key bytes, encoding not supported");
        }

        // Determine the number of digest rounds
        numRounds = plainKey.length / DIGEST_LEN;
        if ((plainKey.length % DIGEST_LEN) != 0)
            numRounds++;

        // Create a random salt
        byte[] salt = new byte[SALT_LEN];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        // Set up the byte array which will be XORed with "plainKey"
        byte[] xorKey = new byte[plainKey.length];

        // Compute the digests, and store them in "xorKey"
        for (i = 0, xorOffset = 0, digest = salt;
             i < numRounds;
             i++, xorOffset += DIGEST_LEN) {
            md.update(passwdBytes);
            md.update(digest);
            digest = md.digest();
            md.reset();
            // Copy the digest into "xorKey"
            if (i < numRounds - 1) {
                System.arraycopy(digest, 0, xorKey, xorOffset,
                                 digest.length);
            } else {
                System.arraycopy(digest, 0, xorKey, xorOffset,
                                 xorKey.length - xorOffset);
            }
        }

        // XOR "plainKey" with "xorKey", and store the result in "tmpKey"
        byte[] tmpKey = new byte[plainKey.length];
        for (i = 0; i < tmpKey.length; i++) {
            tmpKey[i] = (byte)(plainKey[i] ^ xorKey[i]);
        }

        // Store salt and "tmpKey" in "encrKey"
        byte[] encrKey = new byte[salt.length + tmpKey.length + DIGEST_LEN];
        System.arraycopy(salt, 0, encrKey, encrKeyOffset, salt.length);
        encrKeyOffset += salt.length;
        System.arraycopy(tmpKey, 0, encrKey, encrKeyOffset, tmpKey.length);
        encrKeyOffset += tmpKey.length;

        // Append digest(password, plainKey) as an integrity check to "encrKey"
        md.update(passwdBytes);
        Arrays.fill(passwdBytes, (byte)0x00);
        passwdBytes = null;
        md.update(plainKey);
        digest = md.digest();
        md.reset();
        System.arraycopy(digest, 0, encrKey, encrKeyOffset, digest.length);
        Arrays.fill(plainKey, (byte)0);

        // wrap the protected private key in a PKCS#8-style
        // EncryptedPrivateKeyInfo, and returns its encoding
        AlgorithmId encrAlg;
        try {
            encrAlg = new AlgorithmId(ObjectIdentifier.of
                    (KnownOIDs.JAVASOFT_JDKKeyProtector));
            return new EncryptedPrivateKeyInfo(encrAlg,encrKey).getEncoded();
        } catch (IOException ioe) {
            throw new KeyStoreException(ioe.getMessage());
        }
    }

    /*
     * Recovers the plaintext version of the given key (in protected format),
     * using the password provided at construction time.
     */
    public Key recover(EncryptedPrivateKeyInfo encrInfo)
        throws UnrecoverableKeyException
    {
        int i;
        byte[] digest;
        int numRounds;
        int xorOffset; // offset in xorKey where next digest will be stored
        int encrKeyLen; // the length of the encrpyted key

        // do we support the algorithm?
        AlgorithmId encrAlg = encrInfo.getAlgorithm();
        if (!(encrAlg.getOID().toString().equals
                (KnownOIDs.JAVASOFT_JDKKeyProtector.value()))) {
            throw new UnrecoverableKeyException("Unsupported key protection "
                                                + "algorithm");
        }

        byte[] protectedKey = encrInfo.getEncryptedData();

        /*
         * Get the salt associated with this key (the first SALT_LEN bytes of
         * <code>protectedKey</code>)
         */
        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(protectedKey, 0, salt, 0, SALT_LEN);

        // Determine the number of digest rounds
        encrKeyLen = protectedKey.length - SALT_LEN - DIGEST_LEN;
        numRounds = encrKeyLen / DIGEST_LEN;
        if ((encrKeyLen % DIGEST_LEN) != 0) numRounds++;

        // Get the encrypted key portion and store it in "encrKey"
        byte[] encrKey = new byte[encrKeyLen];
        System.arraycopy(protectedKey, SALT_LEN, encrKey, 0, encrKeyLen);

        // Set up the byte array which will be XORed with "encrKey"
        byte[] xorKey = new byte[encrKey.length];

        // Compute the digests, and store them in "xorKey"
        for (i = 0, xorOffset = 0, digest = salt;
             i < numRounds;
             i++, xorOffset += DIGEST_LEN) {
            md.update(passwdBytes);
            md.update(digest);
            digest = md.digest();
            md.reset();
            // Copy the digest into "xorKey"
            if (i < numRounds - 1) {
                System.arraycopy(digest, 0, xorKey, xorOffset,
                                 digest.length);
            } else {
                System.arraycopy(digest, 0, xorKey, xorOffset,
                                 xorKey.length - xorOffset);
            }
        }

        // XOR "encrKey" with "xorKey", and store the result in "plainKey"
        byte[] plainKey = new byte[encrKey.length];
        for (i = 0; i < plainKey.length; i++) {
            plainKey[i] = (byte)(encrKey[i] ^ xorKey[i]);
        }

        /*
         * Check the integrity of the recovered key by concatenating it with
         * the password, digesting the concatenation, and comparing the
         * result of the digest operation with the digest provided at the end
         * of <code>protectedKey</code>. If the two digest values are
         * different, throw an exception.
         */
        md.update(passwdBytes);
        Arrays.fill(passwdBytes, (byte)0x00);
        passwdBytes = null;
        md.update(plainKey);
        digest = md.digest();
        md.reset();
        for (i = 0; i < digest.length; i++) {
            if (digest[i] != protectedKey[SALT_LEN + encrKeyLen + i]) {
                throw new UnrecoverableKeyException("Cannot recover key");
            }
        }

        // The parseKey() method of PKCS8Key parses the key
        // algorithm and instantiates the appropriate key factory,
        // which in turn parses the key material.
        try {
            return PKCS8Key.parseKey(plainKey);
        } catch (IOException ioe) {
            throw new UnrecoverableKeyException(ioe.getMessage());
        } finally {
            Arrays.fill(plainKey, (byte)0);
        }
    }
}
