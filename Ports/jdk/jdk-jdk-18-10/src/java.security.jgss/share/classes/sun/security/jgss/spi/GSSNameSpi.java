/*
 * Copyright (c) 2000, 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.spi;

import org.ietf.jgss.*;
import java.security.Provider;

/**
 * This interface is implemented by a mechanism specific name element. A
 * GSSName is conceptually a container class of several name elements from
 * different mechanisms.
 *
 * @author Mayank Upadhyay
 */

public interface GSSNameSpi {

    public Provider getProvider();

    /**
     * Equals method for the GSSNameSpi objects.
     * If either name denotes an anonymous principal, the call should
     * return false.
     *
     * @param name to be compared with
     * @return true if they both refer to the same entity, else false
     * @exception GSSException with major codes of BAD_NAMETYPE,
     *    BAD_NAME, FAILURE
     */
    public boolean equals(GSSNameSpi name) throws GSSException;

    /**
     * Compares this <code>GSSNameSpi</code> object to another Object
     * that might be a <code>GSSNameSpi</code>. The behaviour is exactly
     * the same as in {@link #equals(GSSNameSpi) equals} except that
     * no GSSException is thrown; instead, false will be returned in the
     * situation where an error occurs.
     *
     * @param another the object to be compared to
     * @return true if they both refer to the same entity, else false
     * @see #equals(GSSNameSpi)
     */
    public boolean equals(Object another);

    /**
     * Returns a hashcode value for this GSSNameSpi.
     *
     * @return a hashCode value
     */
    public int hashCode();

    /**
     * Returns a flat name representation for this object. The name
     * format is defined in RFC 2078.
     *
     * @return the flat name representation for this object
     * @exception GSSException with major codes NAME_NOT_MN, BAD_NAME,
     *    BAD_NAME, FAILURE.
     */
    public byte[] export() throws GSSException;


    /**
     * Get the mechanism type that this NameElement corresponds to.
     *
     * @return the Oid of the mechanism type
     */
    public Oid getMechanism();

    /**
     * Returns a string representation for this name. The printed
     * name type can be obtained by calling getStringNameType().
     *
     * @return string form of this name
     * @see #getStringNameType()
     * @overrides Object#toString
     */
    public String toString();


    /**
     * Returns the oid describing the format of the printable name.
     *
     * @return the Oid for the format of the printed name
     */
    public Oid getStringNameType();

    /**
     * Indicates if this name object represents an Anonymous name.
     */
    public boolean isAnonymousName();
}
