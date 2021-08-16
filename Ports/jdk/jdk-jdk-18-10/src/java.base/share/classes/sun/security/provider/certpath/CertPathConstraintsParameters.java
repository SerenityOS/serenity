/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Key;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

import sun.security.util.ConstraintsParameters;
import sun.security.validator.Validator;

/**
 * This class contains parameters for checking certificates against
 * constraints specified in the jdk.certpath.disabledAlgorithms security
 * property.
 */
class CertPathConstraintsParameters implements ConstraintsParameters {
    // The public key of the certificate
    private final Key key;
    // The certificate's trust anchor which will be checked against the
    // jdkCA constraint, if specified.
    private final TrustAnchor anchor;
    // The PKIXParameter validity date or the timestamp of the signed JAR
    // file, if this chain is associated with a timestamped signed JAR.
    private final Date date;
    // The variant or usage of this certificate
    private final String variant;
    // The certificate being checked (may be null if a CRL or OCSPResponse is
    // being checked)
    private final X509Certificate cert;

    public CertPathConstraintsParameters(X509Certificate cert,
            String variant, TrustAnchor anchor, Date date) {
        this(cert.getPublicKey(), variant, anchor, date, cert);
    }

    public CertPathConstraintsParameters(Key key, String variant,
            TrustAnchor anchor) {
        this(key, variant, anchor, null, null);
    }

    private CertPathConstraintsParameters(Key key, String variant,
            TrustAnchor anchor, Date date, X509Certificate cert) {
        this.key = key;
        this.variant = (variant == null ? Validator.VAR_GENERIC : variant);
        this.anchor = anchor;
        this.date = date;
        this.cert = cert;
    }

    @Override
    public boolean anchorIsJdkCA() {
        return CertPathHelper.isJdkCA(anchor);
    }

    @Override
    public Set<Key> getKeys() {
        return (key == null) ? Set.of() : Set.of(key);
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public String getVariant() {
        return variant;
    }

    @Override
    public String extendedExceptionMsg() {
        return (cert == null ? "."
                 : " used with certificate: " +
                   cert.getSubjectX500Principal());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[\n");
        sb.append("\n  Variant: ").append(variant);
        if (anchor != null) {
            sb.append("\n  Anchor: ").append(anchor);
        }
        if (cert != null) {
            sb.append("\n  Cert Issuer: ")
              .append(cert.getIssuerX500Principal());
            sb.append("\n  Cert Subject: ")
              .append(cert.getSubjectX500Principal());
        }
        if (key != null) {
            sb.append("\n  Key: ").append(key.getAlgorithm());
        }
        if (date != null) {
            sb.append("\n  Date: ").append(date);
        }
        sb.append("\n]");
        return sb.toString();
    }
}
