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

import javax.naming.NamingException;

/**
  * This class represents an event fired when the procedures/processes
  * used to collect information for notifying listeners of
  * {@code NamingEvent}s threw a {@code NamingException}.
  * This can happen, for example, if the server which the listener is using
  * aborts subsequent to the {@code addNamingListener()} call.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see NamingListener#namingExceptionThrown
  * @see EventContext
  * @since 1.3
  */

public class NamingExceptionEvent extends java.util.EventObject {
    /**
     * Contains the exception that was thrown
     * @serial
     */
    private NamingException exception;

    /**
     * Constructs an instance of {@code NamingExceptionEvent} using
     * the context in which the {@code NamingException} was thrown and the exception
     * that was thrown.
     *
     * @param source The non-null context in which the exception was thrown.
     * @param exc    The non-null {@code NamingException} that was thrown.
     *
     */
    public NamingExceptionEvent(EventContext source, NamingException exc) {
        super(source);
        exception = exc;
    }

    /**
     * Retrieves the exception that was thrown.
     * @return The exception that was thrown.
     */
    public NamingException getException() {
        return exception;
    }

    /**
     * Retrieves the {@code EventContext} that fired this event.
     * This returns the same object as {@code EventObject.getSource()}.
     * @return The non-null {@code EventContext} that fired this event.
     */
    public EventContext getEventContext() {
        return (EventContext)getSource();
    }

    /**
     * Invokes the {@code namingExceptionThrown()} method on
     * a listener using this event.
     * @param listener The non-null naming listener on which to invoke
     * the method.
     */
    public void dispatch(NamingListener listener) {
        listener.namingExceptionThrown(this);
    }

    private static final long serialVersionUID = -4877678086134736336L;
}
