/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.net.Socket;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.security.auth.x500.X500Principal;


/**
 * An implementation of X509KeyManager backed by a KeyStore.
 *
 * The backing KeyStore is inspected when this object is constructed.
 * All key entries containing a PrivateKey and a non-empty chain of
 * X509Certificate are then copied into an internal store. This means
 * that subsequent modifications of the KeyStore have no effect on the
 * X509KeyManagerImpl object.
 *
 * Note that this class assumes that all keys are protected by the same
 * password.
 *
 * The JSSE handshake code currently calls into this class via
 * chooseClientAlias() and chooseServerAlias() to find the certificates to
 * use. As implemented here, both always return the first alias returned by
 * getClientAliases() and getServerAliases(). In turn, these methods are
 * implemented by calling getAliases(), which performs the actual lookup.
 *
 * Note that this class currently implements no checking of the local
 * certificates. In particular, it is *not* guaranteed that:
 *  . the certificates are within their validity period and not revoked
 *  . the signatures verify
 *  . they form a PKIX compliant chain.
 *  . the certificate extensions allow the certificate to be used for
 *    the desired purpose.
 *
 * Chains that fail any of these criteria will probably be rejected by
 * the remote peer.
 *
 */
final class SunX509KeyManagerImpl extends X509ExtendedKeyManager {

    private static final String[] STRING0 = new String[0];

    /*
     * The credentials from the KeyStore as
     * Map: String(alias) -> X509Credentials(credentials)
     */
    private final Map<String,X509Credentials> credentialsMap;

    /*
     * Cached server aliases for the case issuers == null.
     * (in the current JSSE implementation, issuers are always null for
     * server certs). See chooseServerAlias() for details.
     *
     * Map: String(keyType) -> String[](alias)
     */
    private final Map<String,String[]> serverAliasCache;

    /*
     * Basic container for credentials implemented as an inner class.
     */
    private static class X509Credentials {
        final PrivateKey privateKey;
        final X509Certificate[] certificates;
        private final Set<X500Principal> issuerX500Principals;

        X509Credentials(PrivateKey privateKey, X509Certificate[] certificates) {
            // assert privateKey and certificates != null
            this.privateKey = privateKey;
            this.certificates = certificates;
            this.issuerX500Principals = new HashSet<>(certificates.length);
            for (X509Certificate certificate : certificates) {
                issuerX500Principals.add(certificate.getIssuerX500Principal());
            }
        }

        Set<X500Principal> getIssuerX500Principals() {
            return issuerX500Principals;
        }
    }

    SunX509KeyManagerImpl(KeyStore ks, char[] password)
            throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException {

        credentialsMap = new HashMap<String,X509Credentials>();
        serverAliasCache = Collections.synchronizedMap(
                            new HashMap<String,String[]>());
        if (ks == null) {
            return;
        }

        for (Enumeration<String> aliases = ks.aliases();
                                        aliases.hasMoreElements(); ) {
            String alias = aliases.nextElement();
            if (!ks.isKeyEntry(alias)) {
                continue;
            }
            Key key = ks.getKey(alias, password);
            if (!(key instanceof PrivateKey)) {
                continue;
            }
            Certificate[] certs = ks.getCertificateChain(alias);
            if ((certs == null) || (certs.length == 0) ||
                    !(certs[0] instanceof X509Certificate)) {
                continue;
            }
            if (!(certs instanceof X509Certificate[])) {
                Certificate[] tmp = new X509Certificate[certs.length];
                System.arraycopy(certs, 0, tmp, 0, certs.length);
                certs = tmp;
            }

            X509Credentials cred = new X509Credentials((PrivateKey)key,
                (X509Certificate[])certs);
            credentialsMap.put(alias, cred);
            if (SSLLogger.isOn && SSLLogger.isOn("keymanager")) {
                SSLLogger.fine("found key for : " + alias, (Object[])certs);
            }
        }
    }

    /*
     * Returns the certificate chain associated with the given alias.
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last)
     */
    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        if (alias == null) {
            return null;
        }
        X509Credentials cred = credentialsMap.get(alias);
        if (cred == null) {
            return null;
        } else {
            return cred.certificates.clone();
        }
    }

    /*
     * Returns the key associated with the given alias
     */
    @Override
    public PrivateKey getPrivateKey(String alias) {
        if (alias == null) {
            return null;
        }
        X509Credentials cred = credentialsMap.get(alias);
        if (cred == null) {
            return null;
        } else {
            return cred.privateKey;
        }
    }

    /*
     * Choose an alias to authenticate the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers,
            Socket socket) {
        /*
         * We currently don't do anything with socket, but
         * someday we might.  It might be a useful hint for
         * selecting one of the aliases we get back from
         * getClientAliases().
         */

        if (keyTypes == null) {
            return null;
        }

        for (int i = 0; i < keyTypes.length; i++) {
            String[] aliases = getClientAliases(keyTypes[i], issuers);
            if ((aliases != null) && (aliases.length > 0)) {
                return aliases[0];
            }
        }
        return null;
    }

    /*
     * Choose an alias to authenticate the client side of an
     * <code>SSLEngine</code> connection given the public key type
     * and the list of certificate issuer authorities recognized by
     * the peer (if any).
     *
     * @since 1.5
     */
    @Override
    public String chooseEngineClientAlias(String[] keyType,
            Principal[] issuers, SSLEngine engine) {
        /*
         * If we ever start using socket as a selection criteria,
         * we'll need to adjust this.
         */
        return chooseClientAlias(keyType, issuers, null);
    }

    /*
     * Choose an alias to authenticate the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String chooseServerAlias(String keyType,
            Principal[] issuers, Socket socket) {
        /*
         * We currently don't do anything with socket, but
         * someday we might.  It might be a useful hint for
         * selecting one of the aliases we get back from
         * getServerAliases().
         */
        if (keyType == null) {
            return null;
        }

        String[] aliases;

        if (issuers == null || issuers.length == 0) {
            aliases = serverAliasCache.get(keyType);
            if (aliases == null) {
                aliases = getServerAliases(keyType, issuers);
                // Cache the result (positive and negative lookups)
                if (aliases == null) {
                    aliases = STRING0;
                }
                serverAliasCache.put(keyType, aliases);
            }
        } else {
            aliases = getServerAliases(keyType, issuers);
        }
        if ((aliases != null) && (aliases.length > 0)) {
            return aliases[0];
        }
        return null;
    }

    /*
     * Choose an alias to authenticate the server side of an
     * <code>SSLEngine</code> connection given the public key type
     * and the list of certificate issuer authorities recognized by
     * the peer (if any).
     *
     * @since 1.5
     */
    @Override
    public String chooseEngineServerAlias(String keyType,
            Principal[] issuers, SSLEngine engine) {
        /*
         * If we ever start using socket as a selection criteria,
         * we'll need to adjust this.
         */
        return chooseServerAlias(keyType, issuers, null);
    }

    /*
     * Get the matching aliases for authenticating the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return getAliases(keyType, issuers);
    }

    /*
     * Get the matching aliases for authenticating the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return getAliases(keyType, issuers);
    }

    /*
     * Get the matching aliases for authenticating the either side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * Issuers comes to us in the form of X500Principal[].
     */
    private String[] getAliases(String keyType, Principal[] issuers) {
        if (keyType == null) {
            return null;
        }
        if (issuers == null) {
            issuers = new X500Principal[0];
        }
        if (!(issuers instanceof X500Principal[])) {
            // normally, this will never happen but try to recover if it does
            issuers = convertPrincipals(issuers);
        }
        String sigType;
        if (keyType.contains("_")) {
            int k = keyType.indexOf('_');
            sigType = keyType.substring(k + 1);
            keyType = keyType.substring(0, k);
        } else {
            sigType = null;
        }

        X500Principal[] x500Issuers = (X500Principal[])issuers;
        // the algorithm below does not produce duplicates, so avoid Set
        List<String> aliases = new ArrayList<>();

        for (Map.Entry<String,X509Credentials> entry :
                                                credentialsMap.entrySet()) {

            String alias = entry.getKey();
            X509Credentials credentials = entry.getValue();
            X509Certificate[] certs = credentials.certificates;

            if (!keyType.equals(certs[0].getPublicKey().getAlgorithm())) {
                continue;
            }
            if (sigType != null) {
                if (certs.length > 1) {
                    // if possible, check the public key in the issuer cert
                    if (!sigType.equals(
                            certs[1].getPublicKey().getAlgorithm())) {
                        continue;
                    }
                } else {
                    // Check the signature algorithm of the certificate itself.
                    // Look for the "withRSA" in "SHA1withRSA", etc.
                    String sigAlgName =
                        certs[0].getSigAlgName().toUpperCase(Locale.ENGLISH);
                    String pattern = "WITH" +
                        sigType.toUpperCase(Locale.ENGLISH);
                    if (!sigAlgName.contains(pattern)) {
                        continue;
                    }
                }
            }

            if (issuers.length == 0) {
                // no issuer specified, match all
                aliases.add(alias);
                if (SSLLogger.isOn && SSLLogger.isOn("keymanager")) {
                    SSLLogger.fine("matching alias: " + alias);
                }
            } else {
                Set<X500Principal> certIssuers =
                                        credentials.getIssuerX500Principals();
                for (int i = 0; i < x500Issuers.length; i++) {
                    if (certIssuers.contains(issuers[i])) {
                        aliases.add(alias);
                        if (SSLLogger.isOn && SSLLogger.isOn("keymanager")) {
                            SSLLogger.fine("matching alias: " + alias);
                        }
                        break;
                    }
                }
            }
        }

        String[] aliasStrings = aliases.toArray(STRING0);
        return ((aliasStrings.length == 0) ? null : aliasStrings);
    }

    /*
     * Convert an array of Principals to an array of X500Principals, if
     * possible. Principals that cannot be converted are ignored.
     */
    private static X500Principal[] convertPrincipals(Principal[] principals) {
        List<X500Principal> list = new ArrayList<>(principals.length);
        for (int i = 0; i < principals.length; i++) {
            Principal p = principals[i];
            if (p instanceof X500Principal) {
                list.add((X500Principal)p);
            } else {
                try {
                    list.add(new X500Principal(p.getName()));
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }
        return list.toArray(new X500Principal[0]);
    }
}
