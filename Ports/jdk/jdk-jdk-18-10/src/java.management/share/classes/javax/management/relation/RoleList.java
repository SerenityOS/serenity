/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.relation;

import com.sun.jmx.mbeanserver.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A RoleList represents a list of roles (Role objects). It is used as
 * parameter when creating a relation, and when trying to set several roles in
 * a relation (via 'setRoles()' method). It is returned as part of a
 * RoleResult, to provide roles successfully retrieved.
 *
 * @since 1.5
 */
/* We cannot extend ArrayList<Role> because our legacy
   add(Role) method would then override add(E) in ArrayList<E>,
   and our return value is void whereas ArrayList.add(E)'s is boolean.
   Likewise for set(int,Role).  Grrr.  We cannot use covariance
   to override the most important methods and have them return
   Role, either, because that would break subclasses that
   override those methods in turn (using the original return type
   of Object).  Finally, we cannot implement Iterable<Role>
   so you could write
       for (Role r : roleList)
   because ArrayList<> implements Iterable<> and the same class cannot
   implement two versions of a generic interface.  Instead we provide
   the asList() method so you can write
       for (Role r : roleList.asList())
*/
public class RoleList extends ArrayList<Object> {

    private transient boolean typeSafe;
    private transient boolean tainted;

    /* Serial version */
    private static final long serialVersionUID = 5568344346499649313L;

    //
    // Constructors
    //

    /**
     * Constructs an empty RoleList.
     */
    public RoleList() {
        super();
    }

    /**
     * Constructs an empty RoleList with the initial capacity
     * specified.
     *
     * @param initialCapacity  initial capacity
     */
    public RoleList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a {@code RoleList} containing the elements of the
     * {@code List} specified, in the order in which they are returned by
     * the {@code List}'s iterator. The {@code RoleList} instance has
     * an initial capacity of 110% of the size of the {@code List}
     * specified.
     *
     * @param list the {@code List} that defines the initial contents of
     * the new {@code RoleList}.
     *
     * @exception IllegalArgumentException if the {@code list} parameter
     * is {@code null} or if the {@code list} parameter contains any
     * non-Role objects.
     *
     * @see ArrayList#ArrayList(java.util.Collection)
     */
    public RoleList(List<Role> list) throws IllegalArgumentException {
        // Check for null parameter
        //
        if (list == null)
            throw new IllegalArgumentException("Null parameter");

        // Check for non-Role objects
        //
        checkTypeSafe(list);

        // Build the List<Role>
        //
        super.addAll(list);
    }

    /**
     * Return a view of this list as a {@code List<Role>}.
     * Changes to the returned value are reflected by changes
     * to the original {@code RoleList} and vice versa.
     *
     * @return a {@code List<Role>} whose contents
     * reflect the contents of this {@code RoleList}.
     *
     * <p>If this method has ever been called on a given
     * {@code RoleList} instance, a subsequent attempt to add
     * an object to that instance which is not a {@code Role}
     * will fail with an {@code IllegalArgumentException}. For compatibility
     * reasons, a {@code RoleList} on which this method has never
     * been called does allow objects other than {@code Role}s to
     * be added.</p>
     *
     * @throws IllegalArgumentException if this {@code RoleList} contains
     * an element that is not a {@code Role}.
     *
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public List<Role> asList() {
        if (!typeSafe) {
            if (tainted)
                checkTypeSafe(this);
            typeSafe = true;
        }
        return Util.cast(this);
    }

    //
    // Accessors
    //

    /**
     * Adds the Role specified as the last element of the list.
     *
     * @param role  the role to be added.
     *
     * @exception IllegalArgumentException  if the role is null.
     */
    public void add(Role role)
        throws IllegalArgumentException {

        if (role == null) {
            String excMsg = "Invalid parameter";
            throw new IllegalArgumentException(excMsg);
        }
        super.add(role);
    }

    /**
     * Inserts the role specified as an element at the position specified.
     * Elements with an index greater than or equal to the current position are
     * shifted up.
     *
     * @param index  The position in the list where the new Role
     * object is to be inserted.
     * @param role  The Role object to be inserted.
     *
     * @exception IllegalArgumentException  if the role is null.
     * @exception IndexOutOfBoundsException  if accessing with an index
     * outside of the list.
     */
    public void add(int index,
                    Role role)
        throws IllegalArgumentException,
               IndexOutOfBoundsException {

        if (role == null) {
            String excMsg = "Invalid parameter";
            throw new IllegalArgumentException(excMsg);
        }

        super.add(index, role);
    }

    /**
     * Sets the element at the position specified to be the role
     * specified.
     * The previous element at that position is discarded.
     *
     * @param index  The position specified.
     * @param role  The value to which the role element should be set.
     *
     * @exception IllegalArgumentException  if the role is null.
     * @exception IndexOutOfBoundsException  if accessing with an index
     * outside of the list.
     */
     public void set(int index,
                     Role role)
         throws IllegalArgumentException,
                IndexOutOfBoundsException {

        if (role == null) {
            // Revisit [cebro] Localize message
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        super.set(index, role);
     }

    /**
     * Appends all the elements in the RoleList specified to the end
     * of the list, in the order in which they are returned by the Iterator of
     * the RoleList specified.
     *
     * @param roleList  Elements to be inserted into the list (can be null)
     *
     * @return true if this list changed as a result of the call.
     *
     * @exception IndexOutOfBoundsException  if accessing with an index
     * outside of the list.
     *
     * @see ArrayList#addAll(Collection)
     */
    public boolean addAll(RoleList roleList)
        throws IndexOutOfBoundsException {

        if (roleList == null) {
            return true;
        }

        return (super.addAll(roleList));
    }

    /**
     * Inserts all of the elements in the RoleList specified into this
     * list, starting at the specified position, in the order in which they are
     * returned by the Iterator of the RoleList specified.
     *
     * @param index  Position at which to insert the first element from the
     * RoleList specified.
     * @param roleList  Elements to be inserted into the list.
     *
     * @return true if this list changed as a result of the call.
     *
     * @exception IllegalArgumentException  if the role is null.
     * @exception IndexOutOfBoundsException  if accessing with an index
     * outside of the list.
     *
     * @see ArrayList#addAll(int, Collection)
     */
    public boolean addAll(int index,
                          RoleList roleList)
        throws IllegalArgumentException,
               IndexOutOfBoundsException {

        if (roleList == null) {
            // Revisit [cebro] Localize message
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        return (super.addAll(index, roleList));
    }

    /*
     * Override all of the methods from ArrayList<Object> that might add
     * a non-Role to the List, and disallow that if asList has ever
     * been called on this instance.
     */

    @Override
    public boolean add(Object o) {
        if (!tainted)
            tainted = isTainted(o);
        if (typeSafe)
            checkTypeSafe(o);
        return super.add(o);
    }

    @Override
    public void add(int index, Object element) {
        if (!tainted)
            tainted = isTainted(element);
        if (typeSafe)
            checkTypeSafe(element);
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<?> c) {
        if (!tainted)
            tainted = isTainted(c);
        if (typeSafe)
            checkTypeSafe(c);
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        if (!tainted)
            tainted = isTainted(c);
        if (typeSafe)
            checkTypeSafe(c);
        return super.addAll(index, c);
    }

    @Override
    public Object set(int index, Object element) {
        if (!tainted)
            tainted = isTainted(element);
        if (typeSafe)
            checkTypeSafe(element);
        return super.set(index, element);
    }

    /**
     * IllegalArgumentException if o is a non-Role object.
     */
    private static void checkTypeSafe(Object o) {
        try {
            o = (Role) o;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * IllegalArgumentException if c contains any non-Role objects.
     */
    private static void checkTypeSafe(Collection<?> c) {
        try {
            Role r;
            for (Object o : c)
                r = (Role) o;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns true if o is a non-Role object.
     */
    private static boolean isTainted(Object o) {
        try {
            checkTypeSafe(o);
        } catch (IllegalArgumentException e) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if c contains any non-Role objects.
     */
    private static boolean isTainted(Collection<?> c) {
        try {
            checkTypeSafe(c);
        } catch (IllegalArgumentException e) {
            return true;
        }
        return false;
    }
}
