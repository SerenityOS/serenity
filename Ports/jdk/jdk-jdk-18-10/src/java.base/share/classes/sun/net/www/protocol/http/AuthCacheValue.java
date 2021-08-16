/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.http;

import java.io.Serializable;
import java.net.PasswordAuthentication;

/**
 * AuthCacheValue: interface to minimize exposure to authentication cache
 * for external users (i.e. plugin)
 *
 * @author Michael McMahon
 */

public abstract class AuthCacheValue implements Serializable {

    @java.io.Serial
    static final long serialVersionUID = 735249334068211611L;

    public enum Type {
        Proxy,
        Server
    };

    /**
     * Caches authentication info entered by user.  See cacheKey()
     */
    protected static AuthCache cache = new AuthCacheImpl();

    public static void setAuthCache (AuthCache map) {
        cache = map;
    }

    /* Package private ctor to prevent extension outside package */

    AuthCacheValue() {}

    /**
     * Proxy or Server
     */
    abstract Type getAuthType ();

    /**
     * Authentication scheme
     */
    abstract AuthScheme getAuthScheme();

   /**
    * name of server/proxy
    */
    abstract String getHost ();

   /**
    * portnumber of server/proxy
    */
    abstract int getPort();

   /**
    * realm of authentication if known
    */
    abstract String getRealm();

    /**
     * root path of realm or the request path if the root
     * is not known yet.
     */
    abstract String getPath();

    /**
     * returns http or https
     */
    abstract String getProtocolScheme();

    /**
     * the credentials associated with this authentication
     */
    abstract PasswordAuthentication credentials();
}
