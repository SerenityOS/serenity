/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.crypto.spec.IvParameterSpec;
import sun.security.util.*;

/**
 * This class implements the parameter set used with the ChaCha20-Poly1305
 * algorithm.  The parameter definition comes from
 * <a href="https://tools.ietf.org/html/rfc8103"><i>RFC 8103</i></a>
 * and is defined according to the following ASN.1:
 *
 * <pre>
 * id-alg-AEADChaCha20Poly1305 OBJECT IDENTIFIER ::=
 *        { iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1)
 *          pkcs9(9) smime(16) alg(3) 18 }
 *
 * AEADChaCha20Poly1305Nonce ::= OCTET STRING (SIZE(12))
 * </pre>
 *
 * The AlgorithmParameters may be instantiated either by its name
 * ("ChaCha20-Poly1305") or via its OID (1.2.840.113549.1.9.16.3.18)
 */
public final class ChaCha20Poly1305Parameters extends AlgorithmParametersSpi {

    private static final String DEFAULT_FMT = "ASN.1";
    private byte[] nonce;

    public ChaCha20Poly1305Parameters() {}

    /**
     * Initialize the ChaCha20Poly1305Parameters using an IvParameterSpec.
     *
     * @param paramSpec the {@code IvParameterSpec} used to configure
     *      this object.
     *
     * @throws InvalidParameterSpecException if an object of a type other
     *      than {@code IvParameterSpec} is used.
     */
    @Override
    protected void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException {

        if (!(paramSpec instanceof IvParameterSpec)) {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
        IvParameterSpec ivps = (IvParameterSpec)paramSpec;

        // Obtain the nonce
        nonce = ivps.getIV();
        if (nonce.length != 12) {
            throw new InvalidParameterSpecException("ChaCha20-Poly1305 nonce" +
                    " must be 12 bytes in length");
        }
    }

    /**
     * Initialize the ChaCha20Poly1305Parameters from a DER encoded
     * parameter block.
     *
     * @param encoded the DER encoding of the nonce as an OCTET STRING.
     *
     * @throws IOException if the encoded nonce is not 12 bytes long or a DER
     *      decoding error occurs.
     */
    @Override
    protected void engineInit(byte[] encoded) throws IOException {
        DerValue val = new DerValue(encoded);

        // Get the nonce value
        nonce = val.getOctetString();
        if (nonce.length != 12) {
           throw new IOException(
                   "ChaCha20-Poly1305 nonce must be 12 bytes in length");
        }
    }

    /**
     * Initialize the ChaCha20Poly1305Parameters from a DER encoded
     * parameter block.
     *
     * @param encoded the DER encoding of the nonce and initial block counter.
     * @param decodingMethod the decoding method.  The only currently accepted
     *      value is "ASN.1"
     *
     * @throws IOException if the encoded nonce is not 12 bytes long, a DER
     *      decoding error occurs, or an unsupported decoding method is
     *      provided.
     */
    @Override
    protected void engineInit(byte[] encoded, String decodingMethod)
            throws IOException {
        if (decodingMethod == null ||
                decodingMethod.equalsIgnoreCase(DEFAULT_FMT)) {
            engineInit(encoded);
        } else {
            throw new IOException("Unsupported parameter format: " +
                    decodingMethod);
        }
    }

    /**
     * Return an IvParameterSpec with the same parameters as those
     * held in this object.
     *
     * @param paramSpec the class name of the spec.  In this case it should
     *      be {@code IvParameterSpec.class}.
     *
     * @return a {@code IvParameterSpec} object containing the nonce
     *      value held in this object.
     *
     * @throws InvalidParameterSpecException if a class other than
     *      {@code IvParameterSpec.class} was specified in the paramSpec
     *      parameter.
     */
    @Override
    protected <T extends AlgorithmParameterSpec>
            T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException {

        if (IvParameterSpec.class.isAssignableFrom(paramSpec)) {
            return paramSpec.cast(new IvParameterSpec(nonce));
        } else {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
    }

    /**
     * Return the encoded parameters in ASN.1 form.
     *
     * @return a byte array containing the DER-encoding for the
     *      ChaCha20-Poly1305 parameters.  This will be the nonce
     *      encoded as a DER OCTET STRING.
     *
     * @throws IOException if any DER encoding error occurs.
     */
    @Override
    protected byte[] engineGetEncoded() throws IOException {
        DerOutputStream out = new DerOutputStream();
        out.write(DerValue.tag_OctetString, nonce);
        return out.toByteArray();
    }

    /**
     * Return the encoded parameters in ASN.1 form.
     *
     * @param encodingMethod the encoding method to be used.  This parameter
     *      must be "ASN.1" as it is the only currently supported encoding
     *      format.  If the parameter is {@code null} then the default
     *      encoding format will be used.
     *
     * @return a byte array containing the DER-encoding for the
     *      ChaCha20-Poly1305 parameters.
     *
     * @throws IOException if any DER encoding error occurs or an unsupported
     *      encoding method is provided.
     */
    @Override
    protected byte[] engineGetEncoded(String encodingMethod)
        throws IOException {
        if (encodingMethod == null ||
                encodingMethod.equalsIgnoreCase(DEFAULT_FMT)) {
            return engineGetEncoded();
        } else {
            throw new IOException("Unsupported encoding format: " +
                    encodingMethod);
        }
    }

    /**
     * Creates a formatted string describing the parameters.
     *
     * @return a string representation of the ChaCha20 parameters.
     */
    @Override
    protected String engineToString() {
        String LINE_SEP = System.lineSeparator();
        HexDumpEncoder encoder = new HexDumpEncoder();
        StringBuilder sb = new StringBuilder(LINE_SEP + "nonce:" +
                LINE_SEP + "[" + encoder.encodeBuffer(nonce) + "]");
        return sb.toString();
    }
}
