/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.wrapper;

import org.ietf.jgss.*;
import java.security.Provider;
import sun.security.jgss.GSSHeader;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSExceptionImpl;
import sun.security.jgss.spi.*;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.jgss.spnego.NegTokenInit;
import sun.security.jgss.spnego.NegTokenTarg;
import javax.security.auth.kerberos.DelegationPermission;
import java.io.*;


/**
 * This class is essentially a wrapper class for the gss_ctx_id_t
 * structure of the native GSS library.
 * @author Valerie Peng
 * @since 1.6
 */
class NativeGSSContext implements GSSContextSpi {

    private static final int GSS_C_DELEG_FLAG = 1;
    private static final int GSS_C_MUTUAL_FLAG = 2;
    private static final int GSS_C_REPLAY_FLAG = 4;
    private static final int GSS_C_SEQUENCE_FLAG = 8;
    private static final int GSS_C_CONF_FLAG = 16;
    private static final int GSS_C_INTEG_FLAG = 32;
    private static final int GSS_C_ANON_FLAG = 64;
    private static final int GSS_C_PROT_READY_FLAG = 128;
    private static final int GSS_C_TRANS_FLAG = 256;

    private static final int NUM_OF_INQUIRE_VALUES = 6;

    // Warning: The following 9 fields are used by NativeUtil.c
    private long pContext = 0; // Pointer to the gss_ctx_id_t structure
    private GSSNameElement srcName;
    private GSSNameElement targetName;
    private boolean isInitiator;
    private boolean isEstablished;
    private GSSCredElement delegatedCred;
    private int flags;
    private int lifetime = GSSCredential.DEFAULT_LIFETIME;
    private Oid actualMech; // Assigned during context establishment

    private GSSCredElement cred;
    private GSSCredElement disposeCred;

    private ChannelBinding cb;
    private GSSCredElement disposeDelegatedCred;
    private final GSSLibStub cStub;

    private boolean skipDelegPermCheck;
    private boolean skipServicePermCheck;

    // Retrieve the (preferred) mech out of SPNEGO tokens, i.e.
    // NegTokenInit & NegTokenTarg
    private static Oid getMechFromSpNegoToken(byte[] token,
                                              boolean isInitiator)
        throws GSSException {
        Oid mech = null;
        if (isInitiator) {
            GSSHeader header = null;
            try {
                header = new GSSHeader(new ByteArrayInputStream(token));
            } catch (IOException ioe) {
                throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
            }
            int negTokenLen = header.getMechTokenLength();
            byte[] negToken = new byte[negTokenLen];
            System.arraycopy(token, token.length-negTokenLen,
                             negToken, 0, negToken.length);

            NegTokenInit ntok = new NegTokenInit(negToken);
            if (ntok.getMechToken() != null) {
                Oid[] mechList = ntok.getMechTypeList();
                mech = mechList[0];
            }
        } else {
            NegTokenTarg ntok = new NegTokenTarg(token);
            mech = ntok.getSupportedMech();
        }
        return mech;
    }

    // Perform the Service permission check
    @SuppressWarnings("removal")
    private void doServicePermCheck() throws GSSException {
        if (System.getSecurityManager() != null) {
            String action = (isInitiator? "initiate" : "accept");
            // Need to check Service permission for accessing
            // initiator cred for SPNEGO during context establishment
            if (GSSUtil.isSpNegoMech(cStub.getMech()) && isInitiator
                && !isEstablished) {
                if (srcName == null) {
                    // Check by creating default initiator KRB5 cred
                    GSSCredElement tempCred =
                        new GSSCredElement(null, lifetime,
                                           GSSCredential.INITIATE_ONLY,
                                           GSSLibStub.getInstance(GSSUtil.GSS_KRB5_MECH_OID));
                    tempCred.dispose();
                } else {
                    String tgsName = Krb5Util.getTGSName(srcName);
                    Krb5Util.checkServicePermission(tgsName, action);
                }
            }
            String targetStr = targetName.getKrbName();
            Krb5Util.checkServicePermission(targetStr, action);
            skipServicePermCheck = true;
        }
    }

    // Perform the Delegation permission check
    private void doDelegPermCheck() throws GSSException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            String targetStr = targetName.getKrbName();
            String tgsStr = Krb5Util.getTGSName(targetName);
            StringBuilder sb = new StringBuilder("\"");
            sb.append(targetStr).append("\" \"");
            sb.append(tgsStr).append('\"');
            String krbPrincPair = sb.toString();
            SunNativeProvider.debug("Checking DelegationPermission (" +
                                    krbPrincPair + ")");
            DelegationPermission perm =
                new DelegationPermission(krbPrincPair);
            sm.checkPermission(perm);
            skipDelegPermCheck = true;
        }
    }

    private byte[] retrieveToken(InputStream is, int mechTokenLen)
        throws GSSException {
        try {
            byte[] result = null;
            if (mechTokenLen != -1) {
                // Need to add back the GSS header for a complete GSS token
                SunNativeProvider.debug("Precomputed mechToken length: " +
                                         mechTokenLen);
                GSSHeader gssHeader = new GSSHeader
                    (ObjectIdentifier.of(cStub.getMech().toString()),
                     mechTokenLen);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(600);

                byte[] mechToken = new byte[mechTokenLen];
                int len = is.read(mechToken);
                assert(mechTokenLen == len);
                gssHeader.encode(baos);
                baos.write(mechToken);
                result = baos.toByteArray();
            } else {
                // Must be unparsed GSS token or SPNEGO's NegTokenTarg token
                assert(mechTokenLen == -1);
                DerValue dv = new DerValue(is);
                result = dv.toByteArray();
            }
            SunNativeProvider.debug("Complete Token length: " +
                                    result.length);
            return result;
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
    }

    // Constructor for context initiator
    NativeGSSContext(GSSNameElement peer, GSSCredElement myCred,
                     int time, GSSLibStub stub) throws GSSException {
        if (peer == null) {
            throw new GSSException(GSSException.FAILURE, 1, "null peer");
        }
        cStub = stub;
        cred = myCred;
        disposeCred = null;
        targetName = peer;
        isInitiator = true;
        lifetime = time;

        if (GSSUtil.isKerberosMech(cStub.getMech())) {
            doServicePermCheck();
            if (cred == null) {
                disposeCred = cred =
                    new GSSCredElement(null, lifetime,
                            GSSCredential.INITIATE_ONLY, cStub);
            }
            srcName = cred.getName();
        }
    }

    // Constructor for context acceptor
    NativeGSSContext(GSSCredElement myCred, GSSLibStub stub)
        throws GSSException {
        cStub = stub;
        cred = myCred;
        disposeCred = null;

        if (cred != null) targetName = cred.getName();

        isInitiator = false;
        // Defer Service permission check for default acceptor cred
        // to acceptSecContext()
        if (GSSUtil.isKerberosMech(cStub.getMech()) && targetName != null) {
            doServicePermCheck();
        }

        // srcName and potentially targetName (when myCred is null)
        // will be set in GSSLibStub.acceptContext(...)
    }

    // Constructor for imported context
    // Warning: called by NativeUtil.c
    NativeGSSContext(long pCtxt, GSSLibStub stub) throws GSSException {
        assert(pContext != 0);
        pContext = pCtxt;
        cStub = stub;

        // Set everything except cred, cb, delegatedCred
        long[] info = cStub.inquireContext(pContext);
        if (info.length != NUM_OF_INQUIRE_VALUES) {
            throw new RuntimeException("Bug w/ GSSLibStub.inquireContext()");
        }
        srcName = new GSSNameElement(info[0], cStub);
        targetName = new GSSNameElement(info[1], cStub);
        isInitiator = (info[2] != 0);
        isEstablished = (info[3] != 0);
        flags = (int) info[4];
        lifetime = (int) info[5];

        // Do Service Permission check when importing SPNEGO context
        // just to be safe
        Oid mech = cStub.getMech();
        if (GSSUtil.isSpNegoMech(mech) || GSSUtil.isKerberosMech(mech)) {
            doServicePermCheck();
        }
    }

    public Provider getProvider() {
        return SunNativeProvider.INSTANCE;
    }

    public byte[] initSecContext(InputStream is, int mechTokenLen)
        throws GSSException {
        byte[] outToken = null;
        if ((!isEstablished) && (isInitiator)) {
            byte[] inToken = null;
            // Ignore the specified input stream on the first call
            if (pContext != 0) {
                inToken = retrieveToken(is, mechTokenLen);
                SunNativeProvider.debug("initSecContext=> inToken len=" +
                    inToken.length);
            }

            if (!getCredDelegState()) skipDelegPermCheck = true;

            if (GSSUtil.isKerberosMech(cStub.getMech()) && !skipDelegPermCheck) {
                doDelegPermCheck();
            }

            long pCred = (cred == null? 0 : cred.pCred);
            outToken = cStub.initContext(pCred, targetName.pName,
                                         cb, inToken, this);
            SunNativeProvider.debug("initSecContext=> outToken len=" +
                (outToken == null ? 0 : outToken.length));

            // Only inspect the token when the permission check
            // has not been performed
            if (GSSUtil.isSpNegoMech(cStub.getMech()) && outToken != null) {
                // WORKAROUND for SEAM bug#6287358
                actualMech = getMechFromSpNegoToken(outToken, true);

                if (GSSUtil.isKerberosMech(actualMech)) {
                    if (!skipServicePermCheck) doServicePermCheck();
                    if (!skipDelegPermCheck) doDelegPermCheck();
                }
            }

            if (isEstablished) {
                if (srcName == null) {
                    srcName = new GSSNameElement
                        (cStub.getContextName(pContext, true), cStub);
                }
                if (cred == null) {
                    disposeCred = cred =
                        new GSSCredElement(srcName, lifetime,
                                GSSCredential.INITIATE_ONLY, cStub);
                }
            }
        }
        return outToken;
    }

    public byte[] acceptSecContext(InputStream is, int mechTokenLen)
        throws GSSException {
        byte[] outToken = null;
        if ((!isEstablished) && (!isInitiator)) {
            byte[] inToken = retrieveToken(is, mechTokenLen);
            SunNativeProvider.debug("acceptSecContext=> inToken len=" +
                                    inToken.length);
            long pCred = (cred == null? 0 : cred.pCred);
            outToken = cStub.acceptContext(pCred, cb, inToken, this);
            disposeDelegatedCred = delegatedCred;
            SunNativeProvider.debug("acceptSecContext=> outToken len=" +
                                    (outToken == null? 0 : outToken.length));

            if (targetName == null) {
                targetName = new GSSNameElement
                    (cStub.getContextName(pContext, false), cStub);
                // Replace the current default acceptor cred now that
                // the context acceptor name is available
                if (disposeCred != null) {
                    disposeCred.dispose();
                }
                disposeCred = cred =
                    new GSSCredElement(targetName, lifetime,
                            GSSCredential.ACCEPT_ONLY, cStub);
            }

            // Only inspect token when the permission check has not
            // been performed
            if (GSSUtil.isSpNegoMech(cStub.getMech()) &&
                (outToken != null) && !skipServicePermCheck) {
                if (GSSUtil.isKerberosMech(getMechFromSpNegoToken
                                           (outToken, false))) {
                    doServicePermCheck();
                }
            }
        }
        return outToken;
    }

    public boolean isEstablished() {
        return isEstablished;
    }

    public void dispose() throws GSSException {
        if (disposeCred != null) {
            disposeCred.dispose();
        }
        if (disposeDelegatedCred != null) {
            disposeDelegatedCred.dispose();
        }
        disposeDelegatedCred = disposeCred = cred = null;
        srcName = null;
        targetName = null;
        delegatedCred = null;
        if (pContext != 0) {
            pContext = cStub.deleteContext(pContext);
            pContext = 0;
        }
    }

    public int getWrapSizeLimit(int qop, boolean confReq,
                                int maxTokenSize)
        throws GSSException {
        return cStub.wrapSizeLimit(pContext, (confReq? 1:0), qop,
                                   maxTokenSize);
    }

    public byte[] wrap(byte[] inBuf, int offset, int len,
                       MessageProp msgProp) throws GSSException {
        byte[] data = inBuf;
        if ((offset != 0) || (len != inBuf.length)) {
            data = new byte[len];
            System.arraycopy(inBuf, offset, data, 0, len);
        }
        return cStub.wrap(pContext, data, msgProp);
    }
    public void wrap(byte[] inBuf, int offset, int len,
                     OutputStream os, MessageProp msgProp)
        throws GSSException {
        try {
        byte[] result = wrap(inBuf, offset, len, msgProp);
        os.write(result);
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
    }
    public int wrap(byte[] inBuf, int inOffset, int len, byte[] outBuf,
                    int outOffset, MessageProp msgProp)
        throws GSSException {
        byte[] result = wrap(inBuf, inOffset, len, msgProp);
        System.arraycopy(result, 0, outBuf, outOffset, result.length);
        return result.length;
    }
    public void wrap(InputStream inStream, OutputStream outStream,
                     MessageProp msgProp) throws GSSException {
        try {
            byte[] data = new byte[inStream.available()];
            int length = inStream.read(data);
            byte[] token = wrap(data, 0, length, msgProp);
            outStream.write(token);
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
    }

    public byte[] unwrap(byte[] inBuf, int offset, int len,
                         MessageProp msgProp)
        throws GSSException {
        if ((offset != 0) || (len != inBuf.length)) {
            byte[] temp = new byte[len];
            System.arraycopy(inBuf, offset, temp, 0, len);
            return cStub.unwrap(pContext, temp, msgProp);
        } else {
            return cStub.unwrap(pContext, inBuf, msgProp);
        }
    }
    public int unwrap(byte[] inBuf, int inOffset, int len,
                      byte[] outBuf, int outOffset,
                      MessageProp msgProp) throws GSSException {
        byte[] result = null;
        if ((inOffset != 0) || (len != inBuf.length)) {
            byte[] temp = new byte[len];
            System.arraycopy(inBuf, inOffset, temp, 0, len);
            result = cStub.unwrap(pContext, temp, msgProp);
        } else {
            result = cStub.unwrap(pContext, inBuf, msgProp);
        }
        System.arraycopy(result, 0, outBuf, outOffset, result.length);
        return result.length;
    }
    public void unwrap(InputStream inStream, OutputStream outStream,
                       MessageProp msgProp) throws GSSException {
        try {
            byte[] wrapped = new byte[inStream.available()];
            int wLength = inStream.read(wrapped);
            byte[] data = unwrap(wrapped, 0, wLength, msgProp);
            outStream.write(data);
            outStream.flush();
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
    }

    public int unwrap(InputStream inStream,
                      byte[] outBuf, int outOffset,
                      MessageProp msgProp) throws GSSException {
        byte[] wrapped = null;
        int wLength = 0;
        try {
            wrapped = new byte[inStream.available()];
            wLength = inStream.read(wrapped);
            byte[] result = unwrap(wrapped, 0, wLength, msgProp);
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
        byte[] result = unwrap(wrapped, 0, wLength, msgProp);
        System.arraycopy(result, 0, outBuf, outOffset, result.length);
        return result.length;
    }

    public byte[] getMIC(byte[] in, int offset, int len,
                         MessageProp msgProp) throws GSSException {
        int qop = (msgProp == null? 0:msgProp.getQOP());
        byte[] inMsg = in;
        if ((offset != 0) || (len != in.length)) {
            inMsg = new byte[len];
            System.arraycopy(in, offset, inMsg, 0, len);
        }
        return cStub.getMic(pContext, qop, inMsg);
    }

    public void getMIC(InputStream inStream, OutputStream outStream,
                       MessageProp msgProp) throws GSSException {
        try {
            int length = 0;
            byte[] msg = new byte[inStream.available()];
            length = inStream.read(msg);

            byte[] msgToken = getMIC(msg, 0, length, msgProp);
            if ((msgToken != null) && msgToken.length != 0) {
                outStream.write(msgToken);
            }
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
    }

    public void verifyMIC(byte[] inToken, int tOffset, int tLen,
                          byte[] inMsg, int mOffset, int mLen,
                          MessageProp msgProp) throws GSSException {
        byte[] token = inToken;
        byte[] msg = inMsg;
        if ((tOffset != 0) || (tLen != inToken.length)) {
            token = new byte[tLen];
            System.arraycopy(inToken, tOffset, token, 0, tLen);
        }
        if ((mOffset != 0) || (mLen != inMsg.length)) {
            msg = new byte[mLen];
            System.arraycopy(inMsg, mOffset, msg, 0, mLen);
        }
        cStub.verifyMic(pContext, token, msg, msgProp);
    }

    public void verifyMIC(InputStream tokStream, InputStream msgStream,
                          MessageProp msgProp) throws GSSException {
        try {
            byte[] msg = new byte[msgStream.available()];
            int mLength = msgStream.read(msg);
            byte[] tok = new byte[tokStream.available()];
            int tLength = tokStream.read(tok);
            verifyMIC(tok, 0, tLength, msg, 0, mLength, msgProp);
        } catch (IOException ioe) {
            throw new GSSExceptionImpl(GSSException.FAILURE, ioe);
        }
    }

    public byte[] export() throws GSSException {
        byte[] result = cStub.exportContext(pContext);
        pContext = 0;
        return result;
    }

    private void changeFlags(int flagMask, boolean isEnable) {
        if (isInitiator && pContext == 0) {
            if (isEnable) {
                flags |= flagMask;
            } else {
                flags &= ~flagMask;
            }
        }
    }
    public void requestMutualAuth(boolean state) throws GSSException {
        changeFlags(GSS_C_MUTUAL_FLAG, state);
    }
    public void requestReplayDet(boolean state) throws GSSException {
        changeFlags(GSS_C_REPLAY_FLAG, state);
    }
    public void requestSequenceDet(boolean state) throws GSSException {
        changeFlags(GSS_C_SEQUENCE_FLAG, state);
    }
    public void requestCredDeleg(boolean state) throws GSSException {
        changeFlags(GSS_C_DELEG_FLAG, state);
    }
    public void requestAnonymity(boolean state) throws GSSException {
        changeFlags(GSS_C_ANON_FLAG, state);
    }
    public void requestConf(boolean state) throws GSSException {
        changeFlags(GSS_C_CONF_FLAG, state);
    }
    public void requestInteg(boolean state) throws GSSException {
        changeFlags(GSS_C_INTEG_FLAG, state);
    }
    public void requestDelegPolicy(boolean state) throws GSSException {
        // Not supported, ignore
    }
    public void requestLifetime(int lifetime) throws GSSException {
        if (isInitiator && pContext == 0) {
            this.lifetime = lifetime;
        }
    }
    public void setChannelBinding(ChannelBinding cb) throws GSSException {
        if (pContext == 0) {
            this.cb = cb;
        }
    }

    private boolean checkFlags(int flagMask) {
        return ((flags & flagMask) != 0);
    }
    public boolean getCredDelegState() {
        return checkFlags(GSS_C_DELEG_FLAG);
    }
    public boolean getMutualAuthState() {
        return checkFlags(GSS_C_MUTUAL_FLAG);
    }
    public boolean getReplayDetState() {
        return checkFlags(GSS_C_REPLAY_FLAG);
    }
    public boolean getSequenceDetState() {
        return checkFlags(GSS_C_SEQUENCE_FLAG);
    }
    public boolean getAnonymityState() {
        return checkFlags(GSS_C_ANON_FLAG);
    }
    public boolean isTransferable() throws GSSException {
        return checkFlags(GSS_C_TRANS_FLAG);
    }
    public boolean isProtReady() {
        return checkFlags(GSS_C_PROT_READY_FLAG);
    }
    public boolean getConfState() {
        return checkFlags(GSS_C_CONF_FLAG);
    }
    public boolean getIntegState() {
        return checkFlags(GSS_C_INTEG_FLAG);
    }
    public boolean getDelegPolicyState() {
        return false;
    }
    public int getLifetime() {
        return cStub.getContextTime(pContext);
    }
    public GSSNameSpi getSrcName() throws GSSException {
        return srcName;
    }
    public GSSNameSpi getTargName() throws GSSException {
        return targetName;
    }
    public Oid getMech() throws GSSException {
        if (isEstablished && actualMech != null) {
            return actualMech;
        } else {
            return cStub.getMech();
        }
    }
    public GSSCredentialSpi getDelegCred() throws GSSException {
        disposeDelegatedCred = null;
        return delegatedCred;
    }
    public boolean isInitiator() {
        return isInitiator;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        dispose();
    }

    public Object inquireSecContext(String type)
            throws GSSException {
        throw new GSSException(GSSException.UNAVAILABLE, -1,
                "Inquire type not supported.");
    }
}
