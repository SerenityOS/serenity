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

/**
 * This class implements the LDAPv3 Request Control for the persistent search
 * mechanism as defined in
 * <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapext-psearch-02.txt">draft-ietf-ldapext-psearch-02.txt</a>.
 *
 * The control's value has the following ASN.1 definition:
 * <pre>
 *
 *     PersistentSearch ::= SEQUENCE {
 *         changeTypes INTEGER,
 *         changesOnly BOOLEAN,
 *         returnECs BOOLEAN
 *     }
 *
 * </pre>
 *
 * @see EntryChangeResponseControl
 * @author Vincent Ryan
 */
public final class PersistentSearchControl extends BasicControl {

    /**
     * The persistent search control's assigned object identifier
     * is 2.16.840.1.113730.3.4.3.
     */
    public static final String OID = "2.16.840.1.113730.3.4.3";

    /**
     * Indicates interest in entries which have been added.
     */
    public static final int ADD = 1;

    /**
     * Indicates interest in entries which have been deleted.
     */
    public static final int DELETE = 2;

    /**
     * Indicates interest in entries which have been modified.
     */
    public static final int MODIFY = 4;

    /**
     * Indicates interest in entries which have been renamed.
     */
    public static final int RENAME = 8;

    /**
     * Indicates interest in entries which have been added, deleted,
     * modified or renamed.
     */
    public static final int ANY = ADD | DELETE | MODIFY | RENAME;

    /**
     * The change types of interest. All changes, by default.
     *
     * @serial
     */
    private int changeTypes = ANY;

    /**
     * Return original entries and changed entries or only changed entries.
     *
     * @serial
     */
    private boolean changesOnly = false;

    /**
     * Return entry change controls.
     *
     * @serial
     */
    private boolean returnControls = true;

    private static final long serialVersionUID = 6335140491154854116L;

    /**
     * Constructs a persistent search non-critical control.
     * The original entries, any changed entries (additions,
     * deletions, modifications or renames) and entry change
     * controls are requested.
     *
     * @exception IOException If a BER encoding error occurs.
     */
    public PersistentSearchControl() throws IOException {
        super(OID);
        super.value = setEncodedValue();
    }

    /**
     * Constructs a persistent search control.
     *
     * @param   changeTypes     The change types of interest.
     * @param   changesOnly     Return original entries and changed entries
     *                          or only the changed entries.
     * @param   returnControls  Return entry change controls.
     * @param   criticality     The control's criticality.
     * @exception IOException If a BER encoding error occurs.
     */
    public PersistentSearchControl(int changeTypes, boolean changesOnly,
        boolean returnControls, boolean criticality) throws IOException {

        super(OID, criticality, null);
        this.changeTypes = changeTypes;
        this.changesOnly = changesOnly;
        this.returnControls = returnControls;
        super.value = setEncodedValue();
    }

    /*
     * Sets the ASN.1 BER encoded value of the persistent search control.
     * The result is the raw BER bytes including the tag and length of
     * the control's value. It does not include the controls OID or criticality.
     *
     * @return A possibly null byte array representing the ASN.1 BER encoded
     *         value of the LDAP persistent search control.
     * @exception IOException If a BER encoding error occurs.
     */
    private byte[] setEncodedValue() throws IOException {

        // build the ASN.1 encoding
        BerEncoder ber = new BerEncoder(32);

        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeInt(changeTypes);
            ber.encodeBoolean(changesOnly);
            ber.encodeBoolean(returnControls);
        ber.endSeq();

        return ber.getTrimmedBuf();
    }
}
