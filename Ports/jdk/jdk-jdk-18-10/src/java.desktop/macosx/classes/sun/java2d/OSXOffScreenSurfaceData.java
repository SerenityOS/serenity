/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.nio.*;

import sun.awt.image.*;
import sun.java2d.loops.*;

public class OSXOffScreenSurfaceData extends OSXSurfaceData // implements RasterListener
{
    private static native void initIDs();

    static {
        initIDs();
    }

    // the image associated with this surface
    BufferedImage bim;
    // the image associated with this custom surface
    BufferedImage bimBackup;
    // <rdar://problem/4177639> nio based images use ARGB_PRE
    static DirectColorModel dcmBackup = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000, true, DataBuffer.TYPE_INT);

    Object lock;

    // cached rasters for easy access
    WritableRaster bufImgRaster;
    SunWritableRaster bufImgSunRaster;

    // these are extra image types we can handle
    private static final int TYPE_3BYTE_RGB = BufferedImage.TYPE_BYTE_INDEXED + 1;

    // these are for callbacks when pixes have been touched
    protected ByteBuffer fImageInfo;
    IntBuffer fImageInfoInt;
    private static final int kNeedToSyncFromJavaPixelsIndex = 0;
    private static final int kNativePixelsChangedIndex = 1;
    private static final int kImageStolenIndex = 2;
    private static final int kSizeOfParameters = kImageStolenIndex + 1;

    public static native SurfaceData getSurfaceData(BufferedImage bufImg);

    protected static native void setSurfaceData(BufferedImage bufImg, SurfaceData sData);

    public static SurfaceData createData(BufferedImage bufImg) {
        /*
         * if ((bufImg.getWidth() == 32) && (bufImg.getHeight() == 32)) { Thread.dumpStack(); }
         */
        // This could be called from multiple threads. We need to synchronized on the image so that
        // we can ensure that only one surface data is created per image. (<rdar://4564873>)
        // Note: Eventually, we should switch to using the same mechanism (CachingSurfaceManager) that Sun uses
        // <rdar://4563741>
        synchronized (bufImg) {
            SurfaceData sData = getSurfaceData(bufImg);
            if (sData != null) { return sData; }

            OSXOffScreenSurfaceData osData = OSXOffScreenSurfaceData.createNewSurface(bufImg);

            OSXOffScreenSurfaceData.setSurfaceData(bufImg, osData);
            osData.cacheRasters(bufImg);
//            osData.setRasterListener();

            return osData;
        }
    }

    public static SurfaceData createData(Raster ras, ColorModel cm) {
        throw new InternalError("SurfaceData not implemented for Raster/CM");
    }

    static OSXOffScreenSurfaceData createNewSurface(BufferedImage bufImg) {
        SurfaceData sData = null;

        ColorModel cm = bufImg.getColorModel();
        int type = bufImg.getType();
        // REMIND: Check the image type and pick an appropriate subclass
        switch (type) {
            case BufferedImage.TYPE_INT_BGR:
                sData = createDataIC(bufImg, SurfaceType.IntBgr);
                break;
            case BufferedImage.TYPE_INT_RGB:
                sData = createDataIC(bufImg, SurfaceType.IntRgb);
                break;
            case BufferedImage.TYPE_INT_ARGB:
                sData = createDataIC(bufImg, SurfaceType.IntArgb);
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                sData = createDataIC(bufImg, SurfaceType.IntArgbPre);
                break;
            case BufferedImage.TYPE_3BYTE_BGR:
                sData = createDataBC(bufImg, SurfaceType.ThreeByteBgr, 2);
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                sData = createDataBC(bufImg, SurfaceType.FourByteAbgr, 3);
                break;
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                sData = createDataBC(bufImg, SurfaceType.FourByteAbgrPre, 3);
                break;
            case BufferedImage.TYPE_USHORT_565_RGB:
                sData = createDataSC(bufImg, SurfaceType.Ushort565Rgb, null);
                break;
            case BufferedImage.TYPE_USHORT_555_RGB:
                sData = createDataSC(bufImg, SurfaceType.Ushort555Rgb, null);
                break;
            case BufferedImage.TYPE_BYTE_INDEXED: {
                SurfaceType sType;
                switch (cm.getTransparency()) {
                    case OPAQUE:
                        if (isOpaqueGray((IndexColorModel) cm)) {
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
                sData = createDataBC(bufImg, sType, 0);
            }
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                sData = createDataBC(bufImg, SurfaceType.ByteGray, 0);
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                sData = createDataSC(bufImg, SurfaceType.UshortGray, null);
                break;
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_CUSTOM:
            default: {
                Raster raster = bufImg.getRaster();

                // we try to fit a custom image into one of the predefined BufferedImages (BufferedImage does that
                // first, we further refine it here)
                // we can do that because a pointer in C is a pointer (pixel pointer not dependent on DataBuffer type)
                SampleModel sm = bufImg.getSampleModel();
                SurfaceType sType = SurfaceType.Custom;
                int transferType = cm.getTransferType();
                int pixelSize = cm.getPixelSize();
                int numOfComponents = cm.getNumColorComponents();
                if ((numOfComponents == 3) && (cm instanceof ComponentColorModel) && (sm instanceof PixelInterleavedSampleModel)) {
                    int[] sizes = cm.getComponentSize();
                    boolean validsizes = (sizes[0] == 8) && (sizes[1] == 8) && (sizes[2] == 8);
                    int[] offs = ((ComponentSampleModel) sm).getBandOffsets();
                    int numBands = raster.getNumBands();
                    boolean bigendian = (offs[0] == numBands - 3) && (offs[1] == numBands - 2) && (offs[2] == numBands - 1);
                    boolean littleendian = (offs[0] == numBands - 1) && (offs[1] == numBands - 2) && (offs[2] == numBands - 3);

                    if ((pixelSize == 32) && (transferType == DataBuffer.TYPE_INT)) {
                        if (validsizes && bigendian && cm.hasAlpha() && cm.isAlphaPremultiplied() && sizes[3] == 8) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_INT_ARGB_PRE);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && bigendian && cm.hasAlpha() && sizes[3] == 8) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_INT_ARGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian && cm.hasAlpha() && cm.isAlphaPremultiplied() && sizes[3] == 8) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_4BYTE_ABGR_PRE);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian && cm.hasAlpha() && sizes[3] == 8) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_4BYTE_ABGR);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && bigendian) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_INT_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 32) && (transferType == DataBuffer.TYPE_BYTE)) {
                        if (validsizes && bigendian && cm.hasAlpha() && cm.isAlphaPremultiplied() && sizes[3] == 8) {
                            try {
                                sData = createDataBC(bufImg, sType, 3, BufferedImage.TYPE_INT_ARGB_PRE);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                        if (validsizes && bigendian && cm.hasAlpha() && sizes[3] == 8) {
                            try {
                                sData = createDataBC(bufImg, sType, 3, BufferedImage.TYPE_INT_ARGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian && cm.hasAlpha() && cm.isAlphaPremultiplied() && sizes[3] == 8) {
                            try {
                                sData = createDataBC(bufImg, sType, 3, BufferedImage.TYPE_4BYTE_ABGR_PRE);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian && cm.hasAlpha() && sizes[3] == 8) {
                            try {
                                sData = createDataBC(bufImg, sType, 3, BufferedImage.TYPE_4BYTE_ABGR);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian) {
                            try {
                                sData = createDataBC(bufImg, sType, 3, BufferedImage.TYPE_INT_BGR);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && bigendian) {
                            try {
                                sData = createDataBC(bufImg, sType, 3, BufferedImage.TYPE_INT_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 24) && (transferType == DataBuffer.TYPE_INT)) {
                        if (validsizes && bigendian) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_INT_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian) {
                            try {
                                sData = createDataIC(bufImg, sType, BufferedImage.TYPE_INT_BGR);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 24) && (transferType == DataBuffer.TYPE_BYTE)) {
                        if (validsizes && bigendian) {
                            try {
                                sData = createDataBC(bufImg, sType, 0, TYPE_3BYTE_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        } else if (validsizes && littleendian) {
                            try {
                                sData = createDataBC(bufImg, sType, 0, BufferedImage.TYPE_3BYTE_BGR);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 16) && (transferType == DataBuffer.TYPE_USHORT)) {
                        validsizes = (sizes[0] == 5) && (sizes[1] == 6) && (sizes[2] == 5);
                        if (validsizes && bigendian) {
                            try {
                                sData = createDataSC(bufImg, sType, null, BufferedImage.TYPE_USHORT_565_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 16) && (transferType == DataBuffer.TYPE_BYTE)) {
                        validsizes = (sizes[0] == 5) && (sizes[1] == 6) && (sizes[2] == 5);
                        if (validsizes && bigendian) {
                            try {
                                sData = createDataBC(bufImg, sType, 1, BufferedImage.TYPE_USHORT_565_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 15) && (transferType == DataBuffer.TYPE_USHORT)) {
                        validsizes = (sizes[0] == 5) && (sizes[1] == 5) && (sizes[2] == 5);
                        if (validsizes && bigendian) {
                            try {
                                sData = createDataSC(bufImg, sType, null, BufferedImage.TYPE_USHORT_555_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    } else if ((pixelSize == 15) && (transferType == DataBuffer.TYPE_BYTE)) {
                        validsizes = (sizes[0] == 5) && (sizes[1] == 5) && (sizes[2] == 5);
                        if (validsizes && bigendian) {
                            try {
                                sData = createDataBC(bufImg, sType, 1, BufferedImage.TYPE_USHORT_555_RGB);
                            } catch (ClassCastException e) {
                                sData = null;
                            }
                        }
                    }
                }
            }
                break;
        }

        // we failed to match
        if (sData == null) {
            sData = new OSXOffScreenSurfaceData(bufImg, SurfaceType.Custom);
            OSXOffScreenSurfaceData offsd = (OSXOffScreenSurfaceData) sData;

            // 2004_03_26 cmc: We used to use createCompatibleImage here. Now that createCompatibleImage returns
            // an INT_ARGB_PRE instead of an NIO-based image, we need to explicitly create an NIO-based image.
            IntegerNIORaster backupRaster = (IntegerNIORaster) IntegerNIORaster.createNIORaster(bufImg.getWidth(), bufImg.getHeight(), dcmBackup.getMasks(), null);
            offsd.bimBackup = new BufferedImage(dcmBackup, backupRaster, dcmBackup.isAlphaPremultiplied(), null);

            // the trick that makes it work - assign the raster from backup to the surface data of the original image
            offsd.initCustomRaster(backupRaster.getBuffer(),
                                    backupRaster.getWidth(),
                                    backupRaster.getHeight(),
                                    offsd.fGraphicsStates,
                                    offsd.fGraphicsStatesObject,
                                    offsd.fImageInfo);

            //offsd.checkIfLazyPixelConversionDisabled();
            offsd.fImageInfoInt.put(kImageStolenIndex, 1);
        }

        return (OSXOffScreenSurfaceData) sData;
    }

    private static SurfaceData createDataIC(BufferedImage bImg, SurfaceType sType, int iType) {
        OSXOffScreenSurfaceData offsd = new OSXOffScreenSurfaceData(bImg, sType);

        IntegerComponentRaster icRaster = (IntegerComponentRaster) bImg.getRaster();
        offsd.initRaster(icRaster.getDataStorage(),
                            icRaster.getDataOffset(0) * 4,
                            icRaster.getWidth(),
                            icRaster.getHeight(),
                            icRaster.getPixelStride() * 4,
                            icRaster.getScanlineStride() * 4,
                            null,
                            iType,
                            offsd.fGraphicsStates,
                            offsd.fGraphicsStatesObject,
                            offsd.fImageInfo);

       // offsd.checkIfLazyPixelConversionDisabled();
        offsd.fImageInfoInt.put(kImageStolenIndex, 1);
        return offsd;
    }

    public static SurfaceData createDataIC(BufferedImage bImg, SurfaceType sType) {
        return createDataIC(bImg, sType, bImg.getType());
    }

    private static SurfaceData createDataSC(BufferedImage bImg, SurfaceType sType, IndexColorModel icm, int iType) {
        OSXOffScreenSurfaceData offsd = new OSXOffScreenSurfaceData(bImg, sType);

        ShortComponentRaster scRaster = (ShortComponentRaster) bImg.getRaster();
        offsd.initRaster(scRaster.getDataStorage(),
                            scRaster.getDataOffset(0) * 2,
                            scRaster.getWidth(),
                            scRaster.getHeight(),
                            scRaster.getPixelStride() * 2,
                            scRaster.getScanlineStride() * 2,
                            icm,
                            iType,
                            offsd.fGraphicsStates,
                            offsd.fGraphicsStatesObject,
                            offsd.fImageInfo);

        //offsd.checkIfLazyPixelConversionDisabled();
        offsd.fImageInfoInt.put(kImageStolenIndex, 1);
        return offsd;
    }

    public static SurfaceData createDataSC(BufferedImage bImg, SurfaceType sType, IndexColorModel icm) {
        return createDataSC(bImg, sType, icm, bImg.getType());
    }

    private static SurfaceData createDataBC(BufferedImage bImg, SurfaceType sType, int primaryBank, int iType) {
        OSXOffScreenSurfaceData offsd = new OSXOffScreenSurfaceData(bImg, sType);

        ByteComponentRaster bcRaster = (ByteComponentRaster) bImg.getRaster();
        ColorModel cm = bImg.getColorModel();
        IndexColorModel icm = ((cm instanceof IndexColorModel) ? (IndexColorModel) cm : null);
        offsd.initRaster(bcRaster.getDataStorage(),
                            bcRaster.getDataOffset(primaryBank),
                            bcRaster.getWidth(),
                            bcRaster.getHeight(),
                            bcRaster.getPixelStride(),
                            bcRaster.getScanlineStride(),
                            icm,
                            iType,
                            offsd.fGraphicsStates,
                            offsd.fGraphicsStatesObject,
                            offsd.fImageInfo);

        offsd.fImageInfoInt.put(kImageStolenIndex, 1);

        return offsd;
    }

    public static SurfaceData createDataBC(BufferedImage bImg, SurfaceType sType, int primaryBank) {
        return createDataBC(bImg, sType, primaryBank, bImg.getType());
    }

    private static SurfaceData createDataBP(BufferedImage bImg, SurfaceType sType, int iType) {
        OSXOffScreenSurfaceData offsd = new OSXOffScreenSurfaceData(bImg, sType);

        BytePackedRaster bpRaster = (BytePackedRaster) bImg.getRaster();
        ColorModel cm = bImg.getColorModel();
        IndexColorModel icm = ((cm instanceof IndexColorModel) ? (IndexColorModel) cm : null);
        offsd.initRaster(bpRaster.getDataStorage(),
                            bpRaster.getDataBitOffset(), // in bits, NOT bytes! (needs special attention in native
                                                         // code!)
                bpRaster.getWidth(),
                            bpRaster.getHeight(),
                            bpRaster.getPixelBitStride(),
                            bpRaster.getScanlineStride() * 8,
                            icm,
                            iType,
                            offsd.fGraphicsStates,
                            offsd.fGraphicsStatesObject,
                            offsd.fImageInfo);

        //offsd.checkIfLazyPixelConversionDisabled();
        offsd.fImageInfoInt.put(kImageStolenIndex, 1);
        return offsd;
    }

    protected native void initRaster(Object theArray, int offset, int width, int height, int pixStr, int scanStr, IndexColorModel icm, int type, ByteBuffer graphicsStates, Object graphicsStatesObjects, ByteBuffer imageInfo);

    protected native void initCustomRaster(IntBuffer buffer, int width, int height, ByteBuffer graphicsStates, Object graphicsStatesObjects, ByteBuffer imageInfo);

    public Object getLockObject() {
        return this.lock;
    }

    // Makes the constructor package private instead of public.
    OSXOffScreenSurfaceData(BufferedImage bufImg, SurfaceType sType) {
        super(sType, bufImg.getColorModel());
        setBounds(0, 0, bufImg.getWidth(), bufImg.getHeight());

        this.bim = bufImg;

        this.fImageInfo = ByteBuffer.allocateDirect(4 * kSizeOfParameters);
        this.fImageInfo.order(ByteOrder.nativeOrder());
        this.fImageInfoInt = this.fImageInfo.asIntBuffer();

        this.fImageInfoInt.put(kNeedToSyncFromJavaPixelsIndex, 1); // need to sync from Java the very first time
        this.fImageInfoInt.put(kNativePixelsChangedIndex, 0);
        this.fImageInfoInt.put(kImageStolenIndex, 0);

        this.lock = new Object();
    }

    /**
     * Performs a copyArea within this surface.
     */
    public boolean copyArea(SunGraphics2D sg2d, int x, int y, int w, int h, int dx, int dy) {
        // <rdar://problem/4488745> For the Sun2D renderer we should rely on the implementation of the super class.
        // BufImageSurfaceData.java doesn't have an implementation of copyArea() and relies on the super class.

        if (sg2d.transformState >= SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
            return false;
        }

        // reset the clip (this is how it works on windows)
        // we actually can handle a case with any clips but windows ignores the light clip
        Shape clip = sg2d.getClip();
        sg2d.setClip(getBounds());

        // clip copyArea
        Rectangle clippedCopyAreaRect = clipCopyArea(sg2d, x, y, w, h, dx, dy);
        if (clippedCopyAreaRect == null) {
            // clipped out
            return true;
        }

        // the rectangle returned from clipCopyArea() is in the coordinate space
        // of the surface (image)
        x = clippedCopyAreaRect.x;
        y = clippedCopyAreaRect.y;
        w = clippedCopyAreaRect.width;
        h = clippedCopyAreaRect.height;

        // copy (dst coordinates are in the coord space of the graphics2d, and
        // src coordinates are in the coordinate space of the image)
        // sg2d.drawImage expects the destination rect to be in the coord space
        // of the graphics2d. <rdar://3746194> (vm)
        // we need to substract the transX and transY to move it
        // to the coordinate space of the graphics2d.
        int dstX = x + dx - sg2d.transX;
        int dstY = y + dy - sg2d.transY;
        sg2d.drawImage(this.bim, dstX, dstY, dstX + w, dstY + h,
                       x, y, x + w, y + h, null);

        // restore the clip
        sg2d.setClip(clip);

        return true;
    }

    /**
     * Performs a copyarea from this surface to a buffered image. If null is passed in for the image a new image will be
     * created.
     *
     * Only used by compositor code (private API)
     */
    public BufferedImage copyArea(SunGraphics2D sg2d, int x, int y, int w, int h, BufferedImage dstImage) {
        // create the destination image if needed
        if (dstImage == null) {
            dstImage = getDeviceConfiguration().createCompatibleImage(w, h);
        }

        // copy
        Graphics g = dstImage.createGraphics();
        g.drawImage(this.bim, 0, 0, w, h, x, y, x + w, y + h, null);
        g.dispose();

        return dstImage;
    }

    public boolean xorSurfacePixels(SunGraphics2D sg2d, BufferedImage srcPixels, int x, int y, int w, int h, int colorXOR) {

        int type = this.bim.getType();

        if ((type == BufferedImage.TYPE_INT_ARGB_PRE) || (type == BufferedImage.TYPE_INT_ARGB) || (type == BufferedImage.TYPE_INT_RGB)) { return xorSurfacePixels(createData(srcPixels), colorXOR, x, y, w, h); }

        return false;
    }

    native boolean xorSurfacePixels(SurfaceData src, int colorXOR, int x, int y, int w, int h);

    public void clearRect(BufferedImage bim, int w, int h) {
        OSXOffScreenSurfaceData offsd = (OSXOffScreenSurfaceData) (OSXOffScreenSurfaceData.createData(bim));
        // offsd.clear();
        if (offsd.clearSurfacePixels(w, h) == false) {
            Graphics2D g = bim.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    native boolean clearSurfacePixels(int w, int h);

    // 04/06/04 cmc: radr://3612381 Graphics.drawImage ignores bgcolor parameter.
    // getCopyWithBgColor returns a new version of an image, drawn with a background
    // color. Called by blitImage in OSXSurfaceData.java.
    BufferedImage copyWithBgColor_cache = null;

    public SurfaceData getCopyWithBgColor(Color bgColor) {
        int bimW = this.bim.getWidth();
        int bimH = this.bim.getHeight();

        if ((this.copyWithBgColor_cache == null)
                || (this.copyWithBgColor_cache.getWidth() < bimW) || (this.copyWithBgColor_cache.getHeight() < bimH)) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            this.copyWithBgColor_cache = gc.createCompatibleImage(bimW, bimH);
        }

        Graphics g2 = this.copyWithBgColor_cache.createGraphics();
        g2.setColor(bgColor);
        g2.fillRect(0, 0, bimW, bimH);
        g2.drawImage(this.bim, 0, 0, bimW, bimH, null);
        g2.dispose();

        return getSurfaceData(this.copyWithBgColor_cache);
    }

    /**
     * Invoked before the raster's contents are to be read (via one of the modifier methods in Raster such as
     * getPixel())
     */
    public void rasterRead() {
        if (fImageInfoInt.get(kNativePixelsChangedIndex) == 1) {
            syncToJavaPixels();
        }
    }

    /**
     * Invoked before the raster's contents are to be written to (via one of the modifier methods in Raster such as
     * setPixel())
     */
    public void rasterWrite() {
        if (fImageInfoInt.get(kNativePixelsChangedIndex) == 1) {
            syncToJavaPixels();
        }

        fImageInfoInt.put(kNeedToSyncFromJavaPixelsIndex, 1); // the pixels will change
    }

    private void syncFromCustom() {

    }

    private void syncToCustom() {

    }
//    /**
//     * Invoked when the raster's contents will be taken (via the Raster.getDataBuffer() method)
//     */
//    public void rasterStolen() {
//        fImageInfoInt.put(kImageStolenIndex, 1); // this means we must convert between Java and native pixels every
//                                                 // single primitive! (very expensive)
//        if (fImageInfoInt.get(kNativePixelsChangedIndex) == 1) {
//            syncToJavaPixels();
//        }
//
//        // we know the pixels have been stolen, no need to listen for changes any more
////        if (this.bufImgSunRaster != null) {
////            this.bufImgSunRaster.setRasterListener(null);
////        }
//    }

    private native void syncToJavaPixels();

    // we need to refer to rasters often, so cache them
    void cacheRasters(BufferedImage bim) {
        this.bufImgRaster = bim.getRaster();
        if (this.bufImgRaster instanceof SunWritableRaster) {
            this.bufImgSunRaster = (SunWritableRaster) this.bufImgRaster;
        }
    }

//    void setRasterListener() {
//        if (this.bufImgSunRaster != null) {
//            this.bufImgSunRaster.setRasterListener(this);
//
//            Raster parentRaster = this.bufImgSunRaster.getParent();
//            if (parentRaster != null) {
//                if (parentRaster instanceof SunWritableRaster) {
//                    // mark subimages stolen to turn off lazy pixel conversion (gznote: can we do better here?)
//                    ((SunWritableRaster) parentRaster).notifyStolen();
//                }
//                rasterStolen();
//            }
//        } else {
//            // it's a custom image (non-natively supported) and we can not set a raster listener
//            // so mark the image as stolen - this will turn off LazyPixelConversion optimization (slow, but correct)
//            rasterStolen();
//        }
//    }
}
