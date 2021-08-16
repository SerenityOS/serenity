/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.Certificate;
import java.security.cert.CRL;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.CRLSelector;
import java.security.cert.CertStoreSpi;

/**
 * A <code>CertStore</code> that retrieves <code>Certificates</code> and
 * <code>CRL</code>s from a <code>Collection</code>.
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
 *
 * @since       1.4
 * @author      Steve Hanna
 */
public class CollectionCertStore extends CertStoreSpi {

    private Collection<?> coll;

    /**
     * Creates a <code>CertStore</code> with the specified parameters.
     * For this class, the parameters object must be an instance of
     * <code>CollectionCertStoreParameters</code>. The <code>Collection</code>
     * included in the <code>CollectionCertStoreParameters</code> object
     * must be thread-safe.
     *
     * @param params the algorithm parameters
     * @exception InvalidAlgorithmParameterException if params is not an
     *   instance of <code>CollectionCertStoreParameters</code>
     */
    public CollectionCertStore(CertStoreParameters params)
        throws InvalidAlgorithmParameterException
    {
        super(params);
        if (!(params instanceof CollectionCertStoreParameters))
            throw new InvalidAlgorithmParameterException(
                "parameters must be CollectionCertStoreParameters");
        coll = ((CollectionCertStoreParameters) params).getCollection();
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
    public Collection<Certificate> engineGetCertificates
            (CertSelector selector) throws CertStoreException {
        if (coll == null) {
            throw new CertStoreException("Collection is null");
        }
        // Tolerate a few ConcurrentModificationExceptions
        for (int c = 0; c < 10; c++) {
            try {
                HashSet<Certificate> result = new HashSet<>();
                if (selector != null) {
                    for (Object o : coll) {
                        if ((o instanceof Certificate) &&
                            selector.match((Certificate) o))
                            result.add((Certificate)o);
                    }
                } else {
                    for (Object o : coll) {
                        if (o instanceof Certificate)
                            result.add((Certificate)o);
                    }
                }
                return(result);
            } catch (ConcurrentModificationException e) { }
        }
        throw new ConcurrentModificationException("Too many "
            + "ConcurrentModificationExceptions");
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
        throws CertStoreException
    {
        if (coll == null)
            throw new CertStoreException("Collection is null");

        // Tolerate a few ConcurrentModificationExceptions
        for (int c = 0; c < 10; c++) {
            try {
                HashSet<CRL> result = new HashSet<>();
                if (selector != null) {
                    for (Object o : coll) {
                        if ((o instanceof CRL) && selector.match((CRL) o))
                            result.add((CRL)o);
                    }
                } else {
                    for (Object o : coll) {
                        if (o instanceof CRL)
                            result.add((CRL)o);
                    }
                }
                return result;
            } catch (ConcurrentModificationException e) { }
        }
        throw new ConcurrentModificationException("Too many "
            + "ConcurrentModificationExceptions");
    }
}
