/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.event;

import javax.naming.Binding;

/**
  * This class represents an event fired by a naming/directory service.
  *<p>
  * The {@code NamingEvent}'s state consists of
  * <ul>
  * <li>The event source: the {@code EventContext} which fired this event.
  * <li>The event type.
  * <li>The new binding: information about the object after the change.
  * <li>The old binding: information about the object before the change.
  * <li>Change information: information about the change
  * that triggered this event; usually service provider-specific or server-specific
  * information.
  * </ul>
  * <p>
  * Note that the event source is always the same {@code EventContext}
  * <em>instance</em>  that the listener has registered with.
  * Furthermore, the names of the bindings in
  * the {@code NamingEvent} are always relative to that instance.
  * For example, suppose a listener makes the following registration:
  *<blockquote><pre>
  *     NamespaceChangeListener listener = ...;
  *     src.addNamingListener("x", SUBTREE_SCOPE, listener);
  *</pre></blockquote>
  * When an object named "x/y" is subsequently deleted, the corresponding
  * {@code NamingEvent} ({@code evt}) must contain:
  *<blockquote><pre>
  *     evt.getEventContext() == src
  *     evt.getOldBinding().getName().equals("x/y")
  *</pre></blockquote>
  *
  * Care must be taken when multiple threads are accessing the same
  * {@code EventContext} concurrently.
  * See the
  * <a href=package-summary.html#THREADING>package description</a>
  * for more information on threading issues.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see NamingListener
  * @see EventContext
  * @since 1.3
  */
public class NamingEvent extends java.util.EventObject {
    /**
     * Naming event type for indicating that a new object has been added.
     * The value of this constant is {@code 0}.
     */
    public static final int OBJECT_ADDED = 0;

    /**
     * Naming event type for indicating that an object has been removed.
     * The value of this constant is {@code 1}.
     */
    public static final int OBJECT_REMOVED = 1;

    /**
     * Naming event type for indicating that an object has been renamed.
     * Note that some services might fire multiple events for a single
     * logical rename operation. For example, the rename operation might
     * be implemented by adding a binding with the new name and removing
     * the old binding.
     *<p>
     * The old/new binding in {@code NamingEvent} may be null if the old
     * name or new name is outside of the scope for which the listener
     * has registered.
     *<p>
     * When an interior node in the namespace tree has been renamed, the
     * topmost node which is part of the listener's scope should used to generate
     * a rename event. The extent to which this can be supported is
     * provider-specific. For example, a service might generate rename
     * notifications for all descendants of the changed interior node and the
     * corresponding provider might not be able to prevent those
     * notifications from being propagated to the listeners.
     *<p>
     * The value of this constant is {@code 2}.
     */
    public static final int OBJECT_RENAMED = 2;

    /**
     * Naming event type for indicating that an object has been changed.
     * The changes might include the object's attributes, or the object itself.
     * Note that some services might fire multiple events for a single
     * modification. For example, the modification might
     * be implemented by first removing the old binding and adding
     * a new binding containing the same name but a different object.
     *<p>
     * The value of this constant is {@code 3}.
     */
    public static final int OBJECT_CHANGED = 3;

    /**
     * Contains information about the change that generated this event.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected Object changeInfo;

    /**
     * Contains the type of this event.
     * @see #OBJECT_ADDED
     * @see #OBJECT_REMOVED
     * @see #OBJECT_RENAMED
     * @see #OBJECT_CHANGED
     * @serial
     */
    protected int type;

    /**
     * Contains information about the object before the change.
     * @serial
     */
    protected Binding oldBinding;

    /**
     * Contains information about the object after the change.
     * @serial
     */
    protected Binding newBinding;

    /**
     * Constructs an instance of {@code NamingEvent}.
     *<p>
     * The names in {@code newBd} and {@code oldBd} are to be resolved relative
     * to the event source {@code source}.
     *
     * For an {@code OBJECT_ADDED} event type, {@code newBd} must not be null.
     * For an {@code OBJECT_REMOVED} event type, {@code oldBd} must not be null.
     * For an {@code OBJECT_CHANGED} event type,  {@code newBd} and
     * {@code oldBd} must not be null. For  an {@code OBJECT_RENAMED} event type,
     * one of {@code newBd} or {@code oldBd} may be null if the new or old
     * binding is outside of the scope for which the listener has registered.
     *
     * @param source The non-null context that fired this event.
     * @param type The type of the event.
     * @param newBd A possibly null binding before the change. See method description.
     * @param oldBd A possibly null binding after the change. See method description.
     * @param changeInfo A possibly null object containing information about the change.
     * @see #OBJECT_ADDED
     * @see #OBJECT_REMOVED
     * @see #OBJECT_RENAMED
     * @see #OBJECT_CHANGED
     */
    public NamingEvent(EventContext source, int type,
        Binding newBd, Binding oldBd, Object changeInfo) {
        super(source);
        this.type = type;
        oldBinding = oldBd;
        newBinding = newBd;
        this.changeInfo = changeInfo;
    }

    /**
     * Returns the type of this event.
     * @return The type of this event.
     * @see #OBJECT_ADDED
     * @see #OBJECT_REMOVED
     * @see #OBJECT_RENAMED
     * @see #OBJECT_CHANGED
     */
    public int getType() {
        return type;
    }

    /**
     * Retrieves the event source that fired this event.
     * This returns the same object as {@code EventObject.getSource()}.
     *<p>
     * If the result of this method is used to access the
     * event source, for example, to look up the object or get its attributes,
     * then it needs to be locked  because implementations of {@code Context}
     * are not guaranteed to be thread-safe
     * (and {@code EventContext} is a subinterface of {@code Context}).
     * See the
     * <a href=package-summary.html#THREADING>package description</a>
     * for more information on threading issues.
     *
     * @return The non-null context that fired this event.
     */
    public EventContext getEventContext() {
        return (EventContext)getSource();
    }

    /**
     * Retrieves the binding of the object before the change.
     *<p>
     * The binding must be nonnull if the object existed before the change
     * relative to the source context ({@code getEventContext()}).
     * That is, it must be nonnull for {@code OBJECT_REMOVED} and
     * {@code OBJECT_CHANGED}.
     * For {@code OBJECT_RENAMED}, it is null if the object before the rename
     * is outside of the scope for which the listener has registered interest;
     * it is nonnull if the object is inside the scope before the rename.
     *<p>
     * The name in the binding is to be resolved relative
     * to the event source {@code getEventContext()}.
     * The object returned by {@code Binding.getObject()} may be null if
     * such information is unavailable.
     *
     * @return The possibly null binding of the object before the change.
     */
    public Binding getOldBinding() {
        return oldBinding;
    }

    /**
     * Retrieves the binding of the object after the change.
     *<p>
     * The binding must be nonnull if the object existed after the change
     * relative to the source context ({@code getEventContext()}).
     * That is, it must be nonnull for {@code OBJECT_ADDED} and
     * {@code OBJECT_CHANGED}. For {@code OBJECT_RENAMED},
     * it is null if the object after the rename is outside the scope for
     * which the listener registered interest; it is nonnull if the object
     * is inside the scope after the rename.
     *<p>
     * The name in the binding is to be resolved relative
     * to the event source {@code getEventContext()}.
     * The object returned by {@code Binding.getObject()} may be null if
     * such information is unavailable.
     *
     * @return The possibly null binding of the object after the change.
     */
    public Binding getNewBinding() {
        return newBinding;
    }

    /**
     * Retrieves the change information for this event.
     * The value of the change information is service-specific. For example,
     * it could be an ID that identifies the change in a change log on the server.
     *
     * @return The possibly null change information of this event.
     */
    public Object getChangeInfo() {
        return changeInfo;
    }

    /**
     * Invokes the appropriate listener method on this event.
     * The default implementation of
     * this method handles the following event types:
     * {@code OBJECT_ADDED, OBJECT_REMOVED,
     * OBJECT_RENAMED, OBJECT_CHANGED}.
     *<p>
     * The listener method is executed in the same thread
     * as this method.  See the
     * <a href=package-summary.html#THREADING>package description</a>
     * for more information on threading issues.
     * @param listener The nonnull listener.
     */
    public void dispatch(NamingListener listener) {
        switch (type) {
        case OBJECT_ADDED:
            ((NamespaceChangeListener)listener).objectAdded(this);
            break;

        case OBJECT_REMOVED:
            ((NamespaceChangeListener)listener).objectRemoved(this);
            break;

        case OBJECT_RENAMED:
            ((NamespaceChangeListener)listener).objectRenamed(this);
            break;

        case OBJECT_CHANGED:
            ((ObjectChangeListener)listener).objectChanged(this);
            break;
        }
    }
    private static final long serialVersionUID = -7126752885365133499L;
}
