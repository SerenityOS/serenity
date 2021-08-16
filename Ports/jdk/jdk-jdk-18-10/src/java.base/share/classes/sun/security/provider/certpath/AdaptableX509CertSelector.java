/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Date;

import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.util.KnownOIDs;
import sun.security.x509.SerialNumber;
import sun.security.x509.AuthorityKeyIdentifierExtension;

/**
 * An adaptable X509 certificate selector for forward certification path
 * building. This selector overrides the default X509CertSelector matching
 * rules for the subjectKeyIdentifier and serialNumber criteria, and adds
 * additional rules for certificate validity.
 *
 * @since 1.7
 */
class AdaptableX509CertSelector extends X509CertSelector {

    private static final Debug debug = Debug.getInstance("certpath");

    // The start date of a validity period.
    private Date startDate;

    // The end date of a validity period.
    private Date endDate;

    // The subject key identifier
    private byte[] ski;

    // The serial number
    private BigInteger serial;

    /**
     * Sets the criterion of the X509Certificate validity period.
     *
     * Normally, we may not have to check that a certificate validity period
     * must fall within its issuer's certificate validity period. However,
     * when we face root CA key updates for version 1 certificates, according
     * to scheme of RFC 4210 or 2510, the validity periods should be checked
     * to determine the right issuer's certificate.
     *
     * Conservatively, we will only check the validity periods for version
     * 1 and version 2 certificates. For version 3 certificates, we can
     * determine the right issuer by authority and subject key identifier
     * extensions.
     *
     * @param startDate the start date of a validity period that must fall
     *        within the certificate validity period for the X509Certificate
     * @param endDate the end date of a validity period that must fall
     *        within the certificate validity period for the X509Certificate
     */
    void setValidityPeriod(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * This selector overrides the subjectKeyIdentifier matching rules of
     * X509CertSelector, so it throws IllegalArgumentException if this method
     * is ever called.
     */
    @Override
    public void setSubjectKeyIdentifier(byte[] subjectKeyID) {
        throw new IllegalArgumentException();
    }

    /**
     * This selector overrides the serialNumber matching rules of
     * X509CertSelector, so it throws IllegalArgumentException if this method
     * is ever called.
     */
    @Override
    public void setSerialNumber(BigInteger serial) {
        throw new IllegalArgumentException();
    }

    /**
     * Sets the subjectKeyIdentifier and serialNumber criteria from the
     * authority key identifier extension.
     *
     * The subjectKeyIdentifier criterion is set to the keyIdentifier field
     * of the extension, or null if it is empty. The serialNumber criterion
     * is set to the authorityCertSerialNumber field, or null if it is empty.
     *
     * Note that we do not set the subject criterion to the
     * authorityCertIssuer field of the extension. The caller MUST set
     * the subject criterion before calling match().
     *
     * @param ext the authorityKeyIdentifier extension
     * @throws IOException if there is an error parsing the extension
     */
    void setSkiAndSerialNumber(AuthorityKeyIdentifierExtension ext)
        throws IOException {

        ski = null;
        serial = null;

        if (ext != null) {
            ski = ext.getEncodedKeyIdentifier();
            SerialNumber asn = (SerialNumber)ext.get(
                AuthorityKeyIdentifierExtension.SERIAL_NUMBER);
            if (asn != null) {
                serial = asn.getNumber();
            }
            // the subject criterion should be set by the caller
        }
    }

    /**
     * Decides whether a <code>Certificate</code> should be selected.
     *
     * This method overrides the matching rules for the subjectKeyIdentifier
     * and serialNumber criteria and adds additional rules for certificate
     * validity.
     *
     * For the purpose of compatibility, when a certificate is of
     * version 1 and version 2, or the certificate does not include
     * a subject key identifier extension, the selection criterion
     * of subjectKeyIdentifier will be disabled.
     */
    @Override
    public boolean match(Certificate cert) {
        X509Certificate xcert = (X509Certificate)cert;

        // match subject key identifier
        if (!matchSubjectKeyID(xcert)) {
            return false;
        }

        // In practice, a CA may replace its root certificate and require that
        // the existing certificate is still valid, even if the AKID extension
        // does not match the replacement root certificate fields.
        //
        // Conservatively, we only support the replacement for version 1 and
        // version 2 certificate. As for version 3, the certificate extension
        // may contain sensitive information (for example, policies), the
        // AKID need to be respected to seek the exact certificate in case
        // of key or certificate abuse.
        int version = xcert.getVersion();
        if (serial != null && version > 2) {
            if (!serial.equals(xcert.getSerialNumber())) {
                return false;
            }
        }

        // Check the validity period for version 1 and 2 certificate.
        if (version < 3) {
            if (startDate != null) {
                try {
                    xcert.checkValidity(startDate);
                } catch (CertificateException ce) {
                    return false;
                }
            }
            if (endDate != null) {
                try {
                    xcert.checkValidity(endDate);
                } catch (CertificateException ce) {
                    return false;
                }
            }
        }


        if (!super.match(cert)) {
            return false;
        }

        return true;
    }

    /*
     * Match on subject key identifier extension value. These matching rules
     * are identical to X509CertSelector except that if the certificate does
     * not have a subject key identifier extension, it returns true.
     */
    private boolean matchSubjectKeyID(X509Certificate xcert) {
        if (ski == null) {
            return true;
        }
        try {
            byte[] extVal = xcert.getExtensionValue(
                    KnownOIDs.SubjectKeyID.value());
            if (extVal == null) {
                if (debug != null && Debug.isVerbose()) {
                    debug.println("AdaptableX509CertSelector.match: "
                        + "no subject key ID extension. Subject: "
                        + xcert.getSubjectX500Principal());
                }
                return true;
            }
            DerInputStream in = new DerInputStream(extVal);
            byte[] certSubjectKeyID = in.getOctetString();
            if (certSubjectKeyID == null ||
                    !Arrays.equals(ski, certSubjectKeyID)) {
                if (debug != null && Debug.isVerbose()) {
                    debug.println("AdaptableX509CertSelector.match: "
                        + "subject key IDs don't match. "
                        + "Expected: " + Arrays.toString(ski) + " "
                        + "Cert's: " + Arrays.toString(certSubjectKeyID));
                }
                return false;
            }
        } catch (IOException ex) {
            if (debug != null && Debug.isVerbose()) {
                debug.println("AdaptableX509CertSelector.match: "
                    + "exception in subject key ID check");
            }
            return false;
        }
        return true;
    }

    @Override
    public Object clone() {
        AdaptableX509CertSelector copy =
                        (AdaptableX509CertSelector)super.clone();
        if (startDate != null) {
            copy.startDate = (Date)startDate.clone();
        }

        if (endDate != null) {
            copy.endDate = (Date)endDate.clone();
        }

        if (ski != null) {
            copy.ski = ski.clone();
        }
        return copy;
    }
}
