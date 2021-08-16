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

import java.util.*;
import java.lang.reflect.*;
import java.io.*;

import sun.jvmstat.monitor.*;
import sun.jvmstat.monitor.event.*;
import sun.jvmstat.perfdata.monitor.*;

/**
 * Concrete implementation of the AbstractMonitoredVm class for the
 * <em>local:</em> protocol for the HotSpot PerfData monitoring implementation.
 * <p>
 * This class provides the ability to attach to the instrumentation buffer
 * of a live target Java Virtual Machine through a HotSpot specific attach
 * mechanism.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class LocalMonitoredVm extends AbstractMonitoredVm {

    /**
     * List of registered listeners.
     */
    private ArrayList<VmListener> listeners;

    /**
     * Task performing listener notification.
     */
    private NotifierTask task;

    /**
     * Create a LocalMonitoredVm instance.
     *
     * @param vmid the vm identifier specifying the target JVM
     * @param interval the sampling interval
     */
    public LocalMonitoredVm(VmIdentifier vmid, int interval)
           throws MonitorException {
        super(vmid, interval);
        this.pdb = new PerfDataBuffer(vmid);
        listeners = new ArrayList<VmListener>();
    }

    /**
     * {@inheritDoc}.
     */
    public void detach() {
        if (interval > 0) {
            /*
             * if the notifier task is running, stop it, otherwise it can
             * access non-existent memory once we've detached from the
             * underlying buffer.
             */
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
        super.detach();
    }

    /**
     * {@inheritDoc}.
     */
    public void addVmListener(VmListener l) {
        synchronized(listeners) {
            listeners.add(l);
            if (task == null) {
                task = new NotifierTask();
                LocalEventTimer timer = LocalEventTimer.getInstance();
                timer.schedule(task, interval, interval);
            }
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void removeVmListener(VmListener l) {
        synchronized(listeners) {
            listeners.remove(l);
            if (listeners.isEmpty() && task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    /**
     * {@inheritDoc}.
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
     * Fire MonitoredVmStructureChanged events.
     *
     * @param inserted List of Monitor objects inserted.
     * @param removed List of Monitor objects removed.
     */
    @SuppressWarnings("unchecked") // Cast of result of clone
    void fireMonitorStatusChangedEvents(List<Monitor> inserted, List<Monitor> removed) {
        MonitorStatusChangeEvent ev = null;
        ArrayList<VmListener> registered = null;

        synchronized (listeners) {
            registered = (ArrayList)listeners.clone();
        }

        for (Iterator<VmListener> i = registered.iterator(); i.hasNext(); /* empty */) {
            VmListener l = i.next();
            // lazily create the event object;
            if (ev == null) {
                ev = new MonitorStatusChangeEvent(this, inserted, removed);
            }
            l.monitorStatusChanged(ev);
        }
    }

    /**
     * Fire MonitoredUpdated events.
     */
    void fireMonitorsUpdatedEvents() {
        VmEvent ev = null;
        ArrayList<VmListener> registered = null;

        synchronized (listeners) {
            registered = cast(listeners.clone());
        }

        for (VmListener l :  registered) {
            // lazily create the event object;
            if (ev == null) {
                ev = new VmEvent(this);
            }
            l.monitorsUpdated(ev);
        }
    }

    /**
     * Class to notify listeners of Monitor related events for
     * the target JVM.
     */
    private class NotifierTask extends CountedTimerTask {
        public void run() {
            super.run();
            try {
                MonitorStatus status = getMonitorStatus();
                List<Monitor> inserted = status.getInserted();
                List<Monitor> removed = status.getRemoved();

                if (!inserted.isEmpty() || !removed.isEmpty()) {
                    fireMonitorStatusChangedEvents(inserted, removed);
                }
                fireMonitorsUpdatedEvents();
            } catch (MonitorException e) {
                // XXX: use logging api
                System.err.println("Exception updating monitors for "
                                   + getVmIdentifier());
                e.printStackTrace();
            }
        }
    }
    // Suppress unchecked cast warning msg.
    @SuppressWarnings("unchecked")
    static <T> T cast(Object x) {
        return (T) x;
    }
}
