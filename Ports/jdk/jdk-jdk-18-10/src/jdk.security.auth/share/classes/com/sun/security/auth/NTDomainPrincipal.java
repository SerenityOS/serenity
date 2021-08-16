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
 * and represents the name of the Windows NT domain into which the
 * user authenticated.  This will be a domain name if the user logged
 * into a Windows NT domain, a workgroup name if the user logged into
 * a workgroup, or a machine name if the user logged into a standalone
 * configuration.
 *
 * <p> Principals such as this {@code NTDomainPrincipal}
 * may be associated with a particular {@code Subject}
 * to augment that {@code Subject} with an additional
 * identity.  Refer to the {@code Subject} class for more information
 * on how to achieve this.  Authorization decisions can then be based upon
 * the Principals associated with a {@code Subject}.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class NTDomainPrincipal implements Principal, java.io.Serializable {

    private static final long serialVersionUID = -4408637351440771220L;

    /**
     * @serial
     */
    private String name;

    /**
     * Create an {@code NTDomainPrincipal} with a Windows NT domain name.
     *
     * @param name the Windows NT domain name for this user.
     *
     * @exception NullPointerException if the {@code name}
     *                  is {@code null}.
     */
    public NTDomainPrincipal(String name) {
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
     * Return the Windows NT domain name for this
     * {@code NTDomainPrincipal}.
     *
     * @return the Windows NT domain name for this
     *                  {@code NTDomainPrincipal}
     */
    public String getName() {
        return name;
    }

    /**
     * Return a string representation of this {@code NTDomainPrincipal}.
     *
     * @return a string representation of this {@code NTDomainPrincipal}.
     */
    public String toString() {
        java.text.MessageFormat form = new java.text.MessageFormat
                (sun.security.util.ResourcesMgr.getAuthResourceString
                        ("NTDomainPrincipal.name"));
        Object[] source = {name};
        return form.format(source);
    }

    /**
     * Compares the specified Object with this {@code NTDomainPrincipal}
     * for equality.  Returns true if the given object is also a
     * {@code NTDomainPrincipal} and the two NTDomainPrincipals
     * have the same name.
     *
     * @param o Object to be compared for equality with this
     *          {@code NTDomainPrincipal}.
     *
     * @return true if the specified Object is equal to this
     *          {@code NTDomainPrincipal}.
     */
    public boolean equals(Object o) {
        if (o == null)
                return false;

        if (this == o)
            return true;

        if (!(o instanceof NTDomainPrincipal))
            return false;
        NTDomainPrincipal that = (NTDomainPrincipal)o;

            if (name.equals(that.getName()))
                return true;
            return false;
    }

    /**
     * Return a hash code for this {@code NTDomainPrincipal}.
     *
     * @return a hash code for this {@code NTDomainPrincipal}.
     */
    public int hashCode() {
        return this.getName().hashCode();
    }
}
