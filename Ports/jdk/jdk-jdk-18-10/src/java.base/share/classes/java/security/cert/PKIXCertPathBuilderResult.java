/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.security.PublicKey;

/**
 * This class represents the successful result of the PKIX certification
 * path builder algorithm. All certification paths that are built and
 * returned using this algorithm are also validated according to the PKIX
 * certification path validation algorithm.
 *
 * <p>Instances of {@code PKIXCertPathBuilderResult} are returned by
 * the {@code build} method of {@code CertPathBuilder}
 * objects implementing the PKIX algorithm.
 *
 * <p>All {@code PKIXCertPathBuilderResult} objects contain the
 * certification path constructed by the build algorithm, the
 * valid policy tree and subject public key resulting from the build
 * algorithm, and a {@code TrustAnchor} describing the certification
 * authority (CA) that served as a trust anchor for the certification path.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertPathBuilderResult
 *
 * @since       1.4
 * @author      Anne Anderson
 */
public class PKIXCertPathBuilderResult extends PKIXCertPathValidatorResult
    implements CertPathBuilderResult {

    private CertPath certPath;

    /**
     * Creates an instance of {@code PKIXCertPathBuilderResult}
     * containing the specified parameters.
     *
     * @param certPath the validated {@code CertPath}
     * @param trustAnchor a {@code TrustAnchor} describing the CA that
     * served as a trust anchor for the certification path
     * @param policyTree the immutable valid policy tree, or {@code null}
     * if there are no valid policies
     * @param subjectPublicKey the public key of the subject
     * @throws NullPointerException if the {@code certPath},
     * {@code trustAnchor} or {@code subjectPublicKey} parameters
     * are {@code null}
     */
    public PKIXCertPathBuilderResult(CertPath certPath,
        TrustAnchor trustAnchor, PolicyNode policyTree,
        PublicKey subjectPublicKey)
    {
        super(trustAnchor, policyTree, subjectPublicKey);
        if (certPath == null)
            throw new NullPointerException("certPath must be non-null");
        this.certPath = certPath;
    }

    /**
     * Returns the built and validated certification path. The
     * {@code CertPath} object does not include the trust anchor.
     * Instead, use the {@link #getTrustAnchor() getTrustAnchor()} method to
     * obtain the {@code TrustAnchor} that served as the trust anchor
     * for the certification path.
     *
     * @return the built and validated {@code CertPath} (never
     * {@code null})
     */
    public CertPath getCertPath() {
        return certPath;
    }

    /**
     * Return a printable representation of this
     * {@code PKIXCertPathBuilderResult}.
     *
     * @return a {@code String} describing the contents of this
     *         {@code PKIXCertPathBuilderResult}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PKIXCertPathBuilderResult: [\n");
        sb.append("  Certification Path: " + certPath + "\n");
        sb.append("  Trust Anchor: " + getTrustAnchor().toString() + "\n");
        sb.append("  Policy Tree: " + String.valueOf(getPolicyTree()) + "\n");
        sb.append("  Subject Public Key: " + getPublicKey() + "\n");
        sb.append("]");
        return sb.toString();
    }
}
