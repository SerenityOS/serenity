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
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.nio.*;

import sun.awt.*;
import sun.awt.image.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.*;
import sun.lwawt.macosx.*;

import java.lang.annotation.Native;

/*
 * This is the SurfaceData for a CGContextRef.
 */
public abstract class OSXSurfaceData extends BufImgSurfaceData {
    static final float UPPER_BND = Float.MAX_VALUE / 2.0f;
    static final float LOWER_BND = -UPPER_BND;

    protected static CRenderer sQuartzPipe = null;
    protected static CTextPipe sCocoaTextPipe = null;
    protected static CompositeCRenderer sQuartzCompositePipe = null;

    private GraphicsConfiguration fConfig;
    private Rectangle fBounds; // bounds in user coordinates

    static {
        sQuartzPipe = new CRenderer(); // Creates the singleton quartz pipe.
    }

    // NOTE: Any subclasses must eventually call QuartzSurfaceData_InitOps in OSXSurfaceData.h
    // This sets up the native side for the SurfaceData, and is required.
    public OSXSurfaceData(SurfaceType sType, ColorModel cm) {
        this(sType, cm, null, new Rectangle());
    }

    public OSXSurfaceData(SurfaceType sType, ColorModel cm, GraphicsConfiguration config, Rectangle bounds) {
        super(sType, cm);

        this.fConfig = config;

        this.fBounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.y + bounds.height);

        this.fGraphicsStates = getBufferOfSize(kSizeOfParameters);
        this.fGraphicsStatesInt = this.fGraphicsStates.asIntBuffer();
        this.fGraphicsStatesFloat = this.fGraphicsStates.asFloatBuffer();
        this.fGraphicsStatesLong = this.fGraphicsStates.asLongBuffer();
        this.fGraphicsStatesObject = new Object[8]; // clip coordinates +
                                                    // clip types +
                                                    // texture paint image +
                                                    // stroke dash array +
                                                    // font + font paint +
                                                    // linear/radial gradient color +
                                                    // linear/radial gradient fractions

        // NOTE: All access to the DrawingQueue comes through this OSXSurfaceData instance. Therefore
        // every instance method of OSXSurfaceData that accesses the fDrawingQueue is synchronized.

        // Thread.dumpStack();
    }

    public void validatePipe(SunGraphics2D sg2d) {

        if (sg2d.compositeState <= SunGraphics2D.COMP_ALPHA) {
            if (sCocoaTextPipe == null) {
                sCocoaTextPipe = new CTextPipe();
            }

            sg2d.imagepipe = sQuartzPipe;
            sg2d.drawpipe = sQuartzPipe;
            sg2d.fillpipe = sQuartzPipe;
            sg2d.shapepipe = sQuartzPipe;
            sg2d.textpipe = sCocoaTextPipe;
        } else {
            setPipesToQuartzComposite(sg2d);
        }
    }

    protected void setPipesToQuartzComposite(SunGraphics2D sg2d) {
        if (sQuartzCompositePipe == null) {
            sQuartzCompositePipe = new CompositeCRenderer();
        }

        if (sCocoaTextPipe == null) {
            sCocoaTextPipe = new CTextPipe();
        }

        sg2d.imagepipe = sQuartzCompositePipe;
        sg2d.drawpipe = sQuartzCompositePipe;
        sg2d.fillpipe = sQuartzCompositePipe;
        sg2d.shapepipe = sQuartzCompositePipe;
        sg2d.textpipe = sCocoaTextPipe;
    }

    public Rectangle getBounds() {
        // gznote: always return a copy, not the rect itself and translate into device space
        return new Rectangle(fBounds.x, fBounds.y, fBounds.width, fBounds.height - fBounds.y);
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return fConfig;
    }

    protected void setBounds(int x, int y, int w, int h) {
        fBounds.setBounds(x, y, w, y + h);
    }

    // START compositing support API
    public abstract BufferedImage copyArea(SunGraphics2D sg2d, int x, int y, int w, int h, BufferedImage image);

    public abstract boolean xorSurfacePixels(SunGraphics2D sg2d, BufferedImage srcPixels, int x, int y, int w, int h, int colorXOR);

    GraphicsConfiguration sDefaultGraphicsConfiguration = null;

    protected BufferedImage getCompositingImage(int w, int h) {
        if (sDefaultGraphicsConfiguration == null) {
            sDefaultGraphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        // clear the image.
        clearRect(img, w, h);
        return img;
    }

    protected BufferedImage getCompositingImageSame(BufferedImage img, int w, int h) {
        if ((img == null) || (img.getWidth() != w) || (img.getHeight() != h)) {
            img = getCompositingImage(w, h);
        }
        return img;
    }

    BufferedImage sSrcComposite = null;

    public BufferedImage getCompositingSrcImage(int w, int h) {
        // <rdar://problem/3720263>. Changed from getCompositingImageBiggerOrSame() to
        // getCompositingImageSame(). (vm)
        BufferedImage bim = getCompositingImageSame(sSrcComposite, w, h);
        sSrcComposite = bim;
        return bim;
    }

    BufferedImage sDstInComposite = null;

    public BufferedImage getCompositingDstInImage(int w, int h) {
        BufferedImage bim = getCompositingImageSame(sDstInComposite, w, h);
        sDstInComposite = bim;
        return bim;
    }

    BufferedImage sDstOutComposite = null;

    public BufferedImage getCompositingDstOutImage(int w, int h) {
        BufferedImage bim = getCompositingImageSame(sDstOutComposite, w, h);
        sDstOutComposite = bim;
        return bim;
    }

    public void clearRect(BufferedImage bim, int w, int h) {
        Graphics2D g = bim.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, w, h);
        g.dispose();
    }

    // END compositing support API

    public void invalidate() {
        // always valid
    }

     // graphics primitives drawing implementation:

    // certain primitives don't care about all the states (ex. drawing an image needs not involve setting current paint)
    @Native static final int kPrimitive = 0;
    @Native static final int kImage = 1;
    @Native static final int kText = 2;
    @Native static final int kCopyArea = 3;
    @Native static final int kExternal = 4;

    @Native static final int kLine = 5; // belongs to kPrimitive
    @Native static final int kRect = 6; // belongs to kPrimitive
    @Native static final int kRoundRect = 7; // belongs to kPrimitive
    @Native static final int kOval = 8; // belongs to kPrimitive
    @Native static final int kArc = 9; // belongs to kPrimitive
    @Native static final int kPolygon = 10; // belongs to kPrimitive
    @Native static final int kShape = 11; // belongs to kPrimitive
    // static final int kImage = 12; // belongs to kImage
    @Native static final int kString = 13; // belongs to kText
    @Native static final int kGlyphs = 14; // belongs to kText
    @Native static final int kUnicodes = 15; // belongs to kText
    // static final int kCopyArea = 16; // belongs to kCopyArea
    // static final int kExternal = 17; // belongs to kExternal

    @Native static final int kCommonParameterCount = 1 + 1 + 4 + 4; // type + change flags + color info (type(1) align(1) and
                                                            // value(2)) + parameters ((x1, y1, x2, y2) OR (x, y, w, h))
    @Native static final int kLineParametersCount = kCommonParameterCount; // kCommonParameterCount
    @Native static final int kRectParametersCount = kCommonParameterCount + 1; // kCommonParameterCount + isfill
    @Native static final int kRoundRectParametersCount = kCommonParameterCount + 2 + 1; // kCommonParameterCount + arcW + arcH +
                                                                                // isfill
    @Native static final int kOvalParametersCount = kCommonParameterCount + 1; // kCommonParameterCount + isfill
    @Native static final int kArcParametersCount = kCommonParameterCount + 2 + 1 + 1;// kCommonParameterCount + startAngle +
                                                                             // arcAngle + isfill + type
    @Native static final int kPolygonParametersCount = 0; // not supported
    @Native static final int kShapeParametersCount = 0; // not supported
    @Native static final int kImageParametersCount = kCommonParameterCount + 2 + 2 + 4 + 4; // flip horz vert + w&h + src + dst
    @Native static final int kStringParametersCount = 0; // not supported
    @Native static final int kGlyphsParametersCount = 0; // not supported
    @Native static final int kUnicodesParametersCount = 0; // not supported
    @Native static final int kPixelParametersCount = 0; // not supported
    @Native static final int kExternalParametersCount = 0; // not supported

    // for intParameters
    // states info
    @Native static final int kChangeFlagIndex = 0; // kBoundsChangedBit | .. | kFontChangedBit
    // bounds info
    @Native static final int kBoundsXIndex = 1;
    @Native static final int kBoundsYIndex = 2;
    @Native static final int kBoundsWidthIndex = 3;
    @Native static final int kBoundsHeightIndex = 4;
    // clip info
    @Native static final int kClipStateIndex = 5;
    @Native static final int kClipNumTypesIndex = 6;
    @Native static final int kClipNumCoordsIndex = 7;
    @Native static final int kClipWindingRuleIndex = 8;
    @Native static final int kClipXIndex = 9;
    @Native static final int kClipYIndex = 10;
    @Native static final int kClipWidthIndex = 11;
    @Native static final int kClipHeightIndex = 12;
    // ctm info
    @Native static final int kCTMaIndex = 13;
    @Native static final int kCTMbIndex = 14;
    @Native static final int kCTMcIndex = 15;
    @Native static final int kCTMdIndex = 16;
    @Native static final int kCTMtxIndex = 17;
    @Native static final int kCTMtyIndex = 18;
    // color info
    @Native static final int kColorStateIndex = 19; // kColorSimple or kColorGradient or kColorTexture
    @Native static final int kColorRGBValueIndex = 20; // if kColorSimple
    @Native static final int kColorIndexValueIndex = 21; // if kColorSystem
    @Native static final int kColorPointerIndex = 22; //
    @Native static final int kColorPointerIndex2 = 23; //
    @Native static final int kColorRGBValue1Index = 24; // if kColorGradient
    @Native static final int kColorWidthIndex = 25; // if kColorTexture
    @Native static final int kColorRGBValue2Index = 26; // if kColorGradient
    @Native static final int kColorHeightIndex = 27; // if kColorTexture
    @Native static final int kColorIsCyclicIndex = 28; // if kColorGradient (kColorNonCyclic or kColorCyclic)
    @Native static final int kColorx1Index = 29;
    @Native static final int kColortxIndex = 30;
    @Native static final int kColory1Index = 31;
    @Native static final int kColortyIndex = 32;
    @Native static final int kColorx2Index = 33;
    @Native static final int kColorsxIndex = 34;
    @Native static final int kColory2Index = 35;
    @Native static final int kColorsyIndex = 36;
    // composite info
    @Native static final int kCompositeRuleIndex = 37; // kCGCompositeClear or ... or kCGCompositeXor
    @Native static final int kCompositeValueIndex = 38;
    // stroke info
    @Native static final int kStrokeJoinIndex = 39; // see BasicStroke.java
    @Native static final int kStrokeCapIndex = 40; // see BasicStroke.java
    @Native static final int kStrokeWidthIndex = 41;
    @Native static final int kStrokeDashPhaseIndex = 42;
    @Native static final int kStrokeLimitIndex = 43;
    // hints info
    @Native static final int kHintsAntialiasIndex = 44;
    @Native static final int kHintsTextAntialiasIndex = 45;
    @Native static final int kHintsFractionalMetricsIndex = 46;
    @Native static final int kHintsRenderingIndex = 47;
    @Native static final int kHintsInterpolationIndex = 48;
    //gradient info
    @Native static final int kRadiusIndex = 49;

    @Native static final int kSizeOfParameters = kRadiusIndex + 1;

    // for objectParameters
    @Native static final int kClipCoordinatesIndex = 0;
    @Native static final int kClipTypesIndex = 1;
    @Native static final int kTextureImageIndex = 2;
    @Native static final int kStrokeDashArrayIndex = 3;
    @Native static final int kFontIndex = 4;
    @Native static final int kFontPaintIndex = 5;
    @Native static final int kColorArrayIndex = 6;
    @Native static final int kFractionsArrayIndex = 7;

    // possible state changes
    @Native static final int kBoundsChangedBit = 1 << 0;
    @Native static final int kBoundsNotChangedBit = ~kBoundsChangedBit;
    @Native static final int kClipChangedBit = 1 << 1;
    @Native static final int kClipNotChangedBit = ~kClipChangedBit;
    @Native static final int kCTMChangedBit = 1 << 2;
    @Native static final int kCTMNotChangedBit = ~kCTMChangedBit;
    @Native static final int kColorChangedBit = 1 << 3;
    @Native static final int kColorNotChangedBit = ~kColorChangedBit;
    @Native static final int kCompositeChangedBit = 1 << 4;
    @Native static final int kCompositeNotChangedBit = ~kCompositeChangedBit;
    @Native static final int kStrokeChangedBit = 1 << 5;
    @Native static final int kStrokeNotChangedBit = ~kStrokeChangedBit;
    @Native static final int kHintsChangedBit = 1 << 6;
    @Native static final int kHintsNotChangedBit = ~kHintsChangedBit;
    @Native static final int kFontChangedBit = 1 << 7;
    @Native static final int kFontNotChangedBit = ~kFontChangedBit;
    @Native static final int kEverythingChangedFlag = 0xffffffff;

    // possible color states
    @Native static final int kColorSimple = 0;
    @Native static final int kColorSystem = 1;
    @Native static final int kColorGradient = 2;
    @Native static final int kColorTexture = 3;
    @Native static final int kColorLinearGradient = 4;
    @Native static final int kColorRadialGradient = 5;

    // possible gradient color states
    @Native static final int kColorNonCyclic = 0;
    @Native static final int kColorCyclic = 1;

    // possible clip states
    @Native static final int kClipRect = 0;
    @Native static final int kClipShape = 1;

    static int getRendererTypeForPrimitive(int primitiveType) {
        switch (primitiveType) {
            case kImage:
                return kImage;
            case kCopyArea:
                return kCopyArea;
            case kExternal:
                return kExternal;
            case kString:
            case kGlyphs:
            case kUnicodes:
                return kText;
            default:
                return kPrimitive;
        }
    }

    int fChangeFlag;
    protected ByteBuffer fGraphicsStates = null;
    IntBuffer fGraphicsStatesInt = null;
    FloatBuffer fGraphicsStatesFloat = null;
    LongBuffer fGraphicsStatesLong = null;
    protected Object[] fGraphicsStatesObject = null;

    Rectangle userBounds = new Rectangle();
    float lastUserX = 0;
    float lastUserY = 0;
    float lastUserW = 0;
    float lastUserH = 0;

    void setUserBounds(SunGraphics2D sg2d, int x, int y, int width, int height) {
        if ((lastUserX != x) || (lastUserY != y) || (lastUserW != width) || (lastUserH != height)) {
            lastUserX = x;
            lastUserY = y;
            lastUserW = width;
            lastUserH = height;

            this.fGraphicsStatesInt.put(kBoundsXIndex, x);
            this.fGraphicsStatesInt.put(kBoundsYIndex, y);
            this.fGraphicsStatesInt.put(kBoundsWidthIndex, width);
            this.fGraphicsStatesInt.put(kBoundsHeightIndex, height);

            userBounds.setBounds(x, y, width, height);

            this.fChangeFlag = (this.fChangeFlag | kBoundsChangedBit);
        } else {
            this.fChangeFlag = (this.fChangeFlag & kBoundsNotChangedBit);
        }
    }

    static ByteBuffer getBufferOfSize(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    FloatBuffer clipCoordinatesArray = null;
    IntBuffer clipTypesArray = null;
    Shape lastClipShape = null;
    float lastClipX = 0;
    float lastClipY = 0;
    float lastClipW = 0;
    float lastClipH = 0;

    void setupClip(SunGraphics2D sg2d) {
        switch (sg2d.clipState) {
            case SunGraphics2D.CLIP_DEVICE:
            case SunGraphics2D.CLIP_RECTANGULAR: {
                Region clip = sg2d.getCompClip();
                float x = clip.getLoX();
                float y = clip.getLoY();
                float w = clip.getWidth();
                float h = clip.getHeight();
                if ((this.fGraphicsStatesInt.get(kClipStateIndex) != kClipRect) ||
                        (x != lastClipX) ||
                            (y != lastClipY) ||
                                (w != lastClipW) ||
                                    (h != lastClipH)) {
                    this.fGraphicsStatesFloat.put(kClipXIndex, x);
                    this.fGraphicsStatesFloat.put(kClipYIndex, y);
                    this.fGraphicsStatesFloat.put(kClipWidthIndex, w);
                    this.fGraphicsStatesFloat.put(kClipHeightIndex, h);

                    lastClipX = x;
                    lastClipY = y;
                    lastClipW = w;
                    lastClipH = h;

                    this.fChangeFlag = (this.fChangeFlag | kClipChangedBit);
                } else {
                    this.fChangeFlag = (this.fChangeFlag & kClipNotChangedBit);
                }
                this.fGraphicsStatesInt.put(kClipStateIndex, kClipRect);
                break;
            }
            case SunGraphics2D.CLIP_SHAPE: {
                // if (lastClipShape != sg2d.usrClip) shapes are mutable!, and doing "equals" traverses all
                // the coordinates, so we might as well do all of it anyhow
                lastClipShape = sg2d.usrClip;

                GeneralPath gp = null;

                if (sg2d.usrClip instanceof GeneralPath) {
                    gp = (GeneralPath) sg2d.usrClip;
                } else {
                    gp = new GeneralPath(sg2d.usrClip);
                }

                int shapeLength = getPathLength(gp);

                if ((clipCoordinatesArray == null) || (clipCoordinatesArray.capacity() < (shapeLength * 6))) {
                    clipCoordinatesArray = getBufferOfSize(shapeLength * 6).asFloatBuffer(); // segment can have a
                                                                                             // max of 6 coordinates
                }
                if ((clipTypesArray == null) || (clipTypesArray.capacity() < shapeLength)) {
                    clipTypesArray = getBufferOfSize(shapeLength).asIntBuffer();
                }

                int windingRule = getPathCoordinates(gp, clipCoordinatesArray, clipTypesArray);

                this.fGraphicsStatesInt.put(kClipNumTypesIndex, clipTypesArray.position());
                this.fGraphicsStatesInt.put(kClipNumCoordsIndex, clipCoordinatesArray.position());
                this.fGraphicsStatesInt.put(kClipWindingRuleIndex, windingRule);
                this.fGraphicsStatesObject[kClipTypesIndex] = clipTypesArray;
                this.fGraphicsStatesObject[kClipCoordinatesIndex] = clipCoordinatesArray;

                this.fChangeFlag = (this.fChangeFlag | kClipChangedBit);
                this.fGraphicsStatesInt.put(kClipStateIndex, kClipShape);
                break;
            }
        }

    }

    final double[] lastCTM = new double[6];
    float lastCTMa = 0;
    float lastCTMb = 0;
    float lastCTMc = 0;
    float lastCTMd = 0;
    float lastCTMtx = 0;
    float lastCTMty = 0;

    void setupTransform(SunGraphics2D sg2d) {
        sg2d.transform.getMatrix(lastCTM);

        float a = (float) lastCTM[0];
        float b = (float) lastCTM[1];
        float c = (float) lastCTM[2];
        float d = (float) lastCTM[3];
        float tx = (float) lastCTM[4];
        float ty = (float) lastCTM[5];
        if (tx != lastCTMtx ||
                ty != lastCTMty ||
                    a != lastCTMa ||
                        b != lastCTMb ||
                            c != lastCTMc ||
                                d != lastCTMd) {
            this.fGraphicsStatesFloat.put(kCTMaIndex, a);
            this.fGraphicsStatesFloat.put(kCTMbIndex, b);
            this.fGraphicsStatesFloat.put(kCTMcIndex, c);
            this.fGraphicsStatesFloat.put(kCTMdIndex, d);
            this.fGraphicsStatesFloat.put(kCTMtxIndex, tx);
            this.fGraphicsStatesFloat.put(kCTMtyIndex, ty);

            lastCTMa = a;
            lastCTMb = b;
            lastCTMc = c;
            lastCTMd = d;
            lastCTMtx = tx;
            lastCTMty = ty;

            this.fChangeFlag = (this.fChangeFlag | kCTMChangedBit);
        } else {
            this.fChangeFlag = (this.fChangeFlag & kCTMNotChangedBit);
        }
    }

    static AffineTransform sIdentityMatrix = new AffineTransform();
    Paint lastPaint = null;
    long lastPaintPtr = 0;
    int lastPaintRGB = 0;
    int lastPaintIndex = 0;
    BufferedImage texturePaintImage = null;

    void setGradientViaRasterPath(SunGraphics2D sg2d) {
        if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorTexture) || (lastPaint != sg2d.paint) || ((this.fChangeFlag & kBoundsChangedBit) != 0)) {
            PaintContext context = sg2d.paint.createContext(sg2d.getDeviceColorModel(), userBounds, userBounds, sIdentityMatrix, sg2d.getRenderingHints());
            WritableRaster raster = (WritableRaster) (context.getRaster(userBounds.x, userBounds.y, userBounds.width, userBounds.height));
            ColorModel cm = context.getColorModel();
            texturePaintImage = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

            this.fGraphicsStatesInt.put(kColorStateIndex, kColorTexture);
            this.fGraphicsStatesInt.put(kColorWidthIndex, texturePaintImage.getWidth());
            this.fGraphicsStatesInt.put(kColorHeightIndex, texturePaintImage.getHeight());
            this.fGraphicsStatesFloat.put(kColortxIndex, (float) userBounds.getX());
            this.fGraphicsStatesFloat.put(kColortyIndex, (float) userBounds.getY());
            this.fGraphicsStatesFloat.put(kColorsxIndex, 1.0f);
            this.fGraphicsStatesFloat.put(kColorsyIndex, 1.0f);
            this.fGraphicsStatesObject[kTextureImageIndex] = OSXOffScreenSurfaceData.createNewSurface(texturePaintImage);

            this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
        } else {
            this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
        }
    }

    void setupPaint(SunGraphics2D sg2d, int x, int y, int w, int h) {
        if (sg2d.paint instanceof SystemColor) {
            SystemColor color = (SystemColor) sg2d.paint;
            int index = color.hashCode(); // depends on Color.java hashCode implementation! (returns "value" of color)
            if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorSystem) || (index != this.lastPaintIndex)) {
                this.lastPaintIndex = index;

                this.fGraphicsStatesInt.put(kColorStateIndex, kColorSystem);
                this.fGraphicsStatesInt.put(kColorIndexValueIndex, index);

                this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
            } else {
                this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
            }
        } else if (sg2d.paint instanceof Color) {
            Color color = (Color) sg2d.paint;
            int rgb = color.getRGB();
            if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorSimple) || (rgb != this.lastPaintRGB)) {
                this.lastPaintRGB = rgb;

                this.fGraphicsStatesInt.put(kColorStateIndex, kColorSimple);
                this.fGraphicsStatesInt.put(kColorRGBValueIndex, rgb);

                this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
            } else {
                this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
            }
        } else if (sg2d.paint instanceof GradientPaint) {
            if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorGradient) || (lastPaint != sg2d.paint)) {
                GradientPaint color = (GradientPaint) sg2d.paint;
                this.fGraphicsStatesInt.put(kColorStateIndex, kColorGradient);
                this.fGraphicsStatesInt.put(kColorRGBValue1Index, color.getColor1().getRGB());
                this.fGraphicsStatesInt.put(kColorRGBValue2Index, color.getColor2().getRGB());
                this.fGraphicsStatesInt.put(kColorIsCyclicIndex, (color.isCyclic()) ? kColorCyclic : kColorNonCyclic);
                Point2D p = color.getPoint1();
                this.fGraphicsStatesFloat.put(kColorx1Index, (float) p.getX());
                this.fGraphicsStatesFloat.put(kColory1Index, (float) p.getY());
                p = color.getPoint2();
                this.fGraphicsStatesFloat.put(kColorx2Index, (float) p.getX());
                this.fGraphicsStatesFloat.put(kColory2Index, (float) p.getY());

                this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
            } else {
                this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
            }
        } else if (sg2d.paint instanceof LinearGradientPaint) {
            LinearGradientPaint color = (LinearGradientPaint) sg2d.paint;
            if (color.getCycleMethod() == LinearGradientPaint.CycleMethod.NO_CYCLE) {
                if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorLinearGradient) || (lastPaint != sg2d.paint)) {

                    this.fGraphicsStatesInt.put(kColorStateIndex, kColorLinearGradient);
                    int numColor = color.getColors().length;
                    int[] colorArray = new int[numColor];
                    for (int i = 0; i < numColor; i++) {
                        colorArray[i] = color.getColors()[i].getRGB();
                    }
                    this.fGraphicsStatesObject[kColorArrayIndex] = colorArray;

                    int numFractions = color.getFractions().length;
                    float[] fractionArray = new float[numFractions];
                    for (int i = 0; i < numFractions; i++) {
                        fractionArray[i] = color.getFractions()[i];
                    }
                    this.fGraphicsStatesObject[kFractionsArrayIndex] = color.getFractions();

                    Point2D p = color.getStartPoint();
                    this.fGraphicsStatesFloat.put(kColorx1Index, (float) p.getX());
                    this.fGraphicsStatesFloat.put(kColory1Index, (float) p.getY());
                    p = color.getEndPoint();
                    this.fGraphicsStatesFloat.put(kColorx2Index, (float) p.getX());
                    this.fGraphicsStatesFloat.put(kColory2Index, (float) p.getY());

                    this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
                } else {
                    this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
                }
            } else {
                setGradientViaRasterPath(sg2d);
            }
        } else if (sg2d.paint instanceof RadialGradientPaint) {
            RadialGradientPaint color = (RadialGradientPaint) sg2d.paint;
            if (color.getCycleMethod() == RadialGradientPaint.CycleMethod.NO_CYCLE) {
                if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorRadialGradient) || (lastPaint != sg2d.paint)) {

                    this.fGraphicsStatesInt.put(kColorStateIndex, kColorRadialGradient);
                    int numColor = color.getColors().length;
                    int[] colorArray = new int[numColor];
                    for (int i = 0; i < numColor; i++) {
                        colorArray[i] = color.getColors()[i].getRGB();
                    }
                    this.fGraphicsStatesObject[kColorArrayIndex] = colorArray;

                    int numStops = color.getFractions().length;
                    float[] stopsArray = new float[numStops];
                    for (int i = 0; i < numStops; i++) {
                        stopsArray[i] = color.getFractions()[i];
                    }
                    this.fGraphicsStatesObject[kFractionsArrayIndex] = color.getFractions();

                    Point2D p = color.getFocusPoint();
                    this.fGraphicsStatesFloat.put(kColorx1Index, (float) p.getX());
                    this.fGraphicsStatesFloat.put(kColory1Index, (float) p.getY());
                    p = color.getCenterPoint();
                    this.fGraphicsStatesFloat.put(kColorx2Index, (float) p.getX());
                    this.fGraphicsStatesFloat.put(kColory2Index, (float) p.getY());
                    this.fGraphicsStatesFloat.put(kRadiusIndex,     color.getRadius());

                    this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
                } else {
                    this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
                }
            } else {
                setGradientViaRasterPath(sg2d);
            }
        } else if (sg2d.paint instanceof TexturePaint) {
            if ((this.fGraphicsStatesInt.get(kColorStateIndex) != kColorTexture) || (lastPaint != sg2d.paint)) {
                TexturePaint color = (TexturePaint) sg2d.paint;
                this.fGraphicsStatesInt.put(kColorStateIndex, kColorTexture);
                texturePaintImage = color.getImage();
                SurfaceData textureSurfaceData = OSXOffScreenSurfaceData.createNewSurface(texturePaintImage);
                this.fGraphicsStatesInt.put(kColorWidthIndex, texturePaintImage.getWidth());
                this.fGraphicsStatesInt.put(kColorHeightIndex, texturePaintImage.getHeight());
                Rectangle2D anchor = color.getAnchorRect();
                this.fGraphicsStatesFloat.put(kColortxIndex, (float) anchor.getX());
                this.fGraphicsStatesFloat.put(kColortyIndex, (float) anchor.getY());
                this.fGraphicsStatesFloat.put(kColorsxIndex, (float) (anchor.getWidth() / texturePaintImage.getWidth()));
                this.fGraphicsStatesFloat.put(kColorsyIndex, (float) (anchor.getHeight() / texturePaintImage.getHeight()));
                this.fGraphicsStatesObject[kTextureImageIndex] = textureSurfaceData;

                this.fChangeFlag = (this.fChangeFlag | kColorChangedBit);
            } else {
                this.fChangeFlag = (this.fChangeFlag & kColorNotChangedBit);
            }
        } else {
            setGradientViaRasterPath(sg2d);
        }
        lastPaint = sg2d.paint;
    }

    Composite lastComposite;
    int lastCompositeAlphaRule = 0;
    float lastCompositeAlphaValue = 0;

    void setupComposite(SunGraphics2D sg2d) {
        Composite composite = sg2d.composite;

        if (lastComposite != composite) {
            lastComposite = composite;

            // For composite state COMP_ISCOPY, COMP_XOR or COMP_CUSTOM set alpha compositor to COPY:
            int alphaRule = AlphaComposite.SRC_OVER;
            float alphaValue = 1.0f;

            // For composite state COMP_ISCOPY composite could be null. If it's not (or composite state == COMP_ALPHA)
            // get alpha compositor's values:
            if ((sg2d.compositeState <= SunGraphics2D.COMP_ALPHA) && (composite != null)) {
                AlphaComposite alphaComposite = (AlphaComposite) composite;
                alphaRule = alphaComposite.getRule();
                alphaValue = alphaComposite.getAlpha();
            }

            // 2-17-03 VL: [Radar 3174922]
            // For COMP_XOR and COMP_CUSTOM compositing modes we should be setting alphaRule = AlphaComposite.SRC
            // which should map to kCGCompositeCopy.

            if ((lastCompositeAlphaRule != alphaRule) || (lastCompositeAlphaValue != alphaValue)) {
                this.fGraphicsStatesInt.put(kCompositeRuleIndex, alphaRule);
                this.fGraphicsStatesFloat.put(kCompositeValueIndex, alphaValue);

                lastCompositeAlphaRule = alphaRule;
                lastCompositeAlphaValue = alphaValue;

                this.fChangeFlag = (this.fChangeFlag | kCompositeChangedBit);
            } else {
                this.fChangeFlag = (this.fChangeFlag & kCompositeNotChangedBit);
            }
        } else {
            this.fChangeFlag = (this.fChangeFlag & kCompositeNotChangedBit);
        }
    }

    BasicStroke lastStroke = null;
    static BasicStroke defaultBasicStroke = new BasicStroke();

    void setupStroke(SunGraphics2D sg2d) {
        BasicStroke stroke = defaultBasicStroke;

        if (sg2d.stroke instanceof BasicStroke) {
            stroke = (BasicStroke) sg2d.stroke;
        }

        if (lastStroke != stroke) {
            this.fGraphicsStatesObject[kStrokeDashArrayIndex] = stroke.getDashArray();
            this.fGraphicsStatesFloat.put(kStrokeDashPhaseIndex, stroke.getDashPhase());
            this.fGraphicsStatesInt.put(kStrokeCapIndex, stroke.getEndCap());
            this.fGraphicsStatesInt.put(kStrokeJoinIndex, stroke.getLineJoin());
            this.fGraphicsStatesFloat.put(kStrokeWidthIndex, stroke.getLineWidth());
            this.fGraphicsStatesFloat.put(kStrokeLimitIndex, stroke.getMiterLimit());

            this.fChangeFlag = (this.fChangeFlag | kStrokeChangedBit);

            lastStroke = stroke;
        } else {
            this.fChangeFlag = (this.fChangeFlag & kStrokeNotChangedBit);
        }
    }

    Font lastFont;

    void setupFont(Font font, Paint paint) {
        if (font == null) { return; }

        // We have to setup the kFontPaintIndex if we have changed the color so we added the last
        // test to see if the color has changed - needed for complex strings
        // see Radar 3368674
        if ((font != lastFont) || ((this.fChangeFlag & kColorChangedBit) != 0)) {
            this.fGraphicsStatesObject[kFontIndex] = font;
            this.fGraphicsStatesObject[kFontPaintIndex] = paint;

            this.fChangeFlag = (this.fChangeFlag | kFontChangedBit);

            lastFont = font;
        } else {
            this.fChangeFlag = (this.fChangeFlag & kFontNotChangedBit);
        }
    }

    void setupRenderingHints(SunGraphics2D sg2d) {
        boolean hintsChanged = false;

        // Significant for draw, fill, text, and image ops:
        int antialiasHint = sg2d.antialiasHint;
        if (this.fGraphicsStatesInt.get(kHintsAntialiasIndex) != antialiasHint) {
            this.fGraphicsStatesInt.put(kHintsAntialiasIndex, antialiasHint);
            hintsChanged = true;
        }

        // Significant only for text ops:
        int textAntialiasHint = sg2d.textAntialiasHint;
        if (this.fGraphicsStatesInt.get(kHintsTextAntialiasIndex) != textAntialiasHint) {
            this.fGraphicsStatesInt.put(kHintsTextAntialiasIndex, textAntialiasHint);
            hintsChanged = true;
        }

        // Significant only for text ops:
        int fractionalMetricsHint = sg2d.fractionalMetricsHint;
        if (this.fGraphicsStatesInt.get(kHintsFractionalMetricsIndex) != fractionalMetricsHint) {
            this.fGraphicsStatesInt.put(kHintsFractionalMetricsIndex, fractionalMetricsHint);
            hintsChanged = true;
        }

        // Significant only for image ops:
        int renderHint = sg2d.renderHint;
        if (this.fGraphicsStatesInt.get(kHintsRenderingIndex) != renderHint) {
            this.fGraphicsStatesInt.put(kHintsRenderingIndex, renderHint);
            hintsChanged = true;
        }

        // Significant only for image ops:
        Object hintValue = sg2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        int interpolationHint = (hintValue != null ? ((SunHints.Value) hintValue).getIndex() : -1);
        if (this.fGraphicsStatesInt.get(kHintsInterpolationIndex) != interpolationHint) {
            this.fGraphicsStatesInt.put(kHintsInterpolationIndex, interpolationHint);
            hintsChanged = true;
        }

        if (hintsChanged) {
            this.fChangeFlag = (this.fChangeFlag | kHintsChangedBit);
        } else {
            this.fChangeFlag = (this.fChangeFlag & kHintsNotChangedBit);
        }
    }

    SunGraphics2D sg2dCurrent = null;
    Thread threadCurrent = null;

    void setupGraphicsState(SunGraphics2D sg2d, int primitiveType) {
        setupGraphicsState(sg2d, primitiveType, sg2d.font, 0, 0, fBounds.width, fBounds.height); // deviceBounds into userBounds
    }

    void setupGraphicsState(SunGraphics2D sg2d, int primitiveType, int x, int y, int w, int h) {
        setupGraphicsState(sg2d, primitiveType, sg2d.font, x, y, w, h);
    }

    // the method below is overriden by CPeerSurface to check the last peer used to draw
    // if the peer changed we finish lazy drawing
    void setupGraphicsState(SunGraphics2D sg2d, int primitiveType, Font font, int x, int y, int w, int h) {
        this.fChangeFlag = 0;

        setUserBounds(sg2d, x, y, w, h);

        Thread thread = Thread.currentThread();
        if ((this.sg2dCurrent != sg2d) || (this.threadCurrent != thread)) {
            this.sg2dCurrent = sg2d;
            this.threadCurrent = thread;

            setupClip(sg2d);
            setupTransform(sg2d);
            setupPaint(sg2d, x, y, w, h);
            setupComposite(sg2d);
            setupStroke(sg2d);
            setupFont(font, sg2d.paint);
            setupRenderingHints(sg2d);

            this.fChangeFlag = kEverythingChangedFlag;
        } else {
            int rendererType = getRendererTypeForPrimitive(primitiveType);

            setupClip(sg2d);
            setupTransform(sg2d);

            if (rendererType != kCopyArea) {
                setupComposite(sg2d);
                setupRenderingHints(sg2d);

                if ((rendererType != kImage)) {
                    setupPaint(sg2d, x, y, w, h);
                    setupStroke(sg2d);
                }
                if (rendererType != kPrimitive) {
                    setupFont(font, sg2d.paint);
                }

            }
        }

        this.fGraphicsStatesInt.put(kChangeFlagIndex, this.fChangeFlag);
    }

    boolean isCustomPaint(SunGraphics2D sg2d) {
        if ((sg2d.paint instanceof Color) || (sg2d.paint instanceof SystemColor) || (sg2d.paint instanceof GradientPaint) || (sg2d.paint instanceof TexturePaint)) { return false; }

        return true;
    }

    final float[] segmentCoordinatesArray = new float[6];

    int getPathLength(GeneralPath gp) {
        int length = 0;

        PathIterator pi = gp.getPathIterator(null);
        while (pi.isDone() == false) {
            pi.next();
            length++;
        }

        return length;
    }

    int getPathCoordinates(GeneralPath gp, FloatBuffer coordinates, IntBuffer types) {
        // System.err.println("getPathCoordinates");
        boolean skip = false;

        coordinates.clear();
        types.clear();

        int type;

        PathIterator pi = gp.getPathIterator(null);
        while (pi.isDone() == false) {
            skip = false;
            type = pi.currentSegment(segmentCoordinatesArray);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    // System.err.println(" SEG_MOVETO ("+segmentCoordinatesArray[0]+", "+segmentCoordinatesArray[1]+")");
                    if (segmentCoordinatesArray[0] < UPPER_BND && segmentCoordinatesArray[0] > LOWER_BND &&
                            segmentCoordinatesArray[1] < UPPER_BND && segmentCoordinatesArray[1] > LOWER_BND) {
                        coordinates.put(segmentCoordinatesArray[0]);
                        coordinates.put(segmentCoordinatesArray[1]);
                    } else {
                        skip = true;
                    }
                    break;
                case PathIterator.SEG_LINETO:
                    // System.err.println(" SEG_LINETO ("+segmentCoordinatesArray[0]+", "+segmentCoordinatesArray[1]+")");
                    if (segmentCoordinatesArray[0] < UPPER_BND && segmentCoordinatesArray[0] > LOWER_BND &&
                            segmentCoordinatesArray[1] < UPPER_BND && segmentCoordinatesArray[1] > LOWER_BND) {
                        coordinates.put(segmentCoordinatesArray[0]);
                        coordinates.put(segmentCoordinatesArray[1]);
                    } else {
                        skip = true;
                    }
                    break;
                case PathIterator.SEG_QUADTO:
                    // System.err.println(" SEG_QUADTO ("+segmentCoordinatesArray[0]+", "+segmentCoordinatesArray[1]+"), ("+segmentCoordinatesArray[2]+", "+segmentCoordinatesArray[3]+")");
                    if (segmentCoordinatesArray[0] < UPPER_BND && segmentCoordinatesArray[0] > LOWER_BND &&
                            segmentCoordinatesArray[1] < UPPER_BND && segmentCoordinatesArray[1] > LOWER_BND &&
                            segmentCoordinatesArray[2] < UPPER_BND && segmentCoordinatesArray[2] > LOWER_BND &&
                            segmentCoordinatesArray[3] < UPPER_BND && segmentCoordinatesArray[3] > LOWER_BND) {
                        coordinates.put(segmentCoordinatesArray[0]);
                        coordinates.put(segmentCoordinatesArray[1]);
                        coordinates.put(segmentCoordinatesArray[2]);
                        coordinates.put(segmentCoordinatesArray[3]);
                    } else {
                        skip = true;
                    }
                    break;
                case PathIterator.SEG_CUBICTO:
                    // System.err.println(" SEG_QUADTO ("+segmentCoordinatesArray[0]+", "+segmentCoordinatesArray[1]+"), ("+segmentCoordinatesArray[2]+", "+segmentCoordinatesArray[3]+"), ("+segmentCoordinatesArray[4]+", "+segmentCoordinatesArray[5]+")");
                    if (segmentCoordinatesArray[0] < UPPER_BND && segmentCoordinatesArray[0] > LOWER_BND &&
                            segmentCoordinatesArray[1] < UPPER_BND && segmentCoordinatesArray[1] > LOWER_BND &&
                            segmentCoordinatesArray[2] < UPPER_BND && segmentCoordinatesArray[2] > LOWER_BND &&
                            segmentCoordinatesArray[3] < UPPER_BND && segmentCoordinatesArray[3] > LOWER_BND &&
                            segmentCoordinatesArray[4] < UPPER_BND && segmentCoordinatesArray[4] > LOWER_BND &&
                            segmentCoordinatesArray[5] < UPPER_BND && segmentCoordinatesArray[5] > LOWER_BND) {
                        coordinates.put(segmentCoordinatesArray[0]);
                        coordinates.put(segmentCoordinatesArray[1]);
                        coordinates.put(segmentCoordinatesArray[2]);
                        coordinates.put(segmentCoordinatesArray[3]);
                        coordinates.put(segmentCoordinatesArray[4]);
                        coordinates.put(segmentCoordinatesArray[5]);
                    } else {
                        skip = true;
                    }
                    break;
                case PathIterator.SEG_CLOSE:
                    // System.err.println(" SEG_CLOSE");
                    break;
            }

            if (!skip) {
                types.put(type);
            }

            pi.next();
        }

        return pi.getWindingRule();
    }

    public void doLine(CRenderer renderer, SunGraphics2D sg2d, float x1, float y1, float x2, float y2) {
        // System.err.println("-- doLine x1="+x1+" y1="+y1+" x2="+x2+" y2="+y2+" paint="+sg2d.paint);
        setupGraphicsState(sg2d, kLine, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        renderer.doLine(this, x1, y1, x2, y2);
    }

    public void doRect(CRenderer renderer, SunGraphics2D sg2d, float x, float y, float width, float height, boolean isfill) {
        // System.err.println("-- doRect x="+x+" y="+y+" w="+width+" h="+height+" isfill="+isfill+" paint="+sg2d.paint);
        if ((isfill) && (isCustomPaint(sg2d))) {
            setupGraphicsState(sg2d, kRect, (int) x, (int) y, (int) width, (int) height);
        } else {
            setupGraphicsState(sg2d, kRect, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        }
        renderer.doRect(this, x, y, width, height, isfill);
    }

    public void doRoundRect(CRenderer renderer, SunGraphics2D sg2d, float x, float y, float width, float height, float arcW, float arcH, boolean isfill) {
        // System.err.println("--- doRoundRect");
        if ((isfill) && (isCustomPaint(sg2d))) {
            setupGraphicsState(sg2d, kRoundRect, (int) x, (int) y, (int) width, (int) height);
        } else {
            setupGraphicsState(sg2d, kRoundRect, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        }
        renderer.doRoundRect(this, x, y, width, height, arcW, arcH, isfill);
    }

    public void doOval(CRenderer renderer, SunGraphics2D sg2d, float x, float y, float width, float height, boolean isfill) {
        // System.err.println("--- doOval");
        if ((isfill) && (isCustomPaint(sg2d))) {
            setupGraphicsState(sg2d, kOval, (int) x, (int) y, (int) width, (int) height);
        } else {
            setupGraphicsState(sg2d, kOval, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        }
        renderer.doOval(this, x, y, width, height, isfill);
    }

    public void doArc(CRenderer renderer, SunGraphics2D sg2d, float x, float y, float width, float height, float startAngle, float arcAngle, int type, boolean isfill) {
        // System.err.println("--- doArc");
        if ((isfill) && (isCustomPaint(sg2d))) {
            setupGraphicsState(sg2d, kArc, (int) x, (int) y, (int) width, (int) height);
        } else {
            setupGraphicsState(sg2d, kArc, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        }

        renderer.doArc(this, x, y, width, height, startAngle, arcAngle, type, isfill);
    }

    public void doPolygon(CRenderer renderer, SunGraphics2D sg2d, int[] xpoints, int[] ypoints, int npoints, boolean ispolygon, boolean isfill) {
        // System.err.println("--- doPolygon");

        if ((isfill) && (isCustomPaint(sg2d))) {
            int minx = xpoints[0];
            int miny = ypoints[0];
            int maxx = minx;
            int maxy = miny;
            for (int i = 1; i < npoints; i++) {
                int x = xpoints[i];
                if (x < minx) {
                    minx = x;
                } else if (x > maxx) {
                    maxx = x;
                }

                int y = ypoints[i];
                if (y < miny) {
                    miny = y;
                } else if (y > maxy) {
                    maxy = y;
                }
            }
            setupGraphicsState(sg2d, kPolygon, minx, miny, maxx - minx, maxy - miny);
        } else {
            setupGraphicsState(sg2d, kPolygon, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        }
        renderer.doPoly(this, xpoints, ypoints, npoints, ispolygon, isfill);
    }

    FloatBuffer shapeCoordinatesArray = null;
    IntBuffer shapeTypesArray = null;

    public void drawfillShape(CRenderer renderer, SunGraphics2D sg2d, GeneralPath gp, boolean isfill, boolean shouldApplyOffset) {
        // System.err.println("--- drawfillShape");

        if ((isfill) && (isCustomPaint(sg2d))) {
            Rectangle bounds = gp.getBounds();
            setupGraphicsState(sg2d, kShape, bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            setupGraphicsState(sg2d, kShape, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        }

        int shapeLength = getPathLength(gp);

        if ((shapeCoordinatesArray == null) || (shapeCoordinatesArray.capacity() < (shapeLength * 6))) {
            shapeCoordinatesArray = getBufferOfSize(shapeLength * 6).asFloatBuffer(); // segment can have a max of 6
                                                                                      // coordinates
        }
        if ((shapeTypesArray == null) || (shapeTypesArray.capacity() < shapeLength)) {
            shapeTypesArray = getBufferOfSize(shapeLength).asIntBuffer();
        }

        int windingRule = getPathCoordinates(gp, shapeCoordinatesArray, shapeTypesArray);

        renderer.doShape(this, shapeLength, shapeCoordinatesArray, shapeTypesArray, windingRule, isfill, shouldApplyOffset);
    }

    public void blitImage(CRenderer renderer, SunGraphics2D sg2d, SurfaceData img, boolean fliph, boolean flipv, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, Color bgColor) {
        // System.err.println("--- blitImage sx="+sx+", sy="+sy+", sw="+sw+", sh="+sh+", img="+img);
        OSXOffScreenSurfaceData osxsd = (OSXOffScreenSurfaceData) img;
        synchronized (osxsd.getLockObject()) {
            int w = osxsd.bim.getWidth();
            int h = osxsd.bim.getHeight();

            // the image itself can have outstanding graphics primitives that might need to be flushed
            setupGraphicsState(sg2d, kImage, sg2d.font, 0, 0, fBounds.width, fBounds.height);

            // 04/06/04 cmc: radr://3612381 Graphics.drawImage ignores bgcolor parameter
            if (bgColor != null) {
                img = osxsd.getCopyWithBgColor(bgColor);
            }

            renderer.doImage(this, img, fliph, flipv, w, h, sx, sy, sw, sh, dx, dy, dw, dh);
        }
    }

    public interface CGContextDrawable {
        public void drawIntoCGContext(final long cgContext);
    }

    public void drawString(CTextPipe renderer, SunGraphics2D sg2d, long nativeStrikePtr, String str, double x, double y) {
        // System.err.println("--- drawString str=\""+str+"\"");
        // see <rdar://problem/3825795>. We don't want to call anything if the string is empty!
        if (str.length() == 0) { return; }

        setupGraphicsState(sg2d, kString, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        renderer.doDrawString(this, nativeStrikePtr, str, x, y);
    }

    public void drawGlyphs(CTextPipe renderer, SunGraphics2D sg2d, long nativeStrikePtr, GlyphVector gv, float x, float y) {
        // System.err.println("--- drawGlyphs");
        setupGraphicsState(sg2d, kGlyphs, gv.getFont(), 0, 0, fBounds.width, fBounds.height);
        renderer.doDrawGlyphs(this, nativeStrikePtr, gv, x, y);
    }

    public void drawUnicodes(CTextPipe renderer, SunGraphics2D sg2d, long nativeStrikePtr, char[] unicodes, int offset, int length, float x, float y) {
        // System.err.println("--- drawUnicodes "+(new String(unicodes, offset, length)));
        setupGraphicsState(sg2d, kUnicodes, sg2d.font, 0, 0, fBounds.width, fBounds.height);
        if (length == 1) {
            renderer.doOneUnicode(this, nativeStrikePtr, unicodes[offset], x, y);
        } else {
            renderer.doUnicodes(this, nativeStrikePtr, unicodes, offset, length, x, y);
        }
    }

    // used by copyArea:

    Rectangle srcCopyAreaRect = new Rectangle();
    Rectangle dstCopyAreaRect = new Rectangle();
    Rectangle finalCopyAreaRect = new Rectangle();
    Rectangle copyAreaBounds = new Rectangle();

    void intersection(Rectangle r1, Rectangle r2, Rectangle r3) {
        // this code is taken from Rectangle.java (modified to put results in r3)
        int tx1 = r1.x;
        int ty1 = r1.y;
        long tx2 = tx1 + r1.width;
        long ty2 = ty1 + r1.height;

        int rx1 = r2.x;
        int ry1 = r2.y;
        long rx2 = rx1 + r2.width;
        long ry2 = ry1 + r2.height;

        if (tx1 < rx1) tx1 = rx1;
        if (ty1 < ry1) ty1 = ry1;
        if (tx2 > rx2) tx2 = rx2;
        if (ty2 > ry2) ty2 = ry2;

        tx2 -= tx1;
        ty2 -= ty1;

        // tx2,ty2 will never overflow (they will never be
        // larger than the smallest of the two source w,h)
        // they might underflow, though...
        if (tx2 < Integer.MIN_VALUE) tx2 = Integer.MIN_VALUE;
        if (ty2 < Integer.MIN_VALUE) ty2 = Integer.MIN_VALUE;

        r3.setBounds(tx1, ty1, (int) tx2, (int) ty2);
    }

    /**
     * Clips the copy area to the heavyweight bounds and returns the clipped rectangle.
     * The returned clipped rectangle is in the coordinate space of the surface.
     */
    protected Rectangle clipCopyArea(SunGraphics2D sg2d, int x, int y, int w, int h, int dx, int dy) {
        // we need to clip against the heavyweight bounds
        copyAreaBounds.setBounds(sg2d.devClip.getLoX(), sg2d.devClip.getLoY(), sg2d.devClip.getWidth(), sg2d.devClip.getHeight());

        // clip src rect
        srcCopyAreaRect.setBounds(x, y, w, h);
        intersection(srcCopyAreaRect, copyAreaBounds, srcCopyAreaRect);
        if ((srcCopyAreaRect.width <= 0) || (srcCopyAreaRect.height <= 0)) {
            // src rect outside bounds
            return null;
        }

        // clip dst rect
        dstCopyAreaRect.setBounds(srcCopyAreaRect.x + dx, srcCopyAreaRect.y + dy, srcCopyAreaRect.width, srcCopyAreaRect.height);
        intersection(dstCopyAreaRect, copyAreaBounds, dstCopyAreaRect);
        if ((dstCopyAreaRect.width <= 0) || (dstCopyAreaRect.height <= 0)) {
            // dst rect outside clip
            return null;
        }

        x = dstCopyAreaRect.x - dx;
        y = dstCopyAreaRect.y - dy;
        w = dstCopyAreaRect.width;
        h = dstCopyAreaRect.height;

        finalCopyAreaRect.setBounds(x, y, w, h);

        return finalCopyAreaRect;
    }

    // <rdar://3785539> We only need to mark dirty on screen surfaces. This method is
    // marked as protected and it is intended for subclasses to override if they need to
    // be notified when the surface is dirtied. See CPeerSurfaceData.markDirty() for implementation.
    // We don't do anything for buffered images.
    protected void markDirty(boolean markAsDirty) {
        // do nothing by default
    }

    // LazyDrawing optimization implementation:

    @Override
    public boolean canRenderLCDText(SunGraphics2D sg2d) {
        if (sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY &&
                sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR &&
                sg2d.clipState <= SunGraphics2D.CLIP_RECTANGULAR &&
                // sg2d.surfaceData.getTransparency() == Transparency.OPAQUE &&
                // This last test is a workaround until we fix loop selection
                // in the pipe validation
                sg2d.antialiasHint != SunHints.INTVAL_ANTIALIAS_ON) { return true; }
        return false; /* for now - in the future we may want to search */
    }

    public static boolean IsSimpleColor(Object c) {
        return ((c instanceof Color) || (c instanceof SystemColor) || (c instanceof javax.swing.plaf.ColorUIResource));
    }

    static {
        if ((kColorPointerIndex % 2) != 0) {
            System.err.println("kColorPointerIndex=" + kColorPointerIndex + " is NOT aligned for 64 bit");
            System.exit(0);
        }
    }
}
