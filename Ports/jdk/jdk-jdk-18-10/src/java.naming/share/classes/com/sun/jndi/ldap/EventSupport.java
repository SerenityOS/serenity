/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Hashtable;
import java.util.Vector;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Map;

import javax.naming.*;
import javax.naming.event.*;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.UnsolicitedNotificationListener;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotification;

/**
 * This is a utility class that can be used by a context that supports
 * event notification.  You can use an instance of this class as a member field
 * of your context and delegate various work to it.
 * It is currently structured so that each context should have its own
 * EventSupport (instead of static version shared by all contexts
 * of a service provider).
 *<p>
 * This class supports two types of listeners: those that register for
 * NamingEvents, and those for UnsolicitedNotificationEvents (they can be mixed
 * into the same listener).
 * For NamingEvent listeners, it maintains a hashtable that maps
 * registration requests--the key--to
 * <em>notifiers</em>--the value. Each registration request consists of:
 *<ul>
 *<li>The name argument of the registration.
 *<li>The filter (default is "(objectclass=*)").
 *<li>The search controls (default is null SearchControls).
 *<li>The events that the listener is interested in. This is determined by
 * finding out which {@code NamingListener} interface the listener supports.
 *</ul>
 *<p>
 *A notifier ({@code NamingEventNotifier}) is a worker thread that is responsible
 *for gathering information for generating events requested by its listeners.
 *Each notifier maintains its own list of listeners; these listeners have
 *all made the same registration request (at different times) and implements
 *the same {@code NamingListener} interfaces.
 *<p>
 *For unsolicited listeners, this class maintains a vector, unsolicited.
 *When an unsolicited listener is registered, this class adds itself
 *to the context's LdapClient. When LdapClient receives an unsolicited
 *notification, it notifies this EventSupport to fire an event to the
 *the listeners. Special handling in LdapClient is done for the DISCONNECT
 *notification. [It results in the EventSupport firing also a
 *NamingExceptionEvent to the unsolicited listeners.]
 *<p>
 *
 *When a context no longer needs this EventSupport, it should invoke
 *cleanup() on it.
 *<p>
 *<h2>Registration</h2>
 *When a registration request is made, this class attempts to find an
 *existing notifier that's already working on the request. If one is
 *found, the listener is added to the notifier's list. If one is not found,
 *a new notifier is created for the listener.
 *
 *<h2>Deregistration</h2>
 *When a deregistration request is made, this class attempts to find its
 *corresponding notifier. If the notifier is found, the listener is removed
 *from the notifier's list. If the listener is the last listener on the list,
 *the notifier's thread is terminated and removed from this class's hashtable.
 *Nothing happens if the notifier is not found.
 *
 *<h2>Event Dispatching</h2>
 *The notifiers are responsible for gather information for generating events
 *requested by their respective listeners. When a notifier gets sufficient
 *information to generate an event, it creates invokes the
 *appropriate {@code fireXXXEvent} on this class with the information and list of
 *listeners. This causes an event and the list of listeners to be added
 *to the <em>event queue</em>.
 *This class maintains an event queue and a dispatching thread that dequeues
 *events from the queue and dispatches them to the listeners.
 *
 *<h2>Synchronization</h2>
 *This class is used by the main thread (LdapCtx) to add/remove listeners.
 *It is also used asynchronously by NamingEventNotifiers threads and
 *the context's Connection thread. It is used by the notifier threads to
 *queue events and to update the notifiers list when the notifiers exit.
 *It is used by the Connection thread to fire unsolicited notifications.
 *Methods that access/update the 'unsolicited' and 'notifiers' lists are
 *thread-safe.
 *
 * @author Rosanna Lee
 */
final class EventSupport {
    private static final boolean debug = false;

    private LdapCtx ctx;

    /**
     * NamingEventNotifiers; hashed by search arguments;
     */
    private Hashtable<NotifierArgs, NamingEventNotifier> notifiers =
            new Hashtable<>(11);

    /**
     * List of unsolicited notification listeners.
     */
    private Vector<UnsolicitedNotificationListener> unsolicited = null;

    /**
     * Constructs EventSupport for ctx.
     * <em>Do we need to record the name of the target context?
     * Or can we assume that EventSupport is called on a resolved
     * context? Do we need other add/remove-NamingListener methods?
     * package private;
     */
    EventSupport(LdapCtx ctx) {
        this.ctx = ctx;
    }

    /**
     * Adds {@code l} to list of listeners interested in {@code nm}.
     */
    /*
     * Make the add/removeNamingListeners synchronized to:
     * 1. protect usage of 'unsolicited', which may be read by
     *    the Connection thread when dispatching unsolicited notification.
     * 2. ensure that NamingEventNotifier thread's access to 'notifiers'
     *    is safe
     */
    synchronized void addNamingListener(String nm, int scope,
        NamingListener l) throws NamingException {

        if (l instanceof ObjectChangeListener ||
            l instanceof NamespaceChangeListener) {
            NotifierArgs args = new NotifierArgs(nm, scope, l);

            NamingEventNotifier notifier = notifiers.get(args);
            if (notifier == null) {
                notifier = new NamingEventNotifier(this, ctx, args, l);
                notifiers.put(args, notifier);
            } else {
                notifier.addNamingListener(l);
            }
        }
        if (l instanceof UnsolicitedNotificationListener) {
            // Add listener to this's list of unsolicited notifiers
            if (unsolicited == null) {
                unsolicited = new Vector<>(3);
            }

            unsolicited.addElement((UnsolicitedNotificationListener)l);
        }
    }

    /**
     * Adds {@code l} to list of listeners interested in {@code nm}
     * and filter.
     */
    synchronized void addNamingListener(String nm, String filter,
        SearchControls ctls, NamingListener l) throws NamingException {

        if (l instanceof ObjectChangeListener ||
            l instanceof NamespaceChangeListener) {
            NotifierArgs args = new NotifierArgs(nm, filter, ctls, l);

            NamingEventNotifier notifier = notifiers.get(args);
            if (notifier == null) {
                notifier = new NamingEventNotifier(this, ctx, args, l);
                notifiers.put(args, notifier);
            } else {
                notifier.addNamingListener(l);
            }
        }
        if (l instanceof UnsolicitedNotificationListener) {
            // Add listener to this's list of unsolicited notifiers
            if (unsolicited == null) {
                unsolicited = new Vector<>(3);
            }
            unsolicited.addElement((UnsolicitedNotificationListener)l);
        }
    }

    /**
     * Removes {@code l} from all notifiers in this context.
     */
    synchronized void removeNamingListener(NamingListener l) {
        if (debug) {
            System.err.println("EventSupport removing listener");
        }
        // Go through list of notifiers, remove 'l' from each.
        // If 'l' is notifier's only listener, remove notifier too.
        Iterator<NamingEventNotifier> iterator = notifiers.values().iterator();
        while (iterator.hasNext()) {
            NamingEventNotifier notifier = iterator.next();
            if (notifier != null) {
                if (debug) {
                    System.err.println("EventSupport removing listener from notifier");
                }
                notifier.removeNamingListener(l);
                if (!notifier.hasNamingListeners()) {
                    if (debug) {
                        System.err.println("EventSupport stopping notifier");
                    }
                    notifier.stop();
                    iterator.remove();
                }
            }
        }
        // Remove from list of unsolicited notifier
        if (debug) {
            System.err.println("EventSupport removing unsolicited: " + unsolicited);
        }
        if (unsolicited != null) {
            unsolicited.removeElement(l);
        }
    }

    synchronized boolean hasUnsolicited() {
        return (unsolicited != null && unsolicited.size() > 0);
    }

    /**
      * package private;
      * Called by NamingEventNotifier to remove itself when it encounters
      * a NamingException.
      */
    synchronized void removeDeadNotifier(NotifierArgs info) {
        if (debug) {
            System.err.println("EventSupport.removeDeadNotifier: " + info.name);
        }
        if (notifiers != null) {
            // Only do this if cleanup() not been triggered, otherwise here
            // will throw NullPointerException since notifiers will be set to
            // null in cleanup()
            notifiers.remove(info);
        }
    }

    /**
     * Fire an event to unsolicited listeners.
     * package private;
     * Called by LdapCtx when its clnt receives an unsolicited notification.
     */
    synchronized void fireUnsolicited(Object obj) {
        if (debug) {
            System.err.println("EventSupport.fireUnsolicited: " + obj + " "
                + unsolicited);
        }
        if (unsolicited == null || unsolicited.size() == 0) {
            // This shouldn't really happen, but might in case
            // there is a timing problem that removes a listener
            // before a fired event reaches here.
            return;
        }

        if (obj instanceof UnsolicitedNotification) {

            // Fire UnsolicitedNotification to unsolicited listeners

            UnsolicitedNotificationEvent evt =
                new UnsolicitedNotificationEvent(ctx, (UnsolicitedNotification)obj);
            queueEvent(evt, unsolicited);

        } else if (obj instanceof NamingException) {

            // Fire NamingExceptionEvent to unsolicited listeners.

            NamingExceptionEvent evt =
                new NamingExceptionEvent(ctx, (NamingException)obj);
            queueEvent(evt, unsolicited);

            // When an exception occurs, the unsolicited listeners
            // are automatically deregistered.
            // When LdapClient.processUnsolicited() fires a NamingException,
            // it will update its listener list so we don't have to.
            // Likewise for LdapCtx.

            unsolicited = null;
        }
    }

    /**
     * Stops notifier threads that are collecting event data and
     * stops the event queue from dispatching events.
     * Package private; used by LdapCtx.
     */
    synchronized void cleanup() {
        if (debug) System.err.println("EventSupport clean up");
        if (notifiers != null) {
            for (NamingEventNotifier notifier : notifiers.values()) {
                notifier.stop();
            }
            notifiers = null;
        }
        if (eventQueue != null) {
            eventQueue.stop();
            eventQueue = null;
        }
        // %%% Should we fire NamingExceptionEvents to unsolicited listeners?
    }

    /*
     * The queue of events to be delivered.
     */
    private EventQueue eventQueue;

    /**
     * Add the event and vector of listeners to the queue to be delivered.
     * An event dispatcher thread dequeues events from the queue and dispatches
     * them to the registered listeners.
     * Package private; used by NamingEventNotifier to fire events
     */
    synchronized void queueEvent(EventObject event,
                                 Vector<? extends NamingListener> vector) {
        if (notifiers == null) {
            // That means cleanup() already done, not queue event anymore,
            // otherwise, new created EventQueue will not been cleanup.
            return;
        }
        if (eventQueue == null)
            eventQueue = new EventQueue();

        /*
         * Copy the vector in order to freeze the state of the set
         * of EventListeners the event should be delivered to prior
         * to delivery.  This ensures that any changes made to the
         * Vector from a target listener's method during the delivery
         * of this event will not take effect until after the event is
         * delivered.
         */
        @SuppressWarnings("unchecked") // clone()
        Vector<NamingListener> v =
                (Vector<NamingListener>)vector.clone();
        eventQueue.enqueue(event, v);
    }

    // No finalize() needed because EventSupport is always owned by
    // an LdapCtx. LdapCtx's finalize() and close() always call cleanup() so
    // there is no need for EventSupport to have a finalize().
}
