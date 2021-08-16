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

import sun.security.action.GetBooleanAction;
import sun.security.krb5.*;

class AcceptSecContextToken extends InitialToken {

    private KrbApRep apRep = null;

    /**
     * Creates an AcceptSecContextToken for the context acceptor to send to
     * the context initiator.
     */
    public AcceptSecContextToken(Krb5Context context,
                                 KrbApReq apReq)
        throws KrbException, IOException, GSSException {

        boolean useSubkey = GetBooleanAction
                .privilegedGetProperty("sun.security.krb5.acceptor.subkey");

        boolean useSequenceNumber = true;

        EncryptionKey subKey = null;
        if (useSubkey) {
            subKey = new EncryptionKey(apReq.getCreds().getSessionKey());
            context.setKey(Krb5Context.ACCEPTOR_SUBKEY, subKey);
        }
        apRep = new KrbApRep(apReq, useSequenceNumber, subKey);

        context.resetMySequenceNumber(apRep.getSeqNumber().intValue());

        /*
         * Note: The acceptor side context key was set when the
         * InitSecContextToken was received.
         */
    }

    /**
     * Creates an AcceptSecContextToken at the context initiator's side
     * using the bytes received from  the acceptor.
     */
    public AcceptSecContextToken(Krb5Context context,
                                 Credentials serviceCreds, KrbApReq apReq,
                                 InputStream is)
        throws IOException, GSSException, KrbException  {

        int tokenId = ((is.read()<<8) | is.read());

        if (tokenId != Krb5Token.AP_REP_ID)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                   "AP_REP token id does not match!");

        byte[] apRepBytes =
            new sun.security.util.DerValue(is).toByteArray();

        KrbApRep apRep = new KrbApRep(apRepBytes, serviceCreds, apReq);

        /*
         * Allow the context acceptor to set a subkey if desired, even
         * though our context acceptor will not do so.
         */
        EncryptionKey subKey = apRep.getSubKey();
        if (subKey != null) {
            context.setKey(Krb5Context.ACCEPTOR_SUBKEY, subKey);
            /*
            System.out.println("\n\nSub-Session key from AP-REP is: " +
                               getHexBytes(subKey.getBytes()) + "\n");
            */
        }

        Integer apRepSeqNumber = apRep.getSeqNumber();
        int peerSeqNumber = (apRepSeqNumber != null ?
                             apRepSeqNumber.intValue() :
                             0);
        context.resetPeerSequenceNumber(peerSeqNumber);
    }

    public final byte[] encode() throws IOException {
        byte[] apRepBytes = apRep.getMessage();
        byte[] retVal = new byte[2 + apRepBytes.length];
        writeInt(Krb5Token.AP_REP_ID, retVal, 0);
        System.arraycopy(apRepBytes, 0, retVal, 2, apRepBytes.length);
        return retVal;
    }
}
