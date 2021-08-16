/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.internal.spec;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.SecretKey;

/**
 * Parameters for the TLS PRF (pseudo-random function). The PRF function
 * is defined in RFC 2246.
 * This class is used to initialize KeyGenerators of the type "TlsPrf".
 *
 * <p>Instances of this class are immutable.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @deprecated Sun JDK internal use only --- WILL BE REMOVED in a future
 * release.
 */
@Deprecated
public class TlsPrfParameterSpec implements AlgorithmParameterSpec {

    private final SecretKey secret;
    private final String label;
    private final byte[] seed;
    private final int outputLength;
    private final String prfHashAlg;
    private final int prfHashLength;
    private final int prfBlockSize;

    /**
     * Constructs a new TlsPrfParameterSpec.
     *
     * @param secret the secret to use in the calculation (or null)
     * @param label the label to use in the calculation
     * @param seed the random seed to use in the calculation
     * @param outputLength the length in bytes of the output key to be produced
     * @param prfHashAlg the name of the TLS PRF hash algorithm to use.
     *        Used only for TLS 1.2+.  TLS1.1 and earlier use a fixed PRF.
     * @param prfHashLength the output length of the TLS PRF hash algorithm.
     *        Used only for TLS 1.2+.
     * @param prfBlockSize the input block size of the TLS PRF hash algorithm.
     *        Used only for TLS 1.2+.
     *
     * @throws NullPointerException if label or seed is null
     * @throws IllegalArgumentException if outputLength is negative
     */
    public TlsPrfParameterSpec(SecretKey secret, String label,
            byte[] seed, int outputLength,
            String prfHashAlg, int prfHashLength, int prfBlockSize) {
        if ((label == null) || (seed == null)) {
            throw new NullPointerException("label and seed must not be null");
        }
        if (outputLength <= 0) {
            throw new IllegalArgumentException("outputLength must be positive");
        }
        this.secret = secret;
        this.label = label;
        this.seed = seed.clone();
        this.outputLength = outputLength;
        this.prfHashAlg = prfHashAlg;
        this.prfHashLength = prfHashLength;
        this.prfBlockSize = prfBlockSize;
    }

    /**
     * Returns the secret to use in the PRF calculation, or null if there is no
     * secret.
     *
     * @return the secret to use in the PRF calculation, or null if there is no
     * secret.
     */
    public SecretKey getSecret() {
        return secret;
    }

    /**
     * Returns the label to use in the PRF calcuation.
     *
     * @return the label to use in the PRF calcuation.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns a copy of the seed to use in the PRF calcuation.
     *
     * @return a copy of the seed to use in the PRF calcuation.
     */
    public byte[] getSeed() {
        return seed.clone();
    }

    /**
     * Returns the length in bytes of the output key to be produced.
     *
     * @return the length in bytes of the output key to be produced.
     */
    public int getOutputLength() {
        return outputLength;
    }

    /**
     * Obtains the PRF hash algorithm to use in the PRF calculation.
     *
     * @return the hash algorithm, or null if no algorithm was specified.
     */
    public String getPRFHashAlg() {
        return prfHashAlg;
    }

    /**
     * Obtains the length of PRF hash algorithm.
     *
     * It would have been preferred to use MessageDigest.getDigestLength(),
     * but the API does not require implementations to support the method.
     *
     * @return the hash algorithm length.
     */
    public int getPRFHashLength() {
        return prfHashLength;
    }

    /**
     * Obtains the length of PRF hash algorithm.
     *
     * @return the hash algorithm length.
     */
    public int getPRFBlockSize() {
        return prfBlockSize;
    }
}
