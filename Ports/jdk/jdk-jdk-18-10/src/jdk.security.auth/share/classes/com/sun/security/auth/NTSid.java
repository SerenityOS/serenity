/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.auth;

import java.security.Principal;

/**
 * This class implements the {@code Principal} interface
 * and represents information about a Windows NT user, group or realm.
 *
 * <p> Windows NT chooses to represent users, groups and realms (or domains)
 * with not only common names, but also relatively unique numbers.  These
 * numbers are called Security IDentifiers, or SIDs.  Windows NT
 * also provides services that render these SIDs into string forms.
 * This class represents these string forms.
 *
 * <p> Principals such as this {@code NTSid}
 * may be associated with a particular {@code Subject}
 * to augment that {@code Subject} with an additional
 * identity.  Refer to the {@code Subject} class for more information
 * on how to achieve this.  Authorization decisions can then be based upon
 * the Principals associated with a {@code Subject}.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class NTSid implements Principal, java.io.Serializable {

    private static final long serialVersionUID = 4412290580770249885L;

    /**
     * @serial
     */
    private String sid;

    /**
     * Create an {@code NTSid} with a Windows NT SID.
     *
     * @param stringSid the Windows NT SID.
     *
     * @exception NullPointerException if the {@code String}
     *                  is {@code null}.
     *
     * @exception IllegalArgumentException if the {@code String}
     *                  has zero length.
     */
    public NTSid (String stringSid) {
        if (stringSid == null) {
            java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("invalid.null.input.value"));
            Object[] source = {"stringSid"};
            throw new NullPointerException(form.format(source));
        }
        if (stringSid.length() == 0) {
            throw new IllegalArgumentException
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("Invalid.NTSid.value"));
        }
        sid = new String(stringSid);
    }

    /**
     * Return a string version of this {@code NTSid}.
     *
     * @return a string version of this {@code NTSid}
     */
    public String getName() {
        return sid;
    }

    /**
     * Return a string representation of this {@code NTSid}.
     *
     * @return a string representation of this {@code NTSid}.
     */
    public String toString() {
        java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("NTSid.name"));
        Object[] source = {sid};
        return form.format(source);
    }

    /**
     * Compares the specified Object with this {@code NTSid}
     * for equality.  Returns true if the given object is also a
     * {@code NTSid} and the two NTSids have the same String
     * representation.
     *
     * @param o Object to be compared for equality with this
     *          {@code NTSid}.
     *
     * @return true if the specified Object is equal to this
     *          {@code NTSid}.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof NTSid))
            return false;
        NTSid that = (NTSid)o;

        if (sid.equals(that.sid)) {
            return true;
        }
        return false;
    }

    /**
     * Return a hash code for this {@code NTSid}.
     *
     * @return a hash code for this {@code NTSid}.
     */
    public int hashCode() {
        return sid.hashCode();
    }
}
