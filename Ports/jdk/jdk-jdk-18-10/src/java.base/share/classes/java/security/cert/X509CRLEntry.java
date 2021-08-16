/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.math.BigInteger;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

import sun.security.x509.X509CRLEntryImpl;

/**
 * <p>Abstract class for a revoked certificate in a CRL (Certificate
 * Revocation List).
 *
 * The ASN.1 definition for <em>revokedCertificates</em> is:
 * <pre>
 * revokedCertificates    SEQUENCE OF SEQUENCE  {
 *     userCertificate    CertificateSerialNumber,
 *     revocationDate     ChoiceOfTime,
 *     crlEntryExtensions Extensions OPTIONAL
 *                        -- if present, must be v2
 * }  OPTIONAL
 *
 * CertificateSerialNumber  ::=  INTEGER
 *
 * Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
 *
 * Extension  ::=  SEQUENCE  {
 *     extnId        OBJECT IDENTIFIER,
 *     critical      BOOLEAN DEFAULT FALSE,
 *     extnValue     OCTET STRING
 *                   -- contains a DER encoding of a value
 *                   -- of the type registered for use with
 *                   -- the extnId object identifier value
 * }
 * </pre>
 *
 * @see X509CRL
 * @see X509Extension
 *
 * @author Hemma Prafullchandra
 * @since 1.2
 */

public abstract class X509CRLEntry implements X509Extension {

    /**
     * Constructor for subclasses to call.
     */
    public X509CRLEntry() {}

    /**
     * Compares this CRL entry for equality with the given
     * object. If the {@code other} object is an
     * {@code instanceof} {@code X509CRLEntry}, then
     * its encoded form (the inner SEQUENCE) is retrieved and compared
     * with the encoded form of this CRL entry.
     *
     * @param other the object to test for equality with this CRL entry.
     * @return true iff the encoded forms of the two CRL entries
     * match, false otherwise.
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof X509CRLEntry))
            return false;
        try {
            byte[] thisCRLEntry = this.getEncoded();
            byte[] otherCRLEntry = ((X509CRLEntry)other).getEncoded();

            if (thisCRLEntry.length != otherCRLEntry.length)
                return false;
            for (int i = 0; i < thisCRLEntry.length; i++)
                 if (thisCRLEntry[i] != otherCRLEntry[i])
                     return false;
        } catch (CRLException ce) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hashcode value for this CRL entry from its
     * encoded form.
     *
     * @return the hashcode value.
     */
    public int hashCode() {
        int     retval = 0;
        try {
            byte[] entryData = this.getEncoded();
            for (int i = 1; i < entryData.length; i++)
                 retval += entryData[i] * i;

        } catch (CRLException ce) {
            return(retval);
        }
        return(retval);
    }

    /**
     * Returns the ASN.1 DER-encoded form of this CRL Entry,
     * that is the inner SEQUENCE.
     *
     * @return the encoded form of this certificate
     * @throws    CRLException if an encoding error occurs.
     */
    public abstract byte[] getEncoded() throws CRLException;

    /**
     * Gets the serial number from this X509CRLEntry,
     * the <em>userCertificate</em>.
     *
     * @return the serial number.
     */
    public abstract BigInteger getSerialNumber();

    /**
     * Get the issuer of the X509Certificate described by this entry. If
     * the certificate issuer is also the CRL issuer, this method returns
     * null.
     *
     * <p>This method is used with indirect CRLs. The default implementation
     * always returns null. Subclasses that wish to support indirect CRLs
     * should override it.
     *
     * @return the issuer of the X509Certificate described by this entry
     * or null if it is issued by the CRL issuer.
     *
     * @since 1.5
     */
    public X500Principal getCertificateIssuer() {
        return null;
    }

    /**
     * Gets the revocation date from this X509CRLEntry,
     * the <em>revocationDate</em>.
     *
     * @return the revocation date.
     */
    public abstract Date getRevocationDate();

    /**
     * Returns true if this CRL entry has extensions.
     *
     * @return true if this entry has extensions, false otherwise.
     */
    public abstract boolean hasExtensions();

    /**
     * Returns a string representation of this CRL entry.
     *
     * @return a string representation of this CRL entry.
     */
    public abstract String toString();

    /**
     * Returns the reason the certificate has been revoked, as specified
     * in the Reason Code extension of this CRL entry.
     *
     * @return the reason the certificate has been revoked, or
     *    {@code null} if this CRL entry does not have
     *    a Reason Code extension
     * @since 1.7
     */
    public CRLReason getRevocationReason() {
        if (!hasExtensions()) {
            return null;
        }
        return X509CRLEntryImpl.getRevocationReason(this);
    }
}
