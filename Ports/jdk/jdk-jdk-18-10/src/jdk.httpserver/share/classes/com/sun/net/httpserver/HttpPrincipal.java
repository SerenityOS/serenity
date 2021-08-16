/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.net.httpserver;
import java.security.Principal;

/**
 * Represents a user authenticated by HTTP Basic or Digest
 * authentication.
 */
public class HttpPrincipal implements Principal {
    private String username, realm;

    /**
     * Creates a {@code HttpPrincipal} from the given {@code username} and
     * {@code realm}.
     *
     * @param username the name of the user within the realm
     * @param realm the realm for this user
     * @throws NullPointerException if either username or realm are {@code null}
     */
    public HttpPrincipal(String username, String realm) {
        if (username == null || realm == null) {
            throw new NullPointerException();
        }
        this.username = username;
        this.realm = realm;
    }

    /**
     * Compare two instances of {@code HttpPrincipal}. Returns {@code true} if
     * <i>another</i> is an instance of {@code HttpPrincipal}, and its username
     * and realm are equal to this object's username and realm. Returns {@code false}
     * otherwise.
     *
     * @param another the object to compare this instance of {@code HttpPrincipal} against
     * @return {@code true} or {@code false} depending on whether objects are
     * equal or not
     */
    public boolean equals(Object another) {
        if (!(another instanceof HttpPrincipal)) {
            return false;
        }
        HttpPrincipal theother = (HttpPrincipal)another;
        return (username.equals(theother.username) &&
                realm.equals(theother.realm));
    }

    /**
     * Returns the contents of this principal in the form
     * <i>realm:username</i>.
     *
     * @return the contents of this principal in the form realm:username
     */
    public String getName() {
        return String.format("%s:%s", realm, username);
    }

    /**
     * Returns the {@code username} this object was created with.
     *
     * @return the name of the user associated with this object
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the {@code realm} this object was created with.
     *
     * @return the realm associated with this object
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Returns a hashcode for this {@code HttpPrincipal}. This is calculated
     * as {@code (getUsername()+getRealm()).hashCode()}.
     *
     * @return the hashcode for this object
     */
    public int hashCode() {
        return (username+realm).hashCode();
    }

    /**
     * Returns the same string as {@link #getName()}.
     *
     * @return the name associated with this object
     */
    public String toString() {
        return getName();
    }
}
