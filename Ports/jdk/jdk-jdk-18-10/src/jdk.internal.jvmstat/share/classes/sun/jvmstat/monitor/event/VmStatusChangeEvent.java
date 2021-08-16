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

import java.util.Set;
import sun.jvmstat.monitor.MonitoredHost;

/**
 * Provides a description of a change in status of the Java Virtual Machines
 * associated with a MonitoredHost.
 *
 * @author Brian Doherty
 * @since 1.5
 */
@SuppressWarnings("serial") // JDK implementation class
public class VmStatusChangeEvent extends HostEvent {

    /**
     * The set of currently active Java Virtual Machines for the MonitoredHost.
     * The set contains an Integer object holding the <em>lvmid</em> for each
     * active Java Virtual Machine on the MonitoredHost. This Set will only
     * contain Integer objects.
     */
    protected Set<Integer> active;

    /**
     * The set of Java Virtual Machines started on MonitoredHost since the
     * previous event. The set contains an Integer object holding the
     * <em>lvmid</em> for each Java Virtual Machine started on the
     * MonitoredHost. This Set will only contain Integer objects.
     */
    protected Set<Integer> started;

    /**
     * The set of Java Virtual Machines terminated on MonitoredHost since the
     * previous event. The set contains an Integer object holding the
     * <em>lvmid</em> for each Java Virtual Machine started on the
     * MonitoredHost. This Set will only contain Integer objects.
     */
    protected Set<Integer> terminated;

    /**
     * Construct a new VmStatusChangeEvent instance.
     *
     * @param host the MonitoredHost that is the source of the event.
     * @param active the set of currently active Java Virtual Machines
     * @param started the set of Java Virtual Machines started since the
     *                last event.
     * @param terminated the set of Java Virtual Machines terminated since
     *                   the last event.
     */
    public VmStatusChangeEvent(MonitoredHost host, Set<Integer> active,
                               Set<Integer> started, Set<Integer> terminated) {
        super(host);
        this.active = active;
        this.started = started;
        this.terminated = terminated;
    }

    /**
     * Return the set of currently active Java Virtual Machines.
     * The set contains an Integer object holding the <em>lvmid</em> for each
     * active Java Virtual Machine on the MonitoredHost.
     *
     * @return Set - a set of Integer objects containing the <em>lvmid</em>
     *               of each active Java Virtual Machine on the host. If
     *               there are no active Java Virtual Machines on the host,
     *               an empty Set is returned.
     */
    public Set<Integer> getActive() {
        return active;
    }

    /**
     * Return the set of Java Virtual Machines started since the last
     * event notification. The set contains an Integer object holding
     * the <em>lvmid</em> for each Java Virtual Machine started on the
     * MonitoredHost since the last event notification.
     *
     * @return Set - a set of Integer objects containing the <em>lvmid</em>
     *               of each Java Virtual Machine started on the host. If
     *               no Java Virtual Machines were recently started on the
     *               host, an empty Set is returned.
     */
    public Set<Integer> getStarted() {
        return started;
    }

    /**
     * Return the set of Java Virtual Machines terminated since the last
     * event notification. The set contains an Integer object holding
     * the <em>lvmid</em> for each Java Virtual Machine terminated on the
     * MonitoredHost since the last event notification.
     *
     * @return Set - a set of Integer objects containing the <em>lvmid</em>
     *               of each Java Virtual Machine terminated on the host. If
     *               no Java Virtual Machines were recently terminated on the
     *               host, an empty Set is returned.
     */
    public Set<Integer> getTerminated() {
        return terminated;
    }
}
