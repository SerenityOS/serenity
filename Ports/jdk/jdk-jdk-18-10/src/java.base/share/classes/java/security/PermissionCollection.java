/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Abstract class representing a collection of Permission objects.
 *
 * <p>With a PermissionCollection, you can:
 * <UL>
 * <LI> add a permission to the collection using the {@code add} method.
 * <LI> check to see if a particular permission is implied in the
 *      collection, using the {@code implies} method.
 * <LI> enumerate all the permissions, using the {@code elements} method.
 * </UL>
 *
 * <p>When it is desirable to group together a number of Permission objects
 * of the same type, the {@code newPermissionCollection} method on that
 * particular type of Permission object should first be called. The default
 * behavior (from the Permission class) is to simply return null.
 * Subclasses of class Permission override the method if they need to store
 * their permissions in a particular PermissionCollection object in order
 * to provide the correct semantics when the
 * {@code PermissionCollection.implies} method is called.
 * If a non-null value is returned, that PermissionCollection must be used.
 * If null is returned, then the caller of {@code newPermissionCollection}
 * is free to store permissions of the
 * given type in any PermissionCollection they choose
 * (one that uses a Hashtable, one that uses a Vector, etc).
 *
 * <p>The PermissionCollection returned by the
 * {@code Permission.newPermissionCollection}
 * method is a homogeneous collection, which stores only Permission objects
 * for a given Permission type.  A PermissionCollection may also be
 * heterogeneous.  For example, Permissions is a PermissionCollection
 * subclass that represents a collection of PermissionCollections.
 * That is, its members are each a homogeneous PermissionCollection.
 * For example, a Permissions object might have a FilePermissionCollection
 * for all the FilePermission objects, a SocketPermissionCollection for all the
 * SocketPermission objects, and so on. Its {@code add} method adds a
 * permission to the appropriate collection.
 *
 * <p>Whenever a permission is added to a heterogeneous PermissionCollection
 * such as Permissions, and the PermissionCollection doesn't yet contain a
 * PermissionCollection of the specified permission's type, the
 * PermissionCollection should call
 * the {@code newPermissionCollection} method on the permission's class
 * to see if it requires a special PermissionCollection. If
 * {@code newPermissionCollection}
 * returns null, the PermissionCollection
 * is free to store the permission in any type of PermissionCollection it
 * desires (one using a Hashtable, one using a Vector, etc.). For example,
 * the Permissions object uses a default PermissionCollection implementation
 * that stores the permission objects in a Hashtable.
 *
 * <p> Subclass implementations of PermissionCollection should assume
 * that they may be called simultaneously from multiple threads,
 * and therefore should be synchronized properly.  Furthermore,
 * Enumerations returned via the {@code elements} method are
 * not <em>fail-fast</em>.  Modifications to a collection should not be
 * performed while enumerating over that collection.
 *
 * @see Permission
 * @see Permissions
 *
 *
 * @author Roland Schemers
 * @since 1.2
 */

public abstract class PermissionCollection implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = -6727011328946861783L;

    /**
     * Whether this permission collection is read-only.
     * <p>
     * If set, the {@code add} method will throw an exception.
     */
    private volatile boolean readOnly;

    /**
     * Constructor for subclasses to call.
     */
    public PermissionCollection() {}

    /**
     * Adds a permission object to the current collection of permission objects.
     *
     * @param permission the Permission object to add.
     *
     * @throws    SecurityException    if this PermissionCollection object
     *                                 has been marked readonly
     * @throws    IllegalArgumentException   if this PermissionCollection
     *                object is a homogeneous collection and the permission
     *                is not of the correct type.
     */
    public abstract void add(Permission permission);

    /**
     * Checks to see if the specified permission is implied by
     * the collection of Permission objects held in this PermissionCollection.
     *
     * @param permission the Permission object to compare.
     *
     * @return true if "permission" is implied by the  permissions in
     * the collection, false if not.
     */
    public abstract boolean implies(Permission permission);

    /**
     * Returns an enumeration of all the Permission objects in the collection.
     *
     * @return an enumeration of all the Permissions.
     * @see #elementsAsStream()
     */
    public abstract Enumeration<Permission> elements();

    /**
     * Returns a stream of all the Permission objects in the collection.
     *
     * <p> The collection should not be modified (see {@link #add}) during the
     * execution of the terminal stream operation. Otherwise, the result of the
     * terminal stream operation is undefined.
     *
     * @implSpec
     * The default implementation creates a stream whose source is derived from
     * the enumeration returned from a call to {@link #elements()}.
     *
     * @return a stream of all the Permissions.
     * @since 9
     */
    public Stream<Permission> elementsAsStream() {
        int characteristics = isReadOnly()
                ? Spliterator.NONNULL | Spliterator.IMMUTABLE
                : Spliterator.NONNULL;
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        elements().asIterator(), characteristics),
                false);
    }

    /**
     * Marks this PermissionCollection object as "readonly". After
     * a PermissionCollection object
     * is marked as readonly, no new Permission objects can be added to it
     * using {@code add}.
     */
    public void setReadOnly() {
        readOnly = true;
    }

    /**
     * Returns true if this PermissionCollection object is marked as readonly.
     * If it is readonly, no new Permission objects can be added to it
     * using {@code add}.
     *
     * <p>By default, the object is <i>not</i> readonly. It can be set to
     * readonly by a call to {@code setReadOnly}.
     *
     * @return true if this PermissionCollection object is marked as readonly,
     * false otherwise.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns a string describing this PermissionCollection object,
     * providing information about all the permissions it contains.
     * The format is:
     * <pre>
     * super.toString() (
     *   // enumerate all the Permission
     *   // objects and call toString() on them,
     *   // one per line..
     * )</pre>
     *
     * {@code super.toString} is a call to the {@code toString}
     * method of this
     * object's superclass, which is Object. The result is
     * this PermissionCollection's type name followed by this object's
     * hashcode, thus enabling clients to differentiate different
     * PermissionCollections object, even if they contain the same permissions.
     *
     * @return information about this PermissionCollection object,
     *         as described above.
     *
     */
    public String toString() {
        Enumeration<Permission> enum_ = elements();
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()+" (\n");
        while (enum_.hasMoreElements()) {
            try {
                sb.append(" ");
                sb.append(enum_.nextElement().toString());
                sb.append("\n");
            } catch (NoSuchElementException e){
                // ignore
            }
        }
        sb.append(")\n");
        return sb.toString();
    }
}
