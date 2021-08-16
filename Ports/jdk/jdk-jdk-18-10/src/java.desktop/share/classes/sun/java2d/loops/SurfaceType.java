/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

import java.awt.image.ColorModel;
import sun.awt.image.PixelConverter;
import java.util.HashMap;

/**
 * A SurfaceType object provides a chained description of a type of
 * drawing surface.  The object will provide a single String constant
 * descriptor which is one way of viewing or accessing a particular
 * drawing surface as well as a pointer to another SurfaceType which
 * describes the same drawing surface in a different (typically more
 * generalized) way.
 * <p>
 * A more specific description of a surface is considered a "subtype"
 * and a more general description is considered a "supertype".  Thus,
 * the deriveSubType method provides a way to create a new SurfaceType
 * that is related to but more specific than an existing SurfaceType and
 * the getSuperType method provides a way to ask a given SurfaceType
 * for a more general way to describe the same surface.
 * <p>
 * Note that you cannot construct a brand new root for a chain since
 * the constructor is private.  Every chain of types must at some point
 * derive from the Any node provided here using the deriveSubType()
 * method.  The presence of this common Any node on every chain
 * ensures that all chains end with the DESC_ANY descriptor so that
 * a suitable General GraphicsPrimitive object can be obtained for
 * the indicated surface if all of the more specific searches fail.
 */
public final class SurfaceType {

    private static int unusedUID = 1;
    private static HashMap<String, Integer> surfaceUIDMap = new HashMap<>(100);

    /*
     * CONSTANTS USED BY ALL PRIMITIVES TO DESCRIBE THE SURFACES
     * THEY CAN OPERATE ON
     */

    /**
     * surface is unknown color model or sample model.
     */
    public static final String
        DESC_ANY            = "Any Surface";

    /**
     * common surface formats defined in BufferedImage
     */
    public static final String
        DESC_INT_RGB        = "Integer RGB";
    public static final String
        DESC_INT_ARGB       = "Integer ARGB";
    public static final String
        DESC_INT_ARGB_PRE   = "Integer ARGB Premultiplied";
    public static final String
        DESC_INT_BGR        = "Integer BGR";
    public static final String
        DESC_3BYTE_BGR      = "3 Byte BGR";
    public static final String
        DESC_4BYTE_ABGR     = "4 Byte ABGR";
    public static final String
        DESC_4BYTE_ABGR_PRE = "4 Byte ABGR Premultiplied";
    public static final String
        DESC_USHORT_565_RGB = "Short 565 RGB";
    public static final String
        DESC_USHORT_555_RGB = "Short 555 RGB";
    public static final String
        DESC_USHORT_555_RGBx= "Short 555 RGBx";
    public static final String
        DESC_USHORT_4444_ARGB= "Short 4444 ARGB";
    public static final String
        DESC_BYTE_GRAY      = "8-bit Gray";
    public static final String
        DESC_USHORT_INDEXED = "16-bit Indexed";
    public static final String
        DESC_USHORT_GRAY    = "16-bit Gray";
    public static final String
        DESC_BYTE_BINARY    = "Packed Binary Bitmap";
    public static final String
        DESC_BYTE_INDEXED   = "8-bit Indexed";

    /**
     * wildcard format which indicates that the GraphicsPrimitive
     * is independent of the color model on an IntegerComponent
     * sample model surface
     */
    public static final String DESC_ANY_INT = "Any Discrete Integer";

    /**
     * wildcard format which indicates that the GraphicsPrimitive
     * is independent of the color model on a ShortComponent
     * sample model surface
     */
    public static final String DESC_ANY_SHORT = "Any Discrete Short";

    /**
     * wildcard format which indicates that the GraphicsPrimitive
     * is independent of the color model on a ByteComponent
     * sample model surface
     */
    public static final String DESC_ANY_BYTE = "Any Discrete Byte";

    /**
     * wildcard format which indicates that the GraphicsPrimitive
     * operates on a surface with 3 component interleaved Raster and
     * sample model and a ComponentColorModel with an arbitrary ordering
     * of the RGB channels
     */
    public static final String DESC_ANY_3BYTE = "Any 3 Byte Component";

    /**
     * wildcard format which indicates that the GraphicsPrimitive
     * operates on a surface with 4 component interleaved Raster and
     * sample model and a ComponentColorModel with an arbitrary ordering
     * of the ARGB channels
     */
    public static final String DESC_ANY_4BYTE = "Any 4 Byte Component";

    /**
     * wildcard format which indicates that the GraphicsPrimitive
     * operates on a surface with a single component IntegerComponent
     * sample model and a DirectColorModel with an arbitrary ordering
     * of the RGB channels
     */
    public static final String DESC_ANY_INT_DCM = "Any Integer DCM";

    /**
     * additional IntegerComponent types common on Windows
     */
    public static final String DESC_INT_RGBx = "Integer RGBx";
    public static final String DESC_INT_BGRx = "Integer BGRx";

    /**
     * additional 3 byte format common on Windows
     */
    public static final String DESC_3BYTE_RGB = "3 Byte RGB";

    /**
     * common formats for BITMASK transparency.
     */
    public static final String DESC_INT_ARGB_BM     = "Int ARGB (Bitmask)";
    public static final String DESC_BYTE_INDEXED_BM = "8-bit Indexed (Bitmask)";

    /**
     * Opaque 8-bit indexed images
     */
    public static final String
        DESC_BYTE_INDEXED_OPAQUE = "8-bit Indexed (Opaque)";

    /**
     * Special Gray Scale types for rendering loops.  Really indexed
     * types, but colormap has all gray values.
     */
    public static final String DESC_INDEX8_GRAY  = "8-bit Palettized Gray";
    public static final String DESC_INDEX12_GRAY = "12-bit Palettized Gray";

    public static final String
        DESC_BYTE_BINARY_1BIT = "Packed Binary 1-bit Bitmap";
    public static final String
        DESC_BYTE_BINARY_2BIT = "Packed Binary 2-bit Bitmap";
    public static final String
        DESC_BYTE_BINARY_4BIT = "Packed Binary 4-bit Bitmap";

    /**
     * Special type for describing the sources of loops that render the
     * current foreground color or paint instead of copying colors from
     * a source surface.
     */
    public static final String DESC_ANY_PAINT      = "Paint Object";
    public static final String DESC_ANY_COLOR      = "Single Color";
    public static final String DESC_OPAQUE_COLOR   = "Opaque Color";
    public static final String
        DESC_GRADIENT_PAINT        = "Gradient Paint";
    public static final String
        DESC_OPAQUE_GRADIENT_PAINT = "Opaque Gradient Paint";
    public static final String
        DESC_TEXTURE_PAINT         = "Texture Paint";
    public static final String
        DESC_OPAQUE_TEXTURE_PAINT  = "Opaque Texture Paint";
    public static final String
        DESC_LINEAR_GRADIENT_PAINT        = "Linear Gradient Paint";
    public static final String
        DESC_OPAQUE_LINEAR_GRADIENT_PAINT = "Opaque Linear Gradient Paint";
    public static final String
        DESC_RADIAL_GRADIENT_PAINT        = "Radial Gradient Paint";
    public static final String
        DESC_OPAQUE_RADIAL_GRADIENT_PAINT = "Opaque Radial Gradient Paint";

    /*
     * END OF SURFACE TYPE CONSTANTS
     */


    /**
     * The root SurfaceType object for all chains of surface descriptions.
     * The root uses the default PixelConverter object, which uses a given
     * ColorModel object to calculate its pixelFor() values when asked.
     * Any SurfaceType objects that are not created with a specific
     * PixelConverter object will inherit this behavior from the root.
     */
    public static final SurfaceType Any =
        new SurfaceType(null, DESC_ANY, PixelConverter.instance);

    /*
     * START OF SurfaceType OBJECTS FOR THE VARIOUS CONSTANTS
     */

    public static final SurfaceType
        AnyInt            = Any.deriveSubType(DESC_ANY_INT);
    public static final SurfaceType
        AnyShort          = Any.deriveSubType(DESC_ANY_SHORT);
    public static final SurfaceType
        AnyByte           = Any.deriveSubType(DESC_ANY_BYTE);
    public static final SurfaceType
        AnyByteBinary     = Any.deriveSubType(DESC_BYTE_BINARY);
    public static final SurfaceType
        Any3Byte          = Any.deriveSubType(DESC_ANY_3BYTE);
    public static final SurfaceType
        Any4Byte          = Any.deriveSubType(DESC_ANY_4BYTE);
    public static final SurfaceType
        AnyDcm            = AnyInt.deriveSubType(DESC_ANY_INT_DCM);

    public static final SurfaceType
        Custom            = Any;
    public static final SurfaceType IntRgb =
        AnyDcm.deriveSubType(DESC_INT_RGB, PixelConverter.Xrgb.instance);

    public static final SurfaceType IntArgb =
        AnyDcm.deriveSubType(DESC_INT_ARGB, PixelConverter.Argb.instance);

    public static final SurfaceType IntArgbPre =
        AnyDcm.deriveSubType(DESC_INT_ARGB_PRE,
                             PixelConverter.ArgbPre.instance);

    public static final SurfaceType IntBgr =
        AnyDcm.deriveSubType(DESC_INT_BGR, PixelConverter.Xbgr.instance);

    public static final SurfaceType ThreeByteBgr =
        Any3Byte.deriveSubType(DESC_3BYTE_BGR, PixelConverter.Xrgb.instance);

    public static final SurfaceType FourByteAbgr =
        Any4Byte.deriveSubType(DESC_4BYTE_ABGR, PixelConverter.Rgba.instance);

    public static final SurfaceType FourByteAbgrPre =
        Any4Byte.deriveSubType(DESC_4BYTE_ABGR_PRE,
                               PixelConverter.RgbaPre.instance);

    public static final SurfaceType Ushort565Rgb =
        AnyShort.deriveSubType(DESC_USHORT_565_RGB,
                               PixelConverter.Ushort565Rgb.instance);

    public static final SurfaceType Ushort555Rgb =
        AnyShort.deriveSubType(DESC_USHORT_555_RGB,
                               PixelConverter.Ushort555Rgb.instance);

    public static final SurfaceType Ushort555Rgbx =
        AnyShort.deriveSubType(DESC_USHORT_555_RGBx,
                               PixelConverter.Ushort555Rgbx.instance);

    public static final SurfaceType Ushort4444Argb =
        AnyShort.deriveSubType(DESC_USHORT_4444_ARGB,
                               PixelConverter.Ushort4444Argb.instance);

    public static final SurfaceType UshortIndexed =
        AnyShort.deriveSubType(DESC_USHORT_INDEXED);

    public static final SurfaceType ByteGray =
        AnyByte.deriveSubType(DESC_BYTE_GRAY,
                              PixelConverter.ByteGray.instance);

    public static final SurfaceType UshortGray =
        AnyShort.deriveSubType(DESC_USHORT_GRAY,
                               PixelConverter.UshortGray.instance);

    public static final SurfaceType ByteBinary1Bit =
        AnyByteBinary.deriveSubType(DESC_BYTE_BINARY_1BIT);
    public static final SurfaceType ByteBinary2Bit =
        AnyByteBinary.deriveSubType(DESC_BYTE_BINARY_2BIT);
    public static final SurfaceType ByteBinary4Bit =
        AnyByteBinary.deriveSubType(DESC_BYTE_BINARY_4BIT);

    public static final SurfaceType ByteIndexed =
        AnyByte.deriveSubType(DESC_BYTE_INDEXED);

    public static final SurfaceType IntRgbx =
        AnyDcm.deriveSubType(DESC_INT_RGBx, PixelConverter.Rgbx.instance);

    public static final SurfaceType IntBgrx =
        AnyDcm.deriveSubType(DESC_INT_BGRx, PixelConverter.Bgrx.instance);

    public static final SurfaceType ThreeByteRgb =
        Any3Byte.deriveSubType(DESC_3BYTE_RGB, PixelConverter.Xbgr.instance);

    public static final SurfaceType IntArgbBm =
        AnyDcm.deriveSubType(DESC_INT_ARGB_BM, PixelConverter.ArgbBm.instance);

    public static final SurfaceType ByteIndexedBm =
        ByteIndexed.deriveSubType(DESC_BYTE_INDEXED_BM);

    public static final SurfaceType ByteIndexedOpaque =
        ByteIndexedBm.deriveSubType(DESC_BYTE_INDEXED_OPAQUE);

    public static final SurfaceType Index8Gray =
        ByteIndexedOpaque.deriveSubType(DESC_INDEX8_GRAY);

    public static final SurfaceType Index12Gray =
        Any.deriveSubType(DESC_INDEX12_GRAY);

    public static final SurfaceType AnyPaint =
        Any.deriveSubType(DESC_ANY_PAINT);

    public static final SurfaceType AnyColor =
        AnyPaint.deriveSubType(DESC_ANY_COLOR);

    public static final SurfaceType OpaqueColor =
        AnyColor.deriveSubType(DESC_OPAQUE_COLOR);

    public static final SurfaceType GradientPaint =
        AnyPaint.deriveSubType(DESC_GRADIENT_PAINT);
    public static final SurfaceType OpaqueGradientPaint =
        GradientPaint.deriveSubType(DESC_OPAQUE_GRADIENT_PAINT);

    public static final SurfaceType LinearGradientPaint =
        AnyPaint.deriveSubType(DESC_LINEAR_GRADIENT_PAINT);
    public static final SurfaceType OpaqueLinearGradientPaint =
        LinearGradientPaint.deriveSubType(DESC_OPAQUE_LINEAR_GRADIENT_PAINT);

    public static final SurfaceType RadialGradientPaint =
        AnyPaint.deriveSubType(DESC_RADIAL_GRADIENT_PAINT);
    public static final SurfaceType OpaqueRadialGradientPaint =
        RadialGradientPaint.deriveSubType(DESC_OPAQUE_RADIAL_GRADIENT_PAINT);

    public static final SurfaceType TexturePaint =
        AnyPaint.deriveSubType(DESC_TEXTURE_PAINT);
    public static final SurfaceType OpaqueTexturePaint =
        TexturePaint.deriveSubType(DESC_OPAQUE_TEXTURE_PAINT);

    /*
     * END OF SurfaceType OBJECTS FOR THE VARIOUS CONSTANTS
     */

    /**
     * Return a new SurfaceType object which uses this object as its
     * more general "supertype" descriptor.  If no operation can be
     * found that manipulates the type of surface described more exactly
     * by desc, then this object will define the more relaxed specification
     * of the surface that can be used to find a more general operator.
     */
    public SurfaceType deriveSubType(String desc) {
        return new SurfaceType(this, desc);
    }

    public SurfaceType deriveSubType(String desc,
                                     PixelConverter pixelConverter) {
        return new SurfaceType(this, desc, pixelConverter);
    }

    private int uniqueID;
    private String desc;
    private SurfaceType next;
    protected PixelConverter pixelConverter;

    private SurfaceType(SurfaceType parent, String desc,
                        PixelConverter pixelConverter) {
        next = parent;
        this.desc = desc;
        this.uniqueID = makeUniqueID(desc);
        this.pixelConverter = pixelConverter;
    }

    private SurfaceType(SurfaceType parent, String desc) {
        next = parent;
        this.desc = desc;
        this.uniqueID = makeUniqueID(desc);
        this.pixelConverter = parent.pixelConverter;
    }

    public static synchronized int makeUniqueID(String desc) {
        Integer i = surfaceUIDMap.get(desc);

        if (i == null) {
            if (unusedUID > 255) {
                throw new InternalError("surface type id overflow");
            }
            i = Integer.valueOf(unusedUID++);
            surfaceUIDMap.put(desc, i);
        }
        return i.intValue();
    }

    public int getUniqueID() {
        return uniqueID;
    }

    public String getDescriptor() {
        return desc;
    }

    public SurfaceType getSuperType() {
        return next;
    }

    public PixelConverter getPixelConverter() {
        return pixelConverter;
    }

    public int pixelFor(int rgb, ColorModel cm) {
        return pixelConverter.rgbToPixel(rgb, cm);
    }

    public int rgbFor(int pixel, ColorModel cm) {
        return pixelConverter.pixelToRgb(pixel, cm);
    }

    public int getAlphaMask() {
        return pixelConverter.getAlphaMask();
    }

    public int hashCode() {
        return desc.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof SurfaceType) {
            return (((SurfaceType) o).uniqueID == this.uniqueID);
        }
        return false;
    }

    public String toString() {
        return desc;
    }

}
