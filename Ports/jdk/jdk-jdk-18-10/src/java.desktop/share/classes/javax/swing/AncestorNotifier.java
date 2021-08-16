/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;


import javax.swing.event.*;
import java.awt.event.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.io.Serializable;


/**
 * @author Dave Moore
 */

@SuppressWarnings("serial")
class AncestorNotifier implements ComponentListener, PropertyChangeListener, Serializable
{
    transient Component firstInvisibleAncestor;
    EventListenerList listenerList = new EventListenerList();
    JComponent root;

    AncestorNotifier(JComponent root) {
        this.root = root;
        addListeners(root, true);
    }

    void addAncestorListener(AncestorListener l) {
        listenerList.add(AncestorListener.class, l);
    }

    void removeAncestorListener(AncestorListener l) {
        listenerList.remove(AncestorListener.class, l);
    }

    AncestorListener[] getAncestorListeners() {
        return listenerList.getListeners(AncestorListener.class);
    }

    /**
     * Notify all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireAncestorAdded(JComponent source, int id, Container ancestor, Container ancestorParent) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==AncestorListener.class) {
                // Lazily create the event:
                AncestorEvent ancestorEvent =
                    new AncestorEvent(source, id, ancestor, ancestorParent);
                ((AncestorListener)listeners[i+1]).ancestorAdded(ancestorEvent);
            }
        }
    }

    /**
     * Notify all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireAncestorRemoved(JComponent source, int id, Container ancestor, Container ancestorParent) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==AncestorListener.class) {
                // Lazily create the event:
                AncestorEvent ancestorEvent =
                    new AncestorEvent(source, id, ancestor, ancestorParent);
                ((AncestorListener)listeners[i+1]).ancestorRemoved(ancestorEvent);
            }
        }
    }
    /**
     * Notify all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireAncestorMoved(JComponent source, int id, Container ancestor, Container ancestorParent) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==AncestorListener.class) {
                // Lazily create the event:
                AncestorEvent ancestorEvent =
                    new AncestorEvent(source, id, ancestor, ancestorParent);
                ((AncestorListener)listeners[i+1]).ancestorMoved(ancestorEvent);
            }
        }
    }

    void removeAllListeners() {
        removeListeners(root);
    }

    void addListeners(Component ancestor, boolean addToFirst) {
        Component a;

        firstInvisibleAncestor = null;
        for (a = ancestor;
             firstInvisibleAncestor == null;
             a = a.getParent()) {
            if (addToFirst || a != ancestor) {
                a.addComponentListener(this);

                if (a instanceof JComponent) {
                    JComponent jAncestor = (JComponent)a;

                    jAncestor.addPropertyChangeListener(this);
                }
            }
            if (!a.isVisible() || a.getParent() == null || a instanceof Window) {
                firstInvisibleAncestor = a;
            }
        }
        if (firstInvisibleAncestor instanceof Window &&
            firstInvisibleAncestor.isVisible()) {
            firstInvisibleAncestor = null;
        }
    }

    void removeListeners(Component ancestor) {
        Component a;
        for (a = ancestor; a != null; a = a.getParent()) {
            a.removeComponentListener(this);
            if (a instanceof JComponent) {
                JComponent jAncestor = (JComponent)a;
                jAncestor.removePropertyChangeListener(this);
            }
            if (a == firstInvisibleAncestor || a instanceof Window) {
                break;
            }
        }
    }

    public void componentResized(ComponentEvent e) {}

    public void componentMoved(ComponentEvent e) {
        Component source = e.getComponent();

        fireAncestorMoved(root, AncestorEvent.ANCESTOR_MOVED,
                          (Container)source, source.getParent());
    }

    public void componentShown(ComponentEvent e) {
        Component ancestor = e.getComponent();

        if (ancestor == firstInvisibleAncestor) {
            addListeners(ancestor, false);
            if (firstInvisibleAncestor == null) {
                fireAncestorAdded(root, AncestorEvent.ANCESTOR_ADDED,
                                  (Container)ancestor, ancestor.getParent());
            }
        }
    }

    public void componentHidden(ComponentEvent e) {
        Component ancestor = e.getComponent();
        boolean needsNotify = firstInvisibleAncestor == null;

        if ( !(ancestor instanceof Window) ) {
            removeListeners(ancestor.getParent());
        }
        firstInvisibleAncestor = ancestor;
        if (needsNotify) {
            fireAncestorRemoved(root, AncestorEvent.ANCESTOR_REMOVED,
                                (Container)ancestor, ancestor.getParent());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String s = evt.getPropertyName();

        if (s!=null && (s.equals("parent") || s.equals("ancestor"))) {
            JComponent component = (JComponent)evt.getSource();

            if (evt.getNewValue() != null) {
                if (component == firstInvisibleAncestor) {
                    addListeners(component, false);
                    if (firstInvisibleAncestor == null) {
                        fireAncestorAdded(root, AncestorEvent.ANCESTOR_ADDED,
                                          component, component.getParent());
                    }
                }
            } else {
                boolean needsNotify = firstInvisibleAncestor == null;
                Container oldParent = (Container)evt.getOldValue();

                removeListeners(oldParent);
                firstInvisibleAncestor = component;
                if (needsNotify) {
                    fireAncestorRemoved(root, AncestorEvent.ANCESTOR_REMOVED,
                                        component, oldParent);
                }
            }
        }
    }
}
