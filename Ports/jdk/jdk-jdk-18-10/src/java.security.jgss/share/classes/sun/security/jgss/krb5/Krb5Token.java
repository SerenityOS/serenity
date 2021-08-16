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

package sun.security.jgss.krb5;

import java.io.IOException;
import sun.security.util.*;
import sun.security.jgss.*;

/**
 * This class represents a base class for all Kerberos v5 GSS-API
 * tokens. It contains commonly used definitions and utilities.
 *
 * @author Mayank Upadhyay
 */

abstract class Krb5Token extends GSSToken {

    /**
     * The token id defined for the token emitted by the initSecContext call
     * carrying the AP_REQ .
     */
    public static final int AP_REQ_ID = 0x0100;

    /**
     * The token id defined for the token emitted by the acceptSecContext call
     * carrying the AP_REP .
     */
    public static final int AP_REP_ID = 0x0200;

    /**
     * The token id defined for any token carrying a KRB-ERR message.
     */
    public static final int ERR_ID    = 0x0300;

    /**
     * The token id defined for the token emitted by the getMIC call.
     */
    public static final int MIC_ID    = 0x0101;

    /**
     * The token id defined for the token emitted by the wrap call.
     */
    public static final int WRAP_ID   = 0x0201;

    // new token ID draft-ietf-krb-wg-gssapi-cfx-07.txt
    public static final int MIC_ID_v2  = 0x0404;
    public static final int WRAP_ID_v2 = 0x0504;

    /**
     * The object identifier corresponding to the Kerberos v5 GSS-API
     * mechanism.
     */
    public static ObjectIdentifier OID;

    static {
        try {
            OID = ObjectIdentifier.of(Krb5MechFactory.
                                       GSS_KRB5_MECH_OID.toString());
        } catch (IOException ioe) {
          // should not happen
        }
    }

    /**
     * Returns a strign representing the token type.
     *
     * @param tokenId the token id for which a string name is desired
     * @return the String name of this token type
     */
    public static String getTokenName(int tokenId) {
        String retVal = null;
        switch (tokenId) {
            case AP_REQ_ID:
            case AP_REP_ID:
                retVal = "Context Establishment Token";
                break;
            case MIC_ID:
                retVal = "MIC Token";
                break;
            case MIC_ID_v2:
                retVal = "MIC Token (new format)";
                break;
            case WRAP_ID:
                retVal = "Wrap Token";
                break;
            case WRAP_ID_v2:
                retVal = "Wrap Token (new format)";
                break;
            default:
                retVal = "Kerberos GSS-API Mechanism Token";
                break;
        }
        return retVal;
    }
}
