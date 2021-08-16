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
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.lang.ref.*;

import java.security.*;
import javax.security.auth.login.LoginException;

import sun.security.jca.JCAUtil;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.TemplateManager.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.PKCS11Exception.*;

/**
 * PKCS#11 token.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
class Token implements Serializable {

    // need to be serializable to allow SecureRandom to be serialized
    private static final long serialVersionUID = 2541527649100571747L;

    // how often to check if the token is still present (in ms)
    // this is different from checking if a token has been inserted,
    // that is done in SunPKCS11. Currently 50 ms.
    private static final long CHECK_INTERVAL = 50;

    final SunPKCS11 provider;

    final PKCS11 p11;

    final Config config;

    final CK_TOKEN_INFO tokenInfo;

    // session manager to pool sessions
    final SessionManager sessionManager;

    // template manager to customize the attributes used when creating objects
    private final TemplateManager templateManager;

    // flag indicating whether we need to explicitly cancel operations
    // we started on the token. If false, we assume operations are
    // automatically cancelled once we start another one
    final boolean explicitCancel;

    // translation cache for secret keys
    final KeyCache secretCache;

    // translation cache for asymmetric keys (public and private)
    final KeyCache privateCache;

    // cached instances of the various key factories, initialized on demand
    private volatile P11KeyFactory rsaFactory, dsaFactory, dhFactory, ecFactory;

    // table which maps mechanisms to the corresponding cached
    // MechanismInfo objects
    private final Map<Long, CK_MECHANISM_INFO> mechInfoMap;

    // single SecureRandomSpi instance we use per token
    // initialized on demand (if supported)
    private volatile P11SecureRandom secureRandom;

    // single KeyStoreSpi instance we use per provider
    // initialized on demand
    private volatile P11KeyStore keyStore;

    // whether this token is a removable token
    private final boolean removable;

    // for removable tokens: whether this token is valid or has been removed
    private volatile boolean valid;

    // for removable tokens: time last checked for token presence
    private long lastPresentCheck;

    // unique token id, used for serialization only
    private byte[] tokenId;

    // flag indicating whether the token is write protected
    private boolean writeProtected;

    // flag indicating whether we are logged in
    private volatile boolean loggedIn;

    // time we last checked login status
    private long lastLoginCheck;

    // mutex for token-present-check
    private static final Object CHECK_LOCK = new Object();

    // object for indicating unsupported mechanism in 'mechInfoMap'
    private static final CK_MECHANISM_INFO INVALID_MECH =
        new CK_MECHANISM_INFO(0, 0, 0);

    // flag indicating whether the token supports raw secret key material import
    private Boolean supportsRawSecretKeyImport;

    Token(SunPKCS11 provider) throws PKCS11Exception {
        this.provider = provider;
        this.removable = provider.removable;
        this.valid = true;
        p11 = provider.p11;
        config = provider.config;
        tokenInfo = p11.C_GetTokenInfo(provider.slotID);
        writeProtected = (tokenInfo.flags & CKF_WRITE_PROTECTED) != 0;
        // create session manager and open a test session
        SessionManager sessionManager;
        try {
            sessionManager = new SessionManager(this);
            Session s = sessionManager.getOpSession();
            sessionManager.releaseSession(s);
        } catch (PKCS11Exception e) {
            if (writeProtected) {
                throw e;
            }
            // token might not permit RW sessions even though
            // CKF_WRITE_PROTECTED is not set
            writeProtected = true;
            sessionManager = new SessionManager(this);
            Session s = sessionManager.getOpSession();
            sessionManager.releaseSession(s);
        }
        this.sessionManager = sessionManager;
        secretCache = new KeyCache();
        privateCache = new KeyCache();
        templateManager = config.getTemplateManager();
        explicitCancel = config.getExplicitCancel();
        mechInfoMap =
            new ConcurrentHashMap<Long, CK_MECHANISM_INFO>(10);
    }

    boolean isWriteProtected() {
        return writeProtected;
    }

    // return whether the token supports raw secret key material import
    boolean supportsRawSecretKeyImport() {
        if (supportsRawSecretKeyImport == null) {
            SecureRandom random = JCAUtil.getSecureRandom();
            byte[] encoded = new byte[48];
            random.nextBytes(encoded);

            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[3];
            attributes[0] = new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY);
            attributes[1] = new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_GENERIC_SECRET);
            attributes[2] = new CK_ATTRIBUTE(CKA_VALUE, encoded);

            Session session = null;
            try {
                attributes = getAttributes(O_IMPORT,
                        CKO_SECRET_KEY, CKK_GENERIC_SECRET, attributes);
                session = getObjSession();
                long keyID = p11.C_CreateObject(session.id(), attributes);

                supportsRawSecretKeyImport = Boolean.TRUE;
            } catch (PKCS11Exception e) {
                supportsRawSecretKeyImport = Boolean.FALSE;
            } finally {
                releaseSession(session);
            }
        }

        return supportsRawSecretKeyImport;
    }

    // return whether we are logged in
    // uses cached result if current. session is optional and may be null
    boolean isLoggedIn(Session session) throws PKCS11Exception {
        // volatile load first
        boolean loggedIn = this.loggedIn;
        long time = System.currentTimeMillis();
        if (time - lastLoginCheck > CHECK_INTERVAL) {
            loggedIn = isLoggedInNow(session);
            lastLoginCheck = time;
        }
        return loggedIn;
    }

    // return whether we are logged in now
    // does not use cache
    boolean isLoggedInNow(Session session) throws PKCS11Exception {
        boolean allocSession = (session == null);
        try {
            if (allocSession) {
                session = getOpSession();
            }
            CK_SESSION_INFO info = p11.C_GetSessionInfo(session.id());
            boolean loggedIn = (info.state == CKS_RO_USER_FUNCTIONS) ||
                                (info.state == CKS_RW_USER_FUNCTIONS);
            this.loggedIn = loggedIn;
            return loggedIn;
        } finally {
            if (allocSession) {
                releaseSession(session);
            }
        }
    }

    // ensure that we are logged in
    // call provider.login() if not
    void ensureLoggedIn(Session session) throws PKCS11Exception, LoginException {
        if (isLoggedIn(session) == false) {
            provider.login(null, null);
        }
    }

    // return whether this token object is valid (i.e. token not removed)
    // returns value from last check, does not perform new check
    boolean isValid() {
        if (removable == false) {
            return true;
        }
        return valid;
    }

    void ensureValid() {
        if (isValid() == false) {
            throw new ProviderException("Token has been removed");
        }
    }

    // return whether a token is present (i.e. token not removed)
    // returns cached value if current, otherwise performs new check
    boolean isPresent(long sessionID) {
        if (removable == false) {
            return true;
        }
        if (valid == false) {
            return false;
        }
        long time = System.currentTimeMillis();
        if ((time - lastPresentCheck) >= CHECK_INTERVAL) {
            synchronized (CHECK_LOCK) {
                if ((time - lastPresentCheck) >= CHECK_INTERVAL) {
                    boolean ok = false;
                    try {
                        // check if token still present
                        CK_SLOT_INFO slotInfo =
                                provider.p11.C_GetSlotInfo(provider.slotID);
                        if ((slotInfo.flags & CKF_TOKEN_PRESENT) != 0) {
                            // if the token has been removed and re-inserted,
                            // the token should return an error
                            CK_SESSION_INFO sessInfo =
                                    provider.p11.C_GetSessionInfo
                                    (sessionID);
                            ok = true;
                        }
                    } catch (PKCS11Exception e) {
                        // empty
                    }
                    valid = ok;
                    lastPresentCheck = System.currentTimeMillis();
                    if (ok == false) {
                        destroy();
                    }
                }
            }
        }
        return valid;
    }

    void destroy() {
        secretCache.clear();
        privateCache.clear();

        sessionManager.clearPools();
        provider.uninitToken(this);
        valid = false;
    }

    Session getObjSession() throws PKCS11Exception {
        return sessionManager.getObjSession();
    }

    Session getOpSession() throws PKCS11Exception {
        return sessionManager.getOpSession();
    }

    Session releaseSession(Session session) {
        return sessionManager.releaseSession(session);
    }

    Session killSession(Session session) {
        return sessionManager.killSession(session);
    }

    CK_ATTRIBUTE[] getAttributes(String op, long type, long alg,
            CK_ATTRIBUTE[] attrs) throws PKCS11Exception {
        CK_ATTRIBUTE[] newAttrs =
                    templateManager.getAttributes(op, type, alg, attrs);
        for (CK_ATTRIBUTE attr : newAttrs) {
            if (attr.type == CKA_TOKEN) {
                if (attr.getBoolean()) {
                    try {
                        ensureLoggedIn(null);
                    } catch (LoginException e) {
                        throw new ProviderException("Login failed", e);
                    }
                }
                // break once we have found a CKA_TOKEN attribute
                break;
            }
        }
        return newAttrs;
    }

    P11KeyFactory getKeyFactory(String algorithm) {
        P11KeyFactory f;
        if (algorithm.equals("RSA")) {
            f = rsaFactory;
            if (f == null) {
                f = new P11RSAKeyFactory(this, algorithm);
                rsaFactory = f;
            }
        } else if (algorithm.equals("DSA")) {
            f = dsaFactory;
            if (f == null) {
                f = new P11DSAKeyFactory(this, algorithm);
                dsaFactory = f;
            }
        } else if (algorithm.equals("DH")) {
            f = dhFactory;
            if (f == null) {
                f = new P11DHKeyFactory(this, algorithm);
                dhFactory = f;
            }
        } else if (algorithm.equals("EC")) {
            f = ecFactory;
            if (f == null) {
                f = new P11ECKeyFactory(this, algorithm);
                ecFactory = f;
            }
        } else {
            throw new ProviderException("Unknown algorithm " + algorithm);
        }
        return f;
    }

    P11SecureRandom getRandom() {
        if (secureRandom == null) {
            secureRandom = new P11SecureRandom(this);
        }
        return secureRandom;
    }

    P11KeyStore getKeyStore() {
        if (keyStore == null) {
            keyStore = new P11KeyStore(this);
        }
        return keyStore;
    }

    CK_MECHANISM_INFO getMechanismInfo(long mechanism) throws PKCS11Exception {
        CK_MECHANISM_INFO result = mechInfoMap.get(mechanism);
        if (result == null) {
            try {
                result = p11.C_GetMechanismInfo(provider.slotID,
                                                mechanism);
                mechInfoMap.put(mechanism, result);
            } catch (PKCS11Exception e) {
                if (e.getErrorCode() != CKR_MECHANISM_INVALID) {
                    throw e;
                } else {
                    mechInfoMap.put(mechanism, INVALID_MECH);
                }
            }
        } else if (result == INVALID_MECH) {
            result = null;
        }
        return result;
    }

    private synchronized byte[] getTokenId() {
        if (tokenId == null) {
            SecureRandom random = JCAUtil.getSecureRandom();
            tokenId = new byte[20];
            random.nextBytes(tokenId);
            serializedTokens.add(new WeakReference<Token>(this));
        }
        return tokenId;
    }

    // list of all tokens that have been serialized within this VM
    // NOTE that elements are never removed from this list
    // the assumption is that the number of tokens that are serialized
    // is relatively small
    private static final List<Reference<Token>> serializedTokens =
        new ArrayList<Reference<Token>>();

    private Object writeReplace() throws ObjectStreamException {
        if (isValid() == false) {
            throw new NotSerializableException("Token has been removed");
        }
        return new TokenRep(this);
    }

    // serialized representation of a token
    // tokens can only be de-serialized within the same VM invocation
    // and if the token has not been removed in the meantime
    private static class TokenRep implements Serializable {

        private static final long serialVersionUID = 3503721168218219807L;

        private final byte[] tokenId;

        TokenRep(Token token) {
            tokenId = token.getTokenId();
        }

        private Object readResolve() throws ObjectStreamException {
            for (Reference<Token> tokenRef : serializedTokens) {
                Token token = tokenRef.get();
                if ((token != null) && token.isValid()) {
                    if (Arrays.equals(token.getTokenId(), tokenId)) {
                        return token;
                    }
                }
            }
            throw new NotSerializableException("Could not find token");
        }
    }

}
