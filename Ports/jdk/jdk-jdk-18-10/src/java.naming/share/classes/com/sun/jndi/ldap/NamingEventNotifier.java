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

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.event.*;
import javax.naming.ldap.*;
import javax.naming.ldap.LdapName;

import java.util.Vector;
import com.sun.jndi.toolkit.ctx.Continuation;

/**
  * Gathers information to generate events by using the Persistent Search
  * control.
  *<p>
  * This class maintains a list of listeners all interested in the same
  * "search" request. It creates a thread that does the persistent search
  * and blocks, collecting the results of the search.
  * For each result that it receives from the search, it fires the
  * corresponding event to its listeners. If an exception is encountered,
  * it fires a NamingExceptionEvent.
  *
  * @author Rosanna Lee
  */
final class NamingEventNotifier implements Runnable {
    private static final boolean debug = false;

    private Vector<NamingListener> namingListeners;
    private Thread worker;
    private LdapCtx context;
    private EventContext eventSrc;
    private EventSupport support;
    private NamingEnumeration<SearchResult> results;

    // package private; used by EventSupport to remove it
    NotifierArgs info;

    NamingEventNotifier(EventSupport support, LdapCtx ctx, NotifierArgs info,
        NamingListener firstListener) throws NamingException {
        this.info = info;
        this.support = support;

        Control psearch;
        try {
            psearch = new PersistentSearchControl(
                info.mask,
                true /* no info about original entry(s) */,
                true /* additional info about changes */,
                Control.CRITICAL);
        } catch (java.io.IOException e) {
            NamingException ne = new NamingException(
                "Problem creating persistent search control");
            ne.setRootCause(e);
            throw ne;
        }

        // Add psearch control to existing list
        context = (LdapCtx)ctx.newInstance(new Control[]{psearch});
        eventSrc = ctx;

        namingListeners = new Vector<>();
        namingListeners.addElement(firstListener);

        worker = Obj.helper.createThread(this);
        worker.setDaemon(true);  // not a user thread
        worker.start();
    }

    // package private; used by EventSupport; namingListener already synchronized
    void addNamingListener(NamingListener l) {
        namingListeners.addElement(l);
    }

    // package private; used by EventSupport; namingListener already synchronized
    void removeNamingListener(NamingListener l) {
        namingListeners.removeElement(l);
    }

    // package private; used by EventSupport; namingListener already synchronized
    boolean hasNamingListeners() {
        return namingListeners.size() > 0;
    }

    /**
     * Execute "persistent search".
     * For each result, create the appropriate NamingEvent and
     * queue to be dispatched to listeners.
     */
    public void run() {
        try {
            Continuation cont = new Continuation();
            cont.setError(this, info.name);
            Name nm = (info.name == null || info.name.isEmpty()) ?
                new CompositeName() : new CompositeName().add(info.name);

            results = context.searchAux(nm, info.filter, info.controls,
                true, false, cont);

            // Change root of search results so that it will generate
            // names relative to the event context instead of that
            // named by nm
            ((LdapSearchEnumeration)(NamingEnumeration)results)
                    .setStartName(context.currentParsedDN);

            SearchResult si;
            Control[] respctls;
            EntryChangeResponseControl ec;
            long changeNum;

            while (results.hasMore()) {
                si = results.next();
                respctls = (si instanceof HasControls) ?
                    ((HasControls) si).getControls() : null;

                if (debug) {
                    System.err.println("notifier: " + si);
                    System.err.println("respCtls: " + respctls);
                }

                // Just process ECs; ignore all the rest
                if (respctls != null) {
                    for (int i = 0; i < respctls.length; i++) {
                        // %%% Should be checking OID instead of class
                        // %%% in case using someone else's  EC ctl
                        if (respctls[i] instanceof EntryChangeResponseControl) {
                            ec = (EntryChangeResponseControl)respctls[i];
                            changeNum = ec.getChangeNumber();
                            switch (ec.getChangeType()) {
                            case EntryChangeResponseControl.ADD:
                                fireObjectAdded(si, changeNum);
                                break;
                            case EntryChangeResponseControl.DELETE:
                                fireObjectRemoved(si, changeNum);
                                break;
                            case EntryChangeResponseControl.MODIFY:
                                fireObjectChanged(si, changeNum);
                                break;
                            case EntryChangeResponseControl.RENAME:
                                fireObjectRenamed(si, ec.getPreviousDN(),
                                    changeNum);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        } catch (InterruptedNamingException e) {
            if (debug) System.err.println("NamingEventNotifier Interrupted");
        } catch (NamingException e) {
            // Fire event to notify NamingExceptionEvent listeners
            fireNamingException(e);

            // This notifier is no longer valid
            support.removeDeadNotifier(info);
        } finally {
            cleanup();
        }
        if (debug) System.err.println("NamingEventNotifier finished");
    }

    private void cleanup() {
        if (debug) System.err.println("NamingEventNotifier cleanup");

        try {
            if (results != null) {
                if (debug) System.err.println("NamingEventNotifier enum closing");
                results.close(); // this will abandon the search
                results = null;
            }
            if (context != null) {
                if (debug) System.err.println("NamingEventNotifier ctx closing");
                context.close();
                context = null;
            }
        } catch (NamingException e) {}
    }

    /**
     * Stop the dispatcher so we can be destroyed.
     * package private; used by EventSupport
     */
    void stop() {
        if (debug) System.err.println("NamingEventNotifier being stopping");
        if (worker != null) {
            worker.interrupt(); // kill our thread
            worker = null;
        }
    }

    /**
     * Fire an "object added" event to registered NamingListeners.
     */
    private void fireObjectAdded(Binding newBd, long changeID) {
        if (namingListeners == null || namingListeners.size() == 0)
            return;

        NamingEvent e = new NamingEvent(eventSrc, NamingEvent.OBJECT_ADDED,
            newBd, null, changeID);
        support.queueEvent(e, namingListeners);
    }

    /**
     * Fire an "object removed" event to registered NamingListeners.
     */
    private void fireObjectRemoved(Binding oldBd, long changeID) {
        if (namingListeners == null || namingListeners.size() == 0)
            return;

        NamingEvent e = new NamingEvent(eventSrc, NamingEvent.OBJECT_REMOVED,
            null, oldBd, changeID);
        support.queueEvent(e, namingListeners);
    }

    /**
     * Fires an "object changed" event to registered NamingListeners.
     */
    private void fireObjectChanged(Binding newBd, long changeID) {
        if (namingListeners == null || namingListeners.size() == 0)
            return;

        // Name hasn't changed; construct old binding using name from new binding
        Binding oldBd = new Binding(newBd.getName(), null, newBd.isRelative());

        NamingEvent e = new NamingEvent(
            eventSrc, NamingEvent.OBJECT_CHANGED, newBd, oldBd, changeID);
        support.queueEvent(e, namingListeners);
    }

    /**
     * Fires an "object renamed" to registered NamingListeners.
     */
    private void fireObjectRenamed(Binding newBd, String oldDN, long changeID) {
        if (namingListeners == null || namingListeners.size() == 0)
            return;

        Binding oldBd = null;
        try {
            LdapName dn = new LdapName(oldDN);
            if (dn.startsWith(context.currentParsedDN)) {
                String relDN = dn.getSuffix(context.currentParsedDN.size()).toString();
                oldBd = new Binding(relDN, null);
            }
        } catch (NamingException e) {}

        if (oldBd == null) {
            oldBd = new Binding(oldDN, null, false /* not relative name */);
        }

        NamingEvent e = new NamingEvent(
            eventSrc, NamingEvent.OBJECT_RENAMED, newBd, oldBd, changeID);
        support.queueEvent(e, namingListeners);
    }

    private void fireNamingException(NamingException e) {
        if (namingListeners == null || namingListeners.size() == 0)
            return;

        NamingExceptionEvent evt = new NamingExceptionEvent(eventSrc, e);
        support.queueEvent(evt, namingListeners);
    }
}
