/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import org.ietf.jgss.*;
import sun.security.util.HexDumpEncoder;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.spi.*;
import sun.security.jgss.TokenTracker;
import sun.security.krb5.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.Provider;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.Key;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.ServicePermission;
import javax.security.auth.kerberos.KerberosCredMessage;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import sun.security.krb5.internal.Ticket;
import sun.security.krb5.internal.AuthorizationData;

/**
 * Implements the mechanism specific context class for the Kerberos v5
 * GSS-API mechanism.
 *
 * @author Mayank Upadhyay
 * @author Ram Marti
 * @since 1.4
 */
class Krb5Context implements GSSContextSpi {

    /*
     * The different states that this context can be in.
     */

    private static final int STATE_NEW = 1;
    private static final int STATE_IN_PROCESS = 2;
    private static final int STATE_DONE = 3;
    private static final int STATE_DELETED = 4;

    private int state = STATE_NEW;

    public static final int SESSION_KEY = 0;
    public static final int INITIATOR_SUBKEY = 1;
    public static final int ACCEPTOR_SUBKEY = 2;

    /*
     * Optional features that the application can set and their default
     * values.
     */

    private boolean credDelegState  = false;    // now only useful at client
    private boolean mutualAuthState  = true;
    private boolean replayDetState  = true;
    private boolean sequenceDetState  = true;
    private boolean confState  = true;
    private boolean integState  = true;
    private boolean delegPolicyState = false;

    private boolean isConstrainedDelegationTried = false;

    private int mySeqNumber;
    private int peerSeqNumber;
    private int keySrc;
    private TokenTracker peerTokenTracker;

    private CipherHelper cipherHelper = null;

    /*
     * Separate locks for the sequence numbers allow the application to
     * receive tokens at the same time that it is sending tokens. Note
     * that the application must synchronize the generation and
     * transmission of tokens such that tokens are processed in the same
     * order that they are generated. This is important when sequence
     * checking of per-message tokens is enabled.
     */

    private Object mySeqNumberLock = new Object();
    private Object peerSeqNumberLock = new Object();

    private EncryptionKey key;
    private Krb5NameElement myName;
    private Krb5NameElement peerName;
    private int lifetime;
    private boolean initiator;
    private ChannelBinding channelBinding;

    private Krb5CredElement myCred;
    private Krb5CredElement delegatedCred; // Set only on acceptor side

    // XXX See if the required info from these can be extracted and
    // stored elsewhere
    private Credentials tgt;
    private Credentials serviceCreds;
    private KrbApReq apReq;
    Ticket serviceTicket;
    private final GSSCaller caller;
    private static final boolean DEBUG = Krb5Util.DEBUG;

    /**
     * Constructor for Krb5Context to be called on the context initiator's
     * side.
     */
    Krb5Context(GSSCaller caller, Krb5NameElement peerName, Krb5CredElement myCred,
                int lifetime)
        throws GSSException {

        if (peerName == null)
            throw new IllegalArgumentException("Cannot have null peer name");

        this.caller = caller;
        this.peerName = peerName;
        this.myCred = myCred;
        this.lifetime = lifetime;
        this.initiator = true;
    }

    /**
     * Constructor for Krb5Context to be called on the context acceptor's
     * side.
     */
    Krb5Context(GSSCaller caller, Krb5CredElement myCred)
        throws GSSException {
        this.caller = caller;
        this.myCred = myCred;
        this.initiator = false;
    }

    /**
     * Constructor for Krb5Context to import a previously exported context.
     */
    public Krb5Context(GSSCaller caller, byte[] interProcessToken)
        throws GSSException {
        throw new GSSException(GSSException.UNAVAILABLE,
                               -1, "GSS Import Context not available");
    }

    /**
     * Method to determine if the context can be exported and then
     * re-imported.
     */
    public final boolean isTransferable() throws GSSException {
        return false;
    }

    /**
     * The lifetime remaining for this context.
     */
    public final int getLifetime() {
        // XXX Return service ticket lifetime
        return GSSContext.INDEFINITE_LIFETIME;
    }

    /*
     * Methods that may be invoked by the GSS framework in response
     * to an application request for setting/getting these
     * properties.
     *
     * These can only be called on the initiator side.
     *
     * Notice that an application can only request these
     * properties. The mechanism may or may not support them. The
     * application must make getXXX calls after context establishment
     * to see if the mechanism implementations on both sides support
     * these features. requestAnonymity is an exception where the
     * application will want to call getAnonymityState prior to sending any
     * GSS token during context establishment.
     *
     * Also note that the requests can only be placed before context
     * establishment starts. i.e. when state is STATE_NEW
     */

    /**
     * Requests the desired lifetime. Can only be used on the context
     * initiator's side.
     */
    public void requestLifetime(int lifetime) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            this.lifetime = lifetime;
    }

    /**
     * Requests that confidentiality be available.
     */
    public final void requestConf(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            confState  = value;
    }

    /**
     * Is confidentiality available?
     */
    public final boolean getConfState() {
        return confState;
    }

    /**
     * Requests that integrity be available.
     */
    public final void requestInteg(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            integState  = value;
    }

    /**
     * Is integrity available?
     */
    public final boolean getIntegState() {
        return integState;
    }

    /**
     * Requests that credential delegation be done during context
     * establishment.
     */
    public final void requestCredDeleg(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator()) {
            if (myCred == null || !(myCred instanceof Krb5ProxyCredential)) {
                credDelegState  = value;
            }
        }
    }

    /**
     * Is credential delegation enabled?
     */
    public final boolean getCredDelegState() {
        if (isInitiator()) {
            return credDelegState;
        } else {
            // Server side deleg state is not flagged by credDelegState.
            // It can use constrained delegation.
            tryConstrainedDelegation();
            return delegatedCred != null;
        }
    }

    /**
     * Requests that mutual authentication be done during context
     * establishment. Since this is fromm the client's perspective, it
     * essentially requests that the server be authenticated.
     */
    public final void requestMutualAuth(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator()) {
            mutualAuthState  = value;
        }
    }

    /**
     * Is mutual authentication enabled? Since this is from the client's
     * perspective, it essentially meas that the server is being
     * authenticated.
     */
    public final boolean getMutualAuthState() {
        return mutualAuthState;
    }

    /**
     * Requests that replay detection be done on the GSS wrap and MIC
     * tokens.
     */
    public final void requestReplayDet(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            replayDetState  = value;
    }

    /**
     * Is replay detection enabled on the GSS wrap and MIC tokens?
     * We enable replay detection if sequence checking is enabled.
     */
    public final boolean getReplayDetState() {
        return replayDetState || sequenceDetState;
    }

    /**
     * Requests that sequence checking be done on the GSS wrap and MIC
     * tokens.
     */
    public final void requestSequenceDet(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            sequenceDetState  = value;
    }

    /**
     * Is sequence checking enabled on the GSS Wrap and MIC tokens?
     * We enable sequence checking if replay detection is enabled.
     */
    public final boolean getSequenceDetState() {
        return sequenceDetState || replayDetState;
    }

    /**
     * Requests that the deleg policy be respected.
     */
    public final void requestDelegPolicy(boolean value) {
        if (state == STATE_NEW && isInitiator())
            delegPolicyState = value;
    }

    /**
     * Is deleg policy respected?
     */
    public final boolean getDelegPolicyState() {
        return delegPolicyState;
    }

    /*
     * Anonymity is a little different in that after an application
     * requests anonymity it will want to know whether the mechanism
     * can support it or not, prior to sending any tokens across for
     * context establishment. Since this is from the initiator's
     * perspective, it essentially requests that the initiator be
     * anonymous.
     */

    public final void requestAnonymity(boolean value) throws GSSException {
        // Ignore silently. Application will check back with
        // getAnonymityState.
    }

    // RFC 2853 actually calls for this to be called after context
    // establishment to get the right answer, but that is
    // incorrect. The application may not want to send over any
    // tokens if anonymity is not available.
    public final boolean getAnonymityState() {
        return false;
    }

    /*
     * Package private methods invoked by other Krb5 plugin classes.
     */

    /**
     * Get the context specific DESCipher instance, invoked in
     * MessageToken.init()
     */
    final CipherHelper getCipherHelper(EncryptionKey ckey) throws GSSException {
         EncryptionKey cipherKey = null;
         if (cipherHelper == null) {
            cipherKey = (getKey() == null) ? ckey: getKey();
            cipherHelper = new CipherHelper(cipherKey);
         }
         return cipherHelper;
    }

    final int incrementMySequenceNumber() {
        int retVal;
        synchronized (mySeqNumberLock) {
            retVal = mySeqNumber;
            mySeqNumber = retVal + 1;
        }
        return retVal;
    }

    final void resetMySequenceNumber(int seqNumber) {
        if (DEBUG) {
            System.out.println("Krb5Context setting mySeqNumber to: "
                               + seqNumber);
        }
        synchronized (mySeqNumberLock) {
            mySeqNumber = seqNumber;
        }
    }

    final void resetPeerSequenceNumber(int seqNumber) {
        if (DEBUG) {
            System.out.println("Krb5Context setting peerSeqNumber to: "
                               + seqNumber);
        }
        synchronized (peerSeqNumberLock) {
            peerSeqNumber = seqNumber;
            peerTokenTracker = new TokenTracker(peerSeqNumber);
        }
    }

    final void setKey(int keySrc, EncryptionKey key) throws GSSException {
        this.key = key;
        this.keySrc = keySrc;
        // %%% to do: should clear old cipherHelper first
        cipherHelper = new CipherHelper(key);  // Need to use new key
    }

    public final int getKeySrc() {
        return keySrc;
    }

    private final EncryptionKey getKey() {
        return key;
    }

    /**
     * Called on the acceptor side to store the delegated credentials
     * received in the AcceptSecContextToken.
     */
    final void setDelegCred(Krb5CredElement delegatedCred) {
        this.delegatedCred = delegatedCred;
    }

    /*
     * While the application can only request the following features,
     * other classes in the package can call the actual set methods
     * for them. They are called as context establishment tokens are
     * received on an acceptor side and the context feature list that
     * the initiator wants becomes known.
     */

    /*
     * This method is also called by InitialToken.OverloadedChecksum if the
     * TGT is not forwardable and the user requested delegation.
     */
    final void setCredDelegState(boolean state) {
        credDelegState = state;
    }

    final void setMutualAuthState(boolean state) {
        mutualAuthState = state;
    }

    final void setReplayDetState(boolean state) {
        replayDetState = state;
    }

    final void setSequenceDetState(boolean state) {
        sequenceDetState = state;
    }

    final void setConfState(boolean state) {
        confState = state;
    }

    final void setIntegState(boolean state) {
        integState = state;
    }

    final void setDelegPolicyState(boolean state) {
        delegPolicyState = state;
    }

    /**
     * Sets the channel bindings to be used during context
     * establishment.
     */
    public final void setChannelBinding(ChannelBinding channelBinding)
        throws GSSException {
        this.channelBinding = channelBinding;
    }

    final ChannelBinding getChannelBinding() {
        return channelBinding;
    }

    /**
     * Returns the mechanism oid.
     *
     * @return the Oid of this context
     */
    public final Oid getMech() {
        return (Krb5MechFactory.GSS_KRB5_MECH_OID);
    }

    /**
     * Returns the context initiator name.
     *
     * @return initiator name
     * @exception GSSException
     */
    public final GSSNameSpi getSrcName() throws GSSException {
        return (isInitiator()? myName : peerName);
    }

    /**
     * Returns the context acceptor.
     *
     * @return context acceptor(target) name
     * @exception GSSException
     */
    public final GSSNameSpi getTargName() throws GSSException {
        return (!isInitiator()? myName : peerName);
    }

    /**
     * Returns the delegated credential for the context. This
     * is an optional feature of contexts which not all
     * mechanisms will support. A context can be requested to
     * support credential delegation by using the <b>CRED_DELEG</b>,
     * or it can request for a constrained delegation.
     * This is only valid on the acceptor side of the context.
     * @return GSSCredentialSpi object for the delegated credential
     * @exception GSSException
     * @see GSSContext#getDelegCredState
     */
    public final GSSCredentialSpi getDelegCred() throws GSSException {
        if (state != STATE_IN_PROCESS && state != STATE_DONE)
            throw new GSSException(GSSException.NO_CONTEXT);
        if (isInitiator()) {
            throw new GSSException(GSSException.NO_CRED);
        }
        tryConstrainedDelegation();
        if (delegatedCred == null) {
            throw new GSSException(GSSException.NO_CRED);
        }
        return delegatedCred;
    }

    private void tryConstrainedDelegation() {
        if (state != STATE_IN_PROCESS && state != STATE_DONE) {
            return;
        }
        // We will only try constrained delegation once (if necessary).
        if (!isConstrainedDelegationTried) {
            if (delegatedCred == null) {
                if (DEBUG) {
                    System.out.println(">>> Constrained deleg from " + caller);
                }
                // The constrained delegation part. The acceptor needs to have
                // isInitiator=true in order to get a TGT, either earlier at
                // logon stage, if useSubjectCredsOnly, or now.
                try {
                    delegatedCred = new Krb5ProxyCredential(
                        Krb5InitCredential.getInstance(
                            GSSCaller.CALLER_ACCEPT, myName, lifetime),
                        peerName, serviceTicket);
                } catch (GSSException gsse) {
                    // OK, delegatedCred is null then
                }
            }
            isConstrainedDelegationTried = true;
        }
    }
    /**
     * Tests if this is the initiator side of the context.
     *
     * @return boolean indicating if this is initiator (true)
     *  or target (false)
     */
    public final boolean isInitiator() {
        return initiator;
    }

    /**
     * Tests if the context can be used for per-message service.
     * Context may allow the calls to the per-message service
     * functions before being fully established.
     *
     * @return boolean indicating if per-message methods can
     *  be called.
     */
    public final boolean isProtReady() {
        return (state == STATE_DONE);
    }

    /**
     * Initiator context establishment call. This method may be
     * required to be called several times. A CONTINUE_NEEDED return
     * call indicates that more calls are needed after the next token
     * is received from the peer.
     *
     * @param is contains the token received from the peer. On the
     *  first call it will be ignored.
     * @return any token required to be sent to the peer
     *    It is responsibility of the caller
     *    to send the token to its peer for processing.
     * @exception GSSException
     */
    public final byte[] initSecContext(InputStream is, int mechTokenSize)
        throws GSSException {

            byte[] retVal = null;
            InitialToken token = null;
            int errorCode = GSSException.FAILURE;
            if (DEBUG) {
                System.out.println("Entered Krb5Context.initSecContext with " +
                                   "state=" + printState(state));
            }
            if (!isInitiator()) {
                throw new GSSException(GSSException.FAILURE, -1,
                                       "initSecContext on an acceptor " +
                                        "GSSContext");
            }

            try {
                if (state == STATE_NEW) {
                    state = STATE_IN_PROCESS;

                    errorCode = GSSException.NO_CRED;

                    if (myCred == null) {
                        myCred = Krb5InitCredential.getInstance(caller, myName,
                                              GSSCredential.DEFAULT_LIFETIME);
                        myCred = Krb5ProxyCredential.tryImpersonation(
                                caller, (Krb5InitCredential)myCred);
                    } else if (!myCred.isInitiatorCredential()) {
                        throw new GSSException(errorCode, -1,
                                           "No TGT available");
                    }
                    myName = (Krb5NameElement) myCred.getName();
                    final Krb5ProxyCredential second;
                    if (myCred instanceof Krb5InitCredential) {
                        second = null;
                        tgt = ((Krb5InitCredential) myCred).getKrb5Credentials();
                    } else {
                        second = (Krb5ProxyCredential) myCred;
                        tgt = second.self.getKrb5Credentials();
                    }

                    checkPermission(peerName.getKrb5PrincipalName().getName(),
                                    "initiate");
                    /*
                     * If useSubjectCredsonly is true then
                     * we check whether we already have the ticket
                     * for this service in the Subject and reuse it
                     */

                    @SuppressWarnings("removal")
                    final AccessControlContext acc =
                        AccessController.getContext();

                    if (GSSUtil.useSubjectCredsOnly(caller)) {
                        KerberosTicket kerbTicket = null;
                        try {
                           // get service ticket from caller's subject
                           @SuppressWarnings("removal")
                           var tmp = AccessController.doPrivileged(
                                new PrivilegedExceptionAction<KerberosTicket>() {
                                public KerberosTicket run() throws Exception {
                                    // XXX to be cleaned
                                    // highly consider just calling:
                                    // Subject.getSubject
                                    // SubjectComber.find
                                    // instead of Krb5Util.getServiceTicket
                                    return Krb5Util.getServiceTicket(
                                        GSSCaller.CALLER_UNKNOWN,
                                        // since it's useSubjectCredsOnly here,
                                        // don't worry about the null
                                        second == null ?
                                            myName.getKrb5PrincipalName().getName():
                                            second.getName().getKrb5PrincipalName().getName(),
                                        peerName.getKrb5PrincipalName().getName(),
                                        acc);
                                }});
                            kerbTicket = tmp;
                        } catch (PrivilegedActionException e) {
                            if (DEBUG) {
                                System.out.println("Attempt to obtain service"
                                        + " ticket from the subject failed!");
                            }
                        }
                        if (kerbTicket != null) {
                            if (DEBUG) {
                                System.out.println("Found service ticket in " +
                                                   "the subject" +
                                                   kerbTicket);
                            }

                            // convert Ticket to serviceCreds
                            // XXX Should merge these two object types
                            // avoid converting back and forth
                            serviceCreds = Krb5Util.ticketToCreds(kerbTicket);
                        }
                    }
                    if (serviceCreds == null) {
                        // either we did not find the serviceCreds in the
                        // Subject or useSubjectCreds is false
                        if (DEBUG) {
                            System.out.println("Service ticket not found in " +
                                               "the subject");
                        }
                        // Get Service ticket using the Kerberos protocols
                        if (second == null) {
                            serviceCreds = Credentials.acquireServiceCreds(
                                     peerName.getKrb5PrincipalName().getName(),
                                     tgt);
                        } else {
                            serviceCreds = Credentials.acquireS4U2proxyCreds(
                                    peerName.getKrb5PrincipalName().getName(),
                                    second.tkt,
                                    second.getName().getKrb5PrincipalName(),
                                    tgt);
                        }
                        if (GSSUtil.useSubjectCredsOnly(caller)) {
                            @SuppressWarnings("removal")
                            final Subject subject =
                                AccessController.doPrivileged(
                                new java.security.PrivilegedAction<Subject>() {
                                    public Subject run() {
                                        return (Subject.getSubject(acc));
                                    }
                                });
                            if (subject != null &&
                                !subject.isReadOnly()) {
                                /*
                                 * Store the service credentials as
                                 * javax.security.auth.kerberos.KerberosTicket in
                                 * the Subject. We could wait until the context is
                                 * successfully established; however it is easier
                                 * to do it here and there is no harm.
                                 */
                                final KerberosTicket kt =
                                        Krb5Util.credsToTicket(serviceCreds);
                                @SuppressWarnings("removal")
                                var dummy = AccessController.doPrivileged (
                                    new java.security.PrivilegedAction<Void>() {
                                      public Void run() {
                                        subject.getPrivateCredentials().add(kt);
                                        return null;
                                      }
                                    });
                            } else {
                                // log it for debugging purpose
                                if (DEBUG) {
                                    System.out.println("Subject is " +
                                        "readOnly;Kerberos Service "+
                                        "ticket not stored");
                                }
                            }
                        }
                    }

                    errorCode = GSSException.FAILURE;
                    token = new InitSecContextToken(this, tgt, serviceCreds);
                    apReq = ((InitSecContextToken)token).getKrbApReq();
                    retVal = token.encode();
                    myCred = null;
                    if (!getMutualAuthState()) {
                        state = STATE_DONE;
                    }
                    if (DEBUG) {
                        System.out.println("Created InitSecContextToken:\n"+
                            new HexDumpEncoder().encodeBuffer(retVal));
                    }
                } else if (state == STATE_IN_PROCESS) {
                    // No need to write anything;
                    // just validate the incoming token
                    new AcceptSecContextToken(this, serviceCreds, apReq, is);
                    apReq = null;
                    state = STATE_DONE;
                } else {
                    // XXX Use logging API?
                    if (DEBUG) {
                        System.out.println(state);
                    }
                }
            } catch (KrbException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                GSSException gssException =
                        new GSSException(errorCode, -1, e.getMessage());
                gssException.initCause(e);
                throw gssException;
            } catch (IOException e) {
                GSSException gssException =
                        new GSSException(errorCode, -1, e.getMessage());
                gssException.initCause(e);
                throw gssException;
            }
            return retVal;
        }

    public final boolean isEstablished() {
        return (state == STATE_DONE);
    }

    /**
     * Acceptor's context establishment call. This method may be
     * required to be called several times. A CONTINUE_NEEDED return
     * call indicates that more calls are needed after the next token
     * is received from the peer.
     *
     * @param is contains the token received from the peer.
     * @return any token required to be sent to the peer
     *    It is responsibility of the caller
     *    to send the token to its peer for processing.
     * @exception GSSException
     */
    public final byte[] acceptSecContext(InputStream is, int mechTokenSize)
        throws GSSException {

        byte[] retVal = null;

        if (DEBUG) {
            System.out.println("Entered Krb5Context.acceptSecContext with " +
                               "state=" +  printState(state));
        }

        if (isInitiator()) {
            throw new GSSException(GSSException.FAILURE, -1,
                                   "acceptSecContext on an initiator " +
                                   "GSSContext");
        }
        try {
            if (state == STATE_NEW) {
                state = STATE_IN_PROCESS;
                if (myCred == null) {
                    myCred = Krb5AcceptCredential.getInstance(caller, myName);
                } else if (!myCred.isAcceptorCredential()) {
                    throw new GSSException(GSSException.NO_CRED, -1,
                                           "No Secret Key available");
                }
                myName = (Krb5NameElement) myCred.getName();

                // If there is already a bound name, check now
                if (myName != null) {
                    Krb5MechFactory.checkAcceptCredPermission(myName, myName);
                }

                InitSecContextToken token = new InitSecContextToken(this,
                                                    (Krb5AcceptCredential) myCred, is);
                PrincipalName clientName = token.getKrbApReq().getClient();
                peerName = Krb5NameElement.getInstance(clientName);

                // If unbound, check after the bound name is found
                if (myName == null) {
                    myName = Krb5NameElement.getInstance(
                        token.getKrbApReq().getCreds().getServer());
                    Krb5MechFactory.checkAcceptCredPermission(myName, myName);
                }

                if (getMutualAuthState()) {
                        retVal = new AcceptSecContextToken(this,
                                          token.getKrbApReq()).encode();
                }
                serviceTicket = token.getKrbApReq().getCreds().getTicket();
                myCred = null;
                state = STATE_DONE;
            } else  {
                // XXX Use logging API?
                if (DEBUG) {
                    System.out.println(state);
                }
            }
        } catch (KrbException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }

        return retVal;
    }

    /**
     * Queries the context for largest data size to accommodate
     * the specified protection and be <= maxTokSize.
     *
     * @param qop the quality of protection that the context will be
     *  asked to provide.
     * @param confReq a flag indicating whether confidentiality will be
     *  requested or not
     * @param outputSize the maximum size of the output token
     * @return the maximum size for the input message that can be
     *  provided to the wrap() method in order to guarantee that these
     *  requirements are met.
     * @throws GSSException
     */
    public final int getWrapSizeLimit(int qop, boolean confReq,
                                       int maxTokSize) throws GSSException {

        int retVal = 0;
        if (cipherHelper.getProto() == 0) {
            retVal = WrapToken.getSizeLimit(qop, confReq, maxTokSize,
                                        getCipherHelper(null));
        } else if (cipherHelper.getProto() == 1) {
            retVal = WrapToken_v2.getSizeLimit(qop, confReq, maxTokSize,
                                        getCipherHelper(null));
        }
        return retVal;
    }

    /*
     * Per-message calls depend on the sequence number. The sequence number
     * synchronization is at a finer granularity because wrap and getMIC
     * care about the local sequence number (mySeqNumber) where are unwrap
     * and verifyMIC care about the remote sequence number (peerSeqNumber).
     */

    public final byte[] wrap(byte[] inBuf, int offset, int len,
                             MessageProp msgProp) throws GSSException {
        if (DEBUG) {
            System.out.println("Krb5Context.wrap: data=["
                               + getHexBytes(inBuf, offset, len)
                               + "]");
        }

        if (state != STATE_DONE)
        throw new GSSException(GSSException.NO_CONTEXT, -1,
                               "Wrap called in invalid state!");

        byte[] encToken = null;
        try {
            if (cipherHelper.getProto() == 0) {
                WrapToken token =
                        new WrapToken(this, msgProp, inBuf, offset, len);
                encToken = token.encode();
            } else if (cipherHelper.getProto() == 1) {
                WrapToken_v2 token =
                        new WrapToken_v2(this, msgProp, inBuf, offset, len);
                encToken = token.encode();
            }
            if (DEBUG) {
                System.out.println("Krb5Context.wrap: token=["
                                   + getHexBytes(encToken, 0, encToken.length)
                                   + "]");
            }
            return encToken;
        } catch (IOException e) {
            encToken = null;
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
    }

    public final int wrap(byte[] inBuf, int inOffset, int len,
                          byte[] outBuf, int outOffset,
                          MessageProp msgProp) throws GSSException {

        if (state != STATE_DONE)
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                   "Wrap called in invalid state!");

        int retVal = 0;
        try {
            if (cipherHelper.getProto() == 0) {
                WrapToken token =
                        new WrapToken(this, msgProp, inBuf, inOffset, len);
                retVal = token.encode(outBuf, outOffset);
            } else if (cipherHelper.getProto() == 1) {
                WrapToken_v2 token =
                        new WrapToken_v2(this, msgProp, inBuf, inOffset, len);
                retVal = token.encode(outBuf, outOffset);
            }
            if (DEBUG) {
                System.out.println("Krb5Context.wrap: token=["
                                   + getHexBytes(outBuf, outOffset, retVal)
                                   + "]");
            }
            return retVal;
        } catch (IOException e) {
            retVal = 0;
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
    }

    public final void wrap(byte[] inBuf, int offset, int len,
                           OutputStream os, MessageProp msgProp)
        throws GSSException {

        if (state != STATE_DONE)
                throw new GSSException(GSSException.NO_CONTEXT, -1,
                                       "Wrap called in invalid state!");

        byte[] encToken = null;
        try {
            if (cipherHelper.getProto() == 0) {
                WrapToken token =
                        new WrapToken(this, msgProp, inBuf, offset, len);
                token.encode(os);
                if (DEBUG) {
                    encToken = token.encode();
                }
            } else if (cipherHelper.getProto() == 1) {
                WrapToken_v2 token =
                        new WrapToken_v2(this, msgProp, inBuf, offset, len);
                token.encode(os);
                if (DEBUG) {
                    encToken = token.encode();
                }
            }
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }

        if (DEBUG) {
            System.out.println("Krb5Context.wrap: token=["
                        + getHexBytes(encToken, 0, encToken.length)
                        + "]");
        }
    }

    public final void wrap(InputStream is, OutputStream os,
                            MessageProp msgProp) throws GSSException {

        byte[] data;
        try {
            data = new byte[is.available()];
            is.read(data);
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
        wrap(data, 0, data.length, os, msgProp);
    }

    public final byte[] unwrap(byte[] inBuf, int offset, int len,
                               MessageProp msgProp)
        throws GSSException {

            if (DEBUG) {
                System.out.println("Krb5Context.unwrap: token=["
                                   + getHexBytes(inBuf, offset, len)
                                   + "]");
            }

            if (state != STATE_DONE) {
                throw new GSSException(GSSException.NO_CONTEXT, -1,
                                       " Unwrap called in invalid state!");
            }

            byte[] data = null;
            if (cipherHelper.getProto() == 0) {
                WrapToken token =
                        new WrapToken(this, inBuf, offset, len, msgProp);
                data = token.getData();
                setSequencingAndReplayProps(token, msgProp);
            } else if (cipherHelper.getProto() == 1) {
                WrapToken_v2 token =
                        new WrapToken_v2(this, inBuf, offset, len, msgProp);
                data = token.getData();
                setSequencingAndReplayProps(token, msgProp);
            }

            if (DEBUG) {
                System.out.println("Krb5Context.unwrap: data=["
                                   + getHexBytes(data, 0, data.length)
                                   + "]");
            }

            return data;
        }

    public final int unwrap(byte[] inBuf, int inOffset, int len,
                             byte[] outBuf, int outOffset,
                             MessageProp msgProp) throws GSSException {

        if (state != STATE_DONE)
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                   "Unwrap called in invalid state!");

        if (cipherHelper.getProto() == 0) {
            WrapToken token =
                        new WrapToken(this, inBuf, inOffset, len, msgProp);
            len = token.getData(outBuf, outOffset);
            setSequencingAndReplayProps(token, msgProp);
        } else if (cipherHelper.getProto() == 1) {
            WrapToken_v2 token =
                        new WrapToken_v2(this, inBuf, inOffset, len, msgProp);
            len = token.getData(outBuf, outOffset);
            setSequencingAndReplayProps(token, msgProp);
        }
        return len;
    }

    public final int unwrap(InputStream is,
                            byte[] outBuf, int outOffset,
                            MessageProp msgProp) throws GSSException {

        if (state != STATE_DONE)
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                   "Unwrap called in invalid state!");

        int len = 0;
        if (cipherHelper.getProto() == 0) {
            WrapToken token = new WrapToken(this, is, msgProp);
            len = token.getData(outBuf, outOffset);
            setSequencingAndReplayProps(token, msgProp);
        } else if (cipherHelper.getProto() == 1) {
            WrapToken_v2 token = new WrapToken_v2(this, is, msgProp);
            len = token.getData(outBuf, outOffset);
            setSequencingAndReplayProps(token, msgProp);
        }
        return len;
    }


    public final void unwrap(InputStream is, OutputStream os,
                             MessageProp msgProp) throws GSSException {

        if (state != STATE_DONE)
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                   "Unwrap called in invalid state!");

        byte[] data = null;
        if (cipherHelper.getProto() == 0) {
            WrapToken token = new WrapToken(this, is, msgProp);
            data = token.getData();
            setSequencingAndReplayProps(token, msgProp);
        } else if (cipherHelper.getProto() == 1) {
            WrapToken_v2 token = new WrapToken_v2(this, is, msgProp);
            data = token.getData();
            setSequencingAndReplayProps(token, msgProp);
        }

        try {
            os.write(data);
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
    }

    public final byte[] getMIC(byte[] inMsg, int offset, int len,
                               MessageProp msgProp)
        throws GSSException {

            byte[] micToken = null;
            try {
                if (cipherHelper.getProto() == 0) {
                    MicToken token =
                        new MicToken(this, msgProp, inMsg, offset, len);
                    micToken = token.encode();
                } else if (cipherHelper.getProto() == 1) {
                    MicToken_v2 token =
                        new MicToken_v2(this, msgProp, inMsg, offset, len);
                    micToken = token.encode();
                }
                return micToken;
            } catch (IOException e) {
                micToken = null;
                GSSException gssException =
                    new GSSException(GSSException.FAILURE, -1, e.getMessage());
                gssException.initCause(e);
                throw gssException;
            }
        }

    private int getMIC(byte[] inMsg, int offset, int len,
                       byte[] outBuf, int outOffset,
                       MessageProp msgProp)
        throws GSSException {

        int retVal = 0;
        try {
            if (cipherHelper.getProto() == 0) {
                MicToken token =
                        new MicToken(this, msgProp, inMsg, offset, len);
                retVal = token.encode(outBuf, outOffset);
            } else if (cipherHelper.getProto() == 1) {
                MicToken_v2 token =
                        new MicToken_v2(this, msgProp, inMsg, offset, len);
                retVal = token.encode(outBuf, outOffset);
            }
            return retVal;
        } catch (IOException e) {
            retVal = 0;
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
    }

    /*
     * Checksum calculation requires a byte[]. Hence might as well pass
     * a byte[] into the MicToken constructor. However, writing the
     * token can be optimized for cases where the application passed in
     * an OutputStream.
     */

    private void getMIC(byte[] inMsg, int offset, int len,
                        OutputStream os, MessageProp msgProp)
        throws GSSException {

        try {
            if (cipherHelper.getProto() == 0) {
                MicToken token =
                        new MicToken(this, msgProp, inMsg, offset, len);
                token.encode(os);
            } else if (cipherHelper.getProto() == 1) {
                MicToken_v2 token =
                        new MicToken_v2(this, msgProp, inMsg, offset, len);
                token.encode(os);
            }
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
    }

    public final void getMIC(InputStream is, OutputStream os,
                              MessageProp msgProp) throws GSSException {
        byte[] data;
        try {
            data = new byte[is.available()];
            is.read(data);
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
        getMIC(data, 0, data.length, os, msgProp);
    }

    public final void verifyMIC(byte[] inTok, int tokOffset, int tokLen,
                                byte[] inMsg, int msgOffset, int msgLen,
                                MessageProp msgProp)
        throws GSSException {

        if (cipherHelper.getProto() == 0) {
            MicToken token =
                new MicToken(this, inTok, tokOffset, tokLen, msgProp);
            token.verify(inMsg, msgOffset, msgLen);
            setSequencingAndReplayProps(token, msgProp);
        } else if (cipherHelper.getProto() == 1) {
            MicToken_v2 token =
                new MicToken_v2(this, inTok, tokOffset, tokLen, msgProp);
            token.verify(inMsg, msgOffset, msgLen);
            setSequencingAndReplayProps(token, msgProp);
        }
    }

    private void verifyMIC(InputStream is,
                           byte[] inMsg, int msgOffset, int msgLen,
                           MessageProp msgProp)
        throws GSSException {

        if (cipherHelper.getProto() == 0) {
            MicToken token = new MicToken(this, is, msgProp);
            token.verify(inMsg, msgOffset, msgLen);
            setSequencingAndReplayProps(token, msgProp);
        } else if (cipherHelper.getProto() == 1) {
            MicToken_v2 token = new MicToken_v2(this, is, msgProp);
            token.verify(inMsg, msgOffset, msgLen);
            setSequencingAndReplayProps(token, msgProp);
        }
    }

    public final void verifyMIC(InputStream is, InputStream msgStr,
                                 MessageProp mProp) throws GSSException {
        byte[] msg;
        try {
            msg = new byte[msgStr.available()];
            msgStr.read(msg);
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }
        verifyMIC(is, msg, 0, msg.length, mProp);
    }

    /**
     * Produces a token representing this context. After this call
     * the context will no longer be usable until an import is
     * performed on the returned token.
     *
     * @param os the output token will be written to this stream
     * @exception GSSException
     */
    public final byte[] export() throws GSSException {
        throw new GSSException(GSSException.UNAVAILABLE, -1,
                               "GSS Export Context not available");
    }

    /**
     * Releases context resources and terminates the
     * context between 2 peer.
     *
     * @exception GSSException with major codes NO_CONTEXT, FAILURE.
     */

    public final void dispose() throws GSSException {
        state = STATE_DELETED;
        delegatedCred = null;
        tgt = null;
        serviceCreds = null;
        key = null;
    }

    public final Provider getProvider() {
        return Krb5MechFactory.PROVIDER;
    }

    /**
     * Sets replay and sequencing information for a message token received
     * form the peer.
     */
    private void setSequencingAndReplayProps(MessageToken token,
                                             MessageProp prop) {
        if (replayDetState || sequenceDetState) {
            int seqNum = token.getSequenceNumber();
            peerTokenTracker.getProps(seqNum, prop);
        }
    }

    /**
     * Sets replay and sequencing information for a message token received
     * form the peer.
     */
    private void setSequencingAndReplayProps(MessageToken_v2 token,
                                             MessageProp prop) {
        if (replayDetState || sequenceDetState) {
            int seqNum = token.getSequenceNumber();
            peerTokenTracker.getProps(seqNum, prop);
        }
    }

    private void checkPermission(String principal, String action) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ServicePermission perm =
                new ServicePermission(principal, action);
            sm.checkPermission(perm);
        }
    }

    private static String getHexBytes(byte[] bytes, int pos, int len) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {

            int b1 = (bytes[i]>>4) & 0x0f;
            int b2 = bytes[i] & 0x0f;

            sb.append(Integer.toHexString(b1));
            sb.append(Integer.toHexString(b2));
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String printState(int state) {
        switch (state) {
          case STATE_NEW:
                return ("STATE_NEW");
          case STATE_IN_PROCESS:
                return ("STATE_IN_PROCESS");
          case STATE_DONE:
                return ("STATE_DONE");
          case STATE_DELETED:
                return ("STATE_DELETED");
          default:
                return ("Unknown state " + state);
        }
    }

    GSSCaller getCaller() {
        // Currently used by InitialToken only
        return caller;
    }

    /**
     * The session key returned by inquireSecContext(KRB5_INQ_SSPI_SESSION_KEY)
     */
    static class KerberosSessionKey implements Key {
        private static final long serialVersionUID = 699307378954123869L;

        @SuppressWarnings("serial") // Not statically typed as Serializable
        private final EncryptionKey key;

        KerberosSessionKey(EncryptionKey key) {
            this.key = key;
        }

        @Override
        public String getAlgorithm() {
            return Integer.toString(key.getEType());
        }

        @Override
        public String getFormat() {
            return "RAW";
        }

        @Override
        public byte[] getEncoded() {
            return key.getBytes().clone();
        }

        @Override
        public String toString() {
            return "Kerberos session key: etype: " + key.getEType() + "\n" +
                    new HexDumpEncoder().encodeBuffer(key.getBytes());
        }
    }

    /**
     * Return the mechanism-specific attribute associated with {@code type}.
     */
    public Object inquireSecContext(String type)
            throws GSSException {
        if (!isEstablished()) {
             throw new GSSException(GSSException.NO_CONTEXT, -1,
                     "Security context not established.");
        }
        switch (type) {
            case "KRB5_GET_SESSION_KEY":
                return new KerberosSessionKey(key);
            case "KRB5_GET_SESSION_KEY_EX":
                return new javax.security.auth.kerberos.EncryptionKey(
                        key.getBytes(), key.getEType());
            case "KRB5_GET_TKT_FLAGS":
                return tktFlags.clone();
            case "KRB5_GET_AUTHZ_DATA":
                if (isInitiator()) {
                    throw new GSSException(GSSException.UNAVAILABLE, -1,
                            "AuthzData not available on initiator side.");
                } else {
                    return authzData;
                }
            case "KRB5_GET_AUTHTIME":
                return authTime;
            case "KRB5_GET_KRB_CRED":
                if (!isInitiator()) {
                    throw new GSSException(GSSException.UNAVAILABLE, -1,
                            "KRB_CRED not available on acceptor side.");
                }
                KerberosPrincipal sender = new KerberosPrincipal(
                        myName.getKrb5PrincipalName().getName());
                KerberosPrincipal recipient = new KerberosPrincipal(
                        peerName.getKrb5PrincipalName().getName());
                try {
                    byte[] krbCred = new KrbCred(tgt, serviceCreds, key)
                            .getMessage();
                    return new KerberosCredMessage(
                            sender, recipient, krbCred);
                } catch (KrbException | IOException e) {
                    GSSException gsse = new GSSException(GSSException.UNAVAILABLE, -1,
                            "KRB_CRED not generated correctly.");
                    gsse.initCause(e);
                    throw gsse;
                }
        }
        throw new GSSException(GSSException.UNAVAILABLE, -1,
                "Inquire type not supported.");
    }

    // Helpers for inquireSecContext
    private boolean[] tktFlags;
    private String authTime;
    private AuthorizationData authzData;

    public void setTktFlags(boolean[] tktFlags) {
        this.tktFlags = tktFlags;
    }

    public void setAuthTime(String authTime) {
        this.authTime = authTime;
    }

    public void setAuthzData(AuthorizationData authzData) {
        this.authzData = authzData;
    }

}
