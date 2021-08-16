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

package sun.security.provider.certpath;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.security.auth.x500.X500Principal;
import sun.security.util.HexDumpEncoder;
import sun.security.x509.*;
import sun.security.util.*;

/**
 * This class corresponds to the CertId field in OCSP Request
 * and the OCSP Response. The ASN.1 definition for CertID is defined
 * in RFC 2560 as:
 * <pre>
 *
 * CertID          ::=     SEQUENCE {
 *      hashAlgorithm       AlgorithmIdentifier,
 *      issuerNameHash      OCTET STRING, -- Hash of Issuer's DN
 *      issuerKeyHash       OCTET STRING, -- Hash of Issuers public key
 *      serialNumber        CertificateSerialNumber
 *      }
 *
 * </pre>
 *
 * @author      Ram Marti
 */

public class CertId {

    private static final boolean debug = false;
    private static final AlgorithmId SHA1_ALGID
        = new AlgorithmId(AlgorithmId.SHA_oid);
    private final AlgorithmId hashAlgId;
    private final byte[] issuerNameHash;
    private final byte[] issuerKeyHash;
    private final SerialNumber certSerialNumber;
    private int myhash = -1; // hashcode for this CertId

    /**
     * Creates a CertId. The hash algorithm used is SHA-1.
     */
    public CertId(X509Certificate issuerCert, SerialNumber serialNumber)
        throws IOException {

        this(issuerCert.getSubjectX500Principal(),
             issuerCert.getPublicKey(), serialNumber);
    }

    public CertId(X500Principal issuerName, PublicKey issuerKey,
                  SerialNumber serialNumber) throws IOException {

        // compute issuerNameHash
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new IOException("Unable to create CertId", nsae);
        }
        hashAlgId = SHA1_ALGID;
        md.update(issuerName.getEncoded());
        issuerNameHash = md.digest();

        // compute issuerKeyHash (remove the tag and length)
        byte[] pubKey = issuerKey.getEncoded();
        DerValue val = new DerValue(pubKey);
        DerValue[] seq = new DerValue[2];
        seq[0] = val.data.getDerValue(); // AlgorithmID
        seq[1] = val.data.getDerValue(); // Key
        byte[] keyBytes = seq[1].getBitString();
        md.update(keyBytes);
        issuerKeyHash = md.digest();
        certSerialNumber = serialNumber;

        if (debug) {
            HexDumpEncoder encoder = new HexDumpEncoder();
            System.out.println("Issuer Name is " + issuerName);
            System.out.println("issuerNameHash is " +
                encoder.encodeBuffer(issuerNameHash));
            System.out.println("issuerKeyHash is " +
                encoder.encodeBuffer(issuerKeyHash));
            System.out.println("SerialNumber is " + serialNumber.getNumber());
        }
    }

    /**
     * Creates a CertId from its ASN.1 DER encoding.
     */
    public CertId(DerInputStream derIn) throws IOException {
        hashAlgId = AlgorithmId.parse(derIn.getDerValue());
        issuerNameHash = derIn.getOctetString();
        issuerKeyHash = derIn.getOctetString();
        certSerialNumber = new SerialNumber(derIn);
    }

    /**
     * Return the hash algorithm identifier.
     */
    public AlgorithmId getHashAlgorithm() {
        return hashAlgId;
    }

    /**
     * Return the hash value for the issuer name.
     */
    public byte[] getIssuerNameHash() {
        return issuerNameHash;
    }

    /**
     * Return the hash value for the issuer key.
     */
    public byte[] getIssuerKeyHash() {
        return issuerKeyHash;
    }

    /**
     * Return the serial number.
     */
    public BigInteger getSerialNumber() {
        return certSerialNumber.getNumber();
    }

    /**
     * Encode the CertId using ASN.1 DER.
     * The hash algorithm used is SHA-1.
     */
    public void encode(DerOutputStream out) throws IOException {

        DerOutputStream tmp = new DerOutputStream();
        hashAlgId.encode(tmp);
        tmp.putOctetString(issuerNameHash);
        tmp.putOctetString(issuerKeyHash);
        certSerialNumber.encode(tmp);
        out.write(DerValue.tag_Sequence, tmp);

        if (debug) {
            HexDumpEncoder encoder = new HexDumpEncoder();
            System.out.println("Encoded certId is " +
                encoder.encode(out.toByteArray()));
        }
    }

    /**
     * Returns a hashcode value for this CertId.
     *
     * @return the hashcode value.
     */
    @Override public int hashCode() {
        if (myhash == -1) {
            myhash = hashAlgId.hashCode();
            for (int i = 0; i < issuerNameHash.length; i++) {
                myhash += issuerNameHash[i] * i;
            }
            for (int i = 0; i < issuerKeyHash.length; i++) {
                myhash += issuerKeyHash[i] * i;
            }
            myhash += certSerialNumber.getNumber().hashCode();
        }
        return myhash;
    }

    /**
     * Compares this CertId for equality with the specified
     * object. Two CertId objects are considered equal if their hash algorithms,
     * their issuer name and issuer key hash values and their serial numbers
     * are equal.
     *
     * @param other the object to test for equality with this object.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || (!(other instanceof CertId))) {
            return false;
        }

        CertId that = (CertId) other;
        if (hashAlgId.equals(that.getHashAlgorithm()) &&
            Arrays.equals(issuerNameHash, that.getIssuerNameHash()) &&
            Arrays.equals(issuerKeyHash, that.getIssuerKeyHash()) &&
            certSerialNumber.getNumber().equals(that.getSerialNumber())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create a string representation of the CertId.
     */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CertId \n");
        sb.append("Algorithm: " + hashAlgId.toString() +"\n");
        sb.append("issuerNameHash \n");
        HexDumpEncoder encoder = new HexDumpEncoder();
        sb.append(encoder.encode(issuerNameHash));
        sb.append("\nissuerKeyHash: \n");
        sb.append(encoder.encode(issuerKeyHash));
        sb.append("\n" +  certSerialNumber.toString());
        return sb.toString();
    }
}
