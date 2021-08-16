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

package java.beans.beancontext;

import java.io.Serial;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * A {@code BeanContextMembershipEvent} encapsulates
 * the list of children added to, or removed from,
 * the membership of a particular {@code BeanContext}.
 * An instance of this event is fired whenever a successful
 * add(), remove(), retainAll(), removeAll(), or clear() is
 * invoked on a given {@code BeanContext} instance.
 * Objects interested in receiving events of this type must
 * implement the {@code BeanContextMembershipListener}
 * interface, and must register their intent via the
 * {@code BeanContext}'s
 * {@code addBeanContextMembershipListener(BeanContextMembershipListener bcml)}
 * method.
 *
 * @author      Laurence P. G. Cable
 * @since       1.2
 * @see         java.beans.beancontext.BeanContext
 * @see         java.beans.beancontext.BeanContextEvent
 * @see         java.beans.beancontext.BeanContextMembershipListener
 */
public class BeanContextMembershipEvent extends BeanContextEvent {

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 3499346510334590959L;

    /**
     * Contruct a BeanContextMembershipEvent
     *
     * @param bc        The BeanContext source
     * @param changes   The Children affected
     * @throws NullPointerException if {@code changes} is {@code null}
     */

    @SuppressWarnings("rawtypes")
    public BeanContextMembershipEvent(BeanContext bc, Collection changes) {
        super(bc);

        if (changes == null) throw new NullPointerException(
            "BeanContextMembershipEvent constructor:  changes is null.");

        children = changes;
    }

    /**
     * Contruct a BeanContextMembershipEvent
     *
     * @param bc        The BeanContext source
     * @param changes   The Children effected
     * @exception       NullPointerException if changes associated with this
     *                  event are null.
     */

    public BeanContextMembershipEvent(BeanContext bc, Object[] changes) {
        super(bc);

        if (changes == null) throw new NullPointerException(
            "BeanContextMembershipEvent:  changes is null.");

        children = Arrays.asList(changes);
    }

    /**
     * Gets the number of children affected by the notification.
     * @return the number of children affected by the notification
     */
    public int size() { return children.size(); }

    /**
     * Is the child specified affected by the event?
     * @return {@code true} if affected, {@code false}
     * if not
     * @param child the object to check for being affected
     */
    public boolean contains(Object child) {
        return children.contains(child);
    }

    /**
     * Gets the array of children affected by this event.
     * @return the array of children affected
     */
    public Object[] toArray() { return children.toArray(); }

    /**
     * Gets the array of children affected by this event.
     * @return the array of children effected
     */
    @SuppressWarnings("rawtypes")
    public Iterator iterator() { return children.iterator(); }

    /*
     * fields
     */

   /**
    * The list of children affected by this
    * event notification.
    */
    @SuppressWarnings({"rawtypes",
                       "serial"}) // Not statically typed as Serializable
    protected Collection children;
}
