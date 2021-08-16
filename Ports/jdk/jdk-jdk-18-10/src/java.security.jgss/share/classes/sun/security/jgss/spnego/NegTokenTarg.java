/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.ietf.jgss.*;
import sun.security.jgss.*;
import sun.security.util.*;

/**
 * Implements the SPNEGO NegTokenTarg token
 * as specified in RFC 2478
 *
 * NegTokenTarg ::= SEQUENCE {
 *      negResult   [0] ENUMERATED {
 *              accept_completed        (0),
 *              accept_incomplete       (1),
 *              reject                  (2) }   OPTIONAL,
 *      supportedMech   [1] MechType            OPTIONAL,
 *      responseToken   [2] OCTET STRING        OPTIONAL,
 *      mechListMIC     [3] OCTET STRING        OPTIONAL
 * }
 *
 * MechType::= OBJECT IDENTIFIER
 *
 *
 * @author Seema Malkani
 * @since 1.6
 */

public class NegTokenTarg extends SpNegoToken {

    private int negResult = 0;
    private Oid supportedMech = null;
    private byte[] responseToken = null;
    private byte[] mechListMIC = null;

    NegTokenTarg(int result, Oid mech, byte[] token, byte[] mechListMIC)
    {
        super(NEG_TOKEN_TARG_ID);
        this.negResult = result;
        this.supportedMech = mech;
        this.responseToken = token;
        this.mechListMIC = mechListMIC;
    }

    // Used by sun.security.jgss.wrapper.NativeGSSContext
    // to parse SPNEGO tokens
    public NegTokenTarg(byte[] in) throws GSSException {
        super(NEG_TOKEN_TARG_ID);
        parseToken(in);
    }

    final byte[] encode() throws GSSException {
        try {
            // create negTargToken
            DerOutputStream targToken = new DerOutputStream();

            // write the negotiated result with CONTEXT 00
            DerOutputStream result = new DerOutputStream();
            result.putEnumerated(negResult);
            targToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                true, (byte) 0x00), result);

            // supportedMech with CONTEXT 01
            if (supportedMech != null) {
                DerOutputStream mech = new DerOutputStream();
                byte[] mechType = supportedMech.getDER();
                mech.write(mechType);
                targToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                                true, (byte) 0x01), mech);
            }

            // response Token with CONTEXT 02
            if (responseToken != null) {
                DerOutputStream rspToken = new DerOutputStream();
                rspToken.putOctetString(responseToken);
                targToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                        true, (byte) 0x02), rspToken);
            }

            // mechListMIC with CONTEXT 03
            if (mechListMIC != null) {
                if (DEBUG) {
                    System.out.println("SpNegoToken NegTokenTarg: " +
                                                "sending MechListMIC");
                }
                DerOutputStream mic = new DerOutputStream();
                mic.putOctetString(mechListMIC);
                targToken.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                        true, (byte) 0x03), mic);
            }

            // insert in a SEQUENCE
            DerOutputStream out = new DerOutputStream();
            out.write(DerValue.tag_Sequence, targToken);

            return out.toByteArray();

        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                "Invalid SPNEGO NegTokenTarg token : " + e.getMessage());
        }
    }

    private void parseToken(byte[] in) throws GSSException {
        try {
            DerValue der = new DerValue(in);
            // verify NegotiationToken type token
            if (!der.isContextSpecific((byte) NEG_TOKEN_TARG_ID)) {
                throw new IOException("SPNEGO NegoTokenTarg : " +
                        "did not have the right token type");
            }
            DerValue tmp1 = der.data.getDerValue();
            if (tmp1.tag != DerValue.tag_Sequence) {
                throw new IOException("SPNEGO NegoTokenTarg : " +
                        "did not have the Sequence tag");
            }

            // parse various fields if present
            int lastField = -1;
            while (tmp1.data.available() > 0) {
                DerValue tmp2 = tmp1.data.getDerValue();
                if (tmp2.isContextSpecific((byte)0x00)) {
                    lastField = checkNextField(lastField, 0);
                    negResult = tmp2.data.getEnumerated();
                    if (DEBUG) {
                        System.out.println("SpNegoToken NegTokenTarg: negotiated" +
                                    " result = " + getNegoResultString(negResult));
                    }
                } else if (tmp2.isContextSpecific((byte)0x01)) {
                    lastField = checkNextField(lastField, 1);
                    ObjectIdentifier mech = tmp2.data.getOID();
                    supportedMech = new Oid(mech.toString());
                    if (DEBUG) {
                        System.out.println("SpNegoToken NegTokenTarg: " +
                                    "supported mechanism = " + supportedMech);
                    }
                } else if (tmp2.isContextSpecific((byte)0x02)) {
                    lastField = checkNextField(lastField, 2);
                    responseToken = tmp2.data.getOctetString();
                } else if (tmp2.isContextSpecific((byte)0x03)) {
                    lastField = checkNextField(lastField, 3);
                    if (!GSSUtil.useMSInterop()) {
                        mechListMIC = tmp2.data.getOctetString();
                        if (DEBUG) {
                            System.out.println("SpNegoToken NegTokenTarg: " +
                                                "MechListMIC Token = " +
                                                getHexBytes(mechListMIC));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                "Invalid SPNEGO NegTokenTarg token : " + e.getMessage());
        }
    }

    int getNegotiatedResult() {
        return negResult;
    }

    // Used by sun.security.jgss.wrapper.NativeGSSContext
    // to find the supported mech in SPNEGO tokens
    public Oid getSupportedMech() {
        return supportedMech;
    }

    byte[] getResponseToken() {
        return responseToken;
    }

    byte[] getMechListMIC() {
        return mechListMIC;
    }
}
