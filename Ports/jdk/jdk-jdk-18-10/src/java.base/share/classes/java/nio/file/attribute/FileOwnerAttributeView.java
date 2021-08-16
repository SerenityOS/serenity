/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.io.IOException;

/**
 * A file attribute view that supports reading or updating the owner of a file.
 * This file attribute view is intended for file system implementations that
 * support a file attribute that represents an identity that is the owner of
 * the file. Often the owner of a file is the identity of the entity that
 * created the file.
 *
 * <p> The {@link #getOwner getOwner} or {@link #setOwner setOwner} methods may
 * be used to read or update the owner of the file.
 *
 * <p> The {@link java.nio.file.Files#getAttribute getAttribute} and
 * {@link java.nio.file.Files#setAttribute setAttribute} methods may also be
 * used to read or update the owner. In that case, the owner attribute is
 * identified by the name {@code "owner"}, and the value of the attribute is
 * a {@link UserPrincipal}.
 *
 * @since 1.7
 */

public interface FileOwnerAttributeView
    extends FileAttributeView
{
    /**
     * Returns the name of the attribute view. Attribute views of this type
     * have the name {@code "owner"}.
     */
    @Override
    String name();

    /**
     * Read the file owner.
     *
     * <p> It is implementation specific if the file owner can be a {@link
     * GroupPrincipal group}.
     *
     * @return  the file owner
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserInformation")} or its
     *          {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    UserPrincipal getOwner() throws IOException;

    /**
     * Updates the file owner.
     *
     * <p> It is implementation specific if the file owner can be a {@link
     * GroupPrincipal group}. To ensure consistent and correct behavior
     * across platforms it is recommended that this method should only be used
     * to set the file owner to a user principal that is not a group.
     *
     * @param   owner
     *          the new file owner
     *
     * @throws  IOException
     *          if an I/O error occurs, or the {@code owner} parameter is a
     *          group and this implementation does not support setting the owner
     *          to a group
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserInformation")} or its
     *          {@link SecurityManager#checkWrite(String) checkWrite} method
     *          denies write access to the file.
     */
    void setOwner(UserPrincipal owner) throws IOException;
}
