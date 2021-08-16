/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTPermission;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import sun.awt.util.ThreadGroupUtils;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.loops.SurfaceType;
import sun.java2d.opengl.GLXGraphicsConfig;
import sun.java2d.pipe.Region;
import sun.java2d.xr.XRGraphicsConfig;

/**
 * This is an implementation of a GraphicsDevice object for a single
 * X11 screen.
 *
 * @see GraphicsEnvironment
 * @see GraphicsConfiguration
 */
public final class X11GraphicsDevice extends GraphicsDevice
        implements DisplayChangedListener {
    /**
     * X11 screen number. This identifier can become non-valid at any time
     * therefore methods, which is using this id should be ready to it.
     */
    private volatile int screen;
    HashMap<SurfaceType, Object> x11ProxyKeyMap = new HashMap<>();

    private static AWTPermission fullScreenExclusivePermission;
    private static Boolean xrandrExtSupported;
    private final Object configLock = new Object();
    private SunDisplayChanger topLevels = new SunDisplayChanger();
    private DisplayMode origDisplayMode;
    private boolean shutdownHookRegistered;
    private int scale;

    public X11GraphicsDevice(int screennum) {
        this.screen = screennum;
        this.scale = initScaleFactor();
    }

    /**
     * Returns the X11 screen of the device.
     */
    public int getScreen() {
        return screen;
    }

    public Object getProxyKeyFor(SurfaceType st) {
        synchronized (x11ProxyKeyMap) {
            Object o = x11ProxyKeyMap.get(st);
            if (o == null) {
                o = new Object();
                x11ProxyKeyMap.put(st, o);
            }
            return o;
        }
    }

    /**
     * Returns the X11 Display of this device.
     * This method is also in MDrawingSurfaceInfo but need it here
     * to be able to allow a GraphicsConfigTemplate to get the Display.
     */
    public native long getDisplay();

    /**
     * Returns the type of the graphics device.
     * @see #TYPE_RASTER_SCREEN
     * @see #TYPE_PRINTER
     * @see #TYPE_IMAGE_BUFFER
     */
    @Override
    public int getType() {
        return TYPE_RASTER_SCREEN;
    }

    public int scaleUp(int x) {
        return Region.clipRound(x * (double)getScaleFactor());
    }

    public int scaleDown(int x) {
        return Region.clipRound(x / (double)getScaleFactor());
    }

    public Rectangle getBounds() {
        Rectangle rect = pGetBounds(getScreen());
        if (getScaleFactor() != 1) {
            rect.x = scaleDown(rect.x);
            rect.y = scaleDown(rect.y);
            rect.width = scaleDown(rect.width);
            rect.height = scaleDown(rect.height);
        }
        return rect;
    }

    /**
     * Returns the identification string associated with this graphics
     * device.
     */
    @Override
    public String getIDstring() {
        return ":0."+screen;
    }


    GraphicsConfiguration[] configs;
    GraphicsConfiguration defaultConfig;
    HashSet<Integer> doubleBufferVisuals;

    /**
     * Returns all of the graphics
     * configurations associated with this graphics device.
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        if (configs == null) {
            synchronized (configLock) {
                makeConfigurations();
            }
        }
        return configs.clone();
    }

    private void makeConfigurations() {
        if (configs == null) {
            int i = 1;  // Index 0 is always the default config
            int num = getNumConfigs(screen);
            GraphicsConfiguration[] ret = new GraphicsConfiguration[num];
            if (defaultConfig == null) {
                ret [0] = getDefaultConfiguration();
            }
            else {
                ret [0] = defaultConfig;
            }

            boolean glxSupported = X11GraphicsEnvironment.isGLXAvailable();
            boolean xrenderSupported = X11GraphicsEnvironment.isXRenderAvailable();

            boolean dbeSupported = isDBESupported();
            if (dbeSupported && doubleBufferVisuals == null) {
                doubleBufferVisuals = new HashSet<>();
                getDoubleBufferVisuals(screen);
            }
            for ( ; i < num; i++) {
                int visNum = getConfigVisualId(i, screen);
                int depth = getConfigDepth (i, screen);
                if (glxSupported) {
                    ret[i] = GLXGraphicsConfig.getConfig(this, visNum);
                }
                if (ret[i] == null) {
                    boolean doubleBuffer =
                        (dbeSupported &&
                         doubleBufferVisuals.contains(Integer.valueOf(visNum)));

                    if (xrenderSupported) {
                        ret[i] = XRGraphicsConfig.getConfig(this, visNum, depth,
                                getConfigColormap(i, screen), doubleBuffer);
                    } else {
                       ret[i] = X11GraphicsConfig.getConfig(this, visNum, depth,
                              getConfigColormap(i, screen),
                              doubleBuffer);
                    }
                }
            }
            configs = ret;
        }
    }

    /*
     * Returns the number of X11 visuals representable as an
     * X11GraphicsConfig object.
     */
    public native int getNumConfigs(int screen);

    /*
     * Returns the visualid for the given index of graphics configurations.
     */
    public native int getConfigVisualId (int index, int screen);
    /*
     * Returns the depth for the given index of graphics configurations.
     */
    private native int getConfigDepth(int index, int screen);

    /*
     * Returns the colormap for the given index of graphics configurations.
     */
    private native int getConfigColormap(int index, int screen);

    // Whether or not double-buffering extension is supported
    static native boolean isDBESupported();
    // Callback for adding a new double buffer visual into our set
    private void addDoubleBufferVisual(int visNum) {
        doubleBufferVisuals.add(Integer.valueOf(visNum));
    }
    // Enumerates all visuals that support double buffering
    private native void getDoubleBufferVisuals(int screen);

    /**
     * Returns the default graphics configuration
     * associated with this graphics device.
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        if (defaultConfig == null) {
            synchronized (configLock) {
                makeDefaultConfiguration();
            }
        }
        return defaultConfig;
    }

    private void makeDefaultConfiguration() {
        if (defaultConfig == null) {
            int visNum = getConfigVisualId(0, screen);
            if (X11GraphicsEnvironment.isGLXAvailable()) {
                defaultConfig = GLXGraphicsConfig.getConfig(this, visNum);
                if (X11GraphicsEnvironment.isGLXVerbose()) {
                    if (defaultConfig != null) {
                        System.out.print("OpenGL pipeline enabled");
                    } else {
                        System.out.print("Could not enable OpenGL pipeline");
                    }
                    System.out.println(" for default config on screen " +
                                       screen);
                }
            }
            if (defaultConfig == null) {
                int depth = getConfigDepth(0, screen);
                boolean doubleBuffer = false;
                if (isDBESupported() && doubleBufferVisuals == null) {
                    doubleBufferVisuals = new HashSet<>();
                    getDoubleBufferVisuals(screen);
                    doubleBuffer =
                        doubleBufferVisuals.contains(Integer.valueOf(visNum));
                }

                if (X11GraphicsEnvironment.isXRenderAvailable()) {
                    if (X11GraphicsEnvironment.isXRenderVerbose()) {
                        System.out.println("XRender pipeline enabled");
                    }
                    defaultConfig = XRGraphicsConfig.getConfig(this, visNum,
                            depth, getConfigColormap(0, screen),
                            doubleBuffer);
                } else {
                    defaultConfig = X11GraphicsConfig.getConfig(this, visNum,
                                        depth, getConfigColormap(0, screen),
                                        doubleBuffer);
                }
            }
        }
    }

    private static native void enterFullScreenExclusive(long window);
    private static native void exitFullScreenExclusive(long window);
    private static native boolean initXrandrExtension();
    private static native DisplayMode getCurrentDisplayMode(int screen);
    private static native void enumDisplayModes(int screen,
                                                ArrayList<DisplayMode> modes);
    private static native void configDisplayMode(int screen,
                                                 int width, int height,
                                                 int displayMode);
    private static native double getNativeScaleFactor(int screen);
    private native Rectangle pGetBounds(int screenNum);

    /**
     * Returns true only if:
     *   - the Xrandr extension is present
     *   - the necessary Xrandr functions were loaded successfully
     */
    private static synchronized boolean isXrandrExtensionSupported() {
        if (xrandrExtSupported == null) {
            xrandrExtSupported =
                Boolean.valueOf(initXrandrExtension());
        }
        return xrandrExtSupported.booleanValue();
    }

    @Override
    public boolean isFullScreenSupported() {
        boolean fsAvailable = isXrandrExtensionSupported();
        if (fsAvailable) {
            @SuppressWarnings("removal")
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                if (fullScreenExclusivePermission == null) {
                    fullScreenExclusivePermission =
                        new AWTPermission("fullScreenExclusive");
                }
                try {
                    security.checkPermission(fullScreenExclusivePermission);
                } catch (SecurityException e) {
                    return false;
                }
            }
        }
        return fsAvailable;
    }

    @Override
    public boolean isDisplayChangeSupported() {
        return (isFullScreenSupported()
                && (getFullScreenWindow() != null)
                && !((X11GraphicsEnvironment) GraphicsEnvironment
                        .getLocalGraphicsEnvironment()).runningXinerama());
    }

    private static void enterFullScreenExclusive(Window w) {
        X11ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            enterFullScreenExclusive(peer.getWindow());
            peer.setFullScreenExclusiveModeState(true);
        }
    }

    private static void exitFullScreenExclusive(Window w) {
        X11ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            peer.setFullScreenExclusiveModeState(false);
            exitFullScreenExclusive(peer.getWindow());
        }
    }

    @Override
    public synchronized void setFullScreenWindow(Window w) {
        Window old = getFullScreenWindow();
        if (w == old) {
            return;
        }

        boolean fsSupported = isFullScreenSupported();
        if (fsSupported && old != null) {
            // enter windowed mode (and restore original display mode)
            exitFullScreenExclusive(old);
            if (isDisplayChangeSupported()) {
                setDisplayMode(origDisplayMode);
            }
        }

        super.setFullScreenWindow(w);

        if (fsSupported && w != null) {
            // save original display mode
            if (origDisplayMode == null) {
                origDisplayMode = getDisplayMode();
            }

            // enter fullscreen mode
            enterFullScreenExclusive(w);
        }
    }

    private DisplayMode getDefaultDisplayMode() {
        GraphicsConfiguration gc = getDefaultConfiguration();
        Rectangle r = gc.getBounds();
        return new DisplayMode(r.width, r.height,
                               DisplayMode.BIT_DEPTH_MULTI,
                               DisplayMode.REFRESH_RATE_UNKNOWN);
    }

    @Override
    public synchronized DisplayMode getDisplayMode() {
        if (isFullScreenSupported()) {
            DisplayMode mode = getCurrentDisplayMode(screen);
            if (mode == null) {
                mode = getDefaultDisplayMode();
            }
            return mode;
        } else {
            if (origDisplayMode == null) {
                origDisplayMode = getDefaultDisplayMode();
            }
            return origDisplayMode;
        }
    }

    @Override
    public synchronized DisplayMode[] getDisplayModes() {
        if (!isFullScreenSupported()
                || ((X11GraphicsEnvironment) GraphicsEnvironment
                            .getLocalGraphicsEnvironment()).runningXinerama()) {
            // only the current mode will be returned
            return super.getDisplayModes();
        }
        ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
        enumDisplayModes(screen, modes);
        DisplayMode[] retArray = new DisplayMode[modes.size()];
        return modes.toArray(retArray);
    }

    @SuppressWarnings("removal")
    @Override
    public synchronized void setDisplayMode(DisplayMode dm) {
        if (!isDisplayChangeSupported()) {
            super.setDisplayMode(dm);
            return;
        }
        Window w = getFullScreenWindow();
        if (w == null) {
            throw new IllegalStateException("Must be in fullscreen mode " +
                                            "in order to set display mode");
        }
        if (getDisplayMode().equals(dm)) {
            return;
        }
        if (dm == null ||
            (dm = getMatchingDisplayMode(dm)) == null)
        {
            throw new IllegalArgumentException("Invalid display mode");
        }

        if (!shutdownHookRegistered) {
            // register a shutdown hook so that we return to the
            // original DisplayMode when the VM exits (if the application
            // is already in the original DisplayMode at that time, this
            // hook will have no effect)
            shutdownHookRegistered = true;
            PrivilegedAction<Void> a = () -> {
                Runnable r = () -> {
                    Window old = getFullScreenWindow();
                    if (old != null) {
                        exitFullScreenExclusive(old);
                        if (isDisplayChangeSupported()) {
                            setDisplayMode(origDisplayMode);
                        }
                    }
                };
                String name = "Display-Change-Shutdown-Thread-" + screen;
                Thread t = new Thread(
                      ThreadGroupUtils.getRootThreadGroup(), r, name, 0, false);
                t.setContextClassLoader(null);
                Runtime.getRuntime().addShutdownHook(t);
                return null;
            };
            AccessController.doPrivileged(a);
        }

        // switch to the new DisplayMode
        configDisplayMode(screen,
                          dm.getWidth(), dm.getHeight(),
                          dm.getRefreshRate());

        // update bounds of the fullscreen window
        w.setBounds(0, 0, dm.getWidth(), dm.getHeight());

        // configDisplayMode() is synchronous, so the display change will be
        // complete by the time we get here (and it is therefore safe to call
        // displayChanged() now)
        ((X11GraphicsEnvironment)
         GraphicsEnvironment.getLocalGraphicsEnvironment()).displayChanged();
    }

    private synchronized DisplayMode getMatchingDisplayMode(DisplayMode dm) {
        if (!isDisplayChangeSupported()) {
            return null;
        }
        DisplayMode[] modes = getDisplayModes();
        for (DisplayMode mode : modes) {
            if (dm.equals(mode) ||
                (dm.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN &&
                 dm.getWidth() == mode.getWidth() &&
                 dm.getHeight() == mode.getHeight() &&
                 dm.getBitDepth() == mode.getBitDepth()))
            {
                return mode;
            }
        }
        return null;
    }

    /**
     * From the DisplayChangedListener interface; called from
     * X11GraphicsEnvironment when the display mode has been changed.
     */
    @Override
    public synchronized void displayChanged() {
        scale = initScaleFactor();
        // On X11 the visuals do not change, and therefore we don't need
        // to reset the defaultConfig, config, doubleBufferVisuals,
        // neither do we need to reset the native data.

        // pass on to all top-level windows on this screen
        topLevels.notifyListeners();
    }

    /**
     * From the DisplayChangedListener interface; devices do not need
     * to react to this event.
     */
    @Override
    public void paletteChanged() {
    }

    /**
     * Add a DisplayChangeListener to be notified when the display settings
     * are changed.  Typically, only top-level containers need to be added
     * to X11GraphicsDevice.
     */
    public void addDisplayChangedListener(DisplayChangedListener client) {
        topLevels.add(client);
    }

    public int getScaleFactor() {
        return scale;
    }

    public int getNativeScale() {
        return (int)Math.round(getNativeScaleFactor(screen));
    }

    private int initScaleFactor() {

        if (SunGraphicsEnvironment.isUIScaleEnabled()) {

            double debugScale = SunGraphicsEnvironment.getDebugScale();

            if (debugScale >= 1) {
                return (int) debugScale;
            }
            int nativeScale = getNativeScale();
            return nativeScale >= 1 ? nativeScale : 1;
        }

        return 1;
    }

    /**
     * Remove a DisplayChangeListener from this X11GraphicsDevice.
     */
    public void removeDisplayChangedListener(DisplayChangedListener client) {
        topLevels.remove(client);
    }

    public String toString() {
        return ("X11GraphicsDevice[screen="+screen+"]");
    }

    public void invalidate(X11GraphicsDevice device) {
        screen = device.screen;
    }
}
