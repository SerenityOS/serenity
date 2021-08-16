/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AlgorithmParameterGeneratorSpi;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.DSAParameterSpec;
import java.security.spec.DSAGenParameterSpec;

import static sun.security.util.SecurityProviderConstants.DEF_DSA_KEY_SIZE;
import static sun.security.util.SecurityProviderConstants.getDefDSASubprimeSize;


/**
 * This class generates parameters for the DSA algorithm.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.AlgorithmParameters
 * @see java.security.spec.AlgorithmParameterSpec
 * @see DSAParameters
 *
 * @since 1.2
 */

public class DSAParameterGenerator extends AlgorithmParameterGeneratorSpi {

    // the length of prime P, subPrime Q, and seed in bits
    private int valueL = -1;
    private int valueN = -1;
    private int seedLen = -1;

    // the source of randomness
    private SecureRandom random;

    public DSAParameterGenerator() {
    }

    /**
     * Initializes this parameter generator for a certain strength
     * and source of randomness.
     *
     * @param strength the strength (size of prime) in bits
     * @param random the source of randomness
     */
    @Override
    protected void engineInit(int strength, SecureRandom random) {
        if ((strength != 2048) && (strength != 3072) &&
            ((strength < 512) || (strength > 1024) || (strength % 64 != 0))) {
            throw new InvalidParameterException(
                "Unexpected strength (size of prime): " + strength +
                ". Prime size should be 512-1024, 2048, or 3072");
        }
        this.valueL = strength;
        this.valueN = getDefDSASubprimeSize(strength);
        this.seedLen = valueN;
        this.random = random;
    }

    /**
     * Initializes this parameter generator with a set of
     * algorithm-specific parameter generation values.
     *
     * @param genParamSpec the set of algorithm-specific parameter
     *        generation values
     * @param random the source of randomness
     *
     * @exception InvalidAlgorithmParameterException if the given parameter
     * generation values are inappropriate for this parameter generator
     */
    @Override
    protected void engineInit(AlgorithmParameterSpec genParamSpec,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (!(genParamSpec instanceof DSAGenParameterSpec)) {
            throw new InvalidAlgorithmParameterException("Invalid parameter");
        }
        DSAGenParameterSpec dsaGenParams = (DSAGenParameterSpec)genParamSpec;

        // directly initialize using the already validated values
        this.valueL = dsaGenParams.getPrimePLength();
        this.valueN = dsaGenParams.getSubprimeQLength();
        this.seedLen = dsaGenParams.getSeedLength();
        this.random = random;
    }

    /**
     * Generates the parameters.
     *
     * @return the new AlgorithmParameters object
     */
    @Override
    protected AlgorithmParameters engineGenerateParameters() {
        AlgorithmParameters algParams = null;
        try {
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            if (valueL == -1) {
                engineInit(DEF_DSA_KEY_SIZE, this.random);
            }
            BigInteger[] pAndQ = generatePandQ(this.random, valueL,
                                               valueN, seedLen);
            BigInteger paramP = pAndQ[0];
            BigInteger paramQ = pAndQ[1];
            BigInteger paramG = generateG(paramP, paramQ);

            DSAParameterSpec dsaParamSpec =
                new DSAParameterSpec(paramP, paramQ, paramG);
            algParams = AlgorithmParameters.getInstance("DSA", "SUN");
            algParams.init(dsaParamSpec);
        } catch (InvalidParameterSpecException e) {
            // this should never happen
            throw new RuntimeException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            // this should never happen, because we provide it
            throw new RuntimeException(e.getMessage());
        } catch (NoSuchProviderException e) {
            // this should never happen, because we provide it
            throw new RuntimeException(e.getMessage());
        }

        return algParams;
    }

    /*
     * Generates the prime and subprime parameters for DSA,
     * using the provided source of randomness.
     * This method will generate new seeds until a suitable
     * seed has been found.
     *
     * @param random the source of randomness to generate the
     * seed
     * @param valueL the size of <code>p</code>, in bits.
     * @param valueN the size of <code>q</code>, in bits.
     * @param seedLen the length of <code>seed</code>, in bits.
     *
     * @return an array of BigInteger, with <code>p</code> at index 0 and
     * <code>q</code> at index 1, the seed at index 2, and the counter value
     * at index 3.
     */
    private static BigInteger[] generatePandQ(SecureRandom random, int valueL,
                                              int valueN, int seedLen) {
        String hashAlg = null;
        if (valueN == 160) {
            hashAlg = "SHA";
        } else if (valueN == 224) {
            hashAlg = "SHA-224";
        } else if (valueN == 256) {
            hashAlg = "SHA-256";
        }
        MessageDigest hashObj = null;
        try {
            hashObj = MessageDigest.getInstance(hashAlg);
        } catch (NoSuchAlgorithmException nsae) {
            // should never happen
            nsae.printStackTrace();
        }

        /* Step 3, 4: Useful variables */
        int outLen = hashObj.getDigestLength()*8;
        int n = (valueL - 1) / outLen;
        int b = (valueL - 1) % outLen;
        byte[] seedBytes = new byte[seedLen/8];
        BigInteger twoSl = BigInteger.TWO.pow(seedLen);
        int primeCertainty = -1;
        if (valueL <= 1024) {
            primeCertainty = 80;
        } else if (valueL == 2048) {
            primeCertainty = 112;
        } else if (valueL == 3072) {
            primeCertainty = 128;
        }
        if (primeCertainty < 0) {
            throw new ProviderException("Invalid valueL: " + valueL);
        }
        BigInteger resultP, resultQ, seed = null;
        int counter;
        while (true) {
            do {
                /* Step 5 */
                random.nextBytes(seedBytes);
                seed = new BigInteger(1, seedBytes);

                /* Step 6 */
                BigInteger U = new BigInteger(1, hashObj.digest(seedBytes)).
                    mod(BigInteger.TWO.pow(valueN - 1));

                /* Step 7 */
                resultQ = BigInteger.TWO.pow(valueN - 1)
                            .add(U)
                            .add(BigInteger.ONE)
                            .subtract(U.mod(BigInteger.TWO));
            } while (!resultQ.isProbablePrime(primeCertainty));

            /* Step 10 */
            BigInteger offset = BigInteger.ONE;
            /* Step 11 */
            for (counter = 0; counter < 4*valueL; counter++) {
                BigInteger[] V = new BigInteger[n + 1];
                /* Step 11.1 */
                for (int j = 0; j <= n; j++) {
                    BigInteger J = BigInteger.valueOf(j);
                    BigInteger tmp = (seed.add(offset).add(J)).mod(twoSl);
                    byte[] vjBytes = hashObj.digest(toByteArray(tmp));
                    V[j] = new BigInteger(1, vjBytes);
                }
                /* Step 11.2 */
                BigInteger W = V[0];
                for (int i = 1; i < n; i++) {
                    W = W.add(V[i].multiply(BigInteger.TWO.pow(i * outLen)));
                }
                W = W.add((V[n].mod(BigInteger.TWO.pow(b)))
                               .multiply(BigInteger.TWO.pow(n * outLen)));
                /* Step 11.3 */
                BigInteger twoLm1 = BigInteger.TWO.pow(valueL - 1);
                BigInteger X = W.add(twoLm1);
                /* Step 11.4, 11.5 */
                BigInteger c = X.mod(resultQ.multiply(BigInteger.TWO));
                resultP = X.subtract(c.subtract(BigInteger.ONE));
                /* Step 11.6, 11.7 */
                if (resultP.compareTo(twoLm1) > -1
                    && resultP.isProbablePrime(primeCertainty)) {
                    /* Step 11.8 */
                    BigInteger[] result = {resultP, resultQ, seed,
                                           BigInteger.valueOf(counter)};
                    return result;
                }
                /* Step 11.9 */
                offset = offset.add(BigInteger.valueOf(n)).add(BigInteger.ONE);
             }
        }

    }

    /*
     * Generates the <code>g</code> parameter for DSA.
     *
     * @param p the prime, <code>p</code>.
     * @param q the subprime, <code>q</code>.
     *
     * @param the <code>g</code>
     */
    private static BigInteger generateG(BigInteger p, BigInteger q) {
        BigInteger h = BigInteger.ONE;
        /* Step 1 */
        BigInteger pMinusOneOverQ = (p.subtract(BigInteger.ONE)).divide(q);
        BigInteger resultG = BigInteger.ONE;
        while (resultG.compareTo(BigInteger.TWO) < 0) {
            /* Step 3 */
            resultG = h.modPow(pMinusOneOverQ, p);
            h = h.add(BigInteger.ONE);
        }
        return resultG;
    }

    /*
     * Converts the result of a BigInteger.toByteArray call to an exact
     * signed magnitude representation for any positive number.
     */
    private static byte[] toByteArray(BigInteger bigInt) {
        byte[] result = bigInt.toByteArray();
        if (result[0] == 0) {
            byte[] tmp = new byte[result.length - 1];
            System.arraycopy(result, 1, tmp, 0, tmp.length);
            result = tmp;
        }
        return result;
    }
}
