/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.transport;

import java.rmi.server.UID;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sun.rmi.runtime.RuntimeUtil;

/**
 * Holds strong references to a set of remote objects, or live remote
 * references to remote objects, after they have been marshalled (as
 * remote references) as parts of the arguments or the result of a
 * remote invocation.  The purpose is to prevent remote objects or
 * live remote references that might otherwise be determined to be
 * unreachable in this VM from being locally garbage collected before
 * the receiver has had an opportunity to register the unmarshalled
 * remote references for DGC.
 *
 * The references are held strongly until an acknowledgment has been
 * received that the receiver has had an opportunity to process the
 * remote references or until a timeout has expired.  For remote
 * references sent as parts of the arguments of a remote invocation,
 * the acknowledgment is the beginning of the response indicating
 * completion of the remote invocation.  For remote references sent as
 * parts of the result of a remote invocation, a UID is included as
 * part of the result, and the acknowledgment is a transport-level
 * "DGCAck" message containing that UID.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 **/
public class DGCAckHandler {

    /** timeout for holding references without receiving an acknowledgment */
    @SuppressWarnings("removal")
    private static final long dgcAckTimeout =           // default 5 minutes
        AccessController.doPrivileged((PrivilegedAction<Long>) () ->
            Long.getLong("sun.rmi.dgc.ackTimeout", 300000));

    /** thread pool for scheduling delayed tasks */
    @SuppressWarnings("removal")
    private static final ScheduledExecutorService scheduler =
        AccessController.doPrivileged(
            new RuntimeUtil.GetInstanceAction()).getScheduler();

    /** table mapping ack ID to handler */
    private static final Map<UID,DGCAckHandler> idTable =
        Collections.synchronizedMap(new HashMap<UID,DGCAckHandler>());

    private final UID id;
    private List<Object> objList = new ArrayList<>(); // null if released
    private Future<?> task = null;

    /**
     * Creates a new DGCAckHandler, associated with the specified UID
     * if the argument is not null.
     *
     * References added to this DGCAckHandler will be held strongly
     * until its "release" method is invoked or (after the
     * "startTimer" method has been invoked) the timeout has expired.
     * If the argument is not null, then invoking the static
     * "received" method with the specified UID is equivalent to
     * invoking this instance's "release" method.
     **/
    DGCAckHandler(UID id) {
        this.id = id;
        if (id != null) {
            assert !idTable.containsKey(id);
            idTable.put(id, this);
        }
    }

    /**
     * Adds the specified reference to this DGCAckHandler.
     **/
    synchronized void add(Object obj) {
        if (objList != null) {
            objList.add(obj);
        }
    }

    /**
     * Starts the timer for this DGCAckHandler.  After the timeout has
     * expired, the references are released even if the acknowledgment
     * has not been received.
     **/
    synchronized void startTimer() {
        if (objList != null && task == null) {
            task = scheduler.schedule(new Runnable() {
                public void run() {
                    if (id != null) {
                        idTable.remove(id);
                    }
                    release();
                }
            }, dgcAckTimeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Releases the references held by this DGCAckHandler.
     **/
    synchronized void release() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        objList = null;
    }

    /**
     * Causes the DGCAckHandler associated with the specified UID to
     * release its references.
     **/
    public static void received(UID id) {
        DGCAckHandler h = idTable.remove(id);
        if (h != null) {
            h.release();
        }
    }
}
