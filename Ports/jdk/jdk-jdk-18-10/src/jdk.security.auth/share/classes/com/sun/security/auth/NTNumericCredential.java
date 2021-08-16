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

/**
 * This class abstracts an NT security token
 * and provides a mechanism to do same-process security impersonation.
 */

public class NTNumericCredential {

    private long impersonationToken;

    /**
     * Create an {@code NTNumericCredential} with an integer value.
     *
     * @param token the Windows NT security token for this user.
     */
    public NTNumericCredential(long token) {
        this.impersonationToken = token;
    }

    /**
     * Return an integer representation of this
     * {@code NTNumericCredential}.
     *
     * @return an integer representation of this
     *          {@code NTNumericCredential}.
     */
    public long getToken() {
        return impersonationToken;
    }

    /**
     * Return a string representation of this {@code NTNumericCredential}.
     *
     * @return a string representation of this {@code NTNumericCredential}.
     */
    public String toString() {
        java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("NTNumericCredential.name"));
        Object[] source = {Long.toString(impersonationToken)};
        return form.format(source);
    }

    /**
     * Compares the specified Object with this {@code NTNumericCredential}
     * for equality.  Returns true if the given object is also a
     * {@code NTNumericCredential} and the two NTNumericCredentials
     * represent the same NT security token.
     *
     * @param o Object to be compared for equality with this
     *          {@code NTNumericCredential}.
     *
     * @return true if the specified Object is equal to this
     *          {@code NTNumericCredential}.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof NTNumericCredential))
            return false;
        NTNumericCredential that = (NTNumericCredential)o;

        if (impersonationToken == that.getToken())
            return true;
        return false;
    }

    /**
     * Return a hash code for this {@code NTNumericCredential}.
     *
     * @return a hash code for this {@code NTNumericCredential}.
     */
    public int hashCode() {
        return (int)this.impersonationToken;
    }
}
