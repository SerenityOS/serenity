/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
 * EventDispatcher.  Used by various classes in the Java Sound implementation
 * to send events.
 *
 * @author David Rivas
 * @author Kara Kytle
 * @author Florian Bomers
 */
final class EventDispatcher implements Runnable {

    /**
     * time of inactivity until the auto closing clips
     * are closed.
     */
    private static final int AUTO_CLOSE_TIME = 5000;

    /**
     * List of events.
     */
    private final ArrayList<EventInfo> eventQueue = new ArrayList<>();

    /**
     * Thread object for this EventDispatcher instance.
     */
    private Thread thread = null;

    /*
     * support for auto-closing Clips
     */
    private final ArrayList<ClipInfo> autoClosingClips = new ArrayList<>();

    /*
     * support for monitoring data lines
     */
    private final ArrayList<LineMonitor> lineMonitors = new ArrayList<>();

    /**
     * Approximate interval between calls to LineMonitor.checkLine
     */
    static final int LINE_MONITOR_TIME = 400;

    /**
     * This start() method starts an event thread if one is not already active.
     */
    synchronized void start() {

        if(thread == null) {
            thread = JSSecurityManager.createThread(this,
                                                    "Java Sound Event Dispatcher",   // name
                                                    true,  // daemon
                                                    -1,    // priority
                                                    true); // doStart
        }
    }

    /**
     * Invoked when there is at least one event in the queue.
     * Implement this as a callback to process one event.
     */
    void processEvent(EventInfo eventInfo) {
        int count = eventInfo.getListenerCount();

        // process an LineEvent
        if (eventInfo.getEvent() instanceof LineEvent) {
            LineEvent event = (LineEvent) eventInfo.getEvent();
            for (int i = 0; i < count; i++) {
                try {
                    ((LineListener) eventInfo.getListener(i)).update(event);
                } catch (Throwable t) {
                    if (Printer.err) t.printStackTrace();
                }
            }
            return;
        }

        // process a MetaMessage
        if (eventInfo.getEvent() instanceof MetaMessage) {
            MetaMessage event = (MetaMessage)eventInfo.getEvent();
            for (int i = 0; i < count; i++) {
                try {
                    ((MetaEventListener) eventInfo.getListener(i)).meta(event);
                } catch (Throwable t) {
                    if (Printer.err) t.printStackTrace();
                }
            }
            return;
        }

        // process a Controller or Mode Event
        if (eventInfo.getEvent() instanceof ShortMessage) {
            ShortMessage event = (ShortMessage)eventInfo.getEvent();
            int status = event.getStatus();

            // Controller and Mode events have status byte 0xBc, where
            // c is the channel they are sent on.
            if ((status & 0xF0) == 0xB0) {
                for (int i = 0; i < count; i++) {
                    try {
                        ((ControllerEventListener) eventInfo.getListener(i)).controlChange(event);
                    } catch (Throwable t) {
                        if (Printer.err) t.printStackTrace();
                    }
                }
            }
            return;
        }

        Printer.err("Unknown event type: " + eventInfo.getEvent());
    }

    /**
     * Wait until there is something in the event queue to process.  Then
     * dispatch the event to the listeners.The entire method does not
     * need to be synchronized since this includes taking the event out
     * from the queue and processing the event. We only need to provide
     * exclusive access over the code where an event is removed from the
     *queue.
     */
    void dispatchEvents() {

        EventInfo eventInfo = null;

        synchronized (this) {

            // Wait till there is an event in the event queue.
            try {

                if (eventQueue.size() == 0) {
                    if (autoClosingClips.size() > 0 || lineMonitors.size() > 0) {
                        int waitTime = AUTO_CLOSE_TIME;
                        if (lineMonitors.size() > 0) {
                            waitTime = LINE_MONITOR_TIME;
                        }
                        wait(waitTime);
                    } else {
                        wait();
                    }
                }
            } catch (InterruptedException e) {
            }
            if (eventQueue.size() > 0) {
                // Remove the event from the queue and dispatch it to the listeners.
                eventInfo = eventQueue.remove(0);
            }

        } // end of synchronized
        if (eventInfo != null) {
            processEvent(eventInfo);
        } else {
            if (autoClosingClips.size() > 0) {
                closeAutoClosingClips();
            }
            if (lineMonitors.size() > 0) {
                monitorLines();
            }
        }
    }

    /**
     * Queue the given event in the event queue.
     */
    private synchronized void postEvent(EventInfo eventInfo) {
        eventQueue.add(eventInfo);
        notifyAll();
    }

    /**
     * A loop to dispatch events.
     */
    @Override
    public void run() {

        while (true) {
            try {
                dispatchEvents();
            } catch (Throwable t) {
                if (Printer.err) t.printStackTrace();
            }
        }
    }

    /**
     * Send audio and MIDI events.
     */
    void sendAudioEvents(Object event, List<Object> listeners) {
        if ((listeners == null)
            || (listeners.size() == 0)) {
            // nothing to do
            return;
        }

        start();

        EventInfo eventInfo = new EventInfo(event, listeners);
        postEvent(eventInfo);
    }

    /*
     * go through the list of registered auto-closing
     * Clip instances and close them, if appropriate
     *
     * This method is called in regular intervals
     */
    private void closeAutoClosingClips() {
        synchronized(autoClosingClips) {
            long currTime = System.currentTimeMillis();
            for (int i = autoClosingClips.size()-1; i >= 0 ; i--) {
                ClipInfo info = autoClosingClips.get(i);
                if (info.isExpired(currTime)) {
                    AutoClosingClip clip = info.getClip();
                    // sanity check
                    if (!clip.isOpen() || !clip.isAutoClosing()) {
                        autoClosingClips.remove(i);
                    }
                    else if (!clip.isRunning() && !clip.isActive() && clip.isAutoClosing()) {
                        clip.close();
                    } else {
                    }
                }
            }
        }
    }

    private int getAutoClosingClipIndex(AutoClosingClip clip) {
        synchronized(autoClosingClips) {
            for (int i = autoClosingClips.size()-1; i >= 0; i--) {
                if (clip.equals(autoClosingClips.get(i).getClip())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * called from auto-closing clips when one of their open() method is called.
     */
    void autoClosingClipOpened(AutoClosingClip clip) {
        int index = 0;
        synchronized(autoClosingClips) {
            index = getAutoClosingClipIndex(clip);
            if (index == -1) {
                autoClosingClips.add(new ClipInfo(clip));
            }
        }
        if (index == -1) {
            synchronized (this) {
                // this is only for the case that the first clip is set to autoclosing,
                // and it is already open, and nothing is done with it.
                // EventDispatcher.process() method would block in wait() and
                // never close this first clip, keeping the device open.
                notifyAll();
            }
        }
    }

    /**
     * called from auto-closing clips when their closed() method is called.
     */
    void autoClosingClipClosed(AutoClosingClip clip) {
        synchronized(autoClosingClips) {
            int index = getAutoClosingClipIndex(clip);
            if (index != -1) {
                autoClosingClips.remove(index);
            }
        }
    }


    // ////////////////////////// Line Monitoring Support /////////////////// //
    /*
     * go through the list of registered line monitors
     * and call their checkLine method
     *
     * This method is called in regular intervals
     */
    private void monitorLines() {
        synchronized(lineMonitors) {
            for (int i = 0; i < lineMonitors.size(); i++) {
                lineMonitors.get(i).checkLine();
            }
        }
    }

    /**
     * Add this LineMonitor instance to the list of monitors.
     */
    void addLineMonitor(LineMonitor lm) {
        synchronized(lineMonitors) {
            if (lineMonitors.indexOf(lm) >= 0) {
                return;
            }
            lineMonitors.add(lm);
        }
        synchronized (this) {
            // need to interrupt the infinite wait()
            notifyAll();
        }
    }

    /**
     * Remove this LineMonitor instance from the list of monitors.
     */
    void removeLineMonitor(LineMonitor lm) {
        synchronized(lineMonitors) {
            if (lineMonitors.indexOf(lm) < 0) {
                return;
            }
            lineMonitors.remove(lm);
        }
    }

    /**
     * Container for an event and a set of listeners to deliver it to.
     */
    private class EventInfo {

        private final Object event;
        private final Object[] listeners;

        /**
         * Create a new instance of this event Info class
         * @param event the event to be dispatched
         * @param listeners listener list; will be copied
         */
        EventInfo(Object event, List<Object> listeners) {
            this.event = event;
            this.listeners = listeners.toArray();
        }

        Object getEvent() {
            return event;
        }

        int getListenerCount() {
            return listeners.length;
        }

        Object getListener(int index) {
            return listeners[index];
        }

    } // class EventInfo


    /**
     * Container for a clip with its expiration time.
     */
    private class ClipInfo {

        private final AutoClosingClip clip;
        private final long expiration;

        /**
         * Create a new instance of this clip Info class.
         */
        ClipInfo(AutoClosingClip clip) {
            this.clip = clip;
            this.expiration = System.currentTimeMillis() + AUTO_CLOSE_TIME;
        }

        AutoClosingClip getClip() {
            return clip;
        }

        boolean isExpired(long currTime) {
            return currTime > expiration;
        }
    } // class ClipInfo


    /**
     * Interface that a class that wants to get regular
     * line monitor events implements.
     */
    interface LineMonitor {
        /**
         * Called by event dispatcher in regular intervals.
         */
        void checkLine();
    }

} // class EventDispatcher
