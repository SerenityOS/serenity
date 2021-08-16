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
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.VMDeathRequest;

/**
 * Notification of target VM termination.
 * This event occurs if the target VM terminates before the
 * VM disconnects ({@link VMDisconnectEvent}).
 * Thus, this event will NOT occur if external forces terminate
 * the connection (e.g. a crash) or if the connection is intentionally
 * terminated with {@link VirtualMachine#dispose() VirtualMachine.dispose()}
 * <P>
 * On VM termination, a single unsolicited VMDeathEvent will always be sent with a
 * {@link EventRequest#suspendPolicy() suspend policy}
 * of {@link EventRequest#SUSPEND_NONE SUSPEND_NONE}.
 * Additional VMDeathEvents will be sent in the same event set if they are
 * requested with a {@link VMDeathRequest VMDeathRequest}.
 * <P>
 * The VM is still intact and can be queried at the point this
 * event was initiated but immediately thereafter it is not
 * considered intact and cannot be queried.
 * Note: If the enclosing {@link EventSet} has a
 * {@link EventRequest#suspendPolicy() suspend policy} other than
 * {@link EventRequest#SUSPEND_ALL SUSPEND_ALL}
 * the initiating point may be long past.
 * <P>
 * All VMDeathEvents will be in a single {@link EventSet},
 * no other events will be in the event set. A resume
 * must occur to continue execution after any event set which
 * performs suspensions - in this case to allow proper shutdown.
 *
 * @see VMDisconnectEvent
 * @see EventRequestManager#createVMDeathRequest
 * @see VMDeathRequest
 * @see EventQueue
 * @see VirtualMachine
 *
 * @author Robert Field
 * @since  1.3
 */
public interface VMDeathEvent extends Event {
}
