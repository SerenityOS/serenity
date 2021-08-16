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

import com.sun.jdi.Field;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

/**
 * Notification of a field triggered event encountered by a thread in the
 * target VM.
 *
 * @see EventQueue
 * @see VirtualMachine
 *
 * @author Robert Field
 * @since  1.3
 */
public interface WatchpointEvent extends LocatableEvent {

    /**
     * Returns the field that is about to be accessed/modified.
     *
     * @return a {@link Field} which mirrors the field
     * in the target VM.
     * @throws ObjectCollectedException may be thrown if class
     * has been garbage collected.
     */
    Field field();

    /**
     * Returns the object whose field is about to be accessed/modified.
     * Return null is the access is to a static field.
     *
     * @return a {@link ObjectReference} which mirrors the event's
     * object in the target VM.
     */
    ObjectReference object();

    /**
     * Current value of the field.
     * @throws ObjectCollectedException if object or class have been
     * garbage collected.
     */
    Value valueCurrent();
}
