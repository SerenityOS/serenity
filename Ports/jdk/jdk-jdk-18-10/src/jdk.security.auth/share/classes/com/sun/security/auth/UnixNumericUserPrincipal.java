/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * and represents a user's Unix identification number (UID).
 *
 * <p> Principals such as this {@code UnixNumericUserPrincipal}
 * may be associated with a particular {@code Subject}
 * to augment that {@code Subject} with an additional
 * identity.  Refer to the {@code Subject} class for more information
 * on how to achieve this.  Authorization decisions can then be based upon
 * the Principals associated with a {@code Subject}.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class UnixNumericUserPrincipal implements
                                        Principal,
                                        java.io.Serializable {
    private static final long serialVersionUID = -4329764253802397821L;

    /**
     * @serial
     */
    private String name;

    /**
     * Create a {@code UnixNumericUserPrincipal} using a
     * {@code String} representation of the
     * user's identification number (UID).
     *
     * @param name the user identification number (UID) for this user.
     *
     * @exception NullPointerException if the {@code name}
     *                  is {@code null}.
     */
    public UnixNumericUserPrincipal(String name) {
        if (name == null) {
            java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("invalid.null.input.value"));
            Object[] source = {"name"};
            throw new NullPointerException(form.format(source));
        }

        this.name = name;
    }

    /**
     * Create a {@code UnixNumericUserPrincipal} using a
     * long representation of the user's identification number (UID).
     *
     * @param name the user identification number (UID) for this user
     *                  represented as a long.
     */
    public UnixNumericUserPrincipal(long name) {
        this.name = Long.toString(name);
    }

    /**
     * Return the user identification number (UID) for this
     * {@code UnixNumericUserPrincipal}.
     *
     * @return the user identification number (UID) for this
     *          {@code UnixNumericUserPrincipal}
     */
    public String getName() {
        return name;
    }

    /**
     * Return the user identification number (UID) for this
     * {@code UnixNumericUserPrincipal} as a long.
     *
     * @return the user identification number (UID) for this
     *          {@code UnixNumericUserPrincipal} as a long.
     */
    public long longValue() {
        return Long.parseLong(name);
    }

    /**
     * Return a string representation of this
     * {@code UnixNumericUserPrincipal}.
     *
     * @return a string representation of this
     *          {@code UnixNumericUserPrincipal}.
     */
    public String toString() {
        java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("UnixNumericUserPrincipal.name"));
        Object[] source = {name};
        return form.format(source);
    }

    /**
     * Compares the specified Object with this
     * {@code UnixNumericUserPrincipal}
     * for equality.  Returns true if the given object is also a
     * {@code UnixNumericUserPrincipal} and the two
     * UnixNumericUserPrincipals
     * have the same user identification number (UID).
     *
     * @param o Object to be compared for equality with this
     *          {@code UnixNumericUserPrincipal}.
     *
     * @return true if the specified Object is equal to this
     *          {@code UnixNumericUserPrincipal}.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof UnixNumericUserPrincipal))
            return false;
        UnixNumericUserPrincipal that = (UnixNumericUserPrincipal)o;

        if (this.getName().equals(that.getName()))
            return true;
        return false;
    }

    /**
     * Return a hash code for this {@code UnixNumericUserPrincipal}.
     *
     * @return a hash code for this {@code UnixNumericUserPrincipal}.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
