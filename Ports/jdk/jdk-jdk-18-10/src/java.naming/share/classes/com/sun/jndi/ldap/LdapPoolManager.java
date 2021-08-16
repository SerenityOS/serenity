/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.naming.ldap.Control;
import javax.naming.NamingException;
import javax.naming.CommunicationException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.jndi.ldap.pool.PoolCleaner;
import com.sun.jndi.ldap.pool.Pool;
import jdk.internal.misc.InnocuousThread;

/**
 * Contains utilities for managing connection pools of LdapClient.
 * Contains method for
 * - checking whether attempted connection creation may be pooled
 * - creating a pooled connection
 * - closing idle connections.
 *
 * If a timeout period has been configured, then it will automatically
 * close and remove idle connections (those that have not been
 * used for the duration of the timeout period).
 *
 * @author Rosanna Lee
 */

public final class LdapPoolManager {
    private static final String DEBUG =
        "com.sun.jndi.ldap.connect.pool.debug";

    public static final boolean debug =
        "all".equalsIgnoreCase(getProperty(DEBUG, null));

    public static final boolean trace = debug ||
        "fine".equalsIgnoreCase(getProperty(DEBUG, null));

    // ---------- System properties for connection pooling

    // Authentication mechanisms of connections that may be pooled
    private static final String POOL_AUTH =
        "com.sun.jndi.ldap.connect.pool.authentication";

    // Protocol types of connections that may be pooled
    private static final String POOL_PROTOCOL =
        "com.sun.jndi.ldap.connect.pool.protocol";

    // Maximum number of identical connections per pool
    private static final String MAX_POOL_SIZE =
        "com.sun.jndi.ldap.connect.pool.maxsize";

    // Preferred number of identical connections per pool
    private static final String PREF_POOL_SIZE =
        "com.sun.jndi.ldap.connect.pool.prefsize";

    // Initial number of identical connections per pool
    private static final String INIT_POOL_SIZE =
        "com.sun.jndi.ldap.connect.pool.initsize";

    // Milliseconds to wait before closing idle connections
    private static final String POOL_TIMEOUT =
        "com.sun.jndi.ldap.connect.pool.timeout";

    // Properties for DIGEST
    private static final String SASL_CALLBACK =
        "java.naming.security.sasl.callback";

    // --------- Constants
    private static final int DEFAULT_MAX_POOL_SIZE = 0;
    private static final int DEFAULT_PREF_POOL_SIZE = 0;
    private static final int DEFAULT_INIT_POOL_SIZE = 1;
    private static final int DEFAULT_TIMEOUT = 0;    // no timeout
    private static final String DEFAULT_AUTH_MECHS = "none simple";
    private static final String DEFAULT_PROTOCOLS = "plain";

    private static final int NONE = 0;    // indices into pools
    private static final int SIMPLE = 1;
    private static final int DIGEST = 2;

    // --------- static fields
    private static final long idleTimeout;// ms to wait before closing idle conn
    private static final int maxSize;     // max num of identical conns/pool
    private static final int prefSize;    // preferred num of identical conns/pool
    private static final int initSize;    // initial num of identical conns/pool

    private static boolean supportPlainProtocol = false;
    private static boolean supportSslProtocol = false;

    // List of pools used for different auth types
    private static final Pool[] pools = new Pool[3];

    static {
        maxSize = getInteger(MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);

        prefSize = getInteger(PREF_POOL_SIZE, DEFAULT_PREF_POOL_SIZE);

        initSize = getInteger(INIT_POOL_SIZE, DEFAULT_INIT_POOL_SIZE);

        idleTimeout = getLong(POOL_TIMEOUT, DEFAULT_TIMEOUT);

        // Determine supported authentication mechanisms
        String str = getProperty(POOL_AUTH, DEFAULT_AUTH_MECHS);
        StringTokenizer parser = new StringTokenizer(str);
        int count = parser.countTokens();
        String mech;
        int p;
        for (int i = 0; i < count; i++) {
            mech = parser.nextToken().toLowerCase(Locale.ENGLISH);
            if (mech.equals("anonymous")) {
                mech = "none";
            }

            p = findPool(mech);
            if (p >= 0 && pools[p] == null) {
                pools[p] = new Pool(initSize, prefSize, maxSize);
            }
        }

        // Determine supported protocols
        str= getProperty(POOL_PROTOCOL, DEFAULT_PROTOCOLS);
        parser = new StringTokenizer(str);
        count = parser.countTokens();
        String proto;
        for (int i = 0; i < count; i++) {
            proto = parser.nextToken();
            if ("plain".equalsIgnoreCase(proto)) {
                supportPlainProtocol = true;
            } else if ("ssl".equalsIgnoreCase(proto)) {
                supportSslProtocol = true;
            } else {
                // ignore
            }
        }

        if (idleTimeout > 0) {
            startCleanerThread();
        }

        if (debug) {
            showStats(System.err);
        }
    }

    @SuppressWarnings("removal")
    private static void startCleanerThread() {
        // Create cleaner to expire idle connections
        PrivilegedAction<Void> pa = new PrivilegedAction<Void>() {
            public Void run() {
                Thread t = InnocuousThread.newSystemThread(
                        "LDAP PoolCleaner",
                        new PoolCleaner(idleTimeout, pools));
                assert t.getContextClassLoader() == null;
                t.setDaemon(true);
                t.start();
                return null;
            }};
        AccessController.doPrivileged(pa);
    }

    // Cannot instantiate one of these
    private LdapPoolManager() {
    }

    /**
     * Find the index of the pool for the specified mechanism. If not
     * one of "none", "simple", "DIGEST-MD5", or "GSSAPI",
     * return -1.
     * @param mech mechanism type
     */
    private static int findPool(String mech) {
        if ("none".equalsIgnoreCase(mech)) {
            return NONE;
        } else if ("simple".equalsIgnoreCase(mech)) {
            return SIMPLE;
        } else if ("digest-md5".equalsIgnoreCase(mech)) {
            return DIGEST;
        }
        return -1;
    }

    /**
     * Determines whether pooling is allowed given information on how
     * the connection will be used.
     *
     * Non-configurable rejections:
     * - nonstandard socketFactory has been specified: the pool manager
     *   cannot track input or parameters used by the socket factory and
     *   thus has no way of determining whether two connection requests
     *   are equivalent. Maybe in the future it might add a list of allowed
     *   socket factories to be configured
     * - trace enabled (except when debugging)
     * - for Digest authentication, if a callback handler has been specified:
     *  the pool manager cannot track input collected by the handler
     *  and thus has no way of determining whether two connection requests are
     *  equivalent. Maybe in the future it might add a list of allowed
     *  callback handlers.
     *
     * Configurable tests:
     * - Pooling for the requested protocol (plain or ssl) is supported
     * - Pooling for the requested authentication mechanism is supported
     *
     */
    static boolean isPoolingAllowed(String socketFactory, OutputStream trace,
        String authMech, String protocol, Hashtable<?,?> env)
                throws NamingException {

        if (trace != null && !debug

                // Requesting plain protocol but it is not supported
                || (protocol == null && !supportPlainProtocol)

                // Requesting ssl protocol but it is not supported
                || ("ssl".equalsIgnoreCase(protocol) && !supportSslProtocol)) {

            d("Pooling disallowed due to tracing or unsupported pooling of protocol");
            return false;
        }
        // pooling of custom socket factory is possible only if the
        // socket factory interface implements java.util.comparator
        String COMPARATOR = "java.util.Comparator";
        boolean foundSockCmp = false;
        if ((socketFactory != null) &&
             !socketFactory.equals(LdapCtx.DEFAULT_SSL_FACTORY)) {
            try {
                Class<?> socketFactoryClass = Obj.helper.loadClass(socketFactory);
                Class<?>[] interfaces = socketFactoryClass.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    if (interfaces[i].getCanonicalName().equals(COMPARATOR)) {
                        foundSockCmp = true;
                    }
                }
            } catch (Exception e) {
                CommunicationException ce =
                    new CommunicationException("Loading the socket factory");
                ce.setRootCause(e);
                throw ce;
            }
            if (!foundSockCmp) {
                return false;
            }
        }
        // Cannot use pooling if authMech is not a supported mechs
        // Cannot use pooling if authMech contains multiple mechs
        int p = findPool(authMech);
        if (p < 0 || pools[p] == null) {
            d("authmech not found: ", authMech);

            return false;
        }

        d("using authmech: ", authMech);

        switch (p) {
        case NONE:
        case SIMPLE:
            return true;

        case DIGEST:
            // Provider won't be able to determine connection identity
            // if an alternate callback handler is used
            return (env == null || env.get(SASL_CALLBACK) == null);
        }
        return false;
    }

    /**
     * Obtains a pooled connection that either already exists or is
     * newly created using the parameters supplied. If it is newly
     * created, it needs to go through the authentication checks to
     * determine whether an LDAP bind is necessary.
     *
     * Caller needs to invoke ldapClient.authenticateCalled() to
     * determine whether ldapClient.authenticate() needs to be invoked.
     * Caller has that responsibility because caller needs to deal
     * with the LDAP bind response, which might involve referrals,
     * response controls, errors, etc. This method is responsible only
     * for establishing the connection.
     *
     * @return an LdapClient that is pooled.
     */
    static LdapClient getLdapClient(String host, int port, String socketFactory,
        int connTimeout, int readTimeout, OutputStream trace, int version,
        String authMech, Control[] ctls, String protocol, String user,
        Object passwd, Hashtable<?,?> env) throws NamingException {

        // Create base identity for LdapClient
        ClientId id = null;
        Pool pool;

        int p = findPool(authMech);
        if (p < 0 || (pool=pools[p]) == null) {
            throw new IllegalArgumentException(
                "Attempting to use pooling for an unsupported mechanism: " +
                authMech);
        }
        switch (p) {
        case NONE:
            id = new ClientId(version, host, port, protocol,
                        ctls, trace, socketFactory);
            break;

        case SIMPLE:
            // Add identity information used in simple authentication
            id = new SimpleClientId(version, host, port, protocol,
                ctls, trace, socketFactory, user, passwd);
            break;

        case DIGEST:
            // Add user/passwd/realm/authzid/qop/strength/maxbuf/mutual/policy*
            id = new DigestClientId(version, host, port, protocol,
                ctls, trace, socketFactory, user, passwd, env);
            break;
        }

        return (LdapClient) pool.getPooledConnection(id, connTimeout,
            new LdapClientFactory(host, port, socketFactory, connTimeout,
                                readTimeout, trace));
    }

    public static void showStats(PrintStream out) {
        out.println("***** start *****");
        out.println("idle timeout: " + idleTimeout);
        out.println("maximum pool size: " + maxSize);
        out.println("preferred pool size: " + prefSize);
        out.println("initial pool size: " + initSize);
        out.println("protocol types: " + (supportPlainProtocol ? "plain " : "") +
            (supportSslProtocol ? "ssl" : ""));
        out.println("authentication types: " +
            (pools[NONE] != null ? "none " : "") +
            (pools[SIMPLE] != null ? "simple " : "") +
            (pools[DIGEST] != null ? "DIGEST-MD5 " : ""));

        for (int i = 0; i < pools.length; i++) {
            if (pools[i] != null) {
                out.println(
                    (i == NONE ? "anonymous pools" :
                        i == SIMPLE ? "simple auth pools" :
                        i == DIGEST ? "digest pools" : "")
                            + ":");
                pools[i].showStats(out);
            }
        }
        out.println("***** end *****");
    }

    /**
     * Closes idle connections idle since specified time.
     *
     * @param threshold Close connections idle since this time, as
     * specified in milliseconds since "the epoch".
     * @see java.util.Date
     */
    public static void expire(long threshold) {
        for (int i = 0; i < pools.length; i++) {
            if (pools[i] != null) {
                pools[i].expire(threshold);
            }
        }
    }

    private static void d(String msg) {
        if (debug) {
            System.err.println("LdapPoolManager: " + msg);
        }
    }

    private static void d(String msg, String o) {
        if (debug) {
            System.err.println("LdapPoolManager: " + msg + o);
        }
    }

    @SuppressWarnings("removal")
    private static final String getProperty(final String propName, final String defVal) {
        PrivilegedAction<String> pa = () -> System.getProperty(propName, defVal);
        return AccessController.doPrivileged(pa);
    }

    @SuppressWarnings("removal")
    private static final int getInteger(final String propName, final int defVal) {
        PrivilegedAction<Integer> pa = () -> Integer.getInteger(propName, defVal);
        return AccessController.doPrivileged(pa);
    }

    @SuppressWarnings("removal")
    private static final long getLong(final String propName, final long defVal) {
        PrivilegedAction<Long> pa = () -> Long.getLong(propName, defVal);
        return AccessController.doPrivileged(pa);
    }
}
