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

/**
 * A GuardedObject is an object that is used to protect access to
 * another object.
 *
 * <p>A GuardedObject encapsulates a target object and a Guard object,
 * such that access to the target object is possible
 * only if the Guard object allows it.
 * Once an object is encapsulated by a GuardedObject,
 * access to that object is controlled by the {@code getObject}
 * method, which invokes the
 * {@code checkGuard} method on the Guard object that is
 * guarding access. If access is not allowed,
 * an exception is thrown.
 *
 * @see Guard
 * @see Permission
 *
 * @author Roland Schemers
 * @author Li Gong
 * @since 1.2
 */

public class GuardedObject implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = -5240450096227834308L;

    /**
     * The object we are guarding.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object object;

    /**
     * The guard object.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Guard guard;

    /**
     * Constructs a GuardedObject using the specified object and guard.
     * If the Guard object is null, then no restrictions will
     * be placed on who can access the object.
     *
     * @param object the object to be guarded.
     *
     * @param guard the Guard object that guards access to the object.
     */

    public GuardedObject(Object object, Guard guard)
    {
        this.guard = guard;
        this.object = object;
    }

    /**
     * Retrieves the guarded object, or throws an exception if access
     * to the guarded object is denied by the guard.
     *
     * @return the guarded object.
     *
     * @throws    SecurityException if access to the guarded object is
     * denied.
     */
    public Object getObject()
        throws SecurityException
    {
        if (guard != null)
            guard.checkGuard(object);

        return object;
    }

    /**
     * Writes this object out to a stream (i.e., serializes it).
     * We check the guard if there is one.
     *
     * @param  oos the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        if (guard != null)
            guard.checkGuard(object);

        oos.defaultWriteObject();
    }
}
