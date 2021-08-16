/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.StringTokenizer;

/** A Permission to perform actions related to MBeanServers.
    The <em>name</em> of the permission specifies the operation requested
    or granted by the permission.  For a granted permission, it can be
    <code>*</code> to allow all of the MBeanServer operations specified below.
    Otherwise, for a granted or requested permission, it must be one of the
    following:
    <dl>
    <dt>createMBeanServer</dt>
    <dd>Create a new MBeanServer object using the method
    {@link MBeanServerFactory#createMBeanServer()} or
    {@link MBeanServerFactory#createMBeanServer(java.lang.String)}.
    <dt>findMBeanServer</dt>
    <dd>Find an MBeanServer with a given name, or all MBeanServers in this
    JVM, using the method {@link MBeanServerFactory#findMBeanServer}.
    <dt>newMBeanServer</dt>
    <dd>Create a new MBeanServer object without keeping a reference to it,
    using the method {@link MBeanServerFactory#newMBeanServer()} or
    {@link MBeanServerFactory#newMBeanServer(java.lang.String)}.
    <dt>releaseMBeanServer</dt>
    <dd>Remove the MBeanServerFactory's reference to an MBeanServer,
    using the method {@link MBeanServerFactory#releaseMBeanServer}.
    </dl>
    The <em>name</em> of the permission can also denote a list of one or more
    comma-separated operations.  Spaces are allowed at the beginning and
    end of the <em>name</em> and before and after commas.
    <p>
    <code>MBeanServerPermission("createMBeanServer")</code> implies
    <code>MBeanServerPermission("newMBeanServer")</code>.
 *
 * @since 1.5
 */
public class MBeanServerPermission extends BasicPermission {
    private static final long serialVersionUID = -5661980843569388590L;

    private static final int
        CREATE = 0,
        FIND = 1,
        NEW = 2,
        RELEASE = 3,
        N_NAMES = 4;

    private static final String[] names = {
        "createMBeanServer",
        "findMBeanServer",
        "newMBeanServer",
        "releaseMBeanServer",
    };

    private static final int
        CREATE_MASK = 1<<CREATE,
        FIND_MASK = 1<<FIND,
        NEW_MASK = 1<<NEW,
        RELEASE_MASK = 1<<RELEASE,
        ALL_MASK = CREATE_MASK|FIND_MASK|NEW_MASK|RELEASE_MASK;

    /*
     * Map from permission masks to canonical names.  This array is
     * filled in on demand.
     *
     * This isn't very scalable.  If we have more than five or six
     * permissions, we should consider doing this differently,
     * e.g. with a Map.
     */
    private static final String[] canonicalNames = new String[1 << N_NAMES];

    /*
     * The target names mask.  This is not private to avoid having to
     * generate accessor methods for accesses from the collection class.
     *
     * This mask includes implied bits.  So if it has CREATE_MASK then
     * it necessarily has NEW_MASK too.
     */
    transient int mask;

    /** <p>Create a new MBeanServerPermission with the given name.</p>
        <p>This constructor is equivalent to
        <code>MBeanServerPermission(name,null)</code>.</p>
        @param name the name of the granted permission.  It must
        respect the constraints spelt out in the description of the
        {@link MBeanServerPermission} class.
        @exception NullPointerException if the name is null.
        @exception IllegalArgumentException if the name is not
        <code>*</code> or one of the allowed names or a comma-separated
        list of the allowed names.
    */
    public MBeanServerPermission(String name) {
        this(name, null);
    }

    /** <p>Create a new MBeanServerPermission with the given name.</p>
        @param name the name of the granted permission.  It must
        respect the constraints spelt out in the description of the
        {@link MBeanServerPermission} class.
        @param actions the associated actions.  This parameter is not
        currently used and must be null or the empty string.
        @exception NullPointerException if the name is null.
        @exception IllegalArgumentException if the name is not
        <code>*</code> or one of the allowed names or a comma-separated
        list of the allowed names, or if <code>actions</code> is a non-null
        non-empty string.
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is empty or
     * if arguments are invalid.
     */
    public MBeanServerPermission(String name, String actions) {
        super(getCanonicalName(parseMask(name)), actions);

        /* It's annoying to have to parse the name twice, but since
           Permission.getName() is final and since we can't access "this"
           until after the call to the superclass constructor, there
           isn't any very clean way to do this.  MBeanServerPermission
           objects aren't constructed very often, luckily.  */
        mask = parseMask(name);

        /* Check that actions is a null empty string */
        if (actions != null && actions.length() > 0)
            throw new IllegalArgumentException("MBeanServerPermission " +
                                               "actions must be null: " +
                                               actions);
    }

    MBeanServerPermission(int mask) {
        super(getCanonicalName(mask));
        this.mask = impliedMask(mask);
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        mask = parseMask(getName());
    }

    static int simplifyMask(int mask) {
        if ((mask & CREATE_MASK) != 0)
            mask &= ~NEW_MASK;
        return mask;
    }

    static int impliedMask(int mask) {
        if ((mask & CREATE_MASK) != 0)
            mask |= NEW_MASK;
        return mask;
    }

    static String getCanonicalName(int mask) {
        if (mask == ALL_MASK)
            return "*";

        mask = simplifyMask(mask);

        synchronized (canonicalNames) {
            if (canonicalNames[mask] == null)
                canonicalNames[mask] = makeCanonicalName(mask);
        }

        return canonicalNames[mask];
    }

    private static String makeCanonicalName(int mask) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < N_NAMES; i++) {
            if ((mask & (1<<i)) != 0) {
                if (buf.length() > 0)
                    buf.append(',');
                buf.append(names[i]);
            }
        }
        return buf.toString().intern();
        /* intern() avoids duplication when the mask has only
           one bit, so is equivalent to the string constants
           we have for the names[] array.  */
    }

    /* Convert the string into a bitmask, including bits that
       are implied by the permissions in the string.  */
    private static int parseMask(String name) {
        /* Check that target name is a non-null non-empty string */
        if (name == null) {
            throw new NullPointerException("MBeanServerPermission: " +
                                           "target name can't be null");
        }

        name = name.trim();
        if (name.equals("*"))
            return ALL_MASK;

        /* If the name is empty, nameIndex will barf. */
        if (name.indexOf(',') < 0)
            return impliedMask(1 << nameIndex(name.trim()));

        int mask = 0;

        StringTokenizer tok = new StringTokenizer(name, ",");
        while (tok.hasMoreTokens()) {
            String action = tok.nextToken();
            int i = nameIndex(action.trim());
            mask |= (1 << i);
        }

        return impliedMask(mask);
    }

    private static int nameIndex(String name)
            throws IllegalArgumentException {
        for (int i = 0; i < N_NAMES; i++) {
            if (names[i].equals(name))
                return i;
        }
        final String msg =
            "Invalid MBeanServerPermission name: \"" + name + "\"";
        throw new IllegalArgumentException(msg);
    }

    public int hashCode() {
        return mask;
    }

    /**
     * <p>Checks if this MBeanServerPermission object "implies" the specified
     * permission.</p>
     *
     * <p>More specifically, this method returns true if:</p>
     *
     * <ul>
     * <li> <i>p</i> is an instance of MBeanServerPermission,</li>
     * <li> <i>p</i>'s target names are a subset of this object's target
     * names</li>
     * </ul>
     *
     * <p>The <code>createMBeanServer</code> permission implies the
     * <code>newMBeanServer</code> permission.</p>
     *
     * @param p the permission to check against.
     * @return true if the specified permission is implied by this object,
     * false if not.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof MBeanServerPermission))
            return false;

        MBeanServerPermission that = (MBeanServerPermission) p;

        return ((this.mask & that.mask) == that.mask);
    }

    /**
     * Checks two MBeanServerPermission objects for equality. Checks that
     * <i>obj</i> is an MBeanServerPermission, and represents the same
     * list of allowable actions as this object.
     *
     * @param obj the object we are testing for equality with this object.
     * @return true if the objects are equal.
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof MBeanServerPermission))
            return false;

        MBeanServerPermission that = (MBeanServerPermission) obj;

        return (this.mask == that.mask);
    }

    public PermissionCollection newPermissionCollection() {
        return new MBeanServerPermissionCollection();
    }
}

/**
 * Class returned by {@link MBeanServerPermission#newPermissionCollection()}.
 *
 * @serial include
 */

/*
 * Since every collection of MBSP can be represented by a single MBSP,
 * that is what our PermissionCollection does.  We need to define a
 * PermissionCollection because the one inherited from BasicPermission
 * doesn't know that createMBeanServer implies newMBeanServer.
 *
 * Though the serial form is defined, the TCK does not check it.  We do
 * not require independent implementations to duplicate it.  Even though
 * PermissionCollection is Serializable, instances of this class will
 * hardly ever be serialized, and different implementations do not
 * typically exchange serialized permission collections.
 *
 * If we did require that a particular form be respected here, we would
 * logically also have to require it for
 * MBeanPermission.newPermissionCollection, which would preclude an
 * implementation from defining a PermissionCollection there with an
 * optimized "implies" method.
 */
class MBeanServerPermissionCollection extends PermissionCollection {
    /** @serial Null if no permissions in collection, otherwise a
        single permission that is the union of all permissions that
        have been added.  */
    private MBeanServerPermission collectionPermission;

    private static final long serialVersionUID = -5661980843569388590L;

    public synchronized void add(Permission permission) {
        if (!(permission instanceof MBeanServerPermission)) {
            final String msg =
                "Permission not an MBeanServerPermission: " + permission;
            throw new IllegalArgumentException(msg);
        }
        if (isReadOnly())
            throw new SecurityException("Read-only permission collection");
        MBeanServerPermission mbsp = (MBeanServerPermission) permission;
        if (collectionPermission == null)
            collectionPermission = mbsp;
        else if (!collectionPermission.implies(permission)) {
            int newmask = collectionPermission.mask | mbsp.mask;
            collectionPermission = new MBeanServerPermission(newmask);
        }
    }

    public synchronized boolean implies(Permission permission) {
        return (collectionPermission != null &&
                collectionPermission.implies(permission));
    }

    public synchronized Enumeration<Permission> elements() {
        Set<Permission> set;
        if (collectionPermission == null)
            set = Collections.emptySet();
        else
            set = Collections.singleton((Permission) collectionPermission);
        return Collections.enumeration(set);
    }
}
