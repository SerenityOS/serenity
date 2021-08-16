/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * This class specifies the set of parameters used with mask generation
 * function MGF1 in OAEP Padding and RSASSA-PSS signature scheme, as
 * defined in the
 * <a href="https://tools.ietf.org/rfc/rfc8017.txt">PKCS#1 v2.2</a> standard.
 *
 * <p>Its ASN.1 definition in PKCS#1 standard is described below:
 * <pre>
 * PKCS1MGFAlgorithms    ALGORITHM-IDENTIFIER ::= {
 *   { OID id-mgf1 PARAMETERS HashAlgorithm },
 *   ...  -- Allows for future expansion --
 * }
 * </pre>
 * where
 * <pre>
 * HashAlgorithm ::= AlgorithmIdentifier {
 *   {OAEP-PSSDigestAlgorithms}
 * }
 *
 * OAEP-PSSDigestAlgorithms    ALGORITHM-IDENTIFIER ::= {
 *   { OID id-sha1       PARAMETERS NULL }|
 *   { OID id-sha224     PARAMETERS NULL }|
 *   { OID id-sha256     PARAMETERS NULL }|
 *   { OID id-sha384     PARAMETERS NULL }|
 *   { OID id-sha512     PARAMETERS NULL }|
 *   { OID id-sha512-224 PARAMETERS NULL }|
 *   { OID id-sha512-256 PARAMETERS NULL },
 *   ...  -- Allows for future expansion --
 * }
 * </pre>
 * @see PSSParameterSpec
 * @see javax.crypto.spec.OAEPParameterSpec
 *
 * @author Valerie Peng
 *
 * @since 1.5
 */
public class MGF1ParameterSpec implements AlgorithmParameterSpec {

    /**
     * The MGF1ParameterSpec which uses "SHA-1" message digest
     */
    public static final MGF1ParameterSpec SHA1 =
        new MGF1ParameterSpec("SHA-1");

    /**
     * The MGF1ParameterSpec which uses "SHA-224" message digest
     */
    public static final MGF1ParameterSpec SHA224 =
        new MGF1ParameterSpec("SHA-224");

    /**
     * The MGF1ParameterSpec which uses "SHA-256" message digest
     */
    public static final MGF1ParameterSpec SHA256 =
        new MGF1ParameterSpec("SHA-256");

    /**
     * The MGF1ParameterSpec which uses "SHA-384" message digest
     */
    public static final MGF1ParameterSpec SHA384 =
        new MGF1ParameterSpec("SHA-384");

    /**
     * The MGF1ParameterSpec which uses SHA-512 message digest
     */
    public static final MGF1ParameterSpec SHA512 =
        new MGF1ParameterSpec("SHA-512");

    /**
     * The MGF1ParameterSpec which uses SHA-512/224 message digest
     */
    public static final MGF1ParameterSpec SHA512_224 =
        new MGF1ParameterSpec("SHA-512/224");

    /**
     * The MGF1ParameterSpec which uses SHA-512/256 message digest
     */
    public static final MGF1ParameterSpec SHA512_256 =
        new MGF1ParameterSpec("SHA-512/256");

    /**
     * The MGF1ParameterSpec which uses SHA3-224 message digest
     * @since 16
     */
    public static final MGF1ParameterSpec SHA3_224 =
        new MGF1ParameterSpec("SHA3-224");

    /**
     * The MGF1ParameterSpec which uses SHA3-256 message digest
     * @since 16
     */
    public static final MGF1ParameterSpec SHA3_256 =
        new MGF1ParameterSpec("SHA3-256");

    /**
     * The MGF1ParameterSpec which uses SHA3-384 message digest
     * @since 16
     */
    public static final MGF1ParameterSpec SHA3_384 =
        new MGF1ParameterSpec("SHA3-384");

    /**
     * The MGF1ParameterSpec which uses SHA3-512 message digest
     * @since 16
     */
    public static final MGF1ParameterSpec SHA3_512 =
        new MGF1ParameterSpec("SHA3-512");

    private String mdName;

    /**
     * Constructs a parameter set for mask generation function MGF1
     * as defined in the PKCS #1 standard.
     *
     * @param mdName the algorithm name for the message digest
     * used in this mask generation function MGF1.
     * @throws    NullPointerException if {@code mdName} is null.
     */
    public MGF1ParameterSpec(String mdName) {
        if (mdName == null) {
            throw new NullPointerException("digest algorithm is null");
        }
        this.mdName = mdName;
    }

    /**
     * Returns the algorithm name of the message digest used by the mask
     * generation function.
     *
     * @return the algorithm name of the message digest.
     */
    public String getDigestAlgorithm() {
        return mdName;
    }

    @Override
    public String toString() {
        return "MGF1ParameterSpec[hashAlgorithm=" + mdName + "]";
    }
}
