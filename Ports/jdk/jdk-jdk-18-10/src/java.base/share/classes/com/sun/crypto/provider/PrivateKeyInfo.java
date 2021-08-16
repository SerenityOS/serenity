/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.math.*;
import java.io.*;
import java.util.Arrays;

import sun.security.x509.AlgorithmId;
import sun.security.util.*;


/**
 * This class implements the <code>PrivateKeyInfo</code> type,
 * which is defined in PKCS #8 as follows:
 *
 * <pre>
 * PrivateKeyInfo ::=  SEQUENCE {
 *     version   INTEGER,
 *     privateKeyAlgorithm   AlgorithmIdentifier,
 *     privateKey   OCTET STRING,
 *     attributes   [0] IMPLICIT Attributes OPTIONAL }
 * </pre>
 *
 * @author Jan Luehe
 */
final class PrivateKeyInfo {

    // the version number defined by the PKCS #8 standard
    private static final BigInteger VERSION = BigInteger.ZERO;

    // the private-key algorithm
    private AlgorithmId algid;

    // the private-key value
    private byte[] privkey;

    /**
     * Constructs a PKCS#8 PrivateKeyInfo from its ASN.1 encoding.
     */
    PrivateKeyInfo(byte[] encoded) throws IOException {
        DerValue val = new DerValue(encoded);

        try {
            if (val.tag != DerValue.tag_Sequence)
                throw new IOException("private key parse error: not a sequence");

            // version
            BigInteger parsedVersion = val.data.getBigInteger();
            if (!parsedVersion.equals(VERSION)) {
                throw new IOException("version mismatch: (supported: " +
                        VERSION + ", parsed: " + parsedVersion);
            }

            // privateKeyAlgorithm
            this.algid = AlgorithmId.parse(val.data.getDerValue());

            // privateKey
            this.privkey = val.data.getOctetString();

            // OPTIONAL attributes not supported yet
        } finally {
            val.clear();
        }
    }

    /**
     * Returns the private-key algorithm.
     */
    AlgorithmId getAlgorithm() {
        return this.algid;
    }

    public void clear() {
        Arrays.fill(privkey, (byte)0);
    }
}
