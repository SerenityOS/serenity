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
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ColorModel;
import java.awt.peer.WindowPeer;
import java.util.ArrayList;
import java.util.Vector;

import sun.awt.windows.WWindowPeer;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.opengl.WGLGraphicsConfig;
import sun.java2d.windows.WindowsFlags;

import static sun.awt.Win32GraphicsEnvironment.debugScaleX;
import static sun.awt.Win32GraphicsEnvironment.debugScaleY;

/**
 * This is an implementation of a GraphicsDevice object for a single
 * Win32 screen.
 *
 * @see GraphicsEnvironment
 * @see GraphicsConfiguration
 */
public class Win32GraphicsDevice extends GraphicsDevice implements
 DisplayChangedListener {
    int screen;
    ColorModel dynamicColorModel;   // updated with dev changes
    ColorModel colorModel;          // static for device
    protected GraphicsConfiguration[] configs;
    protected GraphicsConfiguration defaultConfig;

    private final String idString;
    protected String descString;
    // Note that we do not synchronize access to this variable - it doesn't
    // really matter if a thread does an operation on graphics device which is
    // about to become invalid (or already become) - we are prepared to deal
    // with this on the native level.
    private boolean valid;

    // keep track of top-level windows on this display
    private SunDisplayChanger topLevels = new SunDisplayChanger();
    // REMIND: we may disable the use of pixel formats for some accelerated
    // pipelines which are mutually exclusive with opengl, for which
    // pixel formats were added in the first place
    protected static boolean pfDisabled;
    private static AWTPermission fullScreenExclusivePermission;
    // the original display mode we had before entering the fullscreen
    // mode
    private DisplayMode defaultDisplayMode;
    // activation/deactivation listener for the full-screen window
    private WindowListener fsWindowListener;

    private float scaleX;
    private float scaleY;

    static {

        // 4455041 - Even when ddraw is disabled, ddraw.dll is loaded when
        // pixel format calls are made.  This causes problems when a Java app
        // is run as an NT service.  To prevent the loading of ddraw.dll
        // completely, sun.awt.nopixfmt should be set as well.  Apps which use
        // OpenGL w/ Java probably don't want to set this.
        @SuppressWarnings("removal")
        String nopixfmt = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("sun.awt.nopixfmt"));
        pfDisabled = (nopixfmt != null);
        initIDs();
    }

    private static native void initIDs();

    native void initDevice(int screen);
    native void initNativeScale(int screen);
    native void setNativeScale(int screen, float scaleX, float scaleY);
    native float getNativeScaleX(int screen);
    native float getNativeScaleY(int screen);

    public Win32GraphicsDevice(int screennum) {
        this.screen = screennum;
        // we cache the strings because we want toString() and getIDstring
        // to reflect the original screen number (which may change if the
        // device is removed)
        idString = "\\Display"+screen;
        // REMIND: may be should use class name?
        descString = "Win32GraphicsDevice[screen=" + screen;
        valid = true;

        initDevice(screennum);
        initScaleFactors();
    }

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

    /**
     * Returns the Win32 screen of the device.
     */
    public int getScreen() {
        return screen;
    }

    public float getDefaultScaleX() {
        return scaleX;
    }

    public float getDefaultScaleY() {
        return scaleY;
    }

    private void initScaleFactors() {
        if (SunGraphicsEnvironment.isUIScaleEnabled()) {
            if (debugScaleX > 0 && debugScaleY > 0) {
                scaleX = debugScaleX;
                scaleY = debugScaleY;
                setNativeScale(screen, scaleX, scaleY);
            } else {
                initNativeScale(screen);
                scaleX = getNativeScaleX(screen);
                scaleY = getNativeScaleY(screen);
            }
        } else {
            scaleX = 1;
            scaleY = 1;
        }
    }

    /**
     * Returns whether this is a valid devicie. Device can become
     * invalid as a result of device removal event.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Called from native code when the device was removed.
     *
     * @param defaultScreen the current default screen
     */
    protected void invalidate(int defaultScreen) {
        valid = false;
        screen = defaultScreen;
    }

    /**
     * Returns the identification string associated with this graphics
     * device.
     */
    @Override
    public String getIDstring() {
        return idString;
    }


    /**
     * Returns all of the graphics
     * configurations associated with this graphics device.
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        if (configs==null) {
            if (WindowsFlags.isOGLEnabled() && isDefaultDevice()) {
                defaultConfig = getDefaultConfiguration();
                if (defaultConfig != null) {
                    configs = new GraphicsConfiguration[1];
                    configs[0] = defaultConfig;
                    return configs.clone();
                }
            }

            int max = getMaxConfigs(screen);
            int defaultPixID = getDefaultPixID(screen);
            Vector<GraphicsConfiguration> v = new Vector<>( max );
            if (defaultPixID == 0) {
                // Workaround for failing GDI calls
                defaultConfig = Win32GraphicsConfig.getConfig(this,
                                                              defaultPixID);
                v.addElement(defaultConfig);
            }
            else {
                for (int i = 1; i <= max; i++) {
                    if (isPixFmtSupported(i, screen)) {
                        if (i == defaultPixID) {
                            defaultConfig = Win32GraphicsConfig.getConfig(
                             this, i);
                            v.addElement(defaultConfig);
                        }
                        else {
                            v.addElement(Win32GraphicsConfig.getConfig(
                             this, i));
                        }
                    }
                }
            }
            configs = new GraphicsConfiguration[v.size()];
            v.copyInto(configs);
        }
        return configs.clone();
    }

    /**
     * Returns the maximum number of graphics configurations available, or 1
     * if PixelFormat calls fail or are disabled.
     * This number is less than or equal to the number of graphics
     * configurations supported.
     */
    protected int getMaxConfigs(int screen) {
        if (pfDisabled) {
            return 1;
        } else {
            return getMaxConfigsImpl(screen);
        }
    }

    private native int getMaxConfigsImpl(int screen);

    /**
     * Returns whether or not the PixelFormat indicated by index is
     * supported.  Supported PixelFormats support drawing to a Window
     * (PFD_DRAW_TO_WINDOW), support GDI (PFD_SUPPORT_GDI), and in the
     * case of an 8-bit format (cColorBits <= 8) uses indexed colors
     * (iPixelType == PFD_TYPE_COLORINDEX).
     * We use the index 0 to indicate that PixelFormat calls don't work, or
     * are disabled.  Do not call this function with an index of 0.
     * @param index a PixelFormat index
     */
    private native boolean isPixFmtSupported(int index, int screen);

    /**
     * Returns the PixelFormatID of the default graphics configuration
     * associated with this graphics device, or 0 if PixelFormats calls fail or
     * are disabled.
     */
    protected int getDefaultPixID(int screen) {
        if (pfDisabled) {
            return 0;
        } else {
            return getDefaultPixIDImpl(screen);
        }
    }

    /**
     * Returns the default PixelFormat ID from GDI.  Do not call if PixelFormats
     * are disabled.
     */
    private native int getDefaultPixIDImpl(int screen);

    /**
     * Returns the default graphics configuration
     * associated with this graphics device.
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        if (defaultConfig == null) {
            // first try to create a WGLGraphicsConfig if OGL is enabled
            // REMIND: the WGL code does not yet work properly in multimon
            // situations, so we will fallback on GDI if we are not on the
            // default device...
            if (WindowsFlags.isOGLEnabled() && isDefaultDevice()) {
                int defPixID = WGLGraphicsConfig.getDefaultPixFmt(screen);
                defaultConfig = WGLGraphicsConfig.getConfig(this, defPixID);
                if (WindowsFlags.isOGLVerbose()) {
                    if (defaultConfig != null) {
                        System.out.print("OpenGL pipeline enabled");
                    } else {
                        System.out.print("Could not enable OpenGL pipeline");
                    }
                    System.out.println(" for default config on screen " +
                                       screen);
                }
            }

            // Fix for 4669614.  Most apps are not concerned with PixelFormats,
            // yet we ALWAYS used them for determining ColorModels and such.
            // By passing in 0 as the PixelFormatID here, we signal that
            // PixelFormats should not be used, thus avoid loading the opengl
            // library.  Apps concerned with PixelFormats can still use
            // GraphicsConfiguration.getConfigurations().
            // Note that calling native pixel format functions tends to cause
            // problems between those functions (which are OpenGL-related)
            // and our use of DirectX.  For example, some Matrox boards will
            // crash or hang calling these functions when any app is running
            // in DirectX fullscreen mode.  So avoiding these calls unless
            // absolutely necessary is preferable.
            if (defaultConfig == null) {
                defaultConfig = Win32GraphicsConfig.getConfig(this, 0);
            }
        }
        return defaultConfig;
    }

    @Override
    public String toString() {
        return valid ? descString + "]" : descString + ", removed]";
    }

    /**
     * Returns true if this is the default GraphicsDevice for the
     * GraphicsEnvironment.
     */
    private boolean isDefaultDevice() {
        return (this ==
                GraphicsEnvironment.
                    getLocalGraphicsEnvironment().getDefaultScreenDevice());
    }

    private static boolean isFSExclusiveModeAllowed() {
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
        return true;
    }

    /**
     * returns true unless we're not allowed to use fullscreen mode.
     */
    @Override
    public boolean isFullScreenSupported() {
        return isFSExclusiveModeAllowed();
    }

    @Override
    public synchronized void setFullScreenWindow(Window w) {
        Window old = getFullScreenWindow();
        if (w == old) {
            return;
        }
        if (!isFullScreenSupported()) {
            super.setFullScreenWindow(w);
            return;
        }

        // Enter windowed mode.
        if (old != null) {
            // restore the original display mode
            if (defaultDisplayMode != null) {
                setDisplayMode(defaultDisplayMode);
                // we set the default display mode to null here
                // because the default mode could change during
                // the life of the application (user can change it through
                // the desktop properties dialog, for example), so
                // we need to record it every time prior to
                // entering the fullscreen mode.
                defaultDisplayMode = null;
            }
            WWindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(old);
            if (peer != null) {
                peer.setFullScreenExclusiveModeState(false);
                // we used to destroy the buffers on exiting fs mode, this
                // is no longer needed since fs change will cause a surface
                // data replacement
                synchronized(peer) {
                    exitFullScreenExclusive(screen, peer);
                }
            }
            removeFSWindowListener(old);
        }
        super.setFullScreenWindow(w);
        if (w != null) {
            // always record the default display mode prior to going
            // fullscreen
            defaultDisplayMode = getDisplayMode();
            addFSWindowListener(w);
            // Enter full screen exclusive mode.
            WWindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
            if (peer != null) {
                synchronized(peer) {
                    enterFullScreenExclusive(screen, peer);
                    // Note: removed replaceSurfaceData() call because
                    // changing the window size or making it visible
                    // will cause this anyway, and both of these events happen
                    // as part of switching into fullscreen mode.
                }
                peer.setFullScreenExclusiveModeState(true);
            }

            // fix for 4868278
            peer.updateGC();
        }
    }

    // Entering and exiting full-screen mode are done within a
    // tree-lock and should never lock on any resources which are
    // required by other threads which may have them and may require
    // the tree-lock.
    // REMIND: in the future these methods may need to become protected so that
    // subclasses could override them and use appropriate api other than GDI
    // for implementing these functions.
    protected native void enterFullScreenExclusive(int screen, WindowPeer w);
    protected native void exitFullScreenExclusive(int screen, WindowPeer w);

    @Override
    public boolean isDisplayChangeSupported() {
        return (isFullScreenSupported() && getFullScreenWindow() != null);
    }

    @Override
    public synchronized void setDisplayMode(DisplayMode dm) {
        if (!isDisplayChangeSupported()) {
            super.setDisplayMode(dm);
            return;
        }
        if (dm == null || (dm = getMatchingDisplayMode(dm)) == null) {
            throw new IllegalArgumentException("Invalid display mode");
        }
        if (getDisplayMode().equals(dm)) {
            return;
        }
        Window w = getFullScreenWindow();
        if (w != null) {
            WWindowPeer peer = AWTAccessor.getComponentAccessor().getPeer(w);
            configDisplayMode(screen, peer, dm.getWidth(), dm.getHeight(),
                dm.getBitDepth(), dm.getRefreshRate());
            // resize the fullscreen window to the dimensions of the new
            // display mode
            Rectangle screenBounds = getDefaultConfiguration().getBounds();
            w.setBounds(screenBounds.x, screenBounds.y,
                        screenBounds.width, screenBounds.height);
            // Note: no call to replaceSurfaceData is required here since
            // replacement will be caused by an upcoming display change event
        } else {
            throw new IllegalStateException("Must be in fullscreen mode " +
                                            "in order to set display mode");
        }
    }

    protected native DisplayMode getCurrentDisplayMode(int screen);
    protected native void configDisplayMode(int screen, WindowPeer w, int width,
                                          int height, int bitDepth,
                                          int refreshRate);
    protected native void enumDisplayModes(int screen, ArrayList<DisplayMode> modes);

    @Override
    public synchronized DisplayMode getDisplayMode() {
        DisplayMode res = getCurrentDisplayMode(screen);
        return res;
    }

    @Override
    public synchronized DisplayMode[] getDisplayModes() {
        ArrayList<DisplayMode> modes = new ArrayList<>();
        enumDisplayModes(screen, modes);
        int listSize = modes.size();
        DisplayMode[] retArray = new DisplayMode[listSize];
        for (int i = 0; i < listSize; i++) {
            retArray[i] = modes.get(i);
        }
        return retArray;
    }

    protected synchronized DisplayMode getMatchingDisplayMode(DisplayMode dm) {
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

    /*
     * From the DisplayChangeListener interface.
     * Called from Win32GraphicsEnvironment when the display settings have
     * changed.
     */
    @Override
    public void displayChanged() {
        dynamicColorModel = null;
        defaultConfig = null;
        configs = null;
        initScaleFactors();
        // pass on to all top-level windows on this display
        topLevels.notifyListeners();
    }

    /**
     * Part of the DisplayChangedListener interface: devices
     * do not need to react to this event
     */
    @Override
    public void paletteChanged() {
    }

    /*
     * Add a DisplayChangeListener to be notified when the display settings
     * are changed.  Typically, only top-level containers need to be added
     * to Win32GraphicsDevice.
     */
    public void addDisplayChangedListener(DisplayChangedListener client) {
        topLevels.add(client);
    }

    /*
     * Remove a DisplayChangeListener from this Win32GraphicsDevice
     */
     public void removeDisplayChangedListener(DisplayChangedListener client) {
        topLevels.remove(client);
    }

    /**
     * Creates and returns the color model associated with this device
     */
    private native ColorModel makeColorModel (int screen,
                                              boolean dynamic);

    /**
     * Returns a dynamic ColorModel which is updated when there
     * are any changes (e.g., palette changes) in the device
     */
    public ColorModel getDynamicColorModel() {
        if (dynamicColorModel == null) {
            dynamicColorModel = makeColorModel(screen, true);
        }
        return dynamicColorModel;
    }

    /**
     * Returns the non-dynamic ColorModel associated with this device
     */
    public ColorModel getColorModel() {
        if (colorModel == null)  {
            colorModel = makeColorModel(screen, false);
        }
        return colorModel;
    }

    /**
     * WindowAdapter class responsible for de/iconifying full-screen window
     * of this device.
     *
     * The listener restores the default display mode when window is iconified
     * and sets it back to the one set by the user on de-iconification.
     */
    private static class Win32FSWindowAdapter extends WindowAdapter {
        private Win32GraphicsDevice device;
        private DisplayMode dm;

        Win32FSWindowAdapter(Win32GraphicsDevice device) {
            this.device = device;
        }

        private void setFSWindowsState(Window other, int state) {
            GraphicsDevice[] gds =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getScreenDevices();
            // check if the de/activation was caused by other
            // fs window and ignore the event if that's the case
            if (other != null) {
                for (GraphicsDevice gd : gds) {
                    if (other == gd.getFullScreenWindow()) {
                        return;
                    }
                }
            }
            // otherwise apply state to all fullscreen windows
            for (GraphicsDevice gd : gds) {
                Window fsw = gd.getFullScreenWindow();
                if (fsw instanceof Frame) {
                    ((Frame)fsw).setExtendedState(state);
                }
            }
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            setFSWindowsState(e.getOppositeWindow(), Frame.ICONIFIED);
        }

        @Override
        public void windowActivated(WindowEvent e) {
            setFSWindowsState(e.getOppositeWindow(), Frame.NORMAL);
        }

        @Override
        public void windowIconified(WindowEvent e) {
            // restore the default display mode for this device
            DisplayMode ddm = device.defaultDisplayMode;
            if (ddm != null) {
                dm = device.getDisplayMode();
                device.setDisplayMode(ddm);
            }
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            // restore the user-set display mode for this device
            if (dm != null) {
                device.setDisplayMode(dm);
                dm = null;
            }
        }
    }

    /**
     * Adds a WindowListener to be used as
     * activation/deactivation listener for the current full-screen window.
     *
     * @param w full-screen window
     */
    protected void addFSWindowListener(final Window w) {
        // Note: even though we create a listener for Window instances of
        // fs windows they will not receive window events.
        fsWindowListener = new Win32FSWindowAdapter(this);

        // Fix for 6709453. Using invokeLater to avoid listening
        // for the events already posted to the queue.
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                w.addWindowListener(fsWindowListener);
            }
        });
    }

    /**
     * Removes the fs window listener.
     *
     * @param w full-screen window
     */
    protected void removeFSWindowListener(Window w) {
        w.removeWindowListener(fsWindowListener);
        fsWindowListener = null;
    }
}
