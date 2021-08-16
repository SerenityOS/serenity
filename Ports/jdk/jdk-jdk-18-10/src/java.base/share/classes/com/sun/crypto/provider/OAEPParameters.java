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

package com.sun.crypto.provider;

import java.math.BigInteger;
import java.io.*;
import sun.security.util.*;
import sun.security.x509.*;
import java.security.AlgorithmParametersSpi;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.OAEPParameterSpec;

/**
 * This class implements the OAEP parameters used with the RSA
 * algorithm in OAEP padding. Here is its ASN.1 definition:
 * RSAES-OAEP-params ::= SEQUENCE {
 *   hashAlgorithm      [0] HashAlgorithm     DEFAULT sha1,
 *   maskGenAlgorithm   [1] MaskGenAlgorithm  DEFAULT mgf1SHA1,
 *   pSourceAlgorithm   [2] PSourceAlgorithm  DEFAULT pSpecifiedEmpty
 * }
 *
 * @author Valerie Peng
 *
 */

public final class OAEPParameters extends AlgorithmParametersSpi {

    private String mdName;
    private MGF1ParameterSpec mgfSpec;
    private byte[] p;
    private static ObjectIdentifier OID_MGF1 =
            ObjectIdentifier.of(KnownOIDs.MGF1);
    private static ObjectIdentifier OID_PSpecified =
            ObjectIdentifier.of(KnownOIDs.PSpecified);

    public OAEPParameters() {
    }

    protected void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException {
        if (!(paramSpec instanceof OAEPParameterSpec)) {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
        OAEPParameterSpec spec = (OAEPParameterSpec) paramSpec;
        mdName = spec.getDigestAlgorithm();
        String mgfName = spec.getMGFAlgorithm();
        if (!mgfName.equalsIgnoreCase("MGF1")) {
            throw new InvalidParameterSpecException("Unsupported mgf " +
                mgfName + "; MGF1 only");
        }
        AlgorithmParameterSpec mgfSpec = spec.getMGFParameters();
        if (!(mgfSpec instanceof MGF1ParameterSpec)) {
            throw new InvalidParameterSpecException("Inappropriate mgf " +
                "parameters; non-null MGF1ParameterSpec only");
        }
        this.mgfSpec = (MGF1ParameterSpec) mgfSpec;
        PSource pSrc = spec.getPSource();
        if (pSrc.getAlgorithm().equals("PSpecified")) {
            p = ((PSource.PSpecified) pSrc).getValue();
        } else {
            throw new InvalidParameterSpecException("Unsupported pSource " +
                pSrc.getAlgorithm() + "; PSpecified only");
        }
    }

    protected void engineInit(byte[] encoded)
        throws IOException {
        DerInputStream der = new DerInputStream(encoded);
        mdName = "SHA-1";
        mgfSpec = MGF1ParameterSpec.SHA1;
        p = new byte[0];
        DerValue[] datum = der.getSequence(3);
        for (int i=0; i<datum.length; i++) {
            DerValue data = datum[i];
            if (data.isContextSpecific((byte) 0x00)) {
                // hash algid
                mdName = AlgorithmId.parse
                    (data.data.getDerValue()).getName();
            } else if (data.isContextSpecific((byte) 0x01)) {
                // mgf algid
                AlgorithmId val = AlgorithmId.parse(data.data.getDerValue());
                if (!val.getOID().equals(OID_MGF1)) {
                    throw new IOException("Only MGF1 mgf is supported");
                }
                AlgorithmId params = AlgorithmId.parse(
                    new DerValue(val.getEncodedParams()));
                String mgfDigestName = params.getName();
                if (mgfDigestName.equals("SHA-1")) {
                    mgfSpec = MGF1ParameterSpec.SHA1;
                } else if (mgfDigestName.equals("SHA-224")) {
                    mgfSpec = MGF1ParameterSpec.SHA224;
                } else if (mgfDigestName.equals("SHA-256")) {
                    mgfSpec = MGF1ParameterSpec.SHA256;
                } else if (mgfDigestName.equals("SHA-384")) {
                    mgfSpec = MGF1ParameterSpec.SHA384;
                } else if (mgfDigestName.equals("SHA-512")) {
                    mgfSpec = MGF1ParameterSpec.SHA512;
                } else if (mgfDigestName.equals("SHA-512/224")) {
                    mgfSpec = MGF1ParameterSpec.SHA512_224;
                } else if (mgfDigestName.equals("SHA-512/256")) {
                    mgfSpec = MGF1ParameterSpec.SHA512_256;
                } else {
                    throw new IOException(
                        "Unrecognized message digest algorithm");
                }
            } else if (data.isContextSpecific((byte) 0x02)) {
                // pSource algid
                AlgorithmId val = AlgorithmId.parse(data.data.getDerValue());
                if (!val.getOID().equals(OID_PSpecified)) {
                    throw new IOException("Wrong OID for pSpecified");
                }
                DerInputStream dis = new DerInputStream(val.getEncodedParams());
                p = dis.getOctetString();
                if (dis.available() != 0) {
                    throw new IOException("Extra data for pSpecified");
                }
            } else {
                throw new IOException("Invalid encoded OAEPParameters");
            }
        }
    }

    protected void engineInit(byte[] encoded, String decodingMethod)
        throws IOException {
        if ((decodingMethod != null) &&
            (!decodingMethod.equalsIgnoreCase("ASN.1"))) {
            throw new IllegalArgumentException("Only support ASN.1 format");
        }
        engineInit(encoded);
    }

    protected <T extends AlgorithmParameterSpec>
        T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException {
        if (OAEPParameterSpec.class.isAssignableFrom(paramSpec)) {
            return paramSpec.cast(
                new OAEPParameterSpec(mdName, "MGF1", mgfSpec,
                                      new PSource.PSpecified(p)));
        } else {
            throw new InvalidParameterSpecException
                ("Inappropriate parameter specification");
        }
    }

    protected byte[] engineGetEncoded() throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        DerOutputStream tmp2, tmp3;

        // MD
        AlgorithmId mdAlgId;
        try {
            mdAlgId = AlgorithmId.get(mdName);
        } catch (NoSuchAlgorithmException nsae) {
            throw new IOException("AlgorithmId " + mdName +
                                  " impl not found");
        }
        tmp2 = new DerOutputStream();
        mdAlgId.derEncode(tmp2);
        tmp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0),
                      tmp2);

        // MGF
        tmp2 = new DerOutputStream();
        tmp2.putOID(OID_MGF1);
        AlgorithmId mgfDigestId;
        try {
            mgfDigestId = AlgorithmId.get(mgfSpec.getDigestAlgorithm());
        } catch (NoSuchAlgorithmException nase) {
            throw new IOException("AlgorithmId " +
                    mgfSpec.getDigestAlgorithm() + " impl not found");
        }
        mgfDigestId.encode(tmp2);
        tmp3 = new DerOutputStream();
        tmp3.write(DerValue.tag_Sequence, tmp2);
        tmp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)1),
                  tmp3);

        // PSource
        tmp2 = new DerOutputStream();
        tmp2.putOID(OID_PSpecified);
        tmp2.putOctetString(p);
        tmp3 = new DerOutputStream();
        tmp3.write(DerValue.tag_Sequence, tmp2);
        tmp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)2),
                  tmp3);

        // Put all together under a SEQUENCE tag
        DerOutputStream out = new DerOutputStream();
        out.write(DerValue.tag_Sequence, tmp);
        return out.toByteArray();
    }

    protected byte[] engineGetEncoded(String encodingMethod)
        throws IOException {
        if ((encodingMethod != null) &&
            (!encodingMethod.equalsIgnoreCase("ASN.1"))) {
            throw new IllegalArgumentException("Only support ASN.1 format");
        }
        return engineGetEncoded();
    }

    protected String engineToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MD: " + mdName + "\n");
        sb.append("MGF: MGF1" + mgfSpec.getDigestAlgorithm() + "\n");
        sb.append("PSource: PSpecified " +
            (p.length==0? "":Debug.toHexString(new BigInteger(p))) + "\n");
        return sb.toString();
    }
}
