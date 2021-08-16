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
 * This class implements the LDAPv3 Request Control for manageDsaIT as
 * defined in
 * <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapext-namedref-00.txt">draft-ietf-ldapext-namedref-00.txt</a>.
 *
 * The control has no control value.
 *
 * @author Vincent Ryan
 */
public final class ManageReferralControl extends BasicControl {

    /**
     * The manage referral control's assigned object identifier
     * is 2.16.840.1.113730.3.4.2.
     *
     * @serial
     */
    public static final String OID = "2.16.840.1.113730.3.4.2";

    private static final long serialVersionUID = 909382692585717224L;

    /**
     * Constructs a manage referral critical control.
     */
    public ManageReferralControl() {
        super(OID, true, null);
    }

    /**
     * Constructs a manage referral control.
     *
     * @param   criticality The control's criticality setting.
     */
    public ManageReferralControl(boolean criticality) {
        super(OID, criticality, null);
    }
}
