/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.spnego;

import java.io.*;
import java.security.Provider;
import org.ietf.jgss.*;
import sun.security.action.GetBooleanAction;
import sun.security.jgss.*;
import sun.security.jgss.spi.*;
import sun.security.util.*;

/**
 * Implements the mechanism specific context class for SPNEGO
 * GSS-API mechanism
 *
 * @author Seema Malkani
 * @since 1.6
 */
public class SpNegoContext implements GSSContextSpi {

    /*
     * The different states that this context can be in.
     */
    private static final int STATE_NEW = 1;
    private static final int STATE_IN_PROCESS = 2;
    private static final int STATE_DONE = 3;
    private static final int STATE_DELETED = 4;

    private int state = STATE_NEW;

    /*
     * Optional features that the application can set and their default
     * values.
     */
    private boolean credDelegState = false;
    private boolean mutualAuthState = true;
    private boolean replayDetState = true;
    private boolean sequenceDetState = true;
    private boolean confState = true;
    private boolean integState = true;
    private boolean delegPolicyState = false;

    private GSSNameSpi peerName = null;
    private GSSNameSpi myName = null;
    private SpNegoCredElement myCred = null;

    private GSSContext mechContext = null;
    private byte[] DER_mechTypes = null;

    private int lifetime;
    private ChannelBinding channelBinding;
    private boolean initiator;

    // the underlying negotiated mechanism
    private Oid internal_mech = null;

    // the SpNegoMechFactory that creates this context
    private final SpNegoMechFactory factory;

    // debug property
    static final boolean DEBUG = GetBooleanAction
            .privilegedGetProperty("sun.security.spnego.debug");

    /**
     * Constructor for SpNegoContext to be called on the context initiator's
     * side.
     */
    public SpNegoContext(SpNegoMechFactory factory, GSSNameSpi peerName,
                        GSSCredentialSpi myCred,
                        int lifetime) throws GSSException {

        if (peerName == null)
            throw new IllegalArgumentException("Cannot have null peer name");
        if ((myCred != null) && !(myCred instanceof SpNegoCredElement)) {
            throw new IllegalArgumentException("Wrong cred element type");
        }
        this.peerName = peerName;
        this.myCred = (SpNegoCredElement) myCred;
        this.lifetime = lifetime;
        this.initiator = true;
        this.factory = factory;
    }

    /**
     * Constructor for SpNegoContext to be called on the context acceptor's
     * side.
     */
    public SpNegoContext(SpNegoMechFactory factory, GSSCredentialSpi myCred)
            throws GSSException {
        if ((myCred != null) && !(myCred instanceof SpNegoCredElement)) {
            throw new IllegalArgumentException("Wrong cred element type");
        }
        this.myCred = (SpNegoCredElement) myCred;
        this.initiator = false;
        this.factory = factory;
    }

    /**
     * Constructor for SpNegoContext to import a previously exported context.
     */
    public SpNegoContext(SpNegoMechFactory factory, byte [] interProcessToken)
        throws GSSException {
        throw new GSSException(GSSException.UNAVAILABLE,
                               -1, "GSS Import Context not available");
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
     * Requests that deleg policy be respected.
     */
    public final void requestDelegPolicy(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            delegPolicyState = value;
    }

    /**
     * Is integrity available?
     */
    public final boolean getIntegState() {
        return integState;
    }

    /**
     * Is deleg policy respected?
     */
    public final boolean getDelegPolicyState() {
        if (isInitiator() && mechContext != null &&
                mechContext instanceof GSSContextImpl &&
                (state == STATE_IN_PROCESS || state == STATE_DONE)) {
            return ((GSSContextImpl)mechContext).getDelegPolicyState();
        } else {
            return delegPolicyState;
        }
    }

    /**
     * Requests that credential delegation be done during context
     * establishment.
     */
    public final void requestCredDeleg(boolean value) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            credDelegState  = value;
    }

    /**
     * Is credential delegation enabled?
     */
    public final boolean getCredDelegState() {
        if (isInitiator() && mechContext != null &&
                (state == STATE_IN_PROCESS || state == STATE_DONE)) {
            return mechContext.getCredDelegState();
        } else {
            return credDelegState;
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
     * Returns the mechanism oid.
     *
     * @return the Oid of this context
     */
    public final Oid getMech() {
        if (isEstablished()) {
            return getNegotiatedMech();
        }
        return (SpNegoMechFactory.GSS_SPNEGO_MECH_OID);
    }

    public final Oid getNegotiatedMech() {
        return (internal_mech);
    }

    public final Provider getProvider() {
        return SpNegoMechFactory.PROVIDER;
    }

    public final void dispose() throws GSSException {
        mechContext = null;
        state = STATE_DELETED;
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
     *        first call it will be ignored.
     * @return any token required to be sent to the peer
     * It is responsibility of the caller to send the token
     * to its peer for processing.
     * @exception GSSException
     */
    @Deprecated(since="11")
    public final byte[] initSecContext(InputStream is, int mechTokenSize)
        throws GSSException {

        byte[] retVal = null;
        NegTokenInit initToken = null;
        byte[] mechToken = null;
        int errorCode = GSSException.FAILURE;

        if (DEBUG) {
            System.out.println("Entered SpNego.initSecContext with " +
                                "state=" + printState(state));
        }
        if (!isInitiator()) {
            throw new GSSException(GSSException.FAILURE, -1,
                "initSecContext on an acceptor GSSContext");
        }

        try {
            if (state == STATE_NEW) {
                state = STATE_IN_PROCESS;

                errorCode = GSSException.NO_CRED;

                // determine available mech set
                Oid[] mechList = getAvailableMechs();
                DER_mechTypes = getEncodedMechs(mechList);

                // pull out first mechanism
                internal_mech = mechList[0];

                // get the token for first mechanism
                mechToken = GSS_initSecContext(null);

                errorCode = GSSException.DEFECTIVE_TOKEN;
                // generate SPNEGO token
                initToken = new NegTokenInit(DER_mechTypes, getContextFlags(),
                                        mechToken, null);
                if (DEBUG) {
                    System.out.println("SpNegoContext.initSecContext: " +
                                "sending token of type = " +
                                SpNegoToken.getTokenName(initToken.getType()));
                }
                // get the encoded token
                retVal = initToken.getEncoded();

            } else if (state == STATE_IN_PROCESS) {

                errorCode = GSSException.FAILURE;
                if (is == null) {
                    throw new GSSException(errorCode, -1,
                                "No token received from peer!");
                }

                errorCode = GSSException.DEFECTIVE_TOKEN;
                byte[] server_token = new byte[is.available()];
                SpNegoToken.readFully(is, server_token);
                if (DEBUG) {
                    System.out.println("SpNegoContext.initSecContext: " +
                                        "process received token = " +
                                        SpNegoToken.getHexBytes(server_token));
                }

                // read the SPNEGO token
                // token will be validated when parsing
                NegTokenTarg targToken = new NegTokenTarg(server_token);

                if (DEBUG) {
                    System.out.println("SpNegoContext.initSecContext: " +
                                "received token of type = " +
                                SpNegoToken.getTokenName(targToken.getType()));
                }

                // pull out mechanism
                internal_mech = targToken.getSupportedMech();
                if (internal_mech == null) {
                    // return wth failure
                    throw new GSSException(errorCode, -1,
                                "supported mechanism from server is null");
                }

                // get the negotiated result
                SpNegoToken.NegoResult negoResult = null;
                int result = targToken.getNegotiatedResult();
                switch (result) {
                    case 0:
                        negoResult = SpNegoToken.NegoResult.ACCEPT_COMPLETE;
                        state = STATE_DONE;
                        break;
                    case 1:
                        negoResult = SpNegoToken.NegoResult.ACCEPT_INCOMPLETE;
                        state = STATE_IN_PROCESS;
                        break;
                    case 2:
                        negoResult = SpNegoToken.NegoResult.REJECT;
                        state = STATE_DELETED;
                        break;
                    default:
                        state = STATE_DONE;
                        break;
                }

                errorCode = GSSException.BAD_MECH;

                if (negoResult == SpNegoToken.NegoResult.REJECT) {
                    throw new GSSException(errorCode, -1,
                                        internal_mech.toString());
                }

                errorCode = GSSException.DEFECTIVE_TOKEN;

                if ((negoResult == SpNegoToken.NegoResult.ACCEPT_COMPLETE) ||
                    (negoResult == SpNegoToken.NegoResult.ACCEPT_INCOMPLETE)) {

                    // pull out the mechanism token
                    byte[] accept_token = targToken.getResponseToken();
                    if (accept_token == null) {
                        if (!isMechContextEstablished()) {
                            // return with failure
                            throw new GSSException(errorCode, -1,
                                    "mechanism token from server is null");
                        }
                    } else {
                        mechToken = GSS_initSecContext(accept_token);
                    }
                    // verify MIC
                    if (!GSSUtil.useMSInterop()) {
                        byte[] micToken = targToken.getMechListMIC();
                        if (!verifyMechListMIC(DER_mechTypes, micToken)) {
                            throw new GSSException(errorCode, -1,
                                "verification of MIC on MechList Failed!");
                        }
                    }
                    if (isMechContextEstablished()) {
                        state = STATE_DONE;
                        retVal = mechToken;
                        if (DEBUG) {
                            System.out.println("SPNEGO Negotiated Mechanism = "
                                + internal_mech + " " +
                                GSSUtil.getMechStr(internal_mech));
                        }
                    } else {
                        // generate SPNEGO token
                        initToken = new NegTokenInit(null, null,
                                                mechToken, null);
                        if (DEBUG) {
                            System.out.println("SpNegoContext.initSecContext:" +
                                " continue sending token of type = " +
                                SpNegoToken.getTokenName(initToken.getType()));
                        }
                        // get the encoded token
                        retVal = initToken.getEncoded();
                    }
                }

            } else {
                // XXX Use logging API
                if (DEBUG) {
                    System.out.println(state);
                }
            }
            if (DEBUG) {
                if (retVal != null) {
                    System.out.println("SNegoContext.initSecContext: " +
                        "sending token = " + SpNegoToken.getHexBytes(retVal));
                }
            }
        } catch (GSSException e) {
            GSSException gssException =
                        new GSSException(errorCode, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }

        return retVal;
    }


    /**
     * Acceptor's context establishment call. This method may be
     * required to be called several times. A CONTINUE_NEEDED return
     * call indicates that more calls are needed after the next token
     * is received from the peer.
     *
     * @param is contains the token received from the peer.
     * @return any token required to be sent to the peer
     * It is responsibility of the caller to send the token
     * to its peer for processing.
     * @exception GSSException
     */
    @Deprecated(since="11")
    public final byte[] acceptSecContext(InputStream is, int mechTokenSize)
        throws GSSException {

        byte[] retVal = null;
        SpNegoToken.NegoResult negoResult;
        boolean valid = true;

        if (DEBUG) {
            System.out.println("Entered SpNegoContext.acceptSecContext with " +
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

                // read data
                byte[] token = new byte[is.available()];
                SpNegoToken.readFully(is, token);
                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                                        "receiving token = " +
                                        SpNegoToken.getHexBytes(token));
                }

                // read the SPNEGO token
                // token will be validated when parsing
                NegTokenInit initToken = new NegTokenInit(token);

                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                                "received token of type = " +
                                SpNegoToken.getTokenName(initToken.getType()));
                }

                Oid[] mechList = initToken.getMechTypeList();
                DER_mechTypes = initToken.getMechTypes();
                if (DER_mechTypes == null) {
                    valid = false;
                }

                /*
                 * Select the best match between the list of mechs
                 * that the initiator requested and the list that
                 * the acceptor will support.
                 */
                Oid[] supported_mechSet = getAvailableMechs();
                Oid mech_wanted =
                        negotiate_mech_type(supported_mechSet, mechList);
                if (mech_wanted == null) {
                    valid = false;
                }
                // save the desired mechanism
                internal_mech = mech_wanted;

                // get the token for mechanism
                byte[] accept_token;

                if (mechList[0].equals(mech_wanted) ||
                        (GSSUtil.isKerberosMech(mechList[0]) &&
                         GSSUtil.isKerberosMech(mech_wanted))) {
                    // get the mechanism token
                    if (DEBUG && !mech_wanted.equals(mechList[0])) {
                        System.out.println("SpNegoContext.acceptSecContext: " +
                                "negotiated mech adjusted to " + mechList[0]);
                    }
                    byte[] mechToken = initToken.getMechToken();
                    if (mechToken == null) {
                        throw new GSSException(GSSException.FAILURE, -1,
                                "mechToken is missing");
                    }
                    accept_token = GSS_acceptSecContext(mechToken);
                    mech_wanted = mechList[0];
                } else {
                    accept_token = null;
                }

                // verify MIC
                if (!GSSUtil.useMSInterop() && valid) {
                    valid = verifyMechListMIC(DER_mechTypes,
                                                initToken.getMechListMIC());
                }

                // determine negotiated result status
                if (valid) {
                    if (isMechContextEstablished()) {
                        negoResult = SpNegoToken.NegoResult.ACCEPT_COMPLETE;
                        state = STATE_DONE;
                        // now set the context flags for acceptor
                        setContextFlags();
                        // print the negotiated mech info
                        if (DEBUG) {
                            System.out.println("SPNEGO Negotiated Mechanism = "
                                + internal_mech + " " +
                                GSSUtil.getMechStr(internal_mech));
                        }
                    } else {
                        negoResult = SpNegoToken.NegoResult.ACCEPT_INCOMPLETE;
                        state = STATE_IN_PROCESS;
                    }
                } else {
                    negoResult = SpNegoToken.NegoResult.REJECT;
                    state = STATE_DELETED;
                    throw new GSSException(GSSException.FAILURE);
                }

                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                                "mechanism wanted = " + mech_wanted);
                    System.out.println("SpNegoContext.acceptSecContext: " +
                                "negotiated result = " + negoResult);
                }

                // generate SPNEGO token
                NegTokenTarg targToken = new NegTokenTarg(negoResult.ordinal(),
                                mech_wanted, accept_token, null);
                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                                "sending token of type = " +
                                SpNegoToken.getTokenName(targToken.getType()));
                }
                // get the encoded token
                retVal = targToken.getEncoded();

            } else if (state == STATE_IN_PROCESS) {
                // read data
                byte[] token = new byte[is.available()];
                SpNegoToken.readFully(is, token);
                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                            "receiving token = " +
                            SpNegoToken.getHexBytes(token));
                }

                // read the SPNEGO token
                // token will be validated when parsing
                NegTokenTarg inputToken = new NegTokenTarg(token);

                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                            "received token of type = " +
                            SpNegoToken.getTokenName(inputToken.getType()));
                }

                // read the token
                byte[] client_token = inputToken.getResponseToken();
                byte[] accept_token = GSS_acceptSecContext(client_token);
                if (accept_token == null) {
                    valid = false;
                }

                // determine negotiated result status
                if (valid) {
                    if (isMechContextEstablished()) {
                        negoResult = SpNegoToken.NegoResult.ACCEPT_COMPLETE;
                        state = STATE_DONE;
                    } else {
                        negoResult = SpNegoToken.NegoResult.ACCEPT_INCOMPLETE;
                        state = STATE_IN_PROCESS;
                    }
                } else {
                    negoResult = SpNegoToken.NegoResult.REJECT;
                    state = STATE_DELETED;
                    throw new GSSException(GSSException.FAILURE);
                }

                // generate SPNEGO token
                NegTokenTarg targToken = new NegTokenTarg(negoResult.ordinal(),
                                null, accept_token, null);
                if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                                "sending token of type = " +
                                SpNegoToken.getTokenName(targToken.getType()));
                }
                // get the encoded token
                retVal = targToken.getEncoded();

            } else {
                // XXX Use logging API
                if (DEBUG) {
                    System.out.println("AcceptSecContext: state = " + state);
                }
            }
            if (DEBUG) {
                    System.out.println("SpNegoContext.acceptSecContext: " +
                        "sending token = " + SpNegoToken.getHexBytes(retVal));
            }
        } catch (IOException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1, e.getMessage());
            gssException.initCause(e);
            throw gssException;
        }

        if (state == STATE_DONE) {
            // now set the context flags for acceptor
            setContextFlags();
        }
        return retVal;
    }

    /**
     * obtain the available mechanisms
     */
    private Oid[] getAvailableMechs() {
        if (myCred != null) {
            Oid[] mechs = new Oid[1];
            mechs[0] = myCred.getInternalMech();
            return mechs;
        } else {
            return factory.availableMechs;
        }
    }

    /**
     * get ther DER encoded MechList
     */
    private byte[] getEncodedMechs(Oid[] mechSet)
        throws IOException, GSSException {

        DerOutputStream mech = new DerOutputStream();
        for (int i = 0; i < mechSet.length; i++) {
            byte[] mechType = mechSet[i].getDER();
            mech.write(mechType);
        }
        // insert in SEQUENCE
        DerOutputStream mechTypeList = new DerOutputStream();
        mechTypeList.write(DerValue.tag_Sequence, mech);
        byte[] encoded = mechTypeList.toByteArray();
        return encoded;
    }

    /**
     * get the context flags
     */
    private BitArray getContextFlags() {
        BitArray out = new BitArray(7);

        if (getCredDelegState()) out.set(0, true);
        if (getMutualAuthState()) out.set(1, true);
        if (getReplayDetState()) out.set(2, true);
        if (getSequenceDetState()) out.set(3, true);
        if (getConfState()) out.set(5, true);
        if (getIntegState()) out.set(6, true);

        return out;
    }

    // Only called on acceptor side. On the initiator side, most flags
    // are already set at request. For those that might get chanegd,
    // state from mech below is used.
    private void setContextFlags() {

        if (mechContext != null) {
            // default for cred delegation is false
            if (mechContext.getCredDelegState()) {
                credDelegState = true;
            }
            // default for the following are true
            if (!mechContext.getMutualAuthState()) {
                mutualAuthState = false;
            }
            if (!mechContext.getReplayDetState()) {
                replayDetState = false;
            }
            if (!mechContext.getSequenceDetState()) {
                sequenceDetState = false;
            }
            if (!mechContext.getIntegState()) {
                integState = false;
            }
            if (!mechContext.getConfState()) {
                confState = false;
            }
        }
    }

    /**
     * generate MIC on mechList. Not used at the moment.
     */
    /*private byte[] generateMechListMIC(byte[] mechTypes)
        throws GSSException {

        // sanity check the required input
        if (mechTypes == null) {
            if (DEBUG) {
                System.out.println("SpNegoContext: no MIC token included");
            }
            return null;
        }

        // check if mechanism supports integrity
        if (!mechContext.getIntegState()) {
            if (DEBUG) {
                System.out.println("SpNegoContext: no MIC token included" +
                        " - mechanism does not support integrity");
            }
            return null;
        }

        // compute MIC on DER encoded mechanism list
        byte[] mic = null;
        try {
            MessageProp prop = new MessageProp(0, true);
            mic = getMIC(mechTypes, 0, mechTypes.length, prop);
            if (DEBUG) {
                System.out.println("SpNegoContext: getMIC = " +
                                        SpNegoToken.getHexBytes(mic));
            }
        } catch (GSSException e) {
            mic = null;
            if (DEBUG) {
                System.out.println("SpNegoContext: no MIC token included" +
                        " - getMIC failed : " + e.getMessage());
            }
        }
        return mic;
    }*/

    /**
     * verify MIC on MechList
     */
    private boolean verifyMechListMIC(byte[] mechTypes, byte[] token)
        throws GSSException {

        // sanity check the input
        if (token == null) {
            if (DEBUG) {
                System.out.println("SpNegoContext: no MIC token validation");
            }
            return true;
        }

        // check if mechanism supports integrity
        if (!mechContext.getIntegState()) {
            if (DEBUG) {
                System.out.println("SpNegoContext: no MIC token validation" +
                        " - mechanism does not support integrity");
            }
            return true;
        }

        // now verify the token
        boolean valid = false;
        try {
            MessageProp prop = new MessageProp(0, true);
            verifyMIC(token, 0, token.length, mechTypes,
                        0, mechTypes.length, prop);
            valid = true;
        } catch (GSSException e) {
            valid = false;
            if (DEBUG) {
                System.out.println("SpNegoContext: MIC validation failed! " +
                                        e.getMessage());
            }
        }
        return valid;
    }

    /**
     * call gss_init_sec_context for the corresponding underlying mechanism
     */
    private byte[] GSS_initSecContext(byte[] token) throws GSSException {
        byte[] tok = null;

        if (mechContext == null) {
            // initialize mech context
            GSSName serverName =
                factory.manager.createName(peerName.toString(),
                    peerName.getStringNameType(), internal_mech);
            GSSCredential cred = null;
            if (myCred != null) {
                // create context with provided credential
                cred = new GSSCredentialImpl(factory.manager,
                    myCred.getInternalCred());
            }
            mechContext =
                    factory.manager.createContext(serverName,
                    internal_mech, cred, GSSContext.DEFAULT_LIFETIME);
            mechContext.requestConf(confState);
            mechContext.requestInteg(integState);
            mechContext.requestCredDeleg(credDelegState);
            mechContext.requestMutualAuth(mutualAuthState);
            mechContext.requestReplayDet(replayDetState);
            mechContext.requestSequenceDet(sequenceDetState);
            if (mechContext instanceof GSSContextImpl) {
                ((GSSContextImpl)mechContext).requestDelegPolicy(
                        delegPolicyState);
            }
        }

        // pass token
        if (token != null) {
            tok = token;
        } else {
            tok = new byte[0];
        }

        // pass token to mechanism initSecContext
        byte[] init_token = mechContext.initSecContext(tok, 0, tok.length);

        return init_token;
    }

    /**
     * call gss_accept_sec_context for the corresponding underlying mechanism
     */
    private byte[] GSS_acceptSecContext(byte[] token) throws GSSException {

        if (mechContext == null) {
            // initialize mech context
            GSSCredential cred = null;
            if (myCred != null) {
                // create context with provided credential
                cred = new GSSCredentialImpl(factory.manager,
                myCred.getInternalCred());
            }
            mechContext = factory.manager.createContext(cred);
        }

        // pass token to mechanism acceptSecContext
        byte[] accept_token =
                mechContext.acceptSecContext(token, 0, token.length);

        return accept_token;
    }

    /**
     * This routine compares the recieved mechset to the mechset that
     * this server can support. It looks sequentially through the mechset
     * and the first one that matches what the server can support is
     * chosen as the negotiated mechanism. If one is found, negResult
     * is set to ACCEPT_COMPLETE, otherwise we return NULL and negResult
     * is set to REJECT.
     */
    private static Oid negotiate_mech_type(Oid[] supported_mechSet,
                                        Oid[] mechSet) {
        for (int i = 0; i < supported_mechSet.length; i++) {
            for (int j = 0; j < mechSet.length; j++) {
                if (mechSet[j].equals(supported_mechSet[i])) {
                    if (DEBUG) {
                        System.out.println("SpNegoContext: " +
                                "negotiated mechanism = " + mechSet[j]);
                    }
                    return (mechSet[j]);
                }
            }
        }
        return null;
    }

    public final boolean isEstablished() {
        return (state == STATE_DONE);
    }

    public final boolean isMechContextEstablished() {
        if (mechContext != null) {
            return mechContext.isEstablished();
        } else {
            if (DEBUG) {
                System.out.println("The underlying mechanism context has " +
                                        "not been initialized");
            }
            return false;
        }
    }

    public final byte [] export() throws GSSException {
        throw new GSSException(GSSException.UNAVAILABLE, -1,
                               "GSS Export Context not available");
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

    /**
     * Requests the desired lifetime. Can only be used on the context
     * initiator's side.
     */
    public void requestLifetime(int lifetime) throws GSSException {
        if (state == STATE_NEW && isInitiator())
            this.lifetime = lifetime;
    }

    /**
     * The lifetime remaining for this context.
     */
    public final int getLifetime() {
        if (mechContext != null) {
            return mechContext.getLifetime();
        } else {
            return GSSContext.INDEFINITE_LIFETIME;
        }
    }

    public final boolean isTransferable() throws GSSException {
        return false;
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

    public final GSSNameSpi getTargName() throws GSSException {
        // fill-in the GSSName
        // get the peer name for the mechanism
        if (mechContext != null) {
            GSSNameImpl targName = (GSSNameImpl)mechContext.getTargName();
            peerName = targName.getElement(internal_mech);
            return peerName;
        } else {
            if (DEBUG) {
                System.out.println("The underlying mechanism context has " +
                                        "not been initialized");
            }
            return null;
        }
    }

    public final GSSNameSpi getSrcName() throws GSSException {
        // fill-in the GSSName
        // get the src name for the mechanism
        if (mechContext != null) {
            GSSNameImpl srcName = (GSSNameImpl)mechContext.getSrcName();
            myName = srcName.getElement(internal_mech);
            return myName;
        } else {
            if (DEBUG) {
                System.out.println("The underlying mechanism context has " +
                                        "not been initialized");
            }
            return null;
        }
    }

    /**
     * Returns the delegated credential for the context. This
     * is an optional feature of contexts which not all
     * mechanisms will support. A context can be requested to
     * support credential delegation by using the <b>CRED_DELEG</b>.
     * This is only valid on the acceptor side of the context.
     * @return GSSCredentialSpi object for the delegated credential
     * @exception GSSException
     * @see GSSContext#getCredDelegState
     */
    public final GSSCredentialSpi getDelegCred() throws GSSException {
        if (state != STATE_IN_PROCESS && state != STATE_DONE)
            throw new GSSException(GSSException.NO_CONTEXT);
        if (mechContext != null) {
            GSSCredentialImpl delegCred =
                        (GSSCredentialImpl)mechContext.getDelegCred();
            if (delegCred == null) {
                return null;
            }
            // determine delegated cred element usage
            boolean initiate = false;
            if (delegCred.getUsage() == GSSCredential.INITIATE_ONLY) {
                initiate = true;
            }
            GSSCredentialSpi mechCred =
                    delegCred.getElement(internal_mech, initiate);
            SpNegoCredElement cred = new SpNegoCredElement(mechCred);
            return cred.getInternalCred();
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "getDelegCred called in invalid state!");
        }
    }

    public final int getWrapSizeLimit(int qop, boolean confReq,
                                       int maxTokSize) throws GSSException {
        if (mechContext != null) {
            return mechContext.getWrapSizeLimit(qop, confReq, maxTokSize);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "getWrapSizeLimit called in invalid state!");
        }
    }

    public final byte[] wrap(byte inBuf[], int offset, int len,
                             MessageProp msgProp) throws GSSException {
        if (mechContext != null) {
            return mechContext.wrap(inBuf, offset, len, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "Wrap called in invalid state!");
        }
    }

    @Deprecated(since="11")
    public final void wrap(InputStream is, OutputStream os,
                            MessageProp msgProp) throws GSSException {
        if (mechContext != null) {
            mechContext.wrap(is, os, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "Wrap called in invalid state!");
        }
    }

    public final byte[] unwrap(byte inBuf[], int offset, int len,
                               MessageProp msgProp)
        throws GSSException {
        if (mechContext != null) {
            return mechContext.unwrap(inBuf, offset, len, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "UnWrap called in invalid state!");
        }
    }

    @Deprecated(since="11")
    public final void unwrap(InputStream is, OutputStream os,
                             MessageProp msgProp) throws GSSException {
        if (mechContext != null) {
            mechContext.unwrap(is, os, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "UnWrap called in invalid state!");
        }
    }

    public final byte[] getMIC(byte []inMsg, int offset, int len,
                               MessageProp msgProp)
        throws GSSException {
        if (mechContext != null) {
            return mechContext.getMIC(inMsg, offset, len, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "getMIC called in invalid state!");
        }
    }

    @Deprecated(since="11")
    public final void getMIC(InputStream is, OutputStream os,
                              MessageProp msgProp) throws GSSException {
        if (mechContext != null) {
            mechContext.getMIC(is, os, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "getMIC called in invalid state!");
        }
    }

    public final void verifyMIC(byte []inTok, int tokOffset, int tokLen,
                                byte[] inMsg, int msgOffset, int msgLen,
                                MessageProp msgProp)
        throws GSSException {
        if (mechContext != null) {
            mechContext.verifyMIC(inTok, tokOffset, tokLen, inMsg, msgOffset,
                                msgLen,  msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "verifyMIC called in invalid state!");
        }
    }

    @Deprecated(since="11")
    public final void verifyMIC(InputStream is, InputStream msgStr,
                                 MessageProp msgProp) throws GSSException {
        if (mechContext != null) {
            mechContext.verifyMIC(is, msgStr, msgProp);
        } else {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                                "verifyMIC called in invalid state!");
        }
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

    /**
     * Retrieve attribute of the context for {@code type}.
     */
    public Object inquireSecContext(String type)
            throws GSSException {
        if (mechContext == null) {
            throw new GSSException(GSSException.NO_CONTEXT, -1,
                    "Underlying mech not established.");
        }
        if (mechContext instanceof GSSContextImpl) {
            return ((GSSContextImpl)mechContext).inquireSecContext(type);
        } else {
            throw new GSSException(GSSException.BAD_MECH, -1,
                    "inquireSecContext not supported by underlying mech.");
        }
    }
}

