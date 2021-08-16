/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.net.ssl;

import java.security.PrivateKey;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.net.Socket;

/**
 * Instances of this interface manage which X509 certificate-based
 * key pairs are used to authenticate the local side of a secure
 * socket.
 * <P>
 * During secure socket negotiations, implementations
 * call methods in this interface to:
 * <UL>
 * <LI> determine the set of aliases that are available for negotiations
 *      based on the criteria presented,
 * <LI> select the <i> best alias</i> based on
 *      the criteria presented, and
 * <LI> obtain the corresponding key material for given aliases.
 * </UL>
 * <P>
 * Note: the X509ExtendedKeyManager should be used in favor of this
 * class.
 *
 * @since 1.4
 */
public interface X509KeyManager extends KeyManager {
    /**
     * Get the matching aliases for authenticating the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name
     * @param issuers the list of acceptable CA issuer subject names,
     *          or null if it does not matter which issuers are used.
     * @return an array of the matching alias names, or null if there
     *          were no matches.
     */
    public String[] getClientAliases(String keyType, Principal[] issuers);

    /**
     * Choose an alias to authenticate the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name(s), ordered
     *          with the most-preferred key type first.
     * @param issuers the list of acceptable CA issuer subject names
     *           or null if it does not matter which issuers are used.
     * @param socket the socket to be used for this connection.  This
     *          parameter can be null, which indicates that
     *          implementations are free to select an alias applicable
     *          to any socket.
     * @return the alias name for the desired key, or null if there
     *          are no matches.
     */
    public String chooseClientAlias(String[] keyType, Principal[] issuers,
        Socket socket);

    /**
     * Get the matching aliases for authenticating the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name
     * @param issuers the list of acceptable CA issuer subject names
     *          or null if it does not matter which issuers are used.
     * @return an array of the matching alias names, or null
     *          if there were no matches.
     */
    public String[] getServerAliases(String keyType, Principal[] issuers);

    /**
     * Choose an alias to authenticate the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name.
     * @param issuers the list of acceptable CA issuer subject names
     *          or null if it does not matter which issuers are used.
     * @param socket the socket to be used for this connection.  This
     *          parameter can be null, which indicates that
     *          implementations are free to select an alias applicable
     *          to any socket.
     * @return the alias name for the desired key, or null if there
     *          are no matches.
     */
    public String chooseServerAlias(String keyType, Principal[] issuers,
        Socket socket);

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     * @return the certificate chain (ordered with the user's certificate first
     *          and the root certificate authority last), or null
     *          if the alias can't be found.
     */
    public X509Certificate[] getCertificateChain(String alias);

    /**
     * Returns the key associated with the given alias.
     *
     * @param alias the alias name
     * @return the requested key, or null if the alias can't be found.
     */
    public PrivateKey getPrivateKey(String alias);
}
