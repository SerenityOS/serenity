/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectOutputStream;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.Checksum;
import sun.security.krb5.Asn1Exception;
import sun.security.krb5.Realm;
import sun.security.krb5.RealmException;
import sun.security.util.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sun.security.krb5.internal.util.KerberosString;
/**
 * Implements the ASN.1 KRBError type.
 *
 * <pre>{@code
 * KRB-ERROR       ::= [APPLICATION 30] SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (30),
 *         ctime           [2] KerberosTime OPTIONAL,
 *         cusec           [3] Microseconds OPTIONAL,
 *         stime           [4] KerberosTime,
 *         susec           [5] Microseconds,
 *         error-code      [6] Int32,
 *         crealm          [7] Realm OPTIONAL,
 *         cname           [8] PrincipalName OPTIONAL,
 *         realm           [9] Realm -- service realm --,
 *         sname           [10] PrincipalName -- service name --,
 *         e-text          [11] KerberosString OPTIONAL,
 *         e-data          [12] OCTET STRING OPTIONAL
 * }
 *
 * METHOD-DATA     ::= SEQUENCE OF PA-DATA
 *
 * TYPED-DATA      ::= SEQUENCE SIZE (1..MAX) OF SEQUENCE {
 *         data-type       [0] Int32,
 *         data-value      [1] OCTET STRING OPTIONAL
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */
// The instance fields not statically typed as Serializable are ASN.1
// encoded and written by the writeObject method.
@SuppressWarnings("serial")
public class KRBError implements java.io.Serializable {
    static final long serialVersionUID = 3643809337475284503L;

    private int pvno;
    private int msgType;
    private KerberosTime cTime; //optional
    private Integer cuSec; //optional
    private KerberosTime sTime;
    private Integer suSec;
    private int errorCode;
    private Realm crealm; //optional
    private PrincipalName cname; //optional
    private PrincipalName sname;
    private String eText; //optional
    private byte[] eData; //optional
    private Checksum eCksum; //optional

    private PAData[] pa;    // PA-DATA in eData

    private static boolean DEBUG = Krb5.DEBUG;

    private void readObject(ObjectInputStream is)
            throws IOException, ClassNotFoundException {
        try {
            init(new DerValue((byte[])is.readObject()));
            parseEData(eData);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void writeObject(ObjectOutputStream os)
            throws IOException {
        try {
            os.writeObject(asn1Encode());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public KRBError(
                    APOptions new_apOptions,
                    KerberosTime new_cTime,
                    Integer new_cuSec,
                    KerberosTime new_sTime,
                    Integer new_suSec,
                    int new_errorCode,
                    PrincipalName new_cname,
                    PrincipalName new_sname,
                    String new_eText,
                    byte[] new_eData
                        ) throws IOException, Asn1Exception {
        pvno = Krb5.PVNO;
        msgType = Krb5.KRB_ERROR;
        cTime = new_cTime;
        cuSec = new_cuSec;
        sTime = new_sTime;
        suSec = new_suSec;
        errorCode = new_errorCode;
        crealm = new_cname != null ? new_cname.getRealm() : null;
        cname = new_cname;
        sname = new_sname;
        eText = new_eText;
        eData = new_eData;

        parseEData(eData);
    }

    public KRBError(
                    APOptions new_apOptions,
                    KerberosTime new_cTime,
                    Integer new_cuSec,
                    KerberosTime new_sTime,
                    Integer new_suSec,
                    int new_errorCode,
                    PrincipalName new_cname,
                    PrincipalName new_sname,
                    String new_eText,
                    byte[] new_eData,
                    Checksum new_eCksum
                        ) throws IOException, Asn1Exception {
        pvno = Krb5.PVNO;
        msgType = Krb5.KRB_ERROR;
        cTime = new_cTime;
        cuSec = new_cuSec;
        sTime = new_sTime;
        suSec = new_suSec;
        errorCode = new_errorCode;
        crealm = new_cname != null ? new_cname.getRealm() : null;
        cname = new_cname;
        sname = new_sname;
        eText = new_eText;
        eData = new_eData;
        eCksum = new_eCksum;

        parseEData(eData);
    }

    public KRBError(byte[] data) throws Asn1Exception,
            RealmException, KrbApErrException, IOException {
        init(new DerValue(data));
        parseEData(eData);
    }

    public KRBError(DerValue encoding) throws Asn1Exception,
            RealmException, KrbApErrException, IOException {
        init(encoding);
        showDebug();
        parseEData(eData);
    }

    /*
     * Attention:
     *
     * According to RFC 4120, e-data field in a KRB-ERROR message is
     * a METHOD-DATA when errorCode is KDC_ERR_PREAUTH_REQUIRED,
     * and application-specific otherwise (The RFC suggests using
     * TYPED-DATA).
     *
     * Hence, the ideal procedure to parse e-data should look like:
     *
     * if (errorCode is KDC_ERR_PREAUTH_REQUIRED) {
     *    parse as METHOD-DATA
     * } else {
     *    try parsing as TYPED-DATA
     * }
     *
     * Unfortunately, we know that some implementations also use the
     * METHOD-DATA format for errorcode KDC_ERR_PREAUTH_FAILED, and
     * do not use the TYPED-DATA for other errorcodes (say,
     * KDC_ERR_CLIENT_REVOKED).
     */

    // parse the edata field
    private void parseEData(byte[] data) throws IOException {
        if (data == null) {
            return;
        }

        // We need to parse eData as METHOD-DATA for both errorcodes.
        if (errorCode == Krb5.KDC_ERR_PREAUTH_REQUIRED
                || errorCode == Krb5.KDC_ERR_PREAUTH_FAILED) {
            try {
                // RFC 4120 does not guarantee that eData is METHOD-DATA when
                // errorCode is KDC_ERR_PREAUTH_FAILED. Therefore, the parse
                // may fail.
                parsePAData(data);
            } catch (Exception e) {
                if (DEBUG) {
                    System.out.println("Unable to parse eData field of KRB-ERROR:\n" +
                            new sun.security.util.HexDumpEncoder().encodeBuffer(data));
                }
                IOException ioe = new IOException(
                        "Unable to parse eData field of KRB-ERROR");
                ioe.initCause(e);
                throw ioe;
            }
        } else {
            if (DEBUG) {
                System.out.println("Unknown eData field of KRB-ERROR:\n" +
                        new sun.security.util.HexDumpEncoder().encodeBuffer(data));
            }
        }
    }

    /**
     * Try parsing the data as a sequence of PA-DATA.
     * @param data the data block
     */
    private void parsePAData(byte[] data)
            throws IOException, Asn1Exception {
        DerValue derPA = new DerValue(data);
        List<PAData> paList = new ArrayList<>();
        while (derPA.data.available() > 0) {
            // read the PA-DATA
            DerValue tmp = derPA.data.getDerValue();
            PAData pa_data = new PAData(tmp);
            paList.add(pa_data);
            if (DEBUG) {
                System.out.println(pa_data);
            }
        }
        pa = paList.toArray(new PAData[paList.size()]);
    }

    public final Realm getClientRealm() {
        return crealm;
    }

    public final KerberosTime getServerTime() {
        return sTime;
    }

    public final KerberosTime getClientTime() {
        return cTime;
    }

    public final Integer getServerMicroSeconds() {
        return suSec;
    }

    public final Integer getClientMicroSeconds() {
        return cuSec;
    }

    public final int getErrorCode() {
        return errorCode;
    }

    // access pre-auth info
    public final PAData[] getPA() {
        return pa;
    }

    public final String getErrorString() {
        return eText;
    }

    /**
     * Initializes a KRBError object.
     * @param encoding a DER-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception KrbApErrException if the value read from the DER-encoded data
     *  stream does not match the pre-defined value.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */
    private void init(DerValue encoding) throws Asn1Exception,
            RealmException, KrbApErrException, IOException {
        DerValue der, subDer;
        if (((encoding.getTag() & (byte)0x1F) != (byte)0x1E)
                || (encoding.isApplication() != true)
                || (encoding.isConstructed() != true)) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if (der.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte)0x1F) == (byte)0x00) {

            pvno = subDer.getData().getBigInteger().intValue();
            if (pvno != Krb5.PVNO)
                throw new KrbApErrException(Krb5.KRB_AP_ERR_BADVERSION);
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte)0x1F) == (byte)0x01) {
            msgType = subDer.getData().getBigInteger().intValue();
            if (msgType != Krb5.KRB_ERROR) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_MSG_TYPE);
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        cTime = KerberosTime.parse(der.getData(), (byte)0x02, true);
        if ((der.getData().peekByte() & 0x1F) == 0x03) {
            subDer = der.getData().getDerValue();
            cuSec = subDer.getData().getBigInteger().intValue();
        }
        else cuSec = null;
        sTime = KerberosTime.parse(der.getData(), (byte)0x04, false);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte)0x1F) == (byte)0x05) {
            suSec = subDer.getData().getBigInteger().intValue();
        }
        else  throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        subDer = der.getData().getDerValue();
        if ((subDer.getTag() & (byte)0x1F) == (byte)0x06) {
            errorCode = subDer.getData().getBigInteger().intValue();
        }
        else  throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        crealm = Realm.parse(der.getData(), (byte)0x07, true);
        cname = PrincipalName.parse(der.getData(), (byte)0x08, true, crealm);
        Realm realm = Realm.parse(der.getData(), (byte)0x09, false);
        sname = PrincipalName.parse(der.getData(), (byte)0x0A, false, realm);
        eText = null;
        eData = null;
        eCksum = null;
        if (der.getData().available() >0) {
            if ((der.getData().peekByte() & 0x1F) == 0x0B) {
                subDer = der.getData().getDerValue();
                eText = new KerberosString(subDer.getData().getDerValue())
                        .toString();
            }
        }
        if (der.getData().available() >0) {
            if ((der.getData().peekByte() & 0x1F) == 0x0C) {
                subDer = der.getData().getDerValue();
                eData = subDer.getData().getOctetString();
            }
        }
        if (der.getData().available() >0) {
            eCksum = Checksum.parse(der.getData(), (byte)0x0D, true);
        }
        if (der.getData().available() >0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * For debug use only
     */
    private void showDebug() {
        if (DEBUG) {
            System.out.println(">>>KRBError:");
            if (cTime != null)
                System.out.println("\t cTime is " + cTime.toDate().toString() + " " + cTime.toDate().getTime());
            if (cuSec != null) {
                System.out.println("\t cuSec is " + cuSec.intValue());
            }

            System.out.println("\t sTime is " + sTime.toDate().toString
                               () + " " + sTime.toDate().getTime());
            System.out.println("\t suSec is " + suSec);
            System.out.println("\t error code is " + errorCode);
            System.out.println("\t error Message is " + Krb5.getErrorMessage(errorCode));
            if (crealm != null) {
                System.out.println("\t crealm is " + crealm.toString());
            }
            if (cname != null) {
                System.out.println("\t cname is " + cname.toString());
            }
            if (sname != null) {
                System.out.println("\t sname is " + sname.toString());
            }
            if (eData != null) {
                System.out.println("\t eData provided.");
            }
            if (eCksum != null) {
                System.out.println("\t checksum provided.");
            }
            System.out.println("\t msgType is " + msgType);
        }
    }

    /**
     * Encodes an KRBError object.
     * @return the byte array of encoded KRBError object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream temp = new DerOutputStream();
        DerOutputStream bytes = new DerOutputStream();

        temp.putInteger(BigInteger.valueOf(pvno));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), temp);
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(msgType));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), temp);

        if (cTime != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x02), cTime.asn1Encode());
        }
        if (cuSec != null) {
            temp = new DerOutputStream();
            temp.putInteger(BigInteger.valueOf(cuSec.intValue()));
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x03), temp);
        }

        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x04), sTime.asn1Encode());
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(suSec.intValue()));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x05), temp);
        temp = new DerOutputStream();
        temp.putInteger(BigInteger.valueOf(errorCode));
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x06), temp);

        if (crealm != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x07), crealm.asn1Encode());
        }
        if (cname != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x08), cname.asn1Encode());
        }

        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x09), sname.getRealm().asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x0A), sname.asn1Encode());

        if (eText != null) {
            temp = new DerOutputStream();
            temp.putDerValue(new KerberosString(eText).toDerValue());
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x0B), temp);
        }
        if (eData != null) {
            temp = new DerOutputStream();
            temp.putOctetString(eData);
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x0C), temp);
        }
        if (eCksum != null) {
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x0D), eCksum.asn1Encode());
        }

        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_APPLICATION, true, (byte)0x1E), temp);
        return bytes.toByteArray();
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof KRBError)) {
            return false;
        }

        KRBError other = (KRBError)obj;
        return  pvno == other.pvno &&
                msgType == other.msgType &&
                isEqual(cTime, other.cTime) &&
                isEqual(cuSec, other.cuSec) &&
                isEqual(sTime, other.sTime) &&
                isEqual(suSec, other.suSec) &&
                errorCode == other.errorCode &&
                isEqual(crealm, other.crealm) &&
                isEqual(cname, other.cname) &&
                isEqual(sname, other.sname) &&
                isEqual(eText, other.eText) &&
                java.util.Arrays.equals(eData, other.eData) &&
                isEqual(eCksum, other.eCksum);
    }

    private static boolean isEqual(Object a, Object b) {
        return (a == null)?(b == null):(a.equals(b));
    }

    @Override public int hashCode() {
        int result = 17;
        result = 37 * result + pvno;
        result = 37 * result + msgType;
        if (cTime != null) result = 37 * result + cTime.hashCode();
        if (cuSec != null) result = 37 * result + cuSec.hashCode();
        if (sTime != null) result = 37 * result + sTime.hashCode();
        if (suSec != null) result = 37 * result + suSec.hashCode();
        result = 37 * result + errorCode;
        if (crealm != null) result = 37 * result + crealm.hashCode();
        if (cname != null) result = 37 * result + cname.hashCode();
        if (sname != null) result = 37 * result + sname.hashCode();
        if (eText != null) result = 37 * result + eText.hashCode();
        result = 37 * result + Arrays.hashCode(eData);
        if (eCksum != null) result = 37 * result + eCksum.hashCode();
        return result;
    }
}
