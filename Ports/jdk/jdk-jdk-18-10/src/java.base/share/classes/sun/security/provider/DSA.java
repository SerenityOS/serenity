/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import java.security.*;
import java.security.SecureRandom;
import java.security.interfaces.*;
import java.security.spec.*;

import sun.security.util.Debug;
import sun.security.util.DerValue;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.jca.JCAUtil;

/**
 * The Digital Signature Standard (using the Digital Signature
 * Algorithm), as described in fips186-3 of the National Instute of
 * Standards and Technology (NIST), using SHA digest algorithms
 * from FIPS180-3.
 *
 * This file contains the signature implementation for the
 * SHA1withDSA (DSS), SHA224withDSA, SHA256withDSA, SHA384withDSA,
 * SHA512withDSA, SHA3-224withDSA, SHA3-256withDSA, SHA3-384withDSA,
 * SHA3-512withDSA, as well as RawDSA, used by TLS among others.
 * RawDSA expects the 20 byte SHA-1 digest as input via update rather
 * than the original data like other signature implementations.
 *
 * In addition, IEEE P1363 signature format is supported. The
 * corresponding implementation is registered under <sig>inP1363Format,
 * e.g. SHA256withDSAinP1363Format.
 *
 * @author Benjamin Renaud
 *
 * @since   1.1
 *
 * @see DSAPublicKey
 * @see DSAPrivateKey
 */
abstract class DSA extends SignatureSpi {

    /* Are we debugging? */
    private static final boolean debug = false;

    /* The number of bits used in exponent blinding */
    private static final int BLINDING_BITS = 7;

    /* The constant component of the exponent blinding value */
    private static final BigInteger BLINDING_CONSTANT =
        BigInteger.valueOf(1 << BLINDING_BITS);

    /* The parameter object */
    private DSAParams params;

    /* algorithm parameters */
    private BigInteger presetP, presetQ, presetG;

    /* The public key, if any */
    private BigInteger presetY;

    /* The private key, if any */
    private BigInteger presetX;

    /* The RNG used to output a seed for generating k */
    private SecureRandom signingRandom;

    /* The message digest object used */
    private final MessageDigest md;

    /* The format. true for the IEEE P1363 format. false (default) for ASN.1 */
    private final boolean p1363Format;

    /**
     * Construct a blank DSA object. It must be
     * initialized before being usable for signing or verifying.
     */
    DSA(MessageDigest md) {
        this(md, false);
    }

    /**
     * Construct a blank DSA object that will use the specified
     * signature format. {@code p1363Format} should be {@code true} to
     * use the IEEE P1363 format. If {@code p1363Format} is {@code false},
     * the DER-encoded ASN.1 format will be used. The DSA object must be
     * initialized before being usable for signing or verifying.
     */
    DSA(MessageDigest md, boolean p1363Format) {
        super();
        this.md = md;
        this.p1363Format = p1363Format;
    }

    private static void checkKey(DSAParams params, int digestLen, String mdAlgo)
        throws InvalidKeyException {
        // FIPS186-3 states in sec4.2 that a hash function which provides
        // a lower security strength than the (L, N) pair ordinarily should
        // not be used.
        int valueN = params.getQ().bitLength();
        if (valueN > digestLen) {
            throw new InvalidKeyException("The security strength of " +
                mdAlgo + " digest algorithm is not sufficient for this key size");
        }
    }

    /**
     * Initialize the DSA object with a DSA private key.
     *
     * @param privateKey the DSA private key
     *
     * @exception InvalidKeyException if the key is not a valid DSA private
     * key.
     */
    protected void engineInitSign(PrivateKey privateKey)
            throws InvalidKeyException {
        if (!(privateKey instanceof java.security.interfaces.DSAPrivateKey)) {
            throw new InvalidKeyException("not a DSA private key: " +
                                          privateKey);
        }

        java.security.interfaces.DSAPrivateKey priv =
            (java.security.interfaces.DSAPrivateKey)privateKey;

        // check for algorithm specific constraints before doing initialization
        DSAParams params = priv.getParams();
        if (params == null) {
            throw new InvalidKeyException("DSA private key lacks parameters");
        }

        // check key size against hash output size for signing
        // skip this check for verification to minimize impact on existing apps
        if (!"NullDigest20".equals(md.getAlgorithm())) {
            checkKey(params, md.getDigestLength()*8, md.getAlgorithm());
        }

        this.params = params;
        this.presetX = priv.getX();
        this.presetY = null;
        this.presetP = params.getP();
        this.presetQ = params.getQ();
        this.presetG = params.getG();
        this.md.reset();
    }
    /**
     * Initialize the DSA object with a DSA public key.
     *
     * @param publicKey the DSA public key.
     *
     * @exception InvalidKeyException if the key is not a valid DSA public
     * key.
     */
    protected void engineInitVerify(PublicKey publicKey)
            throws InvalidKeyException {
        if (!(publicKey instanceof java.security.interfaces.DSAPublicKey)) {
            throw new InvalidKeyException("not a DSA public key: " +
                                          publicKey);
        }
        java.security.interfaces.DSAPublicKey pub =
            (java.security.interfaces.DSAPublicKey)publicKey;

        // check for algorithm specific constraints before doing initialization
        DSAParams params = pub.getParams();
        if (params == null) {
            throw new InvalidKeyException("DSA public key lacks parameters");
        }
        this.params = params;
        this.presetY = pub.getY();
        this.presetX = null;
        this.presetP = params.getP();
        this.presetQ = params.getQ();
        this.presetG = params.getG();
        this.md.reset();
    }

    /**
     * Update a byte to be signed or verified.
     */
    protected void engineUpdate(byte b) {
        md.update(b);
    }

    /**
     * Update an array of bytes to be signed or verified.
     */
    protected void engineUpdate(byte[] data, int off, int len) {
        md.update(data, off, len);
    }

    protected void engineUpdate(ByteBuffer b) {
        md.update(b);
    }


    /**
     * Sign all the data thus far updated. The signature format is
     * determined by {@code p1363Format}. If {@code p1363Format} is
     * {@code false} (the default), then the signature is formatted
     * according to the Canonical Encoding Rules, returned as a DER
     * sequence of Integers, r and s. If {@code p1363Format} is
     * {@code false}, the signature is returned in the IEEE P1363
     * format, which is the concatenation or r and s.
     *
     * @return a signature block formatted according to the format
     * indicated by {@code p1363Format}
     *
     * @exception SignatureException if the signature object was not
     * properly initialized, or if another exception occurs.
     *
     * @see sun.security.DSA#engineUpdate
     * @see sun.security.DSA#engineVerify
     */
    protected byte[] engineSign() throws SignatureException {
        BigInteger k = generateK(presetQ);
        BigInteger r = generateR(presetP, presetQ, presetG, k);
        BigInteger s = generateS(presetX, presetQ, r, k);

        if (p1363Format) {
            // Return the concatenation of r and s
            byte[] rBytes = r.toByteArray();
            byte[] sBytes = s.toByteArray();

            int size = presetQ.bitLength() / 8;
            byte[] outseq = new byte[size * 2];

            int rLength = rBytes.length;
            int sLength = sBytes.length;
            int i;
            for (i = rLength; i > 0 && rBytes[rLength - i] == 0; i--);

            int j;
            for (j = sLength;
                    j > 0 && sBytes[sLength - j] == 0; j--);

            System.arraycopy(rBytes, rLength - i, outseq, size - i, i);
            System.arraycopy(sBytes, sLength - j, outseq, size * 2 - j, j);

            return outseq;
        } else {
            // Return the DER-encoded ASN.1 form
            try {
                DerOutputStream outseq = new DerOutputStream(100);
                outseq.putInteger(r);
                outseq.putInteger(s);
                DerValue result = new DerValue(DerValue.tag_Sequence,
                        outseq.toByteArray());

                return result.toByteArray();

            } catch (IOException e) {
                throw new SignatureException("error encoding signature");
            }
        }
    }

    /**
     * Verify all the data thus far updated.
     *
     * @param signature the alleged signature, encoded using the
     * Canonical Encoding Rules, as a sequence of integers, r and s.
     *
     * @exception SignatureException if the signature object was not
     * properly initialized, or if another exception occurs.
     *
     * @see sun.security.DSA#engineUpdate
     * @see sun.security.DSA#engineSign
     */
    protected boolean engineVerify(byte[] signature)
            throws SignatureException {
        return engineVerify(signature, 0, signature.length);
    }

    /**
     * Verify all the data thus far updated.
     *
     * @param signature the alleged signature, encoded using the
     * format indicated by {@code p1363Format}. If {@code p1363Format}
     * is {@code false} (the default), then the signature is formatted
     * according to the Canonical Encoding Rules, as a DER sequence of
     * Integers, r and s. If {@code p1363Format} is {@code false},
     * the signature is in the IEEE P1363 format, which is the
     * concatenation or r and s.
     *
     * @param offset the offset to start from in the array of bytes.
     *
     * @param length the number of bytes to use, starting at offset.
     *
     * @exception SignatureException if the signature object was not
     * properly initialized, or if another exception occurs.
     *
     * @see sun.security.DSA#engineUpdate
     * @see sun.security.DSA#engineSign
     */
    protected boolean engineVerify(byte[] signature, int offset, int length)
            throws SignatureException {

        BigInteger r = null;
        BigInteger s = null;

        if (p1363Format) {
            if ((length & 1) == 1) {
                // length of signature byte array should be even
                throw new SignatureException("invalid signature format");
            }
            int mid = length/2;
            r = new BigInteger(Arrays.copyOfRange(signature, 0, mid));
            s = new BigInteger(Arrays.copyOfRange(signature, mid, length));
        } else {
            // first decode the signature.
            try {
                // Enforce strict DER checking for signatures
                DerInputStream in =
                    new DerInputStream(signature, offset, length, false);
                DerValue[] values = in.getSequence(2);

                // check number of components in the read sequence
                // and trailing data
                if ((values.length != 2) || (in.available() != 0)) {
                    throw new IOException("Invalid encoding for signature");
                }
                r = values[0].getBigInteger();
                s = values[1].getBigInteger();
            } catch (IOException e) {
                throw new SignatureException("Invalid encoding for signature", e);
            }
        }

        // some implementations do not correctly encode values in the ASN.1
        // 2's complement format. force r and s to be positive in order to
        // to validate those signatures
        if (r.signum() < 0) {
            r = new BigInteger(1, r.toByteArray());
        }
        if (s.signum() < 0) {
            s = new BigInteger(1, s.toByteArray());
        }

        if ((r.compareTo(presetQ) == -1) && (s.compareTo(presetQ) == -1)) {
            BigInteger w = generateW(presetP, presetQ, presetG, s);
            BigInteger v = generateV(presetY, presetP, presetQ, presetG, w, r);
            return v.equals(r);
        } else {
            throw new SignatureException("invalid signature: out of range values");
        }
    }

    @Deprecated
    protected void engineSetParameter(String key, Object param) {
        throw new InvalidParameterException("No parameter accepted");
    }

    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("No parameter accepted");
        }
    }

    @Deprecated
    protected Object engineGetParameter(String key) {
        return null;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }


    private BigInteger generateR(BigInteger p, BigInteger q, BigInteger g,
                         BigInteger k) {

        // exponent blinding to hide information from timing channel
        SecureRandom random = getSigningRandom();
        // start with a random blinding component
        BigInteger blindingValue = new BigInteger(BLINDING_BITS, random);
        // add the fixed blinding component
        blindingValue = blindingValue.add(BLINDING_CONSTANT);
        // replace k with a blinded value that is congruent (mod q)
        k = k.add(q.multiply(blindingValue));

        BigInteger temp = g.modPow(k, p);
        return temp.mod(q);
    }

    private BigInteger generateS(BigInteger x, BigInteger q,
            BigInteger r, BigInteger k) throws SignatureException {

        byte[] s2;
        try {
            s2 = md.digest();
        } catch (RuntimeException re) {
            // Only for RawDSA due to its 20-byte length restriction
            throw new SignatureException(re.getMessage());
        }
        // get the leftmost min(N, outLen) bits of the digest value
        int nBytes = q.bitLength()/8;
        if (nBytes < s2.length) {
            s2 = Arrays.copyOfRange(s2, 0, nBytes);
        }
        BigInteger z = new BigInteger(1, s2);
        BigInteger k1 = k.modInverse(q);

        return x.multiply(r).add(z).multiply(k1).mod(q);
    }

    private BigInteger generateW(BigInteger p, BigInteger q,
                         BigInteger g, BigInteger s) {
        return s.modInverse(q);
    }

    private BigInteger generateV(BigInteger y, BigInteger p,
             BigInteger q, BigInteger g, BigInteger w, BigInteger r)
             throws SignatureException {

        byte[] s2;
        try {
            s2 = md.digest();
        } catch (RuntimeException re) {
            // Only for RawDSA due to its 20-byte length restriction
            throw new SignatureException(re.getMessage());
        }
        // get the leftmost min(N, outLen) bits of the digest value
        int nBytes = q.bitLength()/8;
        if (nBytes < s2.length) {
            s2 = Arrays.copyOfRange(s2, 0, nBytes);
        }
        BigInteger z = new BigInteger(1, s2);

        BigInteger u1 = z.multiply(w).mod(q);
        BigInteger u2 = (r.multiply(w)).mod(q);

        BigInteger t1 = g.modPow(u1,p);
        BigInteger t2 = y.modPow(u2,p);
        BigInteger t3 = t1.multiply(t2);
        BigInteger t5 = t3.mod(p);
        return t5.mod(q);
    }

    protected BigInteger generateK(BigInteger q) {
        // Implementation defined in FIPS 186-4 AppendixB.2.1.
        SecureRandom random = getSigningRandom();
        byte[] kValue = new byte[(q.bitLength() + 7)/8 + 8];

        random.nextBytes(kValue);
        return new BigInteger(1, kValue).mod(
                q.subtract(BigInteger.ONE)).add(BigInteger.ONE);
    }

    // Use the application-specified SecureRandom Object if provided.
    // Otherwise, use our default SecureRandom Object.
    protected SecureRandom getSigningRandom() {
        if (signingRandom == null) {
            if (appRandom != null) {
                signingRandom = appRandom;
            } else {
                signingRandom = JCAUtil.getSecureRandom();
            }
        }
        return signingRandom;
    }

    /**
     * Return a human readable rendition of the engine.
     */
    public String toString() {
        String printable = "DSA Signature";
        if (presetP != null && presetQ != null && presetG != null) {
            printable += "\n\tp: " + Debug.toHexString(presetP);
            printable += "\n\tq: " + Debug.toHexString(presetQ);
            printable += "\n\tg: " + Debug.toHexString(presetG);
        } else {
            printable += "\n\t P, Q or G not initialized.";
        }
        if (presetY != null) {
            printable += "\n\ty: " + Debug.toHexString(presetY);
        }
        if (presetY == null && presetX == null) {
            printable += "\n\tUNINIIALIZED";
        }
        return printable;
    }

    /**
     * SHA3-224withDSA implementation.
     */
    public static final class SHA3_224withDSA extends DSA {
        public SHA3_224withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-224"));
        }
    }

    /**
     * SHA3-224withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA3_224withDSAinP1363Format extends DSA {
        public SHA3_224withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-224"), true);
        }
    }

    /**
     * Standard SHA3-256withDSA implementation.
     */
    public static final class SHA3_256withDSA extends DSA {
        public SHA3_256withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-256"));
        }
    }

    /**
     * Standard SHA3-256withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA3_256withDSAinP1363Format extends DSA {
        public SHA3_256withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-256"), true);
        }
    }

    /**
     * Standard SHA3-384withDSA implementation.
     */
    public static final class SHA3_384withDSA extends DSA {
        public SHA3_384withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-384"));
        }
    }

    /**
     * Standard SHA3-384withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA3_384withDSAinP1363Format extends DSA {
        public SHA3_384withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-384"), true);
        }
    }

    /**
     * Standard SHA3-512withDSA implementation.
     */
    public static final class SHA3_512withDSA extends DSA {
        public SHA3_512withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-512"));
        }
    }

    /**
     * Standard SHA3-512withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA3_512withDSAinP1363Format extends DSA {
        public SHA3_512withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA3-512"), true);
        }
    }

    /**
     * Standard SHA224withDSA implementation as defined in FIPS186-3.
     */
    public static final class SHA224withDSA extends DSA {
        public SHA224withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-224"));
        }
    }

    /**
     * SHA224withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA224withDSAinP1363Format extends DSA {
        public SHA224withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-224"), true);
        }
    }

    /**
     * Standard SHA256withDSA implementation as defined in FIPS186-3.
     */
    public static final class SHA256withDSA extends DSA {
        public SHA256withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-256"));
        }
    }

    /**
     * SHA256withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA256withDSAinP1363Format extends DSA {
        public SHA256withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-256"), true);
        }
    }

    /**
     * Standard SHA384withDSA implementation as defined in FIPS186-3.
     */
    public static final class SHA384withDSA extends DSA {
        public SHA384withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-384"));
        }
    }

    /**
     * SHA384withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA384withDSAinP1363Format extends DSA {
        public SHA384withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-384"), true);
        }
    }

    /**
     * Standard SHA512withDSA implementation as defined in FIPS186-3.
     */
    public static final class SHA512withDSA extends DSA {
        public SHA512withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-512"));
        }
    }

    /**
     * SHA512withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA512withDSAinP1363Format extends DSA {
        public SHA512withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-512"), true);
        }
    }

    /**
     * Standard SHA1withDSA implementation.
     */
    public static final class SHA1withDSA extends DSA {
        public SHA1withDSA() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-1"));
        }
    }

    /**
     * SHA1withDSA implementation that uses the IEEE P1363 format.
     */
    public static final class SHA1withDSAinP1363Format extends DSA {
        public SHA1withDSAinP1363Format() throws NoSuchAlgorithmException {
            super(MessageDigest.getInstance("SHA-1"), true);
        }
    }

    /**
     * Raw DSA.
     *
     * Raw DSA requires the data to be exactly 20 bytes long. If it is
     * not, a SignatureException is thrown when sign()/verify() is called
     * per JCA spec.
     */
    static class Raw extends DSA {
        // Internal special-purpose MessageDigest impl for RawDSA
        // Only override whatever methods used
        // NOTE: no clone support
        public static final class NullDigest20 extends MessageDigest {
            // 20 byte digest buffer
            private final byte[] digestBuffer = new byte[20];

            // offset into the buffer; use Integer.MAX_VALUE to indicate
            // out-of-bound condition
            private int ofs = 0;

            protected NullDigest20() {
                super("NullDigest20");
            }
            protected void engineUpdate(byte input) {
                if (ofs == digestBuffer.length) {
                    ofs = Integer.MAX_VALUE;
                } else {
                    digestBuffer[ofs++] = input;
                }
            }
            protected void engineUpdate(byte[] input, int offset, int len) {
                if (len > (digestBuffer.length - ofs)) {
                    ofs = Integer.MAX_VALUE;
                } else {
                    System.arraycopy(input, offset, digestBuffer, ofs, len);
                    ofs += len;
                }
            }
            protected final void engineUpdate(ByteBuffer input) {
                int inputLen = input.remaining();
                if (inputLen > (digestBuffer.length - ofs)) {
                    ofs = Integer.MAX_VALUE;
                } else {
                    input.get(digestBuffer, ofs, inputLen);
                    ofs += inputLen;
                }
            }
            protected byte[] engineDigest() throws RuntimeException {
                if (ofs != digestBuffer.length) {
                    throw new RuntimeException
                        ("Data for RawDSA must be exactly 20 bytes long");
                }
                reset();
                return digestBuffer;
            }
            protected int engineDigest(byte[] buf, int offset, int len)
                throws DigestException {
                if (ofs != digestBuffer.length) {
                    throw new DigestException
                        ("Data for RawDSA must be exactly 20 bytes long");
                }
                if (len < digestBuffer.length) {
                    throw new DigestException
                        ("Output buffer too small; must be at least 20 bytes");
                }
                System.arraycopy(digestBuffer, 0, buf, offset, digestBuffer.length);
                reset();
                return digestBuffer.length;
            }

            protected void engineReset() {
                ofs = 0;
            }
            protected final int engineGetDigestLength() {
                return digestBuffer.length;
            }
        }

        private Raw(boolean p1363Format) throws NoSuchAlgorithmException {
            super(new NullDigest20(), p1363Format);
        }

    }

    /**
     * Standard Raw DSA implementation.
     */
    public static final class RawDSA extends Raw {
        public RawDSA() throws NoSuchAlgorithmException {
            super(false);
        }
    }

    /**
     * Raw DSA implementation that uses the IEEE P1363 format.
     */
    public static final class RawDSAinP1363Format extends Raw {
        public RawDSAinP1363Format() throws NoSuchAlgorithmException {
            super(true);
        }
    }
}
