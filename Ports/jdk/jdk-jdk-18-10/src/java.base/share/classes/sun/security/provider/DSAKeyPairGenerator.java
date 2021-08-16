/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;

import java.security.*;
import java.security.SecureRandom;
import java.security.interfaces.DSAParams;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.DSAParameterSpec;

import sun.security.jca.JCAUtil;
import static sun.security.util.SecurityProviderConstants.DEF_DSA_KEY_SIZE;
import static sun.security.util.SecurityProviderConstants.getDefDSASubprimeSize;

/**
 * This class generates DSA key parameters and public/private key
 * pairs according to the DSS standard NIST FIPS 186. It uses the
 * updated version of SHA, SHA-1 as described in FIPS 180-1.
 *
 * @author Benjamin Renaud
 * @author Andreas Sterbenz
 *
 */
class DSAKeyPairGenerator extends KeyPairGenerator {

    /* Length for prime P and subPrime Q in bits */
    private int plen;
    private int qlen;

    /* whether to force new parameters to be generated for each KeyPair */
    boolean forceNewParameters;

    /* preset algorithm parameters. */
    private DSAParameterSpec params;

    /* The source of random bits to use */
    private SecureRandom random;

    DSAKeyPairGenerator(int defaultKeySize) {
        super("DSA");
        initialize(defaultKeySize, null);
    }

    private static void checkStrength(int sizeP, int sizeQ) {
        if ((sizeP >= 512) && (sizeP <= 1024) && (sizeP % 64 == 0)
            && sizeQ == 160) {
            // traditional - allow for backward compatibility
            // L=multiples of 64 and between 512 and 1024 (inclusive)
            // N=160
        } else if (sizeP == 2048 && (sizeQ == 224 || sizeQ == 256)) {
            // L=2048, N=224 or 256
        } else if (sizeP == 3072 && sizeQ == 256) {
            // L=3072, N=256
        } else {
            throw new InvalidParameterException
                ("Unsupported prime and subprime size combination: " +
                 sizeP + ", " + sizeQ);
        }
    }

    public void initialize(int modlen, SecureRandom random) {
        init(modlen, random, false);
    }

    /**
     * Initializes the DSA object using a parameter object.
     *
     * @param params the parameter set to be used to generate
     * the keys.
     * @param random the source of randomness for this generator.
     *
     * @exception InvalidAlgorithmParameterException if the given parameters
     * are inappropriate for this key pair generator
     */
    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        if (!(params instanceof DSAParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("Inappropriate parameter");
        }
        init((DSAParameterSpec)params, random, false);
    }

    void init(int modlen, SecureRandom random, boolean forceNew) {
        int subPrimeLen = getDefDSASubprimeSize(modlen);
        checkStrength(modlen, subPrimeLen);
        this.plen = modlen;
        this.qlen = subPrimeLen;
        this.params = null;
        this.random = random;
        this.forceNewParameters = forceNew;
    }

    void init(DSAParameterSpec params, SecureRandom random,
        boolean forceNew) {
        int sizeP = params.getP().bitLength();
        int sizeQ = params.getQ().bitLength();
        checkStrength(sizeP, sizeQ);
        this.plen = sizeP;
        this.qlen = sizeQ;
        this.params = params;
        this.random = random;
        this.forceNewParameters = forceNew;
    }

    /**
     * Generates a pair of keys usable by any JavaSecurity compliant
     * DSA implementation.
     */
    public KeyPair generateKeyPair() {
        if (random == null) {
            random = JCAUtil.getSecureRandom();
        }
        DSAParameterSpec spec;
        try {
            if (forceNewParameters) {
                // generate new parameters each time
                spec = ParameterCache.getNewDSAParameterSpec(plen, qlen, random);
            } else {
                if (params == null) {
                    params =
                        ParameterCache.getDSAParameterSpec(plen, qlen, random);
                }
                spec = params;
            }
        } catch (GeneralSecurityException e) {
            throw new ProviderException(e);
        }
        return generateKeyPair(spec.getP(), spec.getQ(), spec.getG(), random);
    }

    private KeyPair generateKeyPair(BigInteger p, BigInteger q, BigInteger g,
                                   SecureRandom random) {

        BigInteger x = generateX(random, q);
        BigInteger y = generateY(x, p, g);

        try {

            // See the comments in DSAKeyFactory, 4532506, and 6232513.

            DSAPublicKey pub;
            pub = new DSAPublicKeyImpl(y, p, q, g);
            DSAPrivateKey priv = new DSAPrivateKey(x, p, q, g);

            KeyPair pair = new KeyPair(pub, priv);
            return pair;
        } catch (InvalidKeyException e) {
            throw new ProviderException(e);
        }
    }

    /**
     * Generate the private key component of the key pair using the
     * provided source of random bits. This method uses the random but
     * source passed to generate a seed and then calls the seed-based
     * generateX method.
     */
    private BigInteger generateX(SecureRandom random, BigInteger q) {
        BigInteger x = null;
        byte[] temp = new byte[qlen];
        while (true) {
            random.nextBytes(temp);
            x = new BigInteger(1, temp).mod(q);
            if (x.signum() > 0 && (x.compareTo(q) < 0)) {
                return x;
            }
        }
    }

    /**
     * Generate the public key component y of the key pair.
     *
     * @param x the private key component.
     *
     * @param p the base parameter.
     */
    BigInteger generateY(BigInteger x, BigInteger p, BigInteger g) {
        BigInteger y = g.modPow(x, p);
        return y;
    }

    public static final class Current extends DSAKeyPairGenerator {
        public Current() {
            super(DEF_DSA_KEY_SIZE);
        }
    }

    public static final class Legacy extends DSAKeyPairGenerator
        implements java.security.interfaces.DSAKeyPairGenerator {

        public Legacy() {
            super(1024);
        }

        /**
         * Initializes the DSA key pair generator. If <code>genParams</code>
         * is false, a set of pre-computed parameters is used.
         */
        @Override
        public void initialize(int modlen, boolean genParams,
            SecureRandom random) throws InvalidParameterException {
            if (genParams) {
                super.init(modlen, random, true);
            } else {
                DSAParameterSpec cachedParams =
                    ParameterCache.getCachedDSAParameterSpec(modlen,
                        getDefDSASubprimeSize(modlen));
                if (cachedParams == null) {
                    throw new InvalidParameterException
                        ("No precomputed parameters for requested modulus" +
                         " size available");
                }
                super.init(cachedParams, random, false);
            }
        }

        /**
         * Initializes the DSA object using a DSA parameter object.
         *
         * @param params a fully initialized DSA parameter object.
         */
        @Override
        public void initialize(DSAParams params, SecureRandom random)
            throws InvalidParameterException {
            if (params == null) {
                throw new InvalidParameterException("Params must not be null");
             }
             DSAParameterSpec spec = new DSAParameterSpec
                 (params.getP(), params.getQ(), params.getG());
             super.init(spec, random, false);
        }
    }
}
