/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.*;

import javax.security.auth.x500.X500Principal;

/**
 * A <code>CertStore</code> that retrieves <code>Certificates</code> and
 * <code>CRL</code>s from a <code>Collection</code>.
 * <p>
 * This implementation is functionally equivalent to CollectionCertStore
 * with two differences:
 * <ol>
 * <li>Upon construction, the elements in the specified Collection are
 * partially indexed. X509Certificates are indexed by subject, X509CRLs
 * by issuer, non-X509 Certificates and CRLs are copied without indexing,
 * other objects are ignored. This increases CertStore construction time
 * but allows significant speedups for searches which specify the indexed
 * attributes, in particular for large Collections (reduction from linear
 * time to effectively constant time). Searches for non-indexed queries
 * are as fast (or marginally faster) than for the standard
 * CollectionCertStore. Certificate subjects and CRL issuers
 * were found to be specified in most searches used internally by the
 * CertPath provider. Additional attributes could be indexed if there are
 * queries that justify the effort.
 *
 * <li>Changes to the specified Collection after construction time are
 * not detected and ignored. This is because there is no way to efficiently
 * detect if a Collection has been modified, a full traversal would be
 * required. That would degrade lookup performance to linear time and
 * eliminated the benefit of indexing. We may fix this via the introduction
 * of new public APIs in the future.
 * </ol>
 * <p>
 * Before calling the {@link #engineGetCertificates engineGetCertificates} or
 * {@link #engineGetCRLs engineGetCRLs} methods, the
 * {@link #CollectionCertStore(CertStoreParameters)
 * CollectionCertStore(CertStoreParameters)} constructor is called to
 * create the <code>CertStore</code> and establish the
 * <code>Collection</code> from which <code>Certificate</code>s and
 * <code>CRL</code>s will be retrieved. If the specified
 * <code>Collection</code> contains an object that is not a
 * <code>Certificate</code> or <code>CRL</code>, that object will be
 * ignored.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * As described in the javadoc for <code>CertStoreSpi</code>, the
 * <code>engineGetCertificates</code> and <code>engineGetCRLs</code> methods
 * must be thread-safe. That is, multiple threads may concurrently
 * invoke these methods on a single <code>CollectionCertStore</code>
 * object (or more than one) with no ill effects.
 * <p>
 * This is achieved by requiring that the <code>Collection</code> passed to
 * the {@link #CollectionCertStore(CertStoreParameters)
 * CollectionCertStore(CertStoreParameters)} constructor (via the
 * <code>CollectionCertStoreParameters</code> object) must have fail-fast
 * iterators. Simultaneous modifications to the <code>Collection</code> can thus be
 * detected and certificate or CRL retrieval can be retried. The fact that
 * <code>Certificate</code>s and <code>CRL</code>s must be thread-safe is also
 * essential.
 *
 * @see java.security.cert.CertStore
 * @see CollectionCertStore
 *
 * @author Andreas Sterbenz
 */
public class IndexedCollectionCertStore extends CertStoreSpi {

    /**
     * Map X500Principal(subject) -> X509Certificate | List of X509Certificate
     */
    private Map<X500Principal, Object> certSubjects;
    /**
     * Map X500Principal(issuer) -> X509CRL | List of X509CRL
     */
    private Map<X500Principal, Object> crlIssuers;
    /**
     * Sets of non-X509 certificates and CRLs
     */
    private Set<Certificate> otherCertificates;
    private Set<CRL> otherCRLs;

    /**
     * Creates a <code>CertStore</code> with the specified parameters.
     * For this class, the parameters object must be an instance of
     * <code>CollectionCertStoreParameters</code>.
     *
     * @param params the algorithm parameters
     * @exception InvalidAlgorithmParameterException if params is not an
     *   instance of <code>CollectionCertStoreParameters</code>
     */
    public IndexedCollectionCertStore(CertStoreParameters params)
            throws InvalidAlgorithmParameterException {
        super(params);
        if (!(params instanceof CollectionCertStoreParameters)) {
            throw new InvalidAlgorithmParameterException(
                "parameters must be CollectionCertStoreParameters");
        }
        Collection<?> coll = ((CollectionCertStoreParameters)params).getCollection();
        if (coll == null) {
            throw new InvalidAlgorithmParameterException
                                        ("Collection must not be null");
        }
        buildIndex(coll);
    }

    /**
     * Index the specified Collection copying all references to Certificates
     * and CRLs.
     */
    private void buildIndex(Collection<?> coll) {
        certSubjects = new HashMap<X500Principal, Object>();
        crlIssuers = new HashMap<X500Principal, Object>();
        otherCertificates = null;
        otherCRLs = null;
        for (Object obj : coll) {
            if (obj instanceof X509Certificate) {
                indexCertificate((X509Certificate)obj);
            } else if (obj instanceof X509CRL) {
                indexCRL((X509CRL)obj);
            } else if (obj instanceof Certificate) {
                if (otherCertificates == null) {
                    otherCertificates = new HashSet<Certificate>();
                }
                otherCertificates.add((Certificate)obj);
            } else if (obj instanceof CRL) {
                if (otherCRLs == null) {
                    otherCRLs = new HashSet<CRL>();
                }
                otherCRLs.add((CRL)obj);
            } else {
                // ignore
            }
        }
        if (otherCertificates == null) {
            otherCertificates = Collections.<Certificate>emptySet();
        }
        if (otherCRLs == null) {
            otherCRLs = Collections.<CRL>emptySet();
        }
    }

    /**
     * Add an X509Certificate to the index.
     */
    private void indexCertificate(X509Certificate cert) {
        X500Principal subject = cert.getSubjectX500Principal();
        Object oldEntry = certSubjects.put(subject, cert);
        if (oldEntry != null) { // assume this is unlikely
            if (oldEntry instanceof X509Certificate) {
                if (cert.equals(oldEntry)) {
                    return;
                }
                List<X509Certificate> list = new ArrayList<>(2);
                list.add(cert);
                list.add((X509Certificate)oldEntry);
                certSubjects.put(subject, list);
            } else {
                @SuppressWarnings("unchecked") // See certSubjects javadoc.
                List<X509Certificate> list = (List<X509Certificate>)oldEntry;
                if (list.contains(cert) == false) {
                    list.add(cert);
                }
                certSubjects.put(subject, list);
            }
        }
    }

    /**
     * Add an X509CRL to the index.
     */
    private void indexCRL(X509CRL crl) {
        X500Principal issuer = crl.getIssuerX500Principal();
        Object oldEntry = crlIssuers.put(issuer, crl);
        if (oldEntry != null) { // assume this is unlikely
            if (oldEntry instanceof X509CRL) {
                if (crl.equals(oldEntry)) {
                    return;
                }
                List<X509CRL> list = new ArrayList<>(2);
                list.add(crl);
                list.add((X509CRL)oldEntry);
                crlIssuers.put(issuer, list);
            } else {
                // See crlIssuers javadoc.
                @SuppressWarnings("unchecked")
                List<X509CRL> list = (List<X509CRL>)oldEntry;
                if (list.contains(crl) == false) {
                    list.add(crl);
                }
                crlIssuers.put(issuer, list);
            }
        }
    }

    /**
     * Returns a <code>Collection</code> of <code>Certificate</code>s that
     * match the specified selector. If no <code>Certificate</code>s
     * match the selector, an empty <code>Collection</code> will be returned.
     *
     * @param selector a <code>CertSelector</code> used to select which
     *  <code>Certificate</code>s should be returned. Specify <code>null</code>
     *  to return all <code>Certificate</code>s.
     * @return a <code>Collection</code> of <code>Certificate</code>s that
     *         match the specified selector
     * @throws CertStoreException if an exception occurs
     */
    @Override
    public Collection<? extends Certificate> engineGetCertificates(CertSelector selector)
            throws CertStoreException {

        // no selector means match all
        if (selector == null) {
            Set<Certificate> matches = new HashSet<>();
            matchX509Certs(new X509CertSelector(), matches);
            matches.addAll(otherCertificates);
            return matches;
        }

        if (selector instanceof X509CertSelector == false) {
            Set<Certificate> matches = new HashSet<>();
            matchX509Certs(selector, matches);
            for (Certificate cert : otherCertificates) {
                if (selector.match(cert)) {
                    matches.add(cert);
                }
            }
            return matches;
        }

        if (certSubjects.isEmpty()) {
            return Collections.<X509Certificate>emptySet();
        }
        X509CertSelector x509Selector = (X509CertSelector)selector;
        // see if the subject is specified
        X500Principal subject;
        X509Certificate matchCert = x509Selector.getCertificate();
        if (matchCert != null) {
            subject = matchCert.getSubjectX500Principal();
        } else {
            subject = x509Selector.getSubject();
        }
        if (subject != null) {
            // yes, narrow down candidates to indexed possibilities
            Object entry = certSubjects.get(subject);
            if (entry == null) {
                return Collections.<X509Certificate>emptySet();
            }
            if (entry instanceof X509Certificate) {
                X509Certificate x509Entry = (X509Certificate)entry;
                if (x509Selector.match(x509Entry)) {
                    return Collections.singleton(x509Entry);
                } else {
                    return Collections.<X509Certificate>emptySet();
                }
            } else {
                // See certSubjects javadoc.
                @SuppressWarnings("unchecked")
                List<X509Certificate> list = (List<X509Certificate>)entry;
                Set<X509Certificate> matches = new HashSet<>(16);
                for (X509Certificate cert : list) {
                    if (x509Selector.match(cert)) {
                        matches.add(cert);
                    }
                }
                return matches;
            }
        }
        // cannot use index, iterate all
        Set<Certificate> matches = new HashSet<>(16);
        matchX509Certs(x509Selector, matches);
        return matches;
    }

    /**
     * Iterate through all the X509Certificates and add matches to the
     * collection.
     */
    private void matchX509Certs(CertSelector selector,
        Collection<Certificate> matches) {

        for (Object obj : certSubjects.values()) {
            if (obj instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate)obj;
                if (selector.match(cert)) {
                    matches.add(cert);
                }
            } else {
                // See certSubjects javadoc.
                @SuppressWarnings("unchecked")
                List<X509Certificate> list = (List<X509Certificate>)obj;
                for (X509Certificate cert : list) {
                    if (selector.match(cert)) {
                        matches.add(cert);
                    }
                }
            }
        }
    }

    /**
     * Returns a <code>Collection</code> of <code>CRL</code>s that
     * match the specified selector. If no <code>CRL</code>s
     * match the selector, an empty <code>Collection</code> will be returned.
     *
     * @param selector a <code>CRLSelector</code> used to select which
     *  <code>CRL</code>s should be returned. Specify <code>null</code>
     *  to return all <code>CRL</code>s.
     * @return a <code>Collection</code> of <code>CRL</code>s that
     *         match the specified selector
     * @throws CertStoreException if an exception occurs
     */
    @Override
    public Collection<CRL> engineGetCRLs(CRLSelector selector)
            throws CertStoreException {

        if (selector == null) {
            Set<CRL> matches = new HashSet<>();
            matchX509CRLs(new X509CRLSelector(), matches);
            matches.addAll(otherCRLs);
            return matches;
        }

        if (selector instanceof X509CRLSelector == false) {
            Set<CRL> matches = new HashSet<>();
            matchX509CRLs(selector, matches);
            for (CRL crl : otherCRLs) {
                if (selector.match(crl)) {
                    matches.add(crl);
                }
            }
            return matches;
        }

        if (crlIssuers.isEmpty()) {
            return Collections.<CRL>emptySet();
        }
        X509CRLSelector x509Selector = (X509CRLSelector)selector;
        // see if the issuer is specified
        Collection<X500Principal> issuers = x509Selector.getIssuers();
        if (issuers != null) {
            HashSet<CRL> matches = new HashSet<>(16);
            for (X500Principal issuer : issuers) {
                Object entry = crlIssuers.get(issuer);
                if (entry == null) {
                    // empty
                } else if (entry instanceof X509CRL) {
                    X509CRL crl = (X509CRL)entry;
                    if (x509Selector.match(crl)) {
                        matches.add(crl);
                    }
                } else { // List
                    // See crlIssuers javadoc.
                    @SuppressWarnings("unchecked")
                    List<X509CRL> list = (List<X509CRL>)entry;
                    for (X509CRL crl : list) {
                        if (x509Selector.match(crl)) {
                            matches.add(crl);
                        }
                    }
                }
            }
            return matches;
        }
        // cannot use index, iterate all
        Set<CRL> matches = new HashSet<>(16);
        matchX509CRLs(x509Selector, matches);
        return matches;
    }

    /**
     * Iterate through all the X509CRLs and add matches to the
     * collection.
     */
    private void matchX509CRLs(CRLSelector selector, Collection<CRL> matches) {
        for (Object obj : crlIssuers.values()) {
            if (obj instanceof X509CRL) {
                X509CRL crl = (X509CRL)obj;
                if (selector.match(crl)) {
                    matches.add(crl);
                }
            } else {
                // See crlIssuers javadoc.
                @SuppressWarnings("unchecked")
                List<X509CRL> list = (List<X509CRL>)obj;
                for (X509CRL crl : list) {
                    if (selector.match(crl)) {
                        matches.add(crl);
                    }
                }
            }
        }
    }

}
