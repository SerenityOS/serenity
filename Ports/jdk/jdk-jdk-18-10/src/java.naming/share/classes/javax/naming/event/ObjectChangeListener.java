/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

/**
  * Specifies the method that a listener of a {@code NamingEvent}
  * with event type of {@code OBJECT_CHANGED} must implement.
  *<p>
  * An {@code OBJECT_CHANGED} event type is fired when (the contents of)
  * an object has changed. This might mean that its attributes have been modified,
  * added, or removed, and/or that the object itself has been replaced.
  * How the object has changed can be determined by examining the
  * {@code NamingEvent}'s old and new bindings.
  *<p>
  * A listener interested in {@code OBJECT_CHANGED} event types must:
  *<ol>
  *
  *<li>Implement this interface and its method ({@code objectChanged()})
  *<li>Implement {@code NamingListener.namingExceptionThrown()} so that
  * it will be notified of exceptions thrown while attempting to
  * collect information about the events.
  *<li>Register with the source using the source's {@code addNamingListener()}
  *    method.
  *</ol>
  * A listener that wants to be notified of namespace change events
  * should also implement the {@code NamespaceChangeListener}
  * interface.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see NamingEvent
  * @see NamespaceChangeListener
  * @see EventContext
  * @see EventDirContext
  * @since 1.3
  */
public interface ObjectChangeListener extends NamingListener {

    /**
     * Called when an object has been changed.
     *<p>
     * The binding of the changed object can be obtained using
     * {@code evt.getNewBinding()}. Its old binding (before the change)
     * can be obtained using {@code evt.getOldBinding()}.
     * @param evt The nonnull naming event.
     * @see NamingEvent#OBJECT_CHANGED
     */
    void objectChanged(NamingEvent evt);
}
