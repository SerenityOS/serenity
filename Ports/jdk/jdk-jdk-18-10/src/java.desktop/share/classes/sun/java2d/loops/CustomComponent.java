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

/*
 * @author Charlton Innovations, Inc.
 * @author Jim Graham
 */

package sun.java2d.loops;

import java.awt.Composite;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import sun.awt.image.IntegerComponentRaster;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.SpanIterator;

/**
 *   CustomComponent, collection of GraphicsPrimitive
 *   Basically, this collection of components performs conversion from
 *   ANY to ANY via opaque copy
 */
public final class CustomComponent {
    public static void register() {
        // REMIND: This does not work for all destinations yet since
        // the screen SurfaceData objects do not implement getRaster
        Class<?> owner = CustomComponent.class;
        GraphicsPrimitive[] primitives = {
            new GraphicsPrimitiveProxy(owner, "OpaqueCopyAnyToArgb",
                                       Blit.methodSignature,
                                       Blit.primTypeID,
                                       SurfaceType.Any,
                                       CompositeType.SrcNoEa,
                                       SurfaceType.IntArgb),
            new GraphicsPrimitiveProxy(owner, "OpaqueCopyArgbToAny",
                                       Blit.methodSignature,
                                       Blit.primTypeID,
                                       SurfaceType.IntArgb,
                                       CompositeType.SrcNoEa,
                                       SurfaceType.Any),
            new GraphicsPrimitiveProxy(owner, "XorCopyArgbToAny",
                                       Blit.methodSignature,
                                       Blit.primTypeID,
                                       SurfaceType.IntArgb,
                                       CompositeType.Xor,
                                       SurfaceType.Any),
        };
        GraphicsPrimitiveMgr.register(primitives);
    }

    public static Region getRegionOfInterest(SurfaceData src, SurfaceData dst,
                                             Region clip,
                                             int srcx, int srcy,
                                             int dstx, int dsty,
                                             int w, int h)
    {
        /*
         * Intersect all of:
         *   - operation area (dstx, dsty, w, h)
         *   - destination bounds
         *   - (translated) src bounds
         *   - supplied clip (may be non-rectangular)
         * Intersect the rectangular regions first since those are
         * simpler operations.
         */
        Region ret = Region.getInstanceXYWH(dstx, dsty, w, h);
        ret = ret.getIntersection(dst.getBounds());
        Rectangle r = src.getBounds();
        // srcxy in src space maps to dstxy in dst space
        r.translate(dstx - srcx, dsty - srcy);
        ret = ret.getIntersection(r);
        if (clip != null) {
            // Intersect with clip last since it may be non-rectangular
            ret = ret.getIntersection(clip);
        }
        return ret;
    }
}

/**
 *   ANY format to ARGB format Blit
 */
class OpaqueCopyAnyToArgb extends Blit {
    OpaqueCopyAnyToArgb() {
        super(SurfaceType.Any,
              CompositeType.SrcNoEa,
              SurfaceType.IntArgb);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int srcx, int srcy, int dstx, int dsty, int w, int h)
    {
        Raster srcRast = src.getRaster(srcx, srcy, w, h);
        ColorModel srcCM = src.getColorModel();

        Raster dstRast = dst.getRaster(dstx, dsty, w, h);
        IntegerComponentRaster icr = (IntegerComponentRaster) dstRast;
        int[] dstPix = icr.getDataStorage();

        Region roi = CustomComponent.getRegionOfInterest(src, dst, clip,
                                                         srcx, srcy,
                                                         dstx, dsty, w, h);
        SpanIterator si = roi.getSpanIterator();

        Object srcPix = null;

        int dstScan = icr.getScanlineStride();
        // assert(icr.getPixelStride() == 1);
        srcx -= dstx;
        srcy -= dsty;
        int[] span = new int[4];
        while (si.nextSpan(span)) {
            int rowoff = icr.getDataOffset(0) + span[1] * dstScan + span[0];
            for (int y = span[1]; y < span[3]; y++) {
                int off = rowoff;
                for (int x = span[0]; x < span[2]; x++) {
                    srcPix = srcRast.getDataElements(x+srcx, y+srcy, srcPix);
                    dstPix[off++] = srcCM.getRGB(srcPix);
                }
                rowoff += dstScan;
            }
        }
        // Pixels in the dest were modified directly, we must
        // manually notify the raster that it was modified
        icr.markDirty();
        // REMIND: We need to do something to make sure that dstRast
        // is put back to the destination (as in the native Release
        // function)
        // src.releaseRaster(srcRast);  // NOP?
        // dst.releaseRaster(dstRast);
    }
}

/**
 *   ARGB format to ANY format Blit
 */
class OpaqueCopyArgbToAny extends Blit {
    OpaqueCopyArgbToAny() {
        super(SurfaceType.IntArgb,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int srcx, int srcy, int dstx, int dsty, int w, int h)
    {
        Raster srcRast = src.getRaster(srcx, srcy, w, h);
        IntegerComponentRaster icr = (IntegerComponentRaster) srcRast;
        int[] srcPix = icr.getDataStorage();

        WritableRaster dstRast =
            (WritableRaster) dst.getRaster(dstx, dsty, w, h);
        ColorModel dstCM = dst.getColorModel();

        Region roi = CustomComponent.getRegionOfInterest(src, dst, clip,
                                                         srcx, srcy,
                                                         dstx, dsty, w, h);
        SpanIterator si = roi.getSpanIterator();

        Object dstPix = null;

        int srcScan = icr.getScanlineStride();
        // assert(icr.getPixelStride() == 1);
        srcx -= dstx;
        srcy -= dsty;
        int[] span = new int[4];
        while (si.nextSpan(span)) {
            int rowoff = (icr.getDataOffset(0) +
                          (srcy + span[1]) * srcScan +
                          (srcx + span[0]));
            for (int y = span[1]; y < span[3]; y++) {
                int off = rowoff;
                for (int x = span[0]; x < span[2]; x++) {
                    dstPix = dstCM.getDataElements(srcPix[off++], dstPix);
                    dstRast.setDataElements(x, y, dstPix);
                }
                rowoff += srcScan;
            }
        }
        // REMIND: We need to do something to make sure that dstRast
        // is put back to the destination (as in the native Release
        // function)
        // src.releaseRaster(srcRast);  // NOP?
        // dst.releaseRaster(dstRast);
    }
}

/**
 *   ARGB format to ANY format Blit (pixels are XORed together with XOR pixel)
 */
class XorCopyArgbToAny extends Blit {
    XorCopyArgbToAny() {
        super(SurfaceType.IntArgb,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int srcx, int srcy, int dstx, int dsty, int w, int h)
    {
        Raster srcRast = src.getRaster(srcx, srcy, w, h);
        IntegerComponentRaster icr = (IntegerComponentRaster) srcRast;
        int[] srcPix = icr.getDataStorage();

        WritableRaster dstRast =
            (WritableRaster) dst.getRaster(dstx, dsty, w, h);
        ColorModel dstCM = dst.getColorModel();

        Region roi = CustomComponent.getRegionOfInterest(src, dst, clip,
                                                         srcx, srcy,
                                                         dstx, dsty, w, h);
        SpanIterator si = roi.getSpanIterator();

        int xorrgb = ((XORComposite)comp).getXorColor().getRGB();
        Object xorPixel = dstCM.getDataElements(xorrgb, null);

        Object srcPixel = null;
        Object dstPixel = null;

        int srcScan = icr.getScanlineStride();
        // assert(icr.getPixelStride() == 1);
        srcx -= dstx;
        srcy -= dsty;
        int[] span = new int[4];
        while (si.nextSpan(span)) {
            int rowoff = (icr.getDataOffset(0) +
                          (srcy + span[1]) * srcScan +
                          (srcx + span[0]));
            for (int y = span[1]; y < span[3]; y++) {
                int off = rowoff;
                for (int x = span[0]; x < span[2]; x++) {
                    // REMIND: alpha bits of the destination pixel are
                    // currently altered by the XOR operation, but
                    // should be left untouched
                    srcPixel = dstCM.getDataElements(srcPix[off++], srcPixel);
                    dstPixel = dstRast.getDataElements(x, y, dstPixel);

                    switch (dstCM.getTransferType()) {
                    case DataBuffer.TYPE_BYTE:
                        byte[] bytesrcarr = (byte[]) srcPixel;
                        byte[] bytedstarr = (byte[]) dstPixel;
                        byte[] bytexorarr = (byte[]) xorPixel;
                        for (int i = 0; i < bytedstarr.length; i++) {
                            bytedstarr[i] ^= bytesrcarr[i] ^ bytexorarr[i];
                        }
                        break;
                    case DataBuffer.TYPE_SHORT:
                    case DataBuffer.TYPE_USHORT:
                        short[] shortsrcarr = (short[]) srcPixel;
                        short[] shortdstarr = (short[]) dstPixel;
                        short[] shortxorarr = (short[]) xorPixel;
                        for (int i = 0; i < shortdstarr.length; i++) {
                            shortdstarr[i] ^= shortsrcarr[i] ^ shortxorarr[i];
                        }
                        break;
                    case DataBuffer.TYPE_INT:
                        int[] intsrcarr = (int[]) srcPixel;
                        int[] intdstarr = (int[]) dstPixel;
                        int[] intxorarr = (int[]) xorPixel;
                        for (int i = 0; i < intdstarr.length; i++) {
                            intdstarr[i] ^= intsrcarr[i] ^ intxorarr[i];
                        }
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        float[] floatsrcarr = (float[]) srcPixel;
                        float[] floatdstarr = (float[]) dstPixel;
                        float[] floatxorarr = (float[]) xorPixel;
                        for (int i = 0; i < floatdstarr.length; i++) {
                            int v = (Float.floatToIntBits(floatdstarr[i]) ^
                                     Float.floatToIntBits(floatsrcarr[i]) ^
                                     Float.floatToIntBits(floatxorarr[i]));
                            floatdstarr[i] = Float.intBitsToFloat(v);
                        }
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        double[] doublesrcarr = (double[]) srcPixel;
                        double[] doubledstarr = (double[]) dstPixel;
                        double[] doublexorarr = (double[]) xorPixel;
                        for (int i = 0; i < doubledstarr.length; i++) {
                            long v = (Double.doubleToLongBits(doubledstarr[i]) ^
                                      Double.doubleToLongBits(doublesrcarr[i]) ^
                                      Double.doubleToLongBits(doublexorarr[i]));
                            doubledstarr[i] = Double.longBitsToDouble(v);
                        }
                        break;
                    default:
                        throw new InternalError("Unsupported XOR pixel type");
                    }
                    dstRast.setDataElements(x, y, dstPixel);
                }
                rowoff += srcScan;
            }
        }
        // REMIND: We need to do something to make sure that dstRast
        // is put back to the destination (as in the native Release
        // function)
        // src.releaseRaster(srcRast);  // NOP?
        // dst.releaseRaster(dstRast);
    }
}
