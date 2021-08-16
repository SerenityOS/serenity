/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.util.*;
import java.security.ProviderException;

import sun.security.util.Debug;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Session manager. There is one session manager object per PKCS#11
 * provider. It allows code to checkout a session, release it
 * back to the pool, or force it to be closed.
 *
 * The session manager pools sessions to minimize the number of
 * C_OpenSession() and C_CloseSession() that have to be made. It
 * maintains two pools: one for "object" sessions and one for
 * "operation" sessions.
 *
 * The reason for this separation is how PKCS#11 deals with session objects.
 * It defines that when a session is closed, all objects created within
 * that session are destroyed. In other words, we may never close a session
 * while a Key created it in is still in use. We would like to keep the
 * number of such sessions low. Note that we occasionally want to explicitly
 * close a session, see P11Signature.
 *
 * NOTE that sessions obtained from this class SHOULD be returned using
 * either releaseSession() or closeSession() using a finally block when
 * not needed anymore. Otherwise, they will be left for cleanup via the
 * PhantomReference mechanism when GC kicks in, but it's best not to rely
 * on that since GC may not run timely enough since the native PKCS11 library
 * is also consuming memory.
 *
 * Note that sessions are automatically closed when they are not used for a
 * period of time, see Session.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class SessionManager {

    private static final int DEFAULT_MAX_SESSIONS = 32;

    private static final Debug debug = Debug.getInstance("pkcs11");

    // token instance
    private final Token token;

    // maximum number of sessions to open with this token
    private final int maxSessions;

    // total number of active sessions
    private AtomicInteger activeSessions = new AtomicInteger();

    // pool of available object sessions
    private final Pool objSessions;

    // pool of available operation sessions
    private final Pool opSessions;

    // maximum number of active sessions during this invocation, for debugging
    private int maxActiveSessions;
    private Object maxActiveSessionsLock;

    // flags to use in the C_OpenSession() call
    private final long openSessionFlags;

    SessionManager(Token token) {
        long n;
        if (token.isWriteProtected()) {
            openSessionFlags = CKF_SERIAL_SESSION;
            n = token.tokenInfo.ulMaxSessionCount;
        } else {
            openSessionFlags = CKF_SERIAL_SESSION | CKF_RW_SESSION;
            n = token.tokenInfo.ulMaxRwSessionCount;
        }
        if (n == CK_EFFECTIVELY_INFINITE) {
            n = Integer.MAX_VALUE;
        } else if ((n == CK_UNAVAILABLE_INFORMATION) || (n < 0)) {
            // choose an arbitrary concrete value
            n = DEFAULT_MAX_SESSIONS;
        }
        maxSessions = (int)Math.min(n, Integer.MAX_VALUE);
        this.token = token;
        this.objSessions = new Pool(this, true);
        this.opSessions = new Pool(this, false);
        if (debug != null) {
            maxActiveSessionsLock = new Object();
        }
    }

    // returns whether only a fairly low number of sessions are
    // supported by this token.
    boolean lowMaxSessions() {
        return (maxSessions <= DEFAULT_MAX_SESSIONS);
    }

    Session getObjSession() throws PKCS11Exception {
        Session session = objSessions.poll();
        if (session != null) {
            return ensureValid(session);
        }
        session = opSessions.poll();
        if (session != null) {
            return ensureValid(session);
        }
        session = openSession();
        return ensureValid(session);
    }

    Session getOpSession() throws PKCS11Exception {
        Session session = opSessions.poll();
        if (session != null) {
            return ensureValid(session);
        }
        // create a new session rather than re-using an obj session
        // that avoids potential expensive cancels() for Signatures & RSACipher
        if (maxSessions == Integer.MAX_VALUE ||
                activeSessions.get() < maxSessions) {
            session = openSession();
            return ensureValid(session);
        }
        session = objSessions.poll();
        if (session != null) {
            return ensureValid(session);
        }
        throw new ProviderException("Could not obtain session");
    }

    private Session ensureValid(Session session) {
        session.id();
        return session;
    }

    Session killSession(Session session) {
        if ((session == null) || (token.isValid() == false)) {
            return null;
        }
        if (debug != null) {
            String location = new Exception().getStackTrace()[2].toString();
            System.out.println("Killing session (" + location + ") active: "
                + activeSessions.get());
        }

        session.kill();
        activeSessions.decrementAndGet();
        return null;
    }

    Session releaseSession(Session session) {
        if ((session == null) || (token.isValid() == false)) {
            return null;
        }
        if (session.hasObjects()) {
            objSessions.release(session);
        } else {
            opSessions.release(session);
        }
        return null;
    }

    void clearPools() {
        objSessions.closeAll();
        opSessions.closeAll();
    }

    void demoteObjSession(Session session) {
        if (token.isValid() == false) {
            return;
        }
        if (debug != null) {
            System.out.println("Demoting session, active: " +
                activeSessions.get());
        }

        boolean present = objSessions.remove(session);
        if (present == false) {
            // session is currently in use
            // will be added to correct pool on release, nothing to do now
            return;
        }
        opSessions.release(session);
    }

    private Session openSession() throws PKCS11Exception {
        if ((maxSessions != Integer.MAX_VALUE) &&
                (activeSessions.get() >= maxSessions)) {
            throw new ProviderException("No more sessions available");
        }

        long id = token.p11.C_OpenSession
                    (token.provider.slotID, openSessionFlags, null, null);
        Session session = new Session(token, id);
        activeSessions.incrementAndGet();
        if (debug != null) {
            synchronized(maxActiveSessionsLock) {
                if (activeSessions.get() > maxActiveSessions) {
                    maxActiveSessions = activeSessions.get();
                    if (maxActiveSessions % 10 == 0) {
                        System.out.println("Open sessions: " + maxActiveSessions);
                    }
                }
            }
        }
        return session;
    }

    private void closeSession(Session session) {
        session.close();
        activeSessions.decrementAndGet();
    }

    public static final class Pool {

        private final SessionManager mgr;
        private final AbstractQueue<Session> pool;
        private final int SESSION_MAX = 5;
        private volatile boolean closed = false;

        // Object session pools can contain unlimited sessions.
        // Operation session pools are limited and enforced by the queue.
        Pool(SessionManager mgr, boolean obj) {
            this.mgr = mgr;
            if (obj) {
                pool = new LinkedBlockingQueue<Session>();
            } else {
                pool = new LinkedBlockingQueue<Session>(SESSION_MAX);
            }
        }

        boolean remove(Session session) {
            return pool.remove(session);
        }

        Session poll() {
            return pool.poll();
        }

        void release(Session session) {
            // Object session pools never return false, only Operation ones
            if (closed || !pool.offer(session)) {
                mgr.closeSession(session);
                free();
            }
        }

        // Free any old operation session if this queue is full
        void free() {
            // quick return path
            if (pool.size() == 0) return;

            int n = SESSION_MAX;
            int i = 0;
            Session oldestSession;
            long time = System.currentTimeMillis();
            // Check if the session head is too old and continue through pool
            // until only one is left.
            do {
                oldestSession = pool.peek();
                if (oldestSession == null || oldestSession.isLive(time) ||
                        !pool.remove(oldestSession)) {
                    break;
                }

                i++;
                mgr.closeSession(oldestSession);
            } while ((n - i) > 1);

            if (debug != null) {
                System.out.println("Closing " + i + " idle sessions, active: "
                        + mgr.activeSessions);
            }
        }

        // empty out all sessions inside 'pool' and close them.
        // however the Pool can still accept sessions
        void closeAll() {
            closed = true;
            Session s;
            while ((s = pool.poll()) != null) {
                mgr.killSession(s);
            }
        }
    }
}
