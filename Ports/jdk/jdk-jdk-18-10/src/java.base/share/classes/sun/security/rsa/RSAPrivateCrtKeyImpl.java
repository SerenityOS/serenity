/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.rsa;

import java.io.IOException;
import java.math.BigInteger;

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.Arrays;

import sun.security.util.*;

import sun.security.pkcs.PKCS8Key;

import sun.security.rsa.RSAUtil.KeyType;

/**
 * RSA private key implementation for "RSA", "RSASSA-PSS" algorithms in CRT form.
 * For non-CRT private keys, see RSAPrivateKeyImpl. We need separate classes
 * to ensure correct behavior in instanceof checks, etc.
 *
 * Note: RSA keys must be at least 512 bits long
 *
 * @see RSAPrivateKeyImpl
 * @see RSAKeyFactory
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class RSAPrivateCrtKeyImpl
        extends PKCS8Key implements RSAPrivateCrtKey {

    @java.io.Serial
    private static final long serialVersionUID = -1326088454257084918L;

    private BigInteger n;       // modulus
    private BigInteger e;       // public exponent
    private BigInteger d;       // private exponent
    private BigInteger p;       // prime p
    private BigInteger q;       // prime q
    private BigInteger pe;      // prime exponent p
    private BigInteger qe;      // prime exponent q
    private BigInteger coeff;   // CRT coeffcient

    private transient KeyType type;

    // Optional parameters associated with this RSA key
    // specified in the encoding of its AlgorithmId.
    // Must be null for "RSA" keys.
    private transient AlgorithmParameterSpec keyParams;

    /**
     * Generate a new RSAPrivate(Crt)Key from the specified type,
     * format and encoding. Returns a CRT key if possible and a non-CRT
     * key otherwise.
     * Also used by SunPKCS11 provider.
     */
    public static RSAPrivateKey newKey(KeyType type, String format,
            byte[] encoded) throws InvalidKeyException {
        if (encoded == null || encoded.length == 0) {
            throw new InvalidKeyException("Missing key encoding");
        }
        switch (format) {
        case "PKCS#8":
            RSAPrivateCrtKeyImpl key = new RSAPrivateCrtKeyImpl(encoded);
            RSAKeyFactory.checkKeyAlgo(key, type.keyAlgo);
            // check all CRT-specific components are available, if any one
            // missing, return a non-CRT key instead
            if ((key.getPublicExponent().signum() == 0) ||
                (key.getPrimeExponentP().signum() == 0) ||
                (key.getPrimeExponentQ().signum() == 0) ||
                (key.getPrimeP().signum() == 0) ||
                (key.getPrimeQ().signum() == 0) ||
                (key.getCrtCoefficient().signum() == 0)) {
                return new RSAPrivateKeyImpl(key.type, key.keyParams,
                    key.getModulus(), key.getPrivateExponent());
            } else {
                return key;
            }
        case "PKCS#1":
            try {
                BigInteger[] comps = parseASN1(encoded);
                if ((comps[1].signum() == 0) || (comps[3].signum() == 0) ||
                    (comps[4].signum() == 0) || (comps[5].signum() == 0) ||
                    (comps[6].signum() == 0) || (comps[7].signum() == 0)) {
                    return new RSAPrivateKeyImpl(type, null, comps[0],
                            comps[2]);
                } else {
                    return new RSAPrivateCrtKeyImpl(type, null, comps[0],
                            comps[1], comps[2], comps[3], comps[4], comps[5],
                            comps[6], comps[7]);
                }
            } catch (IOException ioe) {
                throw new InvalidKeyException("Invalid PKCS#1 encoding", ioe);
            }
        default:
            throw new InvalidKeyException("Unsupported RSA Private(Crt)Key "
                    + "format: " + format);
        }
    }

    /**
     * Generate a new key from the specified type and components.
     * Returns a CRT key if possible and a non-CRT key otherwise.
     * Used by SunPKCS11 provider.
     */
    public static RSAPrivateKey newKey(KeyType type,
            AlgorithmParameterSpec params,
            BigInteger n, BigInteger e, BigInteger d,
            BigInteger p, BigInteger q, BigInteger pe, BigInteger qe,
            BigInteger coeff) throws InvalidKeyException {
        RSAPrivateKey key;
        if ((e.signum() == 0) || (p.signum() == 0) ||
            (q.signum() == 0) || (pe.signum() == 0) ||
            (qe.signum() == 0) || (coeff.signum() == 0)) {
            // if any component is missing, return a non-CRT key
            return new RSAPrivateKeyImpl(type, params, n, d);
        } else {
            return new RSAPrivateCrtKeyImpl(type, params, n, e, d,
                p, q, pe, qe, coeff);
        }
    }

    /**
     * Construct a key from its encoding. Called from newKey above.
     */
    private RSAPrivateCrtKeyImpl(byte[] encoded) throws InvalidKeyException {
        super(encoded);
        parseKeyBits();
        RSAKeyFactory.checkRSAProviderKeyLengths(n.bitLength(), e);
        try {
            // check the validity of oid and params
            Object[] o = RSAUtil.getTypeAndParamSpec(algid);
            this.type = (KeyType) o[0];
            this.keyParams = (AlgorithmParameterSpec) o[1];
        } catch (ProviderException e) {
            throw new InvalidKeyException(e);
        }
    }

    /**
     * Construct a RSA key from its components. Used by the
     * RSAKeyFactory and the RSAKeyPairGenerator.
     */
    RSAPrivateCrtKeyImpl(KeyType type, AlgorithmParameterSpec keyParams,
            BigInteger n, BigInteger e, BigInteger d,
            BigInteger p, BigInteger q, BigInteger pe, BigInteger qe,
            BigInteger coeff) throws InvalidKeyException {
        RSAKeyFactory.checkRSAProviderKeyLengths(n.bitLength(), e);

        this.n = n;
        this.e = e;
        this.d = d;
        this.p = p;
        this.q = q;
        this.pe = pe;
        this.qe = qe;
        this.coeff = coeff;

        try {
            // validate and generate the algid encoding
            algid = RSAUtil.createAlgorithmId(type, keyParams);
        } catch (ProviderException exc) {
            throw new InvalidKeyException(exc);
        }

        this.type = type;
        this.keyParams = keyParams;

        try {
            byte[][] nbytes = new byte[8][];
            nbytes[0] = n.toByteArray();
            nbytes[1] = e.toByteArray();
            nbytes[2] = d.toByteArray();
            nbytes[3] = p.toByteArray();
            nbytes[4] = q.toByteArray();
            nbytes[5] = pe.toByteArray();
            nbytes[6] = qe.toByteArray();
            nbytes[7] = coeff.toByteArray();

            // Initiate with a big enough size so there's no need to
            // reallocate memory later and thus can be cleaned up
            // reliably.
            DerOutputStream out = new DerOutputStream(
                    nbytes[0].length + nbytes[1].length +
                    nbytes[2].length + nbytes[3].length +
                    nbytes[4].length + nbytes[5].length +
                    nbytes[6].length + nbytes[7].length +
                    100); // Enough for version(3) and 8 tag+length(3 or 4)
            out.putInteger(0); // version must be 0
            out.putInteger(nbytes[0]);
            out.putInteger(nbytes[1]);
            out.putInteger(nbytes[2]);
            out.putInteger(nbytes[3]);
            out.putInteger(nbytes[4]);
            out.putInteger(nbytes[5]);
            out.putInteger(nbytes[6]);
            out.putInteger(nbytes[7]);
            // Private values from [2] on.
            Arrays.fill(nbytes[2], (byte)0);
            Arrays.fill(nbytes[3], (byte)0);
            Arrays.fill(nbytes[4], (byte)0);
            Arrays.fill(nbytes[5], (byte)0);
            Arrays.fill(nbytes[6], (byte)0);
            Arrays.fill(nbytes[7], (byte)0);
            DerValue val = DerValue.wrap(DerValue.tag_Sequence, out);
            key = val.toByteArray();
            val.clear();
        } catch (IOException exc) {
            // should never occur
            throw new InvalidKeyException(exc);
        }
    }

    // see JCA doc
    @Override
    public String getAlgorithm() {
        return type.keyAlgo;
    }

    // see JCA doc
    @Override
    public BigInteger getModulus() {
        return n;
    }

    // see JCA doc
    @Override
    public BigInteger getPublicExponent() {
        return e;
    }

    // see JCA doc
    @Override
    public BigInteger getPrivateExponent() {
        return d;
    }

    // see JCA doc
    @Override
    public BigInteger getPrimeP() {
        return p;
    }

    // see JCA doc
    @Override
    public BigInteger getPrimeQ() {
        return q;
    }

    // see JCA doc
    @Override
    public BigInteger getPrimeExponentP() {
        return pe;
    }

    // see JCA doc
    @Override
    public BigInteger getPrimeExponentQ() {
        return qe;
    }

    // see JCA doc
    @Override
    public BigInteger getCrtCoefficient() {
        return coeff;
    }

    // see JCA doc
    @Override
    public AlgorithmParameterSpec getParams() {
        return keyParams;
    }

    // return a string representation of this key for debugging
    @Override
    public String toString() {
        return "SunRsaSign " + type.keyAlgo + " private CRT key, "
               + n.bitLength() + " bits" + "\n  params: " + keyParams
               + "\n  modulus: " + n + "\n  private exponent: " + d;
    }

    // utility method for parsing DER encoding of RSA private keys in PKCS#1
    // format as defined in RFC 8017 Appendix A.1.2, i.e. SEQ of version, n,
    // e, d, p, q, pe, qe, and coeff, and return the parsed components.
    private static BigInteger[] parseASN1(byte[] raw) throws IOException {
        DerValue derValue = new DerValue(raw);
        try {
            if (derValue.tag != DerValue.tag_Sequence) {
                throw new IOException("Not a SEQUENCE");
            }
            int version = derValue.data.getInteger();
            if (version != 0) {
                throw new IOException("Version must be 0");
            }

            BigInteger[] result = new BigInteger[8]; // n, e, d, p, q, pe, qe, coeff
            /*
             * Some implementations do not correctly encode ASN.1 INTEGER values
             * in 2's complement format, resulting in a negative integer when
             * decoded. Correct the error by converting it to a positive integer.
             *
             * See CR 6255949
             */
            for (int i = 0; i < result.length; i++) {
                result[i] = derValue.data.getPositiveBigInteger();
            }
            if (derValue.data.available() != 0) {
                throw new IOException("Extra data available");
            }
            return result;
        } finally {
            derValue.clear();
        }
    }

    private void parseKeyBits() throws InvalidKeyException {
        try {
            BigInteger[] comps = parseASN1(key);
            n = comps[0];
            e = comps[1];
            d = comps[2];
            p = comps[3];
            q = comps[4];
            pe = comps[5];
            qe = comps[6];
            coeff = comps[7];
        } catch (IOException e) {
            throw new InvalidKeyException("Invalid RSA private key", e);
        }
    }
}
