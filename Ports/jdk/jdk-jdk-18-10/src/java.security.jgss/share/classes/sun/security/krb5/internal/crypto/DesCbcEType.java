/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.crypto;

import sun.security.krb5.Confounder;
import sun.security.krb5.KrbCryptoException;
import sun.security.krb5.internal.*;

abstract class DesCbcEType extends EType {
    protected abstract byte[] calculateChecksum(byte[] data, int size)
        throws KrbCryptoException;

    public int blockSize() {
        return 8;
    }

    public int keyType() {
        return Krb5.KEYTYPE_DES;
    }

    public int keySize() {
        return 8;
    }

    /**
     * Encrypts the data using DES in CBC mode.
     * @param data the buffer for plain text.
     * @param key the key to encrypt the data.
     * @return the buffer for encrypted data.
     *
     * @written by Yanni Zhang, Dec 6 99.
     */

    public byte[] encrypt(byte[] data, byte[] key, int usage)
         throws KrbCryptoException {
        byte[] ivec = new byte[keySize()];
        return encrypt(data, key, ivec, usage);
    }

    /**
     * Encrypts the data using DES in CBC mode.
     * @param data the buffer for plain text.
     * @param key the key to encrypt the data.
     * @param ivec initialization vector.
     * @return buffer for encrypted data.
     *
     * @modified by Yanni Zhang, Feb 24 00.
     */
    public byte[] encrypt(byte[] data, byte[] key, byte[] ivec,
        int usage) throws KrbCryptoException {

        /*
         * To meet export control requirements, double check that the
         * key being used is no longer than 64 bits.
         *
         * Note that from a protocol point of view, an
         * algorithm that is not DES will be rejected before this
         * point. Also, a  DES key that is not 64 bits will be
         * rejected by a good implementations of JCE.
         */
        if (key.length > 8)
        throw new KrbCryptoException("Invalid DES Key!");

        int new_size = data.length + confounderSize() + checksumSize();
        byte[] new_data;
        byte pad;
        /*Data padding: using Kerberos 5 GSS-API mechanism (1.2.2.3), Jun 1996.
         *Before encryption, plain text data is padded to the next highest multiple of blocksize.
         *by appending between 1 and 8 bytes, the value of each such byte being the total number
         *of pad bytes. For example, if new_size = 10, blockSize is 8, we should pad 2 bytes,
         *and the value of each byte is 2.
         *If plaintext data is a multiple of blocksize, we pad a 8 bytes of 8.
         */
        if (new_size % blockSize() == 0) {
            new_data = new byte[new_size + blockSize()];
            pad = (byte)8;
        }
        else {
            new_data = new byte[new_size + blockSize() - new_size % blockSize()];
            pad = (byte)(blockSize() - new_size % blockSize());
        }
        for (int i = new_size; i < new_data.length; i++) {
            new_data[i] = pad;
        }
        byte[] conf = Confounder.bytes(confounderSize());
        System.arraycopy(conf, 0, new_data, 0, confounderSize());
        System.arraycopy(data, 0, new_data, startOfData(), data.length);
        byte[] cksum = calculateChecksum(new_data, new_data.length);
        System.arraycopy(cksum, 0, new_data, startOfChecksum(),
                         checksumSize());
        byte[] cipher = new byte[new_data.length];
        Des.cbc_encrypt(new_data, cipher, key, ivec, true);
        return cipher;
    }

    /**
     * Decrypts the data using DES in CBC mode.
     * @param cipher the input buffer.
     * @param key the key to decrypt the data.
     *
     * @written by Yanni Zhang, Dec 6 99.
     */
    public byte[] decrypt(byte[] cipher, byte[] key, int usage)
        throws KrbApErrException, KrbCryptoException{
        byte[] ivec = new byte[keySize()];
        return decrypt(cipher, key, ivec, usage);
    }

    /**
     * Decrypts the data using DES in CBC mode.
     * @param cipher the input buffer.
     * @param key the key to decrypt the data.
     * @param ivec initialization vector.
     *
     * @modified by Yanni Zhang, Dec 6 99.
     */
    public byte[] decrypt(byte[] cipher, byte[] key, byte[] ivec, int usage)
        throws KrbApErrException, KrbCryptoException {

        /*
         * To meet export control requirements, double check that the
         * key being used is no longer than 64 bits.
         *
         * Note that from a protocol point of view, an
         * algorithm that is not DES will be rejected before this
         * point. Also, a DES key that is not 64 bits will be
         * rejected by a good JCE provider.
         */
        if (key.length > 8)
            throw new KrbCryptoException("Invalid DES Key!");

        byte[] data = new byte[cipher.length];
        Des.cbc_encrypt(cipher, data, key, ivec, false);
        if (!isChecksumValid(data))
            throw new KrbApErrException(Krb5.KRB_AP_ERR_BAD_INTEGRITY);
        return data;
    }

    private void copyChecksumField(byte[] data, byte[] cksum) {
        for (int i = 0; i < checksumSize();  i++)
            data[startOfChecksum() + i] = cksum[i];
    }

    private byte[] checksumField(byte[] data) {
        byte[] result = new byte[checksumSize()];
        for (int i = 0; i < checksumSize(); i++)
        result[i] = data[startOfChecksum() + i];
        return result;
    }

    private void resetChecksumField(byte[] data) {
        for (int i = startOfChecksum(); i < startOfChecksum() +
                 checksumSize();  i++)
            data[i] = 0;
    }

    /*
        // Not used.
    public void setChecksum(byte[] data, int size) throws KrbCryptoException{
        resetChecksumField(data);
        byte[] cksum = calculateChecksum(data, size);
        copyChecksumField(data, cksum);
    }
*/

    private byte[] generateChecksum(byte[] data) throws KrbCryptoException{
        byte[] cksum1 = checksumField(data);
        resetChecksumField(data);
        byte[] cksum2 = calculateChecksum(data, data.length);
        copyChecksumField(data, cksum1);
        return cksum2;
    }

    private boolean isChecksumEqual(byte[] cksum1, byte[] cksum2) {
        if (cksum1 == cksum2)
            return true;
        if ((cksum1 == null && cksum2 != null) ||
            (cksum1 != null && cksum2 == null))
            return false;
        if (cksum1.length != cksum2.length)
            return false;
        for (int i = 0; i < cksum1.length; i++)
            if (cksum1[i] != cksum2[i])
                return false;
        return true;
    }

    protected boolean isChecksumValid(byte[] data) throws KrbCryptoException {
        byte[] cksum1 = checksumField(data);
        byte[] cksum2 = generateChecksum(data);
        return isChecksumEqual(cksum1, cksum2);
    }
}
