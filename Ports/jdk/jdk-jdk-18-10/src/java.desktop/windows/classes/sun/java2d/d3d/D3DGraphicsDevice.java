/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.d3d;

import java.awt.Dialog;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.peer.WindowPeer;
import java.util.ArrayList;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.Win32GraphicsDevice;
import sun.awt.windows.WWindowPeer;
import sun.java2d.d3d.D3DContext.D3DContextCaps;
import sun.java2d.pipe.hw.ContextCapabilities;
import sun.java2d.windows.WindowsFlags;

import static sun.java2d.d3d.D3DContext.D3DContextCaps.CAPS_DEVICE_OK;
import static sun.java2d.d3d.D3DContext.D3DContextCaps.CAPS_EMPTY;

/**
 * This class implements D3D-specific functionality, such as fullscreen
 * exclusive mode and display changes.  It is kept separate from
 * Win32GraphicsDevice to help avoid overburdening the parent class.
 */
public final class D3DGraphicsDevice extends Win32GraphicsDevice {
    private D3DContext context;

    private static boolean d3dAvailable;

    private ContextCapabilities d3dCaps;

    private static native boolean initD3D();

    static {
        // loading the library doesn't help because we need the
        // toolkit thread running, so we have to call getDefaultToolkit()
        Toolkit.getDefaultToolkit();
        d3dAvailable = initD3D();
        if (d3dAvailable) {
            // we don't use pixel formats for the d3d pipeline
            pfDisabled = true;
        }
    }

    /**
     * Used to construct a Direct3D-enabled GraphicsDevice.
     *
     * @return a D3DGraphicsDevice if it could be created
     * successfully, null otherwise.
     */
    public static D3DGraphicsDevice createDevice(int screen) {
        if (!d3dAvailable) {
            return null;
        }

        ContextCapabilities d3dCaps = getDeviceCaps(screen);
        // could not initialize the device successfully
        if ((d3dCaps.getCaps() & CAPS_DEVICE_OK) == 0) {
            if (WindowsFlags.isD3DVerbose()) {
                System.out.println("Could not enable Direct3D pipeline on " +
                                   "screen " + screen);
            }
            return null;
        }
        if (WindowsFlags.isD3DVerbose()) {
            System.out.println("Direct3D pipeline enabled on screen " + screen);
        }

        D3DGraphicsDevice gd = new D3DGraphicsDevice(screen, d3dCaps);
        return gd;
    }

    private static native int getDeviceCapsNative(int screen);
    private static native String getDeviceIdNative(int screen);
    private static ContextCapabilities getDeviceCaps(final int screen) {
        ContextCapabilities d3dCaps = null;
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            class Result {
                int caps;
                String id;
            };
            final Result res = new Result();
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    res.caps = getDeviceCapsNative(screen);
                    res.id = getDeviceIdNative(screen);
                }
            });
            d3dCaps = new D3DContextCaps(res.caps, res.id);
        } finally {
            rq.unlock();
        }

        return d3dCaps != null ? d3dCaps : new D3DContextCaps(CAPS_EMPTY, null);
    }

    public final boolean isCapPresent(int cap) {
        return ((d3dCaps.getCaps() & cap) != 0);
    }

    private D3DGraphicsDevice(int screennum, ContextCapabilities d3dCaps) {
        super(screennum);
        descString = "D3DGraphicsDevice[screen="+screennum;
        this.d3dCaps = d3dCaps;
        context = new D3DContext(D3DRenderQueue.getInstance(), this);
    }

    public boolean isD3DEnabledOnDevice() {
        return isValid() && isCapPresent(CAPS_DEVICE_OK);
    }

    /**
     * Returns true if d3d pipeline has been successfully initialized.
     * @return true if d3d pipeline is initialized, false otherwise
     */
    public static boolean isD3DAvailable() {
        return d3dAvailable;
    }

    /**
     * Return the owning Frame for a given Window.  Used in setFSWindow below
     * to set the properties of the owning Frame when a Window goes
     * into fullscreen mode.
     */
    private Frame getToplevelOwner(Window w) {
        Window owner = w;
        while (owner != null) {
            owner = owner.getOwner();
            if (owner instanceof Frame) {
                return (Frame) owner;
            }
        }
        // could get here if passed Window is an owner-less Dialog
        return null;
    }

    private boolean fsStatus;
    private Rectangle ownerOrigBounds = null;
    private boolean ownerWasVisible;
    private Window realFSWindow;
    private WindowListener fsWindowListener;
    private boolean fsWindowWasAlwaysOnTop;
    private static native boolean enterFullScreenExclusiveNative(int screen,
                                                                 long hwnd);

    @Override
    protected void enterFullScreenExclusive(final int screen, WindowPeer wp)
    {
        final WWindowPeer wpeer = AWTAccessor.getComponentAccessor()
                                             .getPeer(realFSWindow);
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    long hwnd = wpeer.getHWnd();
                    if (hwnd == 0l) {
                        // window is disposed
                        fsStatus = false;
                        return;
                    }
                    fsStatus = enterFullScreenExclusiveNative(screen, hwnd);
                }
            });
        } finally {
            rq.unlock();
        }
        if (!fsStatus) {
            super.enterFullScreenExclusive(screen, wp);
        }
    }

    private static native boolean exitFullScreenExclusiveNative(int screen);
    @Override
    protected void exitFullScreenExclusive(final int screen, WindowPeer w) {
        if (fsStatus) {
            D3DRenderQueue rq = D3DRenderQueue.getInstance();
            rq.lock();
            try {
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        exitFullScreenExclusiveNative(screen);
                    }
                });
            } finally {
                rq.unlock();
            }
        } else {
            super.exitFullScreenExclusive(screen, w);
        }
    }

    /**
     * WindowAdapter class for the full-screen frame, responsible for
     * restoring the devices. This is important to do because unless the device
     * is restored it will not go back into the FS mode once alt+tabbed out.
     * This is a problem for windows for which we do not do any d3d-related
     * operations (like when we disabled on-screen rendering).
     *
     * REMIND: we create an instance per each full-screen device while a single
     * instance would suffice (but requires more management).
     */
    private static class D3DFSWindowAdapter extends WindowAdapter {
        @Override
        @SuppressWarnings("static")
        public void windowDeactivated(WindowEvent e) {
            D3DRenderQueue.getInstance().restoreDevices();
        }
        @Override
        @SuppressWarnings("static")
        public void windowActivated(WindowEvent e) {
            D3DRenderQueue.getInstance().restoreDevices();
        }
    }

    @Override
    protected void addFSWindowListener(Window w) {
        // if the window is not a toplevel (has an owner) we have to use the
        // real toplevel to enter the full-screen mode with (4933099).
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        if (!(w instanceof Frame) && !(w instanceof Dialog) &&
            (realFSWindow = getToplevelOwner(w)) != null)
        {
            ownerOrigBounds = realFSWindow.getBounds();
            WWindowPeer fp = acc.getPeer(realFSWindow);
            ownerWasVisible = realFSWindow.isVisible();
            Rectangle r = w.getBounds();
            // we use operations on peer instead of component because calling
            // them on component will take the tree lock
            fp.reshape(r.x, r.y, r.width, r.height);
            fp.setVisible(true);
        } else {
            realFSWindow = w;
        }

        fsWindowWasAlwaysOnTop = realFSWindow.isAlwaysOnTop();
        ((WWindowPeer) acc.getPeer(realFSWindow)).setAlwaysOnTop(true);

        fsWindowListener = new D3DFSWindowAdapter();
        realFSWindow.addWindowListener(fsWindowListener);
    }

    @Override
    protected void removeFSWindowListener(Window w) {
        realFSWindow.removeWindowListener(fsWindowListener);
        fsWindowListener = null;

        /**
         * Bug 4933099: There is some funny-business to deal with when this
         * method is called with a Window instead of a Frame.  See 4836744
         * for more information on this.  One side-effect of our workaround
         * for the problem is that the owning Frame of a Window may end
         * up getting resized during the fullscreen process.  When we
         * return from fullscreen mode, we should resize the Frame to
         * its original size (just like the Window is being resized
         * to its original size in GraphicsDevice).
         */
        final WWindowPeer wpeer = AWTAccessor.getComponentAccessor()
                                             .getPeer(realFSWindow);
        if (wpeer != null) {
            if (ownerOrigBounds != null) {
                // if the window went into fs mode before it was realized it
                // could have (0,0) dimensions
                if (ownerOrigBounds.width  == 0) ownerOrigBounds.width  = 1;
                if (ownerOrigBounds.height == 0) ownerOrigBounds.height = 1;
                wpeer.reshape(ownerOrigBounds.x,     ownerOrigBounds.y,
                              ownerOrigBounds.width, ownerOrigBounds.height);
                if (!ownerWasVisible) {
                    wpeer.setVisible(false);
                }
                ownerOrigBounds = null;
            }
            if (!fsWindowWasAlwaysOnTop) {
                wpeer.setAlwaysOnTop(false);
            }
        }

        realFSWindow = null;
    }

    private static native DisplayMode getCurrentDisplayModeNative(int screen);
    @Override
    protected DisplayMode getCurrentDisplayMode(final int screen) {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            class Result {
                DisplayMode dm = null;
            };
            final Result res = new Result();
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    res.dm = getCurrentDisplayModeNative(screen);
                }
            });
            if (res.dm == null) {
                return super.getCurrentDisplayMode(screen);
            }
            return res.dm;
        } finally {
            rq.unlock();
        }
    }
    private static native void configDisplayModeNative(int screen, long hwnd,
                                                       int width, int height,
                                                       int bitDepth,
                                                       int refreshRate);
    @Override
    protected void configDisplayMode(final int screen, final WindowPeer w,
                                     final int width, final int height,
                                     final int bitDepth, final int refreshRate)
    {
        // we entered fs mode via gdi
        if (!fsStatus) {
            super.configDisplayMode(screen, w, width, height, bitDepth,
                                    refreshRate);
            return;
        }
        final WWindowPeer wpeer = AWTAccessor.getComponentAccessor()
                                             .getPeer(realFSWindow);

        // REMIND: we do this before we switch the display mode, so
        // the dimensions may be exceeding the dimensions of the screen,
        // is this a problem?

        // update the bounds of the owner frame
        if (getFullScreenWindow() != realFSWindow) {
            Rectangle screenBounds = getDefaultConfiguration().getBounds();
            wpeer.reshape(screenBounds.x, screenBounds.y, width, height);
        }

        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    long hwnd = wpeer.getHWnd();
                    if (hwnd == 0l) {
                        // window is disposed
                        return;
                    }
                    // REMIND: do we really need a window here?
                    // we should probably just use the current one
                    configDisplayModeNative(screen, hwnd, width, height,
                                            bitDepth, refreshRate);
                }
            });
        } finally {
            rq.unlock();
        }
    }

    private static native void enumDisplayModesNative(int screen,
                                                      ArrayList<DisplayMode> modes);
    @Override
    protected void enumDisplayModes(final int screen, final ArrayList<DisplayMode> modes) {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    enumDisplayModesNative(screen, modes);
                }
            });
            if (modes.size() == 0) {
                modes.add(getCurrentDisplayModeNative(screen));
            }
        } finally {
            rq.unlock();
        }
    }

    private static native long getAvailableAcceleratedMemoryNative(int screen);
    @Override
    public int getAvailableAcceleratedMemory() {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            class Result {
                long mem = 0L;
            };
            final Result res = new Result();
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    res.mem = getAvailableAcceleratedMemoryNative(getScreen());
                }
            });
            return (int)res.mem;
        } finally {
            rq.unlock();
        }
    }

    @Override
    public GraphicsConfiguration[] getConfigurations() {
        if (configs == null) {
            if (isD3DEnabledOnDevice()) {
                defaultConfig = getDefaultConfiguration();
                if (defaultConfig != null) {
                    configs = new GraphicsConfiguration[1];
                    configs[0] = defaultConfig;
                    return configs.clone();
                }
            }
        }
        return super.getConfigurations();
    }

    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        if (defaultConfig == null) {
            if (isD3DEnabledOnDevice()) {
                defaultConfig = new D3DGraphicsConfig(this);
            } else {
                defaultConfig = super.getDefaultConfiguration();
            }
        }
        return defaultConfig;
    }

    private static native boolean isD3DAvailableOnDeviceNative(int screen);
    // REMIND: this method is not used now, we use caps instead
    public static boolean isD3DAvailableOnDevice(final int screen) {
        if (!d3dAvailable) {
            return false;
        }

        // REMIND: should we cache the result per device somehow,
        // and then reset and retry it on display change?
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            class Result {
                boolean avail = false;
            };
            final Result res = new Result();
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    res.avail = isD3DAvailableOnDeviceNative(screen);
                }
            });
            return res.avail;
        } finally {
            rq.unlock();
        }
    }

    D3DContext getContext() {
        return context;
    }

    ContextCapabilities getContextCapabilities() {
        return d3dCaps;
    }

    @Override
    public void displayChanged() {
        super.displayChanged();
        // REMIND: make sure this works when the device is lost and we don't
        // disable d3d too eagerly
        if (d3dAvailable) {
            d3dCaps = getDeviceCaps(getScreen());
        }
    }

    @Override
    protected void invalidate(int defaultScreen) {
        super.invalidate(defaultScreen);
        // REMIND: this is a bit excessive, isD3DEnabledOnDevice will return
        // false anyway because the device is invalid
        d3dCaps = new D3DContextCaps(CAPS_EMPTY, null);
    }
}
