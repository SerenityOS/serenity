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


package java.awt;

import java.awt.image.BufferedImage;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

import sun.awt.PlatformGraphicsInfo;
import sun.font.FontManager;
import sun.font.FontManagerFactory;
import sun.java2d.HeadlessGraphicsEnvironment;
import sun.java2d.SunGraphicsEnvironment;
import sun.security.action.GetPropertyAction;

/**
 *
 * The {@code GraphicsEnvironment} class describes the collection
 * of {@link GraphicsDevice} objects and {@link java.awt.Font} objects
 * available to a Java(tm) application on a particular platform.
 * The resources in this {@code GraphicsEnvironment} might be local
 * or on a remote machine.  {@code GraphicsDevice} objects can be
 * screens, printers or image buffers and are the destination of
 * {@link Graphics2D} drawing methods.  Each {@code GraphicsDevice}
 * has a number of {@link GraphicsConfiguration} objects associated with
 * it.  These objects specify the different configurations in which the
 * {@code GraphicsDevice} can be used.
 * @see GraphicsDevice
 * @see GraphicsConfiguration
 */

public abstract class GraphicsEnvironment {

    /**
     * The headless state of the Toolkit and GraphicsEnvironment
     */
    private static Boolean headless;

    /**
     * The headless state assumed by default
     */
    private static Boolean defaultHeadless;

    /**
     * This is an abstract class and cannot be instantiated directly.
     * Instances must be obtained from a suitable factory or query method.
     */
    protected GraphicsEnvironment() {
    }

    /**
     * Lazy initialization of local graphics environment using holder idiom.
     */
    private static final class LocalGE {

        /**
         * The instance of the local {@code GraphicsEnvironment}.
         */
        static final GraphicsEnvironment INSTANCE = createGE();

        /**
         * Creates and returns the GraphicsEnvironment, according to the
         * platform-specific proxy class.
         *
         * @return the graphics environment
         */
        private static GraphicsEnvironment createGE() {
            GraphicsEnvironment ge = PlatformGraphicsInfo.createGE();
            if (isHeadless()) {
                ge = new HeadlessGraphicsEnvironment(ge);
            }
            return ge;
        }
    }

    /**
     * Returns the local {@code GraphicsEnvironment}.
     * @return the local {@code GraphicsEnvironment}
     */
    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        return LocalGE.INSTANCE;
    }

    /**
     * Tests whether or not a display, keyboard, and mouse can be
     * supported in this environment.  If this method returns true,
     * a HeadlessException is thrown from areas of the Toolkit
     * and GraphicsEnvironment that are dependent on a display,
     * keyboard, or mouse.
     * @return {@code true} if this environment cannot support
     * a display, keyboard, and mouse; {@code false}
     * otherwise
     * @see java.awt.HeadlessException
     * @since 1.4
     */
    public static boolean isHeadless() {
        return getHeadlessProperty();
    }

    /**
     * @return warning message if headless state is assumed by default;
     * null otherwise
     * @since 1.5
     */
    static String getHeadlessMessage() {
        if (headless == null) {
            getHeadlessProperty(); // initialize the values
        }
        return defaultHeadless != Boolean.TRUE ? null :
            PlatformGraphicsInfo.getDefaultHeadlessMessage();
    }

    /**
     * @return the value of the property "java.awt.headless"
     * @since 1.4
     */
    @SuppressWarnings("removal")
    private static boolean getHeadlessProperty() {
        if (headless == null) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                String nm = System.getProperty("java.awt.headless");

                if (nm == null) {
                    headless = defaultHeadless =
                        PlatformGraphicsInfo.getDefaultHeadlessProperty();
                } else {
                    headless = Boolean.valueOf(nm);
                }
                return null;
            });
        }
        return headless;
    }

    /**
     * Check for headless state and throw HeadlessException if headless
     * @since 1.4
     */
    static void checkHeadless() throws HeadlessException {
        if (isHeadless()) {
            throw new HeadlessException();
        }
    }

    /**
     * Returns whether or not a display, keyboard, and mouse can be
     * supported in this graphics environment.  If this returns true,
     * {@code HeadlessException} will be thrown from areas of the
     * graphics environment that are dependent on a display, keyboard, or
     * mouse.
     * @return {@code true} if a display, keyboard, and mouse
     * can be supported in this environment; {@code false}
     * otherwise
     * @see java.awt.HeadlessException
     * @see #isHeadless
     * @since 1.4
     */
    public boolean isHeadlessInstance() {
        // By default (local graphics environment), simply check the
        // headless property.
        return getHeadlessProperty();
    }

    /**
     * Returns an array of all of the screen {@code GraphicsDevice}
     * objects.
     * @return an array containing all the {@code GraphicsDevice}
     * objects that represent screen devices
     * @exception HeadlessException if isHeadless() returns true
     * @see #isHeadless()
     */
    public abstract GraphicsDevice[] getScreenDevices()
        throws HeadlessException;

    /**
     * Returns the default screen {@code GraphicsDevice}.
     * @return the {@code GraphicsDevice} that represents the
     * default screen device
     * @exception HeadlessException if isHeadless() returns true
     * @see #isHeadless()
     */
    public abstract GraphicsDevice getDefaultScreenDevice()
        throws HeadlessException;

    /**
     * Returns a {@code Graphics2D} object for rendering into the
     * specified {@link BufferedImage}.
     * @param img the specified {@code BufferedImage}
     * @return a {@code Graphics2D} to be used for rendering into
     * the specified {@code BufferedImage}
     * @throws NullPointerException if {@code img} is null
     */
    public abstract Graphics2D createGraphics(BufferedImage img);

    /**
     * Returns an array containing a one-point size instance of all fonts
     * available in this {@code GraphicsEnvironment}.  Typical usage
     * would be to allow a user to select a particular font.  Then, the
     * application can size the font and set various font attributes by
     * calling the {@code deriveFont} method on the chosen instance.
     * <p>
     * This method provides for the application the most precise control
     * over which {@code Font} instance is used to render text.
     * If a font in this {@code GraphicsEnvironment} has multiple
     * programmable variations, only one
     * instance of that {@code Font} is returned in the array, and
     * other variations must be derived by the application.
     * <p>
     * If a font in this environment has multiple programmable variations,
     * such as Multiple-Master fonts, only one instance of that font is
     * returned in the {@code Font} array.  The other variations
     * must be derived by the application.
     *
     * @return an array of {@code Font} objects
     * @see #getAvailableFontFamilyNames
     * @see java.awt.Font
     * @see java.awt.Font#deriveFont
     * @see java.awt.Font#getFontName
     * @since 1.2
     */
    public abstract Font[] getAllFonts();

    /**
     * Returns an array containing the names of all font families in this
     * {@code GraphicsEnvironment} localized for the default locale,
     * as returned by {@code Locale.getDefault()}.
     * <p>
     * Typical usage would be for presentation to a user for selection of
     * a particular family name. An application can then specify this name
     * when creating a font, in conjunction with a style, such as bold or
     * italic, giving the font system flexibility in choosing its own best
     * match among multiple fonts in the same font family.
     *
     * @return an array of {@code String} containing font family names
     * localized for the default locale, or a suitable alternative
     * name if no name exists for this locale.
     * @see #getAllFonts
     * @see java.awt.Font
     * @see java.awt.Font#getFamily
     * @since 1.2
     */
    public abstract String[] getAvailableFontFamilyNames();

    /**
     * Returns an array containing the names of all font families in this
     * {@code GraphicsEnvironment} localized for the specified locale.
     * <p>
     * Typical usage would be for presentation to a user for selection of
     * a particular family name. An application can then specify this name
     * when creating a font, in conjunction with a style, such as bold or
     * italic, giving the font system flexibility in choosing its own best
     * match among multiple fonts in the same font family.
     *
     * @param l a {@link Locale} object that represents a
     * particular geographical, political, or cultural region.
     * Specifying {@code null} is equivalent to
     * specifying {@code Locale.getDefault()}.
     * @return an array of {@code String} containing font family names
     * localized for the specified {@code Locale}, or a
     * suitable alternative name if no name exists for the specified locale.
     * @see #getAllFonts
     * @see java.awt.Font
     * @see java.awt.Font#getFamily
     * @since 1.2
     */
    public abstract String[] getAvailableFontFamilyNames(Locale l);

    /**
     * Registers a <i>created</i> {@code Font} in this
     * {@code GraphicsEnvironment}.
     * A created font is one that was returned from calling
     * {@link Font#createFont}, or derived from a created font by
     * calling {@link Font#deriveFont}.
     * After calling this method for such a font, it is available to
     * be used in constructing new {@code Font}s by name or family name,
     * and is enumerated by {@link #getAvailableFontFamilyNames} and
     * {@link #getAllFonts} within the execution context of this
     * application or applet. This means applets cannot register fonts in
     * a way that they are visible to other applets.
     * <p>
     * Reasons that this method might not register the font and therefore
     * return {@code false} are:
     * <ul>
     * <li>The font is not a <i>created</i> {@code Font}.
     * <li>The font conflicts with a non-created {@code Font} already
     * in this {@code GraphicsEnvironment}. For example if the name
     * is that of a system font, or a logical font as described in the
     * documentation of the {@link Font} class. It is implementation dependent
     * whether a font may also conflict if it has the same family name
     * as a system font.
     * <p>Notice that an application can supersede the registration
     * of an earlier created font with a new one.
     * </ul>
     *
     * @param  font the font to be registered
     * @return true if the {@code font} is successfully
     * registered in this {@code GraphicsEnvironment}.
     * @throws NullPointerException if {@code font} is null
     * @since 1.6
     */
    public boolean registerFont(Font font) {
        if (font == null) {
            throw new NullPointerException("font cannot be null.");
        }
        FontManager fm = FontManagerFactory.getInstance();
        return fm.registerFont(font);
    }

    /**
     * Indicates a preference for locale-specific fonts in the mapping of
     * logical fonts to physical fonts. Calling this method indicates that font
     * rendering should primarily use fonts specific to the primary writing
     * system (the one indicated by the default encoding and the initial
     * default locale). For example, if the primary writing system is
     * Japanese, then characters should be rendered using a Japanese font
     * if possible, and other fonts should only be used for characters for
     * which the Japanese font doesn't have glyphs.
     * <p>
     * The actual change in font rendering behavior resulting from a call
     * to this method is implementation dependent; it may have no effect at
     * all, or the requested behavior may already match the default behavior.
     * The behavior may differ between font rendering in lightweight
     * and peered components.  Since calling this method requests a
     * different font, clients should expect different metrics, and may need
     * to recalculate window sizes and layout. Therefore this method should
     * be called before user interface initialisation.
     * @since 1.5
     */
    public void preferLocaleFonts() {
        FontManager fm = FontManagerFactory.getInstance();
        fm.preferLocaleFonts();
    }

    /**
     * Indicates a preference for proportional over non-proportional (e.g.
     * dual-spaced CJK fonts) fonts in the mapping of logical fonts to
     * physical fonts. If the default mapping contains fonts for which
     * proportional and non-proportional variants exist, then calling
     * this method indicates the mapping should use a proportional variant.
     * <p>
     * The actual change in font rendering behavior resulting from a call to
     * this method is implementation dependent; it may have no effect at all.
     * The behavior may differ between font rendering in lightweight and
     * peered components. Since calling this method requests a
     * different font, clients should expect different metrics, and may need
     * to recalculate window sizes and layout. Therefore this method should
     * be called before user interface initialisation.
     * @since 1.5
     */
    public void preferProportionalFonts() {
        FontManager fm = FontManagerFactory.getInstance();
        fm.preferProportionalFonts();
    }

    /**
     * Returns the Point where Windows should be centered.
     * It is recommended that centered Windows be checked to ensure they fit
     * within the available display area using getMaximumWindowBounds().
     * @return the point where Windows should be centered
     *
     * @exception HeadlessException if isHeadless() returns true
     * @see #getMaximumWindowBounds
     * @since 1.4
     */
    public Point getCenterPoint() throws HeadlessException {
    // Default implementation: return the center of the usable bounds of the
    // default screen device.
        Rectangle usableBounds =
         SunGraphicsEnvironment.getUsableBounds(getDefaultScreenDevice());
        return new Point((usableBounds.width / 2) + usableBounds.x,
                         (usableBounds.height / 2) + usableBounds.y);
    }

    /**
     * Returns the maximum bounds for centered Windows.
     * These bounds account for objects in the native windowing system such as
     * task bars and menu bars.  The returned bounds will reside on a single
     * display with one exception: on multi-screen systems where Windows should
     * be centered across all displays, this method returns the bounds of the
     * entire display area.
     * <p>
     * To get the usable bounds of a single display, use
     * {@code GraphicsConfiguration.getBounds()} and
     * {@code Toolkit.getScreenInsets()}.
     * @return  the maximum bounds for centered Windows
     *
     * @exception HeadlessException if isHeadless() returns true
     * @see #getCenterPoint
     * @see GraphicsConfiguration#getBounds
     * @see Toolkit#getScreenInsets
     * @since 1.4
     */
    public Rectangle getMaximumWindowBounds() throws HeadlessException {
    // Default implementation: return the usable bounds of the default screen
    // device.  This is correct for Microsoft Windows and non-Xinerama X11.
        return SunGraphicsEnvironment.getUsableBounds(getDefaultScreenDevice());
    }
}
