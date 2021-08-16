/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 * A principal identified by a distinguished name as specified by
 * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a>.
 *
 * <p>
 * After successful authentication, a user {@link java.security.Principal}
 * can be associated with a particular {@link javax.security.auth.Subject}
 * to augment that <code>Subject</code> with an additional identity.
 * Authorization decisions can then be based upon the
 * <code>Principal</code>s that are associated with a <code>Subject</code>.
 *
 * <p>
 * This class is immutable.
 *
 * @since 1.6
 */
public final class LdapPrincipal implements Principal, java.io.Serializable {

    private static final long serialVersionUID = 6820120005580754861L;

    /**
     * The principal's string name
     *
     * @serial
     */
    private final String nameString;

    /**
     * The principal's name
     *
     * @serial
     */
    private final LdapName name;

    /**
     * Creates an LDAP principal.
     *
     * @param name The principal's string distinguished name.
     * @throws InvalidNameException If a syntax violation is detected.
     * @exception NullPointerException If the <code>name</code> is
     * <code>null</code>.
     */
    public LdapPrincipal(String name) throws InvalidNameException {
        if (name == null) {
            throw new NullPointerException("null name is illegal");
        }
        this.name = getLdapName(name);
        nameString = name;
    }

    /**
     * Compares this principal to the specified object.
     *
     * @param object The object to compare this principal against.
     * @return true if they are equal; false otherwise.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof LdapPrincipal) {
            try {

                return
                    name.equals(getLdapName(((LdapPrincipal)object).getName()));

            } catch (InvalidNameException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Computes the hash code for this principal.
     *
     * @return The principal's hash code.
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Returns the name originally used to create this principal.
     *
     * @return The principal's string name.
     */
    public String getName() {
        return nameString;
    }

    /**
     * Creates a string representation of this principal's name in the format
     * defined by <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a>.
     * If the name has zero components an empty string is returned.
     *
     * @return The principal's string name.
     */
    public String toString() {
        return name.toString();
    }

    // Create an LdapName object from a string distinguished name.
    private LdapName getLdapName(String name) throws InvalidNameException {
        return new LdapName(name);
    }
}
