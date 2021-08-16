/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.Control;

/**
  * %%% public for use by LdapSasl %%%
  */
public final class LdapResult {
    int msgId;
    public int status;                  // %%% public for use by LdapSasl
    String matchedDN;
    String errorMessage;
    // Vector<String | Vector<String>>
    Vector<Vector<String>> referrals = null;
    LdapReferralException refEx = null;
    Vector<LdapEntry> entries = null;
    Vector<Control> resControls = null;
    public byte[] serverCreds = null;   // %%% public for use by LdapSasl
    String extensionId = null;          // string OID
    byte[] extensionValue = null;       // BER OCTET STRING


    // This function turns an LdapResult that came from a compare operation
    // into one that looks like it came from a search operation. This is
    // useful when the caller asked the context to do a search, but it was
    // carried out as a compare. In this case, the client still expects a
    // result that looks like it came from a search.
    boolean compareToSearchResult(String name) {
        boolean successful = false;

        switch (status) {
            case LdapClient.LDAP_COMPARE_TRUE:
                status = LdapClient.LDAP_SUCCESS;
                entries = new Vector<>(1,1);
                Attributes attrs = new BasicAttributes(LdapClient.caseIgnore);
                LdapEntry entry = new LdapEntry( name, attrs );
                entries.addElement(entry);
                successful = true;
                break;

            case LdapClient.LDAP_COMPARE_FALSE:
                status = LdapClient.LDAP_SUCCESS;
                entries = new Vector<>(0);
                successful = true;
                break;

            default:
                successful = false;
                break;
        }

        return successful;
    }
}
