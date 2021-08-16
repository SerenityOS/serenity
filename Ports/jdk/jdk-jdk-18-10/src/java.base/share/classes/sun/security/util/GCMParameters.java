/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.GCMParameterSpec;
import sun.security.util.HexDumpEncoder;
import sun.security.util.*;

/**
 * This class implements the parameter set used with
 * GCM encryption, which is defined in RFC 5084 as follows:
 *
 * <pre>
 *    GCMParameters ::= SEQUENCE {
 *      aes-iv      OCTET STRING, -- recommended size is 12 octets
 *      aes-tLen    AES-GCM-ICVlen DEFAULT 12 }
 *
 *    AES-GCM-ICVlen ::= INTEGER (12 | 13 | 14 | 15 | 16)
 *
 * </pre>
 *
 * @since 13
 */
public final class GCMParameters extends AlgorithmParametersSpi {

    // the iv
    private byte[] iv;
    // the tag length in bytes
    private int tLen;

    public GCMParameters() {}

    protected void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException {

        if (!(paramSpec instanceof GCMParameterSpec)) {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
        GCMParameterSpec gps = (GCMParameterSpec) paramSpec;
        // need to convert from bits to bytes for ASN.1 encoding
        this.tLen = gps.getTLen()/8;
        if (this.tLen < 12 || this.tLen > 16 ) {
            throw new InvalidParameterSpecException
                ("GCM parameter parsing error: unsupported tag len: " +
                 this.tLen);
        }
        this.iv = gps.getIV();
    }

    protected void engineInit(byte[] encoded) throws IOException {
        DerValue val = new DerValue(encoded);
        // check if IV or params
        if (val.tag == DerValue.tag_Sequence) {
            byte[] iv = val.data.getOctetString();
            int tLen;
            if (val.data.available() != 0) {
                tLen = val.data.getInteger();
                if (tLen < 12 || tLen > 16 ) {
                    throw new IOException
                        ("GCM parameter parsing error: unsupported tag len: " +
                         tLen);
                }
                if (val.data.available() != 0) {
                    throw new IOException
                        ("GCM parameter parsing error: extra data");
                }
            } else {
                tLen = 12;
            }
            this.iv = iv.clone();
            this.tLen = tLen;
        } else {
            throw new IOException("GCM parameter parsing error: no SEQ tag");
        }
    }

    protected void engineInit(byte[] encoded, String decodingMethod)
        throws IOException {
        engineInit(encoded);
    }

    protected <T extends AlgorithmParameterSpec>
            T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException {

        if (GCMParameterSpec.class.isAssignableFrom(paramSpec)) {
            return paramSpec.cast(new GCMParameterSpec(tLen * 8, iv));
        } else {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
    }

    protected byte[] engineGetEncoded() throws IOException {
        DerOutputStream out = new DerOutputStream();
        DerOutputStream bytes = new DerOutputStream();

        bytes.putOctetString(iv);
        // Only put non-default values
        if (tLen != 12) {
            bytes.putInteger(tLen);
        }
        out.write(DerValue.tag_Sequence, bytes);
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

        sb.append(LINE_SEP + "tLen(bits):" + LINE_SEP + tLen*8 + LINE_SEP);
        return sb.toString();
    }
}
