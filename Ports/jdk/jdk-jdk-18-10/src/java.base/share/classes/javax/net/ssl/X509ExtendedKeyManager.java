/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Principal;

/**
 * Abstract class that provides for extension of the X509KeyManager
 * interface.
 * <P>
 * Methods in this class should be overridden to provide actual
 * implementations.
 *
 * @since 1.5
 * @author Brad R. Wetmore
 */
public abstract class X509ExtendedKeyManager implements X509KeyManager {

    /**
     * Constructor used by subclasses only.
     */
    protected X509ExtendedKeyManager() {
    }

    /**
     * Choose an alias to authenticate the client side of an
     * <code>SSLEngine</code> connection given the public key type
     * and the list of certificate issuer authorities recognized by
     * the peer (if any).
     * <P>
     * The default implementation returns null.
     *
     * @param keyType the key algorithm type name(s), ordered
     *          with the most-preferred key type first.
     * @param issuers the list of acceptable CA issuer subject names
     *          or null if it does not matter which issuers are used.
     * @param engine the <code>SSLEngine</code> to be used for this
     *          connection.  This parameter can be null, which indicates
     *          that implementations of this interface are free to
     *          select an alias applicable to any engine.
     * @return the alias name for the desired key, or null if there
     *          are no matches.
     */
    public String chooseEngineClientAlias(String[] keyType,
            Principal[] issuers, SSLEngine engine) {
        return null;
    }

    /**
     * Choose an alias to authenticate the server side of an
     * <code>SSLEngine</code> connection given the public key type
     * and the list of certificate issuer authorities recognized by
     * the peer (if any).
     * <P>
     * The default implementation returns null.
     *
     * @param keyType the key algorithm type name.
     * @param issuers the list of acceptable CA issuer subject names
     *          or null if it does not matter which issuers are used.
     * @param engine the <code>SSLEngine</code> to be used for this
     *          connection.  This parameter can be null, which indicates
     *          that implementations of this interface are free to
     *          select an alias applicable to any engine.
     * @return the alias name for the desired key, or null if there
     *          are no matches.
     */
    public String chooseEngineServerAlias(String keyType,
            Principal[] issuers, SSLEngine engine) {
        return null;
    }

}
