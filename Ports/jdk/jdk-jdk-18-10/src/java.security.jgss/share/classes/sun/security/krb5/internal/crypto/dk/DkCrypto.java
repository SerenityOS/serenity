/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
 */

/*
 * Copyright (C) 1998 by the FundsXpress, INC.
 *
 * All rights reserved.
 *
 * Export of this software from the United States of America may require
 * a specific license from the United States Government.  It is the
 * responsibility of any person or organization contemplating export to
 * obtain such a license before exporting.
 *
 * WITHIN THAT CONSTRAINT, permission to use, copy, modify, and
 * distribute this software and its documentation for any purpose and
 * without fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright notice and
 * this permission notice appear in supporting documentation, and that
 * the name of FundsXpress. not be used in advertising or publicity pertaining
 * to distribution of the software without specific, written prior
 * permission.  FundsXpress makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is" without express
 * or implied warranty.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package sun.security.krb5.internal.crypto.dk;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import sun.security.util.HexDumpEncoder;
import sun.security.krb5.Confounder;
import sun.security.krb5.internal.crypto.KeyUsage;
import sun.security.krb5.KrbCryptoException;

import static java.nio.charset.StandardCharsets.*;

/**
 * Implements Derive Key cryptography functionality as defined in RFC 3961.
 * http://www.ietf.org/rfc/rfc3961.txt
 *
 * This is an abstract class. Concrete subclasses need to implement
 * the abstract methods.
 */

public abstract class DkCrypto {

    protected static final boolean debug = false;

    // These values correspond to the ASCII encoding for the string "kerberos"
    static final byte[] KERBEROS_CONSTANT =
        {0x6b, 0x65, 0x72, 0x62, 0x65, 0x72, 0x6f, 0x73};

    protected abstract int getKeySeedLength();  // in bits

    protected abstract byte[] randomToKey(byte[] in);

    protected abstract Cipher getCipher(byte[] key, byte[] ivec, int mode)
        throws GeneralSecurityException;

    public abstract int getChecksumLength();  // in bytes

    protected abstract byte[] getHmac(byte[] key, byte[] plaintext)
        throws GeneralSecurityException;

    /**
     * From RFC 3961.
     *
     * encryption function       conf = random string of length c
     *                     pad = shortest string to bring confounder
     *                           and plaintext to a length that's a
     *                           multiple of m
     *                     (C1, newIV) = E(Ke, conf | plaintext | pad,
     *                                     oldstate.ivec)
     *                    H1 = HMAC(Ki, conf | plaintext | pad)
     *                     ciphertext =  C1 | H1[1..h]
     *                     newstate.ivec = newIV
     *
     * @param ivec initial vector to use when initializing the cipher; if null,
     *     then blocksize number of zeros are used,
     * @param new_ivec if non-null, it is updated upon return to be the
     *       new ivec to use when calling encrypt next time
     */
    public byte[] encrypt(byte[] baseKey, int usage,
        byte[] ivec, byte[] new_ivec, byte[] plaintext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        byte[] Ke = null;
        byte[] Ki = null;

        try {
            // Derive encryption key

            byte[] constant = new byte[5];
            constant[0] = (byte) ((usage>>24)&0xff);
            constant[1] = (byte) ((usage>>16)&0xff);
            constant[2] = (byte) ((usage>>8)&0xff);
            constant[3] = (byte) (usage&0xff);

            constant[4] = (byte) 0xaa;

            Ke = dk(baseKey, constant);
            if (debug) {
                System.err.println("usage: " + usage);
                if (ivec != null) {
                    traceOutput("old_state.ivec", ivec, 0, ivec.length);
                }
                traceOutput("plaintext", plaintext, start, Math.min(len, 32));
                traceOutput("constant", constant, 0, constant.length);
                traceOutput("baseKey", baseKey, 0, baseKey.length);
                traceOutput("Ke", Ke, 0, Ke.length);
            }

            // Encrypt
            // C1 = E(Ke, conf | plaintext | pad, oldivec)
            Cipher encCipher = getCipher(Ke, ivec, Cipher.ENCRYPT_MODE);
            int blockSize = encCipher.getBlockSize();
            byte[] confounder = Confounder.bytes(blockSize);

            int plainSize = roundup(confounder.length + len, blockSize);
            if (debug) {
                System.err.println("confounder = " + confounder.length +
                    "; plaintext = " + len + "; padding = " +
                    (plainSize - confounder.length - len) + "; total = " +
                        plainSize);
                traceOutput("confounder", confounder, 0, confounder.length);
            }

            byte[] toBeEncrypted = new byte[plainSize];
            System.arraycopy(confounder, 0, toBeEncrypted,
                                0, confounder.length);
            System.arraycopy(plaintext, start, toBeEncrypted,
                                confounder.length, len);

            // Set padding bytes to zero
            Arrays.fill(toBeEncrypted, confounder.length + len, plainSize,
                        (byte)0);

            int cipherSize = encCipher.getOutputSize(plainSize);
            int ccSize =  cipherSize + getChecksumLength();  // cipher | hmac

            byte[] ciphertext = new byte[ccSize];

            encCipher.doFinal(toBeEncrypted, 0, plainSize, ciphertext, 0);

            // Update ivec for next operation
            // (last blockSize bytes of ciphertext)
            // newstate.ivec = newIV
            if (new_ivec != null && new_ivec.length == blockSize) {
                System.arraycopy(ciphertext,  cipherSize - blockSize,
                    new_ivec, 0, blockSize);
                if (debug) {
                    traceOutput("new_ivec", new_ivec, 0, new_ivec.length);
                }
            }

            // Derive integrity key
            constant[4] = (byte) 0x55;
            Ki = dk(baseKey, constant);
            if (debug) {
                traceOutput("constant", constant, 0, constant.length);
                traceOutput("Ki", Ki, 0, Ke.length);
            }

            // Generate checksum
            // H1 = HMAC(Ki, conf | plaintext | pad)
            byte[] hmac = getHmac(Ki, toBeEncrypted);

            if (debug) {
                traceOutput("hmac", hmac, 0, hmac.length);
                traceOutput("ciphertext", ciphertext, 0,
                                Math.min(ciphertext.length, 32));
            }

            // C1 | H1[1..h]
            System.arraycopy(hmac, 0, ciphertext, cipherSize,
                                getChecksumLength());
            return ciphertext;
        } finally {
            if (Ke != null) {
                Arrays.fill(Ke, 0, Ke.length, (byte) 0);
            }
            if (Ki != null) {
                Arrays.fill(Ki, 0, Ki.length, (byte) 0);
            }
        }
    }

    /**
     * Performs encryption using given key only; does not add
     * confounder, padding, or checksum. Incoming data to be encrypted
     * assumed to have the correct blocksize.
     * Ignore key usage.
     */
    public byte[] encryptRaw(byte[] baseKey, int usage,
        byte[] ivec, byte[] plaintext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (debug) {
            System.err.println("usage: " + usage);
            if (ivec != null) {
                traceOutput("old_state.ivec", ivec, 0, ivec.length);
            }
            traceOutput("plaintext", plaintext, start, Math.min(len, 32));
            traceOutput("baseKey", baseKey, 0, baseKey.length);
        }

        // Encrypt
        Cipher encCipher = getCipher(baseKey, ivec, Cipher.ENCRYPT_MODE);
        int blockSize = encCipher.getBlockSize();

        if ((len % blockSize) != 0) {
            throw new GeneralSecurityException(
                "length of data to be encrypted (" + len +
                ") is not a multiple of the blocksize (" + blockSize + ")");
        }

        int cipherSize = encCipher.getOutputSize(len);
        byte[] ciphertext = new byte[cipherSize];

        encCipher.doFinal(plaintext, 0, len, ciphertext, 0);
        return ciphertext;
    }

    /**
     * Decrypts data using specified key and initial vector.
     * @param baseKey encryption key to use
     * @param ciphertext  encrypted data to be decrypted
     * @param usage ignored
     */
    public byte[] decryptRaw(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len)
        throws GeneralSecurityException {

        if (debug) {
            System.err.println("usage: " + usage);
            if (ivec != null) {
                traceOutput("old_state.ivec", ivec, 0, ivec.length);
            }
            traceOutput("ciphertext", ciphertext, start, Math.min(len, 32));
            traceOutput("baseKey", baseKey, 0, baseKey.length);
        }

        Cipher decCipher = getCipher(baseKey, ivec, Cipher.DECRYPT_MODE);

        int blockSize = decCipher.getBlockSize();

        if ((len % blockSize) != 0) {
            throw new GeneralSecurityException(
                "length of data to be decrypted (" + len +
                ") is not a multiple of the blocksize (" + blockSize + ")");
        }

        byte[] decrypted = decCipher.doFinal(ciphertext, start, len);

        if (debug) {
            traceOutput("decrypted", decrypted, 0,
                Math.min(decrypted.length, 32));
        }

        return decrypted;
    }

    /**
     * @param baseKey key from which keys are to be derived using usage
     * @param ciphertext  E(Ke, conf | plaintext | padding, ivec) | H1[1..h]
     */
    public byte[] decrypt(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len) throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        byte[] Ke = null;
        byte[] Ki = null;

        try {
            // Derive encryption key
            byte[] constant = new byte[5];
            constant[0] = (byte) ((usage>>24)&0xff);
            constant[1] = (byte) ((usage>>16)&0xff);
            constant[2] = (byte) ((usage>>8)&0xff);
            constant[3] = (byte) (usage&0xff);

            constant[4] = (byte) 0xaa;

            Ke = dk(baseKey, constant);  // Encryption key

            if (debug) {
                System.err.println("usage: " + usage);
                if (ivec != null) {
                    traceOutput("old_state.ivec", ivec, 0, ivec.length);
                }
                traceOutput("ciphertext", ciphertext, start, Math.min(len, 32));
                traceOutput("constant", constant, 0, constant.length);
                traceOutput("baseKey", baseKey, 0, baseKey.length);
                traceOutput("Ke", Ke, 0, Ke.length);
            }

            Cipher decCipher = getCipher(Ke, ivec, Cipher.DECRYPT_MODE);
            int blockSize = decCipher.getBlockSize();

            // Decrypt [confounder | plaintext | padding] (without checksum)
            int cksumSize = getChecksumLength();
            int cipherSize = len - cksumSize;
            byte[] decrypted = decCipher.doFinal(ciphertext, start, cipherSize);

            if (debug) {
                traceOutput("decrypted", decrypted, 0,
                                Math.min(decrypted.length, 32));
            }

            // decrypted = [confounder | plaintext | padding]

            // Derive integrity key
            constant[4] = (byte) 0x55;
            Ki = dk(baseKey, constant);  // Integrity key
            if (debug) {
                traceOutput("constant", constant, 0, constant.length);
                traceOutput("Ki", Ki, 0, Ke.length);
            }

            // Verify checksum
            // H1 = HMAC(Ki, conf | plaintext | pad)
            byte[] calculatedHmac = getHmac(Ki, decrypted);

            if (debug) {
                traceOutput("calculated Hmac", calculatedHmac, 0,
                    calculatedHmac.length);
                traceOutput("message Hmac", ciphertext, cipherSize,
                    cksumSize);
            }

            boolean cksumFailed = false;
            if (calculatedHmac.length >= cksumSize) {
                for (int i = 0; i < cksumSize; i++) {
                    if (calculatedHmac[i] != ciphertext[cipherSize+i]) {
                        cksumFailed = true;
                        break;
                    }
                }
            }

            if (cksumFailed) {
                throw new GeneralSecurityException("Checksum failed");
            }

            // Prepare decrypted msg and ivec to be returned
            // Last blockSize bytes of ciphertext without checksum
            if (ivec != null && ivec.length == blockSize) {
                System.arraycopy(ciphertext,  start + cipherSize - blockSize,
                    ivec, 0, blockSize);
                if (debug) {
                    traceOutput("new_state.ivec", ivec, 0, ivec.length);
                }
            }

            // Get rid of confounder
            // [plaintext | padding]
            byte[] plaintext = new byte[decrypted.length - blockSize];
            System.arraycopy(decrypted, blockSize, plaintext,
                                0, plaintext.length);
            return plaintext; // padding still there
        } finally {
            if (Ke != null) {
                Arrays.fill(Ke, 0, Ke.length, (byte) 0);
            }
            if (Ki != null) {
                Arrays.fill(Ki, 0, Ki.length, (byte) 0);
            }
        }
    }

    // Round up to the next blocksize
    int roundup(int n, int blocksize) {
        return (((n + blocksize - 1) / blocksize) * blocksize);
    }

    public byte[] calculateChecksum(byte[] baseKey, int usage, byte[] input,
        int start, int len) throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        // Derive keys
        byte[] constant = new byte[5];
        constant[0] = (byte) ((usage>>24)&0xff);
        constant[1] = (byte) ((usage>>16)&0xff);
        constant[2] = (byte) ((usage>>8)&0xff);
        constant[3] = (byte) (usage&0xff);

        constant[4] = (byte) 0x99;

        byte[] Kc = dk(baseKey, constant);  // Checksum key
        if (debug) {
            System.err.println("usage: " + usage);
            traceOutput("input", input, start, Math.min(len, 32));
            traceOutput("constant", constant, 0, constant.length);
            traceOutput("baseKey", baseKey, 0, baseKey.length);
            traceOutput("Kc", Kc, 0, Kc.length);
        }

        try {
            // Generate checksum
            // H1 = HMAC(Kc, input)
            byte[] hmac = getHmac(Kc, input);
            if (debug) {
                traceOutput("hmac", hmac, 0, hmac.length);
            }
            if (hmac.length == getChecksumLength()) {
                return hmac;
            } else if (hmac.length > getChecksumLength()) {
                byte[] buf = new byte[getChecksumLength()];
                System.arraycopy(hmac, 0, buf, 0, buf.length);
                return buf;
            } else {
                throw new GeneralSecurityException("checksum size too short: " +
                    hmac.length + "; expecting : " + getChecksumLength());
            }
        } finally {
            Arrays.fill(Kc, 0, Kc.length, (byte)0);
        }
    }

    // DK(Key, Constant) = random-to-key(DR(Key, Constant))
    byte[] dk(byte[] key, byte[] constant)
        throws GeneralSecurityException {
        return randomToKey(dr(key, constant));
    }

    /*
     * From RFC 3961.
     *
     * DR(Key, Constant) = k-truncate(E(Key, Constant,
     *                                  initial-cipher-state))
     *
     * Here DR is the random-octet generation function described below, and
     * DK is the key-derivation function produced from it.  In this
     * construction, E(Key, Plaintext, CipherState) is a cipher, Constant is
     * a well-known constant determined by the specific usage of this
     * function, and k-truncate truncates its argument by taking the first k
     * bits.  Here, k is the key generation seed length needed for the
     * encryption system.
     *
     * The output of the DR function is a string of bits; the actual key is
     * produced by applying the cryptosystem's random-to-key operation on
     * this bitstring.
     *
     * If the Constant is smaller than the cipher block size of E, then it
     * must be expanded with n-fold() so it can be encrypted.  If the output
     * of E is shorter than k bits it is fed back into the encryption as
     * many times as necessary.  The construct is as follows (where |
     * indicates concatentation):
     *
     * K1 = E(Key, n-fold(Constant), initial-cipher-state)
     * K2 = E(Key, K1, initial-cipher-state)
     * K3 = E(Key, K2, initial-cipher-state)
     * K4 = ...
     *
     * DR(Key, Constant) = k-truncate(K1 | K2 | K3 | K4 ...)
     */
    protected byte[] dr(byte[] key, byte[] constant)
        throws GeneralSecurityException {

        Cipher encCipher = getCipher(key, null, Cipher.ENCRYPT_MODE);
        int blocksize = encCipher.getBlockSize();

        if (constant.length != blocksize) {
            constant = nfold(constant, blocksize * 8);
        }
        byte[] toBeEncrypted = constant;

        int keybytes = (getKeySeedLength()>>3);  // from bits to bytes
        byte[] rawkey = new byte[keybytes];
        int posn = 0;

        /* loop encrypting the blocks until enough key bytes are generated */
        int n = 0, len;
        while (n < keybytes) {
            if (debug) {
                System.err.println("Encrypting: " +
                    bytesToString(toBeEncrypted));
            }

            byte[] cipherBlock = encCipher.doFinal(toBeEncrypted);
            if (debug) {
                System.err.println("K: " + ++posn + " = " +
                    bytesToString(cipherBlock));
            }

            len = (keybytes - n <= cipherBlock.length ? (keybytes - n) :
                cipherBlock.length);
            if (debug) {
                System.err.println("copying " + len + " key bytes");
            }
            System.arraycopy(cipherBlock, 0, rawkey, n, len);
            n += len;
            toBeEncrypted = cipherBlock;
        }
        return rawkey;
    }

// ---------------------------------

    // From MIT-1.3.1 distribution
    /*
     * n-fold(k-bits):
     *   l = lcm(n,k)
     *   r = l/k
     * s = k-bits | k-bits rot 13 | k-bits rot 13*2 | ... | k-bits rot 13*(r-1)
     * compute the 1's complement sum:
     * n-fold = s[0..n-1]+s[n..2n-1]+s[2n..3n-1]+..+s[(k-1)*n..k*n-1]
     */

    /*
     * representation: msb first, assume n and k are multiples of 8, and
     *  that k>=16.  this is the case of all the cryptosystems which are
     *  likely to be used.  this function can be replaced if that
     *  assumption ever fails.
     */

    /* input length is in bits */
    static byte[] nfold(byte[] in, int outbits) {

        int inbits = in.length;
        outbits >>= 3;    // count in bytes

        /* first compute lcm(n,k) */
        int a, b, c, lcm;
        a = outbits;  // n
        b = inbits;   // k

        while (b != 0) {
            c = b;
            b = a % b;
            a = c;
        }
        lcm = outbits*inbits/a;

        if (debug) {
            System.err.println("k: " + inbits);
            System.err.println("n: " + outbits);
            System.err.println("lcm: " + lcm);
        }

        /* now do the real work */
        byte[] out = new byte[outbits];
        Arrays.fill(out, (byte)0);

        int thisbyte = 0;
        int msbit, i, bval, oval;

        // this will end up cycling through k lcm(k,n)/k times, which
        // is correct
        for (i = lcm-1; i >= 0; i--) {
            /* compute the msbit in k which gets added into this byte */
            msbit = (/* first, start with msbit in the first, unrotated byte */
                ((inbits<<3)-1)
                /* then, for each byte, shift to right for each repetition */
                + (((inbits<<3)+13)*(i/inbits))
                /* last, pick out correct byte within that shifted repetition */
                + ((inbits-(i%inbits)) << 3)) % (inbits << 3);

            /* pull out the byte value itself */
            // Mask off values using &0xff to get only the lower byte
            // Use >>> to avoid sign extension
            bval =  ((((in[((inbits-1)-(msbit>>>3))%inbits]&0xff)<<8)|
                (in[((inbits)-(msbit>>>3))%inbits]&0xff))
                >>>((msbit&7)+1))&0xff;

            /*
            System.err.println("((" +
                ((in[((inbits-1)-(msbit>>>3))%inbits]&0xff)<<8)
                + "|" + (in[((inbits)-(msbit>>>3))%inbits]&0xff) + ")"
                + ">>>" + ((msbit&7)+1) + ")&0xff = " + bval);
            */

            thisbyte += bval;

            /* do the addition */
            // Mask off values using &0xff to get only the lower byte
            oval = (out[i%outbits]&0xff);
            thisbyte += oval;
            out[i%outbits] = (byte) (thisbyte&0xff);

            if (debug) {
                System.err.println("msbit[" + i + "] = " +  msbit + "\tbval=" +
                    Integer.toHexString(bval) + "\toval=" +
                    Integer.toHexString(oval)
                    + "\tsum = " + Integer.toHexString(thisbyte));
            }


            /* keep around the carry bit, if any */
            thisbyte >>>= 8;

            if (debug) {
                System.err.println("carry=" + thisbyte);
            }
        }

        /* if there's a carry bit left over, add it back in */
        if (thisbyte != 0) {
            for (i = outbits-1; i >= 0; i--) {
                /* do the addition */
                thisbyte += (out[i]&0xff);
                out[i] = (byte) (thisbyte&0xff);

                /* keep around the carry bit, if any */
                thisbyte >>>= 8;
            }
        }

        return out;
    }

    // Routines used for debugging
    static String bytesToString(byte[] digest) {
        // Get character representation of digest
        StringBuilder digestString = new StringBuilder();

        for (int i = 0; i < digest.length; i++) {
            if ((digest[i] & 0x000000ff) < 0x10) {
                digestString.append('0').append(Integer.toHexString(digest[i] & 0x000000ff));
            } else {
                digestString.append(
                    Integer.toHexString(digest[i] & 0x000000ff));
            }
        }
        return digestString.toString();
    }

    private static byte[] binaryStringToBytes(String str) {
        char[] usageStr = str.toCharArray();
        byte[] usage = new byte[usageStr.length/2];
        for (int i = 0; i < usage.length; i++) {
            byte a = Byte.parseByte(new String(usageStr, i*2, 1), 16);
            byte b = Byte.parseByte(new String(usageStr, i*2 + 1, 1), 16);
            usage[i] = (byte) ((a<<4)|b);
        }
        return usage;
    }

    static void traceOutput(String traceTag, byte[] output, int offset,
        int len) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(len);
            new HexDumpEncoder().encodeBuffer(
                new ByteArrayInputStream(output, offset, len), out);

            System.err.println(traceTag + ":\n" + out.toString());
        } catch (Exception e) {
        }
    }

// String.getBytes(UTF_8);
// Do this instead of using String to avoid making password immutable
    static byte[] charToUtf8(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = UTF_8.encode(cb);
        int len = bb.limit();
        byte[] answer = new byte[len];
        bb.get(answer, 0, len);
        return answer;
    }

    static byte[] charToUtf16(char[] chars) {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = UTF_16LE.encode(cb);
        int len = bb.limit();
        byte[] answer = new byte[len];
        bb.get(answer, 0, len);
        return answer;
    }
}
