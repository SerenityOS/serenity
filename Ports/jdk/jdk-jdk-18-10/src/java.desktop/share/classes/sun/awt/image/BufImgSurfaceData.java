/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.image;

import java.awt.Rectangle;
import java.awt.GraphicsConfiguration;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import sun.java2d.SurfaceData;
import sun.java2d.SunGraphics2D;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.RenderLoops;


public class BufImgSurfaceData extends SurfaceData {
    BufferedImage bufImg;
    private BufferedImageGraphicsConfig graphicsConfig;
    RenderLoops solidloops;
    private final double scaleX;
    private final double scaleY;

    private static native void initIDs(Class<?> ICM, Class<?> ICMColorData);

    private static final int DCM_RGBX_RED_MASK   = 0xff000000;
    private static final int DCM_RGBX_GREEN_MASK = 0x00ff0000;
    private static final int DCM_RGBX_BLUE_MASK  = 0x0000ff00;
    private static final int DCM_555X_RED_MASK = 0xF800;
    private static final int DCM_555X_GREEN_MASK = 0x07C0;
    private static final int DCM_555X_BLUE_MASK = 0x003E;
    private static final int DCM_4444_RED_MASK   = 0x0f00;
    private static final int DCM_4444_GREEN_MASK = 0x00f0;
    private static final int DCM_4444_BLUE_MASK  = 0x000f;
    private static final int DCM_4444_ALPHA_MASK = 0xf000;
    private static final int DCM_ARGBBM_ALPHA_MASK = 0x01000000;
    private static final int DCM_ARGBBM_RED_MASK   = 0x00ff0000;
    private static final int DCM_ARGBBM_GREEN_MASK = 0x0000ff00;
    private static final int DCM_ARGBBM_BLUE_MASK  = 0x000000ff;

    static {
        initIDs(IndexColorModel.class, ICMColorData.class);
    }

    public static SurfaceData createData(BufferedImage bufImg) {
        return createData(bufImg, 1, 1);
    }

    public static SurfaceData createData(BufferedImage bufImg,
                                         double scaleX, double scaleY)
    {
        if (bufImg == null) {
            throw new NullPointerException("BufferedImage cannot be null");
        }
        SurfaceData sData;
        ColorModel cm = bufImg.getColorModel();
        int type = bufImg.getType();
        // REMIND: Check the image type and pick an appropriate subclass
        switch (type) {
        case BufferedImage.TYPE_INT_BGR:
            sData = createDataIC(bufImg, SurfaceType.IntBgr, scaleX, scaleY);
            break;
        case BufferedImage.TYPE_INT_RGB:
            sData = createDataIC(bufImg, SurfaceType.IntRgb, scaleX, scaleY);
            break;
        case BufferedImage.TYPE_INT_ARGB:
            sData = createDataIC(bufImg, SurfaceType.IntArgb, scaleX, scaleY);
            break;
        case BufferedImage.TYPE_INT_ARGB_PRE:
            sData = createDataIC(bufImg, SurfaceType.IntArgbPre, scaleX, scaleY);
            break;
        case BufferedImage.TYPE_3BYTE_BGR:
            sData = createDataBC(bufImg, SurfaceType.ThreeByteBgr, 2,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_4BYTE_ABGR:
            sData = createDataBC(bufImg, SurfaceType.FourByteAbgr, 3,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            sData = createDataBC(bufImg, SurfaceType.FourByteAbgrPre, 3,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_USHORT_565_RGB:
            sData = createDataSC(bufImg, SurfaceType.Ushort565Rgb, null,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_USHORT_555_RGB:
            sData = createDataSC(bufImg, SurfaceType.Ushort555Rgb, null,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_BYTE_INDEXED:
            {
                SurfaceType sType;
                switch (cm.getTransparency()) {
                case OPAQUE:
                    if (isOpaqueGray((IndexColorModel)cm)) {
                        sType = SurfaceType.Index8Gray;
                    } else {
                        sType = SurfaceType.ByteIndexedOpaque;
                    }
                    break;
                case BITMASK:
                    sType = SurfaceType.ByteIndexedBm;
                    break;
                case TRANSLUCENT:
                    sType = SurfaceType.ByteIndexed;
                    break;
                default:
                    throw new InternalError("Unrecognized transparency");
                }
                sData = createDataBC(bufImg, sType, 0, scaleX, scaleY);
            }
            break;
        case BufferedImage.TYPE_BYTE_GRAY:
            sData = createDataBC(bufImg, SurfaceType.ByteGray, 0,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_USHORT_GRAY:
            sData = createDataSC(bufImg, SurfaceType.UshortGray, null,
                                 scaleX, scaleY);
            break;
        case BufferedImage.TYPE_BYTE_BINARY:
            {
                SurfaceType sType;
                SampleModel sm = bufImg.getRaster().getSampleModel();
                switch (sm.getSampleSize(0)) {
                case 1:
                    sType = SurfaceType.ByteBinary1Bit;
                    break;
                case 2:
                    sType = SurfaceType.ByteBinary2Bit;
                    break;
                case 4:
                    sType = SurfaceType.ByteBinary4Bit;
                    break;
                default:
                    throw new InternalError("Unrecognized pixel size");
                }
                sData = createDataBP(bufImg, sType, scaleX, scaleY);
            }
            break;
        case BufferedImage.TYPE_CUSTOM:
        default:
            {
                Raster raster = bufImg.getRaster();
                int numBands = raster.getNumBands();
                if (raster instanceof IntegerComponentRaster &&
                    raster.getNumDataElements() == 1 &&
                    ((IntegerComponentRaster)raster).getPixelStride() == 1)
                {
                    SurfaceType sType = SurfaceType.AnyInt;
                    if (cm instanceof DirectColorModel) {
                        DirectColorModel dcm = (DirectColorModel) cm;
                        int aMask = dcm.getAlphaMask();
                        int rMask = dcm.getRedMask();
                        int gMask = dcm.getGreenMask();
                        int bMask = dcm.getBlueMask();
                        if (numBands == 3 &&
                            aMask == 0 &&
                            rMask == DCM_RGBX_RED_MASK &&
                            gMask == DCM_RGBX_GREEN_MASK &&
                            bMask == DCM_RGBX_BLUE_MASK)
                        {
                            sType = SurfaceType.IntRgbx;
                        } else if (numBands == 4 &&
                                   aMask == DCM_ARGBBM_ALPHA_MASK &&
                                   rMask == DCM_ARGBBM_RED_MASK &&
                                   gMask == DCM_ARGBBM_GREEN_MASK &&
                                   bMask == DCM_ARGBBM_BLUE_MASK)
                        {
                            sType = SurfaceType.IntArgbBm;
                        } else {
                            sType = SurfaceType.AnyDcm;
                        }
                    }
                    sData = createDataIC(bufImg, sType, scaleX, scaleY);
                    break;
                } else if (raster instanceof ShortComponentRaster &&
                           raster.getNumDataElements() == 1 &&
                           ((ShortComponentRaster)raster).getPixelStride() == 1)
                {
                    SurfaceType sType = SurfaceType.AnyShort;
                    IndexColorModel icm = null;
                    if (cm instanceof DirectColorModel) {
                        DirectColorModel dcm = (DirectColorModel) cm;
                        int aMask = dcm.getAlphaMask();
                        int rMask = dcm.getRedMask();
                        int gMask = dcm.getGreenMask();
                        int bMask = dcm.getBlueMask();
                        if (numBands == 3 &&
                            aMask == 0 &&
                            rMask == DCM_555X_RED_MASK &&
                            gMask == DCM_555X_GREEN_MASK &&
                            bMask == DCM_555X_BLUE_MASK)
                        {
                            sType = SurfaceType.Ushort555Rgbx;
                        } else
                        if (numBands == 4 &&
                            aMask == DCM_4444_ALPHA_MASK &&
                            rMask == DCM_4444_RED_MASK &&
                            gMask == DCM_4444_GREEN_MASK &&
                            bMask == DCM_4444_BLUE_MASK)
                        {
                            sType = SurfaceType.Ushort4444Argb;
                        }
                    } else if (cm instanceof IndexColorModel) {
                        icm = (IndexColorModel)cm;
                        if (icm.getPixelSize() == 12) {
                            if (isOpaqueGray(icm)) {
                                sType = SurfaceType.Index12Gray;
                            } else {
                                sType = SurfaceType.UshortIndexed;
                            }
                        } else {
                            icm = null;
                        }
                    }
                    sData = createDataSC(bufImg, sType, icm, scaleX, scaleY);
                    break;
                }
                sData = new BufImgSurfaceData(raster.getDataBuffer(), bufImg,
                                              SurfaceType.Custom,
                                              scaleX, scaleY);
            }
            break;
        }
        ((BufImgSurfaceData) sData).initSolidLoops();
        return sData;
    }

    public static SurfaceData createData(Raster ras, ColorModel cm) {
        throw new InternalError("SurfaceData not implemented for Raster/CM");
    }

    public static SurfaceData createDataIC(BufferedImage bImg,
                                           SurfaceType sType,
                                           double scaleX,
                                           double scaleY)
    {
        IntegerComponentRaster icRaster =
            (IntegerComponentRaster)bImg.getRaster();
        BufImgSurfaceData bisd =
            new BufImgSurfaceData(icRaster.getDataBuffer(), bImg, sType,
                                  scaleX, scaleY);
        bisd.initRaster(icRaster.getDataStorage(),
                        icRaster.getDataOffset(0) * 4, 0,
                        icRaster.getWidth(),
                        icRaster.getHeight(),
                        icRaster.getPixelStride() * 4,
                        icRaster.getScanlineStride() * 4,
                        null);
        return bisd;
    }

    public static SurfaceData createDataSC(BufferedImage bImg,
                                           SurfaceType sType,
                                           IndexColorModel icm,
                                           double scaleX, double scaleY)
    {
        ShortComponentRaster scRaster =
            (ShortComponentRaster)bImg.getRaster();
        BufImgSurfaceData bisd =
            new BufImgSurfaceData(scRaster.getDataBuffer(), bImg, sType,
                                  scaleX, scaleY);
        bisd.initRaster(scRaster.getDataStorage(),
                        scRaster.getDataOffset(0) * 2, 0,
                        scRaster.getWidth(),
                        scRaster.getHeight(),
                        scRaster.getPixelStride() * 2,
                        scRaster.getScanlineStride() * 2,
                        icm);
        return bisd;
    }

    public static SurfaceData createDataBC(BufferedImage bImg,
                                           SurfaceType sType,
                                           int primaryBank,
                                           double scaleX, double scaleY)
    {
        ByteComponentRaster bcRaster =
            (ByteComponentRaster)bImg.getRaster();
        BufImgSurfaceData bisd =
            new BufImgSurfaceData(bcRaster.getDataBuffer(), bImg, sType,
                                  scaleX, scaleY);
        ColorModel cm = bImg.getColorModel();
        IndexColorModel icm = ((cm instanceof IndexColorModel)
                               ? (IndexColorModel) cm
                               : null);
        bisd.initRaster(bcRaster.getDataStorage(),
                        bcRaster.getDataOffset(primaryBank), 0,
                        bcRaster.getWidth(),
                        bcRaster.getHeight(),
                        bcRaster.getPixelStride(),
                        bcRaster.getScanlineStride(),
                        icm);
        return bisd;
    }

    public static SurfaceData createDataBP(BufferedImage bImg,
                                           SurfaceType sType,
                                           double scaleX, double scaleY)
    {
        BytePackedRaster bpRaster =
            (BytePackedRaster)bImg.getRaster();
        BufImgSurfaceData bisd =
            new BufImgSurfaceData(bpRaster.getDataBuffer(), bImg, sType,
                                  scaleX, scaleY);
        ColorModel cm = bImg.getColorModel();
        IndexColorModel icm = ((cm instanceof IndexColorModel)
                               ? (IndexColorModel) cm
                               : null);
        bisd.initRaster(bpRaster.getDataStorage(),
                        bpRaster.getDataBitOffset() / 8,
                        bpRaster.getDataBitOffset() & 7,
                        bpRaster.getWidth(),
                        bpRaster.getHeight(),
                        0,
                        bpRaster.getScanlineStride(),
                        icm);
        return bisd;
    }

    public RenderLoops getRenderLoops(SunGraphics2D sg2d) {
        if (sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR &&
            sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY)
        {
            return solidloops;
        }
        return super.getRenderLoops(sg2d);
    }

    public java.awt.image.Raster getRaster(int x, int y, int w, int h) {
        return bufImg.getRaster();
    }

    /**
     * Initializes the native Ops pointer.
     */
    protected native void initRaster(Object theArray,
                                     int offset,
                                     int bitoffset,
                                     int width,
                                     int height,
                                     int pixStr,
                                     int scanStr,
                                     IndexColorModel icm);

    public BufImgSurfaceData(DataBuffer db,
                             BufferedImage bufImg,
                             SurfaceType sType,
                             double scaleX,
                             double scaleY)
    {
        super(SunWritableRaster.stealTrackable(db),
              sType, bufImg.getColorModel());
        this.bufImg = bufImg;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    protected BufImgSurfaceData(SurfaceType surfaceType, ColorModel cm) {
        super(surfaceType, cm);
        this.scaleX = 1;
        this.scaleY = 1;
    }

    public void initSolidLoops() {
        this.solidloops = getSolidLoops(getSurfaceType());
    }

    private static final int CACHE_SIZE = 5;
    private static RenderLoops[] loopcache = new RenderLoops[CACHE_SIZE];
    private static SurfaceType[] typecache = new SurfaceType[CACHE_SIZE];
    public static synchronized RenderLoops getSolidLoops(SurfaceType type) {
        for (int i = CACHE_SIZE - 1; i >= 0; i--) {
            SurfaceType t = typecache[i];
            if (t == type) {
                return loopcache[i];
            } else if (t == null) {
                break;
            }
        }
        RenderLoops l = makeRenderLoops(SurfaceType.OpaqueColor,
                                        CompositeType.SrcNoEa,
                                        type);
        System.arraycopy(loopcache, 1, loopcache, 0, CACHE_SIZE-1);
        System.arraycopy(typecache, 1, typecache, 0, CACHE_SIZE-1);
        loopcache[CACHE_SIZE - 1] = l;
        typecache[CACHE_SIZE - 1] = type;
        return l;
    }

    public SurfaceData getReplacement() {
        // BufImgSurfaceData objects should never lose their contents,
        // so this method should never be called.
        return restoreContents(bufImg);
    }

    public synchronized GraphicsConfiguration getDeviceConfiguration() {
        if (graphicsConfig == null) {
            graphicsConfig = BufferedImageGraphicsConfig
                    .getConfig(bufImg, scaleX, scaleY);
        }
        return graphicsConfig;
    }

    public java.awt.Rectangle getBounds() {
        return new Rectangle(bufImg.getWidth(), bufImg.getHeight());
    }

    protected void checkCustomComposite() {
        // BufferedImages always allow Custom Composite objects since
        // their pixels are immediately retrievable anyway.
    }

    /**
     * Returns destination Image associated with this SurfaceData.
     */
    public Object getDestination() {
        return bufImg;
    }

    @Override
    public double getDefaultScaleX() {
        return scaleX;
    }

    @Override
    public double getDefaultScaleY() {
        return scaleY;
    }

    public static final class ICMColorData {
        private long pData = 0L;

        private ICMColorData(long pData) {
            this.pData = pData;
        }
    }
}
