/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi.event;

import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.request.ExceptionRequest;

/**
 * Notification of an exception in the target VM. When an exception
 * is thrown which satisfies a currently enabled
 * {@link ExceptionRequest exception request},
 * an {@link EventSet event set}
 * containing an instance of this class will be added
 * to the VM's event queue.
 * If the exception is thrown from a non-native method,
 * the exception event is generated at the location where the
 * exception is thrown.
 * If the exception is thrown from a native method, the exception event
 * is generated at the first non-native location reached after the exception
 * is thrown.
 *
 * @author Robert Field
 * @since  1.3
 */
public interface ExceptionEvent extends LocatableEvent {

    /**
     * Gets the thrown exception object. The exception object is
     * an instance of {@link java.lang.Throwable} or a subclass in the
     * target VM.
     *
     * @return an {@link ObjectReference} which mirrors the thrown object in
     * the target VM.
     */
    public ObjectReference exception();

    /**
     * Gets the location where the exception will be caught. An exception
     * is considered to be caught if, at the point of the throw, the
     * current location is dynamically enclosed in a try statement that
     * handles the exception. (See the JVM specification for details).
     * If there is such a try statement, the catch location is the
     * first code index of the appropriate catch clause.
     * <p>
     * If there are native methods in the call stack at the time of the
     * exception, there are important restrictions to note about the
     * returned catch location. In such cases,
     * it is not possible to predict whether an exception will be handled
     * by some native method on the call stack.
     * Thus, it is possible that exceptions considered uncaught
     * here will, in fact, be handled by a native method and not cause
     * termination of the target VM. Furthermore, it cannot be assumed that the
     * catch location returned here will ever be reached by the throwing
     * thread. If there is
     * a native frame between the current location and the catch location,
     * the exception might be handled and cleared in that native method
     * instead.
     * <p>
     * Note that the compiler can generate try-catch blocks in some cases
     * where they are not explicit in the source code; for example,
     * the code generated for <code>synchronized</code> and
     * <code>finally</code> blocks can contain implicit try-catch blocks.
     * If such an implicitly generated try-catch is
     * present on the call stack at the time of the throw, the exception
     * will be considered caught even though it appears to be uncaught from
     * examination of the source code.
     *
     * @return the {@link Location} where the exception will be caught or null if
     * the exception is uncaught.
     */
    public Location catchLocation();
}
