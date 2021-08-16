/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs;

import java.io.IOException;
import java.util.ArrayList;

import sun.security.util.HexDumpEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.GeneralNames;
import sun.security.x509.SerialNumber;

/**
 * This class represents a signing certificate attribute.
 * Its attribute value is defined by the following ASN.1 definition.
 * <pre>
 *
 *   id-aa-signingCertificate OBJECT IDENTIFIER ::= { iso(1)
 *     member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs9(9)
 *     smime(16) id-aa(2) 12 }
 *
 *   SigningCertificate ::=  SEQUENCE {
 *       certs       SEQUENCE OF ESSCertID,
 *       policies    SEQUENCE OF PolicyInformation OPTIONAL
 *   }
 *
 *   ESSCertID ::=  SEQUENCE {
 *       certHash        Hash,
 *       issuerSerial    IssuerSerial OPTIONAL
 *   }
 *
 *   Hash ::= OCTET STRING -- SHA1 hash of entire certificate
 *
 *   IssuerSerial ::= SEQUENCE {
 *       issuer         GeneralNames,
 *       serialNumber   CertificateSerialNumber
 *   }
 *
 *   PolicyInformation ::= SEQUENCE {
 *       policyIdentifier   CertPolicyId,
 *       policyQualifiers   SEQUENCE SIZE (1..MAX) OF
 *               PolicyQualifierInfo OPTIONAL }
 *
 *   CertPolicyId ::= OBJECT IDENTIFIER
 *
 *   PolicyQualifierInfo ::= SEQUENCE {
 *       policyQualifierId  PolicyQualifierId,
 *       qualifier        ANY DEFINED BY policyQualifierId }
 *
 *   -- Implementations that recognize additional policy qualifiers MUST
 *   -- augment the following definition for PolicyQualifierId
 *
 *   PolicyQualifierId ::= OBJECT IDENTIFIER ( id-qt-cps | id-qt-unotice )
 *
 * </pre>
 *
 * @since 1.5
 * @author Vincent Ryan
 */
public class SigningCertificateInfo {

    private byte[] ber = null;

    private ESSCertId[] certId = null;

    public SigningCertificateInfo(byte[] ber) throws IOException {
        parse(ber);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < certId.length; i++) {
            sb.append(certId[i].toString());
        }
        // format policies as a string
        sb.append("\n]");

        return sb.toString();
    }

    public void parse(byte[] bytes) throws IOException {

        // Parse signingCertificate
        DerValue derValue = new DerValue(bytes);
        if (derValue.tag != DerValue.tag_Sequence) {
            throw new IOException("Bad encoding for signingCertificate");
        }

        // Parse certs
        DerValue[] certs = derValue.data.getSequence(1);
        certId = new ESSCertId[certs.length];
        for (int i = 0; i < certs.length; i++) {
            certId[i] = new ESSCertId(certs[i]);
        }

        // Parse policies, if present
        if (derValue.data.available() > 0) {
            DerValue[] policies = derValue.data.getSequence(1);
            for (int i = 0; i < policies.length; i++) {
                // parse PolicyInformation
            }
        }
    }
}

class ESSCertId {

    private static volatile HexDumpEncoder hexDumper;

    private byte[] certHash;
    private GeneralNames issuer;
    private SerialNumber serialNumber;

    ESSCertId(DerValue certId) throws IOException {
        // Parse certHash
        certHash = certId.data.getDerValue().toByteArray();

        // Parse issuerSerial, if present
        if (certId.data.available() > 0) {
            DerValue issuerSerial = certId.data.getDerValue();
            // Parse issuer
            issuer = new GeneralNames(issuerSerial.data.getDerValue());
            // Parse serialNumber
            serialNumber = new SerialNumber(issuerSerial.data.getDerValue());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n\tCertificate hash (SHA-1):\n");
        if (hexDumper == null) {
            hexDumper = new HexDumpEncoder();
        }
        sb.append(hexDumper.encode(certHash));
        if (issuer != null && serialNumber != null) {
            sb.append("\n\tIssuer: " + issuer + "\n");
            sb.append("\t" + serialNumber);
        }
        sb.append("\n]");
        return sb.toString();
    }
}
