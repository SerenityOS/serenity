/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss;

import org.ietf.jgss.*;
import sun.security.jgss.spi.*;
import sun.security.util.ObjectIdentifier;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * This class represents the JGSS security context and its associated
 * operations.  JGSS security contexts are established between
 * peers using locally established credentials.  Multiple contexts
 * may exist simultaneously between a pair of peers, using the same
 * or different set of credentials.  The JGSS is independent of
 * the underlying transport protocols and depends on its callers to
 * transport the tokens between peers.
 * <p>
 * The context object can be thought of as having 3 implicit states:
 * before it is established, during its context establishment, and
 * after a fully established context exists.
 * <p>
 * Before the context establishment phase is initiated, the context
 * initiator may request specific characteristics desired of the
 * established context. These can be set using the set methods. After the
 * context is established, the caller can check the actual characteristic
 * and services offered by the context using the query methods.
 * <p>
 * The context establishment phase begins with the first call to the
 * initSecContext method by the context initiator. During this phase the
 * initSecContext and acceptSecContext methods will produce GSS-API
 * authentication tokens which the calling application needs to send to its
 * peer. The initSecContext and acceptSecContext methods may
 * return a CONTINUE_NEEDED code which indicates that a token is needed
 * from its peer in order to continue the context establishment phase. A
 * return code of COMPLETE signals that the local end of the context is
 * established. This may still require that a token be sent to the peer,
 * depending if one is produced by GSS-API. The isEstablished method can
 * also be used to determine if the local end of the context has been
 * fully established. During the context establishment phase, the
 * isProtReady method may be called to determine if the context can be
 * used for the per-message operations. This allows implementation to
 * use per-message operations on contexts which aren't fully established.
 * <p>
 * After the context has been established or the isProtReady method
 * returns "true", the query routines can be invoked to determine the actual
 * characteristics and services of the established context. The
 * application can also start using the per-message methods of wrap and
 * getMIC to obtain cryptographic operations on application supplied data.
 * <p>
 * When the context is no longer needed, the application should call
 * dispose to release any system resources the context may be using.
 * <DL><DT><B>RFC 2078</b>
 *    <DD>This class corresponds to the context level calls together with
 * the per message calls of RFC 2078. The gss_init_sec_context and
 * gss_accept_sec_context calls have been made simpler by only taking
 * required parameters.  The context can have its properties set before
 * the first call to initSecContext. The supplementary status codes for the
 * per-message operations are returned in an instance of the MessageProp
 * class, which is used as an argument in these calls.</dl>
 */
public class GSSContextImpl implements GSSContext {

    private GSSManagerImpl gssManager;
    private boolean initiator;

    // private flags for the context state
    private static final int PRE_INIT = 1;
    private static final int IN_PROGRESS = 2;
    private static final int READY = 3;
    private static final int DELETED = 4;

    // instance variables
    private int currentState = PRE_INIT;

    private GSSContextSpi mechCtxt = null;
    private Oid mechOid = null;
    private ObjectIdentifier objId = null;

    private GSSCredentialImpl myCred = null;

    private GSSNameImpl srcName = null;
    private GSSNameImpl targName = null;

    private int reqLifetime = INDEFINITE_LIFETIME;
    private ChannelBinding channelBindings = null;

    private boolean reqConfState = true;
    private boolean reqIntegState = true;
    private boolean reqMutualAuthState = true;
    private boolean reqReplayDetState = true;
    private boolean reqSequenceDetState = true;
    private boolean reqCredDelegState = false;
    private boolean reqAnonState = false;
    private boolean reqDelegPolicyState = false;

    public GSSContextImpl() {
        // Useless
    }

    // Used by new ExtendedGSSContext.ExtendedGSSContextImpl(ctxt)
    protected GSSContextImpl(GSSContextImpl src) {
        for (Field f: GSSContextImpl.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                try {
                    f.set(this, f.get(src));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    /**
     * Creates a GSSContextImp on the context initiator's side.
     */
    public GSSContextImpl(GSSManagerImpl gssManager, GSSName peer, Oid mech,
                          GSSCredential myCred, int lifetime)
        throws GSSException {
        if ((peer == null) || !(peer instanceof GSSNameImpl)) {
            throw new GSSException(GSSException.BAD_NAME);
        }
        if (mech == null) mech = ProviderList.DEFAULT_MECH_OID;

        this.gssManager = gssManager;
        this.myCred = (GSSCredentialImpl) myCred;  // XXX Check first
        reqLifetime = lifetime;
        targName = (GSSNameImpl)peer;
        this.mechOid = mech;
        initiator = true;
    }

    /**
     * Creates a GSSContextImpl on the context acceptor's side.
     */
    public GSSContextImpl(GSSManagerImpl gssManager, GSSCredential myCred)
        throws GSSException {
        this.gssManager = gssManager;
        this.myCred = (GSSCredentialImpl) myCred; // XXX Check first
        initiator = false;
    }

    /**
     * Creates a GSSContextImpl out of a previously exported
     * GSSContext.
     *
     * @see #isTransferable
     */
    public GSSContextImpl(GSSManagerImpl gssManager, byte[] interProcessToken)
        throws GSSException {
        this.gssManager = gssManager;
        mechCtxt = gssManager.getMechanismContext(interProcessToken);
        initiator = mechCtxt.isInitiator();
        this.mechOid = mechCtxt.getMech();
    }

    public byte[] initSecContext(byte inputBuf[], int offset, int len)
        throws GSSException {
        /*
         * Size of ByteArrayOutputStream will double each time that extra
         * bytes are to be written. Usually, without delegation, a GSS
         * initial token containing the Kerberos AP-REQ is between 400 and
         * 600 bytes.
         */
        ByteArrayOutputStream bos = new ByteArrayOutputStream(600);
        ByteArrayInputStream bin =
            new ByteArrayInputStream(inputBuf, offset, len);
        int size = initSecContext(bin, bos);
        return (size == 0? null : bos.toByteArray());
    }

    @Deprecated(since="11")
    public int initSecContext(InputStream inStream,
                              OutputStream outStream) throws GSSException {

        if (mechCtxt != null && currentState != IN_PROGRESS) {
            throw new GSSExceptionImpl(GSSException.FAILURE,
                                   "Illegal call to initSecContext");
        }

        GSSHeader gssHeader = null;
        int inTokenLen = -1;
        GSSCredentialSpi credElement = null;
        boolean firstToken = false;

        try {
            if (mechCtxt == null) {
                if (myCred != null) {
                    try {
                        credElement = myCred.getElement(mechOid, true);
                    } catch (GSSException ge) {
                        if (GSSUtil.isSpNegoMech(mechOid) &&
                            ge.getMajor() == GSSException.NO_CRED) {
                            credElement = myCred.getElement
                                (myCred.getMechs()[0], true);
                        } else {
                            throw ge;
                        }
                    }
                }
                GSSNameSpi nameElement = targName.getElement(mechOid);
                mechCtxt = gssManager.getMechanismContext(nameElement,
                                                          credElement,
                                                          reqLifetime,
                                                          mechOid);
                mechCtxt.requestConf(reqConfState);
                mechCtxt.requestInteg(reqIntegState);
                mechCtxt.requestCredDeleg(reqCredDelegState);
                mechCtxt.requestMutualAuth(reqMutualAuthState);
                mechCtxt.requestReplayDet(reqReplayDetState);
                mechCtxt.requestSequenceDet(reqSequenceDetState);
                mechCtxt.requestAnonymity(reqAnonState);
                mechCtxt.setChannelBinding(channelBindings);
                mechCtxt.requestDelegPolicy(reqDelegPolicyState);

                objId = ObjectIdentifier.of(mechOid.toString());

                currentState = IN_PROGRESS;
                firstToken = true;
            } else {
                if (mechCtxt.getProvider().getName().equals("SunNativeGSS") ||
                    GSSUtil.isSpNegoMech(mechOid)) {
                    // do not parse GSS header for native provider or SPNEGO
                    // mech
                } else {
                    // parse GSS header
                    gssHeader = new GSSHeader(inStream);
                    if (!gssHeader.getOid().equals(objId))
                        throw new GSSExceptionImpl
                            (GSSException.DEFECTIVE_TOKEN,
                             "Mechanism not equal to " +
                             mechOid.toString() +
                             " in initSecContext token");
                    inTokenLen = gssHeader.getMechTokenLength();
                }
            }

            byte[] obuf = mechCtxt.initSecContext(inStream, inTokenLen);

            int retVal = 0;

            if (obuf != null) {
                retVal = obuf.length;
                if (mechCtxt.getProvider().getName().equals("SunNativeGSS") ||
                    (!firstToken && GSSUtil.isSpNegoMech(mechOid))) {
                    // do not add GSS header for native provider or SPNEGO
                    // except for the first SPNEGO token
                } else {
                    // add GSS header
                    gssHeader = new GSSHeader(objId, obuf.length);
                    retVal += gssHeader.encode(outStream);
                }
                outStream.write(obuf);
            }

            if (mechCtxt.isEstablished())
                currentState = READY;

            return retVal;

        } catch (IOException e) {
            throw new GSSExceptionImpl(GSSException.DEFECTIVE_TOKEN,
                                   e.getMessage());
        }
    }

    public byte[] acceptSecContext(byte inTok[], int offset, int len)
        throws GSSException {

        /*
         * Usually initial GSS token containing a Kerberos AP-REP is less
         * than 100 bytes.
         */
        ByteArrayOutputStream bos = new ByteArrayOutputStream(100);
        acceptSecContext(new ByteArrayInputStream(inTok, offset, len),
                         bos);
        byte[] out = bos.toByteArray();
        return (out.length == 0) ? null : out;
    }

    @Deprecated(since="11")
    public void acceptSecContext(InputStream inStream,
                                 OutputStream outStream) throws GSSException {

        if (mechCtxt != null && currentState != IN_PROGRESS) {
            throw new GSSExceptionImpl(GSSException.FAILURE,
                                       "Illegal call to acceptSecContext");
        }

        GSSHeader gssHeader = null;
        int inTokenLen = -1;
        GSSCredentialSpi credElement = null;

        try {
            if (mechCtxt == null) {
                // mechOid will be null for an acceptor's context
                gssHeader = new GSSHeader(inStream);
                inTokenLen = gssHeader.getMechTokenLength();

                /*
                 * Convert ObjectIdentifier to Oid
                 */
                objId = gssHeader.getOid();
                mechOid = new Oid(objId.toString());
                // System.out.println("Entered GSSContextImpl.acceptSecContext"
                //                      + " with mechanism = " + mechOid);
                if (myCred != null) {
                    credElement = myCred.getElement(mechOid, false);
                }

                mechCtxt = gssManager.getMechanismContext(credElement,
                                                          mechOid);
                mechCtxt.setChannelBinding(channelBindings);

                currentState = IN_PROGRESS;
            } else {
                if (mechCtxt.getProvider().getName().equals("SunNativeGSS") ||
                    (GSSUtil.isSpNegoMech(mechOid))) {
                    // do not parse GSS header for native provider and SPNEGO
                } else {
                    // parse GSS Header
                    gssHeader = new GSSHeader(inStream);
                    if (!gssHeader.getOid().equals(objId))
                        throw new GSSExceptionImpl
                            (GSSException.DEFECTIVE_TOKEN,
                             "Mechanism not equal to " +
                             mechOid.toString() +
                             " in acceptSecContext token");
                    inTokenLen = gssHeader.getMechTokenLength();
                }
            }

            byte[] obuf = mechCtxt.acceptSecContext(inStream, inTokenLen);

            if (obuf != null) {
                int retVal = obuf.length;
                if (mechCtxt.getProvider().getName().equals("SunNativeGSS") ||
                    (GSSUtil.isSpNegoMech(mechOid))) {
                    // do not add GSS header for native provider and SPNEGO
                } else {
                    // add GSS header
                    gssHeader = new GSSHeader(objId, obuf.length);
                    retVal += gssHeader.encode(outStream);
                }
                outStream.write(obuf);
            }

            if (mechCtxt.isEstablished()) {
                currentState = READY;
            }
        } catch (IOException e) {
            throw new GSSExceptionImpl(GSSException.DEFECTIVE_TOKEN,
                                   e.getMessage());
        }
    }

    public boolean isEstablished() {
        if (mechCtxt == null)
            return false;
        else
            return (currentState == READY);
    }

    public int getWrapSizeLimit(int qop, boolean confReq,
                                int maxTokenSize) throws GSSException {
        if (mechCtxt != null)
            return mechCtxt.getWrapSizeLimit(qop, confReq, maxTokenSize);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    public byte[] wrap(byte inBuf[], int offset, int len,
                       MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            return mechCtxt.wrap(inBuf, offset, len, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                   "No mechanism context yet!");
    }

    @Deprecated(since="11")
    public void wrap(InputStream inStream, OutputStream outStream,
                     MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            mechCtxt.wrap(inStream, outStream, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    public byte [] unwrap(byte[] inBuf, int offset, int len,
                          MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            return mechCtxt.unwrap(inBuf, offset, len, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    @Deprecated(since="11")
    public void unwrap(InputStream inStream, OutputStream outStream,
                       MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            mechCtxt.unwrap(inStream, outStream, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    public byte[] getMIC(byte []inMsg, int offset, int len,
                         MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            return mechCtxt.getMIC(inMsg, offset, len, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    @Deprecated(since="11")
    public void getMIC(InputStream inStream, OutputStream outStream,
                       MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            mechCtxt.getMIC(inStream, outStream, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    public void verifyMIC(byte[] inTok, int tokOffset, int tokLen,
                          byte[] inMsg, int msgOffset, int msgLen,
                          MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            mechCtxt.verifyMIC(inTok, tokOffset, tokLen,
                               inMsg, msgOffset, msgLen, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    @Deprecated(since="11")
    public void verifyMIC(InputStream tokStream, InputStream msgStream,
                          MessageProp msgProp) throws GSSException {
        if (mechCtxt != null)
            mechCtxt.verifyMIC(tokStream, msgStream, msgProp);
        else
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                  "No mechanism context yet!");
    }

    public byte[] export() throws GSSException {
        // Defaults to null to match old behavior
        byte[] result = null;
        // Only allow context export from native provider since JGSS
        // still has not defined its own interprocess token format
        if (mechCtxt.isTransferable() &&
            mechCtxt.getProvider().getName().equals("SunNativeGSS")) {
            result = mechCtxt.export();
        }
        return result;
    }

    public void requestMutualAuth(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqMutualAuthState = state;
    }

    public void requestReplayDet(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqReplayDetState = state;
    }

    public void requestSequenceDet(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqSequenceDetState = state;
    }

    public void requestCredDeleg(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqCredDelegState = state;
    }

    public void requestAnonymity(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqAnonState = state;
    }

    public void requestConf(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqConfState = state;
    }

    public void requestInteg(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqIntegState = state;
    }

    public void requestLifetime(int lifetime) throws GSSException {
        if (mechCtxt == null && initiator)
            reqLifetime = lifetime;
    }

    public void setChannelBinding(ChannelBinding channelBindings)
        throws GSSException {

        if (mechCtxt == null)
            this.channelBindings = channelBindings;

    }

    public boolean getCredDelegState() {
        if (mechCtxt != null)
            return mechCtxt.getCredDelegState();
        else
            return reqCredDelegState;
    }

    public boolean getMutualAuthState() {
        if (mechCtxt != null)
            return mechCtxt.getMutualAuthState();
        else
            return reqMutualAuthState;
    }

    public boolean getReplayDetState() {
        if (mechCtxt != null)
            return mechCtxt.getReplayDetState();
        else
            return reqReplayDetState;
    }

    public boolean getSequenceDetState() {
        if (mechCtxt != null)
            return mechCtxt.getSequenceDetState();
        else
            return reqSequenceDetState;
    }

    public boolean getAnonymityState() {
        if (mechCtxt != null)
            return mechCtxt.getAnonymityState();
        else
            return reqAnonState;
    }

    public boolean isTransferable() throws GSSException {
        if (mechCtxt != null)
            return mechCtxt.isTransferable();
        else
            return false;
    }

    public boolean isProtReady() {
        if (mechCtxt != null)
            return mechCtxt.isProtReady();
        else
            return false;
    }

    public boolean getConfState() {
        if (mechCtxt != null)
            return mechCtxt.getConfState();
        else
            return reqConfState;
    }

    public boolean getIntegState() {
        if (mechCtxt != null)
            return mechCtxt.getIntegState();
        else
            return reqIntegState;
    }

    public int getLifetime() {
        if (mechCtxt != null)
            return mechCtxt.getLifetime();
        else
            return reqLifetime;
    }

    public GSSName getSrcName() throws GSSException {
        if (srcName == null) {
            srcName = GSSNameImpl.wrapElement
                (gssManager, mechCtxt.getSrcName());
        }
        return srcName;
    }

    public GSSName getTargName() throws GSSException {
        if (targName == null) {
            targName = GSSNameImpl.wrapElement
                (gssManager, mechCtxt.getTargName());
        }
        return targName;
    }

    public Oid getMech() throws GSSException {
        if (mechCtxt != null) {
            return mechCtxt.getMech();
        }
        return mechOid;
    }

    public GSSCredential getDelegCred() throws GSSException {

        if (mechCtxt == null)
            throw new GSSExceptionImpl(GSSException.NO_CONTEXT,
                                   "No mechanism context yet!");
        GSSCredentialSpi delCredElement = mechCtxt.getDelegCred();
        return (delCredElement == null ?
            null : GSSManagerImpl.wrap(new GSSCredentialImpl(gssManager, delCredElement)));
    }

    public boolean isInitiator() throws GSSException {
        return initiator;
    }

    public void dispose() throws GSSException {
        currentState = DELETED;
        if (mechCtxt != null) {
            mechCtxt.dispose();
            mechCtxt = null;
        }
        myCred = null;
        srcName = null;
        targName = null;
    }

    // ExtendedGSSContext methods:

    public Object inquireSecContext(String type) throws GSSException {
        if (mechCtxt == null) {
            throw new GSSException(GSSException.NO_CONTEXT);
        }
        return mechCtxt.inquireSecContext(type);
    }

    public void requestDelegPolicy(boolean state) throws GSSException {
        if (mechCtxt == null && initiator)
            reqDelegPolicyState = state;
    }

    public boolean getDelegPolicyState() {
        if (mechCtxt != null)
            return mechCtxt.getDelegPolicyState();
        else
            return reqDelegPolicyState;
    }
}
