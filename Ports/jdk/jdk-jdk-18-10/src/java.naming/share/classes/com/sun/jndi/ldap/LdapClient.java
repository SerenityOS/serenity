/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.Locale;
import java.util.Vector;
import java.util.Hashtable;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;

import com.sun.jndi.ldap.pool.PooledConnection;
import com.sun.jndi.ldap.pool.PoolCallback;
import com.sun.jndi.ldap.sasl.LdapSasl;
import com.sun.jndi.ldap.sasl.SaslInputStream;

/**
 * LDAP (RFC-1777) and LDAPv3 (RFC-2251) compliant client
 *
 * This class represents a connection to an LDAP client.
 * Callers interact with this class at an LDAP operation level.
 * That is, the caller invokes a method to do a SEARCH or MODRDN
 * operation and gets back the result.
 * The caller uses the constructor to create a connection to the server.
 * It then needs to use authenticate() to perform an LDAP BIND.
 * Note that for v3, BIND is optional so authenticate() might not
 * actually send a BIND. authenticate() can be used later on to issue
 * a BIND, for example, for a v3 client that wants to change the connection's
 * credentials.
 *<p>
 * Multiple LdapCtx might share the same LdapClient. For example, contexts
 * derived from the same initial context would share the same LdapClient
 * until changes to a context's properties necessitates its own LdapClient.
 * LdapClient methods that access shared data are thread-safe (i.e., caller
 * does not have to sync).
 *<p>
 * Fields:
 *   isLdapv3 - no sync; initialized and updated within sync authenticate();
 *       always updated when connection is "quiet" and not shared;
 *       read access from outside LdapClient not sync
 *   referenceCount - sync within LdapClient; exception is forceClose() which
 *       is used by Connection thread to close connection upon receiving
 *       an Unsolicited Notification.
 *       access from outside LdapClient must sync;
 *   conn - no sync; Connection takes care of its own sync
 *   unsolicited - sync Vector; multiple operations sync'ed
 *
 * @author Vincent Ryan
 * @author Jagane Sundar
 * @author Rosanna Lee
 */

public final class LdapClient implements PooledConnection {
    // ---------------------- Constants ----------------------------------
    private static final int debug = 0;
    static final boolean caseIgnore = true;

    // Default list of binary attributes
    private static final Hashtable<String, Boolean> defaultBinaryAttrs =
            new Hashtable<>(23,0.75f);
    static {
        defaultBinaryAttrs.put("userpassword", Boolean.TRUE);      //2.5.4.35
        defaultBinaryAttrs.put("javaserializeddata", Boolean.TRUE);
                                                //1.3.6.1.4.1.42.2.27.4.1.8
        defaultBinaryAttrs.put("javaserializedobject", Boolean.TRUE);
                                                // 1.3.6.1.4.1.42.2.27.4.1.2
        defaultBinaryAttrs.put("jpegphoto", Boolean.TRUE);
                                                //0.9.2342.19200300.100.1.60
        defaultBinaryAttrs.put("audio", Boolean.TRUE);  //0.9.2342.19200300.100.1.55
        defaultBinaryAttrs.put("thumbnailphoto", Boolean.TRUE);
                                                //1.3.6.1.4.1.1466.101.120.35
        defaultBinaryAttrs.put("thumbnaillogo", Boolean.TRUE);
                                                //1.3.6.1.4.1.1466.101.120.36
        defaultBinaryAttrs.put("usercertificate", Boolean.TRUE);     //2.5.4.36
        defaultBinaryAttrs.put("cacertificate", Boolean.TRUE);       //2.5.4.37
        defaultBinaryAttrs.put("certificaterevocationlist", Boolean.TRUE);
                                                //2.5.4.39
        defaultBinaryAttrs.put("authorityrevocationlist", Boolean.TRUE); //2.5.4.38
        defaultBinaryAttrs.put("crosscertificatepair", Boolean.TRUE);    //2.5.4.40
        defaultBinaryAttrs.put("photo", Boolean.TRUE);   //0.9.2342.19200300.100.1.7
        defaultBinaryAttrs.put("personalsignature", Boolean.TRUE);
                                                //0.9.2342.19200300.100.1.53
        defaultBinaryAttrs.put("x500uniqueidentifier", Boolean.TRUE); //2.5.4.45
    }

    private static final String DISCONNECT_OID = "1.3.6.1.4.1.1466.20036";


    // ----------------------- instance fields ------------------------
    boolean isLdapv3;         // Used by LdapCtx
    int referenceCount = 1;   // Used by LdapCtx for check for sharing

    final Connection conn;  // Connection to server; has reader thread
                      // used by LdapCtx for StartTLS

    private final PoolCallback pcb;
    private final boolean pooled;
    private boolean authenticateCalled = false;

    ////////////////////////////////////////////////////////////////////////////
    //
    // constructor: Create an authenticated connection to server
    //
    ////////////////////////////////////////////////////////////////////////////

    LdapClient(String host, int port, String socketFactory,
        int connectTimeout, int readTimeout, OutputStream trace, PoolCallback pcb)
        throws NamingException {

        if (debug > 0)
            System.err.println("LdapClient: constructor called " + host + ":" + port );
        conn = new Connection(this, host, port, socketFactory, connectTimeout, readTimeout,
            trace);

        this.pcb = pcb;
        pooled = (pcb != null);
    }

    synchronized boolean authenticateCalled() {
        return authenticateCalled;
    }

    synchronized LdapResult
    authenticate(boolean initial, String name, Object pw, int version,
        String authMechanism, Control[] ctls,  Hashtable<?,?> env)
        throws NamingException {

        int readTimeout = conn.readTimeout;
        conn.readTimeout = conn.connectTimeout;
        LdapResult res = null;

        try {
            authenticateCalled = true;

            try {
                ensureOpen();
            } catch (IOException e) {
                NamingException ne = new CommunicationException();
                ne.setRootCause(e);
                throw ne;
            }

            switch (version) {
            case LDAP_VERSION3_VERSION2:
            case LDAP_VERSION3:
                isLdapv3 = true;
                break;
            case LDAP_VERSION2:
                isLdapv3 = false;
                break;
            default:
                throw new CommunicationException("Protocol version " + version +
                    " not supported");
            }

            if (authMechanism.equalsIgnoreCase("none") ||
                authMechanism.equalsIgnoreCase("anonymous")) {

                // Perform LDAP bind if we are reauthenticating, using LDAPv2,
                // supporting failover to LDAPv2, or controls have been supplied.
                if (!initial ||
                    (version == LDAP_VERSION2) ||
                    (version == LDAP_VERSION3_VERSION2) ||
                    ((ctls != null) && (ctls.length > 0))) {
                    try {
                        // anonymous bind; update name/pw for LDAPv2 retry
                        res = ldapBind(name=null, (byte[])(pw=null), ctls, null,
                            false);
                        if (res.status == LdapClient.LDAP_SUCCESS) {
                            conn.setBound();
                        }
                    } catch (IOException e) {
                        NamingException ne =
                            new CommunicationException("anonymous bind failed: " +
                            conn.host + ":" + conn.port);
                        ne.setRootCause(e);
                        throw ne;
                    }
                } else {
                    // Skip LDAP bind for LDAPv3 anonymous bind
                    res = new LdapResult();
                    res.status = LdapClient.LDAP_SUCCESS;
                }
            } else if (authMechanism.equalsIgnoreCase("simple")) {
                // simple authentication
                byte[] encodedPw = null;
                try {
                    encodedPw = encodePassword(pw, isLdapv3);
                    res = ldapBind(name, encodedPw, ctls, null, false);
                    if (res.status == LdapClient.LDAP_SUCCESS) {
                        conn.setBound();
                    }
                } catch (IOException e) {
                    NamingException ne =
                        new CommunicationException("simple bind failed: " +
                            conn.host + ":" + conn.port);
                    ne.setRootCause(e);
                    throw ne;
                } finally {
                    // If pw was copied to a new array, clear that array as
                    // a security precaution.
                    if (encodedPw != pw && encodedPw != null) {
                        for (int i = 0; i < encodedPw.length; i++) {
                            encodedPw[i] = 0;
                        }
                    }
                }
            } else if (isLdapv3) {
                // SASL authentication
                try {
                    res = LdapSasl.saslBind(this, conn, conn.host, name, pw,
                        authMechanism, env, ctls);
                    if (res.status == LdapClient.LDAP_SUCCESS) {
                        conn.setBound();
                    }
                } catch (IOException e) {
                    NamingException ne =
                        new CommunicationException("SASL bind failed: " +
                        conn.host + ":" + conn.port);
                    ne.setRootCause(e);
                    throw ne;
                }
            } else {
                throw new AuthenticationNotSupportedException(authMechanism);
            }

            //
            // re-try login using v2 if failing over
            //
            if (initial &&
                (res.status == LdapClient.LDAP_PROTOCOL_ERROR) &&
                (version == LdapClient.LDAP_VERSION3_VERSION2) &&
                (authMechanism.equalsIgnoreCase("none") ||
                    authMechanism.equalsIgnoreCase("anonymous") ||
                    authMechanism.equalsIgnoreCase("simple"))) {

                byte[] encodedPw = null;
                try {
                    isLdapv3 = false;
                    encodedPw = encodePassword(pw, false);
                    res = ldapBind(name, encodedPw, ctls, null, false);
                    if (res.status == LdapClient.LDAP_SUCCESS) {
                        conn.setBound();
                    }
                } catch (IOException e) {
                    NamingException ne =
                        new CommunicationException(authMechanism + ":" +
                            conn.host +     ":" + conn.port);
                    ne.setRootCause(e);
                    throw ne;
                } finally {
                    // If pw was copied to a new array, clear that array as
                    // a security precaution.
                    if (encodedPw != pw && encodedPw != null) {
                        for (int i = 0; i < encodedPw.length; i++) {
                            encodedPw[i] = 0;
                        }
                    }
                }
            }

            // principal name not found
            // (map NameNotFoundException to AuthenticationException)
            // %%% This is a workaround for Netscape servers returning
            // %%% no such object when the principal name is not found
            // %%% Note that when this workaround is applied, it does not allow
            // %%% response controls to be recorded by the calling context
            if (res.status == LdapClient.LDAP_NO_SUCH_OBJECT) {
                throw new AuthenticationException(
                    getErrorMessage(res.status, res.errorMessage));
            }
            conn.setV3(isLdapv3);
            return res;
        } finally {
            conn.readTimeout = readTimeout;
        }
    }

    /**
     * Sends an LDAP Bind request.
     * Cannot be private; called by LdapSasl
     * @param dn The possibly null DN to use in the BIND request. null if anonymous.
     * @param toServer The possibly null array of bytes to send to the server.
     * @param auth The authentication mechanism
     *
     */
    synchronized public LdapResult ldapBind(String dn, byte[]toServer,
        Control[] bindCtls, String auth, boolean pauseAfterReceipt)
        throws java.io.IOException, NamingException {

        ensureOpen();

        // flush outstanding requests
        conn.abandonOutstandingReqs(null);

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();
        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        //
        // build the bind request.
        //
        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeInt(curMsgId);
            ber.beginSeq(LdapClient.LDAP_REQ_BIND);
                ber.encodeInt(isLdapv3 ? LDAP_VERSION3 : LDAP_VERSION2);
                ber.encodeString(dn, isLdapv3);

                // if authentication mechanism specified, it is SASL
                if (auth != null) {
                    ber.beginSeq(Ber.ASN_CONTEXT | Ber.ASN_CONSTRUCTOR | 3);
                        ber.encodeString(auth, isLdapv3);    // SASL mechanism
                        if (toServer != null) {
                            ber.encodeOctetString(toServer,
                                Ber.ASN_OCTET_STR);
                        }
                    ber.endSeq();
                } else {
                    if (toServer != null) {
                        ber.encodeOctetString(toServer, Ber.ASN_CONTEXT);
                    } else {
                        ber.encodeOctetString(null, Ber.ASN_CONTEXT, 0, 0);
                    }
                }
            ber.endSeq();

            // Encode controls
            if (isLdapv3) {
                encodeControls(ber, bindCtls);
            }
        ber.endSeq();

        LdapRequest req = conn.writeRequest(ber, curMsgId, pauseAfterReceipt);
        if (toServer != null) {
            ber.reset();        // clear internally-stored password
        }

        // Read reply
        BerDecoder rber = conn.readReply(req);

        rber.parseSeq(null);    // init seq
        rber.parseInt();        // msg id
        if (rber.parseByte() !=  LDAP_REP_BIND) {
            return res;
        }

        rber.parseLength();
        parseResult(rber, res, isLdapv3);

        // handle server's credentials (if present)
        if (isLdapv3 &&
            (rber.bytesLeft() > 0) &&
            (rber.peekByte() == (Ber.ASN_CONTEXT | 7))) {
            res.serverCreds = rber.parseOctetString((Ber.ASN_CONTEXT | 7), null);
        }

        res.resControls = isLdapv3 ? parseControls(rber) : null;

        conn.removeRequest(req);
        return res;
    }

    /**
     * Determines whether SASL encryption/integrity is in progress.
     * This check is made prior to reauthentication. You cannot reauthenticate
     * over an encrypted/integrity-protected SASL channel. You must
     * close the channel and open a new one.
     */
    boolean usingSaslStreams() {
        return (conn.inStream instanceof SaslInputStream);
    }

    // Returns true if client connection was upgraded
    // with STARTTLS extended operation on the server side
    boolean isUpgradedToStartTls() {
        return conn.isUpgradedToStartTls();
    }

    synchronized void incRefCount() {
        ++referenceCount;
        if (debug > 1) {
            System.err.println("LdapClient.incRefCount: " + referenceCount + " " + this);
        }

    }

    /**
     * Returns the encoded password.
     */
    private static byte[] encodePassword(Object pw, boolean v3) throws IOException {

        if (pw instanceof char[]) {
            pw = new String((char[])pw);
        }

        if (pw instanceof String) {
            if (v3) {
                return ((String)pw).getBytes("UTF8");
            } else {
                return ((String)pw).getBytes("8859_1");
            }
        } else {
            return (byte[])pw;
        }
    }

    synchronized void close(Control[] reqCtls, boolean hardClose) {
        --referenceCount;

        if (debug > 1) {
            System.err.println("LdapClient: " + this);
            System.err.println("LdapClient: close() called: " + referenceCount);
            (new Throwable()).printStackTrace();
        }

        if (referenceCount <= 0) {
            if (debug > 0) System.err.println("LdapClient: closed connection " + this);
            if (!pooled) {
                // Not being pooled; continue with closing
                conn.cleanup(reqCtls, false);
            } else {
                // Pooled
                // Is this a real close or a request to return conn to pool
                if (hardClose) {
                    conn.cleanup(reqCtls, false);
                    pcb.removePooledConnection(this);
                } else {
                    pcb.releasePooledConnection(this);
                }
            }
        }
    }

    // NOTE: Should NOT be synchronized otherwise won't be able to close
    private void forceClose(boolean cleanPool) {
        referenceCount = 0; // force closing of connection

        if (debug > 1) {
            System.err.println("LdapClient: forceClose() of " + this);
        }
        if (debug > 0) {
            System.err.println(
                    "LdapClient: forced close of connection " + this);
        }
        conn.cleanup(null, false);
        if (cleanPool) {
            pcb.removePooledConnection(this);
        }
    }

    @SuppressWarnings("deprecation")
    protected void finalize() {
        if (debug > 0) System.err.println("LdapClient: finalize " + this);
        forceClose(pooled);
    }

    /*
     * Used by connection pooling to close physical connection.
     */
    synchronized public void closeConnection() {
        forceClose(false); // this is a pool callback so no need to clean pool
    }

    /**
     * Called by Connection.cleanup(). LdapClient should
     * notify any unsolicited listeners and removing itself from any pool.
     * This is almost like forceClose(), except it doesn't call
     * Connection.cleanup() (because this is called from cleanup()).
     */
    void processConnectionClosure() {
        // Notify listeners
        if (unsolicited.size() > 0) {
            String msg;
            if (conn != null) {
                msg = conn.host + ":" + conn.port + " connection closed";
            } else {
                msg = "Connection closed";
            }
            notifyUnsolicited(new CommunicationException(msg));
        }

        // Remove from pool
        if (pooled) {
            pcb.removePooledConnection(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP search. also includes methods to encode rfc 1558 compliant filters
    //
    ////////////////////////////////////////////////////////////////////////////

    static final int SCOPE_BASE_OBJECT = 0;
    static final int SCOPE_ONE_LEVEL = 1;
    static final int SCOPE_SUBTREE = 2;

    LdapResult search(String dn, int scope, int deref, int sizeLimit,
                      int timeLimit, boolean attrsOnly, String attrs[],
                      String filter, int batchSize, Control[] reqCtls,
                      Hashtable<String, Boolean> binaryAttrs,
                      boolean waitFirstReply, int replyQueueCapacity)
        throws IOException, NamingException {

        ensureOpen();

        LdapResult res = new LdapResult();

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(curMsgId);
                ber.beginSeq(LDAP_REQ_SEARCH);
                    ber.encodeString(dn == null ? "" : dn, isLdapv3);
                    ber.encodeInt(scope, LBER_ENUMERATED);
                    ber.encodeInt(deref, LBER_ENUMERATED);
                    ber.encodeInt(sizeLimit);
                    ber.encodeInt(timeLimit);
                    ber.encodeBoolean(attrsOnly);
                    Filter.encodeFilterString(ber, filter, isLdapv3);
                    ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                        ber.encodeStringArray(attrs, isLdapv3);
                    ber.endSeq();
                ber.endSeq();
                if (isLdapv3) encodeControls(ber, reqCtls);
            ber.endSeq();

         LdapRequest req =
                conn.writeRequest(ber, curMsgId, false, replyQueueCapacity);

         res.msgId = curMsgId;
         res.status = LdapClient.LDAP_SUCCESS; //optimistic
         if (waitFirstReply) {
             // get first reply
             res = getSearchReply(req, batchSize, res, binaryAttrs);
         }
         return res;
    }

    /*
     * Abandon the search operation and remove it from the message queue.
     */
    void clearSearchReply(LdapResult res, Control[] ctls) {
        if (res != null) {

            // Only send an LDAP abandon operation when clearing the search
            // reply from a one-level or subtree search.
            LdapRequest req = conn.findRequest(res.msgId);
            if (req == null) {
                return;
            }

            // OK if req got removed after check; double removal attempt
            // but otherwise no harm done

            // Send an LDAP abandon only if the search operation has not yet
            // completed.
            if (req.hasSearchCompleted()) {
                conn.removeRequest(req);
            } else {
                conn.abandonRequest(req, ctls);
            }
        }
    }

    /*
     * Retrieve the next batch of entries and/or referrals.
     */
    LdapResult getSearchReply(int batchSize, LdapResult res,
        Hashtable<String, Boolean> binaryAttrs) throws IOException, NamingException {

        ensureOpen();

        LdapRequest req;

        if ((req = conn.findRequest(res.msgId)) == null) {
            return null;
        }

        return getSearchReply(req, batchSize, res, binaryAttrs);
    }

    private LdapResult getSearchReply(LdapRequest req,
        int batchSize, LdapResult res, Hashtable<String, Boolean> binaryAttrs)
        throws IOException, NamingException {

        if (batchSize == 0)
            batchSize = Integer.MAX_VALUE;

        if (res.entries != null) {
            res.entries.setSize(0); // clear the (previous) set of entries
        } else {
            res.entries =
                new Vector<>(batchSize == Integer.MAX_VALUE ? 32 : batchSize);
        }

        if (res.referrals != null) {
            res.referrals.setSize(0); // clear the (previous) set of referrals
        }

        BerDecoder replyBer;    // Decoder for response
        int seq;                // Request id

        Attributes lattrs;      // Attribute set read from response
        Attribute la;           // Attribute read from response
        String DN;              // DN read from response
        LdapEntry le;           // LDAP entry representing response
        int[] seqlen;           // Holder for response length
        int endseq;             // Position of end of response

        for (int i = 0; i < batchSize;) {
            replyBer = conn.readReply(req);

            //
            // process search reply
            //
            replyBer.parseSeq(null);                    // init seq
            replyBer.parseInt();                        // req id
            seq = replyBer.parseSeq(null);

            if (seq == LDAP_REP_SEARCH) {

                // handle LDAPv3 search entries
                lattrs = new BasicAttributes(caseIgnore);
                DN = replyBer.parseString(isLdapv3);
                le = new LdapEntry(DN, lattrs);
                seqlen = new int[1];

                replyBer.parseSeq(seqlen);
                endseq = replyBer.getParsePosition() + seqlen[0];
                while ((replyBer.getParsePosition() < endseq) &&
                    (replyBer.bytesLeft() > 0)) {
                    la = parseAttribute(replyBer, binaryAttrs);
                    lattrs.put(la);
                }
                le.respCtls = isLdapv3 ? parseControls(replyBer) : null;

                res.entries.addElement(le);
                i++;

            } else if ((seq == LDAP_REP_SEARCH_REF) && isLdapv3) {

                // handle LDAPv3 search reference
                Vector<String> URLs = new Vector<>(4);

                // %%% Although not strictly correct, some LDAP servers
                //     encode the SEQUENCE OF tag in the SearchResultRef
                if (replyBer.peekByte() ==
                    (Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR)) {
                    replyBer.parseSeq(null);
                }

                while ((replyBer.bytesLeft() > 0) &&
                    (replyBer.peekByte() == Ber.ASN_OCTET_STR)) {

                    URLs.addElement(replyBer.parseString(isLdapv3));
                }

                if (res.referrals == null) {
                    res.referrals = new Vector<>(4);
                }
                res.referrals.addElement(URLs);
                res.resControls = isLdapv3 ? parseControls(replyBer) : null;

                // Save referral and continue to get next search result

            } else if (seq == LDAP_REP_EXTENSION) {

                parseExtResponse(replyBer, res); //%%% ignore for now

            } else if (seq == LDAP_REP_RESULT) {

                parseResult(replyBer, res, isLdapv3);
                res.resControls = isLdapv3 ? parseControls(replyBer) : null;

                conn.removeRequest(req);
                return res;     // Done with search
            }
        }

        return res;
    }

    private Attribute parseAttribute(BerDecoder ber,
                                     Hashtable<String, Boolean> binaryAttrs)
        throws IOException {

        int len[] = new int[1];
        int seq = ber.parseSeq(null);
        String attrid = ber.parseString(isLdapv3);
        boolean hasBinaryValues = isBinaryValued(attrid, binaryAttrs);
        Attribute la = new LdapAttribute(attrid);

        if ((seq = ber.parseSeq(len)) == LBER_SET) {
            int attrlen = len[0];
            while (ber.bytesLeft() > 0 && attrlen > 0) {
                try {
                    attrlen -= parseAttributeValue(ber, la, hasBinaryValues);
                } catch (IOException ex) {
                    ber.seek(attrlen);
                    break;
                }
            }
        } else {
            // Skip the rest of the sequence because it is not what we want
            ber.seek(len[0]);
        }
        return la;
    }

    //
    // returns number of bytes that were parsed. Adds the values to attr
    //
    private int parseAttributeValue(BerDecoder ber, Attribute la,
        boolean hasBinaryValues) throws IOException {

        int len[] = new int[1];

        if (hasBinaryValues) {
            la.add(ber.parseOctetString(ber.peekByte(), len));
        } else {
            la.add(ber.parseStringWithTag(
                                    Ber.ASN_SIMPLE_STRING, isLdapv3, len));
        }
        return len[0];
    }

    private boolean isBinaryValued(String attrid,
                                   Hashtable<String, Boolean> binaryAttrs) {
        String id = attrid.toLowerCase(Locale.ENGLISH);

        return ((id.indexOf(";binary") != -1) ||
            defaultBinaryAttrs.containsKey(id) ||
            ((binaryAttrs != null) && (binaryAttrs.containsKey(id))));
    }

    // package entry point; used by Connection
    static void parseResult(BerDecoder replyBer, LdapResult res,
            boolean isLdapv3) throws IOException {

        res.status = replyBer.parseEnumeration();
        res.matchedDN = replyBer.parseString(isLdapv3);
        res.errorMessage = replyBer.parseString(isLdapv3);

        // handle LDAPv3 referrals (if present)
        if (isLdapv3 &&
            (replyBer.bytesLeft() > 0) &&
            (replyBer.peekByte() == LDAP_REP_REFERRAL)) {

            Vector<String> URLs = new Vector<>(4);
            int[] seqlen = new int[1];

            replyBer.parseSeq(seqlen);
            int endseq = replyBer.getParsePosition() + seqlen[0];
            while ((replyBer.getParsePosition() < endseq) &&
                (replyBer.bytesLeft() > 0)) {

                URLs.addElement(replyBer.parseString(isLdapv3));
            }

            if (res.referrals == null) {
                res.referrals = new Vector<>(4);
            }
            res.referrals.addElement(URLs);
        }
    }

    // package entry point; used by Connection
    static Vector<Control> parseControls(BerDecoder replyBer) throws IOException {

        // handle LDAPv3 controls (if present)
        if ((replyBer.bytesLeft() > 0) && (replyBer.peekByte() == LDAP_CONTROLS)) {
            Vector<Control> ctls = new Vector<>(4);
            String controlOID;
            boolean criticality = false; // default
            byte[] controlValue = null;  // optional
            int[] seqlen = new int[1];

            replyBer.parseSeq(seqlen);
            int endseq = replyBer.getParsePosition() + seqlen[0];
            while ((replyBer.getParsePosition() < endseq) &&
                (replyBer.bytesLeft() > 0)) {

                replyBer.parseSeq(null);
                controlOID = replyBer.parseString(true);

                if ((replyBer.bytesLeft() > 0) &&
                    (replyBer.peekByte() == Ber.ASN_BOOLEAN)) {
                    criticality = replyBer.parseBoolean();
                }
                if ((replyBer.bytesLeft() > 0) &&
                    (replyBer.peekByte() == Ber.ASN_OCTET_STR)) {
                    controlValue =
                        replyBer.parseOctetString(Ber.ASN_OCTET_STR, null);
                }
                if (controlOID != null) {
                    ctls.addElement(
                        new BasicControl(controlOID, criticality, controlValue));
                }
            }
            return ctls;
        } else {
            return null;
        }
    }

    private void parseExtResponse(BerDecoder replyBer, LdapResult res)
        throws IOException {

        parseResult(replyBer, res, isLdapv3);

        if ((replyBer.bytesLeft() > 0) &&
            (replyBer.peekByte() == LDAP_REP_EXT_OID)) {
            res.extensionId =
                replyBer.parseStringWithTag(LDAP_REP_EXT_OID, isLdapv3, null);
        }
        if ((replyBer.bytesLeft() > 0) &&
            (replyBer.peekByte() == LDAP_REP_EXT_VAL)) {
            res.extensionValue =
                replyBer.parseOctetString(LDAP_REP_EXT_VAL, null);
        }

        res.resControls = parseControls(replyBer);
    }

    //
    // Encode LDAPv3 controls
    //
    static void encodeControls(BerEncoder ber, Control[] reqCtls)
        throws IOException {

        if ((reqCtls == null) || (reqCtls.length == 0)) {
            return;
        }

        byte[] controlVal;

        ber.beginSeq(LdapClient.LDAP_CONTROLS);

            for (int i = 0; i < reqCtls.length; i++) {
                ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                    ber.encodeString(reqCtls[i].getID(), true); // control OID
                    if (reqCtls[i].isCritical()) {
                        ber.encodeBoolean(true); // critical control
                    }
                    if ((controlVal = reqCtls[i].getEncodedValue()) != null) {
                        ber.encodeOctetString(controlVal, Ber.ASN_OCTET_STR);
                    }
                ber.endSeq();
            }
        ber.endSeq();
    }

    /**
     * Reads the next reply corresponding to msgId, outstanding on requestBer.
     * Processes the result and any controls.
     */
    private LdapResult processReply(LdapRequest req,
        LdapResult res, int responseType) throws IOException, NamingException {

        BerDecoder rber = conn.readReply(req);

        rber.parseSeq(null);    // init seq
        rber.parseInt();        // msg id
        if (rber.parseByte() !=  responseType) {
            return res;
        }

        rber.parseLength();
        parseResult(rber, res, isLdapv3);
        res.resControls = isLdapv3 ? parseControls(rber) : null;

        conn.removeRequest(req);

        return res;     // Done with operation
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP modify:
    //  Modify the DN dn with the operations on attributes attrs.
    //  ie, operations[0] is the operation to be performed on
    //  attrs[0];
    //          dn - DN to modify
    //          operations - add, delete or replace
    //          attrs - array of Attribute
    //          reqCtls - array of request controls
    //
    ////////////////////////////////////////////////////////////////////////////

    static final int ADD = 0;
    static final int DELETE = 1;
    static final int REPLACE = 2;

    LdapResult modify(String dn, int operations[], Attribute attrs[],
                      Control[] reqCtls)
        throws IOException, NamingException {

        ensureOpen();

        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        if (dn == null || operations.length != attrs.length)
            return res;

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();

        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeInt(curMsgId);
            ber.beginSeq(LDAP_REQ_MODIFY);
                ber.encodeString(dn, isLdapv3);
                ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                    for (int i = 0; i < operations.length; i++) {
                        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                            ber.encodeInt(operations[i], LBER_ENUMERATED);

                            // zero values is not permitted for the add op.
                            if ((operations[i] == ADD) && hasNoValue(attrs[i])) {
                                throw new InvalidAttributeValueException(
                                    "'" + attrs[i].getID() + "' has no values.");
                            } else {
                                encodeAttribute(ber, attrs[i]);
                            }
                        ber.endSeq();
                    }
                ber.endSeq();
            ber.endSeq();
            if (isLdapv3) encodeControls(ber, reqCtls);
        ber.endSeq();

        LdapRequest req = conn.writeRequest(ber, curMsgId);

        return processReply(req, res, LDAP_REP_MODIFY);
    }

    private void encodeAttribute(BerEncoder ber, Attribute attr)
        throws IOException, NamingException {

        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeString(attr.getID(), isLdapv3);
            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR | 1);
                NamingEnumeration<?> enum_ = attr.getAll();
                Object val;
                while (enum_.hasMore()) {
                    val = enum_.next();
                    if (val instanceof String) {
                        ber.encodeString((String)val, isLdapv3);
                    } else if (val instanceof byte[]) {
                        ber.encodeOctetString((byte[])val, Ber.ASN_OCTET_STR);
                    } else if (val == null) {
                        // no attribute value
                    } else {
                        throw new InvalidAttributeValueException(
                            "Malformed '" + attr.getID() + "' attribute value");
                    }
                }
            ber.endSeq();
        ber.endSeq();
    }

    private static boolean hasNoValue(Attribute attr) throws NamingException {
        return attr.size() == 0 || (attr.size() == 1 && attr.get() == null);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP add
    //          Adds entry to the Directory
    //
    ////////////////////////////////////////////////////////////////////////////

    LdapResult add(LdapEntry entry, Control[] reqCtls)
        throws IOException, NamingException {

        ensureOpen();

        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        if (entry == null || entry.DN == null)
            return res;

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();
        Attribute attr;

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(curMsgId);
                ber.beginSeq(LDAP_REQ_ADD);
                    ber.encodeString(entry.DN, isLdapv3);
                    ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                        NamingEnumeration<? extends Attribute> enum_ =
                                entry.attributes.getAll();
                        while (enum_.hasMore()) {
                            attr = enum_.next();

                            // zero values is not permitted
                            if (hasNoValue(attr)) {
                                throw new InvalidAttributeValueException(
                                    "'" + attr.getID() + "' has no values.");
                            } else {
                                encodeAttribute(ber, attr);
                            }
                        }
                    ber.endSeq();
                ber.endSeq();
                if (isLdapv3) encodeControls(ber, reqCtls);
            ber.endSeq();

        LdapRequest req = conn.writeRequest(ber, curMsgId);
        return processReply(req, res, LDAP_REP_ADD);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP delete
    //          deletes entry from the Directory
    //
    ////////////////////////////////////////////////////////////////////////////

    LdapResult delete(String DN, Control[] reqCtls)
        throws IOException, NamingException {

        ensureOpen();

        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        if (DN == null)
            return res;

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(curMsgId);
                ber.encodeString(DN, LDAP_REQ_DELETE, isLdapv3);
                if (isLdapv3) encodeControls(ber, reqCtls);
            ber.endSeq();

        LdapRequest req = conn.writeRequest(ber, curMsgId);

        return processReply(req, res, LDAP_REP_DELETE);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP modrdn
    //  Changes the last element of DN to newrdn
    //          dn - DN to change
    //          newrdn - new RDN to rename to
    //          deleteoldrdn - boolean whether to delete old attrs or not
    //          newSuperior - new place to put the entry in the tree
    //                        (ignored if server is LDAPv2)
    //          reqCtls - array of request controls
    //
    ////////////////////////////////////////////////////////////////////////////

    LdapResult moddn(String DN, String newrdn, boolean deleteOldRdn,
                     String newSuperior, Control[] reqCtls)
        throws IOException, NamingException {

        ensureOpen();

        boolean changeSuperior = (newSuperior != null &&
                                  newSuperior.length() > 0);

        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        if (DN == null || newrdn == null)
            return res;

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(curMsgId);
                ber.beginSeq(LDAP_REQ_MODRDN);
                    ber.encodeString(DN, isLdapv3);
                    ber.encodeString(newrdn, isLdapv3);
                    ber.encodeBoolean(deleteOldRdn);
                    if(isLdapv3 && changeSuperior) {
                        //System.err.println("changin superior");
                        ber.encodeString(newSuperior, LDAP_SUPERIOR_DN, isLdapv3);
                    }
                ber.endSeq();
                if (isLdapv3) encodeControls(ber, reqCtls);
            ber.endSeq();


        LdapRequest req = conn.writeRequest(ber, curMsgId);

        return processReply(req, res, LDAP_REP_MODRDN);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP compare
    //  Compare attribute->value pairs in dn
    //
    ////////////////////////////////////////////////////////////////////////////

    LdapResult compare(String DN, String type, String value, Control[] reqCtls)
        throws IOException, NamingException {

        ensureOpen();

        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        if (DN == null || type == null || value == null)
            return res;

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(curMsgId);
                ber.beginSeq(LDAP_REQ_COMPARE);
                    ber.encodeString(DN, isLdapv3);
                    ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                        ber.encodeString(type, isLdapv3);

                        // replace any escaped characters in the value
                        byte[] val = isLdapv3 ?
                            value.getBytes("UTF8") : value.getBytes("8859_1");
                        ber.encodeOctetString(
                            Filter.unescapeFilterValue(val, 0, val.length),
                            Ber.ASN_OCTET_STR);

                    ber.endSeq();
                ber.endSeq();
                if (isLdapv3) encodeControls(ber, reqCtls);
            ber.endSeq();

        LdapRequest req = conn.writeRequest(ber, curMsgId);

        return processReply(req, res, LDAP_REP_COMPARE);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // LDAP extended operation
    //
    ////////////////////////////////////////////////////////////////////////////

    LdapResult extendedOp(String id, byte[] request, Control[] reqCtls,
        boolean pauseAfterReceipt) throws IOException, NamingException {

        ensureOpen();

        LdapResult res = new LdapResult();
        res.status = LDAP_OPERATIONS_ERROR;

        if (id == null)
            return res;

        BerEncoder ber = new BerEncoder();
        int curMsgId = conn.getMsgId();

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(curMsgId);
                ber.beginSeq(LDAP_REQ_EXTENSION);
                    ber.encodeString(id,
                        Ber.ASN_CONTEXT | 0, isLdapv3);//[0]
                    if (request != null) {
                        ber.encodeOctetString(request,
                            Ber.ASN_CONTEXT | 1);//[1]
                    }
                ber.endSeq();
                encodeControls(ber, reqCtls); // always v3
            ber.endSeq();

        LdapRequest req = conn.writeRequest(ber, curMsgId, pauseAfterReceipt);

        BerDecoder rber = conn.readReply(req);

        rber.parseSeq(null);    // init seq
        rber.parseInt();        // msg id
        if (rber.parseByte() !=  LDAP_REP_EXTENSION) {
            return res;
        }

        rber.parseLength();
        parseExtResponse(rber, res);
        conn.removeRequest(req);

        return res;     // Done with operation
    }



    ////////////////////////////////////////////////////////////////////////////
    //
    // Some BER definitions convenient for LDAP
    //
    ////////////////////////////////////////////////////////////////////////////

    static final int LDAP_VERSION3_VERSION2 = 32;
    static final int LDAP_VERSION2 = 0x02;
    static final int LDAP_VERSION3 = 0x03;              // LDAPv3
    static final int LDAP_VERSION = LDAP_VERSION3;

    static final int LDAP_REF_FOLLOW = 0x01;            // follow referrals
    static final int LDAP_REF_THROW = 0x02;             // throw referral ex.
    static final int LDAP_REF_IGNORE = 0x03;            // ignore referrals
    static final int LDAP_REF_FOLLOW_SCHEME = 0x04;     // follow referrals of the same scheme

    static final String LDAP_URL = "ldap://";           // LDAPv3
    static final String LDAPS_URL = "ldaps://";         // LDAPv3

    static final int LBER_BOOLEAN = 0x01;
    static final int LBER_INTEGER = 0x02;
    static final int LBER_BITSTRING = 0x03;
    static final int LBER_OCTETSTRING = 0x04;
    static final int LBER_NULL = 0x05;
    static final int LBER_ENUMERATED = 0x0a;
    static final int LBER_SEQUENCE = 0x30;
    static final int LBER_SET = 0x31;

    static final int LDAP_SUPERIOR_DN = 0x80;

    static final int LDAP_REQ_BIND = 0x60;      // app + constructed
    static final int LDAP_REQ_UNBIND = 0x42;    // app + primitive
    static final int LDAP_REQ_SEARCH = 0x63;    // app + constructed
    static final int LDAP_REQ_MODIFY = 0x66;    // app + constructed
    static final int LDAP_REQ_ADD = 0x68;       // app + constructed
    static final int LDAP_REQ_DELETE = 0x4a;    // app + primitive
    static final int LDAP_REQ_MODRDN = 0x6c;    // app + constructed
    static final int LDAP_REQ_COMPARE = 0x6e;   // app + constructed
    static final int LDAP_REQ_ABANDON = 0x50;   // app + primitive
    static final int LDAP_REQ_EXTENSION = 0x77; // app + constructed    (LDAPv3)

    static final int LDAP_REP_BIND = 0x61;      // app + constructed | 1
    static final int LDAP_REP_SEARCH = 0x64;    // app + constructed | 4
    static final int LDAP_REP_SEARCH_REF = 0x73;// app + constructed    (LDAPv3)
    static final int LDAP_REP_RESULT = 0x65;    // app + constructed | 5
    static final int LDAP_REP_MODIFY = 0x67;    // app + constructed | 7
    static final int LDAP_REP_ADD = 0x69;       // app + constructed | 9
    static final int LDAP_REP_DELETE = 0x6b;    // app + primitive | b
    static final int LDAP_REP_MODRDN = 0x6d;    // app + primitive | d
    static final int LDAP_REP_COMPARE = 0x6f;   // app + primitive | f
    static final int LDAP_REP_EXTENSION = 0x78; // app + constructed    (LDAPv3)

    static final int LDAP_REP_REFERRAL = 0xa3;  // ctx + constructed    (LDAPv3)
    static final int LDAP_REP_EXT_OID = 0x8a;   // ctx + primitive      (LDAPv3)
    static final int LDAP_REP_EXT_VAL = 0x8b;   // ctx + primitive      (LDAPv3)

    // LDAPv3 Controls

    static final int LDAP_CONTROLS = 0xa0;      // ctx + constructed    (LDAPv3)
    static final String LDAP_CONTROL_MANAGE_DSA_IT = "2.16.840.1.113730.3.4.2";
    static final String LDAP_CONTROL_PREFERRED_LANG = "1.3.6.1.4.1.1466.20035";
    static final String LDAP_CONTROL_PAGED_RESULTS = "1.2.840.113556.1.4.319";
    static final String LDAP_CONTROL_SERVER_SORT_REQ = "1.2.840.113556.1.4.473";
    static final String LDAP_CONTROL_SERVER_SORT_RES = "1.2.840.113556.1.4.474";

    ////////////////////////////////////////////////////////////////////////////
    //
    // return codes
    //
    ////////////////////////////////////////////////////////////////////////////

    static final int LDAP_SUCCESS = 0;
    static final int LDAP_OPERATIONS_ERROR = 1;
    static final int LDAP_PROTOCOL_ERROR = 2;
    static final int LDAP_TIME_LIMIT_EXCEEDED = 3;
    static final int LDAP_SIZE_LIMIT_EXCEEDED = 4;
    static final int LDAP_COMPARE_FALSE = 5;
    static final int LDAP_COMPARE_TRUE = 6;
    static final int LDAP_AUTH_METHOD_NOT_SUPPORTED = 7;
    static final int LDAP_STRONG_AUTH_REQUIRED = 8;
    static final int LDAP_PARTIAL_RESULTS = 9;                  // Slapd
    static final int LDAP_REFERRAL = 10;                        // LDAPv3
    static final int LDAP_ADMIN_LIMIT_EXCEEDED = 11;            // LDAPv3
    static final int LDAP_UNAVAILABLE_CRITICAL_EXTENSION = 12;  // LDAPv3
    static final int LDAP_CONFIDENTIALITY_REQUIRED = 13;        // LDAPv3
    static final int LDAP_SASL_BIND_IN_PROGRESS = 14;           // LDAPv3
    static final int LDAP_NO_SUCH_ATTRIBUTE = 16;
    static final int LDAP_UNDEFINED_ATTRIBUTE_TYPE = 17;
    static final int LDAP_INAPPROPRIATE_MATCHING = 18;
    static final int LDAP_CONSTRAINT_VIOLATION = 19;
    static final int LDAP_ATTRIBUTE_OR_VALUE_EXISTS = 20;
    static final int LDAP_INVALID_ATTRIBUTE_SYNTAX = 21;
    static final int LDAP_NO_SUCH_OBJECT = 32;
    static final int LDAP_ALIAS_PROBLEM = 33;
    static final int LDAP_INVALID_DN_SYNTAX = 34;
    static final int LDAP_IS_LEAF = 35;
    static final int LDAP_ALIAS_DEREFERENCING_PROBLEM = 36;
    static final int LDAP_INAPPROPRIATE_AUTHENTICATION = 48;
    static final int LDAP_INVALID_CREDENTIALS = 49;
    static final int LDAP_INSUFFICIENT_ACCESS_RIGHTS = 50;
    static final int LDAP_BUSY = 51;
    static final int LDAP_UNAVAILABLE = 52;
    static final int LDAP_UNWILLING_TO_PERFORM = 53;
    static final int LDAP_LOOP_DETECT = 54;
    static final int LDAP_NAMING_VIOLATION = 64;
    static final int LDAP_OBJECT_CLASS_VIOLATION = 65;
    static final int LDAP_NOT_ALLOWED_ON_NON_LEAF = 66;
    static final int LDAP_NOT_ALLOWED_ON_RDN = 67;
    static final int LDAP_ENTRY_ALREADY_EXISTS = 68;
    static final int LDAP_OBJECT_CLASS_MODS_PROHIBITED = 69;
    static final int LDAP_AFFECTS_MULTIPLE_DSAS = 71;           // LDAPv3
    static final int LDAP_OTHER = 80;

    static final String[] ldap_error_message = {
        "Success",                                      // 0
        "Operations Error",                             // 1
        "Protocol Error",                               // 2
        "Timelimit Exceeded",                           // 3
        "Sizelimit Exceeded",                           // 4
        "Compare False",                                // 5
        "Compare True",                                 // 6
        "Authentication Method Not Supported",          // 7
        "Strong Authentication Required",               // 8
        null,
        "Referral",                                     // 10
        "Administrative Limit Exceeded",                // 11
        "Unavailable Critical Extension",               // 12
        "Confidentiality Required",                     // 13
        "SASL Bind In Progress",                        // 14
        null,
        "No Such Attribute",                            // 16
        "Undefined Attribute Type",                     // 17
        "Inappropriate Matching",                       // 18
        "Constraint Violation",                         // 19
        "Attribute Or Value Exists",                    // 20
        "Invalid Attribute Syntax",                     // 21
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "No Such Object",                               // 32
        "Alias Problem",                                // 33
        "Invalid DN Syntax",                            // 34
        null,
        "Alias Dereferencing Problem",                  // 36
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "Inappropriate Authentication",                 // 48
        "Invalid Credentials",                          // 49
        "Insufficient Access Rights",                   // 50
        "Busy",                                         // 51
        "Unavailable",                                  // 52
        "Unwilling To Perform",                         // 53
        "Loop Detect",                                  // 54
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "Naming Violation",                             // 64
        "Object Class Violation",                       // 65
        "Not Allowed On Non-leaf",                      // 66
        "Not Allowed On RDN",                           // 67
        "Entry Already Exists",                         // 68
        "Object Class Modifications Prohibited",        // 69
        null,
        "Affects Multiple DSAs",                        // 71
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "Other",                                        // 80
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    };


    /*
     * Generate an error message from the LDAP error code and error diagnostic.
     * The message format is:
     *
     *     "[LDAP: error code <errorCode> - <errorMessage>]"
     *
     * where <errorCode> is a numeric error code
     * and <errorMessage> is a textual description of the error (if available)
     *
     */
    static String getErrorMessage(int errorCode, String errorMessage) {

        String message = "[LDAP: error code " + errorCode;

        if ((errorMessage != null) && (errorMessage.length() != 0)) {

            // append error message from the server
            message = message + " - " + errorMessage + "]";

        } else {

            // append built-in error message
            try {
                if (ldap_error_message[errorCode] != null) {
                    message = message + " - " + ldap_error_message[errorCode] +
                                "]";
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                message = message + "]";
            }
        }
        return message;
    }


    ////////////////////////////////////////////////////////////////////////////
    //
    // Unsolicited notification support.
    //
    // An LdapClient maintains a list of LdapCtx that have registered
    // for UnsolicitedNotifications. This is a list because a single
    // LdapClient might be shared among multiple contexts.
    //
    // When addUnsolicited() is invoked, the LdapCtx is added to the list.
    //
    // When Connection receives an unsolicited notification (msgid == 0),
    // it invokes LdapClient.processUnsolicited(). processUnsolicited()
    // parses the Extended Response. If there are registered listeners,
    // LdapClient creates an UnsolicitedNotification from the response
    // and informs each LdapCtx to fire an event for the notification.
    // If it is a DISCONNECT notification, the connection is closed and a
    // NamingExceptionEvent is fired to the listeners.
    //
    // When the connection is closed out-of-band like this, the next
    // time a method is invoked on LdapClient, an IOException is thrown.
    //
    // removeUnsolicited() is invoked to remove an LdapCtx from this client.
    //
    ////////////////////////////////////////////////////////////////////////////
    private Vector<LdapCtx> unsolicited = new Vector<>(3);
    void addUnsolicited(LdapCtx ctx) {
        if (debug > 0) {
            System.err.println("LdapClient.addUnsolicited" + ctx);
        }
        unsolicited.addElement(ctx);
    }

    void removeUnsolicited(LdapCtx ctx) {
        if (debug > 0) {
            System.err.println("LdapClient.removeUnsolicited" + ctx);
        }
            unsolicited.removeElement(ctx);
        }

    // NOTE: Cannot be synchronized because this is called asynchronously
    // by the reader thread in Connection. Instead, sync on 'unsolicited' Vector.
    void processUnsolicited(BerDecoder ber) {
        if (debug > 0) {
            System.err.println("LdapClient.processUnsolicited");
        }
        try {
            // Parse the response
            LdapResult res = new LdapResult();

            ber.parseSeq(null); // init seq
            ber.parseInt();             // msg id; should be 0; ignored
            if (ber.parseByte() != LDAP_REP_EXTENSION) {
                throw new IOException(
                    "Unsolicited Notification must be an Extended Response");
            }
            ber.parseLength();
            parseExtResponse(ber, res);

            if (DISCONNECT_OID.equals(res.extensionId)) {
                // force closing of connection
                forceClose(pooled);
            }

            LdapCtx first = null;
            UnsolicitedNotification notice = null;

            synchronized (unsolicited) {
                if (unsolicited.size() > 0) {
                    first = unsolicited.elementAt(0);

                    // Create an UnsolicitedNotification using the parsed data
                    // Need a 'ctx' object because we want to use the context's
                    // list of provider control factories.
                    notice = new UnsolicitedResponseImpl(
                        res.extensionId,
                        res.extensionValue,
                        res.referrals,
                        res.status,
                        res.errorMessage,
                        res.matchedDN,
                        (res.resControls != null) ?
                        first.convertControls(res.resControls) :
                        null);
                }
            }

            if (notice != null) {
                // Fire UnsolicitedNotification events to listeners
                notifyUnsolicited(notice);

                // If "disconnect" notification,
                // notify unsolicited listeners via NamingException
                if (DISCONNECT_OID.equals(res.extensionId)) {
                    notifyUnsolicited(
                        new CommunicationException("Connection closed"));
                }
            }
        } catch (IOException e) {
            NamingException ne = new CommunicationException(
                "Problem parsing unsolicited notification");
            ne.setRootCause(e);

            notifyUnsolicited(ne);

        } catch (NamingException e) {
            notifyUnsolicited(e);
        }
    }


    private void notifyUnsolicited(Object e) {
        Vector<LdapCtx> unsolicitedCopy;
        synchronized (unsolicited) {
            unsolicitedCopy = new Vector<>(unsolicited);
            if (e instanceof NamingException) {
                unsolicited.setSize(0);  // no more listeners after exception
            }
        }
        for (int i = 0; i < unsolicitedCopy.size(); i++) {
            unsolicitedCopy.elementAt(i).fireUnsolicited(e);
        }
    }

    private void ensureOpen() throws IOException {
        if (conn == null || !conn.useable) {
            if (conn != null && conn.closureReason != null) {
                throw conn.closureReason;
            } else {
                throw new IOException("connection closed");
            }
        }
    }

    // package private (used by LdapCtx)
    static LdapClient getInstance(boolean usePool, String hostname, int port,
        String factory, int connectTimeout, int readTimeout, OutputStream trace,
        int version, String authMechanism, Control[] ctls, String protocol,
        String user, Object passwd, Hashtable<?,?> env) throws NamingException {

        if (usePool) {
            if (LdapPoolManager.isPoolingAllowed(factory, trace,
                    authMechanism, protocol, env)) {
                LdapClient answer = LdapPoolManager.getLdapClient(
                        hostname, port, factory, connectTimeout, readTimeout,
                        trace, version, authMechanism, ctls, protocol, user,
                        passwd, env);
                answer.referenceCount = 1;   // always one when starting out
                return answer;
            }
        }
        return new LdapClient(hostname, port, factory, connectTimeout,
                                        readTimeout, trace, null);
    }
}
