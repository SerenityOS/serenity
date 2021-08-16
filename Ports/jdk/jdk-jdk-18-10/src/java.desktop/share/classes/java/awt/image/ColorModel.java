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

package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.ColorTransform;
import sun.java2d.cmm.PCMM;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Arrays;

/**
 * The {@code ColorModel} abstract class encapsulates the
 * methods for translating a pixel value to color components
 * (for example, red, green, and blue) and an alpha component.
 * In order to render an image to the screen, a printer, or another
 * image, pixel values must be converted to color and alpha components.
 * As arguments to or return values from methods of this class,
 * pixels are represented as 32-bit ints or as arrays of primitive types.
 * The number, order, and interpretation of color components for a
 * {@code ColorModel} is specified by its {@code ColorSpace}.
 * A {@code ColorModel} used with pixel data that does not include
 * alpha information treats all pixels as opaque, which is an alpha
 * value of 1.0.
 * <p>
 * This {@code ColorModel} class supports two representations of
 * pixel values.  A pixel value can be a single 32-bit int or an
 * array of primitive types.  The Java(tm) Platform 1.0 and 1.1 APIs
 * represented pixels as single {@code byte} or single
 * {@code int} values.  For purposes of the {@code ColorModel}
 * class, pixel value arguments were passed as ints.  The Java(tm) 2
 * Platform API introduced additional classes for representing images.
 * With {@link BufferedImage} or {@link RenderedImage}
 * objects, based on {@link Raster} and {@link SampleModel} classes, pixel
 * values might not be conveniently representable as a single int.
 * Consequently, {@code ColorModel} now has methods that accept
 * pixel values represented as arrays of primitive types.  The primitive
 * type used by a particular {@code ColorModel} object is called its
 * transfer type.
 * <p>
 * {@code ColorModel} objects used with images for which pixel values
 * are not conveniently representable as a single int throw an
 * {@link IllegalArgumentException} when methods taking a single int pixel
 * argument are called.  Subclasses of {@code ColorModel} must
 * specify the conditions under which this occurs.  This does not
 * occur with {@link DirectColorModel} or {@link IndexColorModel} objects.
 * <p>
 * Currently, the transfer types supported by the Java 2D(tm) API are
 * DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, DataBuffer.TYPE_INT,
 * DataBuffer.TYPE_SHORT, DataBuffer.TYPE_FLOAT, and DataBuffer.TYPE_DOUBLE.
 * Most rendering operations will perform much faster when using ColorModels
 * and images based on the first three of these types.  In addition, some
 * image filtering operations are not supported for ColorModels and
 * images based on the latter three types.
 * The transfer type for a particular {@code ColorModel} object is
 * specified when the object is created, either explicitly or by default.
 * All subclasses of {@code ColorModel} must specify what the
 * possible transfer types are and how the number of elements in the
 * primitive arrays representing pixels is determined.
 * <p>
 * For {@code BufferedImages}, the transfer type of its
 * {@code Raster} and of the {@code Raster} object's
 * {@code SampleModel} (available from the
 * {@code getTransferType} methods of these classes) must match that
 * of the {@code ColorModel}.  The number of elements in an array
 * representing a pixel for the {@code Raster} and
 * {@code SampleModel} (available from the
 * {@code getNumDataElements} methods of these classes) must match
 * that of the {@code ColorModel}.
 * <p>
 * The algorithm used to convert from pixel values to color and alpha
 * components varies by subclass.  For example, there is not necessarily
 * a one-to-one correspondence between samples obtained from the
 * {@code SampleModel} of a {@code BufferedImage} object's
 * {@code Raster} and color/alpha components.  Even when
 * there is such a correspondence, the number of bits in a sample is not
 * necessarily the same as the number of bits in the corresponding color/alpha
 * component.  Each subclass must specify how the translation from
 * pixel values to color/alpha components is done.
 * <p>
 * Methods in the {@code ColorModel} class use two different
 * representations of color and alpha components - a normalized form
 * and an unnormalized form.  In the normalized form, each component is a
 * {@code float} value between some minimum and maximum values.  For
 * the alpha component, the minimum is 0.0 and the maximum is 1.0.  For
 * color components the minimum and maximum values for each component can
 * be obtained from the {@code ColorSpace} object.  These values
 * will often be 0.0 and 1.0 (e.g. normalized component values for the
 * default sRGB color space range from 0.0 to 1.0), but some color spaces
 * have component values with different upper and lower limits.  These
 * limits can be obtained using the {@code getMinValue} and
 * {@code getMaxValue} methods of the {@code ColorSpace}
 * class.  Normalized color component values are not premultiplied.
 * All {@code ColorModels} must support the normalized form.
 * <p>
 * In the unnormalized
 * form, each component is an unsigned integral value between 0 and
 * 2<sup>n</sup> - 1, where n is the number of significant bits for a
 * particular component.  If pixel values for a particular
 * {@code ColorModel} represent color samples premultiplied by
 * the alpha sample, unnormalized color component values are
 * also premultiplied.  The unnormalized form is used only with instances
 * of {@code ColorModel} whose {@code ColorSpace} has minimum
 * component values of 0.0 for all components and maximum values of
 * 1.0 for all components.
 * The unnormalized form for color and alpha components can be a convenient
 * representation for {@code ColorModels} whose normalized component
 * values all lie
 * between 0.0 and 1.0.  In such cases the integral value 0 maps to 0.0 and
 * the value 2<sup>n</sup> - 1 maps to 1.0.  In other cases, such as
 * when the normalized component values can be either negative or positive,
 * the unnormalized form is not convenient.  Such {@code ColorModel}
 * objects throw an {@link IllegalArgumentException} when methods involving
 * an unnormalized argument are called.  Subclasses of {@code ColorModel}
 * must specify the conditions under which this occurs.
 *
 * @see IndexColorModel
 * @see ComponentColorModel
 * @see PackedColorModel
 * @see DirectColorModel
 * @see java.awt.Image
 * @see BufferedImage
 * @see RenderedImage
 * @see java.awt.color.ColorSpace
 * @see SampleModel
 * @see Raster
 * @see DataBuffer
 */
public abstract class ColorModel implements Transparency{
    private long pData;         // Placeholder for data for native functions

    /**
     * The total number of bits in the pixel.
     */
    protected int pixel_bits;
    int[] nBits;
    int transparency = Transparency.TRANSLUCENT;
    boolean supportsAlpha = true;
    boolean isAlphaPremultiplied = false;
    int numComponents = -1;
    int numColorComponents = -1;
    ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    int colorSpaceType = ColorSpace.TYPE_RGB;
    int maxBits;
    boolean is_sRGB = true;

    /**
     * Data type of the array used to represent pixel values.
     */
    protected int transferType;

    /**
     * This is copied from java.awt.Toolkit since we need the library
     * loaded in java.awt.image also:
     *
     * WARNING: This is a temporary workaround for a problem in the
     * way the AWT loads native libraries. A number of classes in the
     * AWT package have a native method, initIDs(), which initializes
     * the JNI field and method ids used in the native portion of
     * their implementation.
     *
     * Since the use and storage of these ids is done by the
     * implementation libraries, the implementation of these method is
     * provided by the particular AWT implementations (for example,
     * "Toolkit"s/Peer), such as Motif, Microsoft Windows, or Tiny. The
     * problem is that this means that the native libraries must be
     * loaded by the java.* classes, which do not necessarily know the
     * names of the libraries to load. A better way of doing this
     * would be to provide a separate library which defines java.awt.*
     * initIDs, and exports the relevant symbols out to the
     * implementation libraries.
     *
     * For now, we know it's done by the implementation, and we assume
     * that the name of the library is "awt".  -br.
     */
    private static boolean loaded = false;
    @SuppressWarnings("removal")
    static void loadLibraries() {
        if (!loaded) {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("awt");
                        return null;
                    }
                });
            loaded = true;
        }
    }
    private static native void initIDs();
    static {
        /* ensure that the proper libraries are loaded */
        loadLibraries();
        initIDs();
    }
    private static ColorModel RGBdefault;

    /**
     * Returns a {@code DirectColorModel} that describes the default
     * format for integer RGB values used in many of the methods in the
     * AWT image interfaces for the convenience of the programmer.
     * The color space is the default {@link ColorSpace}, sRGB.
     * The format for the RGB values is an integer with 8 bits
     * each of alpha, red, green, and blue color components ordered
     * correspondingly from the most significant byte to the least
     * significant byte, as in:  0xAARRGGBB.  Color components are
     * not premultiplied by the alpha component.  This format does not
     * necessarily represent the native or the most efficient
     * {@code ColorModel} for a particular device or for all images.
     * It is merely used as a common color model format.
     * @return a {@code DirectColorModel} object describing default
     *          RGB values.
     */
    public static ColorModel getRGBdefault() {
        if (RGBdefault == null) {
            RGBdefault = new DirectColorModel(32,
                                              0x00ff0000,       // Red
                                              0x0000ff00,       // Green
                                              0x000000ff,       // Blue
                                              0xff000000        // Alpha
                                              );
        }
        return RGBdefault;
    }

    /**
     * Constructs a {@code ColorModel} that translates pixels of the
     * specified number of bits to color/alpha components.  The color
     * space is the default RGB {@code ColorSpace}, which is sRGB.
     * Pixel values are assumed to include alpha information.  If color
     * and alpha information are represented in the pixel value as
     * separate spatial bands, the color bands are assumed not to be
     * premultiplied with the alpha value. The transparency type is
     * java.awt.Transparency.TRANSLUCENT.  The transfer type will be the
     * smallest of DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT,
     * or DataBuffer.TYPE_INT that can hold a single pixel
     * (or DataBuffer.TYPE_UNDEFINED if bits is greater
     * than 32).  Since this constructor has no information about the
     * number of bits per color and alpha component, any subclass calling
     * this constructor should override any method that requires this
     * information.
     * @param bits the number of bits of a pixel
     * @throws IllegalArgumentException if the number
     *          of bits in {@code bits} is less than 1
     */
    public ColorModel(int bits) {
        pixel_bits = bits;
        if (bits < 1) {
            throw new IllegalArgumentException("Number of bits must be > 0");
        }
        numComponents = 4;
        numColorComponents = 3;
        maxBits = bits;
        // REMIND: make sure transferType is set correctly
        transferType = ColorModel.getDefaultTransferType(bits);
    }

    /**
     * Constructs a {@code ColorModel} that translates pixel values
     * to color/alpha components.  Color components will be in the
     * specified {@code ColorSpace}. {@code pixel_bits} is the
     * number of bits in the pixel values.  The bits array
     * specifies the number of significant bits per color and alpha component.
     * Its length should be the number of components in the
     * {@code ColorSpace} if there is no alpha information in the
     * pixel values, or one more than this number if there is alpha
     * information.  {@code hasAlpha} indicates whether or not alpha
     * information is present.  The {@code boolean}
     * {@code isAlphaPremultiplied} specifies how to interpret pixel
     * values in which color and alpha information are represented as
     * separate spatial bands.  If the {@code boolean}
     * is {@code true}, color samples are assumed to have been
     * multiplied by the alpha sample.  The {@code transparency}
     * specifies what alpha values can be represented by this color model.
     * The transfer type is the type of primitive array used to represent
     * pixel values.  Note that the bits array contains the number of
     * significant bits per color/alpha component after the translation
     * from pixel values.  For example, for an
     * {@code IndexColorModel} with {@code pixel_bits} equal to
     * 16, the bits array might have four elements with each element set
     * to 8.
     * @param pixel_bits the number of bits in the pixel values
     * @param bits array that specifies the number of significant bits
     *          per color and alpha component
     * @param cspace the specified {@code ColorSpace}
     * @param hasAlpha {@code true} if alpha information is present;
     *          {@code false} otherwise
     * @param isAlphaPremultiplied {@code true} if color samples are
     *          assumed to be premultiplied by the alpha samples;
     *          {@code false} otherwise
     * @param transparency what alpha values can be represented by this
     *          color model
     * @param transferType the type of the array used to represent pixel
     *          values
     * @throws IllegalArgumentException if the length of
     *          the bit array is less than the number of color or alpha
     *          components in this {@code ColorModel}, or if the
     *          transparency is not a valid value.
     * @throws IllegalArgumentException if the sum of the number
     *          of bits in {@code bits} is less than 1 or if
     *          any of the elements in {@code bits} is less than 0.
     * @see java.awt.Transparency
     */
    protected ColorModel(int pixel_bits, int[] bits, ColorSpace cspace,
                         boolean hasAlpha,
                         boolean isAlphaPremultiplied,
                         int transparency,
                         int transferType) {
        colorSpace                = cspace;
        colorSpaceType            = cspace.getType();
        numColorComponents        = cspace.getNumComponents();
        numComponents             = numColorComponents + (hasAlpha ? 1 : 0);
        supportsAlpha             = hasAlpha;
        if (bits.length < numComponents) {
            throw new IllegalArgumentException("Number of color/alpha "+
                                               "components should be "+
                                               numComponents+
                                               " but length of bits array is "+
                                               bits.length);
        }

        // 4186669
        if (transparency < Transparency.OPAQUE ||
            transparency > Transparency.TRANSLUCENT)
        {
            throw new IllegalArgumentException("Unknown transparency: "+
                                               transparency);
        }

        if (supportsAlpha == false) {
            this.isAlphaPremultiplied = false;
            this.transparency = Transparency.OPAQUE;
        }
        else {
            this.isAlphaPremultiplied = isAlphaPremultiplied;
            this.transparency         = transparency;
        }

        /*
         * We need significant bits value only for the length
         * of number of components, so we truncate remaining part.
         * It also helps in hashCode calculation since bits[] can contain
         * different values after the length of number of components between
         * two ColorModels.
         */
        nBits = Arrays.copyOf(bits, numComponents);
        this.pixel_bits = pixel_bits;
        if (pixel_bits <= 0) {
            throw new IllegalArgumentException("Number of pixel bits must "+
                                               "be > 0");
        }
        // Check for bits < 0
        maxBits = 0;
        for (int i=0; i < bits.length; i++) {
            // bug 4304697
            if (bits[i] < 0) {
                throw new
                    IllegalArgumentException("Number of bits must be >= 0");
            }
            if (maxBits < bits[i]) {
                maxBits = bits[i];
            }
        }

        // Make sure that we don't have all 0-bit components
        if (maxBits == 0) {
            throw new IllegalArgumentException("There must be at least "+
                                               "one component with > 0 "+
                                              "pixel bits.");
        }

        // Save this since we always need to check if it is the default CS
        if (cspace != ColorSpace.getInstance(ColorSpace.CS_sRGB)) {
            is_sRGB = false;
        }

        // Save the transfer type
        this.transferType = transferType;
    }

    /**
     * Returns whether or not alpha is supported in this
     * {@code ColorModel}.
     * @return {@code true} if alpha is supported in this
     * {@code ColorModel}; {@code false} otherwise.
     */
    public final boolean hasAlpha() {
        return supportsAlpha;
    }

    /**
     * Returns whether or not the alpha has been premultiplied in the
     * pixel values to be translated by this {@code ColorModel}.
     * If the boolean is {@code true}, this {@code ColorModel}
     * is to be used to interpret pixel values in which color and alpha
     * information are represented as separate spatial bands, and color
     * samples are assumed to have been multiplied by the
     * alpha sample.
     * @return {@code true} if the alpha values are premultiplied
     *          in the pixel values to be translated by this
     *          {@code ColorModel}; {@code false} otherwise.
     */
    public final boolean isAlphaPremultiplied() {
        return isAlphaPremultiplied;
    }

    /**
     * Returns the transfer type of this {@code ColorModel}.
     * The transfer type is the type of primitive array used to represent
     * pixel values as arrays.
     * @return the transfer type.
     * @since 1.3
     */
    public final int getTransferType() {
        return transferType;
    }

    /**
     * Returns the number of bits per pixel described by this
     * {@code ColorModel}.
     * @return the number of bits per pixel.
     */
    public int getPixelSize() {
        return pixel_bits;
    }

    /**
     * Returns the number of bits for the specified color/alpha component.
     * Color components are indexed in the order specified by the
     * {@code ColorSpace}.  Typically, this order reflects the name
     * of the color space type. For example, for TYPE_RGB, index 0
     * corresponds to red, index 1 to green, and index 2
     * to blue.  If this {@code ColorModel} supports alpha, the alpha
     * component corresponds to the index following the last color
     * component.
     * @param componentIdx the index of the color/alpha component
     * @return the number of bits for the color/alpha component at the
     *          specified index.
     * @throws ArrayIndexOutOfBoundsException if {@code componentIdx}
     *         is greater than the number of components or
     *         less than zero
     * @throws NullPointerException if the number of bits array is
     *         {@code null}
     */
    public int getComponentSize(int componentIdx) {
        // REMIND:
        if (nBits == null) {
            throw new NullPointerException("Number of bits array is null.");
        }

        return nBits[componentIdx];
    }

    /**
     * Returns an array of the number of bits per color/alpha component.
     * The array contains the color components in the order specified by the
     * {@code ColorSpace}, followed by the alpha component, if
     * present.
     * @return an array of the number of bits per color/alpha component
     */
    public int[] getComponentSize() {
        if (nBits != null) {
            return nBits.clone();
        }

        return null;
    }

    /**
     * Returns the transparency.  Returns either OPAQUE, BITMASK,
     * or TRANSLUCENT.
     * @return the transparency of this {@code ColorModel}.
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     */
    public int getTransparency() {
        return transparency;
    }

    /**
     * Returns the number of components, including alpha, in this
     * {@code ColorModel}.  This is equal to the number of color
     * components, optionally plus one, if there is an alpha component.
     * @return the number of components in this {@code ColorModel}
     */
    public int getNumComponents() {
        return numComponents;
    }

    /**
     * Returns the number of color components in this
     * {@code ColorModel}.
     * This is the number of components returned by
     * {@link ColorSpace#getNumComponents}.
     * @return the number of color components in this
     * {@code ColorModel}.
     * @see ColorSpace#getNumComponents
     */
    public int getNumColorComponents() {
        return numColorComponents;
    }

    /**
     * Returns the red color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB ColorSpace, sRGB.  A color conversion
     * is done if necessary.  The pixel value is specified as an int.
     * An {@code IllegalArgumentException} is thrown if pixel
     * values for this {@code ColorModel} are not conveniently
     * representable as a single int.  The returned value is not a
     * pre-multiplied value.  For example, if the
     * alpha is premultiplied, this method divides it out before returning
     * the value.  If the alpha value is 0, the red value is 0.
     * @param pixel a specified pixel
     * @return the value of the red component of the specified pixel.
     */
    public abstract int getRed(int pixel);

    /**
     * Returns the green color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB ColorSpace, sRGB.  A color conversion
     * is done if necessary.  The pixel value is specified as an int.
     * An {@code IllegalArgumentException} is thrown if pixel
     * values for this {@code ColorModel} are not conveniently
     * representable as a single int.  The returned value is a non
     * pre-multiplied value.  For example, if the alpha is premultiplied,
     * this method divides it out before returning
     * the value.  If the alpha value is 0, the green value is 0.
     * @param pixel the specified pixel
     * @return the value of the green component of the specified pixel.
     */
    public abstract int getGreen(int pixel);

    /**
     * Returns the blue color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB ColorSpace, sRGB.  A color conversion
     * is done if necessary.  The pixel value is specified as an int.
     * An {@code IllegalArgumentException} is thrown if pixel values
     * for this {@code ColorModel} are not conveniently representable
     * as a single int.  The returned value is a non pre-multiplied
     * value, for example, if the alpha is premultiplied, this method
     * divides it out before returning the value.  If the alpha value is
     * 0, the blue value is 0.
     * @param pixel the specified pixel
     * @return the value of the blue component of the specified pixel.
     */
    public abstract int getBlue(int pixel);

    /**
     * Returns the alpha component for the specified pixel, scaled
     * from 0 to 255.  The pixel value is specified as an int.
     * An {@code IllegalArgumentException} is thrown if pixel
     * values for this {@code ColorModel} are not conveniently
     * representable as a single int.
     * @param pixel the specified pixel
     * @return the value of alpha component of the specified pixel.
     */
    public abstract int getAlpha(int pixel);

    /**
     * Returns the color/alpha components of the pixel in the default
     * RGB color model format.  A color conversion is done if necessary.
     * The pixel value is specified as an int.
     * An {@code IllegalArgumentException} thrown if pixel values
     * for this {@code ColorModel} are not conveniently representable
     * as a single int.  The returned value is in a non
     * pre-multiplied format. For example, if the alpha is premultiplied,
     * this method divides it out of the color components.  If the alpha
     * value is 0, the color values are 0.
     * @param pixel the specified pixel
     * @return the RGB value of the color/alpha components of the
     *          specified pixel.
     * @see ColorModel#getRGBdefault
     */
    public int getRGB(int pixel) {
        return (getAlpha(pixel) << 24)
            | (getRed(pixel) << 16)
            | (getGreen(pixel) << 8)
            | (getBlue(pixel) << 0);
    }

    /**
     * Returns the red color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB {@code ColorSpace}, sRGB.  A
     * color conversion is done if necessary.  The pixel value is
     * specified by an array of data elements of type transferType passed
     * in as an object reference.  The returned value is a non
     * pre-multiplied value.  For example, if alpha is premultiplied,
     * this method divides it out before returning
     * the value.  If the alpha value is 0, the red value is 0.
     * If {@code inData} is not a primitive array of type
     * transferType, a {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code inData} is not large enough to hold a pixel value for
     * this {@code ColorModel}.
     * If this {@code transferType} is not supported, a
     * {@code UnsupportedOperationException} will be
     * thrown.  Since
     * {@code ColorModel} is an abstract class, any instance
     * must be an instance of a subclass.  Subclasses inherit the
     * implementation of this method and if they don't override it, this
     * method throws an exception if the subclass uses a
     * {@code transferType} other than
     * {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_USHORT}, or
     * {@code DataBuffer.TYPE_INT}.
     * @param inData an array of pixel values
     * @return the value of the red component of the specified pixel.
     * @throws ClassCastException if {@code inData}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code inData} is not large enough to hold a pixel value
     *  for this {@code ColorModel}
     * @throws UnsupportedOperationException if this
     *  {@code transferType} is not supported by this
     *  {@code ColorModel}
     */
    public int getRed(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte[] bdata = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short[] sdata = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int[] idata = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getRed(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }

    /**
     * Returns the green color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB {@code ColorSpace}, sRGB.  A
     * color conversion is done if necessary.  The pixel value is
     * specified by an array of data elements of type transferType passed
     * in as an object reference.  The returned value will be a non
     * pre-multiplied value.  For example, if the alpha is premultiplied,
     * this method divides it out before returning the value.  If the
     * alpha value is 0, the green value is 0.  If {@code inData} is
     * not a primitive array of type transferType, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code inData} is not large enough to hold a pixel value for
     * this {@code ColorModel}.
     * If this {@code transferType} is not supported, a
     * {@code UnsupportedOperationException} will be
     * thrown.  Since
     * {@code ColorModel} is an abstract class, any instance
     * must be an instance of a subclass.  Subclasses inherit the
     * implementation of this method and if they don't override it, this
     * method throws an exception if the subclass uses a
     * {@code transferType} other than
     * {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_USHORT}, or
     * {@code DataBuffer.TYPE_INT}.
     * @param inData an array of pixel values
     * @return the value of the green component of the specified pixel.
     * @throws ClassCastException if {@code inData}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code inData} is not large enough to hold a pixel value
     *  for this {@code ColorModel}
     * @throws UnsupportedOperationException if this
     *  {@code transferType} is not supported by this
     *  {@code ColorModel}
     */
    public int getGreen(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte[] bdata = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short[] sdata = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int[] idata = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getGreen(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }

    /**
     * Returns the blue color component for the specified pixel, scaled
     * from 0 to 255 in the default RGB {@code ColorSpace}, sRGB.  A
     * color conversion is done if necessary.  The pixel value is
     * specified by an array of data elements of type transferType passed
     * in as an object reference.  The returned value is a non
     * pre-multiplied value.  For example, if the alpha is premultiplied,
     * this method divides it out before returning the value.  If the
     * alpha value is 0, the blue value will be 0.  If
     * {@code inData} is not a primitive array of type transferType,
     * a {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is
     * thrown if {@code inData} is not large enough to hold a pixel
     * value for this {@code ColorModel}.
     * If this {@code transferType} is not supported, a
     * {@code UnsupportedOperationException} will be
     * thrown.  Since
     * {@code ColorModel} is an abstract class, any instance
     * must be an instance of a subclass.  Subclasses inherit the
     * implementation of this method and if they don't override it, this
     * method throws an exception if the subclass uses a
     * {@code transferType} other than
     * {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_USHORT}, or
     * {@code DataBuffer.TYPE_INT}.
     * @param inData an array of pixel values
     * @return the value of the blue component of the specified pixel.
     * @throws ClassCastException if {@code inData}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code inData} is not large enough to hold a pixel value
     *  for this {@code ColorModel}
     * @throws UnsupportedOperationException if this
     *  {@code transferType} is not supported by this
     *  {@code ColorModel}
     */
    public int getBlue(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte[] bdata = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short[] sdata = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int[] idata = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getBlue(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }

    /**
     * Returns the alpha component for the specified pixel, scaled
     * from 0 to 255.  The pixel value is specified by an array of data
     * elements of type transferType passed in as an object reference.
     * If inData is not a primitive array of type transferType, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code inData} is not large enough to hold a pixel value for
     * this {@code ColorModel}.
     * If this {@code transferType} is not supported, a
     * {@code UnsupportedOperationException} will be
     * thrown.  Since
     * {@code ColorModel} is an abstract class, any instance
     * must be an instance of a subclass.  Subclasses inherit the
     * implementation of this method and if they don't override it, this
     * method throws an exception if the subclass uses a
     * {@code transferType} other than
     * {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_USHORT}, or
     * {@code DataBuffer.TYPE_INT}.
     * @param inData the specified pixel
     * @return the alpha component of the specified pixel, scaled from
     * 0 to 255.
     * @throws ClassCastException if {@code inData}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code inData} is not large enough to hold a pixel value
     *  for this {@code ColorModel}
     * @throws UnsupportedOperationException if this
     *  {@code tranferType} is not supported by this
     *  {@code ColorModel}
     */
    public int getAlpha(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte[] bdata = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short[] sdata = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int[] idata = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getAlpha(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }

    /**
     * Returns the color/alpha components for the specified pixel in the
     * default RGB color model format.  A color conversion is done if
     * necessary.  The pixel value is specified by an array of data
     * elements of type transferType passed in as an object reference.
     * If inData is not a primitive array of type transferType, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is
     * thrown if {@code inData} is not large enough to hold a pixel
     * value for this {@code ColorModel}.
     * The returned value will be in a non pre-multiplied format, i.e. if
     * the alpha is premultiplied, this method will divide it out of the
     * color components (if the alpha value is 0, the color values will be 0).
     * @param inData the specified pixel
     * @return the color and alpha components of the specified pixel.
     * @see ColorModel#getRGBdefault
     */
    public int getRGB(Object inData) {
        return (getAlpha(inData) << 24)
            | (getRed(inData) << 16)
            | (getGreen(inData) << 8)
            | (getBlue(inData) << 0);
    }

    /**
     * Returns a data element array representation of a pixel in this
     * {@code ColorModel}, given an integer pixel representation in
     * the default RGB color model.
     * This array can then be passed to the
     * {@link WritableRaster#setDataElements} method of
     * a {@link WritableRaster} object.  If the pixel variable is
     * {@code null}, a new array will be allocated.  If
     * {@code pixel} is not
     * {@code null}, it must be a primitive array of type
     * {@code transferType}; otherwise, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code pixel} is
     * not large enough to hold a pixel value for this
     * {@code ColorModel}. The pixel array is returned.
     * If this {@code transferType} is not supported, a
     * {@code UnsupportedOperationException} will be
     * thrown.  Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param rgb the integer pixel representation in the default RGB
     * color model
     * @param pixel the specified pixel
     * @return an array representation of the specified pixel in this
     *  {@code ColorModel}.
     * @throws ClassCastException if {@code pixel}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code pixel} is not large enough to hold a pixel value
     *  for this {@code ColorModel}
     * @throws UnsupportedOperationException if this
     *  method is not supported by this {@code ColorModel}
     * @see WritableRaster#setDataElements
     * @see SampleModel#setDataElements
     */
    public Object getDataElements(int rgb, Object pixel) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model.");
    }

    /**
     * Returns an array of unnormalized color/alpha components given a pixel
     * in this {@code ColorModel}.  The pixel value is specified as
     * an {@code int}.  An {@code IllegalArgumentException}
     * will be thrown if pixel values for this {@code ColorModel} are
     * not conveniently representable as a single {@code int} or if
     * color component values for this {@code ColorModel} are not
     * conveniently representable in the unnormalized form.
     * For example, this method can be used to retrieve the
     * components for a specific pixel value in a
     * {@code DirectColorModel}.  If the components array is
     * {@code null}, a new array will be allocated.  The
     * components array will be returned.  Color/alpha components are
     * stored in the components array starting at {@code offset}
     * (even if the array is allocated by this method).  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if  the
     * components array is not {@code null} and is not large
     * enough to hold all the color and alpha components (starting at offset).
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param pixel the specified pixel
     * @param components the array to receive the color and alpha
     * components of the specified pixel
     * @param offset the offset into the {@code components} array at
     * which to start storing the color and alpha components
     * @return an array containing the color and alpha components of the
     * specified pixel starting at the specified offset.
     * @throws UnsupportedOperationException if this
     *          method is not supported by this {@code ColorModel}
     */
    public int[] getComponents(int pixel, int[] components, int offset) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model.");
    }

    /**
     * Returns an array of unnormalized color/alpha components given a pixel
     * in this {@code ColorModel}.  The pixel value is specified by
     * an array of data elements of type transferType passed in as an
     * object reference.  If {@code pixel} is not a primitive array
     * of type transferType, a {@code ClassCastException} is thrown.
     * An {@code IllegalArgumentException} will be thrown if color
     * component values for this {@code ColorModel} are not
     * conveniently representable in the unnormalized form.
     * An {@code ArrayIndexOutOfBoundsException} is
     * thrown if {@code pixel} is not large enough to hold a pixel
     * value for this {@code ColorModel}.
     * This method can be used to retrieve the components for a specific
     * pixel value in any {@code ColorModel}.  If the components
     * array is {@code null}, a new array will be allocated.  The
     * components array will be returned.  Color/alpha components are
     * stored in the {@code components} array starting at
     * {@code offset} (even if the array is allocated by this
     * method).  An {@code ArrayIndexOutOfBoundsException}
     * is thrown if  the components array is not {@code null} and is
     * not large enough to hold all the color and alpha components
     * (starting at {@code offset}).
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param pixel the specified pixel
     * @param components an array that receives the color and alpha
     * components of the specified pixel
     * @param offset the index into the {@code components} array at
     * which to begin storing the color and alpha components of the
     * specified pixel
     * @return an array containing the color and alpha components of the
     * specified pixel starting at the specified offset.
     * @throws UnsupportedOperationException if this
     *          method is not supported by this {@code ColorModel}
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model.");
    }

    /**
     * Returns an array of all of the color/alpha components in unnormalized
     * form, given a normalized component array.  Unnormalized components
     * are unsigned integral values between 0 and 2<sup>n</sup> - 1, where
     * n is the number of bits for a particular component.  Normalized
     * components are float values between a per component minimum and
     * maximum specified by the {@code ColorSpace} object for this
     * {@code ColorModel}.  An {@code IllegalArgumentException}
     * will be thrown if color component values for this
     * {@code ColorModel} are not conveniently representable in the
     * unnormalized form.  If the
     * {@code components} array is {@code null}, a new array
     * will be allocated.  The {@code components} array will
     * be returned.  Color/alpha components are stored in the
     * {@code components} array starting at {@code offset} (even
     * if the array is allocated by this method). An
     * {@code ArrayIndexOutOfBoundsException} is thrown if the
     * {@code components} array is not {@code null} and is not
     * large enough to hold all the color and alpha
     * components (starting at {@code offset}).  An
     * {@code IllegalArgumentException} is thrown if the
     * {@code normComponents} array is not large enough to hold
     * all the color and alpha components starting at
     * {@code normOffset}.
     * @param normComponents an array containing normalized components
     * @param normOffset the offset into the {@code normComponents}
     * array at which to start retrieving normalized components
     * @param components an array that receives the components from
     * {@code normComponents}
     * @param offset the index into {@code components} at which to
     * begin storing normalized components from
     * {@code normComponents}
     * @return an array containing unnormalized color and alpha
     * components.
     * @throws IllegalArgumentException If the component values for this
     * {@code ColorModel} are not conveniently representable in the
     * unnormalized form.
     * @throws IllegalArgumentException if the length of
     *          {@code normComponents} minus {@code normOffset}
     *          is less than {@code numComponents}
     * @throws UnsupportedOperationException if the
     *          constructor of this {@code ColorModel} called the
     *          {@code super(bits)} constructor, but did not
     *          override this method.  See the constructor,
     *          {@link #ColorModel(int)}.
     */
    public int[] getUnnormalizedComponents(float[] normComponents,
                                           int normOffset,
                                           int[] components, int offset) {
        // Make sure that someone isn't using a custom color model
        // that called the super(bits) constructor.
        if (colorSpace == null) {
            throw new UnsupportedOperationException("This method is not supported "+
                                        "by this color model.");
        }

        if (nBits == null) {
            throw new UnsupportedOperationException ("This method is not supported.  "+
                                         "Unable to determine #bits per "+
                                         "component.");
        }
        if ((normComponents.length - normOffset) < numComponents) {
            throw new
                IllegalArgumentException(
                        "Incorrect number of components.  Expecting "+
                        numComponents);
        }

        if (components == null) {
            components = new int[offset+numComponents];
        }

        if (supportsAlpha && isAlphaPremultiplied) {
            float normAlpha = normComponents[normOffset+numColorComponents];
            for (int i=0; i < numColorComponents; i++) {
                components[offset+i] = (int) (normComponents[normOffset+i]
                                              * ((1<<nBits[i]) - 1)
                                              * normAlpha + 0.5f);
            }
            components[offset+numColorComponents] = (int)
                (normAlpha * ((1<<nBits[numColorComponents]) - 1) + 0.5f);
        }
        else {
            for (int i=0; i < numComponents; i++) {
                components[offset+i] = (int) (normComponents[normOffset+i]
                                              * ((1<<nBits[i]) - 1) + 0.5f);
            }
        }

        return components;
    }

    /**
     * Returns an array of all of the color/alpha components in normalized
     * form, given an unnormalized component array.  Unnormalized components
     * are unsigned integral values between 0 and 2<sup>n</sup> - 1, where
     * n is the number of bits for a particular component.  Normalized
     * components are float values between a per component minimum and
     * maximum specified by the {@code ColorSpace} object for this
     * {@code ColorModel}.  An {@code IllegalArgumentException}
     * will be thrown if color component values for this
     * {@code ColorModel} are not conveniently representable in the
     * unnormalized form.  If the
     * {@code normComponents} array is {@code null}, a new array
     * will be allocated.  The {@code normComponents} array
     * will be returned.  Color/alpha components are stored in the
     * {@code normComponents} array starting at
     * {@code normOffset} (even if the array is allocated by this
     * method).  An {@code ArrayIndexOutOfBoundsException} is thrown
     * if the {@code normComponents} array is not {@code null}
     * and is not large enough to hold all the color and alpha components
     * (starting at {@code normOffset}).  An
     * {@code IllegalArgumentException} is thrown if the
     * {@code components} array is not large enough to hold all the
     * color and alpha components starting at {@code offset}.
     * <p>
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  The default implementation
     * of this method in this abstract class assumes that component values
     * for this class are conveniently representable in the unnormalized
     * form.  Therefore, subclasses which may
     * have instances which do not support the unnormalized form must
     * override this method.
     * @param components an array containing unnormalized components
     * @param offset the offset into the {@code components} array at
     * which to start retrieving unnormalized components
     * @param normComponents an array that receives the normalized components
     * @param normOffset the index into {@code normComponents} at
     * which to begin storing normalized components
     * @return an array containing normalized color and alpha
     * components.
     * @throws IllegalArgumentException If the component values for this
     * {@code ColorModel} are not conveniently representable in the
     * unnormalized form.
     * @throws UnsupportedOperationException if the
     *          constructor of this {@code ColorModel} called the
     *          {@code super(bits)} constructor, but did not
     *          override this method.  See the constructor,
     *          {@link #ColorModel(int)}.
     * @throws UnsupportedOperationException if this method is unable
     *          to determine the number of bits per component
     */
    public float[] getNormalizedComponents(int[] components, int offset,
                                           float[] normComponents,
                                           int normOffset) {
        // Make sure that someone isn't using a custom color model
        // that called the super(bits) constructor.
        if (colorSpace == null) {
            throw new UnsupportedOperationException("This method is not supported by "+
                                        "this color model.");
        }
        if (nBits == null) {
            throw new UnsupportedOperationException ("This method is not supported.  "+
                                         "Unable to determine #bits per "+
                                         "component.");
        }

        if ((components.length - offset) < numComponents) {
            throw new
                IllegalArgumentException(
                        "Incorrect number of components.  Expecting "+
                        numComponents);
        }

        if (normComponents == null) {
            normComponents = new float[numComponents+normOffset];
        }

        if (supportsAlpha && isAlphaPremultiplied) {
            // Normalized coordinates are non premultiplied
            float normAlpha = (float)components[offset+numColorComponents];
            normAlpha /= (float) ((1<<nBits[numColorComponents]) - 1);
            if (normAlpha != 0.0f) {
                for (int i=0; i < numColorComponents; i++) {
                    normComponents[normOffset+i] =
                        ((float) components[offset+i]) /
                        (normAlpha * ((float) ((1<<nBits[i]) - 1)));
                }
            } else {
                for (int i=0; i < numColorComponents; i++) {
                    normComponents[normOffset+i] = 0.0f;
                }
            }
            normComponents[normOffset+numColorComponents] = normAlpha;
        }
        else {
            for (int i=0; i < numComponents; i++) {
                normComponents[normOffset+i] = ((float) components[offset+i]) /
                                               ((float) ((1<<nBits[i]) - 1));
            }
        }

        return normComponents;
    }

    /**
     * Returns a pixel value represented as an {@code int} in this
     * {@code ColorModel}, given an array of unnormalized color/alpha
     * components.  This method will throw an
     * {@code IllegalArgumentException} if component values for this
     * {@code ColorModel} are not conveniently representable as a
     * single {@code int} or if color component values for this
     * {@code ColorModel} are not conveniently representable in the
     * unnormalized form.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if  the
     * {@code components} array is not large enough to hold all the
     * color and alpha components (starting at {@code offset}).
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param components an array of unnormalized color and alpha
     * components
     * @param offset the index into {@code components} at which to
     * begin retrieving the color and alpha components
     * @return an {@code int} pixel value in this
     * {@code ColorModel} corresponding to the specified components.
     * @throws IllegalArgumentException if
     *  pixel values for this {@code ColorModel} are not
     *  conveniently representable as a single {@code int}
     * @throws IllegalArgumentException if
     *  component values for this {@code ColorModel} are not
     *  conveniently representable in the unnormalized form
     * @throws ArrayIndexOutOfBoundsException if
     *  the {@code components} array is not large enough to
     *  hold all of the color and alpha components starting at
     *  {@code offset}
     * @throws UnsupportedOperationException if this
     *  method is not supported by this {@code ColorModel}
     */
    public int getDataElement(int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported "+
                                    "by this color model.");
    }

    /**
     * Returns a data element array representation of a pixel in this
     * {@code ColorModel}, given an array of unnormalized color/alpha
     * components.  This array can then be passed to the
     * {@code setDataElements} method of a {@code WritableRaster}
     * object.  This method will throw an {@code IllegalArgumentException}
     * if color component values for this {@code ColorModel} are not
     * conveniently representable in the unnormalized form.
     * An {@code ArrayIndexOutOfBoundsException} is thrown
     * if the {@code components} array is not large enough to hold
     * all the color and alpha components (starting at
     * {@code offset}).  If the {@code obj} variable is
     * {@code null}, a new array will be allocated.  If
     * {@code obj} is not {@code null}, it must be a primitive
     * array of type transferType; otherwise, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code obj} is not large enough to hold a pixel value for this
     * {@code ColorModel}.
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param components an array of unnormalized color and alpha
     * components
     * @param offset the index into {@code components} at which to
     * begin retrieving color and alpha components
     * @param obj the {@code Object} representing an array of color
     * and alpha components
     * @return an {@code Object} representing an array of color and
     * alpha components.
     * @throws ClassCastException if {@code obj}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code obj} is not large enough to hold a pixel value
     *  for this {@code ColorModel} or the {@code components}
     *  array is not large enough to hold all of the color and alpha
     *  components starting at {@code offset}
     * @throws IllegalArgumentException if
     *  component values for this {@code ColorModel} are not
     *  conveniently representable in the unnormalized form
     * @throws UnsupportedOperationException if this
     *  method is not supported by this {@code ColorModel}
     * @see WritableRaster#setDataElements
     * @see SampleModel#setDataElements
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        throw new UnsupportedOperationException("This method has not been implemented "+
                                    "for this color model.");
    }

    /**
     * Returns a pixel value represented as an {@code int} in this
     * {@code ColorModel}, given an array of normalized color/alpha
     * components.  This method will throw an
     * {@code IllegalArgumentException} if pixel values for this
     * {@code ColorModel} are not conveniently representable as a
     * single {@code int}.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if  the
     * {@code normComponents} array is not large enough to hold all the
     * color and alpha components (starting at {@code normOffset}).
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  The default implementation
     * of this method in this abstract class first converts from the
     * normalized form to the unnormalized form and then calls
     * {@code getDataElement(int[], int)}.  Subclasses which may
     * have instances which do not support the unnormalized form must
     * override this method.
     * @param normComponents an array of normalized color and alpha
     * components
     * @param normOffset the index into {@code normComponents} at which to
     * begin retrieving the color and alpha components
     * @return an {@code int} pixel value in this
     * {@code ColorModel} corresponding to the specified components.
     * @throws IllegalArgumentException if
     *  pixel values for this {@code ColorModel} are not
     *  conveniently representable as a single {@code int}
     * @throws ArrayIndexOutOfBoundsException if
     *  the {@code normComponents} array is not large enough to
     *  hold all of the color and alpha components starting at
     *  {@code normOffset}
     * @since 1.4
     */
    public int getDataElement(float[] normComponents, int normOffset) {
        int[] components = getUnnormalizedComponents(normComponents,
                                                     normOffset, null, 0);
        return getDataElement(components, 0);
    }

    /**
     * Returns a data element array representation of a pixel in this
     * {@code ColorModel}, given an array of normalized color/alpha
     * components.  This array can then be passed to the
     * {@code setDataElements} method of a {@code WritableRaster}
     * object.  An {@code ArrayIndexOutOfBoundsException} is thrown
     * if the {@code normComponents} array is not large enough to hold
     * all the color and alpha components (starting at
     * {@code normOffset}).  If the {@code obj} variable is
     * {@code null}, a new array will be allocated.  If
     * {@code obj} is not {@code null}, it must be a primitive
     * array of type transferType; otherwise, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code obj} is not large enough to hold a pixel value for this
     * {@code ColorModel}.
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  The default implementation
     * of this method in this abstract class first converts from the
     * normalized form to the unnormalized form and then calls
     * {@code getDataElement(int[], int, Object)}.  Subclasses which may
     * have instances which do not support the unnormalized form must
     * override this method.
     * @param normComponents an array of normalized color and alpha
     * components
     * @param normOffset the index into {@code normComponents} at which to
     * begin retrieving color and alpha components
     * @param obj a primitive data array to hold the returned pixel
     * @return an {@code Object} which is a primitive data array
     * representation of a pixel
     * @throws ClassCastException if {@code obj}
     *  is not a primitive array of type {@code transferType}
     * @throws ArrayIndexOutOfBoundsException if
     *  {@code obj} is not large enough to hold a pixel value
     *  for this {@code ColorModel} or the {@code normComponents}
     *  array is not large enough to hold all of the color and alpha
     *  components starting at {@code normOffset}
     * @see WritableRaster#setDataElements
     * @see SampleModel#setDataElements
     * @since 1.4
     */
    public Object getDataElements(float[] normComponents, int normOffset,
                                  Object obj) {
        int[] components = getUnnormalizedComponents(normComponents,
                                                     normOffset, null, 0);
        return getDataElements(components, 0, obj);
    }

    /**
     * Returns an array of all of the color/alpha components in normalized
     * form, given a pixel in this {@code ColorModel}.  The pixel
     * value is specified by an array of data elements of type transferType
     * passed in as an object reference.  If pixel is not a primitive array
     * of type transferType, a {@code ClassCastException} is thrown.
     * An {@code ArrayIndexOutOfBoundsException} is thrown if
     * {@code pixel} is not large enough to hold a pixel value for this
     * {@code ColorModel}.
     * Normalized components are float values between a per component minimum
     * and maximum specified by the {@code ColorSpace} object for this
     * {@code ColorModel}.  If the
     * {@code normComponents} array is {@code null}, a new array
     * will be allocated.  The {@code normComponents} array
     * will be returned.  Color/alpha components are stored in the
     * {@code normComponents} array starting at
     * {@code normOffset} (even if the array is allocated by this
     * method).  An {@code ArrayIndexOutOfBoundsException} is thrown
     * if the {@code normComponents} array is not {@code null}
     * and is not large enough to hold all the color and alpha components
     * (starting at {@code normOffset}).
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  The default implementation
     * of this method in this abstract class first retrieves color and alpha
     * components in the unnormalized form using
     * {@code getComponents(Object, int[], int)} and then calls
     * {@code getNormalizedComponents(int[], int, float[], int)}.
     * Subclasses which may
     * have instances which do not support the unnormalized form must
     * override this method.
     * @param pixel the specified pixel
     * @param normComponents an array to receive the normalized components
     * @param normOffset the offset into the {@code normComponents}
     * array at which to start storing normalized components
     * @return an array containing normalized color and alpha
     * components.
     * @throws ClassCastException if {@code pixel} is not a primitive
     *          array of type transferType
     * @throws ArrayIndexOutOfBoundsException if
     *          {@code normComponents} is not large enough to hold all
     *          color and alpha components starting at {@code normOffset}
     * @throws ArrayIndexOutOfBoundsException if
     *          {@code pixel} is not large enough to hold a pixel
     *          value for this {@code ColorModel}.
     * @throws UnsupportedOperationException if the
     *          constructor of this {@code ColorModel} called the
     *          {@code super(bits)} constructor, but did not
     *          override this method.  See the constructor,
     *          {@link #ColorModel(int)}.
     * @throws UnsupportedOperationException if this method is unable
     *          to determine the number of bits per component
     * @since 1.4
     */
    public float[] getNormalizedComponents(Object pixel,
                                           float[] normComponents,
                                           int normOffset) {
        int[] components = getComponents(pixel, null, 0);
        return getNormalizedComponents(components, 0,
                                       normComponents, normOffset);
    }

    /**
     * This method simply delegates to the default implementation in {@code Object}
     * which is identical to an {@code ==} test since this class cannot enforce the
     * issues of a proper equality test among multiple independent subclass
     * branches.
     * Subclasses are encouraged to override this method and provide equality
     * testing for their own properties in addition to equality tests for the
     * following common base properties of {@code ColorModel}:
     * <ul>
     * <li>Support for alpha component.</li>
     * <li>Is alpha premultiplied.</li>
     * <li>Number of bits per pixel.</li>
     * <li>Type of transparency like Opaque, Bitmask or Translucent.</li>
     * <li>Number of components in a pixel.</li>
     * <li>{@code ColorSpace} type.</li>
     * <li>Type of the array used to represent pixel values.</li>
     * <li>Number of significant bits per color and alpha component.</li>
     * </ul>
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     *         argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * This method simply delegates to the default implementation in {@code Object}
     * which returns the system ID for the class.
     * Subclasses are encouraged to override this method and provide a hash
     * for their own properties in addition to hashing the values of the
     * following common base properties of {@code ColorModel}:
     * <ul>
     * <li>Support for alpha component.</li>
     * <li>Is alpha premultiplied.</li>
     * <li>Number of bits per pixel.</li>
     * <li>Type of transparency like Opaque, Bitmask or Translucent.</li>
     * <li>Number of components in a pixel.</li>
     * <li>{@code ColorSpace} type.</li>
     * <li>Type of the array used to represent pixel values.</li>
     * <li>Number of significant bits per color and alpha component.</li>
     * </ul>
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns the {@code ColorSpace} associated with this
     * {@code ColorModel}.
     * @return the {@code ColorSpace} of this
     * {@code ColorModel}.
     */
    public final ColorSpace getColorSpace() {
        return colorSpace;
    }

    /**
     * Forces the raster data to match the state specified in the
     * {@code isAlphaPremultiplied} variable, assuming the data is
     * currently correctly described by this {@code ColorModel}.  It
     * may multiply or divide the color raster data by alpha, or do
     * nothing if the data is in the correct state.  If the data needs to
     * be coerced, this method will also return an instance of this
     * {@code ColorModel} with the {@code isAlphaPremultiplied}
     * flag set appropriately.  This method will throw a
     * {@code UnsupportedOperationException} if it is not supported
     * by this {@code ColorModel}.
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param raster the {@code WritableRaster} data
     * @param isAlphaPremultiplied {@code true} if the alpha is
     * premultiplied; {@code false} otherwise
     * @return a {@code ColorModel} object that represents the
     * coerced data.
     */
    public ColorModel coerceData (WritableRaster raster,
                                  boolean isAlphaPremultiplied) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }

    /**
      * Returns {@code true} if {@code raster} is compatible
      * with this {@code ColorModel} and {@code false} if it is
      * not.
      * Since {@code ColorModel} is an abstract class,
      * any instance is an instance of a subclass.  Subclasses must
      * override this method since the implementation in this abstract
      * class throws an {@code UnsupportedOperationException}.
      * @param raster the {@link Raster} object to test for compatibility
      * @return {@code true} if {@code raster} is compatible
      * with this {@code ColorModel}.
      * @throws UnsupportedOperationException if this
      *         method has not been implemented for this
      *         {@code ColorModel}
      */
    public boolean isCompatibleRaster(Raster raster) {
        throw new UnsupportedOperationException(
            "This method has not been implemented for this ColorModel.");
    }

    /**
     * Creates a {@code WritableRaster} with the specified width and
     * height that has a data layout ({@code SampleModel}) compatible
     * with this {@code ColorModel}.
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param w the width to apply to the new {@code WritableRaster}
     * @param h the height to apply to the new {@code WritableRaster}
     * @return a {@code WritableRaster} object with the specified
     * width and height.
     * @throws UnsupportedOperationException if this
     *          method is not supported by this {@code ColorModel}
     * @see WritableRaster
     * @see SampleModel
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }

    /**
     * Creates a {@code SampleModel} with the specified width and
     * height that has a data layout compatible with this
     * {@code ColorModel}.
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param w the width to apply to the new {@code SampleModel}
     * @param h the height to apply to the new {@code SampleModel}
     * @return a {@code SampleModel} object with the specified
     * width and height.
     * @throws UnsupportedOperationException if this
     *          method is not supported by this {@code ColorModel}
     * @see SampleModel
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }

    /** Checks if the {@code SampleModel} is compatible with this
     * {@code ColorModel}.
     * Since {@code ColorModel} is an abstract class,
     * any instance is an instance of a subclass.  Subclasses must
     * override this method since the implementation in this abstract
     * class throws an {@code UnsupportedOperationException}.
     * @param sm the specified {@code SampleModel}
     * @return {@code true} if the specified {@code SampleModel}
     * is compatible with this {@code ColorModel}; {@code false}
     * otherwise.
     * @throws UnsupportedOperationException if this
     *          method is not supported by this {@code ColorModel}
     * @see SampleModel
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }

    /**
     * Disposes of system resources associated with this
     * {@code ColorModel} once this {@code ColorModel} is no
     * longer referenced.
     *
     * @deprecated The {@code finalize} method has been deprecated.
     *     Subclasses that override {@code finalize} in order to perform cleanup
     *     should be modified to use alternative cleanup mechanisms and
     *     to remove the overriding {@code finalize} method.
     *     When overriding the {@code finalize} method, its implementation must explicitly
     *     ensure that {@code super.finalize()} is invoked as described in {@link Object#finalize}.
     *     See the specification for {@link Object#finalize()} for further
     *     information about migration options.
     */
    @Deprecated(since = "9", forRemoval = true)
    @SuppressWarnings("removal")
    public void finalize() {
    }


    /**
     * Returns a {@code Raster} representing the alpha channel of an
     * image, extracted from the input {@code Raster}, provided that
     * pixel values of this {@code ColorModel} represent color and
     * alpha information as separate spatial bands (e.g.
     * {@link ComponentColorModel} and {@code DirectColorModel}).
     * This method assumes that {@code Raster} objects associated
     * with such a {@code ColorModel} store the alpha band, if
     * present, as the last band of image data.  Returns {@code null}
     * if there is no separate spatial alpha channel associated with this
     * {@code ColorModel}.  If this is an
     * {@code IndexColorModel} which has alpha in the lookup table,
     * this method will return {@code null} since
     * there is no spatially discrete alpha channel.
     * This method will create a new {@code Raster} (but will share
     * the data array).
     * Since {@code ColorModel} is an abstract class, any instance
     * is an instance of a subclass.  Subclasses must override this
     * method to get any behavior other than returning {@code null}
     * because the implementation in this abstract class returns
     * {@code null}.
     * @param raster the specified {@code Raster}
     * @return a {@code Raster} representing the alpha channel of
     * an image, obtained from the specified {@code Raster}.
     */
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        return null;
    }

    /**
     * Returns the {@code String} representation of the contents of
     * this {@code ColorModel} object.
     * @return a {@code String} representing the contents of this
     * {@code ColorModel} object.
     */
    public String toString() {
       return new String("ColorModel: #pixelBits = "+pixel_bits
                         + " numComponents = "+numComponents
                         + " color space = "+colorSpace
                         + " transparency = "+transparency
                         + " has alpha = "+supportsAlpha
                         + " isAlphaPre = "+isAlphaPremultiplied
                         );
    }

    static int getDefaultTransferType(int pixel_bits) {
        if (pixel_bits <= 8) {
            return DataBuffer.TYPE_BYTE;
        } else if (pixel_bits <= 16) {
            return DataBuffer.TYPE_USHORT;
        } else if (pixel_bits <= 32) {
            return DataBuffer.TYPE_INT;
        } else {
            return DataBuffer.TYPE_UNDEFINED;
        }
    }

    static byte[] l8Tos8 = null;   // 8-bit linear to 8-bit non-linear sRGB LUT
    static byte[] s8Tol8 = null;   // 8-bit non-linear sRGB to 8-bit linear LUT
    static byte[] l16Tos8 = null;  // 16-bit linear to 8-bit non-linear sRGB LUT
    static short[] s8Tol16 = null; // 8-bit non-linear sRGB to 16-bit linear LUT

                                // Maps to hold LUTs for grayscale conversions
    static Map<ICC_ColorSpace, byte[]> g8Tos8Map = null;     // 8-bit gray values to 8-bit sRGB values
    static Map<ICC_ColorSpace, byte[]> lg16Toog8Map = null;  // 16-bit linear to 8-bit "other" gray
    static Map<ICC_ColorSpace, byte[]> g16Tos8Map = null;    // 16-bit gray values to 8-bit sRGB values
    static Map<ICC_ColorSpace, short[]> lg16Toog16Map = null; // 16-bit linear to 16-bit "other" gray

    static boolean isLinearRGBspace(ColorSpace cs) {
        return cs == ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
    }

    static boolean isLinearGRAYspace(ColorSpace cs) {
        return cs == ColorSpace.getInstance(ColorSpace.CS_GRAY);
    }

    static byte[] getLinearRGB8TosRGB8LUT() {
        if (l8Tos8 == null) {
            l8Tos8 = new byte[256];
            float input, output;
            // algorithm for linear RGB to nonlinear sRGB conversion
            // is from the IEC 61966-2-1 International Standard,
            // Colour Management - Default RGB colour space - sRGB,
            // First Edition, 1999-10,
            // available for order at http://www.iec.ch
            for (int i = 0; i <= 255; i++) {
                input = ((float) i) / 255.0f;
                if (input <= 0.0031308f) {
                    output = input * 12.92f;
                } else {
                    output = 1.055f * ((float) Math.pow(input, (1.0 / 2.4)))
                             - 0.055f;
                }
                l8Tos8[i] = (byte) Math.round(output * 255.0f);
            }
        }
        return l8Tos8;
    }

    static byte[] getsRGB8ToLinearRGB8LUT() {
        if (s8Tol8 == null) {
            s8Tol8 = new byte[256];
            float input, output;
            // algorithm from IEC 61966-2-1 International Standard
            for (int i = 0; i <= 255; i++) {
                input = ((float) i) / 255.0f;
                if (input <= 0.04045f) {
                    output = input / 12.92f;
                } else {
                    output = (float) Math.pow((input + 0.055f) / 1.055f, 2.4);
                }
                s8Tol8[i] = (byte) Math.round(output * 255.0f);
            }
        }
        return s8Tol8;
    }

    static byte[] getLinearRGB16TosRGB8LUT() {
        if (l16Tos8 == null) {
            l16Tos8 = new byte[65536];
            float input, output;
            // algorithm from IEC 61966-2-1 International Standard
            for (int i = 0; i <= 65535; i++) {
                input = ((float) i) / 65535.0f;
                if (input <= 0.0031308f) {
                    output = input * 12.92f;
                } else {
                    output = 1.055f * ((float) Math.pow(input, (1.0 / 2.4)))
                             - 0.055f;
                }
                l16Tos8[i] = (byte) Math.round(output * 255.0f);
            }
        }
        return l16Tos8;
    }

    static short[] getsRGB8ToLinearRGB16LUT() {
        if (s8Tol16 == null) {
            s8Tol16 = new short[256];
            float input, output;
            // algorithm from IEC 61966-2-1 International Standard
            for (int i = 0; i <= 255; i++) {
                input = ((float) i) / 255.0f;
                if (input <= 0.04045f) {
                    output = input / 12.92f;
                } else {
                    output = (float) Math.pow((input + 0.055f) / 1.055f, 2.4);
                }
                s8Tol16[i] = (short) Math.round(output * 65535.0f);
            }
        }
        return s8Tol16;
    }

    /*
     * Return a byte LUT that converts 8-bit gray values in the grayCS
     * ColorSpace to the appropriate 8-bit sRGB value.  I.e., if lut
     * is the byte array returned by this method and sval = lut[gval],
     * then the sRGB triple (sval,sval,sval) is the best match to gval.
     * Cache references to any computed LUT in a Map.
     */
    static byte[] getGray8TosRGB8LUT(ICC_ColorSpace grayCS) {
        if (isLinearGRAYspace(grayCS)) {
            return getLinearRGB8TosRGB8LUT();
        }
        if (g8Tos8Map != null) {
            byte[] g8Tos8LUT = g8Tos8Map.get(grayCS);
            if (g8Tos8LUT != null) {
                return g8Tos8LUT;
            }
        }
        byte[] g8Tos8LUT = new byte[256];
        for (int i = 0; i <= 255; i++) {
            g8Tos8LUT[i] = (byte) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace srgbCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        transformList[0] = mdl.createTransform(
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform(
            srgbCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(transformList);
        byte[] tmp = t.colorConvert(g8Tos8LUT, null);
        for (int i = 0, j= 2; i <= 255; i++, j += 3) {
            // All three components of tmp should be equal, since
            // the input color space to colorConvert is a gray scale
            // space.  However, there are slight anomalies in the results.
            // Copy tmp starting at index 2, since colorConvert seems
            // to be slightly more accurate for the third component!
            g8Tos8LUT[i] = tmp[j];
        }
        if (g8Tos8Map == null) {
            g8Tos8Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, byte[]>(2));
        }
        g8Tos8Map.put(grayCS, g8Tos8LUT);
        return g8Tos8LUT;
    }

    /*
     * Return a byte LUT that converts 16-bit gray values in the CS_GRAY
     * linear gray ColorSpace to the appropriate 8-bit value in the
     * grayCS ColorSpace.  Cache references to any computed LUT in a Map.
     */
    static byte[] getLinearGray16ToOtherGray8LUT(ICC_ColorSpace grayCS) {
        if (lg16Toog8Map != null) {
            byte[] lg16Toog8LUT = lg16Toog8Map.get(grayCS);
            if (lg16Toog8LUT != null) {
                return lg16Toog8LUT;
            }
        }
        short[] tmp = new short[65536];
        for (int i = 0; i <= 65535; i++) {
            tmp[i] = (short) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace lgCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        transformList[0] = mdl.createTransform (
            lgCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform (
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(transformList);
        tmp = t.colorConvert(tmp, null);
        byte[] lg16Toog8LUT = new byte[65536];
        for (int i = 0; i <= 65535; i++) {
            // scale unsigned short (0 - 65535) to unsigned byte (0 - 255)
            lg16Toog8LUT[i] =
                (byte) (((float) (tmp[i] & 0xffff)) * (1.0f /257.0f) + 0.5f);
        }
        if (lg16Toog8Map == null) {
            lg16Toog8Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, byte[]>(2));
        }
        lg16Toog8Map.put(grayCS, lg16Toog8LUT);
        return lg16Toog8LUT;
    }

    /*
     * Return a byte LUT that converts 16-bit gray values in the grayCS
     * ColorSpace to the appropriate 8-bit sRGB value.  I.e., if lut
     * is the byte array returned by this method and sval = lut[gval],
     * then the sRGB triple (sval,sval,sval) is the best match to gval.
     * Cache references to any computed LUT in a Map.
     */
    static byte[] getGray16TosRGB8LUT(ICC_ColorSpace grayCS) {
        if (isLinearGRAYspace(grayCS)) {
            return getLinearRGB16TosRGB8LUT();
        }
        if (g16Tos8Map != null) {
            byte[] g16Tos8LUT = g16Tos8Map.get(grayCS);
            if (g16Tos8LUT != null) {
                return g16Tos8LUT;
            }
        }
        short[] tmp = new short[65536];
        for (int i = 0; i <= 65535; i++) {
            tmp[i] = (short) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace srgbCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        transformList[0] = mdl.createTransform (
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform (
            srgbCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(transformList);
        tmp = t.colorConvert(tmp, null);
        byte[] g16Tos8LUT = new byte[65536];
        for (int i = 0, j= 2; i <= 65535; i++, j += 3) {
            // All three components of tmp should be equal, since
            // the input color space to colorConvert is a gray scale
            // space.  However, there are slight anomalies in the results.
            // Copy tmp starting at index 2, since colorConvert seems
            // to be slightly more accurate for the third component!

            // scale unsigned short (0 - 65535) to unsigned byte (0 - 255)
            g16Tos8LUT[i] =
                (byte) (((float) (tmp[j] & 0xffff)) * (1.0f /257.0f) + 0.5f);
        }
        if (g16Tos8Map == null) {
            g16Tos8Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, byte[]>(2));
        }
        g16Tos8Map.put(grayCS, g16Tos8LUT);
        return g16Tos8LUT;
    }

    /*
     * Return a short LUT that converts 16-bit gray values in the CS_GRAY
     * linear gray ColorSpace to the appropriate 16-bit value in the
     * grayCS ColorSpace.  Cache references to any computed LUT in a Map.
     */
    static short[] getLinearGray16ToOtherGray16LUT(ICC_ColorSpace grayCS) {
        if (lg16Toog16Map != null) {
            short[] lg16Toog16LUT = lg16Toog16Map.get(grayCS);
            if (lg16Toog16LUT != null) {
                return lg16Toog16LUT;
            }
        }
        short[] tmp = new short[65536];
        for (int i = 0; i <= 65535; i++) {
            tmp[i] = (short) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace lgCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        transformList[0] = mdl.createTransform (
            lgCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform(
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(
            transformList);
        short[] lg16Toog16LUT = t.colorConvert(tmp, null);
        if (lg16Toog16Map == null) {
            lg16Toog16Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, short[]>(2));
        }
        lg16Toog16Map.put(grayCS, lg16Toog16LUT);
        return lg16Toog16LUT;
    }

}
