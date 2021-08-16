/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.internal.interfaces;

import javax.crypto.SecretKey;

/**
 * An SSL/TLS master secret key. It is a <code>SecretKey</code> that optionally
 * contains protocol version information that is used to detect version
 * rollback attacks during the SSL/TLS handshake.
 *
 * <p>Implementation of this interface are returned by the
 * <code>generateKey()</code> method of KeyGenerators of the type
 * "TlsMasterSecret".
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @deprecated Sun JDK internal use only --- WILL BE REMOVED in a future
 * release.
 */
@Deprecated
public interface TlsMasterSecret extends SecretKey {

    /***
     * @deprecated A {@code serialVersionUID} field in an interface is
     * ineffectual. Do not use; no replacement.
     */
    @Deprecated
    @SuppressWarnings("serial")
    @java.io.Serial
    public static final long serialVersionUID = -461748105810469773L;

    /**
     * Returns the major version number encapsulated in the premaster secret
     * this master secret was derived from, or -1 if it is not available.
     *
     * <p>This information will only usually only be available when RSA
     * was used as the key exchange algorithm.
     *
     * @return the major version number, or -1 if it is not available
     */
    public int getMajorVersion();

    /**
     * Returns the minor version number encapsulated in the premaster secret
     * this master secret was derived from, or -1 if it is not available.
     *
     * <p>This information will only usually only be available when RSA
     * was used as the key exchange algorithm.
     *
     * @return the major version number, or -1 if it is not available
     */
    public int getMinorVersion();

}
