/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import sun.awt.AWTAccessor;
import sun.awt.IconInfo;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.metal.MTLLayer;
import sun.java2d.opengl.CGLLayer;
import sun.lwawt.LWWindowPeer;
import sun.lwawt.PlatformEventNotifier;
import sun.lwawt.SecurityWarningWindow;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.ref.WeakReference;

public final class CWarningWindow extends CPlatformWindow
        implements SecurityWarningWindow, PlatformEventNotifier {

    private static class Lock {}
    private final Lock lock = new Lock();

    private static final int SHOWING_DELAY = 300;
    private static final int HIDING_DELAY = 2000;

    private Rectangle bounds = new Rectangle();
    private final WeakReference<LWWindowPeer> ownerPeer;
    private final Window ownerWindow;

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
        synchronized (CWarningWindow.class) {
            if (icons == null) {
                icons = new IconInfo[4][3];
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
            }
        }
        final int sizeIndex = size % icons.length;
        return icons[sizeIndex][num % icons[sizeIndex].length];
    }

    public CWarningWindow(final Window _ownerWindow, final LWWindowPeer _ownerPeer) {
        super();

        this.ownerPeer = new WeakReference<>(_ownerPeer);
        this.ownerWindow = _ownerWindow;

        initialize(null, null, _ownerPeer.getPlatformWindow());

        setOpaque(false);

        String warningString = ownerWindow.getWarningString();
        if (warningString != null) {
            contentView.setToolTip(ownerWindow.getWarningString());
        }

        updateIconSize();
    }

    /**
     * @param x,y,w,h coordinates of the untrusted window
     */
    public void reposition(int x, int y, int w, int h) {
        final Point2D point = AWTAccessor.getWindowAccessor().
                calculateSecurityWarningPosition(ownerWindow, x, y, w, h);
        setBounds((int)point.getX(), (int)point.getY(), getWidth(), getHeight());
    }

    public void setVisible(boolean visible, boolean doSchedule) {
        synchronized (taskLock) {
            cancelTasks();

            if (visible) {
                if (isVisible()) {
                    currentIcon = 0;
                } else {
                    currentIcon = 2;
                }

                showHideTask = new ShowingTask();
                LWCToolkit.performOnMainThreadAfterDelay(showHideTask, 50);
            } else {
                if (!isVisible()) {
                    return;
                }

                showHideTask = new HidingTask();
                if (doSchedule) {
                    LWCToolkit.performOnMainThreadAfterDelay(showHideTask, HIDING_DELAY);
                } else {
                    LWCToolkit.performOnMainThreadAfterDelay(showHideTask, 50);
                }
            }
        }
    }

    @Override
    public void notifyIconify(boolean iconify) {
    }

    @Override
    public void notifyZoom(boolean isZoomed) {
    }

    @Override
    public void notifyExpose(final Rectangle r) {
        repaint();
    }

    @Override
    public void notifyReshape(int x, int y, int w, int h) {
    }

    @Override
    public void notifyUpdateCursor() {
    }

    @Override
    public void notifyActivation(boolean activation, LWWindowPeer opposite) {
    }

    @Override
    public void notifyNCMouseDown() {
    }

    @Override
    public void notifyMouseEvent(int id, long when, int button, int x, int y,
                                 int absX, int absY, int modifiers,
                                 int clickCount, boolean popupTrigger,
                                 byte[] bdata) {
        LWWindowPeer peer = ownerPeer.get();
        if (id == MouseEvent.MOUSE_EXITED) {
            if (peer != null) {
                peer.updateSecurityWarningVisibility();
            }
        } else if(id == MouseEvent.MOUSE_ENTERED) {
            if (peer != null) {
                peer.updateSecurityWarningVisibility();
            }
        }
    }

    public Rectangle getBounds() {
        synchronized (lock) {
            return bounds.getBounds();
        }
    }

    @Override
    public boolean isVisible() {
        synchronized (lock) {
            return visible;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        synchronized (lock) {
            execute(ptr -> {
                // Actually show or hide the window
                if (visible) {
                    CWrapper.NSWindow.orderFront(ptr);
                } else {
                    CWrapper.NSWindow.orderOut(ptr);
                }
            });

            this.visible = visible;

            // Manage parent-child relationship when showing
            if (visible) {
                // Order myself above my parent
                if (owner != null && owner.isVisible()) {
                    owner.execute(ownerPtr -> {
                        execute(ptr -> {
                            CWrapper.NSWindow.orderWindow(ptr,
                                                          CWrapper.NSWindow.NSWindowAbove,
                                                          ownerPtr);
                        });
                    });

                    // do not allow security warning to be obscured by other windows
                    applyWindowLevel(ownerWindow);
                }
            }
        }
    }

    @Override
    public void notifyMouseWheelEvent(long when, int x, int y, int absX,
                                      int absY, int modifiers, int scrollType,
                                      int scrollAmount, int wheelRotation,
                                      double preciseWheelRotation,
                                      byte[] bdata) {
    }

    @Override
    public void notifyKeyEvent(int id, long when, int modifiers, int keyCode,
                               char keyChar, int keyLocation) {
    }

    protected int getInitialStyleBits() {
        int styleBits = 0;
        CPlatformWindow.SET(styleBits, CPlatformWindow.UTILITY, true);
        return styleBits;
    }

    protected void deliverMoveResizeEvent(int x, int y, int width, int height,
                                          boolean byUser) {

        boolean isResize;
        synchronized (lock) {
            isResize = (bounds.width != width || bounds.height != height);
            bounds = new Rectangle(x, y, width, height);
        }

        if (isResize) {
            replaceSurface();
        }

        super.deliverMoveResizeEvent(x, y, width, height, byUser);
    }

    protected CPlatformResponder createPlatformResponder() {
        return new CPlatformResponder(this, false);
    }

    CPlatformView createContentView() {
        return new CPlatformView() {
            public GraphicsConfiguration getGraphicsConfiguration() {
                LWWindowPeer peer = ownerPeer.get();
                return peer.getGraphicsConfiguration();
            }

            public Rectangle getBounds() {
                return CWarningWindow.this.getBounds();
            }

            public CGLLayer createCGLayer() {
                return new CGLLayer(null) {
                    public Rectangle getBounds() {
                        return CWarningWindow.this.getBounds();
                    }

                    public GraphicsConfiguration getGraphicsConfiguration() {
                        LWWindowPeer peer = ownerPeer.get();
                        return peer.getGraphicsConfiguration();
                    }

                    public boolean isOpaque() {
                        return false;
                    }
                };
            }
            public MTLLayer createMTLLayer() {
                return new MTLLayer(null) {
                    public Rectangle getBounds() {
                        return CWarningWindow.this.getBounds();
                    }

                    public GraphicsConfiguration getGraphicsConfiguration() {
                        LWWindowPeer peer = ownerPeer.get();
                        return peer.getGraphicsConfiguration();
                    }

                    public boolean isOpaque() {
                        return false;
                    }
                };
            }

        };
    }

    @Override
    public void dispose() {
        cancelTasks();
        SurfaceData surfaceData = contentView.getSurfaceData();
        if (surfaceData != null) {
            surfaceData.invalidate();
        }
        super.dispose();
    }

    private void cancelTasks() {
        synchronized (taskLock) {
            if (showHideTask != null) {
                showHideTask.cancel();
                showHideTask = null;
            }
        }
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

        synchronized (lock) {
            if (newSize != currentSize) {
                currentSize = newSize;
                IconInfo ico = getSecurityIconInfo(currentSize, 0);
                AWTAccessor.getWindowAccessor().setSecurityWarningSize(
                    ownerWindow, ico.getWidth(), ico.getHeight());
            }
        }
    }

    private Graphics getGraphics() {
        SurfaceData sd = contentView.getSurfaceData();
        if (ownerWindow == null || sd == null) {
            return null;
        }

        return new SunGraphics2D(sd, SystemColor.windowText, SystemColor.window,
                                 ownerWindow.getFont());
    }


    private void repaint() {
        final Graphics g = getGraphics();
        if (g != null) {
            try {
                ((Graphics2D) g).setComposite(AlphaComposite.Src);
                g.drawImage(getSecurityIconInfo().getImage(), 0, 0, null);
            } finally {
                g.dispose();
            }
        }
    }

    private void replaceSurface() {
        SurfaceData oldData = contentView.getSurfaceData();

        replaceSurfaceData();

        if (oldData != null && oldData != contentView.getSurfaceData()) {
            oldData.flush();
        }
    }

    private int getWidth() {
        return getSecurityIconInfo().getWidth();
    }

    private int getHeight() {
        return getSecurityIconInfo().getHeight();
    }

    private IconInfo getSecurityIconInfo() {
        return getSecurityIconInfo(currentSize, currentIcon);
    }

    private final Lock taskLock = new Lock();
    private CancelableRunnable showHideTask;

    private abstract static class CancelableRunnable implements Runnable {
        private volatile boolean perform = true;

        public final void cancel() {
            perform = false;
        }

        @Override
        public final void run() {
            if (perform) {
                perform();
            }
        }

        public abstract void perform();
    }

    private class HidingTask extends CancelableRunnable {
        @Override
        public void perform() {
            synchronized (lock) {
                setVisible(false);
            }

            synchronized (taskLock) {
                showHideTask = null;
            }
        }
    }

    private class ShowingTask extends CancelableRunnable {
        @Override
        public void perform() {
            synchronized (lock) {
                if (!isVisible()) {
                    setVisible(true);
                }
                repaint();
            }

            synchronized (taskLock) {
                if (currentIcon > 0) {
                    currentIcon--;
                    showHideTask = new ShowingTask();
                    LWCToolkit.performOnMainThreadAfterDelay(showHideTask, SHOWING_DELAY);
                } else {
                    showHideTask = null;
                }
            }
        }
    }
}

