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

package sun.jvmstat.perfdata.monitor.protocol.rmi;

import sun.jvmstat.monitor.*;
import sun.jvmstat.monitor.event.*;
import sun.jvmstat.monitor.remote.*;
import sun.jvmstat.perfdata.monitor.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.rmi.*;

/**
 * Concrete implementation of the AbstractMonitoredVm class for the
 * <em>rmi:</em> protocol for the HotSpot PerfData monitoring implementation.
 * <p>
 * This class provides the ability to acquire to the instrumentation buffer
 * of a live, remote target Java Virtual Machine through an RMI server.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class RemoteMonitoredVm extends AbstractMonitoredVm {

    private ArrayList<VmListener> listeners;
    private NotifierTask notifierTask;
    private SamplerTask samplerTask;
    private Timer timer;

    private RemoteVm rvm;
    private ByteBuffer updateBuffer;

    /**
     * Create a RemoteMonitoredVm instance.
     *
     * @param rvm the proxy to the remote MonitoredVm instance.
     * @param vmid the vm identifier specifying the remot target JVM
     * @param timer the timer used to run polling tasks
     * @param interval the sampling interval
     */
    public RemoteMonitoredVm(RemoteVm rvm, VmIdentifier vmid,
                             Timer timer, int interval)
           throws MonitorException {
        super(vmid, interval);
        this.rvm = rvm;
        pdb = new PerfDataBuffer(rvm, vmid.getLocalVmId());
        this.listeners = new ArrayList<VmListener>();
        this.timer = timer;
    }

    /**
     * Method to attach to the remote MonitoredVm.
     */
    public void attach() throws MonitorException {
        updateBuffer = pdb.getByteBuffer().duplicate();

        // if continuous sampling is requested, register with the sampler thread
        if (interval > 0) {
            samplerTask = new SamplerTask();
            timer.schedule(samplerTask, 0, interval);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void detach() {
        try {
            if (interval > 0) {
                if (samplerTask != null) {
                    samplerTask.cancel();
                    samplerTask = null;
                }
                if (notifierTask != null) {
                    notifierTask.cancel();
                    notifierTask = null;
                }
                sample();
            }
        } catch (RemoteException e) {
            // XXX: - use logging api? throw an exception instead?
            System.err.println("Could not read data for remote JVM " + vmid);
            e.printStackTrace();

        } finally {
            super.detach();
        }
    }

    /**
     * Get a copy of the remote instrumentation buffer.
     *<p>
     * The data in the remote instrumentation buffer is copied into
     * a local byte buffer.
     *
     * @throws RemoteException Thrown on any communications errors with
     *                         the remote system.
     */
    public void sample() throws RemoteException {
        assert updateBuffer != null;
        ((PerfDataBuffer)pdb).sample(updateBuffer);
    }

    /**
     * Get the proxy to the remote MonitoredVm.
     *
     * @return RemoteVm - the proxy to the remote MonitoredVm.
     */
    public RemoteVm getRemoteVm() {
        return rvm;
    }

    /**
     * {@inheritDoc}
     */
    public void addVmListener(VmListener l) {
        synchronized(listeners) {
            listeners.add(l);
            if (notifierTask == null) {
                notifierTask = new NotifierTask();
                timer.schedule(notifierTask, 0, interval);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVmListener(VmListener l) {
        synchronized(listeners) {
            listeners.remove(l);
            if (listeners.isEmpty() && (notifierTask != null)) {
                notifierTask.cancel();
                notifierTask = null;
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

            if (samplerTask != null) {
                samplerTask.cancel();
                SamplerTask oldSamplerTask = samplerTask;
                samplerTask = new SamplerTask();
                CountedTimerTaskUtils.reschedule(timer, oldSamplerTask,
                                                 samplerTask, oldInterval,
                                                 newInterval);
            }
            if (notifierTask != null) {
                notifierTask.cancel();
                NotifierTask oldNotifierTask = notifierTask;
                notifierTask = new NotifierTask();
                CountedTimerTaskUtils.reschedule(timer, oldNotifierTask,
                                                 notifierTask, oldInterval,
                                                 newInterval);
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
        ArrayList<VmListener> registered = null;
        MonitorStatusChangeEvent ev = null;

        synchronized(listeners) {
            registered = (ArrayList)listeners.clone();
        }

        for (Iterator<VmListener> i = registered.iterator(); i.hasNext(); /* empty */) {
            VmListener l = i.next();
            if (ev == null) {
                ev = new MonitorStatusChangeEvent(this, inserted, removed);
            }
            l.monitorStatusChanged(ev);
        }
    }

    /**
     * Fire MonitoredVmStructureChanged events.
     */
    @SuppressWarnings("unchecked") // Cast of result of clone
    void fireMonitorsUpdatedEvents() {
        ArrayList<VmListener> registered = null;
        VmEvent ev = null;

        synchronized(listeners) {
            registered = (ArrayList)listeners.clone();
        }

        for (Iterator<VmListener> i = registered.iterator(); i.hasNext(); /* empty */) {
            VmListener l = i.next();
            if (ev == null) {
                ev = new VmEvent(this);
            }
            l.monitorsUpdated(ev);
        }
    }

    /*
     * Timer Tasks. There are two separate timer tasks here. The SamplerTask
     * is active whenever we are attached to the remote JVM with a periodic
     * sampling interval > 0. The NotifierTask is only active if a VmListener
     * has registered with this RemoteMonitoredVm instance. Also, in the future
     * we may want to run these tasks at different intervals. Currently,
     * they run at the same interval and some significant work may
     * need to be done to complete the separation of these two intervals.
     */

    /**
     * Class to periodically check the state of the defined monitors
     * for the remote MonitoredVm instance and to notify listeners of
     * any detected changes.
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
            } catch (MonitorException e) {
                // XXX: use logging api? fire disconnect events? mark errored?
                // fireDisconnectedEvents();
                System.err.println("Exception updating monitors for "
                                   + getVmIdentifier());
                e.printStackTrace();
                // XXX: should we cancle the notifierTask here?
                // this.cancel();
            }
        }
    }

    /**
     * Class to periodically sample the remote instrumentation byte buffer
     * and refresh the local copy. Registered listeners are notified of
     * the completion of a sampling event.
     */
    private class SamplerTask extends CountedTimerTask {
        public void run() {
            super.run();
            try {
                sample();
                fireMonitorsUpdatedEvents();

            } catch (RemoteException e) {
                // XXX: use logging api, mark vm as errored.
                System.err.println("Exception taking sample for "
                                   + getVmIdentifier());
                e.printStackTrace();
                this.cancel();
            }
        }
    }
}
