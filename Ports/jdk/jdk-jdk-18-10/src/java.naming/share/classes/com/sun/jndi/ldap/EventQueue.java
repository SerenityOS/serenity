/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.util.Vector;
import java.util.EventObject;

import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

/**
 * Package private class used by EventSupport to dispatch events.
 * This class implements an event queue, and a dispatcher thread that
 * dequeues and dispatches events from the queue.
 *
 * Pieces stolen from sun.misc.Queue.
 *
 * @author      Bill Shannon (from javax.mail.event)
 * @author      Rosanna Lee (modified for JNDI-related events)
 */
final class EventQueue implements Runnable {
    private static final boolean debug = false;

    private static class QueueElement {
        QueueElement next = null;
        QueueElement prev = null;
        EventObject event = null;
        Vector<NamingListener> vector = null;

        QueueElement(EventObject event, Vector<NamingListener> vector) {
            this.event = event;
            this.vector = vector;
        }
    }

    private QueueElement head = null;
    private QueueElement tail = null;
    private Thread qThread;

    // package private
    EventQueue() {
        qThread = Obj.helper.createThread(this);
        qThread.setDaemon(true);  // not a user thread
        qThread.start();
    }

    // package private;
    /**
     * Enqueue an event.
     * @param event Either a {@code NamingExceptionEvent} or a subclass
     *        of {@code NamingEvent} or
     *        {@code UnsolicitedNotificationEvent}.
     * If it is a subclass of {@code NamingEvent}, all listeners must implement
     * the corresponding subinterface of {@code NamingListener}.
     * For example, for an {@code ObjectAddedEvent}, all listeners <em>must</em>
     * implement the {@code ObjectAddedListener} interface.
     * <em>The current implementation does not check this before dispatching
     * the event.</em>
     * If the event is a {@code NamingExceptionEvent}, then all listeners
     * are notified.
     * @param vector List of NamingListeners that will be notified of event.
     */
    synchronized void enqueue(EventObject event, Vector<NamingListener> vector) {
        QueueElement newElt = new QueueElement(event, vector);

        if (head == null) {
            head = newElt;
            tail = newElt;
        } else {
            newElt.next = head;
            head.prev = newElt;
            head = newElt;
        }
        notify();
    }

    /**
     * Dequeue the oldest object on the queue.
     * Used only by the run() method.
     *
     * @return    the oldest object on the queue.
     * @exception java.lang.InterruptedException if any thread has
     *              interrupted this thread.
     */
    private synchronized QueueElement dequeue()
                                throws InterruptedException {
        while (tail == null)
            wait();
        QueueElement elt = tail;
        tail = elt.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        elt.prev = elt.next = null;
        return elt;
    }

    /**
     * Pull events off the queue and dispatch them.
     */
    public void run() {
        QueueElement qe;

        try {
            while ((qe = dequeue()) != null) {
                EventObject e = qe.event;
                Vector<NamingListener> v = qe.vector;

                for (int i = 0; i < v.size(); i++) {

                    // Dispatch to corresponding NamingListener
                    // The listener should only be getting the event that
                    // it is interested in. (No need to check mask or
                    // instanceof subinterfaces.)
                    // It is the responsibility of the enqueuer to
                    // only enqueue events with listeners of the correct type.

                    if (e instanceof NamingEvent) {
                        ((NamingEvent)e).dispatch(v.elementAt(i));

                    // An exception occurred: if notify all naming listeners
                    } else if (e instanceof NamingExceptionEvent) {
                        ((NamingExceptionEvent)e).dispatch(v.elementAt(i));
                    } else if (e instanceof UnsolicitedNotificationEvent) {
                        ((UnsolicitedNotificationEvent)e).dispatch(
                            (UnsolicitedNotificationListener)v.elementAt(i));
                    }
                }

                qe = null; e = null; v = null;
            }
        } catch (InterruptedException e) {
            // just die
        }
    }

    // package private; used by EventSupport;
    /**
     * Stop the dispatcher so we can be destroyed.
     */
    void stop() {
        if (debug) System.err.println("EventQueue stopping");
        if (qThread != null) {
            qThread.interrupt();        // kill our thread
            qThread = null;
        }
    }
}
