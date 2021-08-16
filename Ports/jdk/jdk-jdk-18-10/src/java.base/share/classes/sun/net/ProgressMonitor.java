/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;

/**
 * ProgressMonitor is a class for monitoring progress in network input stream.
 *
 * @author Stanley Man-Kit Ho
 */
public class ProgressMonitor
{
    /**
     * Return default ProgressMonitor.
     */
    public static synchronized ProgressMonitor getDefault() {
        return pm;
    }

    /**
     * Change default ProgressMonitor implementation.
     */
    public static synchronized void setDefault(ProgressMonitor m)   {
        if (m != null)
            pm = m;
    }

    /**
     * Change progress metering policy.
     */
    public static synchronized void setMeteringPolicy(ProgressMeteringPolicy policy)    {
        if (policy != null)
            meteringPolicy = policy;
    }


    /**
     * Return a snapshot of the ProgressSource list
     */
    public ArrayList<ProgressSource> getProgressSources()    {
        ArrayList<ProgressSource> snapshot = new ArrayList<>();

        try {
            synchronized(progressSourceList)    {
                for (Iterator<ProgressSource> iter = progressSourceList.iterator(); iter.hasNext();)    {
                    ProgressSource pi = iter.next();

                    // Clone ProgressSource and add to snapshot
                    snapshot.add((ProgressSource)pi.clone());
                }
            }
        }
        catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return snapshot;
    }

    /**
     * Return update notification threshold
     */
    public synchronized int getProgressUpdateThreshold()    {
        return meteringPolicy.getProgressUpdateThreshold();
    }

    /**
     * Return true if metering should be turned on
     * for a particular URL input stream.
     */
    public boolean shouldMeterInput(URL url, String method) {
        return meteringPolicy.shouldMeterInput(url, method);
    }

    /**
     * Register progress source when progress is began.
     */
    public void registerSource(ProgressSource pi) {

        synchronized(progressSourceList)    {
            if (progressSourceList.contains(pi))
                return;

            progressSourceList.add(pi);
        }

        // Notify only if there is at least one listener
        if (progressListenerList.size() > 0)
        {
            // Notify progress listener if there is progress change
            ArrayList<ProgressListener> listeners = new ArrayList<>();

            // Copy progress listeners to another list to avoid holding locks
            synchronized(progressListenerList) {
                for (Iterator<ProgressListener> iter = progressListenerList.iterator(); iter.hasNext();) {
                    listeners.add(iter.next());
                }
            }

            // Fire event on each progress listener
            for (Iterator<ProgressListener> iter = listeners.iterator(); iter.hasNext();) {
                ProgressListener pl = iter.next();
                ProgressEvent pe = new ProgressEvent(pi, pi.getURL(), pi.getMethod(), pi.getContentType(), pi.getState(), pi.getProgress(), pi.getExpected());
                pl.progressStart(pe);
            }
        }
    }

    /**
     * Unregister progress source when progress is finished.
     */
    public void unregisterSource(ProgressSource pi) {

        synchronized(progressSourceList) {
            // Return if ProgressEvent does not exist
            if (progressSourceList.contains(pi) == false)
                return;

            // Close entry and remove from map
            pi.close();
            progressSourceList.remove(pi);
        }

        // Notify only if there is at least one listener
        if (progressListenerList.size() > 0)
        {
            // Notify progress listener if there is progress change
            ArrayList<ProgressListener> listeners = new ArrayList<>();

            // Copy progress listeners to another list to avoid holding locks
            synchronized(progressListenerList) {
                for (Iterator<ProgressListener> iter = progressListenerList.iterator(); iter.hasNext();) {
                    listeners.add(iter.next());
                }
            }

            // Fire event on each progress listener
            for (Iterator<ProgressListener> iter = listeners.iterator(); iter.hasNext();) {
                ProgressListener pl = iter.next();
                ProgressEvent pe = new ProgressEvent(pi, pi.getURL(), pi.getMethod(), pi.getContentType(), pi.getState(), pi.getProgress(), pi.getExpected());
                pl.progressFinish(pe);
            }
        }
    }

    /**
     * Progress source is updated.
     */
    public void updateProgress(ProgressSource pi)   {

        synchronized (progressSourceList)   {
            if (progressSourceList.contains(pi) == false)
                return;
        }

        // Notify only if there is at least one listener
        if (progressListenerList.size() > 0)
        {
            // Notify progress listener if there is progress change
            ArrayList<ProgressListener> listeners = new ArrayList<>();

            // Copy progress listeners to another list to avoid holding locks
            synchronized(progressListenerList)  {
                for (Iterator<ProgressListener> iter = progressListenerList.iterator(); iter.hasNext();) {
                    listeners.add(iter.next());
                }
            }

            // Fire event on each progress listener
            for (Iterator<ProgressListener> iter = listeners.iterator(); iter.hasNext();) {
                ProgressListener pl = iter.next();
                ProgressEvent pe = new ProgressEvent(pi, pi.getURL(), pi.getMethod(), pi.getContentType(), pi.getState(), pi.getProgress(), pi.getExpected());
                pl.progressUpdate(pe);
            }
        }
    }

    /**
     * Add progress listener in progress monitor.
     */
    public void addProgressListener(ProgressListener l) {
        synchronized(progressListenerList) {
            progressListenerList.add(l);
        }
    }

    /**
     * Remove progress listener from progress monitor.
     */
    public void removeProgressListener(ProgressListener l) {
        synchronized(progressListenerList) {
            progressListenerList.remove(l);
        }
    }

    // Metering policy
    private static ProgressMeteringPolicy meteringPolicy = new DefaultProgressMeteringPolicy();

    // Default implementation
    private static ProgressMonitor pm = new ProgressMonitor();

    // ArrayList for outstanding progress sources
    private ArrayList<ProgressSource> progressSourceList = new ArrayList<ProgressSource>();

    // ArrayList for progress listeners
    private ArrayList<ProgressListener> progressListenerList = new ArrayList<ProgressListener>();
}


/**
 * Default progress metering policy.
 */
class DefaultProgressMeteringPolicy implements ProgressMeteringPolicy  {
    /**
     * Return true if metering should be turned on for a particular network input stream.
     */
    public boolean shouldMeterInput(URL url, String method)
    {
        // By default, no URL input stream is metered for
        // performance reason.
        return false;
    }

    /**
     * Return update notification threshold.
     */
    public int getProgressUpdateThreshold() {
        // 8K - same as default I/O buffer size
        return 8192;
    }
}
