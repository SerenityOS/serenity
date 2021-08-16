/*
 * Copyright (c) 2005, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

import javax.crypto.MacSpi;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

import static com.sun.crypto.provider.TlsPrfGenerator.genPad;

/**
 * This file contains the code for the SslMacMD5 and SslMacSHA1 implementations.
 * The SSL 3.0 MAC is a variation of the HMAC algorithm.
 *
 * Note that we don't implement Cloneable as that is not needed for SSL.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
final class SslMacCore {

    private final MessageDigest md;
    private final byte[] pad1, pad2;

    private boolean first;       // Is this the first data to be processed?
    private byte[] secret;

    /**
     * Standard constructor, creates a new SslMacCore instance instantiating
     * a MessageDigest of the specified name.
     */
    SslMacCore(String digestAlgorithm, byte[] pad1, byte[] pad2)
            throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance(digestAlgorithm);
        this.pad1 = pad1;
        this.pad2 = pad2;
        first = true;
    }

    /**
     * Returns the length of the Mac in bytes.
     *
     * @return the Mac length in bytes.
     */
    int getDigestLength() {
        return md.getDigestLength();
    }

    /**
     * Initializes the Mac with the given secret key and algorithm parameters.
     *
     * @param key the secret key.
     * @param params the algorithm parameters.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this MAC.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this MAC.
     */
    void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {

        if (params != null) {
            throw new InvalidAlgorithmParameterException
                ("SslMac does not use parameters");
        }

        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Secret key expected");
        }

        secret = key.getEncoded();
        if (secret == null || secret.length == 0) {
            throw new InvalidKeyException("Missing key data");
        }

        reset();
    }

    /**
     * Processes the given byte.
     *
     * @param input the input byte to be processed.
     */
    void update(byte input) {
        if (first == true) {
            // compute digest for 1st pass; start with inner pad
            md.update(secret);
            md.update(pad1);
            first = false;
        }

        // add the passed byte to the inner digest
        md.update(input);
    }

    /**
     * Processes the first <code>len</code> bytes in <code>input</code>,
     * starting at <code>offset</code>.
     *
     * @param input the input buffer.
     * @param offset the offset in <code>input</code> where the input starts.
     * @param len the number of bytes to process.
     */
    void update(byte input[], int offset, int len) {
        if (first == true) {
            // compute digest for 1st pass; start with inner pad
            md.update(secret);
            md.update(pad1);
            first = false;
        }

        // add the selected part of an array of bytes to the inner digest
        md.update(input, offset, len);
    }

    void update(ByteBuffer input) {
        if (first == true) {
            // compute digest for 1st pass; start with inner pad
            md.update(secret);
            md.update(pad1);
            first = false;
        }

        md.update(input);
    }

    /**
     * Completes the Mac computation and resets the Mac for further use,
     * maintaining the secret key that the Mac was initialized with.
     *
     * @return the Mac result.
     */
    byte[] doFinal() {
        if (first == true) {
            // compute digest for 1st pass; start with inner pad
            md.update(secret);
            md.update(pad1);
        } else {
            first = true;
        }

        try {
            // finish the inner digest
            byte[] tmp = md.digest();

            // compute digest for 2nd pass; start with outer pad
            md.update(secret);
            md.update(pad2);
            // add result of 1st hash
            md.update(tmp);

            md.digest(tmp, 0, tmp.length);
            return tmp;
        } catch (DigestException e) {
            // should never occur
            throw new ProviderException(e);
        }
    }

    /**
     * Resets the Mac for further use, maintaining the secret key that the
     * Mac was initialized with.
     */
    void reset() {
        if (first == false) {
            md.reset();
            first = true;
        }
    }

    // nested static class for the SslMacMD5 implementation
    public static final class SslMacMD5 extends MacSpi {
        private final SslMacCore core;
        public SslMacMD5() throws NoSuchAlgorithmException {
            core = new SslMacCore("MD5", md5Pad1, md5Pad2);
        }
        protected int engineGetMacLength() {
            return core.getDigestLength();
        }
        protected void engineInit(Key key, AlgorithmParameterSpec params)
                throws InvalidKeyException, InvalidAlgorithmParameterException {
            core.init(key, params);
        }
        protected void engineUpdate(byte input) {
            core.update(input);
        }
        protected void engineUpdate(byte input[], int offset, int len) {
            core.update(input, offset, len);
        }
        protected void engineUpdate(ByteBuffer input) {
            core.update(input);
        }
        protected byte[] engineDoFinal() {
            return core.doFinal();
        }
        protected void engineReset() {
            core.reset();
        }

        static final byte[] md5Pad1 = genPad((byte)0x36, 48);
        static final byte[] md5Pad2 = genPad((byte)0x5c, 48);
    }

    // nested static class for the SslMacMD5 implementation
    public static final class SslMacSHA1 extends MacSpi {
        private final SslMacCore core;
        public SslMacSHA1() throws NoSuchAlgorithmException {
            core = new SslMacCore("SHA", shaPad1, shaPad2);
        }
        protected int engineGetMacLength() {
            return core.getDigestLength();
        }
        protected void engineInit(Key key, AlgorithmParameterSpec params)
                throws InvalidKeyException, InvalidAlgorithmParameterException {
            core.init(key, params);
        }
        protected void engineUpdate(byte input) {
            core.update(input);
        }
        protected void engineUpdate(byte input[], int offset, int len) {
            core.update(input, offset, len);
        }
        protected void engineUpdate(ByteBuffer input) {
            core.update(input);
        }
        protected byte[] engineDoFinal() {
            return core.doFinal();
        }
        protected void engineReset() {
            core.reset();
        }

        static final byte[] shaPad1 = genPad((byte)0x36, 40);
        static final byte[] shaPad2 = genPad((byte)0x5c, 40);
    }

}
