/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import sun.net.www.HeaderParser;
import sun.nio.cs.ISO_8859_1;
import sun.nio.cs.UTF_8;

/**
 * BasicAuthentication: Encapsulate an http server authentication using
 * the "basic" scheme.
 *
 * @author Bill Foote
 */


class BasicAuthentication extends AuthenticationInfo {

    @java.io.Serial
    private static final long serialVersionUID = 100L;

    /** The authentication string for this host, port, and realm.  This is
        a simple BASE64 encoding of "login:password".    */
    final String auth;

    /**
     * Create a BasicAuthentication
     */
    public BasicAuthentication(boolean isProxy, String host, int port,
                               String realm, PasswordAuthentication pw,
                               boolean isUTF8, String authenticatorKey) {
        super(isProxy ? PROXY_AUTHENTICATION : SERVER_AUTHENTICATION,
              AuthScheme.BASIC, host, port, realm,
              Objects.requireNonNull(authenticatorKey));
        this.auth = authValueFrom(pw, isUTF8);
        this.pw = pw;
    }

    /**
     * Create a BasicAuthentication
     */
    public BasicAuthentication(boolean isProxy, String host, int port,
                               String realm, String auth,
                               String authenticatorKey) {
        super(isProxy ? PROXY_AUTHENTICATION : SERVER_AUTHENTICATION,
              AuthScheme.BASIC, host, port, realm,
              Objects.requireNonNull(authenticatorKey));
        this.auth = "Basic " + auth;
    }

    /**
     * Create a BasicAuthentication
     */
    public BasicAuthentication(boolean isProxy, URL url, String realm,
                               PasswordAuthentication pw, boolean isUTF8,
                               String authenticatorKey) {
        super(isProxy ? PROXY_AUTHENTICATION : SERVER_AUTHENTICATION,
              AuthScheme.BASIC, url, realm,
              Objects.requireNonNull(authenticatorKey));
        this.auth = authValueFrom(pw, isUTF8);
        this.pw = pw;
    }

    private static String authValueFrom(PasswordAuthentication pw, boolean isUTF8) {
        String plain = pw.getUserName() + ":";
        char[] password = pw.getPassword();
        CharBuffer cbuf = CharBuffer.allocate(plain.length() + password.length);
        cbuf.put(plain).put(password).flip();
        Charset charset = isUTF8 ? UTF_8.INSTANCE : ISO_8859_1.INSTANCE;
        ByteBuffer buf = charset.encode(cbuf);
        ByteBuffer enc = Base64.getEncoder().encode(buf);
        String ret = "Basic " + new String(enc.array(), enc.position(), enc.remaining(),
                ISO_8859_1.INSTANCE);
        Arrays.fill(buf.array(), (byte) 0);
        Arrays.fill(enc.array(), (byte) 0);
        Arrays.fill(cbuf.array(), (char) 0);
        return ret;
    }

    /**
     * Create a BasicAuthentication
     */
    public BasicAuthentication(boolean isProxy, URL url, String realm,
                               String auth, String authenticatorKey) {
        super(isProxy ? PROXY_AUTHENTICATION : SERVER_AUTHENTICATION,
              AuthScheme.BASIC, url, realm,
              Objects.requireNonNull(authenticatorKey));
        this.auth = "Basic " + auth;
    }

    /**
     * @return true if this authentication supports preemptive authorization
     */
    @Override
    public boolean supportsPreemptiveAuthorization() {
        return true;
    }

    /**
     * Set header(s) on the given connection. This will only be called for
     * definitive (i.e. non-preemptive) authorization.
     * @param conn The connection to apply the header(s) to
     * @param p A source of header values for this connection, if needed.
     * @param raw The raw header values for this connection, if needed.
     * @return true if all goes well, false if no headers were set.
     */
    @Override
    public boolean setHeaders(HttpURLConnection conn, HeaderParser p, String raw) {
        // no need to synchronize here:
        //   already locked by s.n.w.p.h.HttpURLConnection
        assert conn.isLockHeldByCurrentThread();
        conn.setAuthenticationProperty(getHeaderName(), getHeaderValue(null,null));
        return true;
    }

    /**
     * @return the value of the HTTP header this authentication wants set
     */
    @Override
    public String getHeaderValue(URL url, String method) {
        /* For Basic the authorization string does not depend on the request URL
         * or the request method
         */
        return auth;
    }

    /**
     * For Basic Authentication, the security parameters can never be stale.
     * In other words there is no possibility to reuse the credentials.
     * They are always either valid or invalid.
     */
    @Override
    public boolean isAuthorizationStale (String header) {
        return false;
    }

    /**
     * @return the common root path between npath and path.
     * This is used to detect when we have an authentication for two
     * paths and the root of th authentication space is the common root.
     */

    static String getRootPath(String npath, String opath) {
        int index = 0;
        int toindex;

        /* Must normalize so we don't get confused by ../ and ./ segments */
        try {
            npath = new URI (npath).normalize().getPath();
            opath = new URI (opath).normalize().getPath();
        } catch (URISyntaxException e) {
            /* ignore error and use the old value */
        }

        while (index < opath.length()) {
            toindex = opath.indexOf('/', index+1);
            if (toindex != -1 && opath.regionMatches(0, npath, 0, toindex+1))
                index = toindex;
            else
                return opath.substring(0, index+1);
        }
        /*should not reach here. If we do simply return npath*/
        return npath;
    }
}
