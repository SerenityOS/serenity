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

package sun.security.timestamp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Extension;
import sun.security.util.DerValue;
import sun.security.util.DerOutputStream;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

/**
 * This class provides a timestamp request, as defined in
 * <a href="http://www.ietf.org/rfc/rfc3161.txt">RFC 3161</a>.
 *
 * The TimeStampReq ASN.1 type has the following definition:
 * <pre>
 *
 *     TimeStampReq ::= SEQUENCE {
 *         version           INTEGER { v1(1) },
 *         messageImprint    MessageImprint
 *           -- a hash algorithm OID and the hash value of the data to be
 *           -- time-stamped.
 *         reqPolicy         TSAPolicyId    OPTIONAL,
 *         nonce             INTEGER        OPTIONAL,
 *         certReq           BOOLEAN        DEFAULT FALSE,
 *         extensions        [0] IMPLICIT Extensions OPTIONAL }
 *
 *     MessageImprint ::= SEQUENCE {
 *         hashAlgorithm     AlgorithmIdentifier,
 *         hashedMessage     OCTET STRING }
 *
 *     TSAPolicyId ::= OBJECT IDENTIFIER
 *
 * </pre>
 *
 * @since 1.5
 * @author Vincent Ryan
 * @see Timestamper
 */

public class TSRequest {

    private int version = 1;

    private AlgorithmId hashAlgorithmId = null;

    private byte[] hashValue;

    private String policyId = null;

    private BigInteger nonce = null;

    private boolean returnCertificate = false;

    private X509Extension[] extensions = null;

    /**
     * Constructs a timestamp request for the supplied data.
     *
     * @param toBeTimeStamped  The data to be timestamped.
     * @param messageDigest The MessageDigest of the hash algorithm to use.
     * @throws NoSuchAlgorithmException if the hash algorithm is not supported
     */
    public TSRequest(String tSAPolicyID, byte[] toBeTimeStamped, MessageDigest messageDigest)
        throws NoSuchAlgorithmException {

        this.policyId = tSAPolicyID;
        this.hashAlgorithmId = AlgorithmId.get(messageDigest.getAlgorithm());
        this.hashValue = messageDigest.digest(toBeTimeStamped);
    }

    public byte[] getHashedMessage() {
        return hashValue.clone();
    }

    /**
     * Sets the Time-Stamp Protocol version.
     *
     * @param version The TSP version.
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Sets an object identifier for the Time-Stamp Protocol policy.
     *
     * @param policyId The policy object identifier.
     */
    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    /**
     * Sets a nonce.
     * A nonce is a single-use random number.
     *
     * @param nonce The nonce value.
     */
    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    /**
     * Request that the TSA include its signing certificate in the response.
     *
     * @param returnCertificate True if the TSA should return its signing
     *                          certificate. By default it is not returned.
     */
    public void requestCertificate(boolean returnCertificate) {
        this.returnCertificate = returnCertificate;
    }

    /**
     * Sets the Time-Stamp Protocol extensions.
     *
     * @param extensions The protocol extensions.
     */
    public void setExtensions(X509Extension[] extensions) {
        this.extensions = extensions;
    }

    public byte[] encode() throws IOException {

        DerOutputStream request = new DerOutputStream();

        // encode version
        request.putInteger(version);

        // encode messageImprint
        DerOutputStream messageImprint = new DerOutputStream();
        hashAlgorithmId.encode(messageImprint);
        messageImprint.putOctetString(hashValue);
        request.write(DerValue.tag_Sequence, messageImprint);

        // encode optional elements

        if (policyId != null) {
            request.putOID(ObjectIdentifier.of(policyId));
        }
        if (nonce != null) {
            request.putInteger(nonce);
        }
        if (returnCertificate) {
            request.putBoolean(true);
        }

        DerOutputStream out = new DerOutputStream();
        out.write(DerValue.tag_Sequence, request);
        return out.toByteArray();
    }
}
