/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.security.spec;

/**
 * This immutable class specifies the set of parameters used for
 * generating DSA parameters as specified in
 * <a href="http://csrc.nist.gov/publications/fips/fips186-3/fips_186-3.pdf">FIPS 186-3 Digital Signature Standard (DSS)</a>.
 *
 * @see AlgorithmParameterSpec
 *
 * @since 1.8
 */
public final class DSAGenParameterSpec implements AlgorithmParameterSpec {

    private final int pLen;
    private final int qLen;
    private final int seedLen;

    /**
     * Creates a domain parameter specification for DSA parameter
     * generation using {@code primePLen} and {@code subprimeQLen}.
     * The value of {@code subprimeQLen} is also used as the default
     * length of the domain parameter seed in bits.
     * @param primePLen the desired length of the prime P in bits.
     * @param subprimeQLen the desired length of the sub-prime Q in bits.
     * @throws    IllegalArgumentException if {@code primePLen}
     * or {@code subprimeQLen} is illegal per the specification of
     * FIPS 186-3.
     */
    public DSAGenParameterSpec(int primePLen, int subprimeQLen) {
        this(primePLen, subprimeQLen, subprimeQLen);
    }

    /**
     * Creates a domain parameter specification for DSA parameter
     * generation using {@code primePLen}, {@code subprimeQLen},
     * and {@code seedLen}.
     * @param primePLen the desired length of the prime P in bits.
     * @param subprimeQLen the desired length of the sub-prime Q in bits.
     * @param seedLen the desired length of the domain parameter seed in bits,
     * shall be equal to or greater than {@code subprimeQLen}.
     * @throws    IllegalArgumentException if {@code primePLenLen},
     * {@code subprimeQLen}, or {@code seedLen} is illegal per the
     * specification of FIPS 186-3.
     */
    public DSAGenParameterSpec(int primePLen, int subprimeQLen, int seedLen) {
        switch (primePLen) {
        case 1024:
            if (subprimeQLen != 160) {
                throw new IllegalArgumentException
                    ("subprimeQLen must be 160 when primePLen=1024");
            }
            break;
        case 2048:
            if (subprimeQLen != 224 && subprimeQLen != 256) {
               throw new IllegalArgumentException
                   ("subprimeQLen must be 224 or 256 when primePLen=2048");
            }
            break;
        case 3072:
            if (subprimeQLen != 256) {
                throw new IllegalArgumentException
                    ("subprimeQLen must be 256 when primePLen=3072");
            }
            break;
        default:
            throw new IllegalArgumentException
                ("primePLen must be 1024, 2048, or 3072");
        }
        if (seedLen < subprimeQLen) {
            throw new IllegalArgumentException
                ("seedLen must be equal to or greater than subprimeQLen");
        }
        this.pLen = primePLen;
        this.qLen = subprimeQLen;
        this.seedLen = seedLen;
    }

    /**
     * Returns the desired length of the prime P of the
     * to-be-generated DSA domain parameters in bits.
     * @return the length of the prime P.
     */
    public int getPrimePLength() {
        return pLen;
    }

    /**
     * Returns the desired length of the sub-prime Q of the
     * to-be-generated DSA domain parameters in bits.
     * @return the length of the sub-prime Q.
     */
    public int getSubprimeQLength() {
        return qLen;
    }

    /**
     * Returns the desired length of the domain parameter seed in bits.
     * @return the length of the domain parameter seed.
     */
    public int getSeedLength() {
        return seedLen;
    }
}
