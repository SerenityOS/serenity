/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package nsk.share.jdi;

import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.*;

/**
 * This class provides a separate thread for asynchronous listening
 * to JDI events. All received events are sequentially passed
 * to event listeners. If current event listener returns true
 * then the event is considered be processed and it is not passed
 * to remaining listeners.
 * <p>
 * The <code>EventHandler</code> thread runs until <code>VMDisconnectEvent</code>
 * is received or <code>VMDisconnectionedException</code> is caught.
 *
 * @see EventListener
 */
public class EventHandler implements Runnable {

    private Debugee debuggee = null;
    private Log log = null;

    private VirtualMachine vm;
    private EventRequestManager requestManager;
    /**
     * Container for event listeners
     */
    private static List<EventListener> listeners = Collections.synchronizedList(new Vector<EventListener>());
    private Thread listenThread;

    /**
     * Exit status of the <code>EventHandler</code> thread
     */
    private static volatile int status = -1;

    /**
     * Return an exit status of the <code>EventHandler</code> thread
     * were the values are:
     * <li><i>-1</i> - no value
     * <li><i>0</i> - normal termination
     * <li><i>1</i> - the thread is still running
     * <li><i>2</i> - abnormal termination
     */
    public static synchronized int getStatus() {
        return status;
    }

    /**
     * Default ExceptionRequest with SUSPEND_EVENT_THREAD to be able
     * to catch debuggee exceptions and output a message.
     */
    private static ExceptionRequest defaultExceptionRequest = null;

    /**
     * This flag will be set true if event of uncaught exception has been
     * received.
     */
    private static volatile boolean defaultExceptionCaught = false;
    public static synchronized boolean getExceptionCaught() {
        return defaultExceptionCaught;
    }

    /**
     * This flag will be set true if any event which does not have specific
     * listener has been received.
     */
    private static volatile boolean unexpectedEventCaught = false;
    public static synchronized boolean unexpectedEventCaught() {
        return unexpectedEventCaught;
    }

    /**
     * This flag shows if debugged VM is connected to debugger.
     */
    private static volatile boolean vmDisconnected = false;
    public static synchronized boolean isDisconnected() {
        return vmDisconnected;
    }

    public EventHandler(Debugee debuggee, Log log) {
        this.listenThread = new Thread(this);
        this.listenThread.setDaemon(true);

        this.debuggee = debuggee;
        this.log = log;
        this.vm = debuggee.VM();
        this.requestManager = vm.eventRequestManager();
    }

    private void display(String str) {
        log.display("EventHandler> " + str);
    }

    // is EventHandler was interrupted
    private volatile boolean wasInterrupted;

    public void stopEventHandler() {
        wasInterrupted = true;

        listenThread.interrupt();

        try {
            listenThread.join();
        }
        catch(InterruptedException e) {
            throw new TestBug("Unexpected exception: " + e);
        }
    }
    /**
     * The <code>EventHandler</code> thread keeps running until a VMDisconnectedEvent occurs
     * or some exception occurs during event processing.
     */
    public void run() {
        synchronized(EventHandler.this) {
            status = 1; // running
        }
        do {
            try {
                EventSet set = vm.eventQueue().remove();

                switch (set.suspendPolicy()) {
                    case EventRequest.SUSPEND_NONE:
                        display("Received event set with policy = SUSPEND_NONE");
                        break;
                    case EventRequest.SUSPEND_ALL:
                        display("Received event set with policy = SUSPEND_ALL");
                        break;
                    case EventRequest.SUSPEND_EVENT_THREAD:
                        display("Received event set with policy = SUSPEND_EVENT_THREAD");
                        break;
                }

                synchronized (listeners) {
                    synchronized (EventHandler.this) {
                        for (EventListener listener : listeners) {
                            // proloque listener for a event set
                            listener.eventSetReceived(set);
                        }

                        for (Event event : set) {
                            // print only event class name here because of Event,toString may cause unexpected exception
                            display("Event: " + event.getClass().getSimpleName()
                                    + " req " + event.request());
                            boolean processed = false;
                            for (EventListener listener : listeners) {
                                processed = listener.eventReceived(event);

                                if (processed) {
                                    if (listener.shouldRemoveListener()) {
                                        listener.eventSetComplete(set);
                                        removeListener(listener);
                                    }

                                    break;
                                }
                            }
                        }

                        for (EventListener listener : listeners) {
                            // epiloque listener for a event set
                            listener.eventSetComplete(set);
                        }
                    }
                }

            }
            catch (Exception e) {

                if(e instanceof InterruptedException) {
                    if(wasInterrupted)
                        break;
                }

                log.complain("Exception occured in eventHandler thread: " + e.getMessage());
                e.printStackTrace(log.getOutStream());
                synchronized(EventHandler.this) {
                    // This will make the waiters such as waitForVMDisconnect
                    // exit their wait loops.
                    vmDisconnected = true;
                    status = 2; // abnormal termination
                    EventHandler.this.notifyAll();
                }
                throw new Failure(e);
            }
        } while (!wasInterrupted && !isDisconnected());

        if (unexpectedEventCaught || defaultExceptionCaught) {
            synchronized(EventHandler.this) {
                status = 2;
            }
        }
        display("finished");
    }


    /**
     * This is normally called in the main thread of the test debugger.
     * It starts up an <code>EventHandler</code> thread that gets events coming in
     * from the debuggee and distributes them to listeners.
     */
    public void startListening() {
        createDefaultEventRequests();
        createDefaultListeners();
        listenThread.start();
    }


    /**
     * This method sets up default requests.
     */
    private void createDefaultEventRequests() {
        /**
         * The following request will allow to print a warning if a debuggee gets an
         * unexpected exception. The unexpected exception will be handled in
         * the eventReceived method in the default listener created.
         * If a test case does not want an uncaught exception to cause a
         * message, it must add new listener for uncaught exception events to
         * handle them.
         */
        defaultExceptionRequest = requestManager.createExceptionRequest(null, false, true);
        defaultExceptionRequest.enable();
    }

    /**
     * This method sets up default listeners.
     */
    private void createDefaultListeners() {
        /**
         * This listener catches up all unexpected events.
         *
         */
        addListener(
                new EventListener() {
                    public boolean eventReceived(Event event) {
                        log.complain("EventHandler>  Unexpected event: " + event.getClass().getName());
                        unexpectedEventCaught = true;
                        return true;
                    }
                }
        );

        /**
         * This listener catches up VMStart event.
         */
        addListener(
                new EventListener() {
                    public boolean eventReceived(Event event) {
                        if (event instanceof  VMStartEvent) {
                            display("received VMStart");
                            removeListener(this);
                            return true;
                        }
                        return false;
                    }
                }
        );

        /**
         * This listener catches up VMDeath event.
         */
        addListener(
                new EventListener() {
                    public boolean eventReceived(Event event) {
                        if (event instanceof VMDeathEvent) {
                            display("receieved VMDeath");
                            removeListener(this);
                            return true;
                        }
                        return false;
                    }
                }
        );

        /**
         * This listener catches up <code>VMDisconnectEvent</code>event and
         * signals <code>EventHandler</code> thread to finish.
         */
        addListener(
                new EventListener() {
                    public boolean eventReceived(Event event) {
                        if (event instanceof VMDisconnectEvent ) {
                            display("receieved VMDisconnect");
                            synchronized(EventHandler.this) {
                                vmDisconnected = true;
                                status = 0; // OK finish
                                EventHandler.this.notifyAll();
                                removeListener(this);
                            }
                            return true;
                        }
                        return false;
                    }
                }
        );

        /**
         * This listener catches uncaught exceptions and print a message.
         */
        addListener( new EventListener() {
            public boolean eventReceived(Event event) {
                boolean handled = false;

                if (event instanceof ExceptionEvent &&
                    defaultExceptionRequest != null &&
                    defaultExceptionRequest.equals(event.request())) {

                    if (EventFilters.filtered(event) == false) {
                        log.complain("EventHandler>  Unexpected Debuggee Exception: " +
                                     (ExceptionEvent)event);
                        defaultExceptionCaught = true;
                    }

                    handled = true;
                    vm.resume();
                }

                return handled;
            }
        }
        );
    }

    /**
     * Add at beginning of the list because we want
     * the LAST added listener to be FIRST to process
     * current event.
     */
    public void addListener(EventListener listener) {
        display("Adding listener " + listener);
        synchronized(listeners) {
            listeners.add(0, listener);
        }
    }

    /**
     * Removes the listener from the list.
     */
    public void removeListener(EventListener listener) {
        display("Removing listener " + listener);
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }


    /**
     * Returns an event which is received for any of given requests.
     */
    public Event waitForRequestedEvent( final EventRequest[] requests,
            long timeout,
            boolean shouldRemoveListeners) {
        class EventNotification {
            volatile Event event = null;
        }
        final EventNotification en = new EventNotification();

        EventListener listener = new EventListener() {
            public boolean eventReceived(Event event) {
                for (int i = 0; i < requests.length; i++) {
                    EventRequest request = requests[i];
                    if (!request.isEnabled())
                        continue;
                    if (request.equals(event.request())) {
                        display("waitForRequestedEvent: Received event(" + event + ") for request(" + request + ")");
                        synchronized (EventHandler.this) {
                            en.event = event;
                            EventHandler.this.notifyAll();
                        }
                        return true;
                    }
                }
                return false;
            }
        };
        if (shouldRemoveListeners) {
            display("waitForRequestedEvent: enabling remove of listener " + listener);
            listener.enableRemovingThisListener();
        }
        for (int i = 0; i < requests.length; i++) {
            requests[i].enable();
        }
        addListener(listener);

        try {
            long timeToFinish = System.currentTimeMillis() + timeout;
            long timeLeft = timeout;
            synchronized (EventHandler.this) {
                display("waitForRequestedEvent: vm.resume called");
                vm.resume();

                while (!isDisconnected() && en.event == null && timeLeft > 0) {
                    EventHandler.this.wait(timeLeft);
                    timeLeft = timeToFinish - System.currentTimeMillis();
                }
            }
        } catch (InterruptedException e) {
            return null;
        }
        if (shouldRemoveListeners && !isDisconnected()) {
            for (int i = 0; i < requests.length; i++) {
                requests[i].disable();
            }
        }
        if (en.event == null) {
            throw new Failure("waitForRequestedEvent: no requested events have been received.");
        }
        return en.event;
    }

    /**
     * Returns an event set which is received for any of given requests.
     */
    public EventSet waitForRequestedEventSet( final EventRequest[] requests,
            long timeout,
            boolean shouldRemoveListeners) {
        class EventNotification {
            volatile EventSet set = null;
        }
        final EventNotification en = new EventNotification();

        EventListener listener = new EventListener() {
            public void eventSetReceived(EventSet set) {

                EventIterator eventIterator = set.eventIterator();

                while (eventIterator.hasNext()) {

                    Event event = eventIterator.nextEvent();

                    for (int i = 0; i < requests.length; i++) {
                        EventRequest request = requests[i];
                        if (!request.isEnabled())
                            continue;

                        if (request.equals(event.request())) {
                            display("waitForRequestedEventSet: Received event set for request: " + request);
                            synchronized (EventHandler.this) {
                                en.set = set;
                                EventHandler.this.notifyAll();
                            }
                            return;
                        }
                    }
                }
            }

            public boolean eventReceived(Event event) {
                return (en.set != null);
            }
        };

        if (shouldRemoveListeners) {
            display("waitForRequestedEventSet: enabling remove of listener " + listener);
            listener.enableRemovingThisListener();
        }
        for (int i = 0; i < requests.length; i++) {
            requests[i].enable();
        }
        addListener(listener);

        try {
            long timeToFinish = System.currentTimeMillis() + timeout;
            long timeLeft = timeout;
            synchronized (EventHandler.this) {
                display("waitForRequestedEventSet: vm.resume called");
                vm.resume();

                while (!isDisconnected() && en.set == null && timeLeft > 0) {
                    EventHandler.this.wait(timeLeft);
                    timeLeft = timeToFinish - System.currentTimeMillis();
                }
            }
        } catch (InterruptedException e) {
            return null;
        }
        if (shouldRemoveListeners && !isDisconnected()) {
            for (int i = 0; i < requests.length; i++) {
                requests[i].disable();
            }
        }
        if (en.set == null) {
            throw new Failure("waitForRequestedEventSet: no requested events have been received.");
        }
        return en.set;
    }

    public synchronized void waitForVMDisconnect() {
        display("waitForVMDisconnect");
        while (!isDisconnected()) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        display("waitForVMDisconnect: done");
    }

    /**
     * This is a superclass for any event listener.
     */
    public static class EventListener {

        /**
         * This flag shows if the listener must be removed
         * after current event has been processed by
         * this listener.
         */
        volatile boolean shouldRemoveListener = false;
        public boolean shouldRemoveListener() {
            return shouldRemoveListener;
        }

        public void enableRemovingThisListener() {
            shouldRemoveListener = true;
        }

        /**
         * This method will be called by <code>EventHandler</code>
         * for received event set before any call of <code>eventReceived</code>
         * method for events contained in this set.
         */
        public void eventSetReceived(EventSet set) {}


        /**
         * This method will be called by <code>EventHandler</code>
         * for received event set after all calls of <code>eventReceived</code>
         * and event specific methods for all events contained in this set.
         */
        public void eventSetComplete(EventSet set) {}

        /**
         * This method will be called by <code>EventHandler</code>
         * for any event contained in received event set.
         *
         * @return <code>true</code> if event was processed by this
         *          <code>EventListener<code> or <code>false</code> otherwise.
         */
        public boolean eventReceived(Event event) {
            return false;
        }
    }
}
