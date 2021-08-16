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

package sun.java2d;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

import sun.font.FontUtilities;
import sun.java2d.loops.RenderCache;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.MaskFill;
import sun.java2d.loops.DrawLine;
import sun.java2d.loops.FillRect;
import sun.java2d.loops.DrawRect;
import sun.java2d.loops.DrawPolygons;
import sun.java2d.loops.DrawPath;
import sun.java2d.loops.FillPath;
import sun.java2d.loops.FillSpans;
import sun.java2d.loops.FillParallelogram;
import sun.java2d.loops.DrawParallelogram;
import sun.java2d.loops.FontInfo;
import sun.java2d.loops.DrawGlyphList;
import sun.java2d.loops.DrawGlyphListAA;
import sun.java2d.loops.DrawGlyphListLCD;
import sun.java2d.loops.DrawGlyphListColor;
import sun.java2d.pipe.LoopPipe;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.ParallelogramPipe;
import sun.java2d.pipe.CompositePipe;
import sun.java2d.pipe.GeneralCompositePipe;
import sun.java2d.pipe.SpanClipRenderer;
import sun.java2d.pipe.SpanShapeRenderer;
import sun.java2d.pipe.AAShapePipe;
import sun.java2d.pipe.AlphaPaintPipe;
import sun.java2d.pipe.AlphaColorPipe;
import sun.java2d.pipe.PixelToShapeConverter;
import sun.java2d.pipe.PixelToParallelogramConverter;
import sun.java2d.pipe.TextPipe;
import sun.java2d.pipe.TextRenderer;
import sun.java2d.pipe.AATextRenderer;
import sun.java2d.pipe.LCDTextRenderer;
import sun.java2d.pipe.SolidTextRenderer;
import sun.java2d.pipe.OutlineTextRenderer;
import sun.java2d.pipe.DrawImagePipe;
import sun.java2d.pipe.DrawImage;
import sun.awt.SunHints;
import sun.awt.image.SurfaceManager;
import sun.java2d.pipe.LoopBasedPipe;

/**
 * This class provides various pieces of information relevant to a
 * particular drawing surface.  The information obtained from this
 * object describes the pixels of a particular instance of a drawing
 * surface and can only be shared among the various graphics objects
 * that target the same BufferedImage or the same screen Component.
 * <p>
 * Each SurfaceData object holds a StateTrackableDelegate object
 * which tracks both changes to the content of the pixels of this
 * surface and changes to the overall state of the pixels - such
 * as becoming invalid or losing the surface.  The delegate is
 * marked "dirty" whenever the setSurfaceLost() or invalidate()
 * methods are called and should also be marked "dirty" by the
 * rendering pipelines whenever they modify the pixels of this
 * SurfaceData.
 * <p>
 * If you get a StateTracker from a SurfaceData and it reports
 * that it is still "current", then you can trust that the pixels
 * have not changed and that the SurfaceData is still valid and
 * has not lost its underlying storage (surfaceLost) since you
 * retrieved the tracker.
 */
public abstract class SurfaceData
    implements Transparency, DisposerTarget, StateTrackable, Surface
{
    private long pData;
    private boolean valid;
    private boolean surfaceLost; // = false;
    private SurfaceType surfaceType;
    private ColorModel colorModel;

    private Object disposerReferent = new Object();

    private static native void initIDs();

    private Object blitProxyKey;
    private StateTrackableDelegate stateDelegate;

    static {
        initIDs();
    }

    protected SurfaceData(SurfaceType surfaceType, ColorModel cm) {
        this(State.STABLE, surfaceType, cm);
    }

    protected SurfaceData(State state, SurfaceType surfaceType, ColorModel cm) {
        this(StateTrackableDelegate.createInstance(state), surfaceType, cm);
    }

    protected SurfaceData(StateTrackableDelegate trackable,
                          SurfaceType surfaceType, ColorModel cm)
    {
        this.stateDelegate = trackable;
        this.colorModel = cm;
        this.surfaceType = surfaceType;
        valid = true;
    }

    protected SurfaceData(State state) {
        this.stateDelegate = StateTrackableDelegate.createInstance(state);
        valid = true;
    }

    /**
     * Subclasses can set a "blit proxy key" which will be used
     * along with the SurfaceManager.getCacheData() mechanism to
     * store acceleration-compatible cached copies of source images.
     * This key is a "tag" used to identify which cached copies
     * are compatible with this destination SurfaceData.
     * The getSourceSurfaceData() method uses this key to manage
     * cached copies of a source image as described below.
     * <p>
     * The Object used as this key should be as unique as it needs
     * to be to ensure that multiple acceleratible destinations can
     * each store their cached copies separately under different keys
     * without interfering with each other or getting back the wrong
     * cached copy.
     * <p>
     * Many acceleratable SurfaceData objects can use their own
     * GraphicsConfiguration as their proxy key as the GC object will
     * typically be unique to a given screen and pixel format, but
     * other rendering destinations may have more or less stringent
     * sharing requirements.  For instance, X11 pixmaps can be
     * shared on a given screen by any GraphicsConfiguration that
     * has the same depth and SurfaceType.  Multiple such GCs with
     * the same depth and SurfaceType can exist per screen so storing
     * a different cached proxy for each would be a waste.  One can
     * imagine platforms where a single cached copy can be created
     * and shared across all screens and pixel formats - such
     * implementations could use a single heavily shared key Object.
     */
    protected void setBlitProxyKey(Object key) {
        // Caching is effectively disabled if we never have a proxy key
        // since the getSourceSurfaceData() method only does caching
        // if the key is not null.
        if (SurfaceDataProxy.isCachingAllowed()) {
            this.blitProxyKey = key;
        }
    }

    /**
     * This method is called on a destination SurfaceData to choose
     * the best SurfaceData from a source Image for an imaging
     * operation, with help from its SurfaceManager.
     * The method may determine that the default SurfaceData was
     * really the best choice in the first place, or it may decide
     * to use a cached surface.  Some general decisions about whether
     * acceleration is enabled are made by this method, but any
     * decision based on the type of the source image is made in
     * the makeProxyFor method below when it comes up with the
     * appropriate SurfaceDataProxy instance.
     * The parameters describe the type of imaging operation being performed.
     * <p>
     * If a blitProxyKey was supplied by the subclass then it is
     * used to potentially override the choice of source SurfaceData.
     * The outline of this process is:
     * <ol>
     * <li> Image pipeline asks destSD to find an appropriate
     *      srcSD for a given source Image object.
     * <li> destSD gets the SurfaceManager of the source Image
     *      and first retrieves the default SD from it using
     *      getPrimarySurfaceData()
     * <li> destSD uses its "blit proxy key" (if set) to look for
     *      some cached data stored in the source SurfaceManager
     * <li> If the cached data is null then makeProxyFor() is used
     *      to create some cached data which is stored back in the
     *      source SurfaceManager under the same key for future uses.
     * <li> The cached data will be a SurfaceDataProxy object.
     * <li> The SurfaceDataProxy object is then consulted to
     *      return a replacement SurfaceData object (typically
     *      a cached copy if appropriate, or the original if not).
     * </ol>
     */
    public SurfaceData getSourceSurfaceData(Image img,
                                            int txtype,
                                            CompositeType comp,
                                            Color bgColor)
    {
        SurfaceManager srcMgr = SurfaceManager.getManager(img);
        SurfaceData srcData = srcMgr.getPrimarySurfaceData();
        if (img.getAccelerationPriority() > 0.0f &&
            blitProxyKey != null)
        {
            SurfaceDataProxy sdp =
                (SurfaceDataProxy) srcMgr.getCacheData(blitProxyKey);
            if (sdp == null || !sdp.isValid()) {
                if (srcData.getState() == State.UNTRACKABLE) {
                    sdp = SurfaceDataProxy.UNCACHED;
                } else {
                    sdp = makeProxyFor(srcData);
                }
                srcMgr.setCacheData(blitProxyKey, sdp);
            }
            srcData = sdp.replaceData(srcData, txtype, comp, bgColor);
        }
        return srcData;
    }

    /**
     * This method is called on a destination SurfaceData to choose
     * a proper SurfaceDataProxy subclass for a source SurfaceData
     * to use to control when and with what surface to override a
     * given image operation.  The argument is the default SurfaceData
     * for the source Image.
     * <p>
     * The type of the return object is chosen based on the
     * acceleration capabilities of this SurfaceData and the
     * type of the given source SurfaceData object.
     * <p>
     * In some cases the original SurfaceData will always be the
     * best choice to use to blit to this SurfaceData.  This can
     * happen if the source image is a hardware surface of the
     * same type as this one and so acceleration will happen without
     * any caching.  It may also be the case that the source image
     * can never be accelerated on this SurfaceData - for example
     * because it is translucent and there are no accelerated
     * translucent image ops for this surface.
     * <p>
     * In those cases there is a special SurfaceDataProxy.UNCACHED
     * instance that represents a NOP for caching purposes - it
     * always returns the original sourceSD object as the replacement
     * copy so no caching is ever performed.
     */
    public SurfaceDataProxy makeProxyFor(SurfaceData srcData) {
        return SurfaceDataProxy.UNCACHED;
    }

    /**
     * Extracts the SurfaceManager from the given Image, and then
     * returns the SurfaceData object that would best be suited as the
     * destination surface in some rendering operation.
     */
    public static SurfaceData getPrimarySurfaceData(Image img) {
        SurfaceManager sMgr = SurfaceManager.getManager(img);
        return sMgr.getPrimarySurfaceData();
    }

    /**
     * Restores the contents of the given Image and then returns the new
     * SurfaceData object in use by the Image's SurfaceManager.
     */
    public static SurfaceData restoreContents(Image img) {
        SurfaceManager sMgr = SurfaceManager.getManager(img);
        return sMgr.restoreContents();
    }

    public State getState() {
        return stateDelegate.getState();
    }

    public StateTracker getStateTracker() {
        return stateDelegate.getStateTracker();
    }

    /**
     * Marks this surface as dirty.
     */
    public final void markDirty() {
        stateDelegate.markDirty();
    }

    /**
     * Sets the value of the surfaceLost variable, which indicates whether
     * something has happened to the rendering surface such that it needs
     * to be restored and re-rendered.
     */
    public void setSurfaceLost(boolean lost) {
        surfaceLost = lost;
        stateDelegate.markDirty();
    }

    public boolean isSurfaceLost() {
        return surfaceLost;
    }

    /**
     * Returns a boolean indicating whether or not this SurfaceData is valid.
     */
    public final boolean isValid() {
        return valid;
    }

    public Object getDisposerReferent() {
        return disposerReferent;
    }

    public long getNativeOps() {
        return pData;
    }

    /**
     * Sets this SurfaceData object to the invalid state.  All Graphics
     * objects must get a new SurfaceData object via the refresh method
     * and revalidate their pipelines before continuing.
     */
    public void invalidate() {
        valid = false;
        stateDelegate.markDirty();
    }

    /**
     * Certain changes in the configuration of a surface require the
     * invalidation of existing associated SurfaceData objects and
     * the creation of brand new ones.  These changes include size,
     * ColorModel, or SurfaceType.  Existing Graphics objects
     * which are directed at such surfaces, however, must continue
     * to render to them even after the change occurs underneath
     * the covers.  The getReplacement() method is called from
     * SunGraphics2D.revalidateAll() when the associated SurfaceData
     * is found to be invalid so that a Graphics object can continue
     * to render to the surface in its new configuration.
     *
     * Such changes only tend to happen to window based surfaces since
     * most image based surfaces never change size or pixel format.
     * Even VolatileImage objects never change size and they only
     * change their pixel format when manually validated against a
     * new GraphicsConfiguration, at which point old Graphics objects
     * are no longer expected to render to them after the validation
     * step.  Thus, only window based surfaces really need to deal
     * with this form of replacement.
     */
    public abstract SurfaceData getReplacement();

    protected static final LoopPipe colorPrimitives;

    public static final TextPipe outlineTextRenderer;
    public static final TextPipe solidTextRenderer;
    public static final TextPipe aaTextRenderer;
    public static final TextPipe lcdTextRenderer;

    protected static final AlphaColorPipe colorPipe;
    protected static final PixelToShapeConverter colorViaShape;
    protected static final PixelToParallelogramConverter colorViaPgram;
    protected static final TextPipe colorText;
    protected static final CompositePipe clipColorPipe;
    protected static final TextPipe clipColorText;
    protected static final AAShapePipe AAColorShape;
    protected static final PixelToParallelogramConverter AAColorViaShape;
    protected static final PixelToParallelogramConverter AAColorViaPgram;
    protected static final AAShapePipe AAClipColorShape;
    protected static final PixelToParallelogramConverter AAClipColorViaShape;

    protected static final CompositePipe paintPipe;
    protected static final SpanShapeRenderer paintShape;
    protected static final PixelToShapeConverter paintViaShape;
    protected static final TextPipe paintText;
    protected static final CompositePipe clipPaintPipe;
    protected static final TextPipe clipPaintText;
    protected static final AAShapePipe AAPaintShape;
    protected static final PixelToParallelogramConverter AAPaintViaShape;
    protected static final AAShapePipe AAClipPaintShape;
    protected static final PixelToParallelogramConverter AAClipPaintViaShape;

    protected static final CompositePipe compPipe;
    protected static final SpanShapeRenderer compShape;
    protected static final PixelToShapeConverter compViaShape;
    protected static final TextPipe compText;
    protected static final CompositePipe clipCompPipe;
    protected static final TextPipe clipCompText;
    protected static final AAShapePipe AACompShape;
    protected static final PixelToParallelogramConverter AACompViaShape;
    protected static final AAShapePipe AAClipCompShape;
    protected static final PixelToParallelogramConverter AAClipCompViaShape;

    protected static final DrawImagePipe imagepipe;

    // Utility subclass to add the LoopBasedPipe tagging interface
    static class PixelToShapeLoopConverter
        extends PixelToShapeConverter
        implements LoopBasedPipe
    {
        public PixelToShapeLoopConverter(ShapeDrawPipe pipe) {
            super(pipe);
        }
    }

    // Utility subclass to add the LoopBasedPipe tagging interface
    static class PixelToPgramLoopConverter
        extends PixelToParallelogramConverter
        implements LoopBasedPipe
    {
        public PixelToPgramLoopConverter(ShapeDrawPipe shapepipe,
                                         ParallelogramPipe pgrampipe,
                                         double minPenSize,
                                         double normPosition,
                                         boolean adjustfill)
        {
            super(shapepipe, pgrampipe, minPenSize, normPosition, adjustfill);
        }
    }

    private static PixelToParallelogramConverter
        makeConverter(AAShapePipe renderer,
                      ParallelogramPipe pgrampipe)
    {
        return new PixelToParallelogramConverter(renderer,
                                                 pgrampipe,
                                                 1.0/8.0, 0.499,
                                                 false);
    }

    private static PixelToParallelogramConverter
        makeConverter(AAShapePipe renderer)
    {
        return makeConverter(renderer, renderer);
    }

    static {
        colorPrimitives = new LoopPipe();

        outlineTextRenderer = new OutlineTextRenderer();
        aaTextRenderer = new AATextRenderer();
        if (FontUtilities.isMacOSX14) {
            solidTextRenderer = aaTextRenderer;
        } else {
            solidTextRenderer = new SolidTextRenderer();
        }
        lcdTextRenderer = new LCDTextRenderer();

        colorPipe = new AlphaColorPipe();
        // colorShape = colorPrimitives;
        colorViaShape = new PixelToShapeLoopConverter(colorPrimitives);
        colorViaPgram = new PixelToPgramLoopConverter(colorPrimitives,
                                                      colorPrimitives,
                                                      1.0, 0.25, true);
        colorText = new TextRenderer(colorPipe);
        clipColorPipe = new SpanClipRenderer(colorPipe);
        clipColorText = new TextRenderer(clipColorPipe);
        AAColorShape = new AAShapePipe(colorPipe);
        AAColorViaShape = makeConverter(AAColorShape);
        AAColorViaPgram = makeConverter(AAColorShape, colorPipe);
        AAClipColorShape = new AAShapePipe(clipColorPipe);
        AAClipColorViaShape = makeConverter(AAClipColorShape);

        paintPipe = new AlphaPaintPipe();
        paintShape = new SpanShapeRenderer.Composite(paintPipe);
        paintViaShape = new PixelToShapeConverter(paintShape);
        paintText = new TextRenderer(paintPipe);
        clipPaintPipe = new SpanClipRenderer(paintPipe);
        clipPaintText = new TextRenderer(clipPaintPipe);
        AAPaintShape = new AAShapePipe(paintPipe);
        AAPaintViaShape = makeConverter(AAPaintShape);
        AAClipPaintShape = new AAShapePipe(clipPaintPipe);
        AAClipPaintViaShape = makeConverter(AAClipPaintShape);

        compPipe = new GeneralCompositePipe();
        compShape = new SpanShapeRenderer.Composite(compPipe);
        compViaShape = new PixelToShapeConverter(compShape);
        compText = new TextRenderer(compPipe);
        clipCompPipe = new SpanClipRenderer(compPipe);
        clipCompText = new TextRenderer(clipCompPipe);
        AACompShape = new AAShapePipe(compPipe);
        AACompViaShape = makeConverter(AACompShape);
        AAClipCompShape = new AAShapePipe(clipCompPipe);
        AAClipCompViaShape = makeConverter(AAClipCompShape);

        imagepipe = new DrawImage();
    }

    /* Not all surfaces and rendering mode combinations support LCD text. */
    static final int LOOP_UNKNOWN = 0;
    static final int LOOP_FOUND = 1;
    static final int LOOP_NOTFOUND = 2;
    int haveLCDLoop;
    int havePgramXORLoop;
    int havePgramSolidLoop;

    public boolean canRenderLCDText(SunGraphics2D sg2d) {
        // For now the answer can only be true in the following cases:
        if (sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY &&
            sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR &&
            sg2d.clipState <= SunGraphics2D.CLIP_RECTANGULAR &&
            sg2d.surfaceData.getTransparency() == Transparency.OPAQUE)
        {
            if (haveLCDLoop == LOOP_UNKNOWN) {
                DrawGlyphListLCD loop =
                    DrawGlyphListLCD.locate(SurfaceType.AnyColor,
                                            CompositeType.SrcNoEa,
                                            getSurfaceType());
                haveLCDLoop = (loop != null) ? LOOP_FOUND : LOOP_NOTFOUND;
            }
            return haveLCDLoop == LOOP_FOUND;
        }
        return false; /* for now - in the future we may want to search */
    }

    public boolean canRenderParallelograms(SunGraphics2D sg2d) {
        if (sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR) {
            if (sg2d.compositeState == SunGraphics2D.COMP_XOR) {
                if (havePgramXORLoop == LOOP_UNKNOWN) {
                    FillParallelogram loop =
                        FillParallelogram.locate(SurfaceType.AnyColor,
                                                 CompositeType.Xor,
                                                 getSurfaceType());
                    havePgramXORLoop =
                        (loop != null) ? LOOP_FOUND : LOOP_NOTFOUND;
                }
                return havePgramXORLoop == LOOP_FOUND;
            } else if (sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY &&
                       sg2d.antialiasHint != SunHints.INTVAL_ANTIALIAS_ON &&
                       sg2d.clipState != SunGraphics2D.CLIP_SHAPE)
            {
                if (havePgramSolidLoop == LOOP_UNKNOWN) {
                    FillParallelogram loop =
                        FillParallelogram.locate(SurfaceType.AnyColor,
                                                 CompositeType.SrcNoEa,
                                                 getSurfaceType());
                    havePgramSolidLoop =
                        (loop != null) ? LOOP_FOUND : LOOP_NOTFOUND;
                }
                return havePgramSolidLoop == LOOP_FOUND;
            }
        }
        return false;
    }

    public void validatePipe(SunGraphics2D sg2d) {
        sg2d.imagepipe = imagepipe;
        if (sg2d.compositeState == SunGraphics2D.COMP_XOR) {
            if (sg2d.paintState > SunGraphics2D.PAINT_ALPHACOLOR) {
                sg2d.drawpipe = paintViaShape;
                sg2d.fillpipe = paintViaShape;
                sg2d.shapepipe = paintShape;
                // REMIND: Ideally custom paint mode would use glyph
                // rendering as opposed to outline rendering but the
                // glyph paint rendering pipeline uses MaskBlit which
                // is not defined for XOR.  This means that text drawn
                // in XOR mode with a Color object is different than
                // text drawn in XOR mode with a Paint object.
                sg2d.textpipe = outlineTextRenderer;
            } else {
                PixelToShapeConverter converter;
                if (canRenderParallelograms(sg2d)) {
                    converter = colorViaPgram;
                    // Note that we use the transforming pipe here because it
                    // will examine the shape and possibly perform an optimized
                    // operation if it can be simplified.  The simplifications
                    // will be valid for all STROKE and TRANSFORM types.
                    sg2d.shapepipe = colorViaPgram;
                } else {
                    converter = colorViaShape;
                    sg2d.shapepipe = colorPrimitives;
                }
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.drawpipe = converter;
                    sg2d.fillpipe = converter;
                    // REMIND: We should not be changing text strategies
                    // between outline and glyph rendering based upon the
                    // presence of a complex clip as that could cause a
                    // mismatch when drawing the same text both clipped
                    // and unclipped on two separate rendering passes.
                    // Unfortunately, all of the clipped glyph rendering
                    // pipelines rely on the use of the MaskBlit operation
                    // which is not defined for XOR.
                    sg2d.textpipe = outlineTextRenderer;
                } else {
                    if (sg2d.transformState >= SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
                        sg2d.drawpipe = converter;
                        sg2d.fillpipe = converter;
                    } else {
                        if (sg2d.strokeState != SunGraphics2D.STROKE_THIN) {
                            sg2d.drawpipe = converter;
                        } else {
                            sg2d.drawpipe = colorPrimitives;
                        }
                        sg2d.fillpipe = colorPrimitives;
                    }
                    sg2d.textpipe = solidTextRenderer;
                }
                // assert(sg2d.surfaceData == this);
            }
        } else if (sg2d.compositeState == SunGraphics2D.COMP_CUSTOM) {
            if (sg2d.antialiasHint == SunHints.INTVAL_ANTIALIAS_ON) {
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.drawpipe = AAClipCompViaShape;
                    sg2d.fillpipe = AAClipCompViaShape;
                    sg2d.shapepipe = AAClipCompViaShape;
                    sg2d.textpipe = clipCompText;
                } else {
                    sg2d.drawpipe = AACompViaShape;
                    sg2d.fillpipe = AACompViaShape;
                    sg2d.shapepipe = AACompViaShape;
                    sg2d.textpipe = compText;
                }
            } else {
                sg2d.drawpipe = compViaShape;
                sg2d.fillpipe = compViaShape;
                sg2d.shapepipe = compShape;
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.textpipe = clipCompText;
                } else {
                    sg2d.textpipe = compText;
                }
            }
        } else if (sg2d.antialiasHint == SunHints.INTVAL_ANTIALIAS_ON) {
            sg2d.alphafill = getMaskFill(sg2d);
            // assert(sg2d.surfaceData == this);
            if (sg2d.alphafill != null) {
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.drawpipe = AAClipColorViaShape;
                    sg2d.fillpipe = AAClipColorViaShape;
                    sg2d.shapepipe = AAClipColorViaShape;
                    sg2d.textpipe = clipColorText;
                } else {
                    PixelToParallelogramConverter converter =
                        (sg2d.alphafill.canDoParallelograms()
                         ? AAColorViaPgram
                         : AAColorViaShape);
                    sg2d.drawpipe = converter;
                    sg2d.fillpipe = converter;
                    sg2d.shapepipe = converter;
                    if (sg2d.paintState > SunGraphics2D.PAINT_ALPHACOLOR ||
                        sg2d.compositeState > SunGraphics2D.COMP_ISCOPY)
                    {
                        sg2d.textpipe = colorText;
                    } else {
                        sg2d.textpipe = getTextPipe(sg2d, true /* AA==ON */);
                    }
                }
            } else {
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.drawpipe = AAClipPaintViaShape;
                    sg2d.fillpipe = AAClipPaintViaShape;
                    sg2d.shapepipe = AAClipPaintViaShape;
                    sg2d.textpipe = clipPaintText;
                } else {
                    sg2d.drawpipe = AAPaintViaShape;
                    sg2d.fillpipe = AAPaintViaShape;
                    sg2d.shapepipe = AAPaintViaShape;
                    sg2d.textpipe = paintText;
                }
            }
        } else if (sg2d.paintState > SunGraphics2D.PAINT_ALPHACOLOR ||
                   sg2d.compositeState > SunGraphics2D.COMP_ISCOPY ||
                   sg2d.clipState == SunGraphics2D.CLIP_SHAPE)
        {
            sg2d.drawpipe = paintViaShape;
            sg2d.fillpipe = paintViaShape;
            sg2d.shapepipe = paintShape;
            sg2d.alphafill = getMaskFill(sg2d);
            // assert(sg2d.surfaceData == this);
            if (sg2d.alphafill != null) {
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.textpipe = clipColorText;
                } else {
                    sg2d.textpipe = colorText;
                }
            } else {
                if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
                    sg2d.textpipe = clipPaintText;
                } else {
                    sg2d.textpipe = paintText;
                }
            }
        } else {
            PixelToShapeConverter converter;
            if (canRenderParallelograms(sg2d)) {
                converter = colorViaPgram;
                // Note that we use the transforming pipe here because it
                // will examine the shape and possibly perform an optimized
                // operation if it can be simplified.  The simplifications
                // will be valid for all STROKE and TRANSFORM types.
                sg2d.shapepipe = colorViaPgram;
            } else {
                converter = colorViaShape;
                sg2d.shapepipe = colorPrimitives;
            }
            if (sg2d.transformState >= SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
                sg2d.drawpipe = converter;
                sg2d.fillpipe = converter;
            } else {
                if (sg2d.strokeState != SunGraphics2D.STROKE_THIN) {
                    sg2d.drawpipe = converter;
                } else {
                    sg2d.drawpipe = colorPrimitives;
                }
                sg2d.fillpipe = colorPrimitives;
            }

            sg2d.textpipe = getTextPipe(sg2d, false /* AA==OFF */);
            // assert(sg2d.surfaceData == this);
        }

        // check for loops
        if (sg2d.textpipe  instanceof LoopBasedPipe ||
            sg2d.shapepipe instanceof LoopBasedPipe ||
            sg2d.fillpipe  instanceof LoopBasedPipe ||
            sg2d.drawpipe  instanceof LoopBasedPipe ||
            sg2d.imagepipe instanceof LoopBasedPipe)
        {
            sg2d.loops = getRenderLoops(sg2d);
        }
    }

    /* Return the text pipe to be used based on the graphics AA hint setting,
     * and the rest of the graphics state is compatible with these loops.
     * If the text AA hint is "DEFAULT", then the AA graphics hint requests
     * the AA text renderer, else it requests the B&W text renderer.
     */
    private TextPipe getTextPipe(SunGraphics2D sg2d, boolean aaHintIsOn) {

        /* Try to avoid calling getFontInfo() unless its needed to
         * resolve one of the new AA types.
         */
        switch (sg2d.textAntialiasHint) {
        case SunHints.INTVAL_TEXT_ANTIALIAS_DEFAULT:
            if (aaHintIsOn) {
                return aaTextRenderer;
            } else {
                return solidTextRenderer;
            }
        case SunHints.INTVAL_TEXT_ANTIALIAS_OFF:
            return solidTextRenderer;

        case SunHints.INTVAL_TEXT_ANTIALIAS_ON:
            return aaTextRenderer;

        default:
            switch (sg2d.getFontInfo().aaHint) {

            case SunHints.INTVAL_TEXT_ANTIALIAS_LCD_HRGB:
            case SunHints.INTVAL_TEXT_ANTIALIAS_LCD_VRGB:
                return lcdTextRenderer;

            case SunHints.INTVAL_TEXT_ANTIALIAS_ON:
                return aaTextRenderer;

            case SunHints.INTVAL_TEXT_ANTIALIAS_OFF:
                return solidTextRenderer;

                 /* This should not be reached as the FontInfo will
                 * always explicitly set its hint value. So whilst
                 * this could be collapsed to returning say just
                 * solidTextRenderer, or even removed, its left
                 * here in case DEFAULT is ever passed in.
                 */
            default:
                if (aaHintIsOn) {
                    return aaTextRenderer;
                } else {
                    return solidTextRenderer;
                }
            }
        }
    }

    private static SurfaceType getPaintSurfaceType(SunGraphics2D sg2d) {
        switch (sg2d.paintState) {
        case SunGraphics2D.PAINT_OPAQUECOLOR:
            return SurfaceType.OpaqueColor;
        case SunGraphics2D.PAINT_ALPHACOLOR:
            return SurfaceType.AnyColor;
        case SunGraphics2D.PAINT_GRADIENT:
            if (sg2d.paint.getTransparency() == OPAQUE) {
                return SurfaceType.OpaqueGradientPaint;
            } else {
                return SurfaceType.GradientPaint;
            }
        case SunGraphics2D.PAINT_LIN_GRADIENT:
            if (sg2d.paint.getTransparency() == OPAQUE) {
                return SurfaceType.OpaqueLinearGradientPaint;
            } else {
                return SurfaceType.LinearGradientPaint;
            }
        case SunGraphics2D.PAINT_RAD_GRADIENT:
            if (sg2d.paint.getTransparency() == OPAQUE) {
                return SurfaceType.OpaqueRadialGradientPaint;
            } else {
                return SurfaceType.RadialGradientPaint;
            }
        case SunGraphics2D.PAINT_TEXTURE:
            if (sg2d.paint.getTransparency() == OPAQUE) {
                return SurfaceType.OpaqueTexturePaint;
            } else {
                return SurfaceType.TexturePaint;
            }
        default:
        case SunGraphics2D.PAINT_CUSTOM:
            return SurfaceType.AnyPaint;
        }
    }

    private static CompositeType getFillCompositeType(SunGraphics2D sg2d) {
        CompositeType compType = sg2d.imageComp;
        if (sg2d.compositeState == SunGraphics2D.COMP_ISCOPY) {
            if (compType == CompositeType.SrcOverNoEa) {
                compType = CompositeType.OpaqueSrcOverNoEa;
            } else {
                compType = CompositeType.SrcNoEa;
            }
        }
        return compType;
    }

    /**
     * Returns a MaskFill object that can be used on this destination
     * with the source (paint) and composite types determined by the given
     * SunGraphics2D, or null if no such MaskFill object can be located.
     * Subclasses can override this method if they wish to filter other
     * attributes (such as the hardware capabilities of the destination
     * surface) before returning a specific MaskFill object.
     */
    protected MaskFill getMaskFill(SunGraphics2D sg2d) {
        SurfaceType src = getPaintSurfaceType(sg2d);
        CompositeType comp = getFillCompositeType(sg2d);
        SurfaceType dst = getSurfaceType();
        return MaskFill.getFromCache(src, comp, dst);
    }

    private static RenderCache loopcache = new RenderCache(30);

    /**
     * Return a RenderLoops object containing all of the basic
     * GraphicsPrimitive objects for rendering to the destination
     * surface with the current attributes of the given SunGraphics2D.
     */
    public RenderLoops getRenderLoops(SunGraphics2D sg2d) {
        SurfaceType src = getPaintSurfaceType(sg2d);
        CompositeType comp = getFillCompositeType(sg2d);
        SurfaceType dst = sg2d.getSurfaceData().getSurfaceType();

        Object o = loopcache.get(src, comp, dst);
        if (o != null) {
            return (RenderLoops) o;
        }

        RenderLoops loops = makeRenderLoops(src, comp, dst);
        loopcache.put(src, comp, dst, loops);
        return loops;
    }

    /**
     * Construct and return a RenderLoops object containing all of
     * the basic GraphicsPrimitive objects for rendering to the
     * destination surface with the given source, destination, and
     * composite types.
     */
    public static RenderLoops makeRenderLoops(SurfaceType src,
                                              CompositeType comp,
                                              SurfaceType dst)
    {
        RenderLoops loops = new RenderLoops();
        loops.drawLineLoop = DrawLine.locate(src, comp, dst);
        loops.fillRectLoop = FillRect.locate(src, comp, dst);
        loops.drawRectLoop = DrawRect.locate(src, comp, dst);
        loops.drawPolygonsLoop = DrawPolygons.locate(src, comp, dst);
        loops.drawPathLoop = DrawPath.locate(src, comp, dst);
        loops.fillPathLoop = FillPath.locate(src, comp, dst);
        loops.fillSpansLoop = FillSpans.locate(src, comp, dst);
        loops.fillParallelogramLoop = FillParallelogram.locate(src, comp, dst);
        loops.drawParallelogramLoop = DrawParallelogram.locate(src, comp, dst);
        loops.drawGlyphListLoop = DrawGlyphList.locate(src, comp, dst);
        loops.drawGlyphListAALoop = DrawGlyphListAA.locate(src, comp, dst);
        loops.drawGlyphListLCDLoop = DrawGlyphListLCD.locate(src, comp, dst);
        loops.drawGlyphListColorLoop =
                DrawGlyphListColor.locate(src, comp, dst);
        /*
        System.out.println("drawLine: "+loops.drawLineLoop);
        System.out.println("fillRect: "+loops.fillRectLoop);
        System.out.println("drawRect: "+loops.drawRectLoop);
        System.out.println("drawPolygons: "+loops.drawPolygonsLoop);
        System.out.println("fillSpans: "+loops.fillSpansLoop);
        System.out.println("drawGlyphList: "+loops.drawGlyphListLoop);
        System.out.println("drawGlyphListAA: "+loops.drawGlyphListAALoop);
        System.out.println("drawGlyphListLCD: "+loops.drawGlyphListLCDLoop);
        */
        return loops;
    }

    /**
     * Return the GraphicsConfiguration object that describes this
     * destination surface.
     */
    public abstract GraphicsConfiguration getDeviceConfiguration();

    /**
     * Return the SurfaceType object that describes the destination
     * surface.
     */
    public final SurfaceType getSurfaceType() {
        return surfaceType;
    }

    /**
     * Return the ColorModel for the destination surface.
     */
    public final ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Returns the type of this {@code Transparency}.
     * @return the field type of this {@code Transparency}, which is
     *          either OPAQUE, BITMASK or TRANSLUCENT.
     */
    public int getTransparency() {
        return getColorModel().getTransparency();
    }

    /**
     * Return a readable Raster which contains the pixels for the
     * specified rectangular region of the destination surface.
     * The coordinate origin of the returned Raster is the same as
     * the device space origin of the destination surface.
     * In some cases the returned Raster might also be writeable.
     * In most cases, the returned Raster might contain more pixels
     * than requested.
     *
     * @see #useTightBBoxes
     */
    public abstract Raster getRaster(int x, int y, int w, int h);

    /**
     * Does the pixel accessibility of the destination surface
     * suggest that rendering algorithms might want to take
     * extra time to calculate a more accurate bounding box for
     * the operation being performed?
     * The typical case when this will be true is when a copy of
     * the pixels has to be made when doing a getRaster.  The
     * fewer pixels copied, the faster the operation will go.
     *
     * @see #getRaster
     */
    public boolean useTightBBoxes() {
        // Note: The native equivalent would trigger on VISIBLE_TO_NATIVE
        // REMIND: This is not used - should be obsoleted maybe
        return true;
    }

    /**
     * Returns the pixel data for the specified Argb value packed
     * into an integer for easy storage and conveyance.
     */
    public int pixelFor(int rgb) {
        return surfaceType.pixelFor(rgb, colorModel);
    }

    /**
     * Returns the pixel data for the specified color packed into an
     * integer for easy storage and conveyance.
     *
     * This method will use the getRGB() method of the Color object
     * and defer to the pixelFor(int rgb) method if not overridden.
     *
     * For now this is a convenience function, but for cases where
     * the highest quality color conversion is requested, this method
     * should be overridden in those cases so that a more direct
     * conversion of the color to the destination color space
     * can be done using the additional information in the Color
     * object.
     */
    public int pixelFor(Color c) {
        return pixelFor(c.getRGB());
    }

    /**
     * Returns the Argb representation for the specified integer value
     * which is packed in the format of the associated ColorModel.
     */
    public int rgbFor(int pixel) {
        return surfaceType.rgbFor(pixel, colorModel);
    }

    /**
     * Returns the bounds of the destination surface.
     */
    public abstract Rectangle getBounds();

    static java.security.Permission compPermission;

    /**
     * Performs Security Permissions checks to see if a Custom
     * Composite object should be allowed access to the pixels
     * of this surface.
     */
    protected void checkCustomComposite() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (compPermission == null) {
                compPermission =
                    new java.awt.AWTPermission("readDisplayPixels");
            }
            sm.checkPermission(compPermission);
        }
    }

    /**
     * Fetches private field IndexColorModel.allgrayopaque
     * which is true when all palette entries in the color
     * model are gray and opaque.
     */
    protected static native boolean isOpaqueGray(IndexColorModel icm);

    /**
     * For our purposes null and NullSurfaceData are the same as
     * they represent a disposed surface.
     */
    public static boolean isNull(SurfaceData sd) {
        if (sd == null || sd == NullSurfaceData.theInstance) {
            return true;
        }
        return false;
    }

    /**
     * Performs a copyarea within this surface.  Returns
     * false if there is no algorithm to perform the copyarea
     * given the current settings of the SunGraphics2D.
     *
     * @param x the x coordinate of the area in device space
     * @param y the y coordinate of the area in device space
     * @param w the width of the area in device space
     * @param h the height of the area in device space
     */
    public boolean copyArea(SunGraphics2D sg2d,
                            int x, int y, int w, int h, int dx, int dy)
    {
        return false;
    }

    /**
     * Synchronously releases resources associated with this surface.
     */
    public void flush() {}

    /**
     * Returns destination associated with this SurfaceData.  This could be
     * either an Image or a Component; subclasses of SurfaceData are
     * responsible for returning the appropriate object.
     */
    public abstract Object getDestination();

    /**
     * Returns default horizontal scale factor of the destination surface. Scale
     * factor describes the mapping between virtual and physical coordinates of the
     * SurfaceData. If the scale is 2 then virtual pixel coordinates need to be
     * doubled for physical pixels.
     */
    public double getDefaultScaleX() {
        return 1;
    }

    /**
     * Returns default vertical scale factor of the destination surface. Scale
     * factor describes the mapping between virtual and physical coordinates of the
     * SurfaceData. If the scale is 2 then virtual pixel coordinates need to be
     * doubled for physical pixels.
     */
    public double getDefaultScaleY() {
        return 1;
    }
}
