/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.math.BigInteger;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.PBEParameterSpec;
import sun.security.util.HexDumpEncoder;
import sun.security.util.*;


/**
 * This class implements the parameter set used with password-based
 * encryption, which is defined in PKCS#5 as follows:
 *
 * <pre>
 * PBEParameter ::=  SEQUENCE {
 *     salt   OCTET STRING SIZE(8),
 *     iterationCount   INTEGER }
 * </pre>
 *
 * @author Jan Luehe
 *
 */

public final class PBEParameters extends AlgorithmParametersSpi {

    // the salt
    private byte[] salt = null;

    // the iteration count
    private int iCount = 0;

    // the cipher parameter
    private AlgorithmParameterSpec cipherParam = null;

    protected void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException
   {
       if (!(paramSpec instanceof PBEParameterSpec)) {
           throw new InvalidParameterSpecException
               ("Inappropriate parameter specification");
       }
       this.salt = ((PBEParameterSpec)paramSpec).getSalt().clone();
       this.iCount = ((PBEParameterSpec)paramSpec).getIterationCount();
       this.cipherParam = ((PBEParameterSpec)paramSpec).getParameterSpec();
    }

    protected void engineInit(byte[] encoded)
        throws IOException
    {
        try {
            DerValue val = new DerValue(encoded);
            if (val.tag != DerValue.tag_Sequence) {
                throw new IOException("PBE parameter parsing error: "
                                      + "not a sequence");
            }
            val.data.reset();

            this.salt = val.data.getOctetString();
            this.iCount = val.data.getInteger();

            if (val.data.available() != 0) {
                throw new IOException
                    ("PBE parameter parsing error: extra data");
            }
        } catch (NumberFormatException e) {
            throw new IOException("iteration count too big");
        }
    }

    protected void engineInit(byte[] encoded, String decodingMethod)
        throws IOException
    {
        engineInit(encoded);
    }

    protected <T extends AlgorithmParameterSpec>
            T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException
    {
        if (PBEParameterSpec.class.isAssignableFrom(paramSpec)) {
            return paramSpec.cast(
                new PBEParameterSpec(this.salt, this.iCount, this.cipherParam));
        } else {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
    }

    protected byte[] engineGetEncoded() throws IOException {
        DerOutputStream out = new DerOutputStream();
        DerOutputStream bytes = new DerOutputStream();

        bytes.putOctetString(this.salt);
        bytes.putInteger(this.iCount);

        out.write(DerValue.tag_Sequence, bytes);
        return out.toByteArray();
    }

    protected byte[] engineGetEncoded(String encodingMethod)
        throws IOException
    {
        return engineGetEncoded();
    }

    /*
     * Returns a formatted string describing the parameters.
     */
    protected String engineToString() {
        String LINE_SEP = System.lineSeparator();
        String saltString = LINE_SEP + "    salt:" + LINE_SEP + "[";
        HexDumpEncoder encoder = new HexDumpEncoder();
        saltString += encoder.encodeBuffer(salt);
        saltString += "]";

        return saltString + LINE_SEP + "    iterationCount:"
            + LINE_SEP + Debug.toHexString(BigInteger.valueOf(iCount))
            + LINE_SEP;
    }
}
