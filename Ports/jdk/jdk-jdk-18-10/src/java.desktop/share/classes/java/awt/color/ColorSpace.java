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

/* ********************************************************************
 **********************************************************************
 **********************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997                      ***
 *** As  an unpublished  work pursuant to Title 17 of the United    ***
 *** States Code.  All rights reserved.                             ***
 **********************************************************************
 **********************************************************************
 **********************************************************************/

package java.awt.color;

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Native;

/**
 * This abstract class is used to serve as a color space tag to identify the
 * specific color space of a {@code Color} object or, via a {@code ColorModel}
 * object, of an {@code Image}, a {@code BufferedImage}, or a
 * {@code GraphicsDevice}. It contains methods that transform colors in a
 * specific color space to/from sRGB and to/from a well-defined CIEXYZ color
 * space.
 * <p>
 * For purposes of the methods in this class, colors are represented as arrays
 * of color components represented as floats in a normalized range defined by
 * each {@code ColorSpace}. For many {@code ColorSpaces} (e.g. sRGB), this range
 * is 0.0 to 1.0. However, some {@code ColorSpaces} have components whose values
 * have a different range. Methods are provided to inquire per component minimum
 * and maximum normalized values.
 * <p>
 * Several variables are defined for purposes of referring to color space types
 * (e.g. {@code TYPE_RGB}, {@code TYPE_XYZ}, etc.) and to refer to specific
 * color spaces (e.g. {@code CS_sRGB} and {@code CS_CIEXYZ}). sRGB is a proposed
 * standard RGB color space. For more information, see
 * <a href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">
 * http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html</a>.
 * <p>
 * The purpose of the methods to transform to/from the well-defined CIEXYZ color
 * space is to support conversions between any two color spaces at a reasonably
 * high degree of accuracy. It is expected that particular implementations of
 * subclasses of {@code ColorSpace} (e.g. {@code ICC_ColorSpace}) will support
 * high performance conversion based on underlying platform color management
 * systems.
 * <p>
 * The {@code CS_CIEXYZ} space used by the {@code toCIEXYZ/fromCIEXYZ} methods
 * can be described as follows:
 * <pre>
 *
 * &nbsp;   CIEXYZ
 * &nbsp;   viewing illuminance: 200 lux
 * &nbsp;   viewing white point: CIE D50
 * &nbsp;   media white point: "that of a perfectly reflecting diffuser" -- D50
 * &nbsp;   media black point: 0 lux or 0 Reflectance
 * &nbsp;   flare: 1 percent
 * &nbsp;   surround: 20percent of the media white point
 * &nbsp;   media description: reflection print (i.e., RLAB, Hunt viewing media)
 * &nbsp;   note: For developers creating an ICC profile for this conversion
 * &nbsp;         space, the following is applicable. Use a simple Von Kries
 * &nbsp;         white point adaptation folded into the 3X3 matrix parameters
 * &nbsp;         and fold the flare and surround effects into the three
 * &nbsp;         one-dimensional lookup tables (assuming one uses the minimal
 * &nbsp;         model for monitors).
 *
 * </pre>
 *
 * @see ICC_ColorSpace
 */
public abstract class ColorSpace implements Serializable {

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -409452704308689724L;

    /**
     * One of the {@code ColorSpace} type constants.
     */
    private final int type;

    /**
     * The number of components in the color space.
     */
    private final int numComponents;

    /**
     * Lazy-initialized names of components in the color space.
     */
    private transient volatile String[] compName;

    /**
     * The lazy cache of singletons for the predefined built-in color spaces.
     */
    private interface BuiltInSpace {

        ColorSpace SRGB = new ICC_ColorSpace(ICC_Profile.getInstance(CS_sRGB));

        ColorSpace LRGB =
                new ICC_ColorSpace(ICC_Profile.getInstance(CS_LINEAR_RGB));

        ColorSpace XYZ = new ICC_ColorSpace(ICC_Profile.getInstance(CS_CIEXYZ));

        ColorSpace PYCC = new ICC_ColorSpace(ICC_Profile.getInstance(CS_PYCC));

        ColorSpace GRAY = new ICC_ColorSpace(ICC_Profile.getInstance(CS_GRAY));
    }

    /**
     * Any of the family of XYZ color spaces.
     */
    @Native public static final int TYPE_XYZ = 0;

    /**
     * Any of the family of Lab color spaces.
     */
    @Native public static final int TYPE_Lab = 1;

    /**
     * Any of the family of Luv color spaces.
     */
    @Native public static final int TYPE_Luv = 2;

    /**
     * Any of the family of YCbCr color spaces.
     */
    @Native public static final int TYPE_YCbCr = 3;

    /**
     * Any of the family of Yxy color spaces.
     */
    @Native public static final int TYPE_Yxy = 4;

    /**
     * Any of the family of RGB color spaces.
     */
    @Native public static final int TYPE_RGB = 5;

    /**
     * Any of the family of GRAY color spaces.
     */
    @Native public static final int TYPE_GRAY = 6;

    /**
     * Any of the family of HSV color spaces.
     */
    @Native public static final int TYPE_HSV = 7;

    /**
     * Any of the family of HLS color spaces.
     */
    @Native public static final int TYPE_HLS = 8;

    /**
     * Any of the family of CMYK color spaces.
     */
    @Native public static final int TYPE_CMYK = 9;

    /**
     * Any of the family of CMY color spaces.
     */
    @Native public static final int TYPE_CMY = 11;

    /**
     * Generic 2 component color spaces.
     */
    @Native public static final int TYPE_2CLR = 12;

    /**
     * Generic 3 component color spaces.
     */
    @Native public static final int TYPE_3CLR = 13;

    /**
     * Generic 4 component color spaces.
     */
    @Native public static final int TYPE_4CLR = 14;

    /**
     * Generic 5 component color spaces.
     */
    @Native public static final int TYPE_5CLR = 15;

    /**
     * Generic 6 component color spaces.
     */
    @Native public static final int TYPE_6CLR = 16;

    /**
     * Generic 7 component color spaces.
     */
    @Native public static final int TYPE_7CLR = 17;

    /**
     * Generic 8 component color spaces.
     */
    @Native public static final int TYPE_8CLR = 18;

    /**
     * Generic 9 component color spaces.
     */
    @Native public static final int TYPE_9CLR = 19;

    /**
     * Generic 10 component color spaces.
     */
    @Native public static final int TYPE_ACLR = 20;

    /**
     * Generic 11 component color spaces.
     */
    @Native public static final int TYPE_BCLR = 21;

    /**
     * Generic 12 component color spaces.
     */
    @Native public static final int TYPE_CCLR = 22;

    /**
     * Generic 13 component color spaces.
     */
    @Native public static final int TYPE_DCLR = 23;

    /**
     * Generic 14 component color spaces.
     */
    @Native public static final int TYPE_ECLR = 24;

    /**
     * Generic 15 component color spaces.
     */
    @Native public static final int TYPE_FCLR = 25;


    /**
     * The built-in sRGB color space defined at
     * <a href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">
     * http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html</a>.
     */
    @Native public static final int CS_sRGB = 1000;

    /**
     * The built-in linear RGB color space. This space is based on the same RGB
     * primaries as {@code CS_sRGB}, but has a linear tone reproduction curve.
     */
    @Native public static final int CS_LINEAR_RGB = 1004;

    /**
     * The built-in CIEXYZ conversion color space defined above.
     */
    @Native public static final int CS_CIEXYZ = 1001;

    /**
     * The built-in Photo YCC conversion color space.
     */
    @Native public static final int CS_PYCC = 1002;

    /**
     * The built-in linear gray scale color space.
     */
    @Native public static final int CS_GRAY = 1003;

    /**
     * Constructs a {@code ColorSpace} object given a color space type and the
     * number of components.
     *
     * @param  type one of the {@code ColorSpace} type constants
     * @param  numComponents the number of components in the color space
     */
    protected ColorSpace(int type, int numComponents) {
        this.type = type;
        this.numComponents = numComponents;
    }

    /**
     * Returns a {@code ColorSpace} representing one of the specific predefined
     * color spaces.
     *
     * @param  cspace a specific color space identified by one of the predefined
     *         class constants (e.g. {@code CS_sRGB}, {@code CS_LINEAR_RGB},
     *         {@code CS_CIEXYZ}, {@code CS_GRAY}, or {@code CS_PYCC})
     * @return the requested {@code ColorSpace} object
     */
    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public static ColorSpace getInstance(int cspace) {
        return switch (cspace) {
            case CS_sRGB -> BuiltInSpace.SRGB;
            case CS_LINEAR_RGB -> BuiltInSpace.LRGB;
            case CS_CIEXYZ -> BuiltInSpace.XYZ;
            case CS_PYCC -> BuiltInSpace.PYCC;
            case CS_GRAY -> BuiltInSpace.GRAY;
            default -> {
                throw new IllegalArgumentException("Unknown color space");
            }
        };
    }

    /**
     * Returns true if the {@code ColorSpace} is {@code CS_sRGB}.
     *
     * @return {@code true} if this is a {@code CS_sRGB} color space,
     *         {@code false} if it is not
     */
    public boolean isCS_sRGB() {
        return this == BuiltInSpace.SRGB;
    }

    /**
     * Transforms a color value assumed to be in this {@code ColorSpace} into a
     * value in the default {@code CS_sRGB} color space.
     * <p>
     * This method transforms color values using algorithms designed to produce
     * the best perceptual match between input and output colors. In order to do
     * colorimetric conversion of color values, you should use the
     * {@code toCIEXYZ} method of this color space to first convert from the
     * input color space to the CS_CIEXYZ color space, and then use the
     * {@code fromCIEXYZ} method of the {@code CS_sRGB} color space to convert
     * from {@code CS_CIEXYZ} to the output color space. See
     * {@link #toCIEXYZ(float[]) toCIEXYZ} and
     * {@link #fromCIEXYZ(float[]) fromCIEXYZ} for further information.
     *
     * @param  colorvalue a float array with length of at least the number of
     *         components in this {@code ColorSpace}
     * @return a float array of length 3
     * @throws ArrayIndexOutOfBoundsException if array length is not at least
     *         the number of components in this {@code ColorSpace}
     */
    public abstract float[] toRGB(float[] colorvalue);

    /**
     * Transforms a color value assumed to be in the default {@code CS_sRGB}
     * color space into this {@code ColorSpace}.
     * <p>
     * This method transforms color values using algorithms designed to produce
     * the best perceptual match between input and output colors. In order to do
     * colorimetric conversion of color values, you should use the
     * {@code toCIEXYZ} method of the {@code CS_sRGB} color space to first
     * convert from the input color space to the {@code CS_CIEXYZ} color space,
     * and then use the {@code fromCIEXYZ} method of this color space to convert
     * from {@code CS_CIEXYZ} to the output color space. See
     * {@link #toCIEXYZ(float[]) toCIEXYZ} and
     * {@link #fromCIEXYZ(float[]) fromCIEXYZ} for further information.
     *
     * @param  rgbvalue a float array with length of at least 3
     * @return a float array with length equal to the number of components in
     *         this {@code ColorSpace}
     * @throws ArrayIndexOutOfBoundsException if array length is not at least 3
     */
    public abstract float[] fromRGB(float[] rgbvalue);

    /**
     * Transforms a color value assumed to be in this {@code ColorSpace} into
     * the {@code CS_CIEXYZ} conversion color space.
     * <p>
     * This method transforms color values using relative colorimetry, as
     * defined by the International Color Consortium standard. This means that
     * the XYZ values returned by this method are represented relative to the
     * D50 white point of the {@code CS_CIEXYZ} color space. This representation
     * is useful in a two-step color conversion process in which colors are
     * transformed from an input color space to {@code CS_CIEXYZ} and then to an
     * output color space. This representation is not the same as the XYZ values
     * that would be measured from the given color value by a colorimeter. A
     * further transformation is necessary to compute the XYZ values that would
     * be measured using current CIE recommended practices. See the
     * {@link ICC_ColorSpace#toCIEXYZ(float[]) toCIEXYZ} method of
     * {@code ICC_ColorSpace} for further information.
     *
     * @param  colorvalue a float array with length of at least the number of
     *         components in this {@code ColorSpace}
     * @return a float array of length 3
     * @throws ArrayIndexOutOfBoundsException if array length is not at least
     *         the number of components in this {@code ColorSpace}.
     */
    public abstract float[] toCIEXYZ(float[] colorvalue);

    /**
     * Transforms a color value assumed to be in the {@code CS_CIEXYZ}
     * conversion color space into this {@code ColorSpace}.
     * <p>
     * This method transforms color values using relative colorimetry, as
     * defined by the International Color Consortium standard. This means that
     * the XYZ argument values taken by this method are represented relative to
     * the D50 white point of the {@code CS_CIEXYZ} color space. This
     * representation is useful in a two-step color conversion process in which
     * colors are transformed from an input color space to {@code CS_CIEXYZ} and
     * then to an output color space. The color values returned by this method
     * are not those that would produce the XYZ value passed to the method when
     * measured by a colorimeter. If you have XYZ values corresponding to
     * measurements made using current CIE recommended practices, they must be
     * converted to D50 relative values before being passed to this method. See
     * the {@link ICC_ColorSpace#fromCIEXYZ(float[]) fromCIEXYZ} method of
     * {@code ICC_ColorSpace} for further information.
     *
     * @param  colorvalue a float array with length of at least 3
     * @return a float array with length equal to the number of components in
     *         this {@code ColorSpace}
     * @throws ArrayIndexOutOfBoundsException if array length is not at least 3
     */
    public abstract float[] fromCIEXYZ(float[] colorvalue);

    /**
     * Returns the color space type of this {@code ColorSpace} (for example
     * {@code TYPE_RGB}, {@code TYPE_XYZ}, ...). The type defines the number of
     * components of the color space and the interpretation, e.g.
     * {@code TYPE_RGB} identifies a color space with three components - red,
     * green, and blue. It does not define the particular color characteristics
     * of the space, e.g. the chromaticities of the primaries.
     *
     * @return the type constant that represents the type of this
     *         {@code ColorSpace}
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the number of components of this ColorSpace.
     *
     * @return the number of components in this {@code ColorSpace}
     */
    public int getNumComponents() {
        return numComponents;
    }

    /**
     * Returns the name of the component given the component index.
     *
     * @param  component the component index
     * @return the name of the component at the specified index
     * @throws IllegalArgumentException if {@code component} is less than 0 or
     *         greater than {@code numComponents - 1}
     */
    public String getName(int component) {
        rangeCheck(component);
        if (compName == null) {
            compName = switch (type) {
                case TYPE_XYZ -> new String[]{"X", "Y", "Z"};
                case TYPE_Lab -> new String[]{"L", "a", "b"};
                case TYPE_Luv -> new String[]{"L", "u", "v"};
                case TYPE_YCbCr -> new String[]{"Y", "Cb", "Cr"};
                case TYPE_Yxy -> new String[]{"Y", "x", "y"};
                case TYPE_RGB -> new String[]{"Red", "Green", "Blue"};
                case TYPE_GRAY -> new String[]{"Gray"};
                case TYPE_HSV -> new String[]{"Hue", "Saturation", "Value"};
                case TYPE_HLS -> new String[]{"Hue", "Lightness", "Saturation"};
                case TYPE_CMYK -> new String[]{"Cyan", "Magenta", "Yellow",
                                               "Black"};
                case TYPE_CMY -> new String[]{"Cyan", "Magenta", "Yellow"};
                default -> {
                    String[] tmp = new String[getNumComponents()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = "Unnamed color component(" + i + ")";
                    }
                    yield tmp;
                }
            };
        }
        return compName[component];
    }

    /**
     * Returns the minimum normalized color component value for the specified
     * component. The default implementation in this abstract class returns 0.0
     * for all components. Subclasses should override this method if necessary.
     *
     * @param  component the component index
     * @return the minimum normalized component value
     * @throws IllegalArgumentException if component is less than 0 or greater
     *         than {@code numComponents - 1}
     * @since 1.4
     */
    public float getMinValue(int component) {
        rangeCheck(component);
        return 0.0f;
    }

    /**
     * Returns the maximum normalized color component value for the specified
     * component. The default implementation in this abstract class returns 1.0
     * for all components. Subclasses should override this method if necessary.
     *
     * @param  component the component index
     * @return the maximum normalized component value
     * @throws IllegalArgumentException if component is less than 0 or greater
     *         than {@code numComponents - 1}
     * @since 1.4
     */
    public float getMaxValue(int component) {
        rangeCheck(component);
        return 1.0f;
    }

    /**
     * Checks that {@code component} is in range of the number of components.
     *
     * @param  component the component index
     * @throws IllegalArgumentException if component is less than 0 or greater
     *         than {@code numComponents - 1}
     */
    final void rangeCheck(int component) {
        if (component < 0 || component > getNumComponents() - 1) {
            throw new IllegalArgumentException(
                    "Component index out of range: " + component);
        }
    }
}
