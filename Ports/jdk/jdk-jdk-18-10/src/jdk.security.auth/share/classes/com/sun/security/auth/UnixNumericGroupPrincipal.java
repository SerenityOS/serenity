/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

/**
 * This class implements the {@code Principal} interface
 * and represents a user's Unix group identification number (GID).
 *
 * <p> Principals such as this {@code UnixNumericGroupPrincipal}
 * may be associated with a particular {@code Subject}
 * to augment that {@code Subject} with an additional
 * identity.  Refer to the {@code Subject} class for more information
 * on how to achieve this.  Authorization decisions can then be based upon
 * the Principals associated with a {@code Subject}.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class UnixNumericGroupPrincipal implements
                                        Principal,
                                        java.io.Serializable {

    private static final long serialVersionUID = 3941535899328403223L;

    /**
     * @serial
     */
    private String name;

    /**
     * @serial
     */
    private boolean primaryGroup;

    /**
     * Create a {@code UnixNumericGroupPrincipal} using a
     * {@code String} representation of the user's
     * group identification number (GID).
     *
     * @param name the user's group identification number (GID)
     *                  for this user.
     *
     * @param primaryGroup true if the specified GID represents the
     *                  primary group to which this user belongs.
     *
     * @exception NullPointerException if the {@code name}
     *                  is {@code null}.
     */
    public UnixNumericGroupPrincipal(String name, boolean primaryGroup) {
        if (name == null) {
            java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("invalid.null.input.value"));
            Object[] source = {"name"};
            throw new NullPointerException(form.format(source));
        }

        this.name = name;
        this.primaryGroup = primaryGroup;
    }

    /**
     * Create a {@code UnixNumericGroupPrincipal} using a
     * long representation of the user's group identification number (GID).
     *
     * @param name the user's group identification number (GID) for this user
     *                  represented as a long.
     *
     * @param primaryGroup true if the specified GID represents the
     *                  primary group to which this user belongs.
     *
     */
    public UnixNumericGroupPrincipal(long name, boolean primaryGroup) {
        this.name = Long.toString(name);
        this.primaryGroup = primaryGroup;
    }

    /**
     * Return the user's group identification number (GID) for this
     * {@code UnixNumericGroupPrincipal}.
     *
     * @return the user's group identification number (GID) for this
     *          {@code UnixNumericGroupPrincipal}
     */
    public String getName() {
        return name;
    }

    /**
     * Return the user's group identification number (GID) for this
     * {@code UnixNumericGroupPrincipal} as a long.
     *
     * @return the user's group identification number (GID) for this
     *          {@code UnixNumericGroupPrincipal} as a long.
     */
    public long longValue() {
        return Long.parseLong(name);
    }

    /**
     * Return whether this group identification number (GID) represents
     * the primary group to which this user belongs.
     *
     * @return true if this group identification number (GID) represents
     *          the primary group to which this user belongs,
     *          or false otherwise.
     */
    public boolean isPrimaryGroup() {
        return primaryGroup;
    }

    /**
     * Return a string representation of this
     * {@code UnixNumericGroupPrincipal}.
     *
     * @return a string representation of this
     *          {@code UnixNumericGroupPrincipal}.
     */
    public String toString() {

        if (primaryGroup) {
            java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("UnixNumericGroupPrincipal.Primary.Group.name"));
            Object[] source = {name};
            return form.format(source);
        } else {
            java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                    ("UnixNumericGroupPrincipal.Supplementary.Group.name"));
            Object[] source = {name};
            return form.format(source);
        }
    }

    /**
     * Compares the specified Object with this
     * {@code UnixNumericGroupPrincipal}
     * for equality.  Returns true if the given object is also a
     * {@code UnixNumericGroupPrincipal} and the two
     * UnixNumericGroupPrincipals
     * have the same group identification number (GID).
     *
     * @param o Object to be compared for equality with this
     *          {@code UnixNumericGroupPrincipal}.
     *
     * @return true if the specified Object is equal to this
     *          {@code UnixNumericGroupPrincipal}.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof UnixNumericGroupPrincipal))
            return false;
        UnixNumericGroupPrincipal that = (UnixNumericGroupPrincipal)o;

        if (this.getName().equals(that.getName()) &&
            this.isPrimaryGroup() == that.isPrimaryGroup())
            return true;
        return false;
    }

    /**
     * Return a hash code for this {@code UnixNumericGroupPrincipal}.
     *
     * @return a hash code for this {@code UnixNumericGroupPrincipal}.
     */
    public int hashCode() {
        return Objects.hash(name, isPrimaryGroup());
    }
}
