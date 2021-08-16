/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.Point2D;
import java.lang.ref.WeakReference;

import sun.awt.IconInfo;
import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;

class XWarningWindow extends XWindow {
    private static final int SHOWING_DELAY = 330;
    private static final int HIDING_DELAY = 2000;

    private final Window ownerWindow;
    private WeakReference<XWindowPeer> ownerPeer;
    private long parentWindow;

    private static final String OWNER = "OWNER";
    private InfoWindow.Tooltip tooltip;

    /**
     * Animation stage.
     */
    private volatile int currentIcon = 0;

    /* -1 - uninitialized.
     * 0 - 16x16
     * 1 - 24x24
     * 2 - 32x32
     * 3 - 48x48
     */
    private int currentSize = -1;
    private static IconInfo[][] icons;
    private static IconInfo getSecurityIconInfo(int size, int num) {
        synchronized (XWarningWindow.class) {
            if (icons == null) {
                icons = new IconInfo[4][3];
                if (XlibWrapper.dataModel == 32) {
                    icons[0][0] = new IconInfo(sun.awt.AWTIcon32_security_icon_bw16_png.security_icon_bw16_png);
                    icons[0][1] = new IconInfo(sun.awt.AWTIcon32_security_icon_interim16_png.security_icon_interim16_png);
                    icons[0][2] = new IconInfo(sun.awt.AWTIcon32_security_icon_yellow16_png.security_icon_yellow16_png);
                    icons[1][0] = new IconInfo(sun.awt.AWTIcon32_security_icon_bw24_png.security_icon_bw24_png);
                    icons[1][1] = new IconInfo(sun.awt.AWTIcon32_security_icon_interim24_png.security_icon_interim24_png);
                    icons[1][2] = new IconInfo(sun.awt.AWTIcon32_security_icon_yellow24_png.security_icon_yellow24_png);
                    icons[2][0] = new IconInfo(sun.awt.AWTIcon32_security_icon_bw32_png.security_icon_bw32_png);
                    icons[2][1] = new IconInfo(sun.awt.AWTIcon32_security_icon_interim32_png.security_icon_interim32_png);
                    icons[2][2] = new IconInfo(sun.awt.AWTIcon32_security_icon_yellow32_png.security_icon_yellow32_png);
                    icons[3][0] = new IconInfo(sun.awt.AWTIcon32_security_icon_bw48_png.security_icon_bw48_png);
                    icons[3][1] = new IconInfo(sun.awt.AWTIcon32_security_icon_interim48_png.security_icon_interim48_png);
                    icons[3][2] = new IconInfo(sun.awt.AWTIcon32_security_icon_yellow48_png.security_icon_yellow48_png);
                } else {
                    icons[0][0] = new IconInfo(sun.awt.AWTIcon64_security_icon_bw16_png.security_icon_bw16_png);
                    icons[0][1] = new IconInfo(sun.awt.AWTIcon64_security_icon_interim16_png.security_icon_interim16_png);
                    icons[0][2] = new IconInfo(sun.awt.AWTIcon64_security_icon_yellow16_png.security_icon_yellow16_png);
                    icons[1][0] = new IconInfo(sun.awt.AWTIcon64_security_icon_bw24_png.security_icon_bw24_png);
                    icons[1][1] = new IconInfo(sun.awt.AWTIcon64_security_icon_interim24_png.security_icon_interim24_png);
                    icons[1][2] = new IconInfo(sun.awt.AWTIcon64_security_icon_yellow24_png.security_icon_yellow24_png);
                    icons[2][0] = new IconInfo(sun.awt.AWTIcon64_security_icon_bw32_png.security_icon_bw32_png);
                    icons[2][1] = new IconInfo(sun.awt.AWTIcon64_security_icon_interim32_png.security_icon_interim32_png);
                    icons[2][2] = new IconInfo(sun.awt.AWTIcon64_security_icon_yellow32_png.security_icon_yellow32_png);
                    icons[3][0] = new IconInfo(sun.awt.AWTIcon64_security_icon_bw48_png.security_icon_bw48_png);
                    icons[3][1] = new IconInfo(sun.awt.AWTIcon64_security_icon_interim48_png.security_icon_interim48_png);
                    icons[3][2] = new IconInfo(sun.awt.AWTIcon64_security_icon_yellow48_png.security_icon_yellow48_png);
                }
            }
        }
        final int sizeIndex = size % icons.length;
        return icons[sizeIndex][num % icons[sizeIndex].length];
    }

    private void updateIconSize() {
        int newSize = -1;

        if (ownerWindow != null) {
            Insets insets = ownerWindow.getInsets();
            int max = Math.max(insets.top, Math.max(insets.bottom,
                        Math.max(insets.left, insets.right)));
            if (max < 24) {
                newSize = 0;
            } else if (max < 32) {
                newSize = 1;
            } else if (max < 48) {
                newSize = 2;
            } else {
                newSize = 3;
            }
        }
        // Make sure we have a valid size
        if (newSize == -1) {
            newSize = 0;
        }

        // Note: this is not the most wise solution to use awtLock here,
        // this should have been sync'ed with the stateLock. However,
        // the awtLock must be taken first (see XBaseWindow.getStateLock()),
        // and we need the awtLock anyway to update the shape of the icon.
        // So it's easier to use just one lock instead.
        XToolkit.awtLock();
        try {
            if (newSize != currentSize) {
                currentSize = newSize;
                IconInfo ico = getSecurityIconInfo(currentSize, 0);
                XlibWrapper.SetBitmapShape(XToolkit.getDisplay(), getWindow(),
                        ico.getWidth(), ico.getHeight(), ico.getIntData());
                AWTAccessor.getWindowAccessor().setSecurityWarningSize(
                        ownerWindow, ico.getWidth(), ico.getHeight());
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    private IconInfo getSecurityIconInfo() {
        updateIconSize();
        return getSecurityIconInfo(currentSize, currentIcon);
    }

    XWarningWindow(final Window ownerWindow, long parentWindow, XWindowPeer ownerPeer) {
        super(new XCreateWindowParams(new Object[] {
                        TARGET, ownerWindow,
                        OWNER, Long.valueOf(parentWindow)
        }));
        this.ownerWindow = ownerWindow;
        this.parentWindow = parentWindow;
        this.tooltip = new InfoWindow.Tooltip(null, getTarget(),
                new InfoWindow.Tooltip.LiveArguments() {
                    public boolean isDisposed() {
                        return XWarningWindow.this.isDisposed();
                    }
                    public Rectangle getBounds() {
                        return XWarningWindow.this.getBounds();
                    }
                    public String getTooltipString() {
                        return XWarningWindow.this.ownerWindow.getWarningString();
                    }
                });
        this.ownerPeer = new WeakReference<XWindowPeer>(ownerPeer);
    }

    private void requestNoTaskbar() {
        XNETProtocol netProtocol = XWM.getWM().getNETProtocol();
        if (netProtocol != null) {
            netProtocol.requestState(this, netProtocol.XA_NET_WM_STATE_SKIP_TASKBAR, true);
        }
    }

    @Override
    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        XToolkit.awtLock();
        try {
            XWM.setMotifDecor(this, false, 0, 0);
            XWM.setOLDecor(this, false, 0);

            long parentWindow = ((Long)params.get(OWNER)).longValue();
            XlibWrapper.XSetTransientFor(XToolkit.getDisplay(),
                    getWindow(), parentWindow);

            XWMHints hints = getWMHints();
            hints.set_flags(hints.get_flags() | (int)XUtilConstants.InputHint | (int)XUtilConstants.StateHint);
            hints.set_input(false);
            hints.set_initial_state(XUtilConstants.NormalState);
            XlibWrapper.XSetWMHints(XToolkit.getDisplay(), getWindow(), hints.pData);

            initWMProtocols();
            requestNoTaskbar();
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * @param x,y,w,h coordinates of the untrusted window
     */
    public void reposition(int x, int y, int w, int h) {
        Point2D point = AWTAccessor.getWindowAccessor().
            calculateSecurityWarningPosition(ownerWindow,
                x, y, w, h);
        reshape((int)point.getX(), (int)point.getY(), getWidth(), getHeight());
    }

    protected String getWMName() {
        return "Warning window";
    }

    public Graphics getGraphics() {
        if ((surfaceData == null) || (ownerWindow == null)) return null;
        return getGraphics(surfaceData,
                                 getColor(),
                                 getBackground(),
                                 getFont());
    }
    void paint(Graphics g, int x, int y, int width, int height) {
        g.drawImage(getSecurityIconInfo().getImage(), 0, 0, null);
    }

    String getWarningString() {
        return ownerWindow.getWarningString();
    }

    int getWidth() {
        return getSecurityIconInfo().getWidth();
    }

    int getHeight() {
        return getSecurityIconInfo().getHeight();
    }

    Color getBackground() {
        return SystemColor.window;
    }
    Color getColor() {
        return Color.black;
    }
    Font getFont () {
        return ownerWindow.getFont();
    }

    @Override
    public void repaint() {
        final Rectangle bounds = getBounds();
        final Graphics g = getGraphics();
        if (g != null) {
            try {
                paint(g, 0, 0, bounds.width, bounds.height);
            } finally {
                g.dispose();
            }
        }
    }
    @Override
    public void handleExposeEvent(XEvent xev) {
        super.handleExposeEvent(xev);

        XExposeEvent xe = xev.get_xexpose();
        final int x = scaleDown(xe.get_x());
        final int y = scaleDown(xe.get_y());
        final int width = scaleDown(xe.get_width());
        final int height = scaleDown(xe.get_height());
        SunToolkit.executeOnEventHandlerThread(target,
                new Runnable() {
                    public void run() {
                        final Graphics g = getGraphics();
                        if (g != null) {
                            try {
                                paint(g, x, y, width, height);
                            } finally {
                                g.dispose();
                            }
                        }
                    }
                });
    }

    @Override
    protected boolean isEventDisabled(XEvent e) {
        return true;
    }

    /** Send a synthetic UnmapNotify in order to withdraw the window.
     */
    private void withdraw() {
        XEvent req = new XEvent();
        try {
            long root;
            XToolkit.awtLock();
            try {
                root = XlibWrapper.RootWindow(XToolkit.getDisplay(), getScreenNumber());
            }
            finally {
                XToolkit.awtUnlock();
            }

            req.set_type(XConstants.UnmapNotify);

            XUnmapEvent umev = req.get_xunmap();

            umev.set_event(root);
            umev.set_window(getWindow());
            umev.set_from_configure(false);

            XToolkit.awtLock();
            try {
                XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                        root,
                        false,
                        XConstants.SubstructureRedirectMask | XConstants.SubstructureNotifyMask,
                        req.pData);
            }
            finally {
                XToolkit.awtUnlock();
            }
        } finally {
            req.dispose();
        }
    }

    @Override
    protected void stateChanged(long time, int oldState, int newState) {
        if (newState == XUtilConstants.IconicState) {
            super.xSetVisible(false);
            withdraw();
        }
    }

    @Override
    protected void setMouseAbove(boolean above) {
        super.setMouseAbove(above);
        XWindowPeer p = ownerPeer.get();
        if (p != null) {
            p.updateSecurityWarningVisibility();
        }
    }

    @Override
    protected void enterNotify(long window) {
        super.enterNotify(window);
        if (window == getWindow()) {
            tooltip.enter();
        }
    }

    @Override
    protected void leaveNotify(long window) {
        super.leaveNotify(window);
        if (window == getWindow()) {
            tooltip.exit();
        }
    }

    @Override
    public void xSetVisible(boolean visible) {
        super.xSetVisible(visible);

        // The _NET_WM_STATE_SKIP_TASKBAR got reset upon hiding/showing,
        // so we request it every time whenever we change the visibility.
        requestNoTaskbar();
    }

    private final Runnable hidingTask = new Runnable() {
        public void run() {
            xSetVisible(false);
        }
    };

    private final Runnable showingTask = new Runnable() {
        public void run() {
            if (!isVisible()) {
                xSetVisible(true);
                updateIconSize();
                XWindowPeer peer = ownerPeer.get();
                if (peer != null) {
                    peer.repositionSecurityWarning();
                }
            }
            repaint();
            if (currentIcon > 0) {
                currentIcon--;
                XToolkit.schedule(showingTask, SHOWING_DELAY);
            }
        }
    };

    public void setSecurityWarningVisible(boolean visible, boolean doSchedule) {
        if (visible) {
            XToolkit.remove(hidingTask);
            XToolkit.remove(showingTask);
            if (isVisible()) {
                currentIcon = 0;
            } else {
                currentIcon = 3;
            }
            if (doSchedule) {
                XToolkit.schedule(showingTask, 1);
            } else {
                showingTask.run();
            }
        } else {
            XToolkit.remove(showingTask);
            XToolkit.remove(hidingTask);
            if (!isVisible()) {
                return;
            }
            if (doSchedule) {
                XToolkit.schedule(hidingTask, HIDING_DELAY);
            } else {
                hidingTask.run();
            }
        }
    }
}
