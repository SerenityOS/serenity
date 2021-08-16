/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.beans.ConstructorProperties;
import java.io.Serial;

/**
 * The {@code Color} class is used to encapsulate colors in the default
 * sRGB color space or colors in arbitrary color spaces identified by a
 * {@link ColorSpace}.  Every color has an implicit alpha value of 1.0 or
 * an explicit one provided in the constructor.  The alpha value
 * defines the transparency of a color and can be represented by
 * a float value in the range 0.0&nbsp;-&nbsp;1.0 or 0&nbsp;-&nbsp;255.
 * An alpha value of 1.0 or 255 means that the color is completely
 * opaque and an alpha value of 0 or 0.0 means that the color is
 * completely transparent.
 * When constructing a {@code Color} with an explicit alpha or
 * getting the color/alpha components of a {@code Color}, the color
 * components are never premultiplied by the alpha component.
 * <p>
 * The default color space for the Java 2D(tm) API is sRGB, a proposed
 * standard RGB color space.  For further information on sRGB,
 * see <A href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">
 * http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html
 * </A>.
 *
 * @version     10 Feb 1997
 * @author      Sami Shaio
 * @author      Arthur van Hoff
 * @see         ColorSpace
 * @see         AlphaComposite
 */
public class Color implements Paint, java.io.Serializable {

    /**
     * The color white.  In the default sRGB space.
     */
    public static final Color white     = new Color(255, 255, 255);

    /**
     * The color white.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color WHITE = white;

    /**
     * The color light gray.  In the default sRGB space.
     */
    public static final Color lightGray = new Color(192, 192, 192);

    /**
     * The color light gray.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color LIGHT_GRAY = lightGray;

    /**
     * The color gray.  In the default sRGB space.
     */
    public static final Color gray      = new Color(128, 128, 128);

    /**
     * The color gray.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color GRAY = gray;

    /**
     * The color dark gray.  In the default sRGB space.
     */
    public static final Color darkGray  = new Color(64, 64, 64);

    /**
     * The color dark gray.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color DARK_GRAY = darkGray;

    /**
     * The color black.  In the default sRGB space.
     */
    public static final Color black     = new Color(0, 0, 0);

    /**
     * The color black.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color BLACK = black;

    /**
     * The color red.  In the default sRGB space.
     */
    public static final Color red       = new Color(255, 0, 0);

    /**
     * The color red.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color RED = red;

    /**
     * The color pink.  In the default sRGB space.
     */
    public static final Color pink      = new Color(255, 175, 175);

    /**
     * The color pink.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color PINK = pink;

    /**
     * The color orange.  In the default sRGB space.
     */
    public static final Color orange    = new Color(255, 200, 0);

    /**
     * The color orange.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color ORANGE = orange;

    /**
     * The color yellow.  In the default sRGB space.
     */
    public static final Color yellow    = new Color(255, 255, 0);

    /**
     * The color yellow.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color YELLOW = yellow;

    /**
     * The color green.  In the default sRGB space.
     */
    public static final Color green     = new Color(0, 255, 0);

    /**
     * The color green.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color GREEN = green;

    /**
     * The color magenta.  In the default sRGB space.
     */
    public static final Color magenta   = new Color(255, 0, 255);

    /**
     * The color magenta.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color MAGENTA = magenta;

    /**
     * The color cyan.  In the default sRGB space.
     */
    public static final Color cyan      = new Color(0, 255, 255);

    /**
     * The color cyan.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color CYAN = cyan;

    /**
     * The color blue.  In the default sRGB space.
     */
    public static final Color blue      = new Color(0, 0, 255);

    /**
     * The color blue.  In the default sRGB space.
     * @since 1.4
     */
    public static final Color BLUE = blue;

    /**
     * The color value.
     * @serial
     * @see #getRGB
     */
    int value;

    /**
     * The color value in the default sRGB {@code ColorSpace} as
     * {@code float} components (no alpha).
     * If {@code null} after object construction, this must be an
     * sRGB color constructed with 8-bit precision, so compute from the
     * {@code int} color value.
     * @serial
     * @see #getRGBColorComponents
     * @see #getRGBComponents
     */
    private float[] frgbvalue = null;

    /**
     * The color value in the native {@code ColorSpace} as
     * {@code float} components (no alpha).
     * If {@code null} after object construction, this must be an
     * sRGB color constructed with 8-bit precision, so compute from the
     * {@code int} color value.
     * @serial
     * @see #getRGBColorComponents
     * @see #getRGBComponents
     */
    private float[] fvalue = null;

    /**
     * The alpha value as a {@code float} component.
     * If {@code frgbvalue} is {@code null}, this is not valid
     * data, so compute from the {@code int} color value.
     * @serial
     * @see #getRGBComponents
     * @see #getComponents
     */
    private float falpha = 0.0f;

    /**
     * The {@code ColorSpace}.  If {@code null}, then it's
     * default is sRGB.
     * @serial
     * @see #getColor
     * @see #getColorSpace
     * @see #getColorComponents
     */
    private ColorSpace cs = null;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = 118526816881161077L;

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    static {
        /** 4112352 - Calling getDefaultToolkit()
         ** here can cause this class to be accessed before it is fully
         ** initialized. DON'T DO IT!!!
         **
         ** Toolkit.getDefaultToolkit();
         **/

        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }

    /**
     * Checks the color integer components supplied for validity.
     * Throws an {@link IllegalArgumentException} if the value is out of
     * range.
     * @param r the Red component
     * @param g the Green component
     * @param b the Blue component
     **/
    private static void testColorValueRange(int r, int g, int b, int a) {
        boolean rangeError = false;
        String badComponentString = "";

        if ( a < 0 || a > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if ( r < 0 || r > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if ( g < 0 || g > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if ( b < 0 || b > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if ( rangeError == true ) {
        throw new IllegalArgumentException("Color parameter outside of expected range:"
                                           + badComponentString);
        }
    }

    /**
     * Checks the color {@code float} components supplied for
     * validity.
     * Throws an {@code IllegalArgumentException} if the value is out
     * of range.
     * @param r the Red component
     * @param g the Green component
     * @param b the Blue component
     **/
    private static void testColorValueRange(float r, float g, float b, float a) {
        boolean rangeError = false;
        String badComponentString = "";
        if ( a < 0.0 || a > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if ( r < 0.0 || r > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if ( g < 0.0 || g > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if ( b < 0.0 || b > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if ( rangeError == true ) {
        throw new IllegalArgumentException("Color parameter outside of expected range:"
                                           + badComponentString);
        }
    }

    /**
     * Creates an opaque sRGB color with the specified red, green,
     * and blue values in the range (0 - 255).
     * The actual color used in rendering depends
     * on finding the best match given the color space
     * available for a given output device.
     * Alpha is defaulted to 255.
     *
     * @throws IllegalArgumentException if {@code r}, {@code g}
     *        or {@code b} are outside of the range
     *        0 to 255, inclusive
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getRGB
     */
    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    /**
     * Creates an sRGB color with the specified red, green, blue, and alpha
     * values in the range (0 - 255).
     *
     * @throws IllegalArgumentException if {@code r}, {@code g},
     *        {@code b} or {@code a} are outside of the range
     *        0 to 255, inclusive
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getAlpha
     * @see #getRGB
     */
    @ConstructorProperties({"red", "green", "blue", "alpha"})
    public Color(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
        testColorValueRange(r,g,b,a);
    }

    /**
     * Creates an opaque sRGB color with the specified combined RGB value
     * consisting of the red component in bits 16-23, the green component
     * in bits 8-15, and the blue component in bits 0-7.  The actual color
     * used in rendering depends on finding the best match given the
     * color space available for a particular output device.  Alpha is
     * defaulted to 255.
     *
     * @param rgb the combined RGB components
     * @see java.awt.image.ColorModel#getRGBdefault
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getRGB
     */
    public Color(int rgb) {
        value = 0xff000000 | rgb;
    }

    /**
     * Creates an sRGB color with the specified combined RGBA value consisting
     * of the alpha component in bits 24-31, the red component in bits 16-23,
     * the green component in bits 8-15, and the blue component in bits 0-7.
     * If the {@code hasalpha} argument is {@code false}, alpha
     * is defaulted to 255.
     *
     * @param rgba the combined RGBA components
     * @param hasalpha {@code true} if the alpha bits are valid;
     *        {@code false} otherwise
     * @see java.awt.image.ColorModel#getRGBdefault
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getAlpha
     * @see #getRGB
     */
    public Color(int rgba, boolean hasalpha) {
        if (hasalpha) {
            value = rgba;
        } else {
            value = 0xff000000 | rgba;
        }
    }

    /**
     * Creates an opaque sRGB color with the specified red, green, and blue
     * values in the range (0.0 - 1.0).  Alpha is defaulted to 1.0.  The
     * actual color used in rendering depends on finding the best
     * match given the color space available for a particular output
     * device.
     *
     * @throws IllegalArgumentException if {@code r}, {@code g}
     *        or {@code b} are outside of the range
     *        0.0 to 1.0, inclusive
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getRGB
     */
    public Color(float r, float g, float b) {
        this( (int) (r*255+0.5), (int) (g*255+0.5), (int) (b*255+0.5));
        testColorValueRange(r,g,b,1.0f);
        frgbvalue = new float[3];
        frgbvalue[0] = r;
        frgbvalue[1] = g;
        frgbvalue[2] = b;
        falpha = 1.0f;
        fvalue = frgbvalue;
    }

    /**
     * Creates an sRGB color with the specified red, green, blue, and
     * alpha values in the range (0.0 - 1.0).  The actual color
     * used in rendering depends on finding the best match given the
     * color space available for a particular output device.
     * @throws IllegalArgumentException if {@code r}, {@code g}
     *        {@code b} or {@code a} are outside of the range
     *        0.0 to 1.0, inclusive
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getAlpha
     * @see #getRGB
     */
    public Color(float r, float g, float b, float a) {
        this((int)(r*255+0.5), (int)(g*255+0.5), (int)(b*255+0.5), (int)(a*255+0.5));
        frgbvalue = new float[3];
        frgbvalue[0] = r;
        frgbvalue[1] = g;
        frgbvalue[2] = b;
        falpha = a;
        fvalue = frgbvalue;
    }

    /**
     * Creates a color in the specified {@code ColorSpace}
     * with the color components specified in the {@code float}
     * array and the specified alpha.  The number of components is
     * determined by the type of the {@code ColorSpace}.  For
     * example, RGB requires 3 components, but CMYK requires 4
     * components.
     * @param cspace the {@code ColorSpace} to be used to
     *                  interpret the components
     * @param components an arbitrary number of color components
     *                      that is compatible with the {@code ColorSpace}
     * @param alpha alpha value
     * @throws IllegalArgumentException if any of the values in the
     *         {@code components} array or {@code alpha} is
     *         outside of the range 0.0 to 1.0
     * @see #getComponents
     * @see #getColorComponents
     */
    public Color(ColorSpace cspace, float[] components, float alpha) {
        boolean rangeError = false;
        String badComponentString = "";
        int n = cspace.getNumComponents();
        fvalue = new float[n];
        for (int i = 0; i < n; i++) {
            if (components[i] < 0.0 || components[i] > 1.0) {
                rangeError = true;
                badComponentString = badComponentString + "Component " + i
                                     + " ";
            } else {
                fvalue[i] = components[i];
            }
        }
        if (alpha < 0.0 || alpha > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + "Alpha";
        } else {
            falpha = alpha;
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                "Color parameter outside of expected range: " +
                badComponentString);
        }
        frgbvalue = cspace.toRGB(fvalue);
        cs = cspace;
        value = ((((int)(falpha*255)) & 0xFF) << 24) |
                ((((int)(frgbvalue[0]*255)) & 0xFF) << 16) |
                ((((int)(frgbvalue[1]*255)) & 0xFF) << 8)  |
                ((((int)(frgbvalue[2]*255)) & 0xFF) << 0);
    }

    /**
     * Returns the red component in the range 0-255 in the default sRGB
     * space.
     * @return the red component.
     * @see #getRGB
     */
    public int getRed() {
        return (getRGB() >> 16) & 0xFF;
    }

    /**
     * Returns the green component in the range 0-255 in the default sRGB
     * space.
     * @return the green component.
     * @see #getRGB
     */
    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    /**
     * Returns the blue component in the range 0-255 in the default sRGB
     * space.
     * @return the blue component.
     * @see #getRGB
     */
    public int getBlue() {
        return (getRGB() >> 0) & 0xFF;
    }

    /**
     * Returns the alpha component in the range 0-255.
     * @return the alpha component.
     * @see #getRGB
     */
    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }

    /**
     * Returns the RGB value representing the color in the default sRGB
     * {@link ColorModel}.
     * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are
     * blue).
     * @return the RGB value of the color in the default sRGB
     *         {@code ColorModel}.
     * @see java.awt.image.ColorModel#getRGBdefault
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @since 1.0
     */
    public int getRGB() {
        return value;
    }

    private static final double FACTOR = 0.7;

    /**
     * Creates a new {@code Color} that is a brighter version of this
     * {@code Color}.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this {@code Color} to create a brighter version
     * of this {@code Color}.
     * The {@code alpha} value is preserved.
     * Although {@code brighter} and
     * {@code darker} are inverse operations, the results of a
     * series of invocations of these two methods might be inconsistent
     * because of rounding errors.
     * @return     a new {@code Color} object that is
     *                 a brighter version of this {@code Color}
     *                 with the same {@code alpha} value.
     * @see        java.awt.Color#darker
     * @since      1.0
     */
    public Color brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int alpha = getAlpha();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int)(1.0/(1.0-FACTOR));
        if ( r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return new Color(Math.min((int)(r/FACTOR), 255),
                         Math.min((int)(g/FACTOR), 255),
                         Math.min((int)(b/FACTOR), 255),
                         alpha);
    }

    /**
     * Creates a new {@code Color} that is a darker version of this
     * {@code Color}.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this {@code Color} to create a darker version of
     * this {@code Color}.
     * The {@code alpha} value is preserved.
     * Although {@code brighter} and
     * {@code darker} are inverse operations, the results of a series
     * of invocations of these two methods might be inconsistent because
     * of rounding errors.
     * @return  a new {@code Color} object that is
     *                    a darker version of this {@code Color}
     *                    with the same {@code alpha} value.
     * @see        java.awt.Color#brighter
     * @since      1.0
     */
    public Color darker() {
        return new Color(Math.max((int)(getRed()  *FACTOR), 0),
                         Math.max((int)(getGreen()*FACTOR), 0),
                         Math.max((int)(getBlue() *FACTOR), 0),
                         getAlpha());
    }

    /**
     * Computes the hash code for this {@code Color}.
     * @return     a hash code value for this object.
     * @since      1.0
     */
    public int hashCode() {
        return value;
    }

    /**
     * Determines whether another object is equal to this
     * {@code Color}.
     * <p>
     * The result is {@code true} if and only if the argument is not
     * {@code null} and is a {@code Color} object that has the same
     * red, green, blue, and alpha values as this object.
     * @param       obj   the object to test for equality with this
     *                          {@code Color}
     * @return      {@code true} if the objects are the same;
     *                             {@code false} otherwise.
     * @since   1.0
     */
    public boolean equals(Object obj) {
        return obj instanceof Color && ((Color)obj).getRGB() == this.getRGB();
    }

    /**
     * Returns a string representation of this {@code Color}. This
     * method is intended to be used only for debugging purposes.  The
     * content and format of the returned string might vary between
     * implementations. The returned string might be empty but cannot
     * be {@code null}.
     *
     * @return  a string representation of this {@code Color}.
     */
    public String toString() {
        return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
    }

    /**
     * Converts a {@code String} to an integer and returns the
     * specified opaque {@code Color}. This method handles string
     * formats that are used to represent octal and hexadecimal numbers.
     * @param      nm a {@code String} that represents
     *                            an opaque color as a 24-bit integer
     * @return     the new {@code Color} object.
     * @see        java.lang.Integer#decode
     * @exception  NumberFormatException  if the specified string cannot
     *                      be interpreted as a decimal,
     *                      octal, or hexadecimal integer.
     * @since      1.1
     */
    public static Color decode(String nm) throws NumberFormatException {
        Integer intval = Integer.decode(nm);
        int i = intval.intValue();
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    /**
     * Finds a color in the system properties.
     * <p>
     * The argument is treated as the name of a system property to
     * be obtained. The string value of this property is then interpreted
     * as an integer which is then converted to a {@code Color}
     * object.
     * <p>
     * If the specified property is not found or could not be parsed as
     * an integer then {@code null} is returned.
     * @param    nm the name of the color property
     * @return   the {@code Color} converted from the system
     *          property.
     * @see      java.lang.System#getProperty(java.lang.String)
     * @see      java.lang.Integer#getInteger(java.lang.String)
     * @see      java.awt.Color#Color(int)
     * @since    1.0
     */
    public static Color getColor(String nm) {
        return getColor(nm, null);
    }

    /**
     * Finds a color in the system properties.
     * <p>
     * The first argument is treated as the name of a system property to
     * be obtained. The string value of this property is then interpreted
     * as an integer which is then converted to a {@code Color}
     * object.
     * <p>
     * If the specified property is not found or cannot be parsed as
     * an integer then the {@code Color} specified by the second
     * argument is returned instead.
     * @param    nm the name of the color property
     * @param    v    the default {@code Color}
     * @return   the {@code Color} converted from the system
     *          property, or the specified {@code Color}.
     * @see      java.lang.System#getProperty(java.lang.String)
     * @see      java.lang.Integer#getInteger(java.lang.String)
     * @see      java.awt.Color#Color(int)
     * @since    1.0
     */
    public static Color getColor(String nm, Color v) {
        Integer intval = Integer.getInteger(nm);
        if (intval == null) {
            return v;
        }
        int i = intval.intValue();
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    /**
     * Finds a color in the system properties.
     * <p>
     * The first argument is treated as the name of a system property to
     * be obtained. The string value of this property is then interpreted
     * as an integer which is then converted to a {@code Color}
     * object.
     * <p>
     * If the specified property is not found or could not be parsed as
     * an integer then the integer value {@code v} is used instead,
     * and is converted to a {@code Color} object.
     * @param    nm  the name of the color property
     * @param    v   the default color value, as an integer
     * @return   the {@code Color} converted from the system
     *          property or the {@code Color} converted from
     *          the specified integer.
     * @see      java.lang.System#getProperty(java.lang.String)
     * @see      java.lang.Integer#getInteger(java.lang.String)
     * @see      java.awt.Color#Color(int)
     * @since    1.0
     */
    public static Color getColor(String nm, int v) {
        Integer intval = Integer.getInteger(nm);
        int i = (intval != null) ? intval.intValue() : v;
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, (i >> 0) & 0xFF);
    }

    /**
     * Converts the components of a color, as specified by the HSB
     * model, to an equivalent set of values for the default RGB model.
     * <p>
     * The {@code saturation} and {@code brightness} components
     * should be floating-point values between zero and one
     * (numbers in the range 0.0-1.0).  The {@code hue} component
     * can be any floating-point number.  The floor of this number is
     * subtracted from it to create a fraction between 0 and 1.  This
     * fractional number is then multiplied by 360 to produce the hue
     * angle in the HSB color model.
     * <p>
     * The integer that is returned by {@code HSBtoRGB} encodes the
     * value of a color in bits 0-23 of an integer value that is the same
     * format used by the method {@link #getRGB() getRGB}.
     * This integer can be supplied as an argument to the
     * {@code Color} constructor that takes a single integer argument.
     * @param     hue   the hue component of the color
     * @param     saturation   the saturation of the color
     * @param     brightness   the brightness of the color
     * @return    the RGB value of the color with the indicated hue,
     *                            saturation, and brightness.
     * @see       java.awt.Color#getRGB()
     * @see       java.awt.Color#Color(int)
     * @see       java.awt.image.ColorModel#getRGBdefault()
     * @since     1.0
     */
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
            case 0:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (q * 255.0f + 0.5f);
                break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

    /**
     * Converts the components of a color, as specified by the default RGB
     * model, to an equivalent set of values for hue, saturation, and
     * brightness that are the three components of the HSB model.
     * <p>
     * If the {@code hsbvals} argument is {@code null}, then a
     * new array is allocated to return the result. Otherwise, the method
     * returns the array {@code hsbvals}, with the values put into
     * that array.
     * @param     r   the red component of the color
     * @param     g   the green component of the color
     * @param     b   the blue component of the color
     * @param     hsbvals  the array used to return the
     *                     three HSB values, or {@code null}
     * @return    an array of three elements containing the hue, saturation,
     *                     and brightness (in that order), of the color with
     *                     the indicated red, green, and blue components.
     * @see       java.awt.Color#getRGB()
     * @see       java.awt.Color#Color(int)
     * @see       java.awt.image.ColorModel#getRGBdefault()
     * @since     1.0
     */
    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    /**
     * Creates a {@code Color} object based on the specified values
     * for the HSB color model.
     * <p>
     * The {@code s} and {@code b} components should be
     * floating-point values between zero and one
     * (numbers in the range 0.0-1.0).  The {@code h} component
     * can be any floating-point number.  The floor of this number is
     * subtracted from it to create a fraction between 0 and 1.  This
     * fractional number is then multiplied by 360 to produce the hue
     * angle in the HSB color model.
     * @param  h   the hue component
     * @param  s   the saturation of the color
     * @param  b   the brightness of the color
     * @return  a {@code Color} object with the specified hue,
     *                                 saturation, and brightness.
     * @since   1.0
     */
    public static Color getHSBColor(float h, float s, float b) {
        return new Color(HSBtoRGB(h, s, b));
    }

    /**
     * Returns a {@code float} array containing the color and alpha
     * components of the {@code Color}, as represented in the default
     * sRGB color space.
     * If {@code compArray} is {@code null}, an array of length
     * 4 is created for the return value.  Otherwise,
     * {@code compArray} must have length 4 or greater,
     * and it is filled in with the components and returned.
     * @param compArray an array that this method fills with
     *                  color and alpha components and returns
     * @return the RGBA components in a {@code float} array.
     */
    public float[] getRGBComponents(float[] compArray) {
        float[] f;
        if (compArray == null) {
            f = new float[4];
        } else {
            f = compArray;
        }
        if (frgbvalue == null) {
            f[0] = ((float)getRed())/255f;
            f[1] = ((float)getGreen())/255f;
            f[2] = ((float)getBlue())/255f;
            f[3] = ((float)getAlpha())/255f;
        } else {
            f[0] = frgbvalue[0];
            f[1] = frgbvalue[1];
            f[2] = frgbvalue[2];
            f[3] = falpha;
        }
        return f;
    }

    /**
     * Returns a {@code float} array containing only the color
     * components of the {@code Color}, in the default sRGB color
     * space.  If {@code compArray} is {@code null}, an array of
     * length 3 is created for the return value.  Otherwise,
     * {@code compArray} must have length 3 or greater, and it is
     * filled in with the components and returned.
     * @param compArray an array that this method fills with color
     *          components and returns
     * @return the RGB components in a {@code float} array.
     */
    public float[] getRGBColorComponents(float[] compArray) {
        float[] f;
        if (compArray == null) {
            f = new float[3];
        } else {
            f = compArray;
        }
        if (frgbvalue == null) {
            f[0] = ((float)getRed())/255f;
            f[1] = ((float)getGreen())/255f;
            f[2] = ((float)getBlue())/255f;
        } else {
            f[0] = frgbvalue[0];
            f[1] = frgbvalue[1];
            f[2] = frgbvalue[2];
        }
        return f;
    }

    /**
     * Returns a {@code float} array containing the color and alpha
     * components of the {@code Color}, in the
     * {@code ColorSpace} of the {@code Color}.
     * If {@code compArray} is {@code null}, an array with
     * length equal to the number of components in the associated
     * {@code ColorSpace} plus one is created for
     * the return value.  Otherwise, {@code compArray} must have at
     * least this length and it is filled in with the components and
     * returned.
     * @param compArray an array that this method fills with the color and
     *          alpha components of this {@code Color} in its
     *          {@code ColorSpace} and returns
     * @return the color and alpha components in a {@code float}
     *          array.
     */
    public float[] getComponents(float[] compArray) {
        if (fvalue == null)
            return getRGBComponents(compArray);
        float[] f;
        int n = fvalue.length;
        if (compArray == null) {
            f = new float[n + 1];
        } else {
            f = compArray;
        }
        for (int i = 0; i < n; i++) {
            f[i] = fvalue[i];
        }
        f[n] = falpha;
        return f;
    }

    /**
     * Returns a {@code float} array containing only the color
     * components of the {@code Color}, in the
     * {@code ColorSpace} of the {@code Color}.
     * If {@code compArray} is {@code null}, an array with
     * length equal to the number of components in the associated
     * {@code ColorSpace} is created for
     * the return value.  Otherwise, {@code compArray} must have at
     * least this length and it is filled in with the components and
     * returned.
     * @param compArray an array that this method fills with the color
     *          components of this {@code Color} in its
     *          {@code ColorSpace} and returns
     * @return the color components in a {@code float} array.
     */
    public float[] getColorComponents(float[] compArray) {
        if (fvalue == null)
            return getRGBColorComponents(compArray);
        float[] f;
        int n = fvalue.length;
        if (compArray == null) {
            f = new float[n];
        } else {
            f = compArray;
        }
        for (int i = 0; i < n; i++) {
            f[i] = fvalue[i];
        }
        return f;
    }

    /**
     * Returns a {@code float} array containing the color and alpha
     * components of the {@code Color}, in the
     * {@code ColorSpace} specified by the {@code cspace}
     * parameter.  If {@code compArray} is {@code null}, an
     * array with length equal to the number of components in
     * {@code cspace} plus one is created for the return value.
     * Otherwise, {@code compArray} must have at least this
     * length, and it is filled in with the components and returned.
     * @param cspace a specified {@code ColorSpace}
     * @param compArray an array that this method fills with the
     *          color and alpha components of this {@code Color} in
     *          the specified {@code ColorSpace} and returns
     * @return the color and alpha components in a {@code float}
     *          array.
     */
    public float[] getComponents(ColorSpace cspace, float[] compArray) {
        if (cs == null) {
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        float[] f;
        if (fvalue == null) {
            f = new float[3];
            f[0] = ((float)getRed())/255f;
            f[1] = ((float)getGreen())/255f;
            f[2] = ((float)getBlue())/255f;
        } else {
            f = fvalue;
        }
        float[] tmp = cs.toCIEXYZ(f);
        float[] tmpout = cspace.fromCIEXYZ(tmp);
        if (compArray == null) {
            compArray = new float[tmpout.length + 1];
        }
        for (int i = 0 ; i < tmpout.length ; i++) {
            compArray[i] = tmpout[i];
        }
        if (fvalue == null) {
            compArray[tmpout.length] = ((float)getAlpha())/255f;
        } else {
            compArray[tmpout.length] = falpha;
        }
        return compArray;
    }

    /**
     * Returns a {@code float} array containing only the color
     * components of the {@code Color} in the
     * {@code ColorSpace} specified by the {@code cspace}
     * parameter. If {@code compArray} is {@code null}, an array
     * with length equal to the number of components in
     * {@code cspace} is created for the return value.  Otherwise,
     * {@code compArray} must have at least this length, and it is
     * filled in with the components and returned.
     * @param cspace a specified {@code ColorSpace}
     * @param compArray an array that this method fills with the color
     *          components of this {@code Color} in the specified
     *          {@code ColorSpace}
     * @return the color components in a {@code float} array.
     */
    public float[] getColorComponents(ColorSpace cspace, float[] compArray) {
        if (cs == null) {
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        float[] f;
        if (fvalue == null) {
            f = new float[3];
            f[0] = ((float)getRed())/255f;
            f[1] = ((float)getGreen())/255f;
            f[2] = ((float)getBlue())/255f;
        } else {
            f = fvalue;
        }
        float[] tmp = cs.toCIEXYZ(f);
        float[] tmpout = cspace.fromCIEXYZ(tmp);
        if (compArray == null) {
            return tmpout;
        }
        for (int i = 0 ; i < tmpout.length ; i++) {
            compArray[i] = tmpout[i];
        }
        return compArray;
    }

    /**
     * Returns the {@code ColorSpace} of this {@code Color}.
     * @return this {@code Color} object's {@code ColorSpace}.
     */
    public ColorSpace getColorSpace() {
        if (cs == null) {
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        }
        return cs;
    }

    /**
     * Creates and returns a {@link PaintContext} used to
     * generate a solid color field pattern.
     * See the {@link Paint#createContext specification} of the
     * method in the {@link Paint} interface for information
     * on null parameter handling.
     *
     * @param cm the preferred {@link ColorModel} which represents the most convenient
     *           format for the caller to receive the pixel data, or {@code null}
     *           if there is no preference.
     * @param r the device space bounding box
     *                     of the graphics primitive being rendered.
     * @param r2d the user space bounding box
     *                   of the graphics primitive being rendered.
     * @param xform the {@link AffineTransform} from user
     *              space into device space.
     * @param hints the set of hints that the context object can use to
     *              choose between rendering alternatives.
     * @return the {@code PaintContext} for
     *         generating color patterns.
     * @see Paint
     * @see PaintContext
     * @see ColorModel
     * @see Rectangle
     * @see Rectangle2D
     * @see AffineTransform
     * @see RenderingHints
     */
    public synchronized PaintContext createContext(ColorModel cm, Rectangle r,
                                                   Rectangle2D r2d,
                                                   AffineTransform xform,
                                                   RenderingHints hints) {
        return new ColorPaintContext(getRGB(), cm);
    }

    /**
     * Returns the transparency mode for this {@code Color}.  This is
     * required to implement the {@code Paint} interface.
     * @return this {@code Color} object's transparency mode.
     * @see Paint
     * @see Transparency
     * @see #createContext
     */
    public int getTransparency() {
        int alpha = getAlpha();
        if (alpha == 0xff) {
            return Transparency.OPAQUE;
        }
        else if (alpha == 0) {
            return Transparency.BITMASK;
        }
        else {
            return Transparency.TRANSLUCENT;
        }
    }

}
