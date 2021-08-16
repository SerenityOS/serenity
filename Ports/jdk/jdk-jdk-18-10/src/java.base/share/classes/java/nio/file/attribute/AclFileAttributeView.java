/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.io.IOException;

/**
 * A file attribute view that supports reading or updating a file's Access
 * Control Lists (ACL) or file owner attributes.
 *
 * <p> ACLs are used to specify access rights to file system objects. An ACL is
 * an ordered list of {@link AclEntry access-control-entries}, each specifying a
 * {@link UserPrincipal} and the level of access for that user principal. This
 * file attribute view defines the {@link #getAcl() getAcl}, and {@link
 * #setAcl(List) setAcl} methods to read and write ACLs based on the ACL
 * model specified in <a href="http://www.ietf.org/rfc/rfc3530.txt"><i>RFC&nbsp;3530:
 * Network File System (NFS) version 4 Protocol</i></a>. This file attribute view
 * is intended for file system implementations that support the NFSv4 ACL model
 * or have a <em>well-defined</em> mapping between the NFSv4 ACL model and the ACL
 * model used by the file system. The details of such mapping are implementation
 * dependent and are therefore unspecified.
 *
 * <p> This class also extends {@code FileOwnerAttributeView} so as to define
 * methods to get and set the file owner.
 *
 * <p> When a file system provides access to a set of {@link FileStore
 * file-systems} that are not homogeneous then only some of the file systems may
 * support ACLs. The {@link FileStore#supportsFileAttributeView
 * supportsFileAttributeView} method can be used to test if a file system
 * supports ACLs.
 *
 * <h2>Interoperability</h2>
 *
 * RFC&nbsp;3530 allows for special user identities to be used on platforms that
 * support the POSIX defined access permissions. The special user identities
 * are "{@code OWNER@}", "{@code GROUP@}", and "{@code EVERYONE@}". When both
 * the {@code AclFileAttributeView} and the {@link PosixFileAttributeView}
 * are supported then these special user identities may be included in ACL {@link
 * AclEntry entries} that are read or written. The file system's {@link
 * UserPrincipalLookupService} may be used to obtain a {@link UserPrincipal}
 * to represent these special identities by invoking the {@link
 * UserPrincipalLookupService#lookupPrincipalByName lookupPrincipalByName}
 * method.
 *
 * <p> <b>Usage Example:</b>
 * Suppose we wish to add an entry to an existing ACL to grant "joe" access:
 * <pre>
 *     // lookup "joe"
 *     UserPrincipal joe = file.getFileSystem().getUserPrincipalLookupService()
 *         .lookupPrincipalByName("joe");
 *
 *     // get view
 *     AclFileAttributeView view = Files.getFileAttributeView(file, AclFileAttributeView.class);
 *
 *     // create ACE to give "joe" read access
 *     AclEntry entry = AclEntry.newBuilder()
 *         .setType(AclEntryType.ALLOW)
 *         .setPrincipal(joe)
 *         .setPermissions(AclEntryPermission.READ_DATA, AclEntryPermission.READ_ATTRIBUTES)
 *         .build();
 *
 *     // read ACL, insert ACE, re-write ACL
 *     List&lt;AclEntry&gt; acl = view.getAcl();
 *     acl.add(0, entry);   // insert before any DENY entries
 *     view.setAcl(acl);
 * </pre>
 *
 * <h2> Dynamic Access </h2>
 * <p> Where dynamic access to file attributes is required, the attributes
 * supported by this attribute view are as follows:
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
 *   <tr>
 *     <th scope="row"> "acl" </th>
 *     <td> {@link List}&lt;{@link AclEntry}&gt; </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "owner" </th>
 *     <td> {@link UserPrincipal} </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 *
 * <p> The {@link Files#getAttribute getAttribute} method may be used to read
 * the ACL or owner attributes as if by invoking the {@link #getAcl getAcl} or
 * {@link #getOwner getOwner} methods.
 *
 * <p> The {@link Files#setAttribute setAttribute} method may be used to
 * update the ACL or owner attributes as if by invoking the {@link #setAcl setAcl}
 * or {@link #setOwner setOwner} methods.
 *
 * <h2> Setting the ACL when creating a file </h2>
 *
 * <p> Implementations supporting this attribute view may also support setting
 * the initial ACL when creating a file or directory. The initial ACL
 * may be provided to methods such as {@link Files#createFile createFile} or {@link
 * Files#createDirectory createDirectory} as an {@link FileAttribute} with {@link
 * FileAttribute#name name} {@code "acl:acl"} and a {@link FileAttribute#value
 * value} that is the list of {@code AclEntry} objects.
 *
 * <p> Where an implementation supports an ACL model that differs from the NFSv4
 * defined ACL model then setting the initial ACL when creating the file must
 * translate the ACL to the model supported by the file system. Methods that
 * create a file should reject (by throwing {@link IOException IOException})
 * any attempt to create a file that would be less secure as a result of the
 * translation.
 *
 * @since 1.7
 */

public interface AclFileAttributeView
    extends FileOwnerAttributeView
{
    /**
     * Returns the name of the attribute view. Attribute views of this type
     * have the name {@code "acl"}.
     */
    @Override
    String name();

    /**
     * Reads the access control list.
     *
     * <p> When the file system uses an ACL model that differs from the NFSv4
     * defined ACL model, then this method returns an ACL that is the translation
     * of the ACL to the NFSv4 ACL model.
     *
     * <p> The returned list is modifiable so as to facilitate changes to the
     * existing ACL. The {@link #setAcl setAcl} method is used to update
     * the file's ACL attribute.
     *
     * @return  an ordered list of {@link AclEntry entries} representing the
     *          ACL
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    List<AclEntry> getAcl() throws IOException;

    /**
     * Updates (replace) the access control list.
     *
     * <p> Where the file system supports Access Control Lists, and it uses an
     * ACL model that differs from the NFSv4 defined ACL model, then this method
     * must translate the ACL to the model supported by the file system. This
     * method should reject (by throwing {@link IOException IOException}) any
     * attempt to write an ACL that would appear to make the file more secure
     * than would be the case if the ACL were updated. Where an implementation
     * does not support a mapping of {@link AclEntryType#AUDIT} or {@link
     * AclEntryType#ALARM} entries, then this method ignores these entries when
     * writing the ACL.
     *
     * <p> If an ACL entry contains a {@link AclEntry#principal user-principal}
     * that is not associated with the same provider as this attribute view then
     * {@link ProviderMismatchException} is thrown. Additional validation, if
     * any, is implementation dependent.
     *
     * <p> If the file system supports other security related file attributes
     * (such as a file {@link PosixFileAttributes#permissions
     * access-permissions} for example), the updating the access control list
     * may also cause these security related attributes to be updated.
     *
     * @param   acl
     *          the new access control list
     *
     * @throws  IOException
     *          if an I/O error occurs or the ACL is invalid
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, it denies {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     */
    void setAcl(List<AclEntry> acl) throws IOException;
}
