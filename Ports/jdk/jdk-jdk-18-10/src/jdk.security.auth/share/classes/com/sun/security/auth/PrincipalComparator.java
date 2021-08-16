/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * An object that implements the {@code java.security.Principal}
 * interface typically also implements this interface to provide
 * a means for comparing that object to a specified {@code Subject}.
 *
 * <p> The comparison is achieved via the {@code implies} method.
 * The implementation of the {@code implies} method determines
 * whether this object "implies" the specified {@code Subject}.
 * One example application of this method may be for
 * a "group" object to imply a particular {@code Subject}
 * if that {@code Subject} belongs to the group.
 * Another example application of this method would be for
 * "role" object to imply a particular {@code Subject}
 * if that {@code Subject} is currently acting in that role.
 *
 * <p> Although classes that implement this interface typically
 * also implement the {@code java.security.Principal} interface,
 * it is not required.  In other words, classes may implement the
 * {@code java.security.Principal} interface by itself,
 * the {@code PrincipalComparator} interface by itself,
 * or both at the same time.
 *
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public interface PrincipalComparator {
    /**
     * Check if the specified {@code Subject} is implied by
     * this object.
     *
     * @param  subject the subject to compare
     *
     * @return true if the specified {@code Subject} is implied by
     *          this object, or false otherwise.
     */
    boolean implies(javax.security.auth.Subject subject);
}
