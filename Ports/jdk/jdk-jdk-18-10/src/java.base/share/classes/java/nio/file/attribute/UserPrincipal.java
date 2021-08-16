/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Principal;

/**
 * A {@code Principal} representing an identity used to determine access rights
 * to objects in a file system.
 *
 * <p> On many platforms and file systems an entity requires appropriate access
 * rights or permissions in order to access objects in a file system. The
 * access rights are generally performed by checking the identity of the entity.
 * For example, on implementations that use Access Control Lists (ACLs) to
 * enforce privilege separation then a file in the file system may have an
 * associated ACL that determines the access rights of identities specified in
 * the ACL.
 *
 * <p> A {@code UserPrincipal} object is an abstract representation of an
 * identity. It has a {@link #getName() name} that is typically the username or
 * account name that it represents. User principal objects may be obtained using
 * a {@link UserPrincipalLookupService}, or returned by {@link
 * FileAttributeView} implementations that provide access to identity related
 * attributes. For example, the {@link AclFileAttributeView} and {@link
 * PosixFileAttributeView} provide access to a file's {@link
 * PosixFileAttributes#owner owner}.
 *
 * @since 1.7
 */

public interface UserPrincipal extends Principal { }
