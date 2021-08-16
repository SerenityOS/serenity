/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.ldap;

import java.io.IOException;
import javax.naming.*;
import javax.naming.directory.*;
import com.sun.jndi.ldap.Ber;
import com.sun.jndi.ldap.BerDecoder;
import com.sun.jndi.ldap.LdapCtx;

/**
 * Indicates whether the requested sort of search results was successful or not.
 * When the result code indicates success then the results have been sorted as
 * requested. Otherwise the sort was unsuccessful and additional details
 * regarding the cause of the error may have been provided by the server.
 * <p>
 * The code sample in {@link SortControl} shows how this class may be used.
 * <p>
 * This class implements the LDAPv3 Response Control for server-side sorting
 * as defined in
 * <a href="http://www.ietf.org/rfc/rfc2891.txt">RFC 2891</a>.
 *
 * The control's value has the following ASN.1 definition:
 * <pre>
 *
 *     SortResult ::= SEQUENCE {
 *        sortResult  ENUMERATED {
 *            success                   (0), -- results are sorted
 *            operationsError           (1), -- server internal failure
 *            timeLimitExceeded         (3), -- timelimit reached before
 *                                           -- sorting was completed
 *            strongAuthRequired        (8), -- refused to return sorted
 *                                           -- results via insecure
 *                                           -- protocol
 *            adminLimitExceeded       (11), -- too many matching entries
 *                                           -- for the server to sort
 *            noSuchAttribute          (16), -- unrecognized attribute
 *                                           -- type in sort key
 *            inappropriateMatching    (18), -- unrecognized or inappro-
 *                                           -- priate matching rule in
 *                                           -- sort key
 *            insufficientAccessRights (50), -- refused to return sorted
 *                                           -- results to this client
 *            busy                     (51), -- too busy to process
 *            unwillingToPerform       (53), -- unable to sort
 *            other                    (80)
 *            },
 *      attributeType [0] AttributeType OPTIONAL }
 *
 * </pre>
 *
 * @since 1.5
 * @see SortControl
 * @author Vincent Ryan
 */
public final class SortResponseControl extends BasicControl {

    /**
     * The server-side sort response control's assigned object identifier
     * is 1.2.840.113556.1.4.474.
     */
    public static final String OID = "1.2.840.113556.1.4.474";

    private static final long serialVersionUID = 5142939176006310877L;

    /**
     * The sort result code.
     *
     * @serial
     */
    private int resultCode = 0;

    /**
     * The ID of the attribute that caused the sort to fail.
     *
     * @serial
     */
    private String badAttrId = null;

    /**
     * Constructs a control to indicate the outcome of a sort request.
     *
     * @param   id              The control's object identifier string.
     * @param   criticality     The control's criticality.
     * @param   value           The control's ASN.1 BER encoded value.
     *                          It is not cloned - any changes to value
     *                          will affect the contents of the control.
     * @throws                  IOException if an error is encountered
     *                          while decoding the control's value.
     */
    public SortResponseControl(String id, boolean criticality, byte[] value)
        throws IOException {

        super(id, criticality, value);

        // decode value
        BerDecoder ber = new BerDecoder(value, 0, value.length);

        ber.parseSeq(null);
        resultCode = ber.parseEnumeration();
        if ((ber.bytesLeft() > 0) && (ber.peekByte() == Ber.ASN_CONTEXT)) {
            badAttrId = ber.parseStringWithTag(Ber.ASN_CONTEXT, true, null);
        }
    }

    /**
     * Determines if the search results have been successfully sorted.
     * If an error occurred during sorting a NamingException is thrown.
     *
     * @return    true if the search results have been sorted.
     */
    public boolean isSorted() {
        return (resultCode == 0); // a result code of zero indicates success
    }

    /**
     * Retrieves the LDAP result code of the sort operation.
     *
     * @return    The result code. A zero value indicates success.
     */
    public int getResultCode() {
        return resultCode;
    }

    /**
     * Retrieves the ID of the attribute that caused the sort to fail.
     * Returns null if no ID was returned by the server.
     *
     * @return The possibly null ID of the bad attribute.
     */
    public String getAttributeID() {
        return badAttrId;
    }

    /**
     * Retrieves the NamingException appropriate for the result code.
     *
     * @return A NamingException or null if the result code indicates
     *         success.
     */
    public NamingException getException() {

        return LdapCtx.mapErrorCode(resultCode, null);
    }
}
