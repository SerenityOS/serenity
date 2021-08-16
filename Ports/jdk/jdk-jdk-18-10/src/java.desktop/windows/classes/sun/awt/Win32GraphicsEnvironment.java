/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.AWTError;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.peer.ComponentPeer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ListIterator;

import sun.awt.windows.WToolkit;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SurfaceManagerFactory;
import sun.java2d.WindowsSurfaceManagerFactory;
import sun.java2d.d3d.D3DGraphicsDevice;
import sun.java2d.windows.WindowsFlags;

/**
 * This is an implementation of a GraphicsEnvironment object for the
 * default local GraphicsEnvironment used by the Java Runtime Environment
 * for Windows.
 *
 * @see GraphicsDevice
 * @see GraphicsConfiguration
 */

public final class Win32GraphicsEnvironment extends SunGraphicsEnvironment {

    static final float debugScaleX;
    static final float debugScaleY;

    static {
        // Ensure awt is loaded already.  Also, this forces static init
        // of WToolkit and Toolkit, which we depend upon
        WToolkit.loadLibraries();
        // setup flags before initializing native layer
        WindowsFlags.initFlags();
        initDisplayWrapper();

        // Install correct surface manager factory.
        SurfaceManagerFactory.setInstance(new WindowsSurfaceManagerFactory());

        double sx = -1;
        double sy = -1;
        if (isUIScaleEnabled()) {
            sx = getScaleFactor("sun.java2d.win.uiScaleX");
            sy = getScaleFactor("sun.java2d.win.uiScaleY");
            if (sx <= 0 || sy <= 0) {
                double s = getDebugScale();
                sx = s;
                sy = s;
            }
        }

        debugScaleX = (float) sx;
        debugScaleY = (float) sy;
    }

    /**
     * Initializes native components of the graphics environment.  This
     * includes everything from the native GraphicsDevice elements to
     * the DirectX rendering layer.
     */
    private static native void initDisplay();

    private static boolean displayInitialized;      // = false;
    public static void initDisplayWrapper() {
        if (!displayInitialized) {
            displayInitialized = true;
            initDisplay();
        }
    }

    public Win32GraphicsEnvironment() {
    }

    protected native int getNumScreens();
    private native int getDefaultScreen();

    public GraphicsDevice getDefaultScreenDevice() {
        GraphicsDevice[] screens = getScreenDevices();
        if (screens.length == 0) {
            throw new AWTError("no screen devices");
        }
        int index = getDefaultScreen();
        return screens[0 < index && index < screens.length ? index : 0];
    }

    /**
     * Returns the number of pixels per logical inch along the screen width.
     * In a system with multiple display monitors, this value is the same for
     * all monitors.
     * @return number of pixels per logical inch in X direction
     */
    public native int getXResolution();
    /**
     * Returns the number of pixels per logical inch along the screen height.
     * In a system with multiple display monitors, this value is the same for
     * all monitors.
     * @return number of pixels per logical inch in Y direction
     */
    public native int getYResolution();


/*
 * ----DISPLAY CHANGE SUPPORT----
 */

    // list of invalidated graphics devices (those which were removed)
    private ArrayList<WeakReference<Win32GraphicsDevice>> oldDevices;
    /*
     * From DisplayChangeListener interface.
     * Called from WToolkit and executed on the event thread when the
     * display settings are changed.
     */
    @Override
    public void displayChanged() {
        // getNumScreens() will return the correct current number of screens
        GraphicsDevice[] newDevices = new GraphicsDevice[getNumScreens()];
        GraphicsDevice[] oldScreens = screens;
        // go through the list of current devices and determine if they
        // could be reused, or will have to be replaced
        if (oldScreens != null) {
            for (int i = 0; i < oldScreens.length; i++) {
                if (!(screens[i] instanceof Win32GraphicsDevice)) {
                    // REMIND: can we ever have anything other than Win32GD?
                    assert (false) : oldScreens[i];
                    continue;
                }
                Win32GraphicsDevice gd = (Win32GraphicsDevice)oldScreens[i];
                // devices may be invalidated from the native code when the
                // display change happens (device add/removal also causes a
                // display change)
                if (!gd.isValid()) {
                    if (oldDevices == null) {
                        oldDevices =
                            new ArrayList<WeakReference<Win32GraphicsDevice>>();
                    }
                    oldDevices.add(new WeakReference<Win32GraphicsDevice>(gd));
                } else if (i < newDevices.length) {
                    // reuse the device
                    newDevices[i] = gd;
                }
            }
            oldScreens = null;
        }
        // create the new devices (those that weren't reused)
        for (int i = 0; i < newDevices.length; i++) {
            if (newDevices[i] == null) {
                newDevices[i] = makeScreenDevice(i);
            }
        }
        // install the new array of devices
        // Note: no synchronization here, it doesn't matter if a thread gets
        // a new or an old array this time around
        screens = newDevices;
        for (GraphicsDevice gd : screens) {
            if (gd instanceof DisplayChangedListener) {
                ((DisplayChangedListener)gd).displayChanged();
            }
        }
        // re-invalidate all old devices. It's needed because those in the list
        // may become "invalid" again - if the current default device is removed,
        // for example. Also, they need to be notified about display
        // changes as well.
        if (oldDevices != null) {
            int defScreen = getDefaultScreen();
            for (ListIterator<WeakReference<Win32GraphicsDevice>> it =
                    oldDevices.listIterator(); it.hasNext();)
            {
                Win32GraphicsDevice gd = it.next().get();
                if (gd != null) {
                    gd.invalidate(defScreen);
                    gd.displayChanged();
                } else {
                    // no more references to this device, remove it
                    it.remove();
                }
            }
        }
        // notify SunDisplayChanger list (e.g. VolatileSurfaceManagers and
        // CachingSurfaceManagers) about the display change event
        displayChanger.notifyListeners();
        // note: do not call super.displayChanged, we've already done everything
    }


/*
 * ----END DISPLAY CHANGE SUPPORT----
 */

    protected GraphicsDevice makeScreenDevice(int screennum) {
        GraphicsDevice device = null;
        if (WindowsFlags.isD3DEnabled()) {
            device = D3DGraphicsDevice.createDevice(screennum);
        }
        if (device == null) {
            device = new Win32GraphicsDevice(screennum);
        }
        return device;
    }

    public boolean isDisplayLocal() {
        return true;
    }

    @Override
    public boolean isFlipStrategyPreferred(ComponentPeer peer) {
        GraphicsConfiguration gc;
        if (peer != null && (gc = peer.getGraphicsConfiguration()) != null) {
            GraphicsDevice gd = gc.getDevice();
            if (gd instanceof D3DGraphicsDevice) {
                return ((D3DGraphicsDevice)gd).isD3DEnabledOnDevice();
            }
        }
        return false;
    }

    private static volatile boolean isDWMCompositionEnabled;
    /**
     * Returns true if dwm composition is currently enabled, false otherwise.
     *
     * @return true if dwm composition is enabled, false otherwise
     */
    public static boolean isDWMCompositionEnabled() {
        return isDWMCompositionEnabled;
    }

    /**
     * Called from the native code when DWM composition state changed.
     * May be called multiple times during the lifetime of the application.
     * REMIND: we may want to create a listener mechanism for this.
     *
     * Note: called on the Toolkit thread, no user code or locks are allowed.
     *
     * @param enabled indicates the state of dwm composition
     */
    private static void dwmCompositionChanged(boolean enabled) {
        isDWMCompositionEnabled = enabled;
    }

    /**
     * Used to find out if the OS is Windows Vista or later.
     *
     * @return {@code true} if the OS is Vista or later, {@code false} otherwise
     */
    public static native boolean isVistaOS();
}
