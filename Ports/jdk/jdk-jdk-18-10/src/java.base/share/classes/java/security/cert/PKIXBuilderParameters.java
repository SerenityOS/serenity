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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.util.Set;

/**
 * Parameters used as input for the PKIX {@code CertPathBuilder}
 * algorithm.
 * <p>
 * A PKIX {@code CertPathBuilder} uses these parameters to {@link
 * CertPathBuilder#build build} a {@code CertPath} which has been
 * validated according to the PKIX certification path validation algorithm.
 *
 * <p>To instantiate a {@code PKIXBuilderParameters} object, an
 * application must specify one or more <i>most-trusted CAs</i> as defined by
 * the PKIX certification path validation algorithm. The most-trusted CA
 * can be specified using one of two constructors. An application
 * can call {@link #PKIXBuilderParameters(Set, CertSelector)
 * PKIXBuilderParameters(Set, CertSelector)}, specifying a
 * {@code Set} of {@code TrustAnchor} objects, each of which
 * identifies a most-trusted CA. Alternatively, an application can call
 * {@link #PKIXBuilderParameters(KeyStore, CertSelector)
 * PKIXBuilderParameters(KeyStore, CertSelector)}, specifying a
 * {@code KeyStore} instance containing trusted certificate entries, each
 * of which will be considered as a most-trusted CA.
 *
 * <p>In addition, an application must specify constraints on the target
 * certificate that the {@code CertPathBuilder} will attempt
 * to build a path to. The constraints are specified as a
 * {@code CertSelector} object. These constraints should provide the
 * {@code CertPathBuilder} with enough search criteria to find the target
 * certificate. Minimal criteria for an {@code X509Certificate} usually
 * include the subject name and/or one or more subject alternative names.
 * If enough criteria is not specified, the {@code CertPathBuilder}
 * may throw a {@code CertPathBuilderException}.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertPathBuilder
 *
 * @since       1.4
 * @author      Sean Mullan
 */
public class PKIXBuilderParameters extends PKIXParameters {

    private int maxPathLength = 5;

    /**
     * Creates an instance of {@code PKIXBuilderParameters} with
     * the specified {@code Set} of most-trusted CAs.
     * Each element of the set is a {@link TrustAnchor TrustAnchor}.
     *
     * <p>Note that the {@code Set} is copied to protect against
     * subsequent modifications.
     *
     * @param trustAnchors a {@code Set} of {@code TrustAnchor}s
     * @param targetConstraints a {@code CertSelector} specifying the
     * constraints on the target certificate
     * @throws InvalidAlgorithmParameterException if {@code trustAnchors}
     * is empty {@code (trustAnchors.isEmpty() == true)}
     * @throws NullPointerException if {@code trustAnchors} is
     * {@code null}
     * @throws ClassCastException if any of the elements of
     * {@code trustAnchors} are not of type
     * {@code java.security.cert.TrustAnchor}
     */
    public PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector
        targetConstraints) throws InvalidAlgorithmParameterException
    {
        super(trustAnchors);
        setTargetCertConstraints(targetConstraints);
    }

    /**
     * Creates an instance of {@code PKIXBuilderParameters} that
     * populates the set of most-trusted CAs from the trusted
     * certificate entries contained in the specified {@code KeyStore}.
     * Only keystore entries that contain trusted {@code X509Certificate}s
     * are considered; all other certificate types are ignored.
     *
     * @param keystore a {@code KeyStore} from which the set of
     * most-trusted CAs will be populated
     * @param targetConstraints a {@code CertSelector} specifying the
     * constraints on the target certificate
     * @throws KeyStoreException if {@code keystore} has not been
     * initialized
     * @throws InvalidAlgorithmParameterException if {@code keystore} does
     * not contain at least one trusted certificate entry
     * @throws NullPointerException if {@code keystore} is
     * {@code null}
     */
    public PKIXBuilderParameters(KeyStore keystore,
        CertSelector targetConstraints)
        throws KeyStoreException, InvalidAlgorithmParameterException
    {
        super(keystore);
        setTargetCertConstraints(targetConstraints);
    }

    /**
     * Sets the value of the maximum number of non-self-issued intermediate
     * certificates that may exist in a certification path. A certificate
     * is self-issued if the DNs that appear in the subject and issuer
     * fields are identical and are not empty. Note that the last certificate
     * in a certification path is not an intermediate certificate, and is not
     * included in this limit. Usually the last certificate is an end entity
     * certificate, but it can be a CA certificate. A PKIX
     * {@code CertPathBuilder} instance must not build
     * paths longer than the length specified.
     *
     * <p> A value of 0 implies that the path can only contain
     * a single certificate. A value of -1 implies that the
     * path length is unconstrained (i.e. there is no maximum).
     * The default maximum path length, if not specified, is 5.
     * Setting a value less than -1 will cause an exception to be thrown.
     *
     * <p> If any of the CA certificates contain the
     * {@code BasicConstraintsExtension}, the value of the
     * {@code pathLenConstraint} field of the extension overrides
     * the maximum path length parameter whenever the result is a
     * certification path of smaller length.
     *
     * @param maxPathLength the maximum number of non-self-issued intermediate
     *  certificates that may exist in a certification path
     * @throws InvalidParameterException if {@code maxPathLength} is set
     *  to a value less than -1
     *
     * @see #getMaxPathLength
     */
    public void setMaxPathLength(int maxPathLength) {
        if (maxPathLength < -1) {
            throw new InvalidParameterException("the maximum path "
                + "length parameter can not be less than -1");
        }
        this.maxPathLength = maxPathLength;
    }

    /**
     * Returns the value of the maximum number of intermediate non-self-issued
     * certificates that may exist in a certification path. See
     * the {@link #setMaxPathLength} method for more details.
     *
     * @return the maximum number of non-self-issued intermediate certificates
     *  that may exist in a certification path, or -1 if there is no limit
     *
     * @see #setMaxPathLength
     */
    public int getMaxPathLength() {
        return maxPathLength;
    }

    /**
     * Returns a formatted string describing the parameters.
     *
     * @return a formatted string describing the parameters
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append(super.toString());
        sb.append("  Maximum Path Length: " + maxPathLength + "\n");
        sb.append("]\n");
        return sb.toString();
    }
}
