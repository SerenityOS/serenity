/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import java.io.IOException;
import java.util.Vector;

import static java.nio.charset.StandardCharsets.*;

import sun.security.krb5.Asn1Exception;
import sun.security.krb5.internal.util.KerberosString;
import sun.security.krb5.internal.crypto.EType;
import sun.security.util.*;

/**
 * Implements the ASN.1 PA-DATA type.
 *
 * <pre>{@code
 * PA-DATA         ::= SEQUENCE {
 *         -- NOTE: first tag is [1], not [0]
 *         padata-type     [1] Int32,
 *         padata-value    [2] OCTET STRING -- might be encoded AP-REQ
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class PAData {
    private int pADataType;
    private byte[] pADataValue = null;
    private static final byte TAG_PATYPE = 1;
    private static final byte TAG_PAVALUE = 2;

    private PAData() {
    }

    public PAData(int new_pADataType, byte[] new_pADataValue) {
        pADataType = new_pADataType;
        if (new_pADataValue != null) {
            pADataValue = new_pADataValue.clone();
        }
    }

    public Object clone() {
        PAData new_pAData = new PAData();
        new_pAData.pADataType = pADataType;
        if (pADataValue != null) {
            new_pAData.pADataValue = new byte[pADataValue.length];
            System.arraycopy(pADataValue, 0, new_pAData.pADataValue,
                             0, pADataValue.length);
        }
        return new_pAData;
    }

    /**
     * Constructs a PAData object.
     * @param encoding a Der-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public PAData(DerValue encoding) throws Asn1Exception, IOException {
        DerValue der = null;
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x01) {
            this.pADataType = der.getData().getBigInteger().intValue();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x02) {
            this.pADataValue = der.getData().getOctetString();
        }
        if (encoding.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes this object to an OutputStream.
     *
     * @return byte array of the encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception Asn1Exception on encoding errors.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {

        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();

        temp.putInteger(pADataType);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_PATYPE), temp);
        temp = new DerOutputStream();
        temp.putOctetString(pADataValue);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_PAVALUE), temp);

        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }

    // accessor methods
    public int getType() {
        return pADataType;
    }

    public byte[] getValue() {
        return ((pADataValue == null) ? null : pADataValue.clone());
    }

    /**
     * Parse (unmarshal) a PAData from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception if an Asn1Exception occurs.
     * @param data the Der input stream value, which contains one or more
     *        marshaled values.
     * @param explicitTag tag number.
     * @param optional indicates if this data field is optional.
     * @return an array of PAData.
     */
    public static PAData[] parseSequence(DerInputStream data,
                                      byte explicitTag, boolean optional)
        throws Asn1Exception, IOException {
        if ((optional) &&
                (((byte)data.peekByte() & (byte)0x1F) != explicitTag))
                return null;
        DerValue subDer = data.getDerValue();
        DerValue subsubDer = subDer.getData().getDerValue();
        if (subsubDer.getTag() != DerValue.tag_SequenceOf) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        Vector<PAData> v = new Vector<>();
        while (subsubDer.getData().available() > 0) {
            v.addElement(new PAData(subsubDer.getData().getDerValue()));
        }
        if (v.size() > 0) {
            PAData[] pas = new PAData[v.size()];
            v.copyInto(pas);
            return pas;
        }
        return null;
    }

    /**
     * Gets the preferred etype from the PAData array.
     * <ol>
     * <li>ETYPE-INFO2-ENTRY with unknown s2kparams ignored</li>
     * <li>ETYPE-INFO2 preferred to ETYPE-INFO</li>
     * <li>Multiple entries for same etype in one PA-DATA, use the first one.</li>
     * <li>Multiple PA-DATA with same type, choose the last one.</li>
     * </ol>
     * (This is useful when PA-DATAs from KRB-ERROR and AS-REP are combined).
     *
     * @return the etype, or defaultEType if not enough info
     * @throws Asn1Exception|IOException if there is an encoding error
     */
    public static int getPreferredEType(PAData[] pas, int defaultEType)
            throws IOException, Asn1Exception {

        if (pas == null) return defaultEType;

        DerValue d = null, d2 = null;
        for (PAData p: pas) {
            if (p.getValue() == null) continue;
            switch (p.getType()) {
                case Krb5.PA_ETYPE_INFO:
                    d = new DerValue(p.getValue());
                    break;
                case Krb5.PA_ETYPE_INFO2:
                    d2 = new DerValue(p.getValue());
                    break;
            }
        }
        if (d2 != null) {
            while (d2.data.available() > 0) {
                DerValue value = d2.data.getDerValue();
                ETypeInfo2 tmp = new ETypeInfo2(value);
                if (EType.isNewer(tmp.getEType()) || tmp.getParams() == null) {
                    // we don't support non-null s2kparams for old etypes
                    return tmp.getEType();
                }
            }
        }
        if (d != null) {
            while (d.data.available() > 0) {
                DerValue value = d.data.getDerValue();
                ETypeInfo tmp = new ETypeInfo(value);
                return tmp.getEType();
            }
        }
        return defaultEType;
    }

    /**
     * A place to store a pair of salt and s2kparams.
     * An empty salt is changed to null, to be interoperable
     * with Windows 2000 server. This is in fact not correct.
     */
    public static class SaltAndParams {
        public final String salt;
        public final byte[] params;
        public SaltAndParams(String s, byte[] p) {
            if (s != null && s.isEmpty()) s = null;
            this.salt = s;
            this.params = p;
        }
    }

    /**
     * Fetches salt and s2kparams value for eType in a series of PA-DATAs.
     * 1. ETYPE-INFO2-ENTRY with unknown s2kparams ignored
     * 2. PA-ETYPE-INFO2 preferred to PA-ETYPE-INFO preferred to PA-PW-SALT.
     * 3. multiple entries for same etype in one PA-DATA, use the first one.
     * 4. Multiple PA-DATA with same type, choose the last one
     * (This is useful when PA-DATAs from KRB-ERROR and AS-REP are combined).
     * @return salt and s2kparams. can be null if not found
     */
    public static SaltAndParams getSaltAndParams(int eType, PAData[] pas)
            throws Asn1Exception, IOException {

        if (pas == null) return null;

        DerValue d = null, d2 = null;
        String paPwSalt = null;

        for (PAData p: pas) {
            if (p.getValue() == null) continue;
            switch (p.getType()) {
                case Krb5.PA_PW_SALT:
                    paPwSalt = new String(p.getValue(),
                            KerberosString.MSNAME ? UTF_8 : ISO_8859_1);
                    break;
                case Krb5.PA_ETYPE_INFO:
                    d = new DerValue(p.getValue());
                    break;
                case Krb5.PA_ETYPE_INFO2:
                    d2 = new DerValue(p.getValue());
                    break;
            }
        }
        if (d2 != null) {
            while (d2.data.available() > 0) {
                DerValue value = d2.data.getDerValue();
                ETypeInfo2 tmp = new ETypeInfo2(value);
                if (tmp.getEType() == eType &&
                        (EType.isNewer(eType) || tmp.getParams() == null)) {
                    // we don't support non-null s2kparams for old etypes
                    return new SaltAndParams(tmp.getSalt(), tmp.getParams());
                }
            }
        }
        if (d != null) {
            while (d.data.available() > 0) {
                DerValue value = d.data.getDerValue();
                ETypeInfo tmp = new ETypeInfo(value);
                if (tmp.getEType() == eType) {
                    return new SaltAndParams(tmp.getSalt(), null);
                }
            }
        }
        if (paPwSalt != null) {
            return new SaltAndParams(paPwSalt, null);
        }
        return null;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(">>>Pre-Authentication Data:\n\t PA-DATA type = ")
                .append(pADataType).append('\n');

        switch(pADataType) {
            case Krb5.PA_ENC_TIMESTAMP:
                sb.append("\t PA-ENC-TIMESTAMP");
                break;
            case Krb5.PA_ETYPE_INFO:
                if (pADataValue != null) {
                    try {
                        DerValue der = new DerValue(pADataValue);
                        while (der.data.available() > 0) {
                            DerValue value = der.data.getDerValue();
                            ETypeInfo info = new ETypeInfo(value);
                            sb.append("\t PA-ETYPE-INFO etype = ")
                                    .append(info.getEType())
                                    .append(", salt = ")
                                    .append(info.getSalt())
                                    .append('\n');
                        }
                    } catch (IOException|Asn1Exception e) {
                        sb.append("\t <Unparseable PA-ETYPE-INFO>\n");
                    }
                }
                break;
            case Krb5.PA_ETYPE_INFO2:
                if (pADataValue != null) {
                    try {
                        DerValue der = new DerValue(pADataValue);
                        while (der.data.available() > 0) {
                            DerValue value = der.data.getDerValue();
                            ETypeInfo2 info2 = new ETypeInfo2(value);
                            sb.append("\t PA-ETYPE-INFO2 etype = ")
                                    .append(info2.getEType())
                                    .append(", salt = ")
                                    .append(info2.getSalt())
                                    .append(", s2kparams = ");
                            byte[] s2kparams = info2.getParams();
                            if (s2kparams == null) {
                                sb.append("null\n");
                            } else if (s2kparams.length == 0) {
                                sb.append("empty\n");
                            } else {
                                sb.append(new sun.security.util.HexDumpEncoder()
                                        .encodeBuffer(s2kparams));
                            }
                        }
                    } catch (IOException|Asn1Exception e) {
                        sb.append("\t <Unparseable PA-ETYPE-INFO>\n");
                    }
                }
                break;
            case Krb5.PA_FOR_USER:
                sb.append("\t PA-FOR-USER\n");
                break;
            default:
                // Unknown Pre-auth type
                break;
        }
        return sb.toString();
    }
}
