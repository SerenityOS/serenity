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

package com.sun.jdi.request;

import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ModificationWatchpointEvent;

/**
 * Request for notification when a field is set.
 * This event will be triggered when a value is assigned to the specified
 * field with a Java programming
 * language statement (assignment, increment, etc) or by a
 * Java Native Interface (JNI) set function (<code>Set&lt;Type&gt;Field,
 * SetStatic&lt;Type&gt;Field</code>).
 * Setting a field to a value which is the same as the previous value
 * still triggers this event.
 * Modification by JDI does not trigger this event.
 * When an enabled ModificationWatchpointRequest is satisfied, an
 * {@link EventSet event set} containing a
 * {@link ModificationWatchpointEvent ModificationWatchpointEvent}
 * will be placed on the {@link EventQueue EventQueue}.
 * The collection of existing watchpoints is
 * managed by the {@link EventRequestManager}.
 *
 * @see ModificationWatchpointEvent
 * @see AccessWatchpointRequest
 * @see EventQueue
 * @see EventRequestManager
 *
 * @author Robert Field
 * @since  1.3
 */
public interface ModificationWatchpointRequest extends WatchpointRequest {
}
