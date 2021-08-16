/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A {@code PKIXCertPathChecker} for checking the revocation status of
 * certificates with the PKIX algorithm.
 *
 * <p>A {@code PKIXRevocationChecker} checks the revocation status of
 * certificates with the Online Certificate Status Protocol (OCSP) or
 * Certificate Revocation Lists (CRLs). OCSP is described in RFC 2560 and
 * is a network protocol for determining the status of a certificate. A CRL
 * is a time-stamped list identifying revoked certificates, and RFC 5280
 * describes an algorithm for determining the revocation status of certificates
 * using CRLs.
 *
 * <p>Each {@code PKIXRevocationChecker} must be able to check the revocation
 * status of certificates with OCSP and CRLs. By default, OCSP is the
 * preferred mechanism for checking revocation status, with CRLs as the
 * fallback mechanism. However, this preference can be switched to CRLs with
 * the {@link Option#PREFER_CRLS PREFER_CRLS} option. In addition, the fallback
 * mechanism can be disabled with the {@link Option#NO_FALLBACK NO_FALLBACK}
 * option.
 *
 * <p>A {@code PKIXRevocationChecker} is obtained by calling the
 * {@link CertPathValidator#getRevocationChecker getRevocationChecker} method
 * of a PKIX {@code CertPathValidator}. Additional parameters and options
 * specific to revocation can be set (by calling the
 * {@link #setOcspResponder setOcspResponder} method for instance). The
 * {@code PKIXRevocationChecker} is added to a {@code PKIXParameters} object
 * using the {@link PKIXParameters#addCertPathChecker addCertPathChecker}
 * or {@link PKIXParameters#setCertPathCheckers setCertPathCheckers} method,
 * and then the {@code PKIXParameters} is passed along with the {@code CertPath}
 * to be validated to the {@link CertPathValidator#validate validate} method
 * of a PKIX {@code CertPathValidator}. When supplying a revocation checker in
 * this manner, it will be used to check revocation irrespective of the setting
 * of the {@link PKIXParameters#isRevocationEnabled RevocationEnabled} flag.
 * Similarly, a {@code PKIXRevocationChecker} may be added to a
 * {@code PKIXBuilderParameters} object for use with a PKIX
 * {@code CertPathBuilder}.
 *
 * <p>Note that when a {@code PKIXRevocationChecker} is added to
 * {@code PKIXParameters}, it clones the {@code PKIXRevocationChecker};
 * thus any subsequent modifications to the {@code PKIXRevocationChecker}
 * have no effect.
 *
 * <p>Any parameter that is not set (or is set to {@code null}) will be set to
 * the default value for that parameter.
 *
 * <p><b>Concurrent Access</b>
 *
 * <p>Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single object
 * concurrently should synchronize amongst themselves and provide the
 * necessary locking. Multiple threads each manipulating separate objects
 * need not synchronize.
 *
 * @since 1.8
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2560.txt"><i>RFC&nbsp;2560: X.509
 * Internet Public Key Infrastructure Online Certificate Status Protocol -
 * OCSP</i></a>
 * @see <a href="http://www.ietf.org/rfc/rfc5280.txt"><i>RFC&nbsp;5280:
 * Internet X.509 Public Key Infrastructure Certificate and Certificate
 * Revocation List (CRL) Profile</i></a>
 */
public abstract class PKIXRevocationChecker extends PKIXCertPathChecker {
    private URI ocspResponder;
    private X509Certificate ocspResponderCert;
    private List<Extension> ocspExtensions = Collections.<Extension>emptyList();
    private Map<X509Certificate, byte[]> ocspResponses = Collections.emptyMap();
    private Set<Option> options = Collections.emptySet();

    /**
     * Default constructor.
     */
    protected PKIXRevocationChecker() {}

    /**
     * Sets the URI that identifies the location of the OCSP responder. This
     * overrides the {@code ocsp.responderURL} security property and any
     * responder specified in a certificate's Authority Information Access
     * Extension, as defined in RFC 5280.
     *
     * @param uri the responder URI
     */
    public void setOcspResponder(URI uri) {
        this.ocspResponder = uri;
    }

    /**
     * Gets the URI that identifies the location of the OCSP responder. This
     * overrides the {@code ocsp.responderURL} security property. If this
     * parameter or the {@code ocsp.responderURL} property is not set, the
     * location is determined from the certificate's Authority Information
     * Access Extension, as defined in RFC 5280.
     *
     * @return the responder URI, or {@code null} if not set
     */
    public URI getOcspResponder() {
        return ocspResponder;
    }

    /**
     * Sets the OCSP responder's certificate. This overrides the
     * {@code ocsp.responderCertSubjectName},
     * {@code ocsp.responderCertIssuerName},
     * and {@code ocsp.responderCertSerialNumber} security properties.
     *
     * @param cert the responder's certificate
     */
    public void setOcspResponderCert(X509Certificate cert) {
        this.ocspResponderCert = cert;
    }

    /**
     * Gets the OCSP responder's certificate. This overrides the
     * {@code ocsp.responderCertSubjectName},
     * {@code ocsp.responderCertIssuerName},
     * and {@code ocsp.responderCertSerialNumber} security properties. If this
     * parameter or the aforementioned properties are not set, then the
     * responder's certificate is determined as specified in RFC 2560.
     *
     * @return the responder's certificate, or {@code null} if not set
     */
    public X509Certificate getOcspResponderCert() {
        return ocspResponderCert;
    }

    // request extensions; single extensions not supported
    /**
     * Sets the optional OCSP request extensions.
     *
     * @param extensions a list of extensions. The list is copied to protect
     *        against subsequent modification.
     */
    public void setOcspExtensions(List<Extension> extensions)
    {
        this.ocspExtensions = (extensions == null)
                              ? Collections.<Extension>emptyList()
                              : new ArrayList<>(extensions);
    }

    /**
     * Gets the optional OCSP request extensions.
     *
     * @return an unmodifiable list of extensions. The list is empty if no
     *         extensions have been specified.
     */
    public List<Extension> getOcspExtensions() {
        return Collections.unmodifiableList(ocspExtensions);
    }

    /**
     * Sets the OCSP responses. These responses are used to determine
     * the revocation status of the specified certificates when OCSP is used.
     *
     * @param responses a map of OCSP responses. Each key is an
     *        {@code X509Certificate} that maps to the corresponding
     *        DER-encoded OCSP response for that certificate. A deep copy of
     *        the map is performed to protect against subsequent modification.
     */
    public void setOcspResponses(Map<X509Certificate, byte[]> responses)
    {
        if (responses == null) {
            this.ocspResponses = Collections.<X509Certificate, byte[]>emptyMap();
        } else {
            Map<X509Certificate, byte[]> copy = new HashMap<>(responses.size());
            for (Map.Entry<X509Certificate, byte[]> e : responses.entrySet()) {
                copy.put(e.getKey(), e.getValue().clone());
            }
            this.ocspResponses = copy;
        }
    }

    /**
     * Gets the OCSP responses. These responses are used to determine
     * the revocation status of the specified certificates when OCSP is used.
     *
     * @return a map of OCSP responses. Each key is an
     *        {@code X509Certificate} that maps to the corresponding
     *        DER-encoded OCSP response for that certificate. A deep copy of
     *        the map is returned to protect against subsequent modification.
     *        Returns an empty map if no responses have been specified.
     */
    public Map<X509Certificate, byte[]> getOcspResponses() {
        Map<X509Certificate, byte[]> copy = new HashMap<>(ocspResponses.size());
        for (Map.Entry<X509Certificate, byte[]> e : ocspResponses.entrySet()) {
            copy.put(e.getKey(), e.getValue().clone());
        }
        return copy;
    }

    /**
     * Sets the revocation options.
     *
     * @param options a set of revocation options. The set is copied to protect
     *        against subsequent modification.
     */
    public void setOptions(Set<Option> options) {
        this.options = (options == null)
                       ? Collections.<Option>emptySet()
                       : new HashSet<>(options);
    }

    /**
     * Gets the revocation options.
     *
     * @return an unmodifiable set of revocation options. The set is empty if
     *         no options have been specified.
     */
    public Set<Option> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    /**
     * Returns a list containing the exceptions that are ignored by the
     * revocation checker when the {@link Option#SOFT_FAIL SOFT_FAIL} option
     * is set. The list is cleared each time {@link #init init} is called.
     * The list is ordered in ascending order according to the certificate
     * index returned by {@link CertPathValidatorException#getIndex getIndex}
     * method of each entry.
     * <p>
     * An implementation of {@code PKIXRevocationChecker} is responsible for
     * adding the ignored exceptions to the list.
     *
     * @return an unmodifiable list containing the ignored exceptions. The list
     *         is empty if no exceptions have been ignored.
     */
    public abstract List<CertPathValidatorException> getSoftFailExceptions();

    @Override
    public PKIXRevocationChecker clone() {
        PKIXRevocationChecker copy = (PKIXRevocationChecker)super.clone();
        copy.ocspExtensions = new ArrayList<>(ocspExtensions);
        copy.ocspResponses = new HashMap<>(ocspResponses);
        // deep-copy the encoded responses, since they are mutable
        for (Map.Entry<X509Certificate, byte[]> entry :
                 copy.ocspResponses.entrySet())
        {
            byte[] encoded = entry.getValue();
            entry.setValue(encoded.clone());
        }
        copy.options = new HashSet<>(options);
        return copy;
    }

    /**
     * Various revocation options that can be specified for the revocation
     * checking mechanism.
     */
    public enum Option {
        /**
         * Only check the revocation status of end-entity certificates.
         */
        ONLY_END_ENTITY,
        /**
         * Prefer CRLs to OSCP. The default behavior is to prefer OCSP. Each
         * PKIX implementation should document further details of their
         * specific preference rules and fallback policies.
         */
        PREFER_CRLS,
        /**
         * Disable the fallback mechanism.
         */
        NO_FALLBACK,
        /**
         * Allow revocation check to succeed if the revocation status cannot be
         * determined for one of the following reasons:
         * <ul>
         *  <li>The CRL or OCSP response cannot be obtained because of a
         *      network error.
         *  <li>The OCSP responder returns one of the following errors
         *      specified in section 2.3 of RFC 2560: internalError or tryLater.
         * </ul><br>
         * Note that these conditions apply to both OCSP and CRLs, and unless
         * the {@code NO_FALLBACK} option is set, the revocation check is
         * allowed to succeed only if both mechanisms fail under one of the
         * conditions as stated above.
         * Exceptions that cause the network errors are ignored but can be
         * later retrieved by calling the
         * {@link #getSoftFailExceptions getSoftFailExceptions} method.
         */
        SOFT_FAIL
    }
}
