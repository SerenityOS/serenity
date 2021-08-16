/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MenuBar;
import java.awt.Rectangle;
import java.awt.peer.FramePeer;
import sun.awt.SunToolkit;
import sun.util.logging.PlatformLogger;
import sun.awt.AWTAccessor;

class XFramePeer extends XDecoratedPeer implements FramePeer {
    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XFramePeer");
    private static PlatformLogger stateLog = PlatformLogger.getLogger("sun.awt.X11.states");
    private static PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XFramePeer");

    XMenuBarPeer menubarPeer;
    MenuBar menubar;
    int state;
    private Boolean undecorated;

    private static final int MENUBAR_HEIGHT_IF_NO_MENUBAR = 0;
    private int lastAppliedMenubarHeight = MENUBAR_HEIGHT_IF_NO_MENUBAR;

    XFramePeer(Frame target) {
        super(target);
    }

    XFramePeer(XCreateWindowParams params) {
        super(params);
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        Frame target = (Frame)(this.target);
        // set the window attributes for this Frame
        winAttr.initialState = target.getExtendedState();
        state = 0;
        undecorated = Boolean.valueOf(target.isUndecorated());
        winAttr.nativeDecor = !target.isUndecorated();
        winAttr.decorations = getWindowDecorationBits();
        winAttr.functions = MWMConstants.MWM_FUNC_ALL;
        winAttr.isResizable = true; // target.isResizable();
        winAttr.title = target.getTitle();
        winAttr.initialResizability = target.isResizable();
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Frame''s initial attributes: decor {0}, resizable {1}, undecorated {2}, initial state {3}",
                     Integer.valueOf(winAttr.decorations), Boolean.valueOf(winAttr.initialResizability),
                     Boolean.valueOf(!winAttr.nativeDecor), Integer.valueOf(winAttr.initialState));
        }

        registerWindowDecorationChangeListener();
    }

    private void registerWindowDecorationChangeListener() {
        if (SunToolkit.isInstanceOf(target, "javax.swing.RootPaneContainer")) { // avoid unnecessary class loading
            javax.swing.JRootPane rootpane = ((javax.swing.RootPaneContainer) target).getRootPane();
            rootpane.addPropertyChangeListener(MWM_DECOR_TITLE_PROPERTY_NAME, e -> winAttr.decorations = getWindowDecorationBits() );
        }
    }

    private int getWindowDecorationBits() {
        int decorations = XWindowAttributesData.AWT_DECOR_NONE;
        final Frame target = (Frame)(this.target);
        final boolean useNativeDecor = !target.isUndecorated();
        if (useNativeDecor) {
            decorations = XWindowAttributesData.AWT_DECOR_ALL;

            if (!getWindowTitleVisible()) {
                // NB: the window must be [re-]mapped to make this change effective. Also, window insets will probably
                // change and that'll be caught by one of the subsequent property change events in XDecoratedPeer
                // (not necessarily the very next event, though).
                decorations = XWindowAttributesData.AWT_DECOR_BORDER;
            }

            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("Frame''s initial decorations affected by the client property {0}={1}",
                         MWM_DECOR_TITLE_PROPERTY_NAME, getMWMDecorTitleProperty());
            }
        }

        return decorations;
    }

    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        setupState(true);
    }

    @Override
    boolean isTargetUndecorated() {
        if (undecorated != null) {
            return undecorated.booleanValue();
        } else {
            return ((Frame)target).isUndecorated();
        }
    }

    void setupState(boolean onInit) {
        if (onInit) {
            state = winAttr.initialState;
        }
        if ((state & Frame.ICONIFIED) != 0) {
            setInitialState(XUtilConstants.IconicState);
        } else {
            setInitialState(XUtilConstants.NormalState);
        }
        setExtendedState(state);
    }

    @SuppressWarnings("deprecation")
    public void setMenuBar(MenuBar mb) {
        // state_lock should always be the second after awt_lock
        XToolkit.awtLock();
        try {
            synchronized(getStateLock()) {
                if (mb == menubar) return;
                if (mb == null) {
                    if (menubar != null) {
                        menubarPeer.xSetVisible(false);
                        menubar = null;
                        menubarPeer.dispose();
                        menubarPeer = null;
                    }
                } else {
                    menubar = mb;
                    menubarPeer = AWTAccessor.getMenuComponentAccessor()
                                             .getPeer(mb);
                    if (menubarPeer != null) {
                        menubarPeer.init((Frame)target);
                    }
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }

        reshapeMenubarPeer();
    }

    XMenuBarPeer getMenubarPeer() {
        return menubarPeer;
    }

    int getMenuBarHeight() {
        if (menubarPeer != null) {
            return menubarPeer.getDesiredHeight();
        } else {
            return MENUBAR_HEIGHT_IF_NO_MENUBAR;
        }
    }

    void updateChildrenSizes() {
        super.updateChildrenSizes();
        int height = getMenuBarHeight();

        // XWindow.reshape calls XBaseWindow.xSetBounds, which acquires
        // the AWT lock, so we have to acquire the AWT lock here
        // before getStateLock() to avoid a deadlock with the Toolkit thread
        // when this method is called on the EDT.
        XToolkit.awtLock();
        try {
            synchronized(getStateLock()) {
                int width = dimensions.getClientSize().width;
                if (menubarPeer != null) {
                    menubarPeer.reshape(0, 0, width, height);
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * In addition to reshaping menubarPeer (by using 'updateChildrenSizes')
     * this method also performs some frame reaction on this (i.e. layouts
     * other frame children, if required)
     */
    final void reshapeMenubarPeer() {
        XToolkit.executeOnEventHandlerThread(
            target,
            new Runnable() {
                public void run() {
                    updateChildrenSizes();
                    boolean heightChanged = false;

                    int height = getMenuBarHeight();
                        // Neither 'XToolkit.awtLock()' nor 'getStateLock()'
                        // is acquired under this call, and it looks to run
                        // thread-safely. I currently see no reason to move
                        // it under following 'synchronized' clause.

                    synchronized(getStateLock()) {
                        if (height != lastAppliedMenubarHeight) {
                            lastAppliedMenubarHeight = height;
                            heightChanged = true;
                        }
                    }
                    if (heightChanged) {
                        // To make frame contents be re-layout (copied from
                        // 'XDecoratedPeer.revalidate()'). These are not
                        // 'synchronized', because can recursively call client
                        // methods, which are not supposed to be called with locks
                        // acquired.
                        target.invalidate();
                        target.validate();
                    }
                }
            }
        );
    }

    public void setMaximizedBounds(Rectangle b) {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting maximized bounds to " + b);
        }
        if (b == null) return;
        maxBounds = new Rectangle(b);
        XToolkit.awtLock();
        try {
            XSizeHints hints = getHints();
            hints.set_flags(hints.get_flags() | (int)XUtilConstants.PMaxSize);
            if (b.width != Integer.MAX_VALUE) {
                hints.set_max_width(b.width);
            } else {
                hints.set_max_width((int)XlibWrapper.DisplayWidth(XToolkit.getDisplay(), XlibWrapper.DefaultScreen(XToolkit.getDisplay())));
            }
            if (b.height != Integer.MAX_VALUE) {
                hints.set_max_height(b.height);
            } else {
                hints.set_max_height((int)XlibWrapper.DisplayHeight(XToolkit.getDisplay(), XlibWrapper.DefaultScreen(XToolkit.getDisplay())));
            }
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                insLog.finer("Setting hints, flags " + XlibWrapper.hintsToString(hints.get_flags()));
            }
            XlibWrapper.XSetWMNormalHints(XToolkit.getDisplay(), window, hints.pData);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public int getState() {
        synchronized(getStateLock()) {
            return state;
        }
    }

    public void setState(int newState) {
        synchronized(getStateLock()) {
            if (!isShowing()) {
                stateLog.finer("Frame is not showing");
                state = newState;
                return;
            }
        }
        changeState(newState);
    }

    void changeState(int newState) {
        int changed = state ^ newState;
        int changeIconic = changed & Frame.ICONIFIED;
        boolean iconic = (newState & Frame.ICONIFIED) != 0;
        if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
            stateLog.finer("Changing state, old state {0}, new state {1}(iconic {2})",
                       Integer.valueOf(state), Integer.valueOf(newState), Boolean.valueOf(iconic));
        }
        if (changeIconic != 0 && iconic) {
            if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
                stateLog.finer("Iconifying shell " + getShell() + ", this " + this + ", screen " + getScreenNumber());
            }
            XToolkit.awtLock();
            try {
                int res = XlibWrapper.XIconifyWindow(XToolkit.getDisplay(), getShell(), getScreenNumber());
                if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
                    stateLog.finer("XIconifyWindow returned " + res);
                }
            }
            finally {
                XToolkit.awtUnlock();
            }
        }
        if ((changed & ~Frame.ICONIFIED) != 0) {
            setExtendedState(newState);
        }
        if (changeIconic != 0 && !iconic) {
            if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
                stateLog.finer("DeIconifying " + this);
            }

            XNETProtocol net_protocol = XWM.getWM().getNETProtocol();
            if (net_protocol != null) {
                net_protocol.setActiveWindow(getWindow());
            }
            xSetVisible(true);
        }
    }

    void setExtendedState(int newState) {
        XWM.getWM().setExtendedState(this, newState);
    }

    public void handlePropertyNotify(XEvent xev) {
        super.handlePropertyNotify(xev);
        XPropertyEvent ev = xev.get_xproperty();

        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Property change {0}", ev);
        }
        /*
         * Let's see if this is a window state protocol message, and
         * if it is - decode a new state in terms of java constants.
         */
        if (!XWM.getWM().isStateChange(this, ev)) {
            stateLog.finer("either not a state atom or state has not been changed");
            return;
        }

        final int newState = XWM.getWM().getState(this);
        int changed = state ^ newState;
        if (changed == 0) {
            if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
                stateLog.finer("State is the same: " + state);
            }
            return;
        }

        int old_state = state;
        state = newState;

        // sync target with peer
        AWTAccessor.getFrameAccessor().setExtendedState((Frame)target, state);

        if ((changed & Frame.ICONIFIED) != 0) {
            if ((state & Frame.ICONIFIED) != 0) {
                stateLog.finer("Iconified");
                handleIconify();
            } else {
                stateLog.finer("DeIconified");
                content.purgeIconifiedExposeEvents();
                handleDeiconify();
            }
        }
        handleStateChange(old_state, state);

        // RepaintManager does not repaint iconified windows. Window needs to be
        // repainted explicitly, when it is deiconified.
        if (((changed & Frame.ICONIFIED) != 0) &&
            ((state & Frame.ICONIFIED) == 0)) {
            repaint();
        }
    }

    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void handleStateChange(int oldState, int newState) {
        super.handleStateChange(oldState, newState);
        for (ToplevelStateListener topLevelListenerTmp : toplevelStateListeners) {
            topLevelListenerTmp.stateChangedJava(oldState, newState);
        }
    }

    public void setVisible(boolean vis) {
        if (vis) {
            setupState(false);
        } else {
            if ((state & Frame.MAXIMIZED_BOTH) != 0) {
                XWM.getWM().setExtendedState(this, state & ~Frame.MAXIMIZED_BOTH);
            }
        }
        super.setVisible(vis);
        if (vis && maxBounds != null) {
            setMaximizedBounds(maxBounds);
        }
    }

    void setInitialState(int wm_state) {
        XToolkit.awtLock();
        try {
            XWMHints hints = getWMHints();
            hints.set_flags((int)XUtilConstants.StateHint | hints.get_flags());
            hints.set_initial_state(wm_state);
            if (stateLog.isLoggable(PlatformLogger.Level.FINE)) {
                stateLog.fine("Setting initial WM state on " + this + " to " + wm_state);
            }
            XlibWrapper.XSetWMHints(XToolkit.getDisplay(), getWindow(), hints.pData);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }

    public void dispose() {
        if (menubarPeer != null) {
            menubarPeer.dispose();
        }
        super.dispose();
    }

    boolean isMaximized() {
        return (state & (Frame.MAXIMIZED_VERT  | Frame.MAXIMIZED_HORIZ)) != 0;
    }




    static final int CROSSHAIR_INSET = 5;

    static final int BUTTON_Y = CROSSHAIR_INSET + 1;
    static final int BUTTON_W = 17;
    static final int BUTTON_H = 17;

    static final int SYS_MENU_X = CROSSHAIR_INSET + 1;
    static final int SYS_MENU_CONTAINED_X = SYS_MENU_X + 5;
    static final int SYS_MENU_CONTAINED_Y = BUTTON_Y + 7;
    static final int SYS_MENU_CONTAINED_W = 8;
    static final int SYS_MENU_CONTAINED_H = 3;

    static final int MAXIMIZE_X_DIFF = CROSSHAIR_INSET + BUTTON_W;
    static final int MAXIMIZE_CONTAINED_X_DIFF = MAXIMIZE_X_DIFF - 5;
    static final int MAXIMIZE_CONTAINED_Y = BUTTON_Y + 5;
    static final int MAXIMIZE_CONTAINED_W = 8;
    static final int MAXIMIZE_CONTAINED_H = 8;

    static final int MINIMIZE_X_DIFF = MAXIMIZE_X_DIFF + BUTTON_W;
    static final int MINIMIZE_CONTAINED_X_DIFF = MINIMIZE_X_DIFF - 7;
    static final int MINIMIZE_CONTAINED_Y = BUTTON_Y + 7;
    static final int MINIMIZE_CONTAINED_W = 3;
    static final int MINIMIZE_CONTAINED_H = 3;

    static final int TITLE_X = SYS_MENU_X + BUTTON_W;
    static final int TITLE_W_DIFF = BUTTON_W * 3 + CROSSHAIR_INSET * 2 - 1;
    static final int TITLE_MID_Y = BUTTON_Y + (BUTTON_H / 2);

    static final int MENUBAR_X = CROSSHAIR_INSET + 1;
    static final int MENUBAR_Y = BUTTON_Y + BUTTON_H;

    static final int HORIZ_RESIZE_INSET = CROSSHAIR_INSET + BUTTON_H;
    static final int VERT_RESIZE_INSET = CROSSHAIR_INSET + BUTTON_W;


    /*
     * Print the native component by rendering the Motif look ourselves.
     * We also explicitly print the MenuBar since a MenuBar isn't a subclass
     * of Component (and thus it has no "print" method which gets called by
     * default).
     */
    public void print(Graphics g) {
        super.print(g);

        Frame f = (Frame)target;
        Insets finsets = f.getInsets();
        Dimension fsize = f.getSize();

        Color bg = f.getBackground();
        Color fg = f.getForeground();
        Color highlight = bg.brighter();
        Color shadow = bg.darker();

        // Well, we could query for the currently running window manager
        // and base the look on that, or we could just always do dtwm.
        // aim, tball, and levenson all agree we'll just do dtwm.

        if (hasDecorations(XWindowAttributesData.AWT_DECOR_BORDER)) {

            // top outer -- because we'll most likely be drawing on white paper,
            // for aesthetic reasons, don't make any part of the outer border
            // pure white
            if (highlight.equals(Color.white)) {
                g.setColor(new Color(230, 230, 230));
            }
            else {
                g.setColor(highlight);
            }
            g.drawLine(0, 0, fsize.width, 0);
            g.drawLine(0, 1, fsize.width - 1, 1);

            // left outer
            // if (highlight.equals(Color.white)) {
            //     g.setColor(new Color(230, 230, 230));
            // }
            // else {
            //     g.setColor(highlight);
            // }
            g.drawLine(0, 0, 0, fsize.height);
            g.drawLine(1, 0, 1, fsize.height - 1);

            // bottom cross-hair
            g.setColor(highlight);
            g.drawLine(CROSSHAIR_INSET + 1, fsize.height - CROSSHAIR_INSET,
                       fsize.width - CROSSHAIR_INSET,
                       fsize.height - CROSSHAIR_INSET);

            // right cross-hair
            // g.setColor(highlight);
            g.drawLine(fsize.width - CROSSHAIR_INSET, CROSSHAIR_INSET + 1,
                       fsize.width - CROSSHAIR_INSET,
                       fsize.height - CROSSHAIR_INSET);

            // bottom outer
            g.setColor(shadow);
            g.drawLine(1, fsize.height, fsize.width, fsize.height);
            g.drawLine(2, fsize.height - 1, fsize.width, fsize.height - 1);

            // right outer
            // g.setColor(shadow);
            g.drawLine(fsize.width, 1, fsize.width, fsize.height);
            g.drawLine(fsize.width - 1, 2, fsize.width - 1, fsize.height);

            // top cross-hair
            // g.setColor(shadow);
            g.drawLine(CROSSHAIR_INSET, CROSSHAIR_INSET,
                       fsize.width - CROSSHAIR_INSET, CROSSHAIR_INSET);

            // left cross-hair
            // g.setColor(shadow);
            g.drawLine(CROSSHAIR_INSET, CROSSHAIR_INSET, CROSSHAIR_INSET,
                       fsize.height - CROSSHAIR_INSET);
        }

        if (hasDecorations(XWindowAttributesData.AWT_DECOR_TITLE)) {

            if (hasDecorations(XWindowAttributesData.AWT_DECOR_MENU)) {

                // system menu
                g.setColor(bg);
                g.fill3DRect(SYS_MENU_X, BUTTON_Y, BUTTON_W, BUTTON_H, true);
                g.fill3DRect(SYS_MENU_CONTAINED_X, SYS_MENU_CONTAINED_Y,
                             SYS_MENU_CONTAINED_W, SYS_MENU_CONTAINED_H, true);
            }

            // title bar
            // g.setColor(bg);
            g.fill3DRect(TITLE_X, BUTTON_Y, fsize.width - TITLE_W_DIFF, BUTTON_H,
                         true);

            if (hasDecorations(XWindowAttributesData.AWT_DECOR_MINIMIZE)) {

                // minimize button
                // g.setColor(bg);
                g.fill3DRect(fsize.width - MINIMIZE_X_DIFF, BUTTON_Y, BUTTON_W,
                             BUTTON_H, true);
                g.fill3DRect(fsize.width - MINIMIZE_CONTAINED_X_DIFF,
                             MINIMIZE_CONTAINED_Y, MINIMIZE_CONTAINED_W,
                             MINIMIZE_CONTAINED_H, true);
            }

            if (hasDecorations(XWindowAttributesData.AWT_DECOR_MAXIMIZE)) {

                // maximize button
                // g.setColor(bg);
                g.fill3DRect(fsize.width - MAXIMIZE_X_DIFF, BUTTON_Y, BUTTON_W,
                             BUTTON_H, true);
                g.fill3DRect(fsize.width - MAXIMIZE_CONTAINED_X_DIFF,
                             MAXIMIZE_CONTAINED_Y, MAXIMIZE_CONTAINED_W,
                             MAXIMIZE_CONTAINED_H, true);
            }

            // title bar text
            g.setColor(fg);
            Font sysfont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
            g.setFont(sysfont);
            FontMetrics sysfm = g.getFontMetrics();
            String ftitle = f.getTitle();
            g.drawString(ftitle,
                         ((TITLE_X + TITLE_X + fsize.width - TITLE_W_DIFF) / 2) -
                         (sysfm.stringWidth(ftitle) / 2),
                         TITLE_MID_Y + sysfm.getMaxDescent());
        }

        if (f.isResizable() &&
            hasDecorations(XWindowAttributesData.AWT_DECOR_RESIZEH)) {

            // add resize cross hairs

            // upper-left horiz (shadow)
            g.setColor(shadow);
            g.drawLine(1, HORIZ_RESIZE_INSET, CROSSHAIR_INSET,
                       HORIZ_RESIZE_INSET);
            // upper-left vert (shadow)
            // g.setColor(shadow);
            g.drawLine(VERT_RESIZE_INSET, 1, VERT_RESIZE_INSET, CROSSHAIR_INSET);
            // upper-right horiz (shadow)
            // g.setColor(shadow);
            g.drawLine(fsize.width - CROSSHAIR_INSET + 1, HORIZ_RESIZE_INSET,
                       fsize.width, HORIZ_RESIZE_INSET);
            // upper-right vert (shadow)
            // g.setColor(shadow);
            g.drawLine(fsize.width - VERT_RESIZE_INSET - 1, 2,
                       fsize.width - VERT_RESIZE_INSET - 1, CROSSHAIR_INSET + 1);
            // lower-left horiz (shadow)
            // g.setColor(shadow);
            g.drawLine(1, fsize.height - HORIZ_RESIZE_INSET - 1,
                       CROSSHAIR_INSET, fsize.height - HORIZ_RESIZE_INSET - 1);
            // lower-left vert (shadow)
            // g.setColor(shadow);
            g.drawLine(VERT_RESIZE_INSET, fsize.height - CROSSHAIR_INSET + 1,
                       VERT_RESIZE_INSET, fsize.height);
            // lower-right horiz (shadow)
            // g.setColor(shadow);
            g.drawLine(fsize.width - CROSSHAIR_INSET + 1,
                       fsize.height - HORIZ_RESIZE_INSET - 1, fsize.width,
                       fsize.height - HORIZ_RESIZE_INSET - 1);
            // lower-right vert (shadow)
            // g.setColor(shadow);
            g.drawLine(fsize.width - VERT_RESIZE_INSET - 1,
                       fsize.height - CROSSHAIR_INSET + 1,
                       fsize.width - VERT_RESIZE_INSET - 1, fsize.height);

            // upper-left horiz (highlight)
            g.setColor(highlight);
            g.drawLine(2, HORIZ_RESIZE_INSET + 1, CROSSHAIR_INSET,
                       HORIZ_RESIZE_INSET + 1);
            // upper-left vert (highlight)
            // g.setColor(highlight);
            g.drawLine(VERT_RESIZE_INSET + 1, 2, VERT_RESIZE_INSET + 1,
                       CROSSHAIR_INSET);
            // upper-right horiz (highlight)
            // g.setColor(highlight);
            g.drawLine(fsize.width - CROSSHAIR_INSET + 1,
                       HORIZ_RESIZE_INSET + 1, fsize.width - 1,
                       HORIZ_RESIZE_INSET + 1);
            // upper-right vert (highlight)
            // g.setColor(highlight);
            g.drawLine(fsize.width - VERT_RESIZE_INSET, 2,
                       fsize.width - VERT_RESIZE_INSET, CROSSHAIR_INSET);
            // lower-left horiz (highlight)
            // g.setColor(highlight);
            g.drawLine(2, fsize.height - HORIZ_RESIZE_INSET, CROSSHAIR_INSET,
                       fsize.height - HORIZ_RESIZE_INSET);
            // lower-left vert (highlight)
            // g.setColor(highlight);
            g.drawLine(VERT_RESIZE_INSET + 1,
                       fsize.height - CROSSHAIR_INSET + 1,
                       VERT_RESIZE_INSET + 1, fsize.height - 1);
            // lower-right horiz (highlight)
            // g.setColor(highlight);
            g.drawLine(fsize.width - CROSSHAIR_INSET + 1,
                       fsize.height - HORIZ_RESIZE_INSET, fsize.width - 1,
                       fsize.height - HORIZ_RESIZE_INSET);
            // lower-right vert (highlight)
            // g.setColor(highlight);
            g.drawLine(fsize.width - VERT_RESIZE_INSET,
                       fsize.height - CROSSHAIR_INSET + 1,
                       fsize.width - VERT_RESIZE_INSET, fsize.height - 1);
        }

        XMenuBarPeer peer = menubarPeer;
        if (peer != null) {
            Insets insets = getInsets();
            Graphics ng = g.create();
            int menubarX = 0;
            int menubarY = 0;
            if (hasDecorations(XWindowAttributesData.AWT_DECOR_BORDER)) {
                menubarX += CROSSHAIR_INSET + 1;
                    menubarY += CROSSHAIR_INSET + 1;
            }
            if (hasDecorations(XWindowAttributesData.AWT_DECOR_TITLE)) {
                menubarY += BUTTON_H;
            }
            try {
                ng.translate(menubarX, menubarY);
                peer.print(ng);
            } finally {
                ng.dispose();
            }
        }
    }

    public void setBoundsPrivate(int x, int y, int width, int height) {
        setBounds(x, y, width, height, SET_BOUNDS);
    }

    public Rectangle getBoundsPrivate() {
        return getBounds();
    }

    public void emulateActivation(boolean doActivate) {
        if (doActivate) {
            handleWindowFocusIn(0);
        } else {
            handleWindowFocusOut(null, 0);
        }
    }
}
