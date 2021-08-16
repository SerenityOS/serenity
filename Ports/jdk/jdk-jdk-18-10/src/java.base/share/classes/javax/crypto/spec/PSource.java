/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto.spec;

/**
 * This class specifies the source for encoding input P in OAEP Padding,
 * as defined in the
 * <a href="https://tools.ietf.org/rfc/rfc8017.txt">PKCS#1 v2.2</a> standard.
 * <pre>
 * PSourceAlgorithm ::= AlgorithmIdentifier {
 *   {PKCS1PSourceAlgorithms}
 * }
 * </pre>
 * where
 * <pre>
 * PKCS1PSourceAlgorithms    ALGORITHM-IDENTIFIER ::= {
 *   { OID id-pSpecified PARAMETERS EncodingParameters },
 *   ...  -- Allows for future expansion --
 * }
 * EncodingParameters ::= OCTET STRING(SIZE(0..MAX))
 * </pre>
 * @author Valerie Peng
 *
 * @since 1.5
 */
public class PSource {

    private String pSrcName;

    /**
     * Constructs a source of the encoding input P for OAEP
     * padding as defined in the PKCS #1 standard using the
     * specified PSource algorithm.
     * @param pSrcName the algorithm for the source of the
     * encoding input P.
     * @exception NullPointerException if <code>pSrcName</code>
     * is null.
     */
    protected PSource(String pSrcName) {
        if (pSrcName == null) {
            throw new NullPointerException("pSource algorithm is null");
        }
        this.pSrcName = pSrcName;
    }
    /**
     * Returns the PSource algorithm name.
     *
     * @return the PSource algorithm name.
     */
    public String getAlgorithm() {
        return pSrcName;
    }

    /**
     * This class is used to explicitly specify the value for
     * encoding input P in OAEP Padding.
     *
     * @since 1.5
     */
    public static final class PSpecified extends PSource {

        private byte[] p = new byte[0];

        /**
         * The encoding input P whose value equals byte[0].
         */
        public static final PSpecified DEFAULT = new PSpecified(new byte[0]);

        /**
         * Constructs the source explicitly with the specified
         * value <code>p</code> as the encoding input P.
         * Note:
         * @param p the value of the encoding input. The contents
         * of the array are copied to protect against subsequent
         * modification.
         * @exception NullPointerException if <code>p</code> is null.
         */
        public PSpecified(byte[] p) {
            super("PSpecified");
            this.p = p.clone();
        }
        /**
         * Returns the value of encoding input P.
         * @return the value of encoding input P. A new array is
         * returned each time this method is called.
         */
        public byte[] getValue() {
            return (p.length==0? p: p.clone());
        }
    }
}
