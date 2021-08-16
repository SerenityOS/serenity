/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.net.PasswordAuthentication;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import sun.util.logging.PlatformLogger;

/**
 * Proxy class for loading NTLMAuthentication, so as to remove static
 * dependancy.
 */
class NTLMAuthenticationProxy {
    private static Method supportsTA;
    private static Method isTrustedSite;
    private static final String clazzStr = "sun.net.www.protocol.http.ntlm.NTLMAuthentication";
    private static final String supportsTAStr = "supportsTransparentAuth";
    private static final String isTrustedSiteStr = "isTrustedSite";

    static final NTLMAuthenticationProxy proxy = tryLoadNTLMAuthentication();
    static final boolean supported = proxy != null ? true : false;
    static final boolean supportsTransparentAuth = supported ? supportsTransparentAuth() : false;

    private final Constructor<? extends AuthenticationInfo> fourArgCtr;
    private final Constructor<? extends AuthenticationInfo> sixArgCtr;

    private NTLMAuthenticationProxy(Constructor<? extends AuthenticationInfo> fourArgCtr,
                                    Constructor<? extends AuthenticationInfo> sixArgCtr) {
        this.fourArgCtr = fourArgCtr;
        this.sixArgCtr = sixArgCtr;
    }


    AuthenticationInfo create(boolean isProxy,
                              URL url,
                              PasswordAuthentication pw,
                              String authenticatorKey) {
        try {
            return fourArgCtr.newInstance(isProxy, url, pw, authenticatorKey);
        } catch (ReflectiveOperationException roe) {
            finest(roe);
        }

        return null;
    }

    AuthenticationInfo create(boolean isProxy,
                              String host,
                              int port,
                              PasswordAuthentication pw,
                              String authenticatorKey) {
        try {
            return sixArgCtr.newInstance(isProxy, host, port, pw, authenticatorKey);
        } catch (ReflectiveOperationException roe) {
            finest(roe);
        }

        return null;
    }

    /* Returns true if the NTLM implementation supports transparent
     * authentication (try with the current users credentials before
     * prompting for username and password, etc).
     */
    private static boolean supportsTransparentAuth() {
        try {
            return (Boolean)supportsTA.invoke(null);
        } catch (ReflectiveOperationException roe) {
            finest(roe);
        }

        return false;
    }

    /* Transparent authentication should only be tried with a trusted
     * site ( when running in a secure environment ).
     */
    public static boolean isTrustedSite(URL url) {
        try {
            return (Boolean)isTrustedSite.invoke(null, url);
        } catch (ReflectiveOperationException roe) {
            finest(roe);
        }

        return false;
    }

    /**
     * Loads the NTLM authentiation implementation through reflection. If
     * the class is present, then it must have the required constructors and
     * method. Otherwise, it is considered an error.
     */
    @SuppressWarnings("unchecked")
    private static NTLMAuthenticationProxy tryLoadNTLMAuthentication() {
        Class<? extends AuthenticationInfo> cl;
        Constructor<? extends AuthenticationInfo> fourArg, sixArg;
        try {
            cl = (Class<? extends AuthenticationInfo>)Class.forName(clazzStr, true, null);
            if (cl != null) {
                fourArg = cl.getConstructor(boolean.class,
                                             URL.class,
                                             PasswordAuthentication.class,
                                             String.class);
                sixArg = cl.getConstructor(boolean.class,
                                            String.class,
                                            int.class,
                                            PasswordAuthentication.class,
                                            String.class);
                supportsTA = cl.getDeclaredMethod(supportsTAStr);
                isTrustedSite = cl.getDeclaredMethod(isTrustedSiteStr, java.net.URL.class);
                return new NTLMAuthenticationProxy(fourArg,
                                                   sixArg);
            }
        } catch (ClassNotFoundException cnfe) {
            finest(cnfe);
        } catch (ReflectiveOperationException roe) {
            throw new AssertionError(roe);
        }

        return null;
    }

    static void finest(Exception e) {
        PlatformLogger logger = HttpURLConnection.getHttpLogger();
        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
            logger.finest("NTLMAuthenticationProxy: " + e);
        }
    }
}
