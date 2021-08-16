/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

 /*
   * This code is ported to XAWT from MAWT based on awt_mgrsel.c
   * code written originally by Valeriy Ushakov
   * Author : Bino George
   */

package sun.awt.X11;

import java.util.*;
import sun.util.logging.PlatformLogger;

public class  XMSelection {

    /*
     * A method for a subsystem to express its interest in a certain
     * manager selection.
     *
     * If owner changes, the ownerChanged of the XMSelectionListener
     * will be called with the screen
     * number and the new owning window when onwership is established, or
     * None if the owner is gone.
     *
     * Events in extra_mask are selected for on owning windows (exsiting
     * ones and on new owners when established) and otherEvent of the
     * XMWSelectionListener will be called with the screen number and an event.
     *
     * The function returns an array of current owners.  The size of the
     * array is ScreenCount(awt_display).  The array is "owned" by this
     * module and should be considered by the caller as read-only.
     */


    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XMSelection");
    /* Name of the selection */
    String selectionName;

    /* list of listeners to be called for events */
    Vector<XMSelectionListener> listeners;

    /* X atom array (one per screen) for this selection */
    XAtom[] atoms;

    /* Window ids of selection owners */
    long[] owners;

    /* event mask to set */
    long eventMask;

    static int numScreens;

    static XAtom XA_MANAGER;

    static HashMap<Long, XMSelection> selectionMap;

    static {
        long display = XToolkit.getDisplay();
        XToolkit.awtLock();
        try {
            numScreens = XlibWrapper.ScreenCount(display);
        } finally {
            XToolkit.awtUnlock();
        }
        XA_MANAGER = XAtom.get("MANAGER");
        for (int screen = 0; screen < numScreens ; screen ++) {
            initScreen(display,screen);
        }

        selectionMap = new HashMap<>();
    }

    static void initScreen(long display, final int screen) {
        XToolkit.awtLock();
        try {
            long root = XlibWrapper.RootWindow(display,screen);
            XWindowAttributes wattr = new XWindowAttributes();
            try {
                XlibWrapper.XGetWindowAttributes(display, root, wattr.pData);
                XlibWrapper.XSelectInput(display, root,
                        XConstants.StructureNotifyMask |
                        wattr.get_your_event_mask());
            } finally {
                wattr.dispose();
            }
            XToolkit.addEventDispatcher(root,
                    new XEventDispatcher() {
                        public void dispatchEvent(XEvent ev) {
                                processRootEvent(ev, screen);
                            }
                        });

        } finally {
            XToolkit.awtUnlock();
        }
    }


    public int getNumberOfScreens() {
        return numScreens;
    }

    void select(long extra_mask) {
        eventMask = extra_mask;
        for (int screen = 0; screen < numScreens ; screen ++) {
            selectPerScreen(screen,extra_mask);
        }
    }

    void resetOwner(long owner, final int screen) {
        XToolkit.awtLock();
        try {
            long display = XToolkit.getDisplay();
            synchronized(this) {
                setOwner(owner, screen);
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("New Selection Owner for screen " + screen + " = " + owner );
                }
                XlibWrapper.XSelectInput(display, owner, XConstants.StructureNotifyMask | eventMask);
                XToolkit.addEventDispatcher(owner,
                        new XEventDispatcher() {
                            public void dispatchEvent(XEvent ev) {
                                dispatchSelectionEvent(ev, screen);
                            }
                        });

            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    void selectPerScreen(final int screen, long extra_mask) {
        XToolkit.awtLock();
        try {
            try {
                long display = XToolkit.getDisplay();
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Grabbing XServer");
                }
                XlibWrapper.XGrabServer(display);

                synchronized(this) {
                    String selection_name = getName()+"_S"+screen;
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("Screen = " + screen + " selection name = " + selection_name);
                    }
                    XAtom atom = XAtom.get(selection_name);
                    selectionMap.put(Long.valueOf(atom.getAtom()),this); // add mapping from atom to the instance of XMSelection
                    setAtom(atom,screen);
                    long owner = XlibWrapper.XGetSelectionOwner(display, atom.getAtom());
                    if (owner != 0) {
                        setOwner(owner, screen);
                        if (log.isLoggable(PlatformLogger.Level.FINE)) {
                            log.fine("Selection Owner for screen " + screen + " = " + owner );
                        }
                        XlibWrapper.XSelectInput(display, owner, XConstants.StructureNotifyMask | extra_mask);
                        XToolkit.addEventDispatcher(owner,
                                new XEventDispatcher() {
                                        public void dispatchEvent(XEvent ev) {
                                            dispatchSelectionEvent(ev, screen);
                                        }
                                    });
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("UnGrabbing XServer");
                }
                XlibWrapper.XUngrabServer(XToolkit.getDisplay());
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }


    static boolean processClientMessage(XEvent xev, int screen) {
        XClientMessageEvent xce = xev.get_xclient();
        if (xce.get_message_type() == XA_MANAGER.getAtom()) {
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("client messags = " + xce);
            }
            long timestamp = xce.get_data(0) & 0xFFFFFFFFL;
            long atom = xce.get_data(1);
            long owner = xce.get_data(2);
            long data = xce.get_data(3);

            XMSelection sel = getInstance(atom);
            if (sel != null) {
                sel.resetOwner(owner,screen);
                sel.dispatchOwnerChangedEvent(xev,screen,owner,data, timestamp);
            }
        }
        return false;
    }

    static  boolean processRootEvent(XEvent xev, int screen) {
        switch (xev.get_type()) {
            case XConstants.ClientMessage: {
                return processClientMessage(xev, screen);
            }
        }

        return false;

    }


    static XMSelection getInstance(long selection) {
        return selectionMap.get(Long.valueOf(selection));
    }


    /*
     * Default constructor specifies PropertyChangeMask as well
     */

    public XMSelection (String selname) {
        this(selname, XConstants.PropertyChangeMask);
    }


   /*
    * Some users may not need to know about selection changes,
    * just owner ship changes, They would specify a zero extra mask.
    */

    public XMSelection (String selname, long extraMask) {

        synchronized (this) {
            selectionName = selname;
            atoms = new XAtom[getNumberOfScreens()];
            owners = new long[getNumberOfScreens()];
        }
        select(extraMask);
    }



    public synchronized void addSelectionListener(XMSelectionListener listener) {
        if (listeners == null) {
            listeners = new Vector<>();
        }
        listeners.add(listener);
    }

    public synchronized void removeSelectionListener(XMSelectionListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    synchronized Collection<XMSelectionListener> getListeners() {
        return listeners;
    }

    synchronized XAtom getAtom(int screen) {
        if (atoms != null) {
            return atoms[screen];
        }
        return null;
    }

    synchronized void setAtom(XAtom a, int screen) {
        if (atoms != null) {
            atoms[screen] = a;
        }
    }

    synchronized long getOwner(int screen) {
        if (owners != null) {
            return owners[screen];
        }
        return 0;
    }

    synchronized void setOwner(long owner, int screen) {
        if (owners != null) {
            owners[screen] = owner;
        }
    }

    synchronized String getName() {
        return selectionName;
    }


    synchronized void dispatchSelectionChanged( XPropertyEvent ev, int screen) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Selection Changed : Screen = " + screen + "Event =" + ev);
        }
        if (listeners != null) {
            Iterator<XMSelectionListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                XMSelectionListener disp = iter.next();
                disp.selectionChanged(screen, this, ev.get_window(), ev);
            }
        }
    }

    synchronized void dispatchOwnerDeath(XDestroyWindowEvent de, int screen) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Owner dead : Screen = " + screen + "Event =" + de);
        }
        if (listeners != null) {
            Iterator<XMSelectionListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                XMSelectionListener disp = iter.next();
                disp.ownerDeath(screen, this, de.get_window());

            }
        }
    }

    void dispatchSelectionEvent(XEvent xev, int screen) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Event =" + xev);
        }
        if (xev.get_type() == XConstants.DestroyNotify) {
            XDestroyWindowEvent de = xev.get_xdestroywindow();
            dispatchOwnerDeath( de, screen);
        }
        else if (xev.get_type() == XConstants.PropertyNotify)  {
            XPropertyEvent xpe = xev.get_xproperty();
            dispatchSelectionChanged( xpe, screen);
        }
    }


    synchronized void dispatchOwnerChangedEvent(XEvent ev, int screen, long owner, long data, long timestamp) {
        if (listeners != null) {
            Iterator<XMSelectionListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                XMSelectionListener disp = iter.next();
                disp.ownerChanged(screen,this, owner, data, timestamp);
            }
        }
    }


}
