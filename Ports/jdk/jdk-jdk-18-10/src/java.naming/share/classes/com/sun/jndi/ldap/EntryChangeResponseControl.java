/*
 * Copyright (c) 1999, 2002, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.io.IOException;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * This class implements the LDAPv3 Response Control for entry-change
 * notification as defined in
 * <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapext-psearch-02.txt">draft-ietf-ldapext-psearch-02.txt</a>.
 *
 * The control's value has the following ASN.1 definition:
 * <pre>
 *
 *     EntryChangeNotification ::= SEQUENCE {
 *         changeType ENUMERATED {
 *             add              (1),
 *             delete           (2),
 *             modify           (4),
 *             modDN            (8)
 *         },
 *         previousDN   LDAPDN OPTIONAL,        -- modifyDN ops. only
 *         changeNumber INTEGER OPTIONAL,       -- if supported
 *    }
 *
 * </pre>
 *
 * @see PersistentSearchControl
 * @see com.sun.jndi.ldap.ctl.ResponseControlFactory ResponseControlFactory
 * @author Vincent Ryan
 */
public final class EntryChangeResponseControl extends BasicControl {

    /**
     * The entry-change response control's assigned object identifier
     * is 2.16.840.1.113730.3.4.7.
     */
    public static final String OID = "2.16.840.1.113730.3.4.7";

    /**
     * Indicates an entry which has been added.
     */
    public static final int ADD = 1;

    /**
     * Indicates an entry which has been deleted.
     */
    public static final int DELETE = 2;

    /**
     * Indicates an entry which has been modified.
     */
    public static final int MODIFY = 4;

    /**
     * Indicates an entry which has been renamed.
     */
    public static final int RENAME = 8;

    /**
     * The type of change that occurred.
     *
     * @serial
     */
    private int changeType;

    /**
     * The previous distinguished name (only applies to RENAME changes).
     *
     * @serial
     */
    private String previousDN = null;

    /**
     * The change number (if supported by the server).
     *
     * @serial
     */
    private long changeNumber = -1L;

    private static final long serialVersionUID = -2087354136750180511L;

    /**
     * Constructs a new instance of EntryChangeResponseControl.
     *
     * @param   id              The control's object identifier string.
     * @param   criticality     The control's criticality.
     * @param   value           The control's ASN.1 BER encoded value.
     *                          May be null.
     * @exception               IOException if an error is encountered
     *                          while decoding the control's value.
     */
    public EntryChangeResponseControl(String id, boolean criticality,
        byte[] value) throws IOException {

        super(id, criticality, value);

        // decode value
        if ((value != null) && (value.length > 0)) {
            BerDecoder ber = new BerDecoder(value, 0, value.length);

            ber.parseSeq(null);
            changeType = ber.parseEnumeration();

            if ((ber.bytesLeft() > 0) && (ber.peekByte() == Ber.ASN_OCTET_STR)){
                previousDN = ber.parseString(true);
            }
            if ((ber.bytesLeft() > 0) && (ber.peekByte() == Ber.ASN_INTEGER)) {
                changeNumber = ber.parseInt();
            }
        }
    }

    /**
     * Retrieves the type of change that occurred.
     *
     * @return    The type of change.
     */
    public int getChangeType() {
        return changeType;
    }

    /**
     * Retrieves the previous distinguished name of the entry before it was
     * renamed and/or moved. This method applies only to RENAME changes.
     *
     * @return    The previous distinguished name or null if not applicable.
     */
    public String getPreviousDN() {
        return previousDN;
    }

    /**
     * Retrieves the change number assigned by the server for this change.
     * Returns -1 if this feature is not supported by the server.
     *
     * @return    The change number or -1 if unsupported.
     */
    public long getChangeNumber() {
        return changeNumber;
    }
}
