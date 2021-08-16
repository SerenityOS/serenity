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

package sun.java2d;

import java.awt.AWTError;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.peer.ComponentPeer;
import java.security.AccessController;
import java.util.Locale;
import java.util.TreeMap;

import sun.awt.DisplayChangedListener;
import sun.awt.SunDisplayChanger;
import sun.font.FontManager;
import sun.font.FontManagerFactory;
import sun.font.FontManagerForSGE;
import sun.java2d.pipe.Region;
import sun.security.action.GetPropertyAction;

/**
 * This is an implementation of a GraphicsEnvironment object for the
 * default local GraphicsEnvironment.
 *
 * @see GraphicsDevice
 * @see GraphicsConfiguration
 */
public abstract class SunGraphicsEnvironment extends GraphicsEnvironment
    implements DisplayChangedListener {

    /** Establish the default font to be used by SG2D. */
    private final Font defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);

    @SuppressWarnings("removal")
    private static final boolean uiScaleEnabled
            = "true".equals(AccessController.doPrivileged(
            new GetPropertyAction("sun.java2d.uiScale.enabled", "true")));

    private static final double debugScale =
            uiScaleEnabled ? getScaleFactor("sun.java2d.uiScale") : -1;

    protected GraphicsDevice[] screens;

    /**
     * Returns an array of all of the screen devices.
     */
    public synchronized GraphicsDevice[] getScreenDevices() {
        GraphicsDevice[] ret = screens;
        if (ret == null) {
            int num = getNumScreens();
            ret = new GraphicsDevice[num];
            for (int i = 0; i < num; i++) {
                ret[i] = makeScreenDevice(i);
            }
            screens = ret;
        }
        return ret;
    }

    /**
     * Returns the number of screen devices of this graphics environment.
     *
     * @return the number of screen devices of this graphics environment
     */
    protected abstract int getNumScreens();

    /**
     * Create and return the screen device with the specified number. The
     * device with number {@code 0} will be the default device (returned
     * by {@link #getDefaultScreenDevice()}.
     *
     * @param screennum the number of the screen to create
     *
     * @return the created screen device
     */
    protected abstract GraphicsDevice makeScreenDevice(int screennum);

    /**
     * Returns the default screen graphics device.
     */
    public GraphicsDevice getDefaultScreenDevice() {
        GraphicsDevice[] screens = getScreenDevices();
        if (screens.length == 0) {
            throw new AWTError("no screen devices");
        }
        return screens[0];
    }

    /**
     * Returns a Graphics2D object for rendering into the
     * given BufferedImage.
     * @throws NullPointerException if BufferedImage argument is null
     */
    public Graphics2D createGraphics(BufferedImage img) {
        if (img == null) {
            throw new NullPointerException("BufferedImage cannot be null");
        }
        SurfaceData sd = SurfaceData.getPrimarySurfaceData(img);
        return new SunGraphics2D(sd, Color.white, Color.black, defaultFont);
    }

    public static FontManagerForSGE getFontManagerForSGE() {
        FontManager fm = FontManagerFactory.getInstance();
        return (FontManagerForSGE) fm;
    }

    /* Modifies the behaviour of a subsequent call to preferLocaleFonts()
     * to use Mincho instead of Gothic for dialoginput in JA locales
     * on windows. Not needed on other platforms.
     *
     * @deprecated as of JDK9. To be removed in a future release
     */
    @Deprecated
    public static void useAlternateFontforJALocales() {
        getFontManagerForSGE().useAlternateFontforJALocales();
    }

     /**
     * Returns all fonts available in this environment.
     */
    public Font[] getAllFonts() {
        FontManagerForSGE fm = getFontManagerForSGE();
        Font[] installedFonts = fm.getAllInstalledFonts();
        Font[] created = fm.getCreatedFonts();
        if (created == null || created.length == 0) {
            return installedFonts;
        } else {
            int newlen = installedFonts.length + created.length;
            Font [] fonts = java.util.Arrays.copyOf(installedFonts, newlen);
            System.arraycopy(created, 0, fonts,
                             installedFonts.length, created.length);
            return fonts;
        }
    }

    public String[] getAvailableFontFamilyNames(Locale requestedLocale) {
        FontManagerForSGE fm = getFontManagerForSGE();
        String[] installed = fm.getInstalledFontFamilyNames(requestedLocale);
        /* Use a new TreeMap as used in getInstalledFontFamilyNames
         * and insert all the keys in lower case, so that the sort order
         * is the same as the installed families. This preserves historical
         * behaviour and inserts new families in the right place.
         * It would have been marginally more efficient to directly obtain
         * the tree map and just insert new entries, but not so much as
         * to justify the extra internal interface.
         */
        TreeMap<String, String> map = fm.getCreatedFontFamilyNames();
        if (map == null || map.size() == 0) {
            return installed;
        } else {
            for (int i=0; i<installed.length; i++) {
                map.put(installed[i].toLowerCase(requestedLocale),
                        installed[i]);
            }
            String[] retval = map.values().toArray(new String[0]);
            return retval;
        }
    }

    public String[] getAvailableFontFamilyNames() {
        return getAvailableFontFamilyNames(Locale.getDefault());
    }

    /**
     * Return the bounds of a GraphicsDevice, less its screen insets.
     * See also java.awt.GraphicsEnvironment.getUsableBounds();
     */
    public static Rectangle getUsableBounds(GraphicsDevice gd) {
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usableBounds = gc.getBounds();

        usableBounds.x += insets.left;
        usableBounds.y += insets.top;
        usableBounds.width -= (insets.left + insets.right);
        usableBounds.height -= (insets.top + insets.bottom);

        return usableBounds;
    }

    /**
     * From the DisplayChangedListener interface; called
     * when the display mode has been changed.
     */
    public void displayChanged() {
        // notify screens in device array to do display update stuff
        for (GraphicsDevice gd : getScreenDevices()) {
            if (gd instanceof DisplayChangedListener) {
                ((DisplayChangedListener) gd).displayChanged();
            }
        }

        // notify SunDisplayChanger list (e.g. VolatileSurfaceManagers and
        // SurfaceDataProxies) about the display change event
        displayChanger.notifyListeners();
    }

    /**
     * Part of the DisplayChangedListener interface:
     * propagate this event to listeners
     */
    public void paletteChanged() {
        displayChanger.notifyPaletteChanged();
    }

    /**
     * Returns true when the display is local, false for remote displays.
     *
     * @return true when the display is local, false for remote displays
     */
    public abstract boolean isDisplayLocal();

    /*
     * ----DISPLAY CHANGE SUPPORT----
     */

    protected SunDisplayChanger displayChanger = new SunDisplayChanger();

    /**
     * Add a DisplayChangeListener to be notified when the display settings
     * are changed.
     */
    public void addDisplayChangedListener(DisplayChangedListener client) {
        displayChanger.add(client);
    }

    /**
     * Remove a DisplayChangeListener from Win32GraphicsEnvironment
     */
    public void removeDisplayChangedListener(DisplayChangedListener client) {
        displayChanger.remove(client);
    }

    /*
     * ----END DISPLAY CHANGE SUPPORT----
     */

    /**
     * Returns true if FlipBufferStrategy with COPIED buffer contents
     * is preferred for this peer's GraphicsConfiguration over
     * BlitBufferStrategy, false otherwise.
     *
     * The reason FlipBS could be preferred is that in some configurations
     * an accelerated copy to the screen is supported (like Direct3D 9)
     *
     * @return true if flip strategy should be used, false otherwise
     */
    public boolean isFlipStrategyPreferred(ComponentPeer peer) {
        return false;
    }

    public static boolean isUIScaleEnabled() {
        return uiScaleEnabled;
    }

    public static double getDebugScale() {
        return debugScale;
    }

    public static double getScaleFactor(String propertyName) {

        @SuppressWarnings("removal")
        String scaleFactor = AccessController.doPrivileged(
                new GetPropertyAction(propertyName, "-1"));

        if (scaleFactor == null || scaleFactor.equals("-1")) {
            return -1;
        }

        try {
            double units = 1.0;

            if (scaleFactor.endsWith("x")) {
                scaleFactor = scaleFactor.substring(0, scaleFactor.length() - 1);
            } else if (scaleFactor.endsWith("dpi")) {
                units = 96;
                scaleFactor = scaleFactor.substring(0, scaleFactor.length() - 3);
            } else if (scaleFactor.endsWith("%")) {
                units = 100;
                scaleFactor = scaleFactor.substring(0, scaleFactor.length() - 1);
            }

            double scale = Double.parseDouble(scaleFactor);
            return scale <= 0 ? -1 : scale / units;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Returns the graphics configuration which bounds contain the given point.
     *
     * @param  current the default configuration which is checked in the first
     *         place
     * @param  x the x coordinate of the given point
     * @param  y the y coordinate of the given point
     * @return the graphics configuration
     */
    public static GraphicsConfiguration getGraphicsConfigurationAtPoint(
            GraphicsConfiguration current, double x, double y) {
        if (current.getBounds().contains(x, y)) {
            return current;
        }
        GraphicsEnvironment env = getLocalGraphicsEnvironment();
        for (GraphicsDevice device : env.getScreenDevices()) {
            GraphicsConfiguration config = device.getDefaultConfiguration();
            if (config.getBounds().contains(x, y)) {
                return config;
            }
        }
        return current;
    }

    /**
     * Returns the bounds of the graphics configuration in device space.
     *
     * @param  config the graphics configuration which bounds are requested
     * @return the bounds of the area covered by this
     *         {@code GraphicsConfiguration} in device space (pixels)
     */
    public static Rectangle getGCDeviceBounds(GraphicsConfiguration config) {
        AffineTransform tx = config.getDefaultTransform();
        Rectangle bounds = config.getBounds();
        bounds.width *= tx.getScaleX();
        bounds.height *= tx.getScaleY();
        return bounds;
    }

    /**
     * Converts the size (w, h) from the device space to the user's space using
     * passed graphics configuration.
     *
     * @param  gc the graphics configuration to be used for transformation
     * @param  w the width in the device space
     * @param  h the height in the device space
     * @return the size in the user's space
     */
    public static Dimension toUserSpace(GraphicsConfiguration gc,
                                        int w, int h) {
        AffineTransform tx = gc.getDefaultTransform();
        return new Dimension(
                Region.clipRound(w / tx.getScaleX()),
                Region.clipRound(h / tx.getScaleY())
        );
    }

    /**
     * Converts absolute coordinates from the user's space to the device space
     * using appropriate device transformation.
     *
     * @param  x absolute coordinate in the user's space
     * @param  y absolute coordinate in the user's space
     * @return the point which uses device space (pixels)
     */
    public static Point toDeviceSpaceAbs(int x, int y) {
        GraphicsConfiguration gc = getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        gc = getGraphicsConfigurationAtPoint(gc, x, y);
        return toDeviceSpaceAbs(gc, x, y, 0, 0).getLocation();
    }

    /**
     * Converts the rectangle from the user's space to the device space using
     * appropriate device transformation.
     *
     * @param  rect the rectangle in the user's space
     * @return the rectangle which uses device space (pixels)
     */
    public static Rectangle toDeviceSpaceAbs(Rectangle rect) {
        GraphicsConfiguration gc = getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        gc = getGraphicsConfigurationAtPoint(gc, rect.x, rect.y);
        return toDeviceSpaceAbs(gc, rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Converts absolute coordinates (x, y) and the size (w, h) from the user's
     * space to the device space using passed graphics configuration.
     *
     * @param  gc the graphics configuration to be used for transformation
     * @param  x absolute coordinate in the user's space
     * @param  y absolute coordinate in the user's space
     * @param  w the width in the user's space
     * @param  h the height in the user's space
     * @return the rectangle which uses device space (pixels)
     */
    public static Rectangle toDeviceSpaceAbs(GraphicsConfiguration gc,
                                             int x, int y, int w, int h) {
        AffineTransform tx = gc.getDefaultTransform();
        Rectangle screen = gc.getBounds();
        return new Rectangle(
                screen.x + Region.clipRound((x - screen.x) * tx.getScaleX()),
                screen.y + Region.clipRound((y - screen.y) * tx.getScaleY()),
                Region.clipRound(w * tx.getScaleX()),
                Region.clipRound(h * tx.getScaleY())
        );
    }

    /**
     * Converts coordinates from the user's space to the device space using
     * appropriate device transformation.
     *
     * @param  x coordinate in the user's space
     * @param  y coordinate in the user's space
     * @return the point which uses device space (pixels)
     */
    public static Point toDeviceSpace(int x, int y) {
        GraphicsConfiguration gc = getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        gc = getGraphicsConfigurationAtPoint(gc, x, y);
        return toDeviceSpace(gc, x, y, 0, 0).getLocation();
    }

    /**
     * Converts coordinates (x, y) and the size (w, h) from the user's
     * space to the device space using passed graphics configuration.
     *
     * @param  gc the graphics configuration to be used for transformation
     * @param  x coordinate in the user's space
     * @param  y coordinate in the user's space
     * @param  w the width in the user's space
     * @param  h the height in the user's space
     * @return the rectangle which uses device space (pixels)
     */
    public static Rectangle toDeviceSpace(GraphicsConfiguration gc,
                                          int x, int y, int w, int h) {
        AffineTransform tx = gc.getDefaultTransform();
        return new Rectangle(
                Region.clipRound(x * tx.getScaleX()),
                Region.clipRound(y * tx.getScaleY()),
                Region.clipRound(w * tx.getScaleX()),
                Region.clipRound(h * tx.getScaleY())
        );
    }
}
