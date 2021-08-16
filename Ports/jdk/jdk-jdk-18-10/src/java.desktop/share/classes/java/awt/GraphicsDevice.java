/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.image.ColorModel;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

/**
 * The {@code GraphicsDevice} class describes the graphics devices
 * that might be available in a particular graphics environment.  These
 * include screen and printer devices. Note that there can be many screens
 * and many printers in an instance of {@link GraphicsEnvironment}. Each
 * graphics device has one or more {@link GraphicsConfiguration} objects
 * associated with it.  These objects specify the different configurations
 * in which the {@code GraphicsDevice} can be used.
 * <p>
 * In a multi-screen environment, the {@code GraphicsConfiguration}
 * objects can be used to render components on multiple screens.  The
 * following code sample demonstrates how to create a {@code JFrame}
 * object for each {@code GraphicsConfiguration} on each screen
 * device in the {@code GraphicsEnvironment}:
 * <pre>{@code
 *   GraphicsEnvironment ge = GraphicsEnvironment.
 *   getLocalGraphicsEnvironment();
 *   GraphicsDevice[] gs = ge.getScreenDevices();
 *   for (int j = 0; j < gs.length; j++) {
 *      GraphicsDevice gd = gs[j];
 *      GraphicsConfiguration[] gc =
 *      gd.getConfigurations();
 *      for (int i=0; i < gc.length; i++) {
 *         JFrame f = new
 *         JFrame(gs[j].getDefaultConfiguration());
 *         Canvas c = new Canvas(gc[i]);
 *         Rectangle gcBounds = gc[i].getBounds();
 *         int xoffs = gcBounds.x;
 *         int yoffs = gcBounds.y;
 *         f.getContentPane().add(c);
 *         f.setLocation((i*50)+xoffs, (i*60)+yoffs);
 *         f.show();
 *      }
 *   }
 * }</pre>
 * <p>
 * For more information on full-screen exclusive mode API, see the
 * <a href="https://docs.oracle.com/javase/tutorial/extra/fullscreen/index.html">
 * Full-Screen Exclusive Mode API Tutorial</a>.
 *
 * @see GraphicsEnvironment
 * @see GraphicsConfiguration
 */
public abstract class GraphicsDevice {

    private Window fullScreenWindow;
    private AppContext fullScreenAppContext; // tracks which AppContext
                                             // created the FS window
    // this lock is used for making synchronous changes to the AppContext's
    // current full screen window
    private final Object fsAppContextLock = new Object();

    private Rectangle windowedModeBounds;

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Instances must be obtained from a suitable factory or query method.
     * @see GraphicsEnvironment#getScreenDevices
     * @see GraphicsEnvironment#getDefaultScreenDevice
     * @see GraphicsConfiguration#getDevice
     */
    protected GraphicsDevice() {
    }

    /**
     * Device is a raster screen.
     */
    public static final int TYPE_RASTER_SCREEN          = 0;

    /**
     * Device is a printer.
     */
    public static final int TYPE_PRINTER                = 1;

    /**
     * Device is an image buffer.  This buffer can reside in device
     * or system memory but it is not physically viewable by the user.
     */
    public static final int TYPE_IMAGE_BUFFER           = 2;

    /**
     * Kinds of translucency supported by the underlying system.
     *
     * @see #isWindowTranslucencySupported
     *
     * @since 1.7
     */
    public static enum WindowTranslucency {
        /**
         * Represents support in the underlying system for windows each pixel
         * of which is guaranteed to be either completely opaque, with
         * an alpha value of 1.0, or completely transparent, with an alpha
         * value of 0.0.
         */
        PERPIXEL_TRANSPARENT,
        /**
         * Represents support in the underlying system for windows all of
         * the pixels of which have the same alpha value between or including
         * 0.0 and 1.0.
         */
        TRANSLUCENT,
        /**
         * Represents support in the underlying system for windows that
         * contain or might contain pixels with arbitrary alpha values
         * between and including 0.0 and 1.0.
         */
        PERPIXEL_TRANSLUCENT;
    }

    /**
     * Returns the type of this {@code GraphicsDevice}.
     * @return the type of this {@code GraphicsDevice}, which can
     * either be TYPE_RASTER_SCREEN, TYPE_PRINTER or TYPE_IMAGE_BUFFER.
     * @see #TYPE_RASTER_SCREEN
     * @see #TYPE_PRINTER
     * @see #TYPE_IMAGE_BUFFER
     */
    public abstract int getType();

    /**
     * Returns the identification string associated with this
     * {@code GraphicsDevice}.
     * <p>
     * A particular program might use more than one
     * {@code GraphicsDevice} in a {@code GraphicsEnvironment}.
     * This method returns a {@code String} identifying a
     * particular {@code GraphicsDevice} in the local
     * {@code GraphicsEnvironment}.  Although there is
     * no public method to set this {@code String}, a programmer can
     * use the {@code String} for debugging purposes.  Vendors of
     * the Java Runtime Environment can
     * format the return value of the {@code String}.  To determine
     * how to interpret the value of the {@code String}, contact the
     * vendor of your Java Runtime.  To find out who the vendor is, from
     * your program, call the
     * {@link System#getProperty(String) getProperty} method of the
     * System class with "java.vendor".
     * @return a {@code String} that is the identification
     * of this {@code GraphicsDevice}.
     */
    public abstract String getIDstring();

    /**
     * Returns all of the {@code GraphicsConfiguration}
     * objects associated with this {@code GraphicsDevice}.
     * @return an array of {@code GraphicsConfiguration}
     * objects that are associated with this
     * {@code GraphicsDevice}.
     */
    public abstract GraphicsConfiguration[] getConfigurations();

    /**
     * Returns the default {@code GraphicsConfiguration}
     * associated with this {@code GraphicsDevice}.
     * @return the default {@code GraphicsConfiguration}
     * of this {@code GraphicsDevice}.
     */
    public abstract GraphicsConfiguration getDefaultConfiguration();

    /**
     * Returns the "best" configuration possible that passes the
     * criteria defined in the {@link GraphicsConfigTemplate}.
     * @param gct the {@code GraphicsConfigTemplate} object
     * used to obtain a valid {@code GraphicsConfiguration}
     * @return a {@code GraphicsConfiguration} that passes
     * the criteria defined in the specified
     * {@code GraphicsConfigTemplate}.
     * @see GraphicsConfigTemplate
     */
    public GraphicsConfiguration
           getBestConfiguration(GraphicsConfigTemplate gct) {
        GraphicsConfiguration[] configs = getConfigurations();
        return gct.getBestConfiguration(configs);
    }

    /**
     * Returns {@code true} if this {@code GraphicsDevice}
     * supports full-screen exclusive mode.
     * If a SecurityManager is installed, its
     * {@code checkPermission} method will be called
     * with {@code AWTPermission("fullScreenExclusive")}.
     * {@code isFullScreenSupported} returns true only if
     * that permission is granted.
     * @return whether full-screen exclusive mode is available for
     * this graphics device
     * @see java.awt.AWTPermission
     * @since 1.4
     */
    public boolean isFullScreenSupported() {
        return false;
    }

    /**
     * Enter full-screen mode, or return to windowed mode.  The entered
     * full-screen mode may be either exclusive or simulated.  Exclusive
     * mode is only available if {@code isFullScreenSupported}
     * returns {@code true}.
     * <p>
     * Exclusive mode implies:
     * <ul>
     * <li>Windows cannot overlap the full-screen window.  All other application
     * windows will always appear beneath the full-screen window in the Z-order.
     * <li>There can be only one full-screen window on a device at any time,
     * so calling this method while there is an existing full-screen Window
     * will cause the existing full-screen window to
     * return to windowed mode.
     * <li>Input method windows are disabled.  It is advisable to call
     * {@code Component.enableInputMethods(false)} to make a component
     * a non-client of the input method framework.
     * </ul>
     * <p>
     * The simulated full-screen mode places and resizes the window to the maximum
     * possible visible area of the screen. However, the native windowing system
     * may modify the requested geometry-related data, so that the {@code Window} object
     * is placed and sized in a way that corresponds closely to the desktop settings.
     * <p>
     * When entering full-screen mode, if the window to be used as a
     * full-screen window is not visible, this method will make it visible.
     * It will remain visible when returning to windowed mode.
     * <p>
     * When entering full-screen mode, all the translucency effects are reset for
     * the window. Its shape is set to {@code null}, the opacity value is set to
     * 1.0f, and the background color alpha is set to 255 (completely opaque).
     * These values are not restored when returning to windowed mode.
     * <p>
     * It is unspecified and platform-dependent how decorated windows operate
     * in full-screen mode. For this reason, it is recommended to turn off
     * the decorations in a {@code Frame} or {@code Dialog} object by using the
     * {@code setUndecorated} method.
     * <p>
     * When returning to windowed mode from an exclusive full-screen window,
     * any display changes made by calling {@code setDisplayMode} are
     * automatically restored to their original state.
     *
     * @param w a window to use as the full-screen window; {@code null}
     * if returning to windowed mode.  Some platforms expect the
     * fullscreen window to be a top-level component (i.e., a {@code Frame});
     * therefore it is preferable to use a {@code Frame} here rather than a
     * {@code Window}.
     *
     * @see #isFullScreenSupported
     * @see #getFullScreenWindow
     * @see #setDisplayMode
     * @see Component#enableInputMethods
     * @see Component#setVisible
     * @see Frame#setUndecorated
     * @see Dialog#setUndecorated
     *
     * @since 1.4
     */
    public void setFullScreenWindow(Window w) {
        if (w != null) {
            if (w.getShape() != null) {
                w.setShape(null);
            }
            if (w.getOpacity() < 1.0f) {
                w.setOpacity(1.0f);
            }
            if (!w.isOpaque()) {
                Color bgColor = w.getBackground();
                bgColor = new Color(bgColor.getRed(), bgColor.getGreen(),
                                    bgColor.getBlue(), 255);
                w.setBackground(bgColor);
            }
            // Check if this window is in fullscreen mode on another device.
            final GraphicsConfiguration gc = w.getGraphicsConfiguration();
            if (gc != null && gc.getDevice() != this
                    && gc.getDevice().getFullScreenWindow() == w) {
                gc.getDevice().setFullScreenWindow(null);
            }
        }
        if (fullScreenWindow != null && windowedModeBounds != null) {
            // if the window went into fs mode before it was realized it may
            // have (0,0) dimensions
            if (windowedModeBounds.width  == 0) windowedModeBounds.width  = 1;
            if (windowedModeBounds.height == 0) windowedModeBounds.height = 1;
            fullScreenWindow.setBounds(windowedModeBounds);
        }
        // Set the full screen window
        synchronized (fsAppContextLock) {
            // Associate fullscreen window with current AppContext
            if (w == null) {
                fullScreenAppContext = null;
            } else {
                fullScreenAppContext = AppContext.getAppContext();
            }
            fullScreenWindow = w;
        }
        if (fullScreenWindow != null) {
            windowedModeBounds = fullScreenWindow.getBounds();
            // Note that we use the graphics configuration of the device,
            // not the window's, because we're setting the fs window for
            // this device.
            final GraphicsConfiguration gc = getDefaultConfiguration();
            final Rectangle screenBounds = gc.getBounds();
            if (SunToolkit.isDispatchThreadForAppContext(fullScreenWindow)) {
                // Update graphics configuration here directly and do not wait
                // asynchronous notification from the peer. Note that
                // setBounds() will reset a GC, if it was set incorrectly.
                fullScreenWindow.setGraphicsConfiguration(gc);
            }
            fullScreenWindow.setBounds(screenBounds.x, screenBounds.y,
                                       screenBounds.width, screenBounds.height);
            fullScreenWindow.setVisible(true);
            fullScreenWindow.toFront();
        }
    }

    /**
     * Returns the {@code Window} object representing the
     * full-screen window if the device is in full-screen mode.
     *
     * @return the full-screen window, or {@code null} if the device is
     * not in full-screen mode.
     * @see #setFullScreenWindow(Window)
     * @since 1.4
     */
    public Window getFullScreenWindow() {
        Window returnWindow = null;
        synchronized (fsAppContextLock) {
            // Only return a handle to the current fs window if we are in the
            // same AppContext that set the fs window
            if (fullScreenAppContext == AppContext.getAppContext()) {
                returnWindow = fullScreenWindow;
            }
        }
        return returnWindow;
    }

    /**
     * Returns {@code true} if this {@code GraphicsDevice}
     * supports low-level display changes.
     * On some platforms low-level display changes may only be allowed in
     * full-screen exclusive mode (i.e., if {@link #isFullScreenSupported()}
     * returns {@code true} and the application has already entered
     * full-screen mode using {@link #setFullScreenWindow}).
     * @return whether low-level display changes are supported for this
     * graphics device.
     * @see #isFullScreenSupported
     * @see #setDisplayMode
     * @see #setFullScreenWindow
     * @since 1.4
     */
    public boolean isDisplayChangeSupported() {
        return false;
    }

    /**
     * Sets the display mode of this graphics device. This is only allowed
     * if {@link #isDisplayChangeSupported()} returns {@code true} and may
     * require first entering full-screen exclusive mode using
     * {@link #setFullScreenWindow} providing that full-screen exclusive mode is
     * supported (i.e., {@link #isFullScreenSupported()} returns
     * {@code true}).
     * <p>
     *
     * The display mode must be one of the display modes returned by
     * {@link #getDisplayModes()}, with one exception: passing a display mode
     * with {@link DisplayMode#REFRESH_RATE_UNKNOWN} refresh rate will result in
     * selecting a display mode from the list of available display modes with
     * matching width, height and bit depth.
     * However, passing a display mode with {@link DisplayMode#BIT_DEPTH_MULTI}
     * for bit depth is only allowed if such mode exists in the list returned by
     * {@link #getDisplayModes()}.
     * <p>
     * Example code:
     * <pre><code>
     * Frame frame;
     * DisplayMode newDisplayMode;
     * GraphicsDevice gd;
     * // create a Frame, select desired DisplayMode from the list of modes
     * // returned by gd.getDisplayModes() ...
     *
     * if (gd.isFullScreenSupported()) {
     *     gd.setFullScreenWindow(frame);
     * } else {
     *    // proceed in non-full-screen mode
     *    frame.setSize(...);
     *    frame.setLocation(...);
     *    frame.setVisible(true);
     * }
     *
     * if (gd.isDisplayChangeSupported()) {
     *     gd.setDisplayMode(newDisplayMode);
     * }
     * </code></pre>
     *
     * @param dm The new display mode of this graphics device.
     * @exception IllegalArgumentException if the {@code DisplayMode}
     * supplied is {@code null}, or is not available in the array returned
     * by {@code getDisplayModes}
     * @exception UnsupportedOperationException if
     * {@code isDisplayChangeSupported} returns {@code false}
     * @see #getDisplayMode
     * @see #getDisplayModes
     * @see #isDisplayChangeSupported
     * @since 1.4
     */
    public void setDisplayMode(DisplayMode dm) {
        throw new UnsupportedOperationException("Cannot change display mode");
    }

    /**
     * Returns the current display mode of this
     * {@code GraphicsDevice}.
     * The returned display mode is allowed to have a refresh rate
     * {@link DisplayMode#REFRESH_RATE_UNKNOWN} if it is indeterminate.
     * Likewise, the returned display mode is allowed to have a bit depth
     * {@link DisplayMode#BIT_DEPTH_MULTI} if it is indeterminate or if multiple
     * bit depths are supported.
     * @return the current display mode of this graphics device.
     * @see #setDisplayMode(DisplayMode)
     * @since 1.4
     */
    public DisplayMode getDisplayMode() {
        GraphicsConfiguration gc = getDefaultConfiguration();
        Rectangle r = gc.getBounds();
        ColorModel cm = gc.getColorModel();
        return new DisplayMode(r.width, r.height, cm.getPixelSize(), 0);
    }

    /**
     * Returns all display modes available for this
     * {@code GraphicsDevice}.
     * The returned display modes are allowed to have a refresh rate
     * {@link DisplayMode#REFRESH_RATE_UNKNOWN} if it is indeterminate.
     * Likewise, the returned display modes are allowed to have a bit depth
     * {@link DisplayMode#BIT_DEPTH_MULTI} if it is indeterminate or if multiple
     * bit depths are supported.
     * @return all of the display modes available for this graphics device.
     * @since 1.4
     */
    public DisplayMode[] getDisplayModes() {
        return new DisplayMode[] { getDisplayMode() };
    }

    /**
     * This method returns the number of bytes available in
     * accelerated memory on this device.
     * Some images are created or cached
     * in accelerated memory on a first-come,
     * first-served basis.  On some operating systems,
     * this memory is a finite resource.  Calling this method
     * and scheduling the creation and flushing of images carefully may
     * enable applications to make the most efficient use of
     * that finite resource.
     * <br>
     * Note that the number returned is a snapshot of how much
     * memory is available; some images may still have problems
     * being allocated into that memory.  For example, depending
     * on operating system, driver, memory configuration, and
     * thread situations, the full extent of the size reported
     * may not be available for a given image.  There are further
     * inquiry methods on the {@link ImageCapabilities} object
     * associated with a VolatileImage that can be used to determine
     * whether a particular VolatileImage has been created in accelerated
     * memory.
     * @return number of bytes available in accelerated memory.
     * A negative return value indicates that the amount of accelerated memory
     * on this GraphicsDevice is indeterminate.
     * @see java.awt.image.VolatileImage#flush
     * @see ImageCapabilities#isAccelerated
     * @since 1.4
     */
    public int getAvailableAcceleratedMemory() {
        return -1;
    }

    /**
     * Returns whether the given level of translucency is supported by
     * this graphics device.
     *
     * @param translucencyKind a kind of translucency support
     * @return whether the given translucency kind is supported
     *
     * @since 1.7
     */
    public boolean isWindowTranslucencySupported(WindowTranslucency translucencyKind) {
        switch (translucencyKind) {
            case PERPIXEL_TRANSPARENT:
                return isWindowShapingSupported();
            case TRANSLUCENT:
                return isWindowOpacitySupported();
            case PERPIXEL_TRANSLUCENT:
                return isWindowPerpixelTranslucencySupported();
        }
        return false;
    }

    /**
     * Returns whether the windowing system supports changing the shape
     * of top-level windows.
     * Note that this method may sometimes return true, but the native
     * windowing system may still not support the concept of
     * shaping (due to the bugs in the windowing system).
     */
    static boolean isWindowShapingSupported() {
        Toolkit curToolkit = Toolkit.getDefaultToolkit();
        if (!(curToolkit instanceof SunToolkit)) {
            return false;
        }
        return ((SunToolkit)curToolkit).isWindowShapingSupported();
    }

    /**
     * Returns whether the windowing system supports changing the opacity
     * value of top-level windows.
     * Note that this method may sometimes return true, but the native
     * windowing system may still not support the concept of
     * translucency (due to the bugs in the windowing system).
     */
    static boolean isWindowOpacitySupported() {
        Toolkit curToolkit = Toolkit.getDefaultToolkit();
        if (!(curToolkit instanceof SunToolkit)) {
            return false;
        }
        return ((SunToolkit)curToolkit).isWindowOpacitySupported();
    }

    boolean isWindowPerpixelTranslucencySupported() {
        /*
         * Per-pixel alpha is supported if all the conditions are TRUE:
         *    1. The toolkit is a sort of SunToolkit
         *    2. The toolkit supports translucency in general
         *        (isWindowTranslucencySupported())
         *    3. There's at least one translucency-capable
         *        GraphicsConfiguration
         */
        Toolkit curToolkit = Toolkit.getDefaultToolkit();
        if (!(curToolkit instanceof SunToolkit)) {
            return false;
        }
        if (!((SunToolkit)curToolkit).isWindowTranslucencySupported()) {
            return false;
        }

        // TODO: cache translucency capable GC
        return getTranslucencyCapableGC() != null;
    }

    GraphicsConfiguration getTranslucencyCapableGC() {
        // If the default GC supports translucency return true.
        // It is important to optimize the verification this way,
        // see CR 6661196 for more details.
        GraphicsConfiguration defaultGC = getDefaultConfiguration();
        if (defaultGC.isTranslucencyCapable()) {
            return defaultGC;
        }

        // ... otherwise iterate through all the GCs.
        GraphicsConfiguration[] configs = getConfigurations();
        for (int j = 0; j < configs.length; j++) {
            if (configs[j].isTranslucencyCapable()) {
                return configs[j];
            }
        }

        return null;
    }
}
