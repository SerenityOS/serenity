/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.X11;

import java.awt.*;

import java.awt.event.ComponentEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import sun.awt.IconInfo;
import sun.util.logging.PlatformLogger;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;

abstract class XDecoratedPeer extends XWindowPeer {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XDecoratedPeer");
    private static final PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XDecoratedPeer");
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.X11.focus.XDecoratedPeer");
    private static final PlatformLogger iconLog = PlatformLogger.getLogger("sun.awt.X11.icon.XDecoratedPeer");

    // Set to true when we get the first ConfigureNotify after being
    // reparented - indicates that WM has adopted the top-level.
    boolean configure_seen;
    boolean insets_corrected;

    XIconWindow iconWindow;
    volatile WindowDimensions dimensions;
    XContentWindow content;
    volatile Insets currentInsets;
    XFocusProxyWindow focusProxy;
    static final Map<Class<?>,Insets> lastKnownInsets =
                                   Collections.synchronizedMap(new HashMap<>());

    XDecoratedPeer(Window target) {
        super(target);
    }

    XDecoratedPeer(XCreateWindowParams params) {
        super(params);
    }

    public long getShell() {
        return window;
    }

    public long getContentWindow() {
        return (content == null) ? window : content.getWindow();
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        winAttr.initialFocus = true;

        currentInsets = new Insets(0,0,0,0);
        if (XWM.getWMID() == XWM.UNITY_COMPIZ_WM) {
            currentInsets = lastKnownInsets.get(getClass());
        }
        applyGuessedInsets();

        Rectangle bounds = (Rectangle)params.get(BOUNDS);
        dimensions = new WindowDimensions(bounds, getRealInsets(), false);
        params.put(BOUNDS, dimensions.getClientRect());
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Initial dimensions {0}", dimensions);
        }

        // Deny default processing of these events on the shell - proxy will take care of
        // them instead
        Long eventMask = (Long)params.get(EVENT_MASK);
        params.add(EVENT_MASK, Long.valueOf(eventMask.longValue() & ~(XConstants.FocusChangeMask | XConstants.KeyPressMask | XConstants.KeyReleaseMask)));
    }

    void postInit(XCreateWindowParams params) {
        // The size hints must be set BEFORE mapping the window (see 6895647)
        updateSizeHints(dimensions);

        // The super method maps the window if it's visible on the shared level
        super.postInit(params);

        // The lines that follow need to be in a postInit, so they
        // happen after the X window is created.
        setResizable(winAttr.initialResizability);
        XWM.requestWMExtents(getWindow());

        content = XContentWindow.createContent(this);

        if (warningWindow != null) {
            warningWindow.toFront();
        }
        focusProxy = createFocusProxy();
    }

    void setIconHints(java.util.List<IconInfo> icons) {
        if (!XWM.getWM().setNetWMIcon(this, icons)) {
            if (icons.size() > 0) {
                if (iconWindow == null) {
                    iconWindow = new XIconWindow(this);
                }
                iconWindow.setIconImages(icons);
            }
        }
    }

    public void updateMinimumSize() {
        super.updateMinimumSize();
        XToolkit.awtLock();
        try {
            updateMinSizeHints();
        } finally {
            XToolkit.awtUnlock();
        }
    }

    private void updateMinSizeHints() {
        if (isResizable()) {
            Dimension minimumSize = getTargetMinimumSize();
            if (minimumSize != null) {
                Insets insets = getRealInsets();
                int minWidth = minimumSize.width - insets.left - insets.right;
                int minHeight = minimumSize.height - insets.top - insets.bottom;
                if (minWidth < 0) minWidth = 0;
                if (minHeight < 0) minHeight = 0;
                setSizeHints(XUtilConstants.PMinSize | (isLocationByPlatform()?0:(XUtilConstants.PPosition | XUtilConstants.USPosition)),
                             getX(), getY(), minWidth, minHeight);
                if (isVisible()) {
                    Rectangle bounds = getShellBounds();
                    int nw = (bounds.width < minWidth) ? minWidth : bounds.width;
                    int nh = (bounds.height < minHeight) ? minHeight : bounds.height;
                    if (nw != bounds.width || nh != bounds.height) {
                        setShellSize(new Rectangle(0, 0, nw, nh));
                    }
                }
            } else {
                boolean isMinSizeSet = isMinSizeSet();
                XWM.removeSizeHints(this, XUtilConstants.PMinSize);
                /* Some WMs need remap to redecorate the window */
                if (isMinSizeSet && isShowing() && XWM.needRemap(this)) {
                    /*
                     * Do the re/mapping at the Xlib level.  Since we essentially
                     * work around a WM bug we don't want this hack to be exposed
                     * to Intrinsics (i.e. don't mess with grabs, callbacks etc).
                     */
                    xSetVisible(false);
                    XToolkit.XSync();
                    xSetVisible(true);
                }
            }
        }
    }

    XFocusProxyWindow createFocusProxy() {
        return new XFocusProxyWindow(this);
    }

    protected XAtomList getWMProtocols() {
        XAtomList protocols = super.getWMProtocols();
        protocols.add(wm_delete_window);
        protocols.add(wm_take_focus);
        return protocols;
    }

    public Graphics getGraphics() {
        AWTAccessor.ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();
        return getGraphics(content.surfaceData,
                           compAccessor.getForeground(target),
                           compAccessor.getBackground(target),
                           compAccessor.getFont(target));
    }

    public void setTitle(String title) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Title is " + title);
        }
        XToolkit.awtLock();
        try {
            winAttr.title = title;
            updateWMName();
        } finally {
            XToolkit.awtUnlock();
        }
    }

    protected String getWMName() {
        if (winAttr.title == null || winAttr.title.trim().isEmpty()) {
            return " ";
        } else {
            return winAttr.title;
        }
    }

    void updateWMName() {
        XToolkit.awtLock();
        try {
            super.updateWMName();
            String name = getWMName();
            if (name == null || name.trim().isEmpty()) {
                name = "Java";
            }
            XAtom iconNameAtom = XAtom.get(XAtom.XA_WM_ICON_NAME);
            iconNameAtom.setProperty(getWindow(), name);
            XAtom netIconNameAtom = XAtom.get("_NET_WM_ICON_NAME");
            netIconNameAtom.setPropertyUTF8(getWindow(), name);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void handleIconify() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_ICONIFIED));
    }

    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void handleDeiconify() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_DEICONIFIED));
    }

    public void handleFocusEvent(XEvent xev) {
        super.handleFocusEvent(xev);
        XFocusChangeEvent xfe = xev.get_xfocus();

        // If we somehow received focus events forward it instead to proxy
        // FIXME: Shouldn't we instead check for inferrior?
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Received focus event on shell: " + xfe);
        }
//         focusProxy.xRequestFocus();
   }

/***************************************************************************************
 *                             I N S E T S   C O D E
 **************************************************************************************/

    protected boolean isInitialReshape() {
        return false;
    }

    private static Insets difference(Insets i1, Insets i2) {
        return new Insets(i1.top-i2.top, i1.left - i2.left, i1.bottom-i2.bottom, i1.right-i2.right);
    }

    private static boolean isNull(Insets i) {
        return (i == null) || ((i.left | i.top | i.right | i.bottom) == 0);
    }

    private static Insets copy(Insets i) {
        return new Insets(i.top, i.left, i.bottom, i.right);
    }

    private Insets copyAndScaleDown(Insets i) {
        return new Insets(scaleDown(i.top), scaleDown(i.left),
                          scaleDown(i.bottom), scaleDown(i.right));
    }


    // insets which we get from WM (e.g from _NET_FRAME_EXTENTS)
    private Insets wm_set_insets;

    private Insets getWMSetInsets(XAtom changedAtom) {
        if (isEmbedded()) {
            return null;
        }

        if (wm_set_insets != null) {
            return wm_set_insets;
        }

        if (changedAtom == null) {
            wm_set_insets = XWM.getInsetsFromExtents(getWindow());
        } else {
            wm_set_insets = XWM.getInsetsFromProp(getWindow(), changedAtom);
        }

        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("FRAME_EXTENTS: {0}", wm_set_insets);
        }

        if (wm_set_insets != null) {
            wm_set_insets = copyAndScaleDown(wm_set_insets);
        }
        return wm_set_insets;
    }

    private void resetWMSetInsets() {
        if (XWM.getWMID() != XWM.UNITY_COMPIZ_WM) {
            currentInsets = new Insets(0, 0, 0, 0);
            wm_set_insets = null;
        } else {
            insets_corrected = false;
        }
    }

    public void handlePropertyNotify(XEvent xev) {
        super.handlePropertyNotify(xev);

        XPropertyEvent ev = xev.get_xproperty();
        if( !insets_corrected && isReparented() &&
                                         XWM.getWMID() == XWM.UNITY_COMPIZ_WM) {
            int state = XWM.getWM().getState(this);
            if ((state & Frame.MAXIMIZED_BOTH) ==  Frame.MAXIMIZED_BOTH) {
                // Stop ignoring ConfigureNotify because no extents will be sent
                // by WM for initially maximized decorated window.
                // Re-request window bounds to ensure actual dimensions and
                // notify the target with the initial size.
                insets_corrected = true;
                XlibWrapper.XConfigureWindow(XToolkit.getDisplay(),
                                                             getWindow(), 0, 0);
            }
        }
        if (ev.get_atom() == XWM.XA_KDE_NET_WM_FRAME_STRUT.getAtom()
            || ev.get_atom() == XWM.XA_NET_FRAME_EXTENTS.getAtom())
        {
            if (XWM.getWMID() != XWM.UNITY_COMPIZ_WM) {
                if (getMWMDecorTitleProperty().isPresent()) {
                    // Insets might have changed "in-flight" if that property
                    // is present, so we need to get the actual values of
                    // insets from the WM and propagate them through all the
                    // proper channels.
                    wm_set_insets = null;
                    Insets in = getWMSetInsets(XAtom.get(ev.get_atom()));
                    if (in != null && !in.equals(dimensions.getInsets())) {
                        handleCorrectInsets(in);
                    }
                } else {
                    getWMSetInsets(XAtom.get(ev.get_atom()));
                }
            } else {
                if (!isReparented()) {
                    return;
                }
                wm_set_insets = null;
                Insets in = getWMSetInsets(XAtom.get(ev.get_atom()));
                if (isNull(in)) {
                    return;
                }
                if (!isEmbedded() && !isTargetUndecorated()) {
                    lastKnownInsets.put(getClass(), in);
                }
                if (!in.equals(dimensions.getInsets())) {
                    if (insets_corrected || isMaximized()) {
                        currentInsets = in;
                        insets_corrected = true;
                        // insets were changed by WM. To handle this situation
                        // re-request window bounds because the current
                        // dimensions may be not actual as well.
                        XlibWrapper.XConfigureWindow(XToolkit.getDisplay(),
                                                             getWindow(), 0, 0);
                    } else {
                        // recalculate dimensions when window is just created
                        // and the initially guessed insets were wrong
                        handleCorrectInsets(in);
                    }
                } else if (!insets_corrected || !dimensions.isClientSizeSet()) {
                    insets_corrected = true;
                    // initial insets were guessed correctly. Re-request
                    // frame bounds because they may be changed by WM if the
                    // initial window position overlapped desktop's toolbars.
                    // This should initiate the final ConfigureNotify upon which
                    // the target will be notified with the final size.
                    XlibWrapper.XConfigureWindow(XToolkit.getDisplay(),
                                                             getWindow(), 0, 0);
                }
            }
        }
    }

    long reparent_serial = 0;

    public void handleReparentNotifyEvent(XEvent xev) {
        XReparentEvent  xe = xev.get_xreparent();
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine(xe.toString());
        }
        reparent_serial = xe.get_serial();
        long root = XlibWrapper.RootWindow(XToolkit.getDisplay(), getScreenNumber());

        if (isEmbedded()) {
            setReparented(true);
            insets_corrected = true;
            return;
        }
        if (getDecorations() == XWindowAttributesData.AWT_DECOR_NONE) {
            setReparented(true);
            insets_corrected = true;
            reshape(dimensions, SET_SIZE, false);
        } else if (xe.get_parent() == root) {
            configure_seen = false;
            insets_corrected = false;

            /*
             * We can be repareted to root for two reasons:
             *   . setVisible(false)
             *   . WM exited
             */
            if (isVisible()) { /* WM exited */
                /* Work around 4775545 */
                XWM.getWM().unshadeKludge(this);
                insLog.fine("- WM exited");
            } else {
                insLog.fine(" - reparent due to hide");
            }
        } else { /* reparented to WM frame, figure out our insets */
            setReparented(true);
            insets_corrected = false;
            if (XWM.getWMID() == XWM.UNITY_COMPIZ_WM) {
                return;
            }

            // Check if we have insets provided by the WM
            Insets correctWM = getWMSetInsets(null);
            if (correctWM != null) {
                if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                    insLog.finer("wm-provided insets {0}", correctWM);
                }
                // If these insets are equal to our current insets - no actions are necessary
                Insets dimInsets = dimensions.getInsets();
                if (correctWM.equals(dimInsets)) {
                    insLog.finer("Insets are the same as estimated - no additional reshapes necessary");
                    no_reparent_artifacts = true;
                    insets_corrected = true;
                    applyGuessedInsets();
                    return;
                }
            } else {
                correctWM = XWM.getWM().getInsets(this, xe.get_window(), xe.get_parent());
                if (correctWM != null) {
                    correctWM = copyAndScaleDown(correctWM);
                }

                if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                    if (correctWM != null) {
                        insLog.finer("correctWM {0}", correctWM);
                    } else {
                        insLog.finer("correctWM insets are not available, waiting for configureNotify");
                    }
                }
            }

            if (correctWM != null) {
                handleCorrectInsets(correctWM);
            }
        }
    }

    private void handleCorrectInsets(Insets correctWM) {
        /*
         * Ok, now see if we need adjust window size because
         * initial insets were wrong (most likely they were).
         */
        Insets correction = difference(correctWM, currentInsets);
        if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
            insLog.finest("Corrention {0}", correction);
        }
        if (!isNull(correction)) {
            currentInsets = copy(correctWM);
            applyGuessedInsets();

            //Fix for 6318109: PIT: Min Size is not honored properly when a
            //smaller size is specified in setSize(), XToolkit
            //update minimum size hints
            updateMinSizeHints();
        }
        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Dimensions before reparent: " + dimensions);
        }
        WindowDimensions newDimensions = new WindowDimensions(dimensions);
        newDimensions.setInsets(getRealInsets());
        dimensions = newDimensions;
        insets_corrected = true;

        if (isMaximized()) {
            return;
        }

        /*
         * If this window has been sized by a pack() we need
         * to keep the interior geometry intact.  Since pack()
         * computed width and height with wrong insets, we
         * must adjust the target dimensions appropriately.
         */
        if ((getHints().get_flags() & (XUtilConstants.USPosition | XUtilConstants.PPosition)) != 0) {
            reshape(dimensions, SET_BOUNDS, false);
        } else {
            reshape(dimensions, SET_SIZE, false);
        }
    }

    void handleMoved(WindowDimensions dims) {
        Point loc = dims.getLocation();
        AWTAccessor.getComponentAccessor().setLocation(target, loc.x, loc.y);
        postEvent(new ComponentEvent(target, ComponentEvent.COMPONENT_MOVED));
    }


    private Insets guessInsets() {
        if (isEmbedded() || isTargetUndecorated()) {
            return new Insets(0, 0, 0, 0);
        } else {
            if (!isNull(currentInsets)) {
                /* insets were set on wdata by System Properties */
                return copy(currentInsets);
            } else {
                Insets res = getWMSetInsets(null);
                if (res == null) {
                    res = XWM.getWM().guessInsets(this);
                    if (res != null) {
                        res = copyAndScaleDown(res);
                    }
                }
                return res;
            }
        }
    }

    private void applyGuessedInsets() {
        Insets guessed = guessInsets();
        currentInsets = copy(guessed);
    }

    private Insets getRealInsets() {
        if (isNull(currentInsets)) {
            applyGuessedInsets();
        }
        return currentInsets;
    }

    public Insets getInsets() {
        Insets in = copy(getRealInsets());
        in.top += getMenuBarHeight();
        if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
            insLog.finest("Get insets returns {0}", in);
        }
        return in;
    }

    boolean gravityBug() {
        return XWM.configureGravityBuggy();
    }

    // The height of area used to display current active input method
    int getInputMethodHeight() {
        return 0;
    }

    void updateSizeHints(WindowDimensions dims) {
        Rectangle rec = dims.getClientRect();
        checkShellRect(rec);
        updateSizeHints(rec.x, rec.y, rec.width, rec.height);
    }

    void updateSizeHints() {
        updateSizeHints(dimensions);
    }

    // Coordinates are that of the target
    // Called only on Toolkit thread
    private void reshape(WindowDimensions newDimensions, int op,
                        boolean userReshape)
    {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Reshaping " + this + " to " + newDimensions + " op " + op + " user reshape " + userReshape);
        }
        if (userReshape) {
            // We handle only userReshape == true cases. It means that
            // if the window manager or any other part of the windowing
            // system sets inappropriate size for this window, we can
            // do nothing but accept it.
            Rectangle newBounds = newDimensions.getBounds();
            Insets insets = newDimensions.getInsets();
            // Inherit isClientSizeSet from newDimensions
            if (newDimensions.isClientSizeSet()) {
                newBounds = new Rectangle(newBounds.x, newBounds.y,
                                          newBounds.width - insets.left - insets.right,
                                          newBounds.height - insets.top - insets.bottom);
            }
            newDimensions = new WindowDimensions(newBounds, insets, newDimensions.isClientSizeSet());
        }
        if (!isReparented() || !isVisible()) {
            if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
                insLog.fine("- not reparented({0}) or not visible({1}), default reshape",
                       Boolean.valueOf(isReparented()), Boolean.valueOf(visible));
            }

            // Fix for 6323293.
            // This actually is needed to preserve compatibility with previous releases -
            // some of licensees are expecting componentMoved event on invisible one while
            // its location changes.
            Point oldLocation = getLocation();

            Point newLocation = new Point(AWTAccessor.getComponentAccessor().getX(target),
                                          AWTAccessor.getComponentAccessor().getY(target));

            if (!newLocation.equals(oldLocation)) {
                handleMoved(newDimensions);
            }

            dimensions = new WindowDimensions(newDimensions);
            updateSizeHints(dimensions);
            Rectangle client = dimensions.getClientRect();
            checkShellRect(client);
            setShellBounds(client);
            if (content != null &&
                !content.getSize().equals(newDimensions.getSize()))
            {
                reconfigureContentWindow(newDimensions);
            }
            return;
        }

        updateChildrenSizes();
        applyGuessedInsets();

        Rectangle shellRect = newDimensions.getClientRect();

        if (gravityBug()) {
            Insets in = newDimensions.getInsets();
            shellRect.translate(in.left, in.top);
        }

        if ((op & NO_EMBEDDED_CHECK) == 0 && isEmbedded()) {
            shellRect.setLocation(0, 0);
        }

        checkShellRectSize(shellRect);
        if (!isEmbedded()) {
            checkShellRectPos(shellRect);
        }

        op = op & ~NO_EMBEDDED_CHECK;

        if (op == SET_LOCATION) {
            setShellPosition(shellRect);
        } else if (isResizable()) {
            if (op == SET_BOUNDS) {
                setShellBounds(shellRect);
            } else {
                setShellSize(shellRect);
            }
        } else {
            XWM.setShellNotResizable(this, newDimensions, shellRect, true);
            if (op == SET_BOUNDS) {
                setShellPosition(shellRect);
            }
        }

        reconfigureContentWindow(newDimensions);
    }

    /**
     * @param x, y, width, heith - dimensions of the window with insets
     */
    private void reshape(int x, int y, int width, int height, int operation,
                         boolean userReshape)
    {
        WindowDimensions dims = new WindowDimensions(dimensions);
        switch (operation & (~NO_EMBEDDED_CHECK)) {
          case SET_LOCATION:
              // Set location always sets bounds location. However, until the window is mapped we
              // should use client coordinates
              dims.setLocation(x, y);
              break;
          case SET_SIZE:
              // Set size sets bounds size. However, until the window is mapped we
              // should use client coordinates
              dims.setSize(width, height);
              break;
          case SET_CLIENT_SIZE: {
              // Sets client rect size. Width and height contain insets.
              Insets in = currentInsets;
              width -= in.left+in.right;
              height -= in.top+in.bottom;
              dims.setClientSize(width, height);
              break;
          }
          case SET_BOUNDS:
          default:
              dims.setLocation(x, y);
              dims.setSize(width, height);
              break;
        }
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("For the operation {0} new dimensions are {1}",
                        operationToString(operation), dims);
        }

        reshape(dims, operation, userReshape);
    }

    // This method gets overriden in XFramePeer & XDialogPeer.
    abstract boolean isTargetUndecorated();

    /**
     * @see java.awt.peer.ComponentPeer#setBounds
     */
    public void setBounds(int x, int y, int width, int height, int op) {
        // TODO: Rewrite with WindowDimensions
        XToolkit.awtLock();
        try {
            reshape(x, y, width, height, op, true);
        } finally {
            XToolkit.awtUnlock();
        }
        validateSurface();
    }

    // Coordinates are that of the shell
    void reconfigureContentWindow(WindowDimensions dims) {
        if (content == null) {
            insLog.fine("WARNING: Content window is null");
            return;
        }
        content.setContentBounds(dims);
    }

    boolean no_reparent_artifacts = false;
    public void handleConfigureNotifyEvent(XEvent xev) {
        if (XWM.getWMID() == XWM.UNITY_COMPIZ_WM && !insets_corrected) {
            return;
        }
        assert (SunToolkit.isAWTLockHeldByCurrentThread());
        XConfigureEvent xe = xev.get_xconfigure();
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Configure notify {0}", xe);
        }

        // XXX: should really only consider synthetic events, but
        if (isReparented()) {
            configure_seen = true;
        }

        if (!isMaximized()
            && (xe.get_serial() == reparent_serial || xe.get_window() != getShell())
            && !no_reparent_artifacts)
        {
            insLog.fine("- reparent artifact, skipping");
            return;
        }
        no_reparent_artifacts = false;

        /**
         * When there is a WM we receive some CN before being visible and after.
         * We should skip all CN which are before being visible, because we assume
         * the gravity is in action while it is not yet.
         *
         * When there is no WM we receive CN only _before_ being visible.
         * We should process these CNs.
         */
        if (!isVisible() && XWM.getWMID() != XWM.NO_WM) {
            insLog.fine(" - not visible, skipping");
            return;
        }

        /*
         * Some window managers configure before we are reparented and
         * the send event flag is set! ugh... (Enlighetenment for one,
         * possibly MWM as well).  If we haven't been reparented yet
         * this is just the WM shuffling us into position.  Ignore
         * it!!!! or we wind up in a bogus location.
         */
        int runningWM = XWM.getWMID();
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("reparented={0}, visible={1}, WM={2}, decorations={3}",
                        isReparented(), isVisible(), runningWM, getDecorations());
        }
        if (!isReparented() && isVisible() && runningWM != XWM.NO_WM
                &&  !XWM.isNonReparentingWM()
                && getDecorations() != XWindowAttributesData.AWT_DECOR_NONE) {
            insLog.fine("- visible but not reparented, skipping");
            return;
        }
        //Last chance to correct insets
        if (!insets_corrected && getDecorations() != XWindowAttributesData.AWT_DECOR_NONE) {
            long parent = XlibUtil.getParentWindow(window);
            Insets correctWM = (parent != -1) ? XWM.getWM().getInsets(this, window, parent) : null;
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                if (correctWM != null) {
                    insLog.finer("Configure notify - insets : " + correctWM);
                } else {
                    insLog.finer("Configure notify - insets are still not available");
                }
            }
            if (correctWM != null) {
                handleCorrectInsets(copyAndScaleDown(correctWM));
            } else {
                //Only one attempt to correct insets is made (to lower risk)
                //if insets are still not available we simply set the flag
                insets_corrected = true;
            }
        }

        updateChildrenSizes();

        Point newLocation = getNewLocation(xe, currentInsets.left, currentInsets.top);
        WindowDimensions newDimensions =
                new WindowDimensions(newLocation,
                                     new Dimension(scaleDown(xe.get_width()),
                                                   scaleDown(xe.get_height())),
                                     copy(currentInsets), true);

        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Insets are {0}, new dimensions {1}",
                     currentInsets, newDimensions);
        }

        checkIfOnNewScreen(newDimensions.getBounds());

        Point oldLocation = getLocation();
        dimensions = newDimensions;
        if (!newLocation.equals(oldLocation)) {
            handleMoved(newDimensions);
        }
        reconfigureContentWindow(newDimensions);
        updateChildrenSizes();

        repositionSecurityWarning();
    }

    private void checkShellRectSize(Rectangle shellRect) {
        shellRect.width = Math.max(MIN_SIZE, shellRect.width);
        shellRect.height = Math.max(MIN_SIZE, shellRect.height);
    }

    private void checkShellRectPos(Rectangle shellRect) {
        int wm = XWM.getWMID();
        if (wm == XWM.MOTIF_WM || wm == XWM.CDE_WM) {
            if (shellRect.x == 0 && shellRect.y == 0) {
                shellRect.x = shellRect.y = 1;
            }
        }
    }

    private void checkShellRect(Rectangle shellRect) {
        checkShellRectSize(shellRect);
        checkShellRectPos(shellRect);
    }

    private void setShellBounds(Rectangle rec) {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting shell bounds on " + this + " to " + rec);
        }
        updateSizeHints(rec.x, rec.y, rec.width, rec.height);
        XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(), getShell(),
                                      scaleUp(rec.x), scaleUp(rec.y),
                                      scaleUp(rec.width), scaleUp(rec.height));
    }

    private void setShellSize(Rectangle rec) {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting shell size on " + this + " to " + rec);
        }
        updateSizeHints(rec.x, rec.y, rec.width, rec.height);
        XlibWrapper.XResizeWindow(XToolkit.getDisplay(), getShell(),
                                  scaleUp(rec.width), scaleUp(rec.height));
    }

    private void setShellPosition(Rectangle rec) {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting shell position on " + this + " to " + rec);
        }
        updateSizeHints(rec.x, rec.y, rec.width, rec.height);
        XlibWrapper.XMoveWindow(XToolkit.getDisplay(), getShell(),
                                scaleUp(rec.x), scaleUp(rec.y));
    }

    public void setResizable(boolean resizable) {
        XToolkit.awtLock();
        try {
            int fs = winAttr.functions;
            if (!isResizable() && resizable) {
                resetWMSetInsets();
                if (!isEmbedded()) {
                    setReparented(false);
                }
                winAttr.isResizable = resizable;
                if ((fs & MWMConstants.MWM_FUNC_ALL) != 0) {
                    fs &= ~(MWMConstants.MWM_FUNC_RESIZE
                          | MWMConstants.MWM_FUNC_MAXIMIZE);
                } else {
                    fs |= (MWMConstants.MWM_FUNC_RESIZE
                         | MWMConstants.MWM_FUNC_MAXIMIZE);
                }
                winAttr.functions = fs;
                XWM.setShellResizable(this);
            } else if (isResizable() && !resizable) {
                resetWMSetInsets();
                if (!isEmbedded()) {
                    setReparented(false);
                }
                winAttr.isResizable = resizable;
                if ((fs & MWMConstants.MWM_FUNC_ALL) != 0) {
                    fs |= (MWMConstants.MWM_FUNC_RESIZE
                         | MWMConstants.MWM_FUNC_MAXIMIZE);
                } else {
                    fs &= ~(MWMConstants.MWM_FUNC_RESIZE
                          | MWMConstants.MWM_FUNC_MAXIMIZE);
                }
                winAttr.functions = fs;
                XWM.setShellNotResizable(this, dimensions,
                        XWM.getWMID() == XWM.UNITY_COMPIZ_WM && configure_seen ?
                        dimensions.getScreenBounds() :
                        dimensions.getBounds(), false);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    Rectangle getShellBounds() {
        return dimensions.getClientRect();
    }

    public Rectangle getBounds() {
        return dimensions.getBounds();
    }

    public Dimension getSize() {
        return dimensions.getSize();
    }

    public int getX() {
        return dimensions.getLocation().x;
    }

    public int getY() {
        return dimensions.getLocation().y;
    }

    public Point getLocation() {
        return dimensions.getLocation();
    }

    public int getAbsoluteX() {
        // NOTE: returning this peer's location which is shell location
        return dimensions.getScreenBounds().x;
    }

    public int getAbsoluteY() {
        // NOTE: returning this peer's location which is shell location
        return dimensions.getScreenBounds().y;
    }

    public int getWidth() {
        return getSize().width;
    }

    public int getHeight() {
        return getSize().height;
    }

    public final WindowDimensions getDimensions() {
        return dimensions;
    }

    public Point getLocationOnScreen() {
        XToolkit.awtLock();
        try {
            if (configure_seen) {
                return toGlobal(0,0);
            }
        } finally {
            XToolkit.awtUnlock();
        }
        Point location = target.getLocation();
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("getLocationOnScreen {0} not reparented: {1} ",
                        this, location);
        }
        return location;
    }


/***************************************************************************************
 *              END            OF             I N S E T S   C O D E
 **************************************************************************************/

    protected boolean isEventDisabled(XEvent e) {
        switch (e.get_type()) {
            // Do not generate MOVED/RESIZED events since we generate them by ourselves
          case XConstants.ConfigureNotify:
              return true;
          case XConstants.EnterNotify:
          case XConstants.LeaveNotify:
              // Disable crossing event on outer borders of Frame so
              // we receive only one set of cross notifications(first set is from content window)
              return true;
          default:
              return super.isEventDisabled(e);
        }
    }

    int getDecorations() {
        return winAttr.decorations;
    }

    int getFunctions() {
        return winAttr.functions;
    }

    public void setVisible(boolean vis) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Setting {0} to visible {1}", this, Boolean.valueOf(vis));
        }
        if (vis && !isVisible()) {
            XWM.setShellDecor(this);
            super.setVisible(vis);
            if (winAttr.isResizable) {
                //Fix for 4320050: Minimum size for java.awt.Frame is not being enforced.
                //We need to update frame's minimum size, not to reset it
                XWM.removeSizeHints(this, XUtilConstants.PMaxSize);
                updateMinimumSize();
            }
        } else {
            super.setVisible(vis);
        }
    }

    protected void suppressWmTakeFocus(boolean doSuppress) {
        XAtomList protocols = getWMProtocols();
        if (doSuppress) {
            protocols.remove(wm_take_focus);
        } else {
            protocols.add(wm_take_focus);
        }
        wm_protocols.setAtomListProperty(this, protocols);
    }

    public void dispose() {
        if (content != null) {
            content.destroy();
        }
        focusProxy.destroy();

        if (iconWindow != null) {
            iconWindow.destroy();
        }

        super.dispose();
    }

    public void handleClientMessage(XEvent xev) {
        super.handleClientMessage(xev);
        XClientMessageEvent cl = xev.get_xclient();
        if ((wm_protocols != null) && (cl.get_message_type() == wm_protocols.getAtom())) {
            if (cl.get_data(0) == wm_delete_window.getAtom()) {
                handleQuit();
            } else if (cl.get_data(0) == wm_take_focus.getAtom()) {
                handleWmTakeFocus(cl);
            }
        }
    }

    private void handleWmTakeFocus(XClientMessageEvent cl) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
            focusLog.fine("WM_TAKE_FOCUS on {0}", this);
        }

        if (XWM.getWMID() == XWM.UNITY_COMPIZ_WM) {
            // JDK-8159460
            Window focusedWindow = XKeyboardFocusManagerPeer.getInstance()
                    .getCurrentFocusedWindow();
            Window activeWindow = XWindowPeer.getDecoratedOwner(focusedWindow);
            if (activeWindow != target) {
                requestWindowFocus(cl.get_data(1), true);
            } else {
                WindowEvent we = new WindowEvent(focusedWindow,
                        WindowEvent.WINDOW_GAINED_FOCUS);
                sendEvent(we);
            }
        } else {
            requestWindowFocus(cl.get_data(1), true);
        }
    }

    /**
     * Requests focus to this decorated top-level by requesting X input focus
     * to the shell window.
     */
    protected void requestXFocus(long time, boolean timeProvided) {
        // We have proxied focus mechanism - instead of shell the focus is held
        // by "proxy" - invisible mapped window. When we want to set X input focus to
        // toplevel set it on proxy instead.
        if (focusProxy == null) {
            if (focusLog.isLoggable(PlatformLogger.Level.WARNING)) {
                focusLog.warning("Focus proxy is null for " + this);
            }
        } else {
            if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                focusLog.fine("Requesting focus to proxy: " + focusProxy);
            }
            if (timeProvided) {
                focusProxy.xRequestFocus(time);
            } else {
                focusProxy.xRequestFocus();
            }
        }
    }

    XFocusProxyWindow getFocusProxy() {
        return focusProxy;
    }

    private void handleQuit() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_CLOSING));
    }

    final void dumpMe() {
        System.err.println(">>> Peer: " + x + ", " + y + ", " + width + ", " + height);
    }

    final void dumpTarget() {
        AWTAccessor.ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();
        int getWidth = compAccessor.getWidth(target);
        int getHeight = compAccessor.getHeight(target);
        int getTargetX = compAccessor.getX(target);
        int getTargetY = compAccessor.getY(target);
        System.err.println(">>> Target: " + getTargetX + ", " + getTargetY + ", " + getWidth + ", " + getHeight);
    }

    final void dumpShell() {
        dumpWindow("Shell", getShell());
    }
    final void dumpContent() {
        dumpWindow("Content", getContentWindow());
    }
    final void dumpParent() {
        long parent = XlibUtil.getParentWindow(getShell());
        if (parent != 0)
        {
            dumpWindow("Parent", parent);
        }
        else
        {
            System.err.println(">>> NO PARENT");
        }
    }

    final void dumpWindow(String id, long window) {
        XWindowAttributes pattr = new XWindowAttributes();
        try {
            XToolkit.awtLock();
            try {
                int status =
                    XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                     window, pattr.pData);
            }
            finally {
                XToolkit.awtUnlock();
            }
            System.err.println(">>>> " + id + ": " + pattr.get_x()
                               + ", " + pattr.get_y() + ", " + pattr.get_width()
                               + ", " + pattr.get_height());
        } finally {
            pattr.dispose();
        }
    }

    final void dumpAll() {
        dumpTarget();
        dumpMe();
        dumpParent();
        dumpShell();
        dumpContent();
    }

    boolean isMaximized() {
        return false;
    }

    @Override
    boolean isOverrideRedirect() {
        return Window.Type.POPUP.equals(getWindowType());
    }

    public boolean requestWindowFocus(long time, boolean timeProvided) {
        focusLog.fine("Request for decorated window focus");
        // If this is Frame or Dialog we can't assure focus request success - but we still can try
        // If this is Window and its owner Frame is active we can be sure request succedded.
        Window focusedWindow = XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow();
        Window activeWindow = XWindowPeer.getDecoratedOwner(focusedWindow);

        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Current window is: active={0}, focused={1}",
                       Boolean.valueOf(target == activeWindow),
                       Boolean.valueOf(target == focusedWindow));
        }

        XWindowPeer toFocus = this;
        while (toFocus.nextTransientFor != null) {
            toFocus = toFocus.nextTransientFor;
        }
        if (toFocus == null || !toFocus.focusAllowedFor()) {
            // This might change when WM will have property to determine focus policy.
            // Right now, because policy is unknown we can't be sure we succedded
            return false;
        }
        if (this == toFocus) {
            if (isWMStateNetHidden()) {
                focusLog.fine("The window is unmapped, so rejecting the request");
                return false;
            }
            if (target == activeWindow && target != focusedWindow) {
                // Happens when an owned window is currently focused
                focusLog.fine("Focus is on child window - transferring it back to the owner");
                handleWindowFocusInSync(-1);
                return true;
            }
            Window realNativeFocusedWindow = XWindowPeer.getNativeFocusedWindow();
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Real native focused window: " + realNativeFocusedWindow +
                            "\nKFM's focused window: " + focusedWindow);
            }

            // A workaround for Metacity. See 6522725, 6613426, 7147075.
            if (target == realNativeFocusedWindow && XWM.getWMID() == XWM.METACITY_WM) {
                if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                    focusLog.fine("The window is already natively focused.");
                }
                return true;
            }
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
            focusLog.fine("Requesting focus to " + (this == toFocus ? "this window" : toFocus));
        }

        if (timeProvided) {
            toFocus.requestXFocus(time);
        } else {
            toFocus.requestXFocus();
        }
        return (this == toFocus);
    }

    XWindowPeer actualFocusedWindow = null;
    void setActualFocusedWindow(XWindowPeer actualFocusedWindow) {
        synchronized(getStateLock()) {
            this.actualFocusedWindow = actualFocusedWindow;
        }
    }

    boolean requestWindowFocus(XWindowPeer actualFocusedWindow,
                               long time, boolean timeProvided)
    {
        setActualFocusedWindow(actualFocusedWindow);
        return requestWindowFocus(time, timeProvided);
    }
    public void handleWindowFocusIn(long serial) {
        if (null == actualFocusedWindow) {
            super.handleWindowFocusIn(serial);
        } else {
            /*
             * Fix for 6314575.
             * If this is a result of clicking on one of the Frame's component
             * then 'actualFocusedWindow' shouldn't be focused. A decision of focusing
             * it or not should be made after the appropriate Java mouse event (if any)
             * is handled by the component where 'actualFocusedWindow' value may be reset.
             *
             * The fix is based on the empiric fact consisting in that the component
             * receives native mouse event nearly at the same time the Frame receives
             * WM_TAKE_FOCUS (when FocusIn is generated via XSetInputFocus call) but
             * definetely before the Frame gets FocusIn event (when this method is called).
             */
            postEvent(new InvocationEvent(target, new Runnable() {
                public void run() {
                    XWindowPeer fw = null;
                    synchronized (getStateLock()) {
                        fw = actualFocusedWindow;
                        actualFocusedWindow = null;
                        if (null == fw || !fw.isVisible() || !fw.isFocusableWindow()) {
                            fw = XDecoratedPeer.this;
                        }
                    }
                    fw.handleWindowFocusIn_Dispatch();
                }
            }));
        }
    }

    public void handleWindowFocusOut(Window oppositeWindow, long serial) {
        Window actualFocusedWindow = XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow();

        // If the actual focused window is not this decorated window then retain it.
        if (actualFocusedWindow != null && actualFocusedWindow != target) {
            Window owner = XWindowPeer.getDecoratedOwner(actualFocusedWindow);

            if (owner != null && owner == target) {
                setActualFocusedWindow(AWTAccessor.getComponentAccessor().getPeer(actualFocusedWindow));
            }
        }
        super.handleWindowFocusOut(oppositeWindow, serial);
    }

    public static final String MWM_DECOR_TITLE_PROPERTY_NAME = "xawt.mwm_decor_title";

    public final Optional<Boolean> getMWMDecorTitleProperty() {
        Optional<Boolean> res = Optional.empty();

        if (SunToolkit.isInstanceOf(target, "javax.swing.RootPaneContainer")) {
            javax.swing.JRootPane rootpane = ((javax.swing.RootPaneContainer) target).getRootPane();
            Object prop = rootpane.getClientProperty(MWM_DECOR_TITLE_PROPERTY_NAME);
            if (prop != null) {
                res = Optional.of(Boolean.parseBoolean(prop.toString()));
            }
        }

        return res;
    }

    public final boolean getWindowTitleVisible() {
        return getMWMDecorTitleProperty().orElse(true);
    }
}
