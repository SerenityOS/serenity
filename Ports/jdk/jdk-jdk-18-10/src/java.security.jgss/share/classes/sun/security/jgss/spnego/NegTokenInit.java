/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import org.ietf.jgss.*;
import sun.security.jgss.*;
import sun.security.util.*;

/**
 * Implements the SPNEGO NegTokenInit token
 * as specified in RFC 2478
 *
 * NegTokenInit ::= SEQUENCE {
 *      mechTypes       [0] MechTypeList  OPTIONAL,
 *      reqFlags        [1] ContextFlags  OPTIONAL,
 *      mechToken       [2] OCTET STRING  OPTIONAL,
 *      mechListMIC     [3] OCTET STRING  OPTIONAL
 * }
 *
 * MechTypeList ::= SEQUENCE OF MechType
 *
 * MechType::= OBJECT IDENTIFIER
 *
 * ContextFlags ::= BIT STRING {
 *      delegFlag       (0),
 *      mutualFlag      (1),
 *      replayFlag      (2),
 *      sequenceFlag    (3),
 *      anonFlag        (4),
 *      confFlag        (5),
 *      integFlag       (6)
 * }
 *
 * @author Seema Malkani
 * @since 1.6
 */

public class NegTokenInit extends SpNegoToken {

    // DER-encoded mechTypes
    private byte[] mechTypes = null;
    private Oid[] mechTypeList = null;

    private BitArray reqFlags = null;
    private byte[] mechToken = null;
    private byte[] mechListMIC = null;

    NegTokenInit(byte[] mechTypes, BitArray flags,
                byte[] token, byte[] mechListMIC)
    {
        super(NEG_TOKEN_INIT_ID);
        this.mechTypes = mechTypes;
        this.reqFlags = flags;
        this.mechToken = token;
        this.mechListMIC = mechListMIC;
    }

    // Used by sun.security.jgss.wrapper.NativeGSSContext
    // to parse SPNEGO tokens
    public NegTokenInit(byte[] in) throws GSSException {
        super(NEG_TOKEN_INIT_ID);
        parseToken(in);
    }

    final byte[] encode() throws GSSException {
        try {
            // create negInitToken
            DerOutputStream initToken = new DerOutputStream();

            // DER-encoded mechTypes with CONTEXT 00
            if (mechTypes != null) {
                initToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                                true, (byte) 0x00), mechTypes);
            }

            // write context flags with CONTEXT 01
            if (reqFlags != null) {
                DerOutputStream flags = new DerOutputStream();
                flags.putUnalignedBitString(reqFlags);
                initToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                                true, (byte) 0x01), flags);
            }

            // mechToken with CONTEXT 02
            if (mechToken != null) {
                DerOutputStream dataValue = new DerOutputStream();
                dataValue.putOctetString(mechToken);
                initToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                                true, (byte) 0x02), dataValue);
            }

            // mechListMIC with CONTEXT 03
            if (mechListMIC != null) {
                if (DEBUG) {
                    System.out.println("SpNegoToken NegTokenInit: " +
                                        "sending MechListMIC");
                }
                DerOutputStream mic = new DerOutputStream();
                mic.putOctetString(mechListMIC);
                initToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                                true, (byte) 0x03), mic);
            }

            // insert in a SEQUENCE
            DerOutputStream out = new DerOutputStream();
            out.write(DerValue.tag_Sequence, initToken);

            return out.toByteArray();

        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                "Invalid SPNEGO NegTokenInit token : " + e.getMessage());
        }
    }

    private void parseToken(byte[] in) throws GSSException {
        try {
            DerValue der = new DerValue(in);
            // verify NegotiationToken type token
            if (!der.isContextSpecific((byte) NEG_TOKEN_INIT_ID)) {
                throw new IOException("SPNEGO NegoTokenInit : " +
                                "did not have right token type");
            }
            DerValue tmp1 = der.data.getDerValue();
            if (tmp1.tag != DerValue.tag_Sequence) {
                throw new IOException("SPNEGO NegoTokenInit : " +
                                "did not have the Sequence tag");
            }

            // parse various fields if present
            int lastField = -1;
            while (tmp1.data.available() > 0) {
                DerValue tmp2 = tmp1.data.getDerValue();
                if (tmp2.isContextSpecific((byte)0x00)) {
                    // get the DER-encoded sequence of mechTypes
                    lastField = checkNextField(lastField, 0);
                    DerInputStream mValue = tmp2.data;
                    mechTypes = mValue.toByteArray();

                    // read all the mechTypes
                    DerValue[] mList = mValue.getSequence(0);
                    mechTypeList = new Oid[mList.length];
                    ObjectIdentifier mech = null;
                    for (int i = 0; i < mList.length; i++) {
                        mech = mList[i].getOID();
                        if (DEBUG) {
                            System.out.println("SpNegoToken NegTokenInit: " +
                                    "reading Mechanism Oid = " + mech);
                        }
                        mechTypeList[i] = new Oid(mech.toString());
                    }
                } else if (tmp2.isContextSpecific((byte)0x01)) {
                    lastField = checkNextField(lastField, 1);
                    // received reqFlags, skip it
                } else if (tmp2.isContextSpecific((byte)0x02)) {
                    lastField = checkNextField(lastField, 2);
                    if (DEBUG) {
                        System.out.println("SpNegoToken NegTokenInit: " +
                                            "reading Mech Token");
                    }
                    mechToken = tmp2.data.getOctetString();
                } else if (tmp2.isContextSpecific((byte)0x03)) {
                    lastField = checkNextField(lastField, 3);
                    if (!GSSUtil.useMSInterop()) {
                        mechListMIC = tmp2.data.getOctetString();
                        if (DEBUG) {
                            System.out.println("SpNegoToken NegTokenInit: " +
                                    "MechListMIC Token = " +
                                    getHexBytes(mechListMIC));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                "Invalid SPNEGO NegTokenInit token : " + e.getMessage());
        }
    }

    byte[] getMechTypes() {
        return mechTypes;
    }

    // Used by sun.security.jgss.wrapper.NativeGSSContext
    // to find the mechs in SPNEGO tokens
    public Oid[] getMechTypeList() {
        return mechTypeList;
    }

    BitArray getReqFlags() {
        return reqFlags;
    }

    // Used by sun.security.jgss.wrapper.NativeGSSContext
    // to access the mech token portion of SPNEGO tokens
    public byte[] getMechToken() {
        return mechToken;
    }

    byte[] getMechListMIC() {
        return mechListMIC;
    }

}
