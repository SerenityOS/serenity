/*
 * Copyright (c) 1997, 2002, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

/**
 * An interface for events that know how to dispatch themselves.
 * By implementing this interface an event can be placed upon the event
 * queue and its {@code dispatch()} method will be called when the event
 * is dispatched, using the {@code EventDispatchThread}.
 * <p>
 * This is a very useful mechanism for avoiding deadlocks. If
 * a thread is executing in a critical section (i.e., it has entered
 * one or more monitors), calling other synchronized code may
 * cause deadlocks. To avoid the potential deadlocks, an
 * {@code ActiveEvent} can be created to run the second section of
 * code at later time. If there is contention on the monitor,
 * the second thread will simply block until the first thread
 * has finished its work and exited its monitors.
 * <p>
 * For security reasons, it is often desirable to use an {@code ActiveEvent}
 * to avoid calling untrusted code from a critical thread. For
 * instance, peer implementations can use this facility to avoid
 * making calls into user code from a system thread. Doing so avoids
 * potential deadlocks and denial-of-service attacks.
 *
 * @author  Timothy Prinzing
 * @since   1.2
 */
public interface ActiveEvent {

    /**
     * Dispatch the event to its target, listeners of the events source,
     * or do whatever it is this event is supposed to do.
     */
    public void dispatch();
}
