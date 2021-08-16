/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.x11;

import java.awt.Composite;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

import sun.awt.SunHints;
import sun.awt.SunToolkit;
import sun.awt.X11ComponentPeer;
import sun.awt.X11GraphicsConfig;
import sun.awt.image.PixelConverter;
import sun.font.X11TextRenderer;
import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphics2D;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SurfaceData;
import sun.java2d.SurfaceDataProxy;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.XORComposite;
import sun.java2d.pipe.PixelToShapeConverter;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.TextPipe;
import sun.java2d.pipe.ValidatePipe;

public abstract class X11SurfaceData extends XSurfaceData {
    X11ComponentPeer peer;
    X11GraphicsConfig graphicsConfig;
    private RenderLoops solidloops;

    protected int depth;

    private static native void initIDs(Class<?> xorComp);
    protected native void initSurface(int depth, int width, int height,
                                      long drawable);

    public static final String
        DESC_INT_BGR_X11        = "Integer BGR Pixmap";
    public static final String
        DESC_INT_RGB_X11        = "Integer RGB Pixmap";

    public static final String
        DESC_4BYTE_ABGR_PRE_X11 = "4 byte ABGR Pixmap with pre-multplied alpha";
    public static final String
        DESC_INT_ARGB_PRE_X11   = "Integer ARGB Pixmap with pre-multiplied " +
                                  "alpha";

    public static final String
        DESC_BYTE_IND_OPQ_X11   = "Byte Indexed Opaque Pixmap";

    public static final String
        DESC_INT_BGR_X11_BM     = "Integer BGR Pixmap with 1-bit transp";
    public static final String
        DESC_INT_RGB_X11_BM     = "Integer RGB Pixmap with 1-bit transp";
    public static final String
        DESC_BYTE_IND_X11_BM    = "Byte Indexed Pixmap with 1-bit transp";

    public static final String
        DESC_BYTE_GRAY_X11      = "Byte Gray Opaque Pixmap";
    public static final String
        DESC_INDEX8_GRAY_X11    = "Index8 Gray Opaque Pixmap";

    public static final String
        DESC_BYTE_GRAY_X11_BM   = "Byte Gray Opaque Pixmap with 1-bit transp";
    public static final String
        DESC_INDEX8_GRAY_X11_BM = "Index8 Gray Opaque Pixmap with 1-bit transp";

    public static final String
        DESC_3BYTE_RGB_X11      = "3 Byte RGB Pixmap";
    public static final String
        DESC_3BYTE_BGR_X11      = "3 Byte BGR Pixmap";

    public static final String
        DESC_3BYTE_RGB_X11_BM   = "3 Byte RGB Pixmap with 1-bit transp";
    public static final String
        DESC_3BYTE_BGR_X11_BM   = "3 Byte BGR Pixmap with 1-bit transp";

    public static final String
        DESC_USHORT_555_RGB_X11 = "Ushort 555 RGB Pixmap";
    public static final String
        DESC_USHORT_565_RGB_X11 = "Ushort 565 RGB Pixmap";

    public static final String
        DESC_USHORT_555_RGB_X11_BM
                                = "Ushort 555 RGB Pixmap with 1-bit transp";
    public static final String
        DESC_USHORT_565_RGB_X11_BM
                                = "Ushort 565 RGB Pixmap with 1-bit transp";
    public static final String
        DESC_USHORT_INDEXED_X11 = "Ushort Indexed Pixmap";

    public static final String
        DESC_USHORT_INDEXED_X11_BM = "Ushort Indexed Pixmap with 1-bit transp";

    public static final SurfaceType IntBgrX11 =
        SurfaceType.IntBgr.deriveSubType(DESC_INT_BGR_X11);
    public static final SurfaceType IntRgbX11 =
        SurfaceType.IntRgb.deriveSubType(DESC_INT_RGB_X11);

    public static final SurfaceType FourByteAbgrPreX11 =
        SurfaceType.FourByteAbgrPre.deriveSubType(DESC_4BYTE_ABGR_PRE_X11);
    public static final SurfaceType IntArgbPreX11 =
        SurfaceType.IntArgbPre.deriveSubType(DESC_INT_ARGB_PRE_X11);

    public static final SurfaceType ThreeByteRgbX11 =
        SurfaceType.ThreeByteRgb.deriveSubType(DESC_3BYTE_RGB_X11);
    public static final SurfaceType ThreeByteBgrX11 =
        SurfaceType.ThreeByteBgr.deriveSubType(DESC_3BYTE_BGR_X11);

    public static final SurfaceType UShort555RgbX11 =
        SurfaceType.Ushort555Rgb.deriveSubType(DESC_USHORT_555_RGB_X11);
    public static final SurfaceType UShort565RgbX11 =
        SurfaceType.Ushort565Rgb.deriveSubType(DESC_USHORT_565_RGB_X11);

    public static final SurfaceType UShortIndexedX11 =
        SurfaceType.UshortIndexed.deriveSubType(DESC_USHORT_INDEXED_X11);

    public static final SurfaceType ByteIndexedOpaqueX11 =
        SurfaceType.ByteIndexedOpaque.deriveSubType(DESC_BYTE_IND_OPQ_X11);

    public static final SurfaceType ByteGrayX11 =
        SurfaceType.ByteGray.deriveSubType(DESC_BYTE_GRAY_X11);
    public static final SurfaceType Index8GrayX11 =
        SurfaceType.Index8Gray.deriveSubType(DESC_INDEX8_GRAY_X11);

    // Bitmap surface types
    public static final SurfaceType IntBgrX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_INT_BGR_X11_BM,
                                         PixelConverter.Xbgr.instance);
    public static final SurfaceType IntRgbX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_INT_RGB_X11_BM,
                                         PixelConverter.Xrgb.instance);

    public static final SurfaceType ThreeByteRgbX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_3BYTE_RGB_X11_BM,
                                         PixelConverter.Xbgr.instance);
    public static final SurfaceType ThreeByteBgrX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_3BYTE_BGR_X11_BM,
                                         PixelConverter.Xrgb.instance);

    public static final SurfaceType UShort555RgbX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_USHORT_555_RGB_X11_BM,
                                         PixelConverter.Ushort555Rgb.instance);
    public static final SurfaceType UShort565RgbX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_USHORT_565_RGB_X11_BM,
                                         PixelConverter.Ushort565Rgb.instance);

    public static final SurfaceType UShortIndexedX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_USHORT_INDEXED_X11_BM);

    public static final SurfaceType ByteIndexedX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_BYTE_IND_X11_BM);

    public static final SurfaceType ByteGrayX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_BYTE_GRAY_X11_BM);
    public static final SurfaceType Index8GrayX11_BM =
        SurfaceType.Custom.deriveSubType(DESC_INDEX8_GRAY_X11_BM);


    private static Boolean accelerationEnabled = null;

    public Raster getRaster(int x, int y, int w, int h) {
        throw new InternalError("not implemented yet");
    }

    protected X11Renderer x11pipe;
    protected PixelToShapeConverter x11txpipe;
    protected static TextPipe x11textpipe;

    static {
       if (!isX11SurfaceDataInitialized() &&
           !GraphicsEnvironment.isHeadless()) {

            initIDs(XORComposite.class);

            @SuppressWarnings("removal")
            String xtextpipe = java.security.AccessController.doPrivileged
                (new sun.security.action.GetPropertyAction("sun.java2d.xtextpipe"));
            if (xtextpipe == null || "true".startsWith(xtextpipe)) {
                if ("true".equals(xtextpipe)) {
                    // Only verbose if they use the full string "true"
                    System.out.println("using X11 text renderer");
                }
                x11textpipe = new X11TextRenderer();
                if (GraphicsPrimitive.tracingEnabled()) {
                    x11textpipe = ((X11TextRenderer) x11textpipe).traceWrap();
                }
            } else {
                if ("false".equals(xtextpipe)) {
                    // Only verbose if they use the full string "false"
                    System.out.println("using DGA text renderer");
                }
                x11textpipe = solidTextRenderer;
            }

            if (isAccelerationEnabled()) {
                X11PMBlitLoops.register();
                X11PMBlitBgLoops.register();
            }
       }
    }

    /**
     * Returns true if shared memory pixmaps are available
     */
    private static native boolean isShmPMAvailable();

    public static boolean isAccelerationEnabled() {
        if (accelerationEnabled == null) {

            if (GraphicsEnvironment.isHeadless()) {
                accelerationEnabled = Boolean.FALSE;
            } else {
                @SuppressWarnings("removal")
                String prop = java.security.AccessController.doPrivileged(
                        new sun.security.action.GetPropertyAction("sun.java2d.pmoffscreen"));
                if (prop != null) {
                    // true iff prop==true, false otherwise
                    accelerationEnabled = Boolean.valueOf(prop);
                } else {
                    boolean isDisplayLocal = false;
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    if (ge instanceof SunGraphicsEnvironment) {
                        isDisplayLocal = ((SunGraphicsEnvironment) ge).isDisplayLocal();
                     }

                    // EXA based drivers tend to place pixmaps in VRAM, slowing down readbacks.
                    // Don't use pixmaps if we are local and shared memory Pixmaps
                    // are not available.
                    accelerationEnabled = !(isDisplayLocal && !isShmPMAvailable());
                }
            }
        }
        return accelerationEnabled.booleanValue();
    }

    @Override
    public SurfaceDataProxy makeProxyFor(SurfaceData srcData) {
        return X11SurfaceDataProxy.createProxy(srcData, graphicsConfig);
    }

    public void validatePipe(SunGraphics2D sg2d) {
        if (sg2d.antialiasHint != SunHints.INTVAL_ANTIALIAS_ON &&
            sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR &&
            (sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY ||
             sg2d.compositeState == SunGraphics2D.COMP_XOR))
        {
            if (x11txpipe == null) {
                /*
                 * Note: this is thread-safe since x11txpipe is the
                 * second of the two pipes constructed in makePipes().
                 * In the rare case we are racing against another
                 * thread making new pipes, setting lazypipe is a
                 * safe alternative to waiting for the other thread.
                 */
                sg2d.drawpipe = lazypipe;
                sg2d.fillpipe = lazypipe;
                sg2d.shapepipe = lazypipe;
                sg2d.imagepipe = lazypipe;
                sg2d.textpipe = lazypipe;
                return;
            }

            if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                // Do this to init textpipe correctly; we will override the
                // other non-text pipes below
                // REMIND: we should clean this up eventually instead of
                // having this work duplicated.
                super.validatePipe(sg2d);
            } else {
                switch (sg2d.textAntialiasHint) {

                case SunHints.INTVAL_TEXT_ANTIALIAS_DEFAULT:
                    /* equating to OFF which it is for us */
                case SunHints.INTVAL_TEXT_ANTIALIAS_OFF:
                    // Use X11 pipe even if DGA is available since DGA
                    // text slows everything down when mixed with X11 calls
                    if (sg2d.compositeState == SunGraphics2D.COMP_ISCOPY) {
                        sg2d.textpipe = x11textpipe;
                    } else {
                        sg2d.textpipe = solidTextRenderer;
                    }
                    break;

                case SunHints.INTVAL_TEXT_ANTIALIAS_ON:
                    // Remind: may use Xrender for these when composite is
                    // copy as above, or if remote X11.
                    sg2d.textpipe = aaTextRenderer;
                    break;

                default:
                    switch (sg2d.getFontInfo().aaHint) {

                    case SunHints.INTVAL_TEXT_ANTIALIAS_LCD_HRGB:
                    case SunHints.INTVAL_TEXT_ANTIALIAS_LCD_VRGB:
                        sg2d.textpipe = lcdTextRenderer;
                        break;

                    case SunHints.INTVAL_TEXT_ANTIALIAS_OFF:
                    // Use X11 pipe even if DGA is available since DGA
                    // text slows everything down when mixed with X11 calls
                    if (sg2d.compositeState == SunGraphics2D.COMP_ISCOPY) {
                        sg2d.textpipe = x11textpipe;
                    } else {
                        sg2d.textpipe = solidTextRenderer;
                    }
                    break;

                    case SunHints.INTVAL_TEXT_ANTIALIAS_ON:
                        sg2d.textpipe = aaTextRenderer;
                        break;

                    default:
                        sg2d.textpipe = solidTextRenderer;
                    }
                }
            }

            if (sg2d.transformState >= SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
                sg2d.drawpipe = x11txpipe;
                sg2d.fillpipe = x11txpipe;
            } else if (sg2d.strokeState != SunGraphics2D.STROKE_THIN){
                sg2d.drawpipe = x11txpipe;
                sg2d.fillpipe = x11pipe;
            } else {
                sg2d.drawpipe = x11pipe;
                sg2d.fillpipe = x11pipe;
            }
            sg2d.shapepipe = x11pipe;
            sg2d.imagepipe = imagepipe;

            // This is needed for AA text.
            // Note that even an X11TextRenderer can dispatch AA text
            // if a GlyphVector overrides the AA setting.
            // We use getRenderLoops() rather than setting solidloops
            // directly so that we get the appropriate loops in XOR mode.
            if (sg2d.loops == null) {
                // assert(some pipe will always be a LoopBasedPipe)
                sg2d.loops = getRenderLoops(sg2d);
            }
        } else {
            super.validatePipe(sg2d);
        }
    }

    public RenderLoops getRenderLoops(SunGraphics2D sg2d) {
        if (sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR &&
            sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY)
        {
            return solidloops;
        }
        return super.getRenderLoops(sg2d);
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return graphicsConfig;
    }

    /**
     * Method for instantiating a Window SurfaceData
     */
    public static X11WindowSurfaceData createData(X11ComponentPeer peer) {
       X11GraphicsConfig gc = getGC(peer);
       return new X11WindowSurfaceData(peer, gc, gc.getSurfaceType());
    }

    /**
     * Method for instantiating a Pixmap SurfaceData (offscreen)
     */
    public static X11PixmapSurfaceData createData(X11GraphicsConfig gc,
                                                  int width, int height,
                                                  ColorModel cm, Image image,
                                                  long drawable,
                                                  int transparency,
                                                  boolean isTexture)
    {
        return new X11PixmapSurfaceData(gc, width, height, image,
                                        getSurfaceType(gc, transparency, true),
                                        cm, drawable, transparency, isTexture);
    }

//    /**
//     * Initializes the native Ops pointer.
//     */
//    private native void initOps(X11ComponentPeer peer,
//                                X11GraphicsConfig gc, int depth);

    protected X11SurfaceData(X11ComponentPeer peer,
                             X11GraphicsConfig gc,
                             SurfaceType sType,
                             ColorModel cm) {
        super(sType, cm);
        this.peer = peer;
        this.graphicsConfig = gc;
        this.solidloops = graphicsConfig.getSolidLoops(sType);
        this.depth = cm.getPixelSize();
        initOps(peer, graphicsConfig, depth);
        if (isAccelerationEnabled()) {
            setBlitProxyKey(gc.getProxyKey());
        }
    }

    public static X11GraphicsConfig getGC(X11ComponentPeer peer) {
        if (peer != null) {
            return (X11GraphicsConfig) peer.getGraphicsConfiguration();
        } else {
            GraphicsEnvironment env =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = env.getDefaultScreenDevice();
            return (X11GraphicsConfig)gd.getDefaultConfiguration();
        }
    }

    /**
     * Returns a boolean indicating whether or not a copyArea from
     * the given rectangle source coordinates might be incomplete
     * and result in X11 GraphicsExposure events being generated
     * from XCopyArea.
     * This method allows the SurfaceData copyArea method to determine
     * if it needs to set the GraphicsExposures attribute of the X11 GC
     * to True or False to receive or avoid the events.
     * @return true if there is any chance that an XCopyArea from the
     *              given source coordinates could produce any X11
     *              Exposure events.
     */
    public abstract boolean canSourceSendExposures(int x, int y, int w, int h);

    public boolean copyArea(SunGraphics2D sg2d,
                            int x, int y, int w, int h, int dx, int dy)
    {
        if (x11pipe == null) {
            if (!isDrawableValid()) {
                return true;
            }
            makePipes();
        }
        CompositeType comptype = sg2d.imageComp;
        if ((CompositeType.SrcOverNoEa.equals(comptype) ||
             CompositeType.SrcNoEa.equals(comptype)))
        {
            SunToolkit.awtLock();
            try {
                boolean needExposures = canSourceSendExposures(x, y, w, h);
                long xgc = getBlitGC(sg2d.getCompClip(), needExposures);
                x11pipe.devCopyArea(getNativeOps(), xgc,
                                    x, y,
                                    x + dx, y + dy,
                                    w, h);
            } finally {
                SunToolkit.awtUnlock();
            }
            return true;
        }
        return false;
    }

    public static SurfaceType getSurfaceType(X11GraphicsConfig gc,
                                             int transparency)
    {
        return getSurfaceType(gc, transparency, false);
    }

    @SuppressWarnings("fallthrough")
    public static SurfaceType getSurfaceType(X11GraphicsConfig gc,
                                             int transparency,
                                             boolean pixmapSurface)
    {
        boolean transparent = (transparency == Transparency.BITMASK);
        SurfaceType sType;
        ColorModel cm = gc.getColorModel();
        switch (cm.getPixelSize()) {
        case 24:
            if (gc.getBitsPerPixel() == 24) {
                if (cm instanceof DirectColorModel) {
                    // 4517321: We will always use ThreeByteBgr for 24 bpp
                    // surfaces, regardless of the pixel masks reported by
                    // X11.  Despite ambiguity in the X11 spec in how 24 bpp
                    // surfaces are treated, it appears that the best
                    // SurfaceType for these configurations (including
                    // some Matrox Millenium and ATI Radeon boards) is
                    // ThreeByteBgr.
                    sType = transparent ? X11SurfaceData.ThreeByteBgrX11_BM : X11SurfaceData.ThreeByteBgrX11;
                } else {
                    throw new sun.java2d.InvalidPipeException("Unsupported bit " +
                                                              "depth/cm combo: " +
                                                              cm.getPixelSize()  +
                                                              ", " + cm);
                }
                break;
            }
            // Fall through for 32 bit case
        case 32:
            if (cm instanceof DirectColorModel) {
                if (((SunToolkit)java.awt.Toolkit.getDefaultToolkit()
                     ).isTranslucencyCapable(gc) && !pixmapSurface)
                {
                    sType = X11SurfaceData.IntArgbPreX11;
                } else {
                    if (((DirectColorModel)cm).getRedMask() == 0xff0000) {
                        sType = transparent ? X11SurfaceData.IntRgbX11_BM :
                                              X11SurfaceData.IntRgbX11;
                    } else {
                        sType = transparent ? X11SurfaceData.IntBgrX11_BM :
                                              X11SurfaceData.IntBgrX11;
                    }
                }
            } else if (cm instanceof ComponentColorModel) {
                   sType = X11SurfaceData.FourByteAbgrPreX11;
            } else {

                throw new sun.java2d.InvalidPipeException("Unsupported bit " +
                                                          "depth/cm combo: " +
                                                          cm.getPixelSize()  +
                                                          ", " + cm);
            }
            break;
        case 15:
            sType = transparent ? X11SurfaceData.UShort555RgbX11_BM : X11SurfaceData.UShort555RgbX11;
            break;
        case 16:
            if ((cm instanceof DirectColorModel) &&
                (((DirectColorModel)cm).getGreenMask() == 0x3e0))
            {
                // fix for 4352984: Riva128 on Linux
                sType = transparent ? X11SurfaceData.UShort555RgbX11_BM : X11SurfaceData.UShort555RgbX11;
            } else {
                sType = transparent ? X11SurfaceData.UShort565RgbX11_BM : X11SurfaceData.UShort565RgbX11;
            }
            break;
        case  12:
            if (cm instanceof IndexColorModel) {
                sType = transparent ?
                    X11SurfaceData.UShortIndexedX11_BM :
                    X11SurfaceData.UShortIndexedX11;
            } else {
                throw new sun.java2d.InvalidPipeException("Unsupported bit " +
                                                          "depth: " +
                                                          cm.getPixelSize() +
                                                          " cm="+cm);
            }
            break;
        case 8:
            if (cm.getColorSpace().getType() == ColorSpace.TYPE_GRAY &&
                cm instanceof ComponentColorModel) {
                sType = transparent ? X11SurfaceData.ByteGrayX11_BM : X11SurfaceData.ByteGrayX11;
            } else if (cm instanceof IndexColorModel &&
                       isOpaqueGray((IndexColorModel)cm)) {
                sType = transparent ? X11SurfaceData.Index8GrayX11_BM : X11SurfaceData.Index8GrayX11;
            } else {
                sType = transparent ? X11SurfaceData.ByteIndexedX11_BM : X11SurfaceData.ByteIndexedOpaqueX11;
            }
            break;
        default:
            throw new sun.java2d.InvalidPipeException("Unsupported bit " +
                                                      "depth: " +
                                                      cm.getPixelSize());
        }
        return sType;
    }

    public void invalidate() {
        if (isValid()) {
            setInvalid();
            super.invalidate();
        }
    }

    /**
     * The following methods and variables are used to keep the Java-level
     * context state in sync with the native X11 GC associated with this
     * X11SurfaceData object.
     */

    private static native void XSetCopyMode(long xgc);
    private static native void XSetXorMode(long xgc);
    private static native void XSetForeground(long xgc, int pixel);

    private long xgc;
    private Region validatedClip;
    private XORComposite validatedXorComp;
    private int xorpixelmod;
    private int validatedPixel;
    private boolean validatedExposures = true;

    public final long getRenderGC(Region clip,
                                  int compState, Composite comp,
                                  int pixel)
    {
        return getGC(clip, compState, comp, pixel, validatedExposures);
    }

    public final long getBlitGC(Region clip, boolean needExposures) {
        return getGC(clip, SunGraphics2D.COMP_ISCOPY, null,
                     validatedPixel, needExposures);
    }

    final long getGC(Region clip,
                     int compState, Composite comp,
                     int pixel, boolean needExposures)
    {
        // assert SunToolkit.isAWTLockHeldByCurrentThread();

        if (!isValid()) {
            throw new InvalidPipeException("bounds changed");
        }

        // validate clip
        if (clip != validatedClip) {
            validatedClip = clip;
            if (clip != null) {
                XSetClip(xgc,
                         clip.getLoX(), clip.getLoY(),
                         clip.getHiX(), clip.getHiY(),
                         (clip.isRectangular() ? null : clip));
            } else {
                XResetClip(xgc);
            }
        }

        // validate composite
        if (compState == SunGraphics2D.COMP_ISCOPY) {
            if (validatedXorComp != null) {
                validatedXorComp = null;
                xorpixelmod = 0;
                XSetCopyMode(xgc);
            }
        } else {
            if (validatedXorComp != comp) {
                validatedXorComp = (XORComposite)comp;
                xorpixelmod = validatedXorComp.getXorPixel();
                XSetXorMode(xgc);
            }
        }

        // validate pixel
        pixel ^= xorpixelmod;
        if (pixel != validatedPixel) {
            validatedPixel = pixel;
            XSetForeground(xgc, pixel);
        }

        if (validatedExposures != needExposures) {
            validatedExposures = needExposures;
            XSetGraphicsExposures(xgc, needExposures);
        }

        return xgc;
    }

    public synchronized void makePipes() {
        if (x11pipe == null) {
            SunToolkit.awtLock();
            try {
                xgc = XCreateGC(getNativeOps());
            } finally {
                SunToolkit.awtUnlock();
            }
            x11pipe = X11Renderer.getInstance();
            x11txpipe = new PixelToShapeConverter(x11pipe);
        }
    }

    private static final class X11WindowSurfaceData extends X11SurfaceData {

        private final int scale;

        public X11WindowSurfaceData(X11ComponentPeer peer,
                                    X11GraphicsConfig gc,
                                    SurfaceType sType) {
            super(peer, gc, sType, peer.getColorModel());
            this.scale = gc.getDevice().getScaleFactor();
            if (isDrawableValid()) {
                makePipes();
            }
        }

        public SurfaceData getReplacement() {
            return peer.getSurfaceData();
        }

        public Rectangle getBounds() {
            Rectangle r = peer.getBounds();
            r.x = r.y = 0;
            r.width *= scale;
            r.height *= scale;
            return r;
        }

        @Override
        public boolean canSourceSendExposures(int x, int y, int w, int h) {
            return true;
        }

        /**
         * Returns destination Component associated with this SurfaceData.
         */
        public Object getDestination() {
            return peer.getTarget();
        }

        @Override
        public double getDefaultScaleX() {
            return scale;
        }

        @Override
        public double getDefaultScaleY() {
            return scale;
        }
    }

    private static final class X11PixmapSurfaceData extends X11SurfaceData {

        private final Image offscreenImage;
        private final int width;
        private final int height;
        private final int transparency;
        private final int scale;

        public X11PixmapSurfaceData(X11GraphicsConfig gc,
                                    int width, int height,
                                    Image image,
                                    SurfaceType sType, ColorModel cm,
                                    long drawable, int transparency,
                                    boolean isTexture)
        {
            super(null, gc, sType, cm);
            this.scale = isTexture ? 1 : gc.getDevice().getScaleFactor();
            this.width = width * scale;
            this.height = height * scale;
            offscreenImage = image;
            this.transparency = transparency;
            initSurface(depth, this.width, this.height, drawable);
            makePipes();
        }

        public SurfaceData getReplacement() {
            return restoreContents(offscreenImage);
        }

        /**
         * Need this since the surface data is created with
         * the color model of the target GC, which is always
         * opaque. But in SunGraphics2D.blitSD we choose loops
         * based on the transparency on the source SD, so
         * it could choose wrong loop (blit instead of blitbg,
         * for example).
         */
        public int getTransparency() {
            return transparency;
        }

        public Rectangle getBounds() {
            return new Rectangle(width, height);
        }

        @Override
        public boolean canSourceSendExposures(int x, int y, int w, int h) {
            return (x < 0 || y < 0 || (x+w) > width || (y+h) > height);
        }

        public void flush() {
            /*
             * We need to invalidate the surface before disposing the
             * native Drawable and GC.  This way if an application tries
             * to render to an already flushed X11SurfaceData, we will notice
             * in the validate() method above that it has been invalidated,
             * and we will avoid using those native resources that have
             * already been disposed.
             */
            invalidate();
            flushNativeSurface();
        }

        /**
         * Returns destination Image associated with this SurfaceData.
         */
        public Object getDestination() {
            return offscreenImage;
        }

        @Override
        public double getDefaultScaleX() {
            return scale;
        }

        @Override
        public double getDefaultScaleY() {
            return scale;
        }
    }

    private static LazyPipe lazypipe = new LazyPipe();

    public static class LazyPipe extends ValidatePipe {
        public boolean validate(SunGraphics2D sg2d) {
            X11SurfaceData xsd = (X11SurfaceData) sg2d.surfaceData;
            if (!xsd.isDrawableValid()) {
                return false;
            }
            xsd.makePipes();
            return super.validate(sg2d);
        }
    }
}
