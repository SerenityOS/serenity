/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.io.Serial;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;

import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

/**
 * A mechanism for ensuring that a series of AWTEvents are executed in a
 * precise order, even across multiple AppContexts. The nested events will be
 * dispatched in the order in which their wrapping SequencedEvents were
 * constructed. The only exception to this rule is if the peer of the target of
 * the nested event was destroyed (with a call to Component.removeNotify)
 * before the wrapping SequencedEvent was able to be dispatched. In this case,
 * the nested event is never dispatched.
 *
 * @author David Mendenhall
 */
@SuppressWarnings("removal")
class SequencedEvent extends AWTEvent implements ActiveEvent {

    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 547742659238625067L;

    private static final int ID =
        java.awt.event.FocusEvent.FOCUS_LAST + 1;
    private static final LinkedList<SequencedEvent> list = new LinkedList<>();

    private final AWTEvent nested;
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private AppContext appContext;
    private boolean disposed;
    private final LinkedList<AWTEvent> pendingEvents = new LinkedList<>();

    private static boolean fxAppThreadIsDispatchThread;
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Thread fxCheckSequenceThread;
    static {
        AWTAccessor.setSequencedEventAccessor(new AWTAccessor.SequencedEventAccessor() {
            public AWTEvent getNested(AWTEvent sequencedEvent) {
                return ((SequencedEvent)sequencedEvent).nested;
            }
            public boolean isSequencedEvent(AWTEvent event) {
                return event instanceof SequencedEvent;
            }

            public AWTEvent create(AWTEvent event) {
                return new SequencedEvent(event);
            }
        });
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                fxAppThreadIsDispatchThread =
                        "true".equals(System.getProperty("javafx.embed.singleThread"));
                return null;
            }
        });
    }

    private static final class SequencedEventsFilter implements EventFilter {
        private final SequencedEvent currentSequencedEvent;
        private SequencedEventsFilter(SequencedEvent currentSequencedEvent) {
            this.currentSequencedEvent = currentSequencedEvent;
        }
        @Override
        public FilterAction acceptEvent(AWTEvent ev) {
            if (ev.getID() == ID) {
                // Move forward dispatching only if the event is previous
                // in SequencedEvent.list. Otherwise, hold it for reposting later.
                synchronized (SequencedEvent.class) {
                    for (SequencedEvent iev : list) {
                        if (iev.equals(currentSequencedEvent)) {
                            break;
                        } else if (iev.equals(ev)) {
                            return FilterAction.ACCEPT;
                        }
                    }
                }
            } else if (ev.getID() == SentEvent.ID) {
                return FilterAction.ACCEPT;
            }
            currentSequencedEvent.pendingEvents.add(ev);
            return FilterAction.REJECT;
        }
    }

    /**
     * Constructs a new SequencedEvent which will dispatch the specified
     * nested event.
     *
     * @param nested the AWTEvent which this SequencedEvent's dispatch()
     *        method will dispatch
     */
    public SequencedEvent(AWTEvent nested) {
        super(nested.getSource(), ID);
        this.nested = nested;
        // All AWTEvents that are wrapped in SequencedEvents are (at
        // least currently) implicitly generated by the system
        SunToolkit.setSystemGenerated(nested);

        if (fxAppThreadIsDispatchThread) {
            fxCheckSequenceThread = new Thread() {
                @Override
                public void run() {
                    while(!isFirstOrDisposed()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            };
        }
        synchronized (SequencedEvent.class) {
            list.add(this);
        }
    }

    /**
     * Dispatches the nested event after all previous nested events have been
     * dispatched or disposed. If this method is invoked before all previous nested events
     * have been dispatched, then this method blocks until such a point is
     * reached.
     * While waiting disposes nested events to disposed AppContext
     *
     * NOTE: Locking protocol.  Since dispose() can get EventQueue lock,
     * dispatch() shall never call dispose() while holding the lock on the list,
     * as EventQueue lock is held during dispatching.  The locks should be acquired
     * in the same order.
     */
    public final void dispatch() {
        try {
            appContext = AppContext.getAppContext();

            if (getFirst() != this) {
                if (EventQueue.isDispatchThread()) {
                    if (Thread.currentThread() instanceof EventDispatchThread) {
                        EventDispatchThread edt = (EventDispatchThread)
                                Thread.currentThread();
                        edt.pumpEventsForFilter(() -> !SequencedEvent.this.isFirstOrDisposed(),
                                new SequencedEventsFilter(this));
                    } else {
                        if (fxAppThreadIsDispatchThread) {
                            fxCheckSequenceThread.start();
                            try {
                                // check if event is dispatched or disposed
                                // but since this user app thread is same as
                                // dispatch thread in fx when run with
                                // javafx.embed.singleThread=true
                                // we do not wait infinitely to avoid deadlock
                                // as dispatch will ultimately be done by this thread
                                fxCheckSequenceThread.join(500);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } else {
                    while(!isFirstOrDisposed()) {
                        synchronized (SequencedEvent.class) {
                            try {
                                SequencedEvent.class.wait(1000);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }
            }

            if (!disposed) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    setCurrentSequencedEvent(this);
                Toolkit.getEventQueue().dispatchEvent(nested);
            }
        } finally {
            dispose();
        }
    }

    /**
     * true only if event exists and nested source appContext is disposed.
     */
    private static final boolean isOwnerAppContextDisposed(SequencedEvent se) {
        if (se != null) {
            Object target = se.nested.getSource();
            if (target instanceof Component) {
                return ((Component)target).appContext.isDisposed();
            }
        }
        return false;
    }

    /**
     * Sequenced events are dispatched in order, so we cannot dispatch
     * until we are the first sequenced event in the queue (i.e. it's our
     * turn).  But while we wait for our turn to dispatch, the event
     * could have been disposed for a number of reasons.
     */
    public final boolean isFirstOrDisposed() {
        if (disposed) {
            return true;
        }
        // getFirstWithContext can dispose this
        return this == getFirstWithContext() || disposed;
    }

    private static final synchronized SequencedEvent getFirst() {
        return list.getFirst();
    }

    /* Disposes all events from disposed AppContext
     * return first valid event
     */
    private static final SequencedEvent getFirstWithContext() {
        SequencedEvent first = getFirst();
        while(isOwnerAppContextDisposed(first)) {
            first.dispose();
            first = getFirst();
        }
        return first;
    }

    /**
     * Disposes of this instance. This method is invoked once the nested event
     * has been dispatched and handled, or when the peer of the target of the
     * nested event has been disposed with a call to Component.removeNotify.
     *
     * NOTE: Locking protocol.  Since SunToolkit.postEvent can get EventQueue lock,
     * it shall never be called while holding the lock on the list,
     * as EventQueue lock is held during dispatching and dispatch() will get
     * lock on the list. The locks should be acquired in the same order.
     */
    final void dispose() {
      synchronized (SequencedEvent.class) {
            if (disposed) {
                return;
            }
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    getCurrentSequencedEvent() == this) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    setCurrentSequencedEvent(null);
            }
            disposed = true;
        }

        SequencedEvent next = null;

        synchronized (SequencedEvent.class) {
          SequencedEvent.class.notifyAll();

          if (list.getFirst() == this) {
              list.removeFirst();

              if (!list.isEmpty()) {
                    next = list.getFirst();
              }
          } else {
              list.remove(this);
          }
      }
        // Wake up waiting threads
        if (next != null && next.appContext != null) {
            SunToolkit.postEvent(next.appContext, new SentEvent());
        }

        for(AWTEvent e : pendingEvents) {
            SunToolkit.postEvent(appContext, e);
        }
    }
}
