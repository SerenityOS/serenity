/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.Arrays;
import sun.security.util.*;
import sun.security.util.HexDumpEncoder;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.IvParameterSpec;

/**
 * This class implements the parameter (IV) used with Block Ciphers
 * in feedback-mode. IV is defined in the standards as follows:
 *
 * <pre>
 * IV ::= OCTET STRING  -- length depends on the block size of the
 * block ciphers
 * </pre>
 *
 * @author Valerie Peng
 *
 */
final class BlockCipherParamsCore {
    private int block_size = 0;
    private byte[] iv = null;

    private int[] moreSizes = null;

    BlockCipherParamsCore(int blksize, int... moreSizes) {
        block_size = blksize;
        this.moreSizes = moreSizes;
    }

    void init(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException {
        if (!(paramSpec instanceof IvParameterSpec)) {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
        byte[] tmpIv = ((IvParameterSpec)paramSpec).getIV();
        boolean check = (tmpIv.length == block_size);
        if (!check && moreSizes != null) {
            for (int s : moreSizes) {
                if (tmpIv.length == s) {
                    check = true;
                    break;
                }
            }
        }
        if (!check) {
            String expectedLen = block_size + (moreSizes == null? "" :
                Arrays.toString(moreSizes));
            throw new InvalidParameterSpecException("IV length not " +
                        expectedLen + " bytes long");
        }
        iv = tmpIv.clone();
    }

    void init(byte[] encoded) throws IOException {
        DerInputStream der = new DerInputStream(encoded);

        byte[] tmpIv = der.getOctetString();
        if (der.available() != 0) {
            throw new IOException("IV parsing error: extra data");
        }
        boolean check = (tmpIv.length == block_size);
        if (!check) {
            String expectedLen = block_size + (moreSizes == null? "" :
                Arrays.toString(moreSizes));
            throw new IOException("IV not " + expectedLen +
                " bytes long");
        }
        iv = tmpIv;
    }

    void init(byte[] encoded, String decodingMethod) throws IOException {
        if ((decodingMethod != null) &&
            (!decodingMethod.equalsIgnoreCase("ASN.1"))) {
            throw new IllegalArgumentException("Only support ASN.1 format");
        }
        init(encoded);
    }

    <T extends AlgorithmParameterSpec> T getParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException {
        if (IvParameterSpec.class.isAssignableFrom(paramSpec)) {
            return paramSpec.cast(new IvParameterSpec(this.iv));
        } else {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
    }

    byte[] getEncoded() throws IOException {
        DerOutputStream out = new DerOutputStream();
        out.putOctetString(this.iv);
        return out.toByteArray();
    }

    byte[] getEncoded(String encodingMethod)
        throws IOException {
        return getEncoded();
    }

    /*
     * Returns a formatted string describing the parameters.
     */
    public String toString() {
        String LINE_SEP = System.lineSeparator();

        String ivString = LINE_SEP + "    iv:" + LINE_SEP + "[";
        HexDumpEncoder encoder = new HexDumpEncoder();
        ivString += encoder.encodeBuffer(this.iv);
        ivString += "]" + LINE_SEP;
        return ivString;
    }
}
