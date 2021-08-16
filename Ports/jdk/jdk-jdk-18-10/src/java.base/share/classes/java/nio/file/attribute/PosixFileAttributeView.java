/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;
import java.util.Set;
import java.io.IOException;

/**
 * A file attribute view that provides a view of the file attributes commonly
 * associated with files on file systems used by operating systems that implement
 * the Portable Operating System Interface (POSIX) family of standards.
 *
 * <p> Operating systems that implement the <a href="http://www.opengroup.org">
 * POSIX</a> family of standards commonly use file systems that have a
 * file <em>owner</em>, <em>group-owner</em>, and related <em>access
 * permissions</em>. This file attribute view provides read and write access
 * to these attributes.
 *
 * <p> The {@link #readAttributes() readAttributes} method is used to read the
 * file's attributes. The file {@link PosixFileAttributes#owner() owner} is
 * represented by a {@link UserPrincipal} that is the identity of the file owner
 * for the purposes of access control. The {@link PosixFileAttributes#group()
 * group-owner}, represented by a {@link GroupPrincipal}, is the identity of the
 * group owner, where a group is an identity created for administrative purposes
 * so as to determine the access rights for the members of the group.
 *
 * <p> The {@link PosixFileAttributes#permissions() permissions} attribute is a
 * set of access permissions. This file attribute view provides access to the nine
 * permission bits defined by the {@link PosixFilePermission} class.
 * These nine permission bits determine the <em>read</em>, <em>write</em>, and
 * <em>execute</em> access for the file owner, group, and others (others
 * meaning identities other than the owner and members of the group). Some
 * operating systems and file systems may provide additional permission bits
 * but access to these other bits is not defined by this class in this release.
 *
 * <p> <b>Usage Example:</b>
 * Suppose we need to print out the owner and access permissions of a file:
 * <pre>
 *     Path file = ...
 *     PosixFileAttributes attrs = Files.getFileAttributeView(file, PosixFileAttributeView.class)
 *         .readAttributes();
 *     System.out.format("%s %s%n",
 *         attrs.owner().getName(),
 *         PosixFilePermissions.toString(attrs.permissions()));
 * </pre>
 *
 * <h2> Dynamic Access </h2>
 * <p> Where dynamic access to file attributes is required, the attributes
 * supported by this attribute view are as defined by {@link
 * BasicFileAttributeView} and {@link FileOwnerAttributeView}, and in addition,
 * the following attributes are supported:
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Supported attributes</caption>
 * <thead>
 *   <tr>
 *     <th scope="col"> Name </th>
 *     <th scope="col"> Type </th>
 *   </tr>
 * </thead>
 * <tbody>
 *  <tr>
 *     <th scope="row"> "permissions" </th>
 *     <td> {@link Set}&lt;{@link PosixFilePermission}&gt; </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "group" </th>
 *     <td> {@link GroupPrincipal} </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 *
 * <p> The {@link Files#getAttribute getAttribute} method may be used to read
 * any of these attributes, or any of the attributes defined by {@link
 * BasicFileAttributeView} as if by invoking the {@link #readAttributes
 * readAttributes()} method.
 *
 * <p> The {@link Files#setAttribute setAttribute} method may be used to update
 * the file's last modified time, last access time or create time attributes as
 * defined by {@link BasicFileAttributeView}. It may also be used to update
 * the permissions, owner, or group-owner as if by invoking the {@link
 * #setPermissions setPermissions}, {@link #setOwner setOwner}, and {@link
 * #setGroup setGroup} methods respectively.
 *
 * <h2> Setting Initial Permissions </h2>
 * <p> Implementations supporting this attribute view may also support setting
 * the initial permissions when creating a file or directory. The
 * initial permissions are provided to the {@link Files#createFile createFile}
 * or {@link Files#createDirectory createDirectory} methods as a {@link
 * FileAttribute} with {@link FileAttribute#name name} {@code "posix:permissions"}
 * and a {@link FileAttribute#value value} that is the set of permissions. The
 * following example uses the {@link PosixFilePermissions#asFileAttribute
 * asFileAttribute} method to construct a {@code FileAttribute} when creating a
 * file:
 *
 * <pre>
 *     Path path = ...
 *     Set&lt;PosixFilePermission&gt; perms =
 *         EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ);
 *     Files.createFile(path, PosixFilePermissions.asFileAttribute(perms));
 * </pre>
 *
 * <p> When the access permissions are set at file creation time then the actual
 * value of the permissions may differ from the value of the attribute object.
 * The reasons for this are implementation specific. On UNIX systems, for
 * example, a process has a <em>umask</em> that impacts the permission bits
 * of newly created files. Where an implementation supports the setting of
 * the access permissions, and the underlying file system supports access
 * permissions, then it is required that the value of the actual access
 * permissions will be equal or less than the value of the attribute
 * provided to the {@link Files#createFile createFile} or {@link
 * Files#createDirectory createDirectory} methods. In other words, the file may
 * be more secure than requested.
 *
 * @since 1.7
 */

public interface PosixFileAttributeView
    extends BasicFileAttributeView, FileOwnerAttributeView
{
    /**
     * Returns the name of the attribute view. Attribute views of this type
     * have the name {@code "posix"}.
     */
    @Override
    String name();

    /**
     * @throws  IOException                {@inheritDoc}
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    @Override
    PosixFileAttributes readAttributes() throws IOException;

    /**
     * Updates the file permissions.
     *
     * @param   perms
     *          the new set of permissions
     *
     * @throws  ClassCastException
     *          if the sets contains elements that are not of type {@code
     *          PosixFilePermission}
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     */
    void setPermissions(Set<PosixFilePermission> perms) throws IOException;

    /**
     * Updates the file group-owner.
     *
     * @param   group
     *          the new file group-owner
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     */
    void setGroup(GroupPrincipal group) throws IOException;
}
