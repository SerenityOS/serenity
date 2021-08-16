/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.IOException;

import sun.security.action.GetPropertyAction;
import sun.security.krb5.*;
import java.net.InetAddress;
import sun.security.krb5.internal.AuthorizationData;
import sun.security.krb5.internal.KerberosTime;

class InitSecContextToken extends InitialToken {

    // If non-mutual authentication is requested, there is no AP-REP message.
    // The acceptor thus has no chance to send the seq-number field to the
    // initiator. In this case, the initiator and acceptor should has an
    // agreement to derive acceptor's initial seq-number if the acceptor wishes
    // to send messages to the initiator.

    // If this flag is true, it will the same as the initiator's initial
    // seq-number (as MIT krb5 and Windows SSPI do). Otherwise, it will be zero
    // (as Heimdal does). The default value is true.
    private static final boolean ACCEPTOR_USE_INITIATOR_SEQNUM;

    static {
        // The ACCEPTOR_USE_INITIATOR_SEQNUM value is determined by the system
        // property "sun.security.krb5.acceptor.sequence.number.nonmutual",
        // which can be set to "initiator", "zero" or "0".
        String propName = "sun.security.krb5.acceptor.sequence.number.nonmutual";
        String s = GetPropertyAction.privilegedGetProperty(propName, "initiator");
        if (s.equals("initiator")) {
            ACCEPTOR_USE_INITIATOR_SEQNUM = true;
        } else if (s.equals("zero") || s.equals("0")) {
            ACCEPTOR_USE_INITIATOR_SEQNUM = false;
        } else {
            throw new AssertionError("Unrecognized value for " + propName
                    + ": " + s);
        }
    }

    private KrbApReq apReq = null;

    /**
     * For the context initiator to call. It constructs a new
     * InitSecContextToken to send over to the peer containing the desired
     * flags and the AP-REQ. It also updates the context with the local
     * sequence number and shared context key.
     * (When mutual auth is enabled the peer has an opportunity to
     * renegotiate the session key in the followup AcceptSecContextToken
     * that it sends.)
     */
    InitSecContextToken(Krb5Context context,
                               Credentials tgt,
                               Credentials serviceTicket)
        throws KrbException, IOException, GSSException {

        boolean mutualRequired = context.getMutualAuthState();
        boolean useSubkey = true; // MIT Impl will crash if this is not set!
        boolean useSequenceNumber = true;

        OverloadedChecksum gssChecksum =
            new OverloadedChecksum(context, tgt, serviceTicket);

        Checksum checksum = gssChecksum.getChecksum();

        context.setTktFlags(serviceTicket.getFlags());
        context.setAuthTime(
                new KerberosTime(serviceTicket.getAuthTime()).toString());
        apReq = new KrbApReq(serviceTicket,
                             mutualRequired,
                             useSubkey,
                             useSequenceNumber,
                             checksum);

        context.resetMySequenceNumber(apReq.getSeqNumber().intValue());

        EncryptionKey subKey = apReq.getSubKey();
        if (subKey != null)
            context.setKey(Krb5Context.INITIATOR_SUBKEY, subKey);
        else
            context.setKey(Krb5Context.SESSION_KEY, serviceTicket.getSessionKey());

        if (!mutualRequired)
            context.resetPeerSequenceNumber(
                    ACCEPTOR_USE_INITIATOR_SEQNUM
                    ? apReq.getSeqNumber().intValue()
                    : 0);
    }

    /**
     * For the context acceptor to call. It reads the bytes out of an
     * InputStream and constructs an InitSecContextToken with them.
     */
    InitSecContextToken(Krb5Context context, Krb5AcceptCredential cred,
                               InputStream is)
        throws IOException, GSSException, KrbException  {

        int tokenId = ((is.read()<<8) | is.read());

        if (tokenId != Krb5Token.AP_REQ_ID)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                   "AP_REQ token id does not match!");

        // XXX Modify KrbApReq cons to take an InputStream
        byte[] apReqBytes =
            new sun.security.util.DerValue(is).toByteArray();
        //debug("=====ApReqBytes: [" + getHexBytes(apReqBytes) + "]\n");

        InetAddress addr = null;
        if (context.getChannelBinding() != null) {
            addr = context.getChannelBinding().getInitiatorAddress();
        }
        apReq = new KrbApReq(apReqBytes, cred, addr);
        //debug("\nReceived AP-REQ and authenticated it.\n");

        EncryptionKey sessionKey = apReq.getCreds().getSessionKey();

        /*
          System.out.println("\n\nSession key from service ticket is: " +
          getHexBytes(sessionKey.getBytes()));
        */

        EncryptionKey subKey = apReq.getSubKey();
        if (subKey != null) {
            context.setKey(Krb5Context.INITIATOR_SUBKEY, subKey);
            /*
              System.out.println("Sub-Session key from authenticator is: " +
              getHexBytes(subKey.getBytes()) + "\n");
            */
        } else {
            context.setKey(Krb5Context.SESSION_KEY, sessionKey);
            //System.out.println("Sub-Session Key Missing in Authenticator.\n");
        }

        OverloadedChecksum gssChecksum = new OverloadedChecksum(
                context, apReq.getChecksum(), sessionKey, subKey);
        gssChecksum.setContextFlags(context);
        Credentials delegCred = gssChecksum.getDelegatedCreds();
        if (delegCred != null) {
            Krb5CredElement credElement =
                Krb5InitCredential.getInstance(
                                   (Krb5NameElement)context.getSrcName(),
                                   delegCred);
            context.setDelegCred(credElement);
        }

        Integer apReqSeqNumber = apReq.getSeqNumber();
        int peerSeqNumber = (apReqSeqNumber != null ?
                             apReqSeqNumber.intValue() :
                             0);
        context.resetPeerSequenceNumber(peerSeqNumber);
        if (!context.getMutualAuthState()) {
            context.resetMySequenceNumber(
                    ACCEPTOR_USE_INITIATOR_SEQNUM
                            ? peerSeqNumber
                            : 0);
        }
        context.setAuthTime(
                new KerberosTime(apReq.getCreds().getAuthTime()).toString());
        context.setTktFlags(apReq.getCreds().getFlags());
        AuthorizationData ad = apReq.getCreds().getAuthzData();
        context.setAuthzData(ad);
    }

    public final KrbApReq getKrbApReq() {
        return apReq;
    }

    public final byte[] encode() throws IOException {
        byte[] apReqBytes = apReq.getMessage();
        byte[] retVal = new byte[2 + apReqBytes.length];
        writeInt(Krb5Token.AP_REQ_ID, retVal, 0);
        System.arraycopy(apReqBytes, 0, retVal, 2, apReqBytes.length);
        //      System.out.println("GSS-Token with AP_REQ is:");
        //      System.out.println(getHexBytes(retVal));
        return retVal;
    }
}
