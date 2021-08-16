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


package javax.net.ssl;

import java.util.Enumeration;


/**
 * A <code>SSLSessionContext</code> represents a set of
 * <code>SSLSession</code>s associated with a single entity. For example,
 * it could be associated with a server or client who participates in many
 * sessions concurrently.
 * <p>
 * Not all environments will contain session contexts.  For example, stateless
 * session resumption.
 * <p>
 * Session contexts may not contain all sessions. For example, stateless
 * sessions are not stored in the session context.
 * <p>
 * There are <code>SSLSessionContext</code> parameters that affect how
 * sessions are stored:
 * <UL>
 *      <LI>Sessions can be set to expire after a specified
 *      time limit.
 *      <LI>The number of sessions that can be stored in context
 *      can be limited.
 * </UL>
 * A session can be retrieved based on its session id, and all session id's
 * in a <code>SSLSessionContext</code> can be listed.
 *
 * @see SSLSession
 *
 * @since 1.4
 * @author Nathan Abramson
 * @author David Brownell
 */
public interface SSLSessionContext {

    /**
     * Returns the <code>SSLSession</code> bound to the specified session id.
     *
     * @param sessionId the Session identifier
     * @return the <code>SSLSession</code> or null if
     * the specified session id does not refer to a valid SSLSession.
     *
     * @throws NullPointerException if <code>sessionId</code> is null.
     */
    public SSLSession getSession(byte[] sessionId);

    /**
     * Returns an Enumeration of all known session id's grouped under this
     * <code>SSLSessionContext</code>.
     * <p>Session contexts may not contain all sessions. For example,
     * stateless sessions are not stored in the session context.
     *
     * @return an enumeration of all the Session id's
     */
    public Enumeration<byte[]> getIds();

    /**
     * Sets the timeout limit for <code>SSLSession</code> objects grouped
     * under this <code>SSLSessionContext</code>.
     * <p>
     * If the timeout limit is set to 't' seconds, a session exceeds the
     * timeout limit 't' seconds after its creation time.
     * When the timeout limit is exceeded for a session, the
     * <code>SSLSession</code> object is invalidated and future connections
     * cannot resume or rejoin the session.
     * A check for sessions exceeding the timeout is made immediately whenever
     * the timeout limit is changed for this <code>SSLSessionContext</code>.
     *
     * @apiNote Note that the JDK Implementation uses default values for both
     *          the session cache size and timeout.  See
     *          {@code getSessionCacheSize} and {@code getSessionTimeout} for
     *          more information.  Applications should consider their
     *          performance requirements and override the defaults if necessary.
     *
     * @param seconds the new session timeout limit in seconds; zero means
     *        there is no limit.
     *
     * @throws IllegalArgumentException if the timeout specified is {@code < 0}.
     *
     * @see #getSessionTimeout
     */
    public void setSessionTimeout(int seconds)
                 throws IllegalArgumentException;

    /**
     * Returns the timeout limit of <code>SSLSession</code> objects grouped
     * under this <code>SSLSessionContext</code>.
     * <p>
     * If the timeout limit is set to 't' seconds, a session exceeds the
     * timeout limit 't' seconds after its creation time.
     * When the timeout limit is exceeded for a session, the
     * <code>SSLSession</code> object is invalidated and future connections
     * cannot resume or rejoin the session.
     * A check for sessions exceeding the timeout limit is made immediately
     * whenever the timeout limit is changed for this
     * <code>SSLSessionContext</code>.
     *
     * @implNote The JDK implementation returns the session timeout as set by
     *           the {@code setSessionTimeout} method, or if not set, a default
     *           value of 86400 seconds (24 hours).
     *
     * @return the session timeout limit in seconds; zero means there is no
     *         limit.
     *
     * @see #setSessionTimeout
     */
    public int getSessionTimeout();

    /**
     * Sets the size of the cache used for storing <code>SSLSession</code>
     * objects grouped under this <code>SSLSessionContext</code>.
     *
     * @apiNote Note that the JDK Implementation uses default values for both
     *          the session cache size and timeout.  See
     *          {@code getSessionCacheSize} and {@code getSessionTimeout} for
     *          more information.  Applications should consider their
     *          performance requirements and override the defaults if necessary.
     *
     * @param size the new session cache size limit; zero means there is no
     *        limit.
     *
     * @throws IllegalArgumentException if the specified size is {@code < 0}.
     *
     * @see #getSessionCacheSize
     */
    public void setSessionCacheSize(int size)
                 throws IllegalArgumentException;

    /**
     * Returns the size of the cache used for storing <code>SSLSession</code>
     * objects grouped under this <code>SSLSessionContext</code>.
     *
     * @implNote The JDK implementation returns the cache size as set by
     *           the {@code setSessionCacheSize} method, or if not set, the
     *           value of the {@systemProperty javax.net.ssl.sessionCacheSize}
     *           system property.  If neither is set, it returns a default
     *           value of 20480.
     *
     * @return size of the session cache; zero means there is no size limit.
     *
     * @see #setSessionCacheSize
     */
    public int getSessionCacheSize();
}
