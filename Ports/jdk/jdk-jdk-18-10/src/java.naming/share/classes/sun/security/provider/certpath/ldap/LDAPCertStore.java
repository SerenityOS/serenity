/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider.certpath.ldap;

import java.net.URI;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import sun.security.util.Cache;
import sun.security.util.Debug;

/**
 * A <code>CertStore</code> that retrieves <code>Certificates</code> and
 * <code>CRL</code>s from an LDAP directory, using the PKIX LDAP V2 Schema
 * (RFC 2587):
 * <a href="http://www.ietf.org/rfc/rfc2587.txt">
 * http://www.ietf.org/rfc/rfc2587.txt</a>.
 * <p>
 * Before calling the {@link #engineGetCertificates engineGetCertificates} or
 * {@link #engineGetCRLs engineGetCRLs} methods, the
 * {@link #LDAPCertStore(CertStoreParameters)
 * LDAPCertStore(CertStoreParameters)} constructor is called to create the
 * <code>CertStore</code> and establish the DNS name and port of the LDAP
 * server from which <code>Certificate</code>s and <code>CRL</code>s will be
 * retrieved.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * As described in the javadoc for <code>CertStoreSpi</code>, the
 * <code>engineGetCertificates</code> and <code>engineGetCRLs</code> methods
 * must be thread-safe. That is, multiple threads may concurrently
 * invoke these methods on a single <code>LDAPCertStore</code> object
 * (or more than one) with no ill effects. This allows a
 * <code>CertPathBuilder</code> to search for a CRL while simultaneously
 * searching for further certificates, for instance.
 * <p>
 * This is achieved by adding the <code>synchronized</code> keyword to the
 * <code>engineGetCertificates</code> and <code>engineGetCRLs</code> methods.
 * <p>
 * This classes uses caching and requests multiple attributes at once to
 * minimize LDAP round trips. The cache is associated with the CertStore
 * instance. It uses soft references to hold the values to minimize impact
 * on footprint and currently has a maximum size of 750 attributes and a
 * 30 second default lifetime.
 * <p>
 * We always request CA certificates, cross certificate pairs, and ARLs in
 * a single LDAP request when any one of them is needed. The reason is that
 * we typically need all of them anyway and requesting them in one go can
 * reduce the number of requests to a third. Even if we don't need them,
 * these attributes are typically small enough not to cause a noticeable
 * overhead. In addition, when the prefetchCRLs flag is true, we also request
 * the full CRLs. It is currently false initially but set to true once any
 * request for an ARL to the server returns an null value. The reason is
 * that CRLs could be rather large but are rarely used. This implementation
 * should improve performance in most cases.
 *
 * @see java.security.cert.CertStore
 *
 * @since       1.4
 * @author      Steve Hanna
 * @author      Andreas Sterbenz
 */
public final class LDAPCertStore extends CertStoreSpi {

    private static final Debug debug = Debug.getInstance("certpath");

    private String ldapDN;

    private LDAPCertStoreImpl impl;

    public LDAPCertStore(CertStoreParameters params)
        throws InvalidAlgorithmParameterException {
        super(params);

        String serverName;
        int port;
        String dn = null;
        if (params == null) {
            throw new InvalidAlgorithmParameterException(
                    "Parameters required for LDAP certstore");
        }
        if (params instanceof LDAPCertStoreParameters) {
            LDAPCertStoreParameters p = (LDAPCertStoreParameters) params;
            serverName = p.getServerName();
            port = p.getPort();
        } else if (params instanceof URICertStoreParameters) {
            URICertStoreParameters p = (URICertStoreParameters) params;
            URI u = p.getURI();
            if (!u.getScheme().equalsIgnoreCase("ldap")) {
                throw new InvalidAlgorithmParameterException(
                        "Unsupported scheme '" + u.getScheme()
                                + "', only LDAP URIs are supported "
                                + "for LDAP certstore");
            }
            // Use the same default values as in LDAPCertStoreParameters
            // if unspecified in URI
            serverName = u.getHost();
            if (serverName == null) {
                serverName = "localhost";
            }
            port = u.getPort();
            if (port == -1) {
                port = 389;
            }
            dn = u.getPath();
            if (dn != null && dn.charAt(0) == '/') {
                dn = dn.substring(1);
            }
        } else {
            throw new InvalidAlgorithmParameterException(
                "Parameters must be either LDAPCertStoreParameters or "
                        + "URICertStoreParameters, but instance of "
                        + params.getClass().getName() + " passed");
        }

        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkConnect(serverName, port);
        }

        Key k = new Key(serverName, port);
        LDAPCertStoreImpl lci = certStoreCache.get(k);
        if (lci == null) {
            this.impl = new LDAPCertStoreImpl(serverName, port);
            certStoreCache.put(k, impl);
        } else {
            this.impl = lci;
            if (debug != null) {
                debug.println("LDAPCertStore.getInstance: cache hit");
            }
        }
        this.ldapDN = dn;
    }

    private static class Key {
        volatile int hashCode;

        String serverName;
        int port;

        Key(String serverName, int port) {
            this.serverName = serverName;
            this.port = port;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return (port == key.port &&
                serverName.equalsIgnoreCase(key.serverName));
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = 17;
                result = 37*result + port;
                result = 37*result +
                    serverName.toLowerCase(Locale.ENGLISH).hashCode();
                hashCode = result;
            }
            return hashCode;
        }
    }

    /**
     * Returns an LDAPCertStoreImpl object. This method consults a cache of
     * LDAPCertStoreImpl objects (shared per JVM) using the corresponding
     * LDAP server name and port info as a key.
     */
    private static final Cache<Key, LDAPCertStoreImpl>
        certStoreCache = Cache.newSoftMemoryCache(185);

    // Exist solely for regression test for ensuring that caching is done
    static synchronized LDAPCertStoreImpl getInstance(LDAPCertStoreParameters params)
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        String serverName = params.getServerName();
        int port = params.getPort();
        Key k = new Key(serverName, port);
        LDAPCertStoreImpl lci = certStoreCache.get(k);
        if (lci == null) {
            lci = new LDAPCertStoreImpl(serverName, port);
            certStoreCache.put(k, lci);
        } else {
            if (debug != null) {
                debug.println("LDAPCertStore.getInstance: cache hit");
            }
        }
        return lci;
    }

    /**
     * Returns a <code>Collection</code> of <code>Certificate</code>s that
     * match the specified selector. If no <code>Certificate</code>s
     * match the selector, an empty <code>Collection</code> will be returned.
     * <p>
     * It is not practical to search every entry in the LDAP database for
     * matching <code>Certificate</code>s. Instead, the <code>CertSelector</code>
     * is examined in order to determine where matching <code>Certificate</code>s
     * are likely to be found (according to the PKIX LDAPv2 schema, RFC 2587).
     * If the subject is specified, its directory entry is searched. If the
     * issuer is specified, its directory entry is searched. If neither the
     * subject nor the issuer are specified (or the selector is not an
     * <code>X509CertSelector</code>), a <code>CertStoreException</code> is
     * thrown.
     *
     * @param selector a <code>CertSelector</code> used to select which
     *  <code>Certificate</code>s should be returned.
     * @return a <code>Collection</code> of <code>Certificate</code>s that
     *         match the specified selector
     * @throws CertStoreException if an exception occurs
     */
    @Override
    public synchronized Collection<X509Certificate> engineGetCertificates
            (CertSelector selector) throws CertStoreException {
        if (debug != null) {
            debug.println("LDAPCertStore.engineGetCertificates() selector: "
                + String.valueOf(selector));
        }
        if (selector == null) {
            selector = new X509CertSelector();
        } else if (!(selector instanceof X509CertSelector)) {
            throw new CertStoreException("Need X509CertSelector to find certs, "
                    + "but instance of " + selector.getClass().getName()
                    + " passed");
        }
        return impl.getCertificates((X509CertSelector) selector, ldapDN);
    }

    /**
     * Returns a <code>Collection</code> of <code>CRL</code>s that
     * match the specified selector. If no <code>CRL</code>s
     * match the selector, an empty <code>Collection</code> will be returned.
     * <p>
     * It is not practical to search every entry in the LDAP database for
     * matching <code>CRL</code>s. Instead, the <code>CRLSelector</code>
     * is examined in order to determine where matching <code>CRL</code>s
     * are likely to be found (according to the PKIX LDAPv2 schema, RFC 2587).
     * If issuerNames or certChecking are specified, the issuer's directory
     * entry is searched. If neither issuerNames or certChecking are specified
     * (or the selector is not an <code>X509CRLSelector</code>), a
     * <code>CertStoreException</code> is thrown.
     *
     * @param selector A <code>CRLSelector</code> used to select which
     *  <code>CRL</code>s should be returned. Specify <code>null</code>
     *  to return all <code>CRL</code>s.
     * @return A <code>Collection</code> of <code>CRL</code>s that
     *         match the specified selector
     * @throws CertStoreException if an exception occurs
     */
    @Override
    public synchronized Collection<X509CRL> engineGetCRLs(CRLSelector selector)
            throws CertStoreException {
        if (debug != null) {
            debug.println("LDAPCertStore.engineGetCRLs() selector: "
                + selector);
        }
        // Set up selector and collection to hold CRLs
        if (selector == null) {
            selector = new X509CRLSelector();
        } else if (!(selector instanceof X509CRLSelector)) {
            throw new CertStoreException("Need X509CRLSelector to find CRLs, "
                    + "but instance of " + selector.getClass().getName()
                    + " passed");
        }
        return impl.getCRLs((X509CRLSelector) selector, ldapDN);
    }
}
