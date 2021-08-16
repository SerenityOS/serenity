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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A UnresolvedPermissionCollection stores a collection
 * of UnresolvedPermission permissions.
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.UnresolvedPermission
 *
 *
 * @author Roland Schemers
 * @since 1.2
 *
 * @serial include
 */

final class UnresolvedPermissionCollection
extends PermissionCollection
implements java.io.Serializable
{
    /**
     * Key is permission type, value is a list of the UnresolvedPermissions
     * of the same type.
     * Not serialized; see serialization section at end of class.
     */
    private transient ConcurrentHashMap<String, List<UnresolvedPermission>> perms;

    /**
     * Create an empty UnresolvedPermissionCollection object.
     *
     */
    public UnresolvedPermissionCollection() {
        perms = new ConcurrentHashMap<>(11);
    }

    /**
     * Adds a permission to this UnresolvedPermissionCollection.
     * The key for the hash is the unresolved permission's type (class) name.
     *
     * @param permission the Permission object to add.
     */
    @Override
    public void add(Permission permission) {
        if (!(permission instanceof UnresolvedPermission unresolvedPermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);

        // Add permission to map. NOTE: cannot use lambda for
        // remappingFunction parameter until JDK-8076596 is fixed.
        perms.compute(unresolvedPermission.getName(),
            new java.util.function.BiFunction<>() {
                @Override
                public List<UnresolvedPermission> apply(String key,
                                        List<UnresolvedPermission> oldValue) {
                    if (oldValue == null) {
                        List<UnresolvedPermission> v =
                            new CopyOnWriteArrayList<>();
                        v.add(unresolvedPermission);
                        return v;
                    } else {
                        oldValue.add(unresolvedPermission);
                        return oldValue;
                    }
                }
            }
        );
    }

    /**
     * get any unresolved permissions of the same type as p,
     * and return the List containing them.
     */
    List<UnresolvedPermission> getUnresolvedPermissions(Permission p) {
        return perms.get(p.getClass().getName());
    }

    /**
     * always returns false for unresolved permissions
     *
     */
    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    /**
     * Returns an enumeration of all the UnresolvedPermission lists in the
     * container.
     *
     * @return an enumeration of all the UnresolvedPermission objects.
     */
    @Override
    public Enumeration<Permission> elements() {
        List<Permission> results =
            new ArrayList<>(); // where results are stored

        // Get iterator of Map values (which are lists of permissions)
        for (List<UnresolvedPermission> l : perms.values()) {
            results.addAll(l);
        }

        return Collections.enumeration(results);
    }

    @java.io.Serial
    private static final long serialVersionUID = -7176153071733132400L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    // private Hashtable permissions; // keyed on type

    /**
     * @serialField permissions java.util.Hashtable
     *     A table of the UnresolvedPermissions keyed on type, value is Vector
     *     of permissions
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Hashtable.class),
    };

    /**
     * Writes the contents of the perms field out as a Hashtable
     * in which the values are Vectors for
     * serialization compatibility with earlier releases.
     *
     * @param  out the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Copy perms into a Hashtable
        Hashtable<String, Vector<UnresolvedPermission>> permissions =
            new Hashtable<>(perms.size()*2);

        // Convert each entry (List) into a Vector
        Set<Map.Entry<String, List<UnresolvedPermission>>> set = perms.entrySet();
        for (Map.Entry<String, List<UnresolvedPermission>> e : set) {
            // Convert list into Vector
            List<UnresolvedPermission> list = e.getValue();
            Vector<UnresolvedPermission> vec = new Vector<>(list);

            // Add to Hashtable being serialized
            permissions.put(e.getKey(), vec);
        }

        // Write out serializable fields
        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /**
     * Reads in a Hashtable in which the values are Vectors of
     * UnresolvedPermissions and saves them in the perms field.
     *
     * @param  in the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get permissions
        @SuppressWarnings("unchecked")
        // writeObject writes a Hashtable<String, Vector<UnresolvedPermission>>
        // for the permissions key, so this cast is safe, unless the data is corrupt.
        Hashtable<String, Vector<UnresolvedPermission>> permissions =
                (Hashtable<String, Vector<UnresolvedPermission>>)
                gfields.get("permissions", null);
        perms = new ConcurrentHashMap<>(permissions.size()*2);

        // Convert each entry (Vector) into a List
        Set<Map.Entry<String, Vector<UnresolvedPermission>>> set = permissions.entrySet();
        for (Map.Entry<String, Vector<UnresolvedPermission>> e : set) {
            // Convert Vector into ArrayList
            Vector<UnresolvedPermission> vec = e.getValue();
            List<UnresolvedPermission> list = new CopyOnWriteArrayList<>(vec);

            // Add to Hashtable being serialized
            perms.put(e.getKey(), list);
        }
    }
}
