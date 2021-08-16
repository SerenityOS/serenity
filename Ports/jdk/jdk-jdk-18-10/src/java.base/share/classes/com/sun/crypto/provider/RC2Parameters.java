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

package com.sun.crypto.provider;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.RC2ParameterSpec;
import sun.security.util.HexDumpEncoder;
import sun.security.util.*;

/**
 * This class implements the parameter set used with RC2
 * encryption, which is defined in RFC2268 as follows:
 *
 * <pre>
 * RC2-CBCParameter ::= CHOICE {
 *   iv IV,
 *   params SEQUENCE {
 *     version RC2Version,
 *     iv IV
 *   }
 * }
 *
 * where
 *
 * IV ::= OCTET STRING -- 8 octets
 * RC2Version ::= INTEGER -- 1-1024
 * </pre>
 *
 * @author Sean Mullan
 * @since 1.5
 */
public final class RC2Parameters extends AlgorithmParametersSpi {

    // TABLE[EKB] from section 6 of RFC 2268, used to convert effective key
    // size to/from encoded version number
    private static final int[] EKB_TABLE = new int[] {
        0xbd, 0x56, 0xea, 0xf2, 0xa2, 0xf1, 0xac, 0x2a,
        0xb0, 0x93, 0xd1, 0x9c, 0x1b, 0x33, 0xfd, 0xd0,
        0x30, 0x04, 0xb6, 0xdc, 0x7d, 0xdf, 0x32, 0x4b,
        0xf7, 0xcb, 0x45, 0x9b, 0x31, 0xbb, 0x21, 0x5a,
        0x41, 0x9f, 0xe1, 0xd9, 0x4a, 0x4d, 0x9e, 0xda,
        0xa0, 0x68, 0x2c, 0xc3, 0x27, 0x5f, 0x80, 0x36,
        0x3e, 0xee, 0xfb, 0x95, 0x1a, 0xfe, 0xce, 0xa8,
        0x34, 0xa9, 0x13, 0xf0, 0xa6, 0x3f, 0xd8, 0x0c,
        0x78, 0x24, 0xaf, 0x23, 0x52, 0xc1, 0x67, 0x17,
        0xf5, 0x66, 0x90, 0xe7, 0xe8, 0x07, 0xb8, 0x60,
        0x48, 0xe6, 0x1e, 0x53, 0xf3, 0x92, 0xa4, 0x72,
        0x8c, 0x08, 0x15, 0x6e, 0x86, 0x00, 0x84, 0xfa,
        0xf4, 0x7f, 0x8a, 0x42, 0x19, 0xf6, 0xdb, 0xcd,
        0x14, 0x8d, 0x50, 0x12, 0xba, 0x3c, 0x06, 0x4e,
        0xec, 0xb3, 0x35, 0x11, 0xa1, 0x88, 0x8e, 0x2b,
        0x94, 0x99, 0xb7, 0x71, 0x74, 0xd3, 0xe4, 0xbf,
        0x3a, 0xde, 0x96, 0x0e, 0xbc, 0x0a, 0xed, 0x77,
        0xfc, 0x37, 0x6b, 0x03, 0x79, 0x89, 0x62, 0xc6,
        0xd7, 0xc0, 0xd2, 0x7c, 0x6a, 0x8b, 0x22, 0xa3,
        0x5b, 0x05, 0x5d, 0x02, 0x75, 0xd5, 0x61, 0xe3,
        0x18, 0x8f, 0x55, 0x51, 0xad, 0x1f, 0x0b, 0x5e,
        0x85, 0xe5, 0xc2, 0x57, 0x63, 0xca, 0x3d, 0x6c,
        0xb4, 0xc5, 0xcc, 0x70, 0xb2, 0x91, 0x59, 0x0d,
        0x47, 0x20, 0xc8, 0x4f, 0x58, 0xe0, 0x01, 0xe2,
        0x16, 0x38, 0xc4, 0x6f, 0x3b, 0x0f, 0x65, 0x46,
        0xbe, 0x7e, 0x2d, 0x7b, 0x82, 0xf9, 0x40, 0xb5,
        0x1d, 0x73, 0xf8, 0xeb, 0x26, 0xc7, 0x87, 0x97,
        0x25, 0x54, 0xb1, 0x28, 0xaa, 0x98, 0x9d, 0xa5,
        0x64, 0x6d, 0x7a, 0xd4, 0x10, 0x81, 0x44, 0xef,
        0x49, 0xd6, 0xae, 0x2e, 0xdd, 0x76, 0x5c, 0x2f,
        0xa7, 0x1c, 0xc9, 0x09, 0x69, 0x9a, 0x83, 0xcf,
        0x29, 0x39, 0xb9, 0xe9, 0x4c, 0xff, 0x43, 0xab
    };

    // the iv
    private byte[] iv;

    // the version number
    private int version = 0;

    // the effective key size
    private int effectiveKeySize = 0;

    public RC2Parameters() {}

    protected void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException {

        if (!(paramSpec instanceof RC2ParameterSpec)) {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
        RC2ParameterSpec rps = (RC2ParameterSpec) paramSpec;

        // check effective key size (a value of 0 means it is unspecified)
        effectiveKeySize = rps.getEffectiveKeyBits();
        if (effectiveKeySize != 0) {
            if (effectiveKeySize < 1 || effectiveKeySize > 1024) {
                throw new InvalidParameterSpecException("RC2 effective key " +
                    "size must be between 1 and 1024 bits");
            }
            if (effectiveKeySize < 256) {
                version = EKB_TABLE[effectiveKeySize];
            } else {
                version = effectiveKeySize;
            }
        }
        this.iv = rps.getIV();
    }

    protected void engineInit(byte[] encoded) throws IOException {
        DerValue val = new DerValue(encoded);
        // check if IV or params
        if (val.tag == DerValue.tag_Sequence) {
            val.data.reset();

            version = val.data.getInteger();
            if (version < 0 || version > 1024) {
                throw new IOException("RC2 parameter parsing error: " +
                    "version number out of legal range (0-1024): " + version);
            }
            if (version > 255) {
                effectiveKeySize = version;
            } else {
                // search table for index containing version
                for (int i = 0; i < EKB_TABLE.length; i++) {
                    if (version == EKB_TABLE[i]) {
                        effectiveKeySize = i;
                        break;
                    }
                }
            }

            iv = val.data.getOctetString();
        } else {
            val.data.reset();
            iv = val.getOctetString();
            version = 0;
            effectiveKeySize = 0;
        }

        if (iv.length != 8) {
            throw new IOException("RC2 parameter parsing error: iv length " +
                "must be 8 bits, actual: " + iv.length);
        }

        if (val.data.available() != 0) {
            throw new IOException("RC2 parameter parsing error: extra data");
        }
    }

    protected void engineInit(byte[] encoded, String decodingMethod)
        throws IOException {
        engineInit(encoded);
    }

    protected <T extends AlgorithmParameterSpec>
            T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException {

        if (RC2ParameterSpec.class.isAssignableFrom(paramSpec)) {
            return paramSpec.cast((iv == null ?
                                   new RC2ParameterSpec(effectiveKeySize) :
                                   new RC2ParameterSpec(effectiveKeySize, iv)));
        } else {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
    }

    protected byte[] engineGetEncoded() throws IOException {
        DerOutputStream out = new DerOutputStream();
        DerOutputStream bytes = new DerOutputStream();

        if (this.effectiveKeySize != 0) {
            bytes.putInteger(version);
            bytes.putOctetString(iv);
            out.write(DerValue.tag_Sequence, bytes);
        } else {
            out.putOctetString(iv);
        }

        return out.toByteArray();
    }

    protected byte[] engineGetEncoded(String encodingMethod)
        throws IOException {
        return engineGetEncoded();
    }

    /*
     * Returns a formatted string describing the parameters.
     */
    protected String engineToString() {
        String LINE_SEP = System.lineSeparator();
        HexDumpEncoder encoder = new HexDumpEncoder();
        StringBuilder sb
            = new StringBuilder(LINE_SEP + "    iv:" + LINE_SEP + "["
                + encoder.encodeBuffer(iv) + "]");

        if (version != 0) {
            sb.append(LINE_SEP + "version:" + LINE_SEP
                + version + LINE_SEP);
        }
        return sb.toString();
    }
}
