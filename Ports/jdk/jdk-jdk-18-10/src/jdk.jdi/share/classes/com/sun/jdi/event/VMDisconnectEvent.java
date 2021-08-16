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

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;

/**
 * Notification of disconnection from target VM.
 * May be caused by normal termination of a VM,
 * VM termination by uncaught exception or other error,
 * debugger action ({@link VirtualMachine#dispose} or
 * {@link VirtualMachine#exit}) or by external events
 * (for example, target process termination by the
 * operating system, transport termination, etc).
 * <p>
 * If the target VM terminates before the disconnection, this event
 * will be preceded by a {@link VMDeathEvent}.
 * <p>
 * This event is always sent.
 * There is no corresponding {@link EventRequest}.
 * The enclosing singleton {@link EventSet} always has a
 * suspend policy of {@link EventRequest#SUSPEND_NONE}.
 *
 * @see VMDeathEvent
 * @see EventQueue
 * @see VirtualMachine
 *
 * @author Robert Field
 * @since  1.3
 */
public interface VMDisconnectEvent extends Event {
}
