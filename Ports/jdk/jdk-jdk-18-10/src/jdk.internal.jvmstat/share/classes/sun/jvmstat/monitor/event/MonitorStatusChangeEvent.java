/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.monitor.event;

import java.util.List;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.Monitor;

/**
 * Provides a description of a change in status of the instrumentation
 * exported by the MonitoredVm.
 *
 * @author Brian Doherty
 * @since 1.5
 */
@SuppressWarnings("serial") // JDK implementation class
public class MonitorStatusChangeEvent extends VmEvent {

    /**
     * List of instrumentation objects inserted since the last event.
     * Elements of this list will always be of type Monitor.
     */
    protected List<Monitor> inserted;

    /**
     * List of instrumentation objects removed since the last event.
     * Elements of this list will always be of type Monitor.
     */
    protected List<Monitor> removed;

    /**
     * Construct a new MonitorStatusChangeEvent.
     *
     * @param vm the MonitoredVm source of the event.
     * @param inserted the list of instrumentation objects inserted since
     *                 the last event.
     * @param removed the list of instrumentation objects removed since
     *                the last event.
     */
    public MonitorStatusChangeEvent(MonitoredVm vm, List<Monitor> inserted,
                                    List<Monitor> removed) {
        super(vm);
        this.inserted = inserted;
        this.removed = removed;
    }

    /**
     * Return the list of instrumentation objects that were inserted
     * since the last event notification.
     *
     * @return List - a List of Monitor objects that were inserted into the
     *                instrumentation exported by the MonitoredHost. If no
     *                new instrumentation was inserted, an emply List is
     *                returned.
     */
    public List<Monitor> getInserted() {
        return inserted;
    }

    /**
     * Return the set of instrumentation objects that were removed
     * since the last event notification.
     *
     * @return List - a List of Monitor objects that were removed from the
     *                instrumentation exported by the MonitoredHost. If no
     *                instrumentation was removed, an emply List is returned.
     */
    public List<Monitor> getRemoved() {
        return removed;
    }
}
