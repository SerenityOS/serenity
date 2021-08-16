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

package sun.jvmstat.perfdata.monitor.protocol.local;

import sun.jvmstat.monitor.*;
import sun.jvmstat.monitor.event.*;
import sun.jvmstat.perfdata.monitor.*;
import java.util.*;
import java.net.*;

/**
 * Concrete implementation of the MonitoredHost interface for the
 * <em>local</em> protocol of the HotSpot PerfData monitoring implementation.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class MonitoredHostProvider extends MonitoredHost {
    private static final int DEFAULT_POLLING_INTERVAL = 1000;

    private ArrayList<HostListener> listeners;
    private NotifierTask task;
    private HashSet<Integer> activeVms;
    private LocalVmManager vmManager;

    /**
     * Create a MonitoredHostProvider instance using the given HostIdentifier.
     *
     * @param hostId the host identifier for this MonitoredHost
     */
    public MonitoredHostProvider(HostIdentifier hostId) {
        this.hostId = hostId;
        this.listeners = new ArrayList<HostListener>();
        this.interval = DEFAULT_POLLING_INTERVAL;
        this.activeVms = new HashSet<Integer>();
        this.vmManager = new LocalVmManager();
    }

    /**
     * {@inheritDoc}
     */
    public MonitoredVm getMonitoredVm(VmIdentifier vmid)
                       throws MonitorException {
        return getMonitoredVm(vmid, DEFAULT_POLLING_INTERVAL);
    }

    /**
     * {@inheritDoc}
     */
    public MonitoredVm getMonitoredVm(VmIdentifier vmid, int interval)
                       throws MonitorException {
        try {
            VmIdentifier nvmid = hostId.resolve(vmid);
            return new LocalMonitoredVm(nvmid, interval);
        } catch (URISyntaxException e) {
            /*
             * the VmIdentifier is expected to be a valid and it should
             * resolve reasonably against the host identifier. A
             * URISyntaxException here is most likely a programming error.
             */
            throw new IllegalArgumentException("Malformed URI: "
                                               + vmid.toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void detach(MonitoredVm vm) {
        vm.detach();
    }

    /**
     * {@inheritDoc}
     */
    public void addHostListener(HostListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
            if (task == null) {
                task = new NotifierTask();
                LocalEventTimer timer = LocalEventTimer.getInstance();
                timer.schedule(task, interval, interval);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeHostListener(HostListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
            if (listeners.isEmpty() && (task != null)) {
                task.cancel();
                task = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setInterval(int newInterval) {
        synchronized(listeners) {
            if (newInterval == interval) {
                return;
            }

            int oldInterval = interval;
            super.setInterval(newInterval);

            if (task != null) {
                task.cancel();
                NotifierTask oldTask = task;
                task = new NotifierTask();
                LocalEventTimer timer = LocalEventTimer.getInstance();
                CountedTimerTaskUtils.reschedule(timer, oldTask, task,
                                                 oldInterval, newInterval);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> activeVms() {
        return vmManager.activeVms();
    }

    /**
     * Fire VmEvent events.
     *
     * @param active a set of Integer objects containing the vmid of
     *               the active Vms
     * @param started a set of Integer objects containing the vmid of
     *                new Vms started since last interval.
     * @param terminated a set of Integer objects containing the vmid of
     *                   terminated Vms since last interval.
     */
    @SuppressWarnings("unchecked") // Cast of result of clone
    private void fireVmStatusChangedEvents(Set<Integer> active, Set<Integer> started,
                                           Set<Integer> terminated) {
        ArrayList<HostListener> registered = null;
        VmStatusChangeEvent ev = null;

        synchronized(listeners) {
            registered = (ArrayList)listeners.clone();
        }

        for (Iterator<HostListener> i = registered.iterator(); i.hasNext(); /* empty */) {
            HostListener l = i.next();
            if (ev == null) {
                ev = new VmStatusChangeEvent(this, active, started, terminated);
            }
            l.vmStatusChanged(ev);
        }
    }

    /**
     * Class to poll the local system and generate event notifications.
     */
    private class NotifierTask extends CountedTimerTask {
        public void run() {
            super.run();

            // save the last set of active JVMs
            Set<Integer> lastActiveVms = activeVms;

            // get the current set of active JVMs
            activeVms = (HashSet<Integer>)vmManager.activeVms();

            if (activeVms.isEmpty()) {
                return;
            }
            Set<Integer> startedVms = new HashSet<>();
            Set<Integer> terminatedVms = new HashSet<>();

            for (Iterator<Integer> i = activeVms.iterator(); i.hasNext(); /* empty */) {
                Integer vmid = i.next();
                if (!lastActiveVms.contains(vmid)) {
                    // a new file has been detected, add to set
                    startedVms.add(vmid);
                }
            }

            for (Iterator<Integer> i = lastActiveVms.iterator(); i.hasNext();
                    /* empty */) {
                Integer o = i.next();
                if (!activeVms.contains(o)) {
                    // JVM has terminated, remove it from the active list
                    terminatedVms.add(o);
                }
            }

            if (!startedVms.isEmpty() || !terminatedVms.isEmpty()) {
                fireVmStatusChangedEvents(activeVms, startedVms,
                                          terminatedVms);
            }
        }
    }
}
