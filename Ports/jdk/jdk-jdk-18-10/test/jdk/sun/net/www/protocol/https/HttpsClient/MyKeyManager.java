/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import javax.net.ssl.X509KeyManager;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.net.Socket;
import javax.net.ssl.X509KeyManager;
import java.util.Set;

final class MyKeyManager implements X509KeyManager {
    private HashMap keyMap = new HashMap();
    private HashMap certChainMap = new HashMap();

    MyKeyManager(KeyStore ks, char[] password)
        throws KeyStoreException, NoSuchAlgorithmException,
        UnrecoverableKeyException
    {
        if (ks == null) {
            return;
        }

        Enumeration aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = (String)aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                Certificate[] certs;
                certs = ks.getCertificateChain(alias);
                if (certs != null && certs.length > 0 &&
                    certs[0] instanceof X509Certificate) {
                    if (!(certs instanceof X509Certificate[])) {
                        Certificate[] tmp = new X509Certificate[certs.length];
                        System.arraycopy(certs, 0, tmp, 0, certs.length);
                        certs = tmp;
                    }
                    Key key = ks.getKey(alias, password);
                    certChainMap.put(alias, certs);
                    keyMap.put(alias, key);
                }
            }
        }
    }

    /*
     * Choose an alias to authenticate the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers,
            Socket socket) {
        return "client";
    }

    /*
     * Get the matching aliases for authenticating the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        String[] s = new String[1];
        s[0] = "client";
        return s;
    }

    private HashMap serverAliasCache = new HashMap();

    /*
     * Choose an alias to authenticate the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    public synchronized String chooseServerAlias(String keyType,
            Principal[] issuers, Socket socket) {
        return "server";
    }

    /*
     * Get the matching aliases for authenticating the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        String[] s = new String[1];
        s[0] = "server";
        return s;
    }

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last)
     *
     * @exception KeyStoreException if the alias is invalid
     */
    public X509Certificate[] getCertificateChain(String alias) {
        Object chain;

        chain = certChainMap.get(alias);
        if (!(chain instanceof X509Certificate[]))
            return null;
        return (X509Certificate[]) chain;
    }

    /*
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     *
     * @return the requested key
     * @exception KeyStoreException if the alias is invalid
     */
    public PrivateKey getPrivateKey(String alias) {
        Object key;

        key = keyMap.get(alias);
        if (!(key instanceof PrivateKey))
            return null;
        return (PrivateKey)key;
    }
}
