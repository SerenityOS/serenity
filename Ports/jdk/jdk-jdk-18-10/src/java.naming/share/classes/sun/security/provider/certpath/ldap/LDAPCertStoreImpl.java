/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import javax.naming.CommunicationException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.x500.X500Principal;

import com.sun.jndi.ldap.LdapReferralException;
import sun.security.util.HexDumpEncoder;
import sun.security.provider.certpath.X509CertificatePair;
import sun.security.util.Cache;
import sun.security.util.Debug;

/**
 * Core implementation of a LDAP Cert Store.
 * @see java.security.cert.CertStore
 *
 * @since       9
 */
final class LDAPCertStoreImpl {

    private static final Debug debug = Debug.getInstance("certpath");

    /**
     * LDAP attribute identifiers.
     */
    private static final String USER_CERT = "userCertificate;binary";
    private static final String CA_CERT = "cACertificate;binary";
    private static final String CROSS_CERT = "crossCertificatePair;binary";
    private static final String CRL = "certificateRevocationList;binary";
    private static final String ARL = "authorityRevocationList;binary";

    // Constants for various empty values
    private static final String[] STRING0 = new String[0];

    private static final byte[][] BB0 = new byte[0][];

    private static final Attributes EMPTY_ATTRIBUTES = new BasicAttributes();

    // cache related constants
    private static final int DEFAULT_CACHE_SIZE = 750;
    private static final int DEFAULT_CACHE_LIFETIME = 30;

    private static final int LIFETIME;

    private static final String PROP_LIFETIME =
                            "sun.security.certpath.ldap.cache.lifetime";

    /*
     * Internal system property, that when set to "true", disables the
     * JNDI application resource files lookup to prevent recursion issues
     * when validating signed JARs with LDAP URLs in certificates.
     */
    private static final String PROP_DISABLE_APP_RESOURCE_FILES =
        "sun.security.certpath.ldap.disable.app.resource.files";

    static {
        @SuppressWarnings("removal")
        String s = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(PROP_LIFETIME));
        if (s != null) {
            LIFETIME = Integer.parseInt(s); // throws NumberFormatException
        } else {
            LIFETIME = DEFAULT_CACHE_LIFETIME;
        }
    }

    /**
     * The CertificateFactory used to decode certificates from
     * their binary stored form.
     */
    private CertificateFactory cf;

    /**
     * The JNDI directory context.
     */
    private LdapContext ctx;

    /**
     * Flag indicating that communication error occurred.
     */
    private boolean communicationError = false;

    /**
     * Flag indicating whether we should prefetch CRLs.
     */
    private boolean prefetchCRLs = false;

    private final Cache<String, byte[][]> valueCache;

    private int cacheHits = 0;
    private int cacheMisses = 0;
    private int requests = 0;

    /**
     * Creates a <code>CertStore</code> with the specified parameters.
     */
    LDAPCertStoreImpl(String serverName, int port)
        throws InvalidAlgorithmParameterException {
        createInitialDirContext(serverName, port);
        // Create CertificateFactory for use later on
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new InvalidAlgorithmParameterException(
                "unable to create CertificateFactory for X.509");
        }
        if (LIFETIME == 0) {
            valueCache = Cache.newNullCache();
        } else if (LIFETIME < 0) {
            valueCache = Cache.newSoftMemoryCache(DEFAULT_CACHE_SIZE);
        } else {
            valueCache = Cache.newSoftMemoryCache(DEFAULT_CACHE_SIZE, LIFETIME);
        }
    }

    /**
     * Create InitialDirContext.
     *
     * @param server Server DNS name hosting LDAP service
     * @param port   Port at which server listens for requests
     * @throws InvalidAlgorithmParameterException if creation fails
     */
    private void createInitialDirContext(String server, int port)
            throws InvalidAlgorithmParameterException {
        String url = "ldap://" + server + ":" + port;
        Hashtable<String,Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);

        // If property is set to true, disable application resource file lookup.
        @SuppressWarnings("removal")
        boolean disableAppResourceFiles = AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> Boolean.getBoolean(PROP_DISABLE_APP_RESOURCE_FILES));
        if (disableAppResourceFiles) {
            if (debug != null) {
                debug.println("LDAPCertStore disabling app resource files");
            }
            env.put("com.sun.naming.disable.app.resource.files", "true");
        }

        try {
            ctx = new InitialLdapContext(env, null);
            /*
             * Always deal with referrals here.
             */
            ctx.addToEnvironment(Context.REFERRAL, "throw");
        } catch (NamingException e) {
            if (debug != null) {
                debug.println("LDAPCertStore.engineInit about to throw "
                    + "InvalidAlgorithmParameterException");
                e.printStackTrace();
            }
            Exception ee = new InvalidAlgorithmParameterException
                ("unable to create InitialDirContext using supplied parameters");
            ee.initCause(e);
            throw (InvalidAlgorithmParameterException)ee;
        }
    }

    /**
     * Private class encapsulating the actual LDAP operations and cache
     * handling. Use:
     *
     *   LDAPRequest request = new LDAPRequest(dn);
     *   request.addRequestedAttribute(CROSS_CERT);
     *   request.addRequestedAttribute(CA_CERT);
     *   byte[][] crossValues = request.getValues(CROSS_CERT);
     *   byte[][] caValues = request.getValues(CA_CERT);
     *
     * At most one LDAP request is sent for each instance created. If all
     * getValues() calls can be satisfied from the cache, no request
     * is sent at all. If a request is sent, all requested attributes
     * are always added to the cache irrespective of whether the getValues()
     * method is called.
     */
    private class LDAPRequest {

        private final String name;
        private Map<String, byte[][]> valueMap;
        private final List<String> requestedAttributes;

        LDAPRequest(String name) throws CertStoreException {
            this.name = checkName(name);
            requestedAttributes = new ArrayList<>(5);
        }

        private String checkName(String name) throws CertStoreException {
            if (name == null) {
                throw new CertStoreException("Name absent");
            }
            try {
                if (new CompositeName(name).size() > 1) {
                    throw new CertStoreException("Invalid name: " + name);
                }
            } catch (InvalidNameException ine) {
                throw new CertStoreException("Invalid name: " + name, ine);
            }
            return name;
        }

        void addRequestedAttribute(String attrId) {
            if (valueMap != null) {
                throw new IllegalStateException("Request already sent");
            }
            requestedAttributes.add(attrId);
        }

        /**
         * Gets one or more binary values from an attribute.
         *
         * @param attrId                the attribute identifier
         * @return                      an array of binary values (byte arrays)
         * @throws NamingException      if a naming exception occurs
         */
        byte[][] getValues(String attrId) throws NamingException {
            if (debug != null && Debug.isVerbose() && ((cacheHits + cacheMisses) % 50 == 0)) {
                debug.println("LDAPRequest Cache hits: " + cacheHits +
                    "; misses: " + cacheMisses);
            }
            String cacheKey = name + "|" + attrId;
            byte[][] values = valueCache.get(cacheKey);
            if (values != null) {
                cacheHits++;
                return values;
            }
            cacheMisses++;
            Map<String, byte[][]> attrs = getValueMap();
            values = attrs.get(attrId);
            return values;
        }

        /**
         * Get a map containing the values for this request. The first time
         * this method is called on an object, the LDAP request is sent,
         * the results parsed and added to a private map and also to the
         * cache of this LDAPCertStore. Subsequent calls return the private
         * map immediately.
         *
         * The map contains an entry for each requested attribute. The
         * attribute name is the key, values are byte[][]. If there are no
         * values for that attribute, values are byte[0][].
         *
         * @return                      the value Map
         * @throws NamingException      if a naming exception occurs
         */
        private Map<String, byte[][]> getValueMap() throws NamingException {
            if (valueMap != null) {
                return valueMap;
            }
            if (debug != null && Debug.isVerbose()) {
                debug.println("LDAPRequest: " + name + ":" + requestedAttributes);
                requests++;
                if (requests % 5 == 0) {
                    debug.println("LDAP requests: " + requests);
                }
            }
            valueMap = new HashMap<>(8);
            String[] attrIds = requestedAttributes.toArray(STRING0);
            Attributes attrs;

            if (communicationError) {
                ctx.reconnect(null);
                communicationError = false;
            }

            try {
                attrs = ctx.getAttributes(name, attrIds);
            } catch (LdapReferralException lre) {
                // LdapCtx has a hopCount field to avoid infinite loop
                while (true) {
                    try {
                        String newName = (String) lre.getReferralInfo();
                        URI newUri = new URI(newName);
                        if (!newUri.getScheme().equalsIgnoreCase("ldap")) {
                            throw new IllegalArgumentException("Not LDAP");
                        }
                        String newDn = newUri.getPath();
                        if (newDn != null && newDn.charAt(0) == '/') {
                            newDn = newDn.substring(1);
                        }
                        checkName(newDn);
                    } catch (Exception e) {
                        throw new NamingException("Cannot follow referral to "
                                + lre.getReferralInfo());
                    }
                    LdapContext refCtx =
                            (LdapContext)lre.getReferralContext();

                    // repeat the original operation at the new context
                    try {
                        attrs = refCtx.getAttributes(name, attrIds);
                        break;
                    } catch (LdapReferralException re) {
                        lre = re;
                        continue;
                    } finally {
                        // Make sure we close referral context
                        refCtx.close();
                    }
                }
            } catch (CommunicationException ce) {
                communicationError = true;
                throw ce;
            } catch (NameNotFoundException e) {
                // name does not exist on this LDAP server
                // treat same as not attributes found
                attrs = EMPTY_ATTRIBUTES;
            }
            for (String attrId : requestedAttributes) {
                Attribute attr = attrs.get(attrId);
                byte[][] values = getAttributeValues(attr);
                cacheAttribute(attrId, values);
                valueMap.put(attrId, values);
            }
            return valueMap;
        }

        /**
         * Add the values to the cache.
         */
        private void cacheAttribute(String attrId, byte[][] values) {
            String cacheKey = name + "|" + attrId;
            valueCache.put(cacheKey, values);
        }

        /**
         * Get the values for the given attribute. If the attribute is null
         * or does not contain any values, a zero length byte array is
         * returned. NOTE that it is assumed that all values are byte arrays.
         */
        private byte[][] getAttributeValues(Attribute attr)
                throws NamingException {
            byte[][] values;
            if (attr == null) {
                values = BB0;
            } else {
                values = new byte[attr.size()][];
                int i = 0;
                NamingEnumeration<?> enum_ = attr.getAll();
                while (enum_.hasMore()) {
                    Object obj = enum_.next();
                    if (debug != null) {
                        if (obj instanceof String) {
                            debug.println("LDAPCertStore.getAttrValues() "
                                + "enum.next is a string!: " + obj);
                        }
                    }
                    byte[] value = (byte[])obj;
                    values[i++] = value;
                }
            }
            return values;
        }

    }

    /*
     * Gets certificates from an attribute id and location in the LDAP
     * directory. Returns a Collection containing only the Certificates that
     * match the specified CertSelector.
     *
     * @param name the location holding the attribute
     * @param id the attribute identifier
     * @param sel a CertSelector that the Certificates must match
     * @return a Collection of Certificates found
     * @throws CertStoreException       if an exception occurs
     */
    private Collection<X509Certificate> getCertificates(LDAPRequest request,
        String id, X509CertSelector sel) throws CertStoreException {

        /* fetch encoded certs from storage */
        byte[][] encodedCert;
        try {
            encodedCert = request.getValues(id);
        } catch (NamingException namingEx) {
            throw new CertStoreException(namingEx);
        }

        int n = encodedCert.length;
        if (n == 0) {
            return Collections.emptySet();
        }

        List<X509Certificate> certs = new ArrayList<>(n);
        /* decode certs and check if they satisfy selector */
        for (int i = 0; i < n; i++) {
            ByteArrayInputStream bais = new ByteArrayInputStream(encodedCert[i]);
            try {
                Certificate cert = cf.generateCertificate(bais);
                if (sel.match(cert)) {
                  certs.add((X509Certificate)cert);
                }
            } catch (CertificateException e) {
                if (debug != null) {
                    debug.println("LDAPCertStore.getCertificates() encountered "
                        + "exception while parsing cert, skipping the bad data: ");
                    HexDumpEncoder encoder = new HexDumpEncoder();
                    debug.println(
                        "[ " + encoder.encodeBuffer(encodedCert[i]) + " ]");
                }
            }
        }

        return certs;
    }

    /*
     * Gets certificate pairs from an attribute id and location in the LDAP
     * directory.
     *
     * @param name the location holding the attribute
     * @param id the attribute identifier
     * @return a Collection of X509CertificatePairs found
     * @throws CertStoreException       if an exception occurs
     */
    private Collection<X509CertificatePair> getCertPairs(
        LDAPRequest request, String id) throws CertStoreException {

        /* fetch the encoded cert pairs from storage */
        byte[][] encodedCertPair;
        try {
            encodedCertPair = request.getValues(id);
        } catch (NamingException namingEx) {
            throw new CertStoreException(namingEx);
        }

        int n = encodedCertPair.length;
        if (n == 0) {
            return Collections.emptySet();
        }

        List<X509CertificatePair> certPairs = new ArrayList<>(n);
        /* decode each cert pair and add it to the Collection */
        for (int i = 0; i < n; i++) {
            try {
                X509CertificatePair certPair =
                    X509CertificatePair.generateCertificatePair(encodedCertPair[i]);
                certPairs.add(certPair);
            } catch (CertificateException e) {
                if (debug != null) {
                    debug.println(
                        "LDAPCertStore.getCertPairs() encountered exception "
                        + "while parsing cert, skipping the bad data: ");
                    HexDumpEncoder encoder = new HexDumpEncoder();
                    debug.println(
                        "[ " + encoder.encodeBuffer(encodedCertPair[i]) + " ]");
                }
            }
        }

        return certPairs;
    }

    /*
     * Looks at certificate pairs stored in the crossCertificatePair attribute
     * at the specified location in the LDAP directory. Returns a Collection
     * containing all X509Certificates stored in the forward component that match
     * the forward X509CertSelector and all Certificates stored in the reverse
     * component that match the reverse X509CertSelector.
     * <p>
     * If either forward or reverse is null, all certificates from the
     * corresponding component will be rejected.
     *
     * @param name the location to look in
     * @param forward the forward X509CertSelector (or null)
     * @param reverse the reverse X509CertSelector (or null)
     * @return a Collection of X509Certificates found
     * @throws CertStoreException       if an exception occurs
     */
    private Collection<X509Certificate> getMatchingCrossCerts(
            LDAPRequest request, X509CertSelector forward,
            X509CertSelector reverse)
            throws CertStoreException {
        // Get the cert pairs
        Collection<X509CertificatePair> certPairs =
                                getCertPairs(request, CROSS_CERT);

        // Find Certificates that match and put them in a list
        ArrayList<X509Certificate> matchingCerts = new ArrayList<>();
        for (X509CertificatePair certPair : certPairs) {
            X509Certificate cert;
            if (forward != null) {
                cert = certPair.getForward();
                if ((cert != null) && forward.match(cert)) {
                    matchingCerts.add(cert);
                }
            }
            if (reverse != null) {
                cert = certPair.getReverse();
                if ((cert != null) && reverse.match(cert)) {
                    matchingCerts.add(cert);
                }
            }
        }
        return matchingCerts;
    }

    /**
     * Returns a <code>Collection</code> of <code>X509Certificate</code>s that
     * match the specified selector. If no <code>X509Certificate</code>s
     * match the selector, an empty <code>Collection</code> will be returned.
     * <p>
     * It is not practical to search every entry in the LDAP database for
     * matching <code>X509Certificate</code>s. Instead, the
     * <code>X509CertSelector</code> is examined in order to determine where
     * matching <code>Certificate</code>s are likely to be found (according
     * to the PKIX LDAPv2 schema, RFC 2587).
     * If the subject is specified, its directory entry is searched. If the
     * issuer is specified, its directory entry is searched. If neither the
     * subject nor the issuer are specified (or the selector is not an
     * <code>X509CertSelector</code>), a <code>CertStoreException</code> is
     * thrown.
     *
     * @param xsel a <code>X509CertSelector</code> used to select which
     *  <code>Certificate</code>s should be returned.
     * @return a <code>Collection</code> of <code>X509Certificate</code>s that
     *         match the specified selector
     * @throws CertStoreException if an exception occurs
     */
    synchronized Collection<X509Certificate> getCertificates
        (X509CertSelector xsel, String ldapDN) throws CertStoreException {

        if (ldapDN == null) {
            X500Principal subject = xsel.getSubject();
            ldapDN = subject == null ? null : subject.getName();
        }
        int basicConstraints = xsel.getBasicConstraints();
        X500Principal issuer = xsel.getIssuer();
        HashSet<X509Certificate> certs = new HashSet<>();
        if (debug != null) {
            debug.println("LDAPCertStore.engineGetCertificates() basicConstraints: "
                + basicConstraints);
        }

        // basicConstraints:
        // -2: only EE certs accepted
        // -1: no check is done
        //  0: any CA certificate accepted
        // >1: certificate's basicConstraints extension pathlen must match
        if (ldapDN != null) {
            if (debug != null) {
                debug.println("LDAPCertStore.engineGetCertificates() "
                    + " subject is not null");
            }
            LDAPRequest request = new LDAPRequest(ldapDN);
            if (basicConstraints > -2) {
                request.addRequestedAttribute(CROSS_CERT);
                request.addRequestedAttribute(CA_CERT);
                request.addRequestedAttribute(ARL);
                if (prefetchCRLs) {
                    request.addRequestedAttribute(CRL);
                }
            }
            if (basicConstraints < 0) {
                request.addRequestedAttribute(USER_CERT);
            }

            if (basicConstraints > -2) {
                certs.addAll(getMatchingCrossCerts(request, xsel, null));
                if (debug != null) {
                    debug.println("LDAPCertStore.engineGetCertificates() after "
                        + "getMatchingCrossCerts(subject,xsel,null),certs.size(): "
                        + certs.size());
                }
                certs.addAll(getCertificates(request, CA_CERT, xsel));
                if (debug != null) {
                    debug.println("LDAPCertStore.engineGetCertificates() after "
                        + "getCertificates(subject,CA_CERT,xsel),certs.size(): "
                        + certs.size());
                }
            }
            if (basicConstraints < 0) {
                certs.addAll(getCertificates(request, USER_CERT, xsel));
                if (debug != null) {
                    debug.println("LDAPCertStore.engineGetCertificates() after "
                        + "getCertificates(subject,USER_CERT, xsel),certs.size(): "
                        + certs.size());
                }
            }
        } else {
            if (debug != null) {
                debug.println
                    ("LDAPCertStore.engineGetCertificates() subject is null");
            }
            if (basicConstraints == -2) {
                throw new CertStoreException("need subject to find EE certs");
            }
            if (issuer == null) {
                throw new CertStoreException("need subject or issuer to find certs");
            }
        }
        if (debug != null) {
            debug.println("LDAPCertStore.engineGetCertificates() about to "
                + "getMatchingCrossCerts...");
        }
        if ((issuer != null) && (basicConstraints > -2)) {
            LDAPRequest request = new LDAPRequest(issuer.getName());
            request.addRequestedAttribute(CROSS_CERT);
            request.addRequestedAttribute(CA_CERT);
            request.addRequestedAttribute(ARL);
            if (prefetchCRLs) {
                request.addRequestedAttribute(CRL);
            }

            certs.addAll(getMatchingCrossCerts(request, null, xsel));
            if (debug != null) {
                debug.println("LDAPCertStore.engineGetCertificates() after "
                    + "getMatchingCrossCerts(issuer,null,xsel),certs.size(): "
                    + certs.size());
            }
            certs.addAll(getCertificates(request, CA_CERT, xsel));
            if (debug != null) {
                debug.println("LDAPCertStore.engineGetCertificates() after "
                    + "getCertificates(issuer,CA_CERT,xsel),certs.size(): "
                    + certs.size());
            }
        }
        if (debug != null) {
            debug.println("LDAPCertStore.engineGetCertificates() returning certs");
        }
        return certs;
    }

    /*
     * Gets CRLs from an attribute id and location in the LDAP directory.
     * Returns a Collection containing only the CRLs that match the
     * specified X509CRLSelector.
     *
     * @param name the location holding the attribute
     * @param id the attribute identifier
     * @param sel a X509CRLSelector that the CRLs must match
     * @return a Collection of CRLs found
     * @throws CertStoreException       if an exception occurs
     */
    private Collection<X509CRL> getCRLs(LDAPRequest request, String id,
            X509CRLSelector sel) throws CertStoreException {

        /* fetch the encoded crls from storage */
        byte[][] encodedCRL;
        try {
            encodedCRL = request.getValues(id);
        } catch (NamingException namingEx) {
            throw new CertStoreException(namingEx);
        }

        int n = encodedCRL.length;
        if (n == 0) {
            return Collections.emptySet();
        }

        List<X509CRL> crls = new ArrayList<>(n);
        /* decode each crl and check if it matches selector */
        for (int i = 0; i < n; i++) {
            try {
                CRL crl = cf.generateCRL(new ByteArrayInputStream(encodedCRL[i]));
                if (sel.match(crl)) {
                    crls.add((X509CRL)crl);
                }
            } catch (CRLException e) {
                if (debug != null) {
                    debug.println("LDAPCertStore.getCRLs() encountered exception"
                        + " while parsing CRL, skipping the bad data: ");
                    HexDumpEncoder encoder = new HexDumpEncoder();
                    debug.println("[ " + encoder.encodeBuffer(encodedCRL[i]) + " ]");
                }
            }
        }

        return crls;
    }

    /**
     * Returns a <code>Collection</code> of <code>X509CRL</code>s that
     * match the specified selector. If no <code>X509CRL</code>s
     * match the selector, an empty <code>Collection</code> will be returned.
     * <p>
     * It is not practical to search every entry in the LDAP database for
     * matching <code>X509CRL</code>s. Instead, the <code>X509CRLSelector</code>
     * is examined in order to determine where matching <code>X509CRL</code>s
     * are likely to be found (according to the PKIX LDAPv2 schema, RFC 2587).
     * If issuerNames or certChecking are specified, the issuer's directory
     * entry is searched. If neither issuerNames or certChecking are specified
     * (or the selector is not an <code>X509CRLSelector</code>), a
     * <code>CertStoreException</code> is thrown.
     *
     * @param xsel A <code>X509CRLSelector</code> used to select which
     *  <code>CRL</code>s should be returned. Specify <code>null</code>
     *  to return all <code>CRL</code>s.
     * @return A <code>Collection</code> of <code>X509CRL</code>s that
     *         match the specified selector
     * @throws CertStoreException if an exception occurs
     */
    synchronized Collection<X509CRL> getCRLs(X509CRLSelector xsel,
         String ldapDN) throws CertStoreException {

        HashSet<X509CRL> crls = new HashSet<>();

        // Look in directory entry for issuer of cert we're checking.
        Collection<Object> issuerNames;
        X509Certificate certChecking = xsel.getCertificateChecking();
        if (certChecking != null) {
            issuerNames = new HashSet<>();
            X500Principal issuer = certChecking.getIssuerX500Principal();
            issuerNames.add(issuer.getName(X500Principal.RFC2253));
        } else {
            // But if we don't know which cert we're checking, try the directory
            // entries of all acceptable CRL issuers
            if (ldapDN != null) {
                issuerNames = new HashSet<>();
                issuerNames.add(ldapDN);
            } else {
                issuerNames = xsel.getIssuerNames();
                if (issuerNames == null) {
                    throw new CertStoreException("need issuerNames or"
                       + " certChecking to find CRLs");
                }
            }
        }
        for (Object nameObject : issuerNames) {
            String issuerName;
            if (nameObject instanceof byte[]) {
                try {
                    X500Principal issuer = new X500Principal((byte[])nameObject);
                    issuerName = issuer.getName(X500Principal.RFC2253);
                } catch (IllegalArgumentException e) {
                    continue;
                }
            } else {
                issuerName = (String)nameObject;
            }
            // If all we want is CA certs, try to get the (probably shorter) ARL
            Collection<X509CRL> entryCRLs = Collections.emptySet();
            if (certChecking == null || certChecking.getBasicConstraints() != -1) {
                LDAPRequest request = new LDAPRequest(issuerName);
                request.addRequestedAttribute(CROSS_CERT);
                request.addRequestedAttribute(CA_CERT);
                request.addRequestedAttribute(ARL);
                if (prefetchCRLs) {
                    request.addRequestedAttribute(CRL);
                }
                try {
                    entryCRLs = getCRLs(request, ARL, xsel);
                    if (entryCRLs.isEmpty()) {
                        // no ARLs found. We assume that means that there are
                        // no ARLs on this server at all and prefetch the CRLs.
                        prefetchCRLs = true;
                    } else {
                        crls.addAll(entryCRLs);
                    }
                } catch (CertStoreException e) {
                    if (debug != null) {
                        debug.println("LDAPCertStore.engineGetCRLs non-fatal error "
                            + "retrieving ARLs:" + e);
                        e.printStackTrace();
                    }
                }
            }
            // Otherwise, get the CRL
            // if certChecking is null, we don't know if we should look in ARL or CRL
            // attribute, so check both for matching CRLs.
            if (entryCRLs.isEmpty() || certChecking == null) {
                LDAPRequest request = new LDAPRequest(issuerName);
                request.addRequestedAttribute(CRL);
                entryCRLs = getCRLs(request, CRL, xsel);
                crls.addAll(entryCRLs);
            }
        }
        return crls;
    }
}
