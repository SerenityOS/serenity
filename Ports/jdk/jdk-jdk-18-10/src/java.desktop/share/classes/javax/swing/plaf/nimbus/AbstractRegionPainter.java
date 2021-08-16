/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.nimbus;

import java.awt.*;
import java.awt.image.*;
import java.lang.reflect.Method;
import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.Painter;
import java.awt.print.PrinterGraphics;
import sun.reflect.misc.MethodUtil;

/**
 * Convenient base class for defining Painter instances for rendering a
 * region or component in Nimbus.
 *
 * @author Jasper Potts
 * @author Richard Bair
 */
public abstract class AbstractRegionPainter implements Painter<JComponent> {
    /**
     * PaintContext, which holds a lot of the state needed for cache hinting and x/y value decoding
     * The data contained within the context is typically only computed once and reused over
     * multiple paint calls, whereas the other values (w, h, f, leftWidth, etc) are recomputed
     * for each call to paint.
     *
     * This field is retrieved from subclasses on each paint operation. It is up
     * to the subclass to compute and cache the PaintContext over multiple calls.
     */
    private PaintContext ctx;
    /**
     * The scaling factor. Recomputed on each call to paint.
     */
    private float f;
    /*
      Various metrics used for decoding x/y values based on the canvas size
      and stretching insets.

      On each call to paint, we first ask the subclass for the PaintContext.
      From the context we get the canvas size and stretching insets, and whether
      the algorithm should be "inverted", meaning the center section remains
      a fixed size and the other sections scale.

      We then use these values to compute a series of metrics (listed below)
      which are used to decode points in a specific axis (x or y).

      The leftWidth represents the distance from the left edge of the region
      to the first stretching inset, after accounting for any scaling factor
      (such as DPI scaling). The centerWidth is the distance between the leftWidth
      and the rightWidth. The rightWidth is the distance from the right edge,
      to the right inset (after scaling has been applied).

      The same logic goes for topHeight, centerHeight, and bottomHeight.

      The leftScale represents the proportion of the width taken by the left section.
      The same logic is applied to the other scales.

      The various widths/heights are used to decode control points. The
      various scales are used to decode bezier handles (or anchors).
    */
    /**
     * The width of the left section. Recomputed on each call to paint.
     */
    private float leftWidth;
    /**
     * The height of the top section. Recomputed on each call to paint.
     */
    private float topHeight;
    /**
     * The width of the center section. Recomputed on each call to paint.
     */
    private float centerWidth;
    /**
     * The height of the center section. Recomputed on each call to paint.
     */
    private float centerHeight;
    /**
     * The width of the right section. Recomputed on each call to paint.
     */
    private float rightWidth;
    /**
     * The height of the bottom section. Recomputed on each call to paint.
     */
    private float bottomHeight;
    /**
     * The scaling factor to use for the left section. Recomputed on each call to paint.
     */
    private float leftScale;
    /**
     * The scaling factor to use for the top section. Recomputed on each call to paint.
     */
    private float topScale;
    /**
     * The scaling factor to use for the center section, in the horizontal
     * direction. Recomputed on each call to paint.
     */
    private float centerHScale;
    /**
     * The scaling factor to use for the center section, in the vertical
     * direction. Recomputed on each call to paint.
     */
    private float centerVScale;
    /**
     * The scaling factor to use for the right section. Recomputed on each call to paint.
     */
    private float rightScale;
    /**
     * The scaling factor to use for the bottom section. Recomputed on each call to paint.
     */
    private float bottomScale;

    /**
     * Create a new AbstractRegionPainter
     */
    protected AbstractRegionPainter() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void paint(Graphics2D g, JComponent c, int w, int h) {
        //don't render if the width/height are too small
        if (w <= 0 || h <=0) return;

        Object[] extendedCacheKeys = getExtendedCacheKeys(c);
        ctx = getPaintContext();
        PaintContext.CacheMode cacheMode = ctx == null ? PaintContext.CacheMode.NO_CACHING : ctx.cacheMode;
        if (cacheMode == PaintContext.CacheMode.NO_CACHING ||
                !ImageCache.getInstance().isImageCachable(w, h) ||
                g instanceof PrinterGraphics) {
            // no caching so paint directly
            paint0(g, c, w, h, extendedCacheKeys);
        } else if (cacheMode == PaintContext.CacheMode.FIXED_SIZES) {
            paintWithFixedSizeCaching(g, c, w, h, extendedCacheKeys);
        } else {
            // 9 Square caching
            paintWith9SquareCaching(g, ctx, c, w, h, extendedCacheKeys);
        }
    }

    /**
     * Get any extra attributes which the painter implementation would like
     * to include in the image cache lookups. This is checked for every call
     * of the paint(g, c, w, h) method.
     *
     * @param c The component on the current paint call
     * @return Array of extra objects to be included in the cache key
     */
    protected Object[] getExtendedCacheKeys(JComponent c) {
        return null;
    }

    /**
     * <p>Gets the PaintContext for this painting operation. This method is called on every
     * paint, and so should be fast and produce no garbage. The PaintContext contains
     * information such as cache hints. It also contains data necessary for decoding
     * points at runtime, such as the stretching insets, the canvas size at which the
     * encoded points were defined, and whether the stretching insets are inverted.</p>
     *
     * <p> This method allows for subclasses to package the painting of different states
     * with possibly different canvas sizes, etc, into one AbstractRegionPainter implementation.</p>
     *
     * @return a PaintContext associated with this paint operation.
     */
    protected abstract PaintContext getPaintContext();

    /**
     * <p>Configures the given Graphics2D. Often, rendering hints or compositing rules are
     * applied to a Graphics2D object prior to painting, which should affect all of the
     * subsequent painting operations. This method provides a convenient hook for configuring
     * the Graphics object prior to rendering, regardless of whether the render operation is
     * performed to an intermediate buffer or directly to the display.</p>
     *
     * @param g The Graphics2D object to configure. Will not be null.
     */
    protected void configureGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * Actually performs the painting operation. Subclasses must implement this method.
     * The graphics object passed may represent the actual surface being rendered to,
     * or it may be an intermediate buffer. It has also been pre-translated. Simply render
     * the component as if it were located at 0, 0 and had a width of <code>width</code>
     * and a height of <code>height</code>. For performance reasons, you may want to read
     * the clip from the Graphics2D object and only render within that space.
     *
     * @param g The Graphics2D surface to paint to
     * @param c The JComponent related to the drawing event. For example, if the
     *          region being rendered is Button, then <code>c</code> will be a
     *          JButton. If the region being drawn is ScrollBarSlider, then the
     *          component will be JScrollBar. This value may be null.
     * @param width The width of the region to paint. Note that in the case of
     *              painting the foreground, this value may differ from c.getWidth().
     * @param height The height of the region to paint. Note that in the case of
     *               painting the foreground, this value may differ from c.getHeight().
     * @param extendedCacheKeys The result of the call to getExtendedCacheKeys()
     */
    protected abstract void doPaint(Graphics2D g, JComponent c, int width,
                                    int height, Object[] extendedCacheKeys);

    /**
     * Decodes and returns a float value representing the actual pixel location for
     * the given encoded X value.
     *
     * @param x an encoded x value (0...1, or 1...2, or 2...3)
     * @return the decoded x value
     * @throws IllegalArgumentException
     *      if {@code x < 0} or {@code x > 3}
     */
    protected final float decodeX(float x) {
        if (x >= 0 && x <= 1) {
            return x * leftWidth;
        } else if (x > 1 && x < 2) {
            return ((x-1) * centerWidth) + leftWidth;
        } else if (x >= 2 && x <= 3) {
            return ((x-2) * rightWidth) + leftWidth + centerWidth;
        } else {
            throw new IllegalArgumentException("Invalid x");
        }
    }

    /**
     * Decodes and returns a float value representing the actual pixel location for
     * the given encoded y value.
     *
     * @param y an encoded y value (0...1, or 1...2, or 2...3)
     * @return the decoded y value
     * @throws IllegalArgumentException
     *      if {@code y < 0} or {@code y > 3}
     */
    protected final float decodeY(float y) {
        if (y >= 0 && y <= 1) {
            return y * topHeight;
        } else if (y > 1 && y < 2) {
            return ((y-1) * centerHeight) + topHeight;
        } else if (y >= 2 && y <= 3) {
            return ((y-2) * bottomHeight) + topHeight + centerHeight;
        } else {
            throw new IllegalArgumentException("Invalid y");
        }
    }

    /**
     * Decodes and returns a float value representing the actual pixel location for
     * the anchor point given the encoded X value of the control point, and the offset
     * distance to the anchor from that control point.
     *
     * @param x an encoded x value of the bezier control point (0...1, or 1...2, or 2...3)
     * @param dx the offset distance to the anchor from the control point x
     * @return the decoded x location of the control point
     * @throws IllegalArgumentException
     *      if {@code x < 0} or {@code x > 3}
     */
    protected final float decodeAnchorX(float x, float dx) {
        if (x >= 0 && x <= 1) {
            return decodeX(x) + (dx * leftScale);
        } else if (x > 1 && x < 2) {
            return decodeX(x) + (dx * centerHScale);
        } else if (x >= 2 && x <= 3) {
            return decodeX(x) + (dx * rightScale);
        } else {
            throw new IllegalArgumentException("Invalid x");
        }
    }

    /**
     * Decodes and returns a float value representing the actual pixel location for
     * the anchor point given the encoded Y value of the control point, and the offset
     * distance to the anchor from that control point.
     *
     * @param y an encoded y value of the bezier control point (0...1, or 1...2, or 2...3)
     * @param dy the offset distance to the anchor from the control point y
     * @return the decoded y position of the control point
     * @throws IllegalArgumentException
     *      if {@code y < 0} or {@code y > 3}
     */
    protected final float decodeAnchorY(float y, float dy) {
        if (y >= 0 && y <= 1) {
            return decodeY(y) + (dy * topScale);
        } else if (y > 1 && y < 2) {
            return decodeY(y) + (dy * centerVScale);
        } else if (y >= 2 && y <= 3) {
            return decodeY(y) + (dy * bottomScale);
        } else {
            throw new IllegalArgumentException("Invalid y");
        }
    }

    /**
     * Decodes and returns a color, which is derived from a base color in UI
     * defaults.
     *
     * @param key     A key corresponding to the value in the UI Defaults table
     *                of UIManager where the base color is defined
     * @param hOffset The hue offset used for derivation.
     * @param sOffset The saturation offset used for derivation.
     * @param bOffset The brightness offset used for derivation.
     * @param aOffset The alpha offset used for derivation. Between 0...255
     * @return The derived color, whose color value will change if the parent
     *         uiDefault color changes.
     */
    protected final Color decodeColor(String key, float hOffset, float sOffset,
                                      float bOffset, int aOffset) {
        if (UIManager.getLookAndFeel() instanceof NimbusLookAndFeel){
            NimbusLookAndFeel laf = (NimbusLookAndFeel) UIManager.getLookAndFeel();
            return laf.getDerivedColor(key, hOffset, sOffset, bOffset, aOffset, true);
        } else {
            // can not give a right answer as painter sould not be used outside
            // of nimbus laf but do the best we can
            return Color.getHSBColor(hOffset,sOffset,bOffset);
        }
    }

    /**
     * Decodes and returns a color, which is derived from a offset between two
     * other colors.
     *
     * @param color1   The first color
     * @param color2   The second color
     * @param midPoint The offset between color 1 and color 2, a value of 0.0 is
     *                 color 1 and 1.0 is color 2;
     * @return The derived color
     */
    protected final Color decodeColor(Color color1, Color color2,
                                      float midPoint) {
        return new Color(NimbusLookAndFeel.deriveARGB(color1, color2, midPoint));
    }

    /**
     * Given parameters for creating a LinearGradientPaint, this method will
     * create and return a linear gradient paint. One primary purpose for this
     * method is to avoid creating a LinearGradientPaint where the start and
     * end points are equal. In such a case, the end y point is slightly
     * increased to avoid the overlap.
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @param midpoints the midpoints
     * @param colors the colors
     * @return a valid LinearGradientPaint. This method never returns null.
     * @throws NullPointerException
     *      if {@code midpoints} array is null,
     *      or {@code colors} array is null,
     * @throws IllegalArgumentException
     *      if start and end points are the same points,
     *      or {@code midpoints.length != colors.length},
     *      or {@code colors} is less than 2 in size,
     *      or a {@code midpoints} value is less than 0.0 or greater than 1.0,
     *      or the {@code midpoints} are not provided in strictly increasing order
     */
    protected final LinearGradientPaint decodeGradient(float x1, float y1, float x2, float y2, float[] midpoints, Color[] colors) {
        if (x1 == x2 && y1 == y2) {
            y2 += .00001f;
        }
        return new LinearGradientPaint(x1, y1, x2, y2, midpoints, colors);
    }

    /**
     * Given parameters for creating a RadialGradientPaint, this method will
     * create and return a radial gradient paint. One primary purpose for this
     * method is to avoid creating a RadialGradientPaint where the radius
     * is non-positive. In such a case, the radius is just slightly
     * increased to avoid 0.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param r radius
     * @param midpoints the midpoints
     * @param colors the colors
     * @return a valid RadialGradientPaint. This method never returns null.
     * @throws NullPointerException
     *      if {@code midpoints} array is null,
     *      or {@code colors} array is null
     * @throws IllegalArgumentException
     *      if {@code r} is non-positive,
     *      or {@code midpoints.length != colors.length},
     *      or {@code colors} is less than 2 in size,
     *      or a {@code midpoints} value is less than 0.0 or greater than 1.0,
     *      or the {@code midpoints} are not provided in strictly increasing order
     */
    protected final RadialGradientPaint decodeRadialGradient(float x, float y, float r, float[] midpoints, Color[] colors) {
        if (r == 0f) {
            r = .00001f;
        }
        return new RadialGradientPaint(x, y, r, midpoints, colors);
    }

    /**
     * Get a color property from the given JComponent. First checks for a
     * <code>getXXX()</code> method and if that fails checks for a client
     * property with key <code>property</code>. If that still fails to return
     * a Color then <code>defaultColor</code> is returned.
     *
     * @param c The component to get the color property from
     * @param property The name of a bean style property or client property
     * @param defaultColor The color to return if no color was obtained from
     *        the component.
     * @param saturationOffset additively modifies the HSB saturation component
     * of the color returned (ignored if default color is returned).
     * @param brightnessOffset additively modifies the HSB brightness component
     * of the color returned (ignored if default color is returned).
     * @param alphaOffset additively modifies the ARGB alpha component of the
     * color returned (ignored if default color is returned).
     *
     * @return The color that was obtained from the component or defaultColor
     */
    protected final Color getComponentColor(JComponent c, String property,
                                            Color defaultColor,
                                            float saturationOffset,
                                            float brightnessOffset,
                                            int alphaOffset) {
        Color color = null;
        if (c != null) {
            // handle some special cases for performance
            if ("background".equals(property)) {
                color = c.getBackground();
            } else if ("foreground".equals(property)) {
                color = c.getForeground();
            } else if (c instanceof JList && "selectionForeground".equals(property)) {
                color = ((JList) c).getSelectionForeground();
            } else if (c instanceof JList && "selectionBackground".equals(property)) {
                color = ((JList) c).getSelectionBackground();
            } else if (c instanceof JTable && "selectionForeground".equals(property)) {
                color = ((JTable) c).getSelectionForeground();
            } else if (c instanceof JTable && "selectionBackground".equals(property)) {
                color = ((JTable) c).getSelectionBackground();
            } else {
                String s = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
                try {
                    Method method = MethodUtil.getMethod(c.getClass(), s, null);
                    color = (Color) MethodUtil.invoke(method, c, null);
                } catch (Exception e) {
                    //don't do anything, it just didn't work, that's all.
                    //This could be a normal occurance if you use a property
                    //name referring to a key in clientProperties instead of
                    //a real property
                }
                if (color == null) {
                    Object value = c.getClientProperty(property);
                    if (value instanceof Color) {
                        color = (Color) value;
                    }
                }
            }
        }
        // we return the defaultColor if the color found is null, or if
        // it is a UIResource. This is done because the color for the
        // ENABLED state is set on the component, but you don't want to use
        // that color for the over state. So we only respect the color
        // specified for the property if it was set by the user, as opposed
        // to set by us.
        if (color == null || color instanceof UIResource) {
            return defaultColor;
        } else if (saturationOffset != 0 || brightnessOffset != 0 || alphaOffset != 0) {
            float[] tmp = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            tmp[1] = clamp(tmp[1] + saturationOffset);
            tmp[2] = clamp(tmp[2] + brightnessOffset);
            int alpha = clamp(color.getAlpha() + alphaOffset);
            return new Color((Color.HSBtoRGB(tmp[0], tmp[1], tmp[2]) & 0xFFFFFF) | (alpha <<24));
        } else {
            return color;
        }
    }

    /**
     * A class encapsulating state useful when painting. Generally, instances of this
     * class are created once, and reused for each paint request without modification.
     * This class contains values useful when hinting the cache engine, and when decoding
     * control points and bezier curve anchors.
     */
    protected static class PaintContext {
        /**
         * Cache mode.
         */
        protected static enum CacheMode {
            /** No caching.*/
            NO_CACHING,
            /** Fixed sizes.*/
            FIXED_SIZES,
            /** Nine square scale.*/
            NINE_SQUARE_SCALE
        }

        private static Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

        private Insets stretchingInsets;
        private Dimension canvasSize;
        private boolean inverted;
        private CacheMode cacheMode;
        private double maxHorizontalScaleFactor;
        private double maxVerticalScaleFactor;

        private float a; // insets.left
        private float b; // canvasSize.width - insets.right
        private float c; // insets.top
        private float d; // canvasSize.height - insets.bottom;
        private float aPercent; // only used if inverted == true
        private float bPercent; // only used if inverted == true
        private float cPercent; // only used if inverted == true
        private float dPercent; // only used if inverted == true

        /**
         * Creates a new PaintContext which does not attempt to cache or scale any cached
         * images.
         *
         * @param insets The stretching insets. May be null. If null, then assumed to be 0, 0, 0, 0.
         * @param canvasSize The size of the canvas used when encoding the various x/y values. May be null.
         *                   If null, then it is assumed that there are no encoded values, and any calls
         *                   to one of the "decode" methods will return the passed in value.
         * @param inverted Whether to "invert" the meaning of the 9-square grid and stretching insets
         */
        public PaintContext(Insets insets, Dimension canvasSize, boolean inverted) {
            this(insets, canvasSize, inverted, null, 1, 1);
        }

        /**
         * Creates a new PaintContext.
         *
         * @param insets The stretching insets. May be null. If null, then assumed to be 0, 0, 0, 0.
         * @param canvasSize The size of the canvas used when encoding the various x/y values. May be null.
         *                   If null, then it is assumed that there are no encoded values, and any calls
         *                   to one of the "decode" methods will return the passed in value.
         * @param inverted Whether to "invert" the meaning of the 9-square grid and stretching insets
         * @param cacheMode A hint as to which caching mode to use. If null, then set to no caching.
         * @param maxH The maximum scale in the horizontal direction to use before punting and redrawing from scratch.
         *             For example, if maxH is 2, then we will attempt to scale any cached images up to 2x the canvas
         *             width before redrawing from scratch. Reasonable maxH values may improve painting performance.
         *             If set too high, then you may get poor looking graphics at higher zoom levels. Must be &gt;= 1.
         * @param maxV The maximum scale in the vertical direction to use before punting and redrawing from scratch.
         *             For example, if maxV is 2, then we will attempt to scale any cached images up to 2x the canvas
         *             height before redrawing from scratch. Reasonable maxV values may improve painting performance.
         *             If set too high, then you may get poor looking graphics at higher zoom levels. Must be &gt;= 1.
         */
        public PaintContext(Insets insets, Dimension canvasSize, boolean inverted,
                            CacheMode cacheMode, double maxH, double maxV) {
            if (maxH < 1 || maxV < 1) {
                throw new IllegalArgumentException("Both maxH and maxV must be >= 1");
            }

            this.stretchingInsets = insets == null ? EMPTY_INSETS : insets;
            this.canvasSize = canvasSize;
            this.inverted = inverted;
            this.cacheMode = cacheMode == null ? CacheMode.NO_CACHING : cacheMode;
            this.maxHorizontalScaleFactor = maxH;
            this.maxVerticalScaleFactor = maxV;

            if (canvasSize != null) {
                a = stretchingInsets.left;
                b = canvasSize.width - stretchingInsets.right;
                c = stretchingInsets.top;
                d = canvasSize.height - stretchingInsets.bottom;
                this.canvasSize = canvasSize;
                this.inverted = inverted;
                if (inverted) {
                    float available = canvasSize.width - (b - a);
                    aPercent = available > 0f ? a / available : 0f;
                    bPercent = available > 0f ? b / available : 0f;
                    available = canvasSize.height - (d - c);
                    cPercent = available > 0f ? c / available : 0f;
                    dPercent = available > 0f ? d / available : 0f;
                }
            }
        }
    }

    //---------------------- private methods

    //initializes the class to prepare it for being able to decode points
    private void prepare(float w, float h) {
        //if no PaintContext has been specified, reset the values and bail
        //also bail if the canvasSize was not set (since decoding will not work)
        if (ctx == null || ctx.canvasSize == null) {
            f = 1f;
            leftWidth = centerWidth = rightWidth = 0f;
            topHeight = centerHeight = bottomHeight = 0f;
            leftScale = centerHScale = rightScale = 0f;
            topScale = centerVScale = bottomScale = 0f;
            return;
        }

        //calculate the scaling factor, and the sizes for the various 9-square sections
        Number scale = (Number)UIManager.get("scale");
        f = scale == null ? 1f : scale.floatValue();

        if (ctx.inverted) {
            centerWidth = (ctx.b - ctx.a) * f;
            float availableSpace = w - centerWidth;
            leftWidth = availableSpace * ctx.aPercent;
            rightWidth = availableSpace * ctx.bPercent;
            centerHeight = (ctx.d - ctx.c) * f;
            availableSpace = h - centerHeight;
            topHeight = availableSpace * ctx.cPercent;
            bottomHeight = availableSpace * ctx.dPercent;
        } else {
            leftWidth = ctx.a * f;
            rightWidth = (float)(ctx.canvasSize.getWidth() - ctx.b) * f;
            centerWidth = w - leftWidth - rightWidth;
            topHeight = ctx.c * f;
            bottomHeight = (float)(ctx.canvasSize.getHeight() - ctx.d) * f;
            centerHeight = h - topHeight - bottomHeight;
        }

        leftScale = ctx.a == 0f ? 0f : leftWidth / ctx.a;
        centerHScale = (ctx.b - ctx.a) == 0f ? 0f : centerWidth / (ctx.b - ctx.a);
        rightScale = (ctx.canvasSize.width - ctx.b) == 0f ? 0f : rightWidth / (ctx.canvasSize.width - ctx.b);
        topScale = ctx.c == 0f ? 0f : topHeight / ctx.c;
        centerVScale = (ctx.d - ctx.c) == 0f ? 0f : centerHeight / (ctx.d - ctx.c);
        bottomScale = (ctx.canvasSize.height - ctx.d) == 0f ? 0f : bottomHeight / (ctx.canvasSize.height - ctx.d);
    }

    private void paintWith9SquareCaching(Graphics2D g, PaintContext ctx,
                                         JComponent c, int w, int h,
                                         Object[] extendedCacheKeys) {
        // check if we can scale to the requested size
        Dimension canvas = ctx.canvasSize;
        Insets insets = ctx.stretchingInsets;
        if (w <= (canvas.width * ctx.maxHorizontalScaleFactor) && h <= (canvas.height * ctx.maxVerticalScaleFactor)) {
            // get image at canvas size
            VolatileImage img = getImage(g.getDeviceConfiguration(), c, canvas.width, canvas.height, extendedCacheKeys);
            if (img != null) {
                // calculate dst inserts
                // todo: destination inserts need to take into acount scale factor for high dpi. Note: You can use f for this, I think
                Insets dstInsets;
                if (ctx.inverted){
                    int leftRight = (w-(canvas.width-(insets.left+insets.right)))/2;
                    int topBottom = (h-(canvas.height-(insets.top+insets.bottom)))/2;
                    dstInsets = new Insets(topBottom,leftRight,topBottom,leftRight);
                } else {
                    dstInsets = insets;
                }
                // paint 9 square scaled
                Object oldScaleingHints = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                ImageScalingHelper.paint(g, 0, 0, w, h, img, insets, dstInsets,
                        ImageScalingHelper.PaintType.PAINT9_STRETCH, ImageScalingHelper.PAINT_ALL);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    oldScaleingHints!=null?oldScaleingHints:RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            } else {
                // render directly
                paint0(g, c, w, h, extendedCacheKeys);
            }
        } else {
            // paint directly
            paint0(g, c, w, h, extendedCacheKeys);
        }
    }

    private void paintWithFixedSizeCaching(Graphics2D g, JComponent c, int w,
                                           int h, Object[] extendedCacheKeys) {
        VolatileImage img = getImage(g.getDeviceConfiguration(), c, w, h, extendedCacheKeys);
        if (img != null) {
            //render cached image
            g.drawImage(img, 0, 0, null);
        } else {
            // render directly
            paint0(g, c, w, h, extendedCacheKeys);
        }
    }

    /** Gets the rendered image for this painter at the requested size, either from cache or create a new one */
    private VolatileImage getImage(GraphicsConfiguration config, JComponent c,
                                   int w, int h, Object[] extendedCacheKeys) {
        ImageCache imageCache = ImageCache.getInstance();
        //get the buffer for this component
        VolatileImage buffer = (VolatileImage) imageCache.getImage(config, w, h, this, extendedCacheKeys);

        int renderCounter = 0; //to avoid any potential, though unlikely, infinite loop
        do {
            //validate the buffer so we can check for surface loss
            int bufferStatus = VolatileImage.IMAGE_INCOMPATIBLE;
            if (buffer != null) {
                bufferStatus = buffer.validate(config);
            }

            //If the buffer status is incompatible or restored, then we need to re-render to the volatile image
            if (bufferStatus == VolatileImage.IMAGE_INCOMPATIBLE || bufferStatus == VolatileImage.IMAGE_RESTORED) {
                //if the buffer is null (hasn't been created), or isn't the right size, or has lost its contents,
                //then recreate the buffer
                if (buffer == null || buffer.getWidth() != w || buffer.getHeight() != h ||
                        bufferStatus == VolatileImage.IMAGE_INCOMPATIBLE) {
                    //clear any resources related to the old back buffer
                    if (buffer != null) {
                        buffer.flush();
                        buffer = null;
                    }
                    //recreate the buffer
                    buffer = config.createCompatibleVolatileImage(w, h,
                            Transparency.TRANSLUCENT);
                    // put in cache for future
                    imageCache.setImage(buffer, config, w, h, this, extendedCacheKeys);
                }
                //create the graphics context with which to paint to the buffer
                Graphics2D bg = buffer.createGraphics();
                //clear the background before configuring the graphics
                bg.setComposite(AlphaComposite.Clear);
                bg.fillRect(0, 0, w, h);
                bg.setComposite(AlphaComposite.SrcOver);
                configureGraphics(bg);
                // paint the painter into buffer
                paint0(bg, c, w, h, extendedCacheKeys);
                //close buffer graphics
                bg.dispose();
            }
        } while (buffer.contentsLost() && renderCounter++ < 3);
        // check if we failed
        if (renderCounter == 3) return null;
        // return image
        return buffer;
    }

    //convenience method which creates a temporary graphics object by creating a
    //clone of the passed in one, configuring it, drawing with it, disposing it.
    //These steps have to be taken to ensure that any hints set on the graphics
    //are removed subsequent to painting.
    private void paint0(Graphics2D g, JComponent c, int width, int height,
                        Object[] extendedCacheKeys) {
        prepare(width, height);
        g = (Graphics2D)g.create();
        configureGraphics(g);
        doPaint(g, c, width, height, extendedCacheKeys);
        g.dispose();
    }

    private float clamp(float value) {
        if (value < 0) {
            value = 0;
        } else if (value > 1) {
            value = 1;
        }
        return value;
    }

    private int clamp(int value) {
        if (value < 0) {
            value = 0;
        } else if (value > 255) {
            value = 255;
        }
        return value;
    }
}
